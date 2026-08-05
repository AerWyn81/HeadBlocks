package fr.aerwyn81.headblocks.utils.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public class BukkitSchedulerAdapter implements SchedulerAdapter {

    private final Plugin plugin;

    public BukkitSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Task runTask(@NotNull Runnable task) {
        return wrap(Bukkit.getScheduler().runTask(plugin, task));
    }

    @Override
    public Task runTaskLater(@NotNull Runnable task, long delayTicks) {
        return wrap(Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks));
    }

    @Override
    public Task runTaskTimer(@NotNull Runnable task, long delayTicks, long periodTicks) {
        return wrap(Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks));
    }

    @Override
    public Task runTask(Location location, @NotNull Runnable task) {
        return runTask(task);
    }

    @Override
    public Task runTaskLater(Location location, @NotNull Runnable task, long delayTicks) {
        return runTaskLater(task, delayTicks);
    }

    @Override
    public Task runTaskTimer(Location location, @NotNull Runnable task, long delayTicks, long periodTicks) {
        return runTaskTimer(task, delayTicks, periodTicks);
    }

    @Override
    public Task runTask(Entity entity, @NotNull Runnable task) {
        return runTask(task);
    }

    @Override
    public Task runTaskLater(Entity entity, @NotNull Runnable task, long delayTicks) {
        return runTaskLater(task, delayTicks);
    }

    @Override
    public void runNow(Location location, @NotNull Runnable task) {
        task.run();
    }

    @Override
    public void runNow(Entity entity, @NotNull Runnable task) {
        task.run();
    }

    @Override
    public Task runTaskAsync(@NotNull Runnable task) {
        return wrap(Bukkit.getScheduler().runTaskAsynchronously(plugin, task));
    }

    @Override
    public void cancelAllTasks() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }

    private Task wrap(BukkitTask task) {
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
