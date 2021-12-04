package fr.aerwyn81.headblocks.runnables;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.handlers.ConfigHandler;
import fr.aerwyn81.headblocks.handlers.HeadHandler;
import fr.aerwyn81.headblocks.handlers.StorageHandler;
import fr.aerwyn81.headblocks.utils.FormatUtils;
import fr.aerwyn81.headblocks.utils.xseries.ParticleDisplay;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.javatuples.Pair;

import java.awt.*;
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
                try {
                    spawnParticles(h.getValue1(), players.toArray(new Player[0]));
                } catch (Exception ex) {
                    inError = true;
                    HeadBlocks.log.sendMessage(FormatUtils.translate("&cCannot spawn particle for HeadBlocks... " + ex.getMessage()));
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

    private void spawnParticles(Location location, Player... players) {
        ParticleDisplay t = new ParticleDisplay();

        int pCount = configHandler.getParticlesNotFoundAmount();
        if (pCount != 1) {
            t.withCount(pCount);
        }

        Particle pType = Particle.valueOf(configHandler.getParticlesNotFoundType());
        t.withParticle(pType);

        if (pType == Particle.REDSTONE) {
            for (String color : configHandler.getParticlesNotFoundColors()) {
                String[] colors = color.split(",");
                t.withColor(new Color(Integer.parseInt(colors[0]), Integer.parseInt(colors[1]), Integer.parseInt(colors[2])), 1);
                t.spawn(location.clone().add(.5f, 0.75f, .5f), players);
            }

            return;
        }

        t.spawn(location.clone().add(.5f, 1f, .5f), players);
    }
}
