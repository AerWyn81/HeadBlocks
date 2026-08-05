package fr.aerwyn81.headblocks.utils.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FoliaSchedulerAdapter implements SchedulerAdapter {

    private final Plugin plugin;

    private final Set<Task> repeatingTasks = ConcurrentHashMap.newKeySet();

    public FoliaSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Task runTask(@NotNull Runnable task) {
        return wrap(Bukkit.getGlobalRegionScheduler().run(plugin, t -> task.run()));
    }

    @Override
    public Task runTaskLater(@NotNull Runnable task, long delayTicks) {
        return wrap(Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> task.run(), atLeastOneTick(delayTicks)));
    }

    @Override
    public Task runTaskTimer(@NotNull Runnable task, long delayTicks, long periodTicks) {
        return track(wrap(Bukkit.getGlobalRegionScheduler()
                .runAtFixedRate(plugin, t -> task.run(), atLeastOneTick(delayTicks), atLeastOneTick(periodTicks))));
    }

    @Override
    public Task runTask(Location location, @NotNull Runnable task) {
        if (hasNoRegion(location)) {
            return runTask(task);
        }

        return wrap(Bukkit.getRegionScheduler().run(plugin, location, t -> task.run()));
    }

    @Override
    public Task runTaskLater(Location location, @NotNull Runnable task, long delayTicks) {
        if (hasNoRegion(location)) {
            return runTaskLater(task, delayTicks);
        }

        return wrap(Bukkit.getRegionScheduler().runDelayed(plugin, location, t -> task.run(), atLeastOneTick(delayTicks)));
    }

    @Override
    public Task runTaskTimer(Location location, @NotNull Runnable task, long delayTicks, long periodTicks) {
        if (hasNoRegion(location)) {
            return runTaskTimer(task, delayTicks, periodTicks);
        }

        return track(wrap(Bukkit.getRegionScheduler()
                .runAtFixedRate(plugin, location, t -> task.run(), atLeastOneTick(delayTicks), atLeastOneTick(periodTicks))));
    }

    @Override
    public Task runTask(Entity entity, @NotNull Runnable task) {
        if (entity == null) {
            return runTask(task);
        }

        return wrap(entity.getScheduler().run(plugin, t -> task.run(), null));
    }

    @Override
    public Task runTaskLater(Entity entity, @NotNull Runnable task, long delayTicks) {
        if (entity == null) {
            return runTaskLater(task, delayTicks);
        }

        return wrap(entity.getScheduler().runDelayed(plugin, t -> task.run(), null, atLeastOneTick(delayTicks)));
    }

    @Override
    public void runNow(Location location, @NotNull Runnable task) {
        if (hasNoRegion(location) || Bukkit.isOwnedByCurrentRegion(location)) {
            task.run();
            return;
        }

        Bukkit.getRegionScheduler().execute(plugin, location, task);
    }

    @Override
    public void runNow(Entity entity, @NotNull Runnable task) {
        if (entity == null || Bukkit.isOwnedByCurrentRegion(entity)) {
            task.run();
            return;
        }

        entity.getScheduler().execute(plugin, task, null, 1L);
    }

    @Override
    public Task runTaskAsync(@NotNull Runnable task) {
        return wrap(Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run()));
    }

    @Override
    public void cancelAllTasks() {
        repeatingTasks.forEach(Task::cancel);
        repeatingTasks.clear();

        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
        Bukkit.getAsyncScheduler().cancelTasks(plugin);
    }

    private Task track(Task task) {
        if (task == Task.NONE) {
            return task;
        }

        repeatingTasks.add(task);

        return new Task() {
            @Override
            public void cancel() {
                repeatingTasks.remove(task);
                task.cancel();
            }

            @Override
            public boolean isCancelled() {
                return task.isCancelled();
            }
        };
    }

    private boolean hasNoRegion(Location location) {
        return location == null || location.getWorld() == null;
    }

    private long atLeastOneTick(long ticks) {
        return Math.max(1, ticks);
    }

    private Task wrap(ScheduledTask task) {
        if (task == null) {
            return Task.NONE;
        }

        return new Task() {
            @Override
            public void cancel() {
                task.cancel();
            }

            @Override
            public boolean isCancelled() {
                return task.isCancelled();
            }
        };
    }
}
