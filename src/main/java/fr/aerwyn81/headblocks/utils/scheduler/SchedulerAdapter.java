package fr.aerwyn81.headblocks.utils.scheduler;

import fr.aerwyn81.headblocks.utils.scheduler.task.Task;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.ApiStatus;

public interface SchedulerAdapter {

    Task runTask(Location location, Runnable task);

    Task runTaskLater(Location location, Runnable task, long delayTicks);

    Task runTaskTimer(Location location, Runnable task, long delayTicks, long periodTicks);

    Task runTask(Entity entity, Runnable task);

    Task runTaskLater(Entity entity, Runnable task, long delayTicks);

    Task runTaskTimer(Entity entity, Runnable task, long delayTicks, long periodTicks);

    Task runTaskGlobal(Runnable task);

    Task runTaskGlobalLater(Runnable task, long delayTicks);

    Task runTaskGlobalTimer(Runnable task, long delayTicks, long periodTicks);

    Task runTaskAsync(Runnable task);

    @ApiStatus.Obsolete
    void cancelTask(Task task);

    void cancelAllTasks();
}
