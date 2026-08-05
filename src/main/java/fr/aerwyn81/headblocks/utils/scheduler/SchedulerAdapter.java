package fr.aerwyn81.headblocks.utils.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Location and entity overloads pin work to the owning region on Folia. A null location or a
 * location whose world is unloaded falls back to the global scheduler on every implementation.
 * Returned tasks are never null; a task that could not be scheduled is {@link Task#NONE}.
 */
public interface SchedulerAdapter {

    Task runTask(@NotNull Runnable task);

    Task runTaskLater(@NotNull Runnable task, long delayTicks);

    Task runTaskTimer(@NotNull Runnable task, long delayTicks, long periodTicks);

    Task runTask(@Nullable Location location, @NotNull Runnable task);

    Task runTaskLater(@Nullable Location location, @NotNull Runnable task, long delayTicks);

    Task runTaskTimer(@Nullable Location location, @NotNull Runnable task, long delayTicks, long periodTicks);

    Task runTask(@Nullable Entity entity, @NotNull Runnable task);

    Task runTaskLater(@Nullable Entity entity, @NotNull Runnable task, long delayTicks);

    /**
     * Runs immediately when the caller already owns the target region, and defers otherwise.
     * Meant for hot loops that must not pay for a scheduled task on a single-threaded server.
     */
    void runNow(@Nullable Location location, @NotNull Runnable task);

    void runNow(@Nullable Entity entity, @NotNull Runnable task);

    Task runTaskAsync(@NotNull Runnable task);

    void cancelAllTasks();
}
