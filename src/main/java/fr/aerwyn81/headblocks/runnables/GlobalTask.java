package fr.aerwyn81.headblocks.runnables;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.data.hunt.HuntConfig;
import fr.aerwyn81.headblocks.utils.bukkit.ParticlesUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class GlobalTask extends BukkitRunnable {

    private static final int CHUNK_SIZE = 16;
    private static int VIEW_RADIUS_CHUNKS = 1;

    private static final Random random = new Random();
    private static final int HUNT_SYNC_INTERVAL = 100; // ~5 seconds at 20 TPS
    private boolean particlesDisabled = false;
    private int tickCounter = 0;

    private final ServiceRegistry registry;

    public GlobalTask(ServiceRegistry registry) {
        this.registry = registry;
        VIEW_RADIUS_CHUNKS = (int) Math.ceil(registry.getConfigService().hologramParticlePlayerViewDistance() / (double) CHUNK_SIZE);
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

        registry.getHeadService().getChargedHeadLocations().forEach(headLocation -> {
            var location = headLocation.getLocation();
            if (location.getWorld() == null || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                return;
            }

            // Resolve primary display hunt for this head
            Hunt primaryHunt = registry.getHuntService().getHighestPriorityHuntForHead(headLocation.getUuid());
            HuntConfig huntConfig = primaryHunt != null ? primaryHunt.getConfig() : new HuntConfig(registry.getConfigService());

            if (huntConfig.isSpinEnabled() && huntConfig.isSpinLinked()) {
                registry.getHeadService().rotateHead(headLocation);
            }

            registry.getHologramService().ensureHologramsCreated(location, huntConfig);

            handleHologramAndParticles(headLocation, huntConfig);
        });
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

        var particle = isFound ? Particle.valueOf(huntConfig.getParticlesFoundType())
                : Particle.valueOf(huntConfig.getParticlesNotFoundType());

        var amount = isFound ? huntConfig.getParticlesFoundAmount()
                : huntConfig.getParticlesNotFoundAmount();

        var colors = isFound ? registry.getConfigService().particlesFoundColors()
                : registry.getConfigService().particlesNotFoundColors();

        try {
            ParticlesUtils.spawn(location, particle, amount, colors, player);
        } catch (Exception ex) {
            LogUtil.error("Cannot spawn particle {0}... {1}", particle.name(), ex.getMessage());
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
            var playerLoc = player.getLocation();
            if (playerLoc.getWorld() != location.getWorld()) {
                continue;
            }

            var playerChunkX = playerLoc.getBlockX() >> 4;
            var playerChunkZ = playerLoc.getBlockZ() >> 4;

            var chunkDistanceX = Math.abs(hologramChunkX - playerChunkX);
            var chunkDistanceZ = Math.abs(hologramChunkZ - playerChunkZ);

            if (chunkDistanceX <= VIEW_RADIUS_CHUNKS && chunkDistanceZ <= VIEW_RADIUS_CHUNKS) {
                var distanceSq = location.distanceSquared(playerLoc);

                if (distanceSq <= rangeParticlesSq || distanceSq <= rangeHintSq) {
                    try {
                        var hasHead = registry.getStorageService().hasHead(player.getUniqueId(), headLocation.getUuid());

                        if (distanceSq <= rangeParticlesSq) {
                            if (hasHead) {
                                spawnParticles(location, true, player, huntConfig);
                                registry.getHologramService().showFoundTo(player, location, huntConfig);
                            } else {
                                spawnParticles(location, false, player, huntConfig);
                                registry.getHologramService().showNotFoundTo(player, location, huntConfig);
                            }

                            registry.getHologramService().refresh(player, location);
                        }

                        if (distanceSq <= rangeHintSq && (headLocation.isHintSoundEnabled() || headLocation.isHintActionBarEnabled())) {
                            // Resolve per-player hint config: use the highest-priority active hunt where the player hasn't found this head
                            HuntConfig hintConfig = null;
                            if (!hasHead) {
                                // Fast path: player hasn't found it globally -> primary hunt config
                                hintConfig = huntConfig;
                            } else {
                                // Player found it globally -- check per-hunt for one they haven't completed
                                for (Hunt h : registry.getHuntService().getHuntsForHead(headLocation.getUuid())) {
                                    if (!h.isActive()) {
                                        continue;
                                    }
                                    try {
                                        if (!registry.getStorageService().getHeadsPlayerForHunt(player.getUniqueId(), h.getId())
                                                .contains(headLocation.getUuid())) {
                                            hintConfig = h.getConfig();
                                            break;
                                        }
                                    } catch (InternalException ignored) {
                                    }
                                }
                            }

                            if (hintConfig != null && hintConfig.isHintsEnabled()) {
                                var hintFrequency = Math.max(1, hintConfig.getHintFrequency());
                                var shouldTriggerHintSound = random.nextInt(hintFrequency) == 0;
                                var shouldTriggerHintActionBar = random.nextInt(hintFrequency) == 0;

                                if (headLocation.isHintSoundEnabled() && shouldTriggerHintSound) {
                                    registry.getConfigService().hintSoundType()
                                            .record()
                                            .withVolume(registry.getConfigService().hintSoundVolume())
                                            .withPitch(random.nextInt(3))
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
                        }
                    } catch (InternalException ex) {
                        LogUtil.error("Error while trying to communicate with the storage : {0}", ex.getMessage());
                    }
                    continue;
                }
            }

            registry.getHologramService().hideHolograms(headLocation, player);
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
            if (up) {
                return "⬆";
            }
            return down ? "⬇" : "⬆";
        }
        if (angle >= 22.5 && angle < 67.5) {
            if (up) {
                return "⬉";
            }
            return down ? "⬋" : "⬉";
        }
        if (angle >= 67.5 && angle < 112.5) {
            if (up) {
                return "⬉";
            }
            return down ? "⬋" : "⬅";
        }
        if (angle >= 112.5 && angle < 157.5) {
            return "⬋";
        }
        if (angle >= -67.5 && angle < -22.5) {
            if (up) {
                return "⬈";
            }
            return down ? "⬊" : "⬈";
        }
        if (angle >= -112.5 && angle < -67.5) {
            if (up) {
                return "⬈";
            }
            return down ? "⬊" : "➡";
        }
        if (angle >= -157.5 && angle < -112.5) {
            return "⬊";
        }

        return "⬇";
    }
}
