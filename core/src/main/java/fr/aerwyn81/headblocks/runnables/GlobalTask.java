package fr.aerwyn81.headblocks.runnables;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.ConfigService;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.HologramService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.utils.bukkit.ParticlesUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;

public class GlobalTask extends BukkitRunnable {

    private static final int CHUNK_SIZE = 16;
    private static int VIEW_RADIUS_CHUNKS = 1;

    public GlobalTask() {
        VIEW_RADIUS_CHUNKS = (int) Math.ceil(ConfigService.getHologramParticlePlayerViewDistance() / (double)CHUNK_SIZE);
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
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cCannot spawn particle " + particle.name() + "... " + ex.getMessage()));
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cTo prevent log spamming, particles is disabled until reload"));
            this.cancel();
        }
    }

    private void handleHologramAndParticles(HeadLocation headLocation) {
        int range = ConfigService.getHologramParticlePlayerViewDistance();

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

                if (distance <= range) {
                    try {
                        if (StorageService.hasHead(player.getUniqueId(), headLocation.getUuid())) {
                            spawnParticles(location, true, player);
                            HologramService.showFoundTo(player, location);
                        } else {
                            spawnParticles(location, false, player);
                            HologramService.showNotFoundTo(player, location);
                        }
                    } catch (InternalException ex) {
                        HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to communicate with the storage : " + ex.getMessage()));
                        this.cancel();
                    }
                    continue;
                }
            }

            HologramService.hideHolograms(headLocation, player);
        }
    }
}