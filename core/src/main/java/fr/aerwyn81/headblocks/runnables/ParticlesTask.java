package fr.aerwyn81.headblocks.runnables;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.handlers.ConfigHandler;
import fr.aerwyn81.headblocks.handlers.HeadHandler;
import fr.aerwyn81.headblocks.handlers.StorageHandler;
import fr.aerwyn81.headblocks.utils.FormatUtils;
import fr.aerwyn81.headblocks.utils.ParticlesUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ParticlesTask extends BukkitRunnable {

    private final ConfigHandler configHandler;
    private final HeadHandler headHandler;
    private final StorageHandler storageHandler;

    public ParticlesTask(HeadBlocks main) {
        this.configHandler = main.getConfigHandler();
        this.headHandler = main.getHeadHandler();
        this.storageHandler = main.getStorageHandler();
    }

    @Override
    public void run() {
        headHandler.getHeadLocations().forEach(h -> {
            Pair<List<Player>, List<Player>> players = playersInRange(h);

            if (players.getValue0().size() != 0) {
                String particleName = configHandler.getParticlesFoundType();
                int amount = configHandler.getParticlesFoundAmount();
                ArrayList<String> colors = configHandler.getParticlesFoundColors();

                spawnParticles(h.getValue1(), Particle.valueOf(particleName), amount, colors, players.getValue0().toArray(new Player[0]));
            }

            if (players.getValue1().size() != 0) {
                String particleName = configHandler.getParticlesNotFoundType();
                int amount = configHandler.getParticlesNotFoundAmount();
                ArrayList<String> colors = configHandler.getParticlesNotFoundColors();

                spawnParticles(h.getValue1(), Particle.valueOf(particleName), amount, colors, players.getValue1().toArray(new Player[0]));
            }
        });
    }

    private void spawnParticles(Location location, Particle particle, int amount, ArrayList<String> colors, Player... players) {
        try {
            ParticlesUtils.spawn(location, particle, amount, colors, players);
        } catch (Exception ex) {
            this.cancel();
            HeadBlocks.log.sendMessage(FormatUtils.translate("&cCannot spawn particle " + particle.name() + "... " + ex.getMessage()));
            HeadBlocks.log.sendMessage(FormatUtils.translate("&cTo prevent log spamming, particles is disabled until reload"));
        }
    }

    private Pair<List<Player>, List<Player>> playersInRange(Pair<UUID, Location> uuidLocPair) {
        int range = configHandler.getParticlesPlayerViewDistance();
        Location loc = uuidLocPair.getValue1();

        List<Player> playersInRange = loc.getWorld().getNearbyEntities(loc, range, range, range).stream()
                .filter(Player.class::isInstance)
                .map(e -> (Player) e)
                .collect(Collectors.toList());

        List<Player> playersFound = playersInRange.stream()
                .filter(p -> storageHandler.hasAlreadyClaimedHead(p.getUniqueId(), uuidLocPair.getValue0()))
                .collect(Collectors.toList());

        List<Player> playersNotFound = playersInRange.stream()
                .filter(i -> !playersFound.contains(i))
                .collect(Collectors.toList());

        return new Pair<>(playersFound, playersNotFound);
    }
}
