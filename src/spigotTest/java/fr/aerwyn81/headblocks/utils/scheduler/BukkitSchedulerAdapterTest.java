package fr.aerwyn81.headblocks.utils.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class BukkitSchedulerAdapterTest {

    private MockedStatic<Bukkit> bukkit;
    private BukkitScheduler bukkitScheduler;
    private BukkitTask bukkitTask;
    private Plugin plugin;
    private BukkitSchedulerAdapter adapter;

    @BeforeEach
    void setUp() {
        bukkitScheduler = mock(BukkitScheduler.class);
        bukkitTask = mock(BukkitTask.class);
        plugin = mock(Plugin.class);

        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(bukkitScheduler);

        adapter = new BukkitSchedulerAdapter(plugin);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    @DisplayName("the location overload ignores the region and uses the single main thread")
    void location_overload_delegates_to_the_main_scheduler() {
        when(bukkitScheduler.runTask(eq(plugin), any(Runnable.class))).thenReturn(bukkitTask);
        Location location = mock(Location.class);
        Runnable body = () -> {
        };

        adapter.runTask(location, body);

        verify(bukkitScheduler).runTask(plugin, body);
        verifyNoInteractions(location);
    }

    @Test
    @DisplayName("the entity overload ignores the entity and uses the single main thread")
    void entity_overload_delegates_to_the_main_scheduler() {
        when(bukkitScheduler.runTaskLater(eq(plugin), any(Runnable.class), anyLong())).thenReturn(bukkitTask);
        Entity entity = mock(Entity.class);
        Runnable body = () -> {
        };

        adapter.runTaskLater(entity, body, 5L);

        verify(bukkitScheduler).runTaskLater(plugin, body, 5L);
        verifyNoInteractions(entity);
    }

    @Test
    @DisplayName("a null Bukkit task never leaks out as a null Task")
    void null_bukkit_task_becomes_task_none() {
        when(bukkitScheduler.runTask(eq(plugin), any(Runnable.class))).thenReturn(null);

        Task task = adapter.runTask(() -> {
        });

        assertThat(task).isSameAs(Task.NONE);
        assertThat(task.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("cancel and isCancelled are forwarded to the wrapped task")
    void task_wrapper_forwards_cancellation() {
        when(bukkitScheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong())).thenReturn(bukkitTask);
        when(bukkitTask.isCancelled()).thenReturn(true);

        Task task = adapter.runTaskTimer(() -> {
        }, 0L, 20L);
        task.cancel();

        verify(bukkitTask).cancel();
        assertThat(task.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("delays are passed through untouched, including zero")
    void delays_are_not_coerced() {
        when(bukkitScheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong())).thenReturn(bukkitTask);

        adapter.runTaskTimer(() -> {
        }, 0L, 20L);

        verify(bukkitScheduler).runTaskTimer(eq(plugin), any(Runnable.class), eq(0L), eq(20L));
    }

    @Test
    @DisplayName("runNow executes inline: a single-threaded server never needs to hand off")
    void run_now_executes_inline() {
        AtomicBoolean ran = new AtomicBoolean(false);

        adapter.runNow(mock(Location.class), () -> ran.set(true));

        assertThat(ran).isTrue();
        verifyNoInteractions(bukkitScheduler);
    }

    @Test
    @DisplayName("runNow for an entity also executes inline")
    void run_now_entity_executes_inline() {
        AtomicBoolean ran = new AtomicBoolean(false);

        adapter.runNow(mock(Entity.class), () -> ran.set(true));

        assertThat(ran).isTrue();
        verifyNoInteractions(bukkitScheduler);
    }

    @Test
    @DisplayName("cancelAllTasks cancels the plugin's tasks")
    void cancel_all_tasks_delegates() {
        adapter.cancelAllTasks();

        verify(bukkitScheduler).cancelTasks(plugin);
    }
}
