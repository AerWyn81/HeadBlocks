package fr.aerwyn81.headblocks.runnables;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HuntConfig;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.SpawnParticle;
import fr.aerwyn81.headblocks.utils.bukkit.ParticlesUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

public class GlobalTask implements Runnable {

    private static final int CHUNK_SIZE = 16;

    private static final int HUNT_SYNC_INTERVAL = 100; // ~5 seconds at 20 TPS
    private volatile boolean particlesDisabled = false;
    private int tickCounter = 0;

    private final int viewRadiusChunks;
    private final ServiceRegistry registry;

    public GlobalTask(ServiceRegistry registry) {
        this.registry = registry;
        this.viewRadiusChunks = (int) Math.ceil(registry.getConfigService().hologramParticlePlayerViewDistance() / (double) CHUNK_SIZE);
    }

    @Override
    public void run() {
        if (HeadBlocks.isReloadInProgress) {
            return;
        }

        // Periodic hunt sync check (cross-server via Redis version counter)
        tickCounter++;
        if (tickCounter >= HUNT_SYNC_INTERVAL) {
            tickCounter = 0;
            registry.getHuntService().checkRemoteChanges();
        }

        registry.getHeadService().getChargedHeadLocations().forEach(this::scheduleHead);
        registry.getHeadService().getSpawnedHeads().forEach(this::scheduleHead);
    }

    private void scheduleHead(HeadLocation headLocation) {
        var location = headLocation.getLocation();
        if (location.getWorld() == null) {
            return;
        }

        registry.getScheduler().runNow(location, () -> handleHead(headLocation, location));
    }

    private void handleHead(HeadLocation headLocation, Location location) {
        if (registry.getHeadService().getHeadByUUID(headLocation.getUuid()) != headLocation) {
            return;
        }

        if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            return;
        }

        // Resolve hunt config for this head (1:1)
        HuntConfig huntConfig = registry.getHuntService().configOf(headLocation.getHuntId());

        if (huntConfig.isSpinEnabled() && huntConfig.isSpinLinked()) {
            registry.getHeadService().rotateHead(headLocation);
        }

        registry.getHologramService().ensureHologramsCreated(location, huntConfig);

        handleHologramAndParticles(headLocation, huntConfig);
    }

    private void spawnTemplateParticle(Location location, SpawnParticle particle, Player player) {
        if (particlesDisabled) {
            return;
        }

        try {
            ParticlesUtils.spawn(location, ParticlesUtils.resolve(particle.name()), particle.amount(), particle.colorList(), player);
        } catch (Exception ex) {
            LogUtil.error("Cannot spawn particle {0}... {1}", particle.name(), ex.getMessage());
            LogUtil.error("To prevent log spamming, particles are disabled until reload");
            particlesDisabled = true;
        }
    }

    private void spawnParticles(Location location, boolean isFound, Player player, HuntConfig huntConfig) {
        if (particlesDisabled) {
            return;
        }

        if (isFound && registry.getConfigService().isHideFoundHeads()) {
            return;
        }

        if (isFound ? !huntConfig.isParticlesFoundEnabled() : !huntConfig.isParticlesNotFoundEnabled()) {
            return;
        }

        var particleName = isFound ? huntConfig.getParticlesFoundType()
                : huntConfig.getParticlesNotFoundType();

        var amount = isFound ? huntConfig.getParticlesFoundAmount()
                : huntConfig.getParticlesNotFoundAmount();

        var colors = isFound ? registry.getConfigService().particlesFoundColors()
                : registry.getConfigService().particlesNotFoundColors();

        // Resolution is inside the guard on purpose: an unknown particle name used to throw here on
        // every head, for every player, every tick.
        try {
            ParticlesUtils.spawn(location, ParticlesUtils.resolve(particleName), amount, colors, player);
        } catch (Exception ex) {
            LogUtil.error("Cannot spawn particle {0}... {1}", particleName, ex.getMessage());
            LogUtil.error("To prevent log spamming, particles are disabled until reload");
            particlesDisabled = true;
        }
    }

    private void handleHologramAndParticles(HeadLocation headLocation, HuntConfig huntConfig) {
        int rangeParticles = registry.getConfigService().hologramParticlePlayerViewDistance();
        int rangeHint = huntConfig.getHintDistance();
        double rangeParticlesSq = (double) rangeParticles * rangeParticles;
        double rangeHintSq = (double) rangeHint * rangeHint;

        var location = headLocation.getLocation();
        if (location.getWorld() == null) {
            return;
        }

        var hologramChunkX = location.getBlockX() >> 4;
        var hologramChunkZ = location.getBlockZ() >> 4;

        for (var player : new java.util.ArrayList<>(Bukkit.getOnlinePlayers())) {
            registry.getScheduler().runNow(player, () -> handleForPlayer(player, headLocation, huntConfig,
                    hologramChunkX, hologramChunkZ, rangeParticlesSq, rangeHintSq));
        }
    }

    private void handleForPlayer(Player player, HeadLocation headLocation, HuntConfig huntConfig,
                                 int hologramChunkX, int hologramChunkZ,
                                 double rangeParticlesSq, double rangeHintSq) {
        int rangeHint = huntConfig.getHintDistance();
        var location = headLocation.getLocation();

        var playerLoc = player.getLocation();
        if (playerLoc.getWorld() != location.getWorld()) {
            return;
        }

        var playerChunkX = playerLoc.getBlockX() >> 4;
        var playerChunkZ = playerLoc.getBlockZ() >> 4;

        var chunkDistanceX = Math.abs(hologramChunkX - playerChunkX);
        var chunkDistanceZ = Math.abs(hologramChunkZ - playerChunkZ);

        if (chunkDistanceX <= viewRadiusChunks && chunkDistanceZ <= viewRadiusChunks) {
            var distanceSq = location.distanceSquared(playerLoc);

            if (distanceSq <= rangeParticlesSq || distanceSq <= rangeHintSq) {
                try {
                    handleNearbyPlayer(player, headLocation, location, huntConfig, new Proximity(distanceSq, rangeParticlesSq, rangeHintSq), rangeHint);
                } catch (InternalException ex) {
                    LogUtil.error("Error while trying to communicate with the storage : {0}", ex.getMessage());
                }
                return;
            }
        }

        registry.getHologramService().hideHolograms(headLocation, player);
    }

    private record Proximity(double distanceSq, double rangeParticlesSq, double rangeHintSq) {
    }

    private void handleNearbyPlayer(Player player, HeadLocation headLocation, Location location, HuntConfig huntConfig,
                                    Proximity proximity, int rangeHint) throws InternalException {
        var distanceSq = proximity.distanceSq();
        var hasHead = registry.getStorageService().hasHead(player.getUniqueId(), headLocation.getUuid());

        if (distanceSq <= proximity.rangeParticlesSq()) {
            showHeadState(player, headLocation, location, huntConfig, hasHead);
        }

        if (distanceSq <= proximity.rangeHintSq() && (headLocation.isHintSoundEnabled() || headLocation.isHintActionBarEnabled())) {
            // Resolve per-player hint config using the head's hunt (1:1)
            HuntConfig hintConfig = hintConfigFor(player, headLocation, huntConfig, hasHead);

            if (hintConfig != null && hintConfig.isHintsEnabled()) {
                sendHints(player, headLocation, location, hintConfig, distanceSq, rangeHint);
            }
        }
    }

    private HuntConfig hintConfigFor(Player player, HeadLocation headLocation, HuntConfig huntConfig, boolean hasHead) {
        if (!hasHead) {
            return huntConfig;
        }
        if (isMissingFromItsHunt(player, headLocation)) {
            return huntConfig;
        }
        return null;
    }

    private void showHeadState(Player player, HeadLocation headLocation, Location location, HuntConfig huntConfig, boolean hasHead) {
        if (hasHead) {
            spawnParticles(location, true, player, huntConfig);
            registry.getHologramService().showFoundTo(player, location, huntConfig);
        } else {
            var templateParticle = registry.getSpawnService().particleOf(headLocation);
            if (templateParticle != null) {
                spawnTemplateParticle(location, templateParticle, player);
            } else {
                spawnParticles(location, false, player, huntConfig);
            }
            registry.getHologramService().showNotFoundTo(player, location, huntConfig);
        }

        registry.getHologramService().refresh(player, location);
    }

    private void sendHints(Player player, HeadLocation headLocation, Location location, HuntConfig hintConfig,
                           double distanceSq, int rangeHint) {
        var hintFrequency = Math.max(1, hintConfig.getHintFrequency());
        var shouldTriggerHintSound = ThreadLocalRandom.current().nextInt(hintFrequency) == 0;
        var shouldTriggerHintActionBar = ThreadLocalRandom.current().nextInt(hintFrequency) == 0;

        if (headLocation.isHintSoundEnabled() && shouldTriggerHintSound) {
            registry.getConfigService().hintSoundType()
                    .record()
                    .withVolume(registry.getConfigService().hintSoundVolume())
                    .withPitch(ThreadLocalRandom.current().nextInt(3))
                    .soundPlayer()
                    .forPlayers(player)
                    .atLocation(location)
                    .play();
        }

        if (headLocation.isHintActionBarEnabled() && shouldTriggerHintActionBar) {
            var distance = Math.sqrt(distanceSq);
            var message = registry.getPlaceholdersService().parse(player.getName(), player.getUniqueId(), headLocation, registry.getConfigService().hintActionBarMessage());
            message = message
                    .replace("%distance%", String.valueOf(distance))
                    .replace("%position%", String.valueOf(rangeHint - distance))
                    .replace("%arrow%", getHintDirectionArrow(player.getLocation(), location));

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
        }
    }

    private String getHintDirectionArrow(Location playerLoc, Location targetLoc) {
        if (playerLoc.distance(targetLoc) < 1.5) {
            return "●";
        }

        var playerToHead = targetLoc.clone().subtract(playerLoc.toVector());
        var playerLooking = playerLoc.getDirection();

        var dy = targetLoc.getY() - playerLoc.getY();
        var angle = Math.atan2(
                playerToHead.getX() * playerLooking.getZ() - playerToHead.getZ() * playerLooking.getX(),
                playerToHead.getX() * playerLooking.getX() + playerToHead.getZ() * playerLooking.getZ()
        ) * 180 / Math.PI;

        var up = dy > 2;
        var down = dy < -2;

        if (angle >= -22.5 && angle < 22.5) {
            return arrowFor(up, down, "⬆", "⬇", "⬆");
        }
        if (angle >= 22.5 && angle < 67.5) {
            return arrowFor(up, down, "⬉", "⬋", "⬉");
        }
        if (angle >= 67.5 && angle < 112.5) {
            return arrowFor(up, down, "⬉", "⬋", "⬅");
        }
        if (angle >= 112.5 && angle < 157.5) {
            return "⬋";
        }
        if (angle >= -67.5 && angle < -22.5) {
            return arrowFor(up, down, "⬈", "⬊", "⬈");
        }
        if (angle >= -112.5 && angle < -67.5) {
            return arrowFor(up, down, "⬈", "⬊", "➡");
        }
        if (angle >= -157.5 && angle < -112.5) {
            return "⬊";
        }

        return "⬇";
    }

    private static String arrowFor(boolean up, boolean down, String upArrow, String downArrow, String flatArrow) {
        if (up) {
            return upArrow;
        }
        return down ? downArrow : flatArrow;
    }

    private boolean isMissingFromItsHunt(Player player, HeadLocation headLocation) {
        try {
            return !registry.getStorageService().getHeadsPlayerForHunt(player.getUniqueId(), headLocation.getHuntId())
                    .contains(headLocation.getUuid());
        } catch (InternalException ignored) {
            return false;
        }
    }
}
