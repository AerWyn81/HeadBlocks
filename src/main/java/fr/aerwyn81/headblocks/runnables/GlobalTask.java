package fr.aerwyn81.headblocks.runnables;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.*;
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

import java.util.Collections;
import java.util.Random;

public class GlobalTask extends BukkitRunnable {

    private static final int CHUNK_SIZE = 16;
    private static int VIEW_RADIUS_CHUNKS = 1;

    private static final Random random = new Random();

    public GlobalTask() {
        VIEW_RADIUS_CHUNKS = (int) Math.ceil(ConfigService.getHologramParticlePlayerViewDistance() / (double) CHUNK_SIZE);
    }

    @Override
    public void run() {
        if (HeadBlocks.isReloadInProgress)
            return;

        HeadService.getChargedHeadLocations().forEach(headLocation -> {
            var location = headLocation.getLocation();
            if (location.getWorld() == null || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4))
                return;

            if (ConfigService.isSpinEnabled() && ConfigService.isSpinLinked()) {
                HeadService.rotateHead(headLocation);
            }

            handleHologramAndParticles(headLocation);
        });
    }

    private void spawnParticles(Location location, boolean isFound, Player player) {
        if (isFound && ConfigService.hideFoundHeads())
            return;

        if (isFound ? !ConfigService.isParticlesFoundEnabled() : !ConfigService.isParticlesNotFoundEnabled())
            return;

        var particle = isFound ? Particle.valueOf(ConfigService.getParticlesFoundType())
                : Particle.valueOf(ConfigService.getParticlesNotFoundType());

        var amount = isFound ? ConfigService.getParticlesFoundAmount()
                : ConfigService.getParticlesNotFoundAmount();

        var colors = isFound ? ConfigService.getParticlesFoundColors()
                : ConfigService.getParticlesNotFoundColors();

        try {
            ParticlesUtils.spawn(location, particle, amount, colors, player);
        } catch (Exception ex) {
            LogUtil.error("Cannot spawn particle {0}... {1}", particle.name(), ex.getMessage());
            LogUtil.error("To prevent log spamming, particles is disabled until reload");
            this.cancel();
        }
    }

    private void handleHologramAndParticles(HeadLocation headLocation) {
        int rangeParticles = ConfigService.getHologramParticlePlayerViewDistance();
        int rangeHint = ConfigService.getHintDistanceBlocks();

        var location = headLocation.getLocation();
        if (location.getWorld() == null)
            return;

        var hologramChunkX = location.getBlockX() / CHUNK_SIZE;
        var hologramChunkZ = location.getBlockZ() / CHUNK_SIZE;

        for (var player : Collections.synchronizedCollection(Bukkit.getOnlinePlayers())) {
            var playerLoc = player.getLocation();
            if (playerLoc.getWorld() != location.getWorld())
                continue;

            var playerChunkX = playerLoc.getBlockX() / CHUNK_SIZE;
            var playerChunkZ = playerLoc.getBlockZ() / CHUNK_SIZE;

            var chunkDistanceX = Math.abs(hologramChunkX - playerChunkX);
            var chunkDistanceZ = Math.abs(hologramChunkZ - playerChunkZ);

            if (chunkDistanceX <= VIEW_RADIUS_CHUNKS && chunkDistanceZ <= VIEW_RADIUS_CHUNKS) {
                var distance = location.distance(playerLoc);

                if (distance <= rangeParticles || distance <= rangeHint) {
                    try {
                        var hasHead = StorageService.hasHead(player.getUniqueId(), headLocation.getUuid());

                        if (distance <= rangeParticles) {
                            if (hasHead) {
                                spawnParticles(location, true, player);
                                HologramService.showFoundTo(player, location);
                            } else {
                                spawnParticles(location, false, player);
                                HologramService.showNotFoundTo(player, location);
                            }

                            HologramService.refresh(player, location);
                        }

                        if (distance <= rangeHint) {
                            if (headLocation.isHintSoundEnabled() || headLocation.isHintActionBarEnabled()) {
                                var shouldTriggerHintSound = random.nextInt(ConfigService.getHintFrequency()) == 0;
                                var shouldTriggerHintActionBar = random.nextInt(ConfigService.getHintFrequency()) == 0;

                                if (headLocation.isHintSoundEnabled() && shouldTriggerHintSound) {
                                    ConfigService.getHintSoundType()
                                            .record()
                                            .withVolume(ConfigService.getHintSoundVolume())
                                            .withPitch(random.nextInt(3))
                                            .soundPlayer()
                                            .forPlayers(player)
                                            .atLocation(location)
                                            .play();
                                }

                                if (headLocation.isHintActionBarEnabled() && shouldTriggerHintActionBar) {
                                    var message = PlaceholdersService.parse(player.getName(), player.getUniqueId(), headLocation, ConfigService.getHintActionBarMessage());
                                    message = message
                                            .replaceAll("%distance%", String.valueOf(distance))
                                            .replaceAll("%position%", String.valueOf(rangeHint - distance))
                                            .replaceAll("%arrow%", getHintDirectionArrow(player.getLocation(), location));

                                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
                                }
                            }
                        }
                    } catch (InternalException ex) {
                        LogUtil.error("Error while trying to communicate with the storage : {0}", ex.getMessage());
                        this.cancel();
                    }
                    continue;
                }
            }

            HologramService.hideHolograms(headLocation, player);
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
            if (up)
                return "⬆";
            return down ? "⬇" : "⬆";
        }
        if (angle >= 22.5 && angle < 67.5) {
            if (up)
                return "⬉";
            return down ? "⬋" : "⬉";
        }
        if (angle >= 67.5 && angle < 112.5) {
            if (up)
                return "⬉";
            return down ? "⬋" : "⬅";
        }
        if (angle >= 112.5 && angle < 157.5) {
            return "⬋";
        }
        if (angle >= -67.5 && angle < -22.5) {
            if (up)
                return "⬈";
            return down ? "⬊" : "⬈";
        }
        if (angle >= -112.5 && angle < -67.5) {
            if (up)
                return "⬈";
            return down ? "⬊" : "➡";
        }
        if (angle >= -157.5 && angle < -112.5) {
            return "⬊";
        }

        return "⬇";
    }
}