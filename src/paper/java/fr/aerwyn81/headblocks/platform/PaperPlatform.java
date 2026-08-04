package fr.aerwyn81.headblocks.platform;

import fr.aerwyn81.headblocks.utils.scheduler.BukkitSchedulerAdapter;
import fr.aerwyn81.headblocks.utils.scheduler.FoliaSchedulerAdapter;
import fr.aerwyn81.headblocks.utils.scheduler.SchedulerAdapter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;

public class PaperPlatform implements Platform {

    private static final boolean FOLIA = classExists("io.papermc.paper.threadedregions.RegionizedServer");

    @Override
    public String name() {
        return FOLIA ? "Folia" : "Paper";
    }

    @Override
    public SchedulerAdapter createScheduler(Plugin plugin) {
        return FOLIA ? new FoliaSchedulerAdapter(plugin) : new BukkitSchedulerAdapter(plugin);
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Entity entity, Location location) {
        return entity.teleportAsync(location);
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
