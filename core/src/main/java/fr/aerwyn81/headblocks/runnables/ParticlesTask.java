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

    private boolean inError;

    public ParticlesTask(HeadBlocks main) {
        this.configHandler = main.getConfigHandler();
        this.headHandler = main.getHeadHandler();
        this.storageHandler = main.getStorageHandler();
    }

    @Override
    public void run() {
        headHandler.getHeadLocations().forEach(h -> {
            List<Player> players = playersInRange(h);

            if (!inError && players.size() != 0) {
                String particleName = configHandler.getParticlesNotFoundType();
                int amount = configHandler.getParticlesNotFoundAmount();
                ArrayList<String> colors = configHandler.getParticlesNotFoundColors();

                try {
                    ParticlesUtils.spawn(h.getValue1(), Particle.valueOf(particleName), amount, colors, players.toArray(new Player[0]));
                } catch (Exception ex) {
                    inError = true;
                    HeadBlocks.log.sendMessage(FormatUtils.translate("&cCannot spawn particle " + particleName + "... " + ex.getMessage()));
                    HeadBlocks.log.sendMessage(FormatUtils.translate("&cTo prevent log spam, particles is disabled until reload!"));
                }
            }
        });
    }

    private List<Player> playersInRange(Pair<UUID, Location> uuidLocPair) {
        int range = configHandler.getParticlesNotFoundPlayerViewDistance();

        Location loc = uuidLocPair.getValue1();

        return loc.getWorld().getNearbyEntities(loc, range, range, range).stream()
                .filter(Player.class::isInstance)
                .map(e -> (Player) e)
                .filter(p -> !storageHandler.hasAlreadyClaimedHead(p.getUniqueId(), uuidLocPair.getValue0()))
                .collect(Collectors.toList());
    }
}
