package fr.aerwyn81.headblocks.utils.scheduler;

import fr.aerwyn81.headblocks.utils.scheduler.task.BukkitTaskImpl;
import fr.aerwyn81.headblocks.utils.scheduler.task.Task;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public record BukkitSchedulerAdapter(Plugin plugin) implements SchedulerAdapter {

    @Override
    public Task runTask(Location location, Runnable task) {
        return new BukkitTaskImpl(Bukkit.getScheduler().runTask(plugin, task));
    }

    @Override
    public Task runTaskLater(Location location, Runnable task, long delayTicks) {
        return new BukkitTaskImpl(Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks));
    }

    @Override
    public Task runTaskTimer(Location location, Runnable task, long delayTicks, long periodTicks) {
        return new BukkitTaskImpl(Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks));
    }

    @Override
    public Task runTask(Entity entity, Runnable task) {
        return new BukkitTaskImpl(Bukkit.getScheduler().runTask(plugin, task));
    }

    @Override
    public Task runTaskLater(Entity entity, Runnable task, long delayTicks) {
        return new BukkitTaskImpl(Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks));
    }

    @Override
    public Task runTaskTimer(Entity entity, Runnable task, long delayTicks, long periodTicks) {
        return new BukkitTaskImpl(Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks));
    }

    @Override
    public Task runTaskGlobal(Runnable task) {
        return new BukkitTaskImpl(Bukkit.getScheduler().runTask(plugin, task));
    }

    @Override
    public Task runTaskGlobalLater(Runnable task, long delayTicks) {
        return new BukkitTaskImpl(Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks));
    }

    @Override
    public Task runTaskGlobalTimer(Runnable task, long delayTicks, long periodTicks) {
        return new BukkitTaskImpl(Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks));
    }

    @Override
    public Task runTaskAsync(Runnable task) {
        return new BukkitTaskImpl(Bukkit.getScheduler().runTaskAsynchronously(plugin, task));
    }

    @Override
    public void cancelTask(Task task) {
        task.cancel();
    }

    @Override
    public void cancelAllTasks() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }
}
