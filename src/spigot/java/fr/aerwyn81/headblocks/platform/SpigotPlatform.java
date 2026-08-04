package fr.aerwyn81.headblocks.platform;

import fr.aerwyn81.headblocks.utils.scheduler.BukkitSchedulerAdapter;
import fr.aerwyn81.headblocks.utils.scheduler.SchedulerAdapter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;

public class SpigotPlatform implements Platform {

    @Override
    public String name() {
        return "Spigot";
    }

    @Override
    public SchedulerAdapter createScheduler(Plugin plugin) {
        return new BukkitSchedulerAdapter(plugin);
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Entity entity, Location location) {
        return CompletableFuture.completedFuture(entity.teleport(location));
    }
}
