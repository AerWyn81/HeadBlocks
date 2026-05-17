package fr.aerwyn81.headblocks.utils.scheduler;

import fr.aerwyn81.headblocks.utils.scheduler.task.ScheduledTaskImpl;
import fr.aerwyn81.headblocks.utils.scheduler.task.Task;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public record FoliaSchedulerAdapter(Plugin plugin) implements SchedulerAdapter {
    @Override
    public Task runTask(Location location, Runnable task) {
        return new ScheduledTaskImpl(Bukkit.getRegionScheduler().run(plugin, location, t -> task.run()));
    }

    @Override
    public Task runTaskLater(Location location, Runnable task, long delayTicks) {
        return new ScheduledTaskImpl(Bukkit.getRegionScheduler().runDelayed(plugin, location, t -> task.run(), coerce(delayTicks)));
    }

    @Override
    public Task runTaskTimer(Location location, Runnable task, long delayTicks, long periodTicks) {
        return new ScheduledTaskImpl(Bukkit.getRegionScheduler().runAtFixedRate(plugin, location, t -> task.run(), coerce(delayTicks), coerce(periodTicks)));
    }

    @Override
    public Task runTask(Entity entity, Runnable task) {
        return new ScheduledTaskImpl(entity.getScheduler().run(plugin, t -> task.run(), null));
    }

    @Override
    public Task runTaskLater(Entity entity, Runnable task, long delayTicks) {
        return new ScheduledTaskImpl(entity.getScheduler().runDelayed(plugin, t -> task.run(), null, coerce(delayTicks)));
    }

    @Override
    public Task runTaskTimer(Entity entity, Runnable task, long delayTicks, long periodTicks) {
        return new ScheduledTaskImpl(entity.getScheduler().runAtFixedRate(plugin, t -> task.run(), null, coerce(delayTicks), coerce(periodTicks)));
    }

    @Override
    public Task runTaskGlobal(Runnable task) {
       return new ScheduledTaskImpl(Bukkit.getGlobalRegionScheduler().run(plugin, t -> task.run()));
    }

    @Override
    public Task runTaskGlobalLater(Runnable task, long delayTicks) {
        return new ScheduledTaskImpl(Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> task.run(), coerce(delayTicks)));
    }

    @Override
    public Task runTaskGlobalTimer(Runnable task, long delayTicks, long periodTicks) {
        return new ScheduledTaskImpl(Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> task.run(), coerce(delayTicks), coerce(periodTicks)));
    }

    @Override
    public Task runTaskAsync(Runnable task) {
        return new ScheduledTaskImpl(Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run()));
    }

    @Override
    public void cancelTask(Task task) {
        task.cancel();
    }

    @Override
    public void cancelAllTasks() {

    }

    private long coerce(long ticks) {
        return Math.max(1, ticks);
    }
}
