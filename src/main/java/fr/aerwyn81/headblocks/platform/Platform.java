package fr.aerwyn81.headblocks.platform;

import fr.aerwyn81.headblocks.utils.scheduler.SchedulerAdapter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;

public interface Platform {

    String name();

    SchedulerAdapter createScheduler(Plugin plugin);

    CompletableFuture<Boolean> teleportAsync(Entity entity, Location location);
}
