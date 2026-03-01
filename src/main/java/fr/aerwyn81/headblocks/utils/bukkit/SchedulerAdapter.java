package fr.aerwyn81.headblocks.utils.bukkit;

public interface SchedulerAdapter {
    void runTask(Runnable task);

    void runTaskAsync(Runnable task);

    void runTaskLater(Runnable task, long delayTicks);

    int runTaskTimer(Runnable task, long delayTicks, long periodTicks);

    void cancelTask(int taskId);

    void cancelAllTasks();
}
