package fr.aerwyn81.headblocks.utils.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class BukkitSchedulerAdapter implements SchedulerAdapter {
    private final Plugin plugin;

    public BukkitSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runTask(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task).getTaskId();
    }

    @Override
    public void runTaskAsync(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task).getTaskId();
    }

    @Override
    public void runTaskLater(Runnable task, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks).getTaskId();
    }

    @Override
    public int runTaskTimer(Runnable task, long delayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks).getTaskId();
    }

    @Override
    public void cancelTask(int taskId) {
        Bukkit.getScheduler().cancelTask(taskId);
    }

    @Override
    public void cancelAllTasks() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }
}
