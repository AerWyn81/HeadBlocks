package fr.aerwyn81.headblocks.runnables;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.handlers.ConfigService;
import fr.aerwyn81.headblocks.handlers.HeadService;
import fr.aerwyn81.headblocks.handlers.HologramHandler;
import fr.aerwyn81.headblocks.handlers.StorageHandler;
import fr.aerwyn81.headblocks.utils.InternalException;
import fr.aerwyn81.headblocks.utils.MessageUtils;
import fr.aerwyn81.headblocks.utils.ParticlesUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GlobalTask extends BukkitRunnable {
    private final StorageHandler storageHandler;
    private final HologramHandler hologramHandler;

    public GlobalTask(HeadBlocks main) {
        this.storageHandler = main.getStorageHandler();
        this.hologramHandler = main.getHologramHandler();
    }

    @Override
    public void run() {
        if (HeadService.getChargedHeadLocations().isEmpty())
            return;

        for (HeadLocation headLocation : HeadService.getChargedHeadLocations()) {
            Location location = headLocation.getLocation();

            if (!location.getChunk().isLoaded())
                continue;

            List<Player> players = playersInRange(location);

            players.forEach(p -> {
                try {
                    if (storageHandler.hasHead(p.getUniqueId(), headLocation.getUuid())) {
                        if (ConfigService.isParticlesFoundEnabled()) {
                            spawnParticles(location, Particle.valueOf(ConfigService.getParticlesFoundType()),
                                    ConfigService.getParticlesFoundAmount(), ConfigService.getParticlesFoundColors(), p);
                        }

                        if (ConfigService.isHologramsFoundEnabled()) {
                            hologramHandler.showFoundTo(p, location);
                        }
                    } else {
                        if (ConfigService.isParticlesNotFoundEnabled()) {
                            spawnParticles(location, Particle.valueOf(ConfigService.getParticlesNotFoundType()),
                                    ConfigService.getParticlesNotFoundAmount(), ConfigService.getParticlesNotFoundColors(), p);
                        }

                        if (ConfigService.isHologramsNotFoundEnabled()) {
                            hologramHandler.showNotFoundTo(p, location);
                        }
                    }
                } catch (InternalException ex) {
                    HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to communicate with the storage : " + ex.getMessage()));
                    this.cancel();
                }
            });
        }
    }

    private void spawnParticles(Location location, Particle particle, int amount, ArrayList<String> colors, Player... players) {
        try {
            ParticlesUtils.spawn(location, particle, amount, colors, players);
        } catch (Exception ex) {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cCannot spawn particle " + particle.name() + "... " + ex.getMessage()));
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cTo prevent log spamming, particles is disabled until reload"));
            this.cancel();
        }
    }

    private List<Player> playersInRange(Location loc) {
        int range = ConfigService.getHologramParticlePlayerViewDistance();
        return loc.getWorld().getNearbyEntities(loc, range, range, range).stream().filter(Player.class::isInstance).map(e -> (Player) e).collect(Collectors.toList());
    }
}