package fr.aerwyn81.headblocks.utils.scheduler;

import io.papermc.paper.threadedregions.scheduler.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
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

class FoliaSchedulerAdapterTest {

    private MockedStatic<Bukkit> bukkit;
    private GlobalRegionScheduler globalScheduler;
    private RegionScheduler regionScheduler;
    private AsyncScheduler asyncScheduler;
    private ScheduledTask scheduledTask;
    private Plugin plugin;
    private FoliaSchedulerAdapter adapter;

    @BeforeEach
    void setUp() {
        globalScheduler = mock(GlobalRegionScheduler.class);
        regionScheduler = mock(RegionScheduler.class);
        asyncScheduler = mock(AsyncScheduler.class);
        scheduledTask = mock(ScheduledTask.class);
        plugin = mock(Plugin.class);

        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getGlobalRegionScheduler).thenReturn(globalScheduler);
        bukkit.when(Bukkit::getRegionScheduler).thenReturn(regionScheduler);
        bukkit.when(Bukkit::getAsyncScheduler).thenReturn(asyncScheduler);

        adapter = new FoliaSchedulerAdapter(plugin);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    private Location locationWithWorld() {
        Location location = mock(Location.class);
        when(location.getWorld()).thenReturn(mock(World.class));
        return location;
    }

    @Test
    @DisplayName("a location with a world is pinned to its region")
    void location_with_world_uses_the_region_scheduler() {
        when(regionScheduler.run(eq(plugin), any(Location.class), any())).thenReturn(scheduledTask);
        Location location = locationWithWorld();

        adapter.runTask(location, () -> {
        });

        verify(regionScheduler).run(eq(plugin), eq(location), any());
        verifyNoInteractions(globalScheduler);
    }

    @Test
    @DisplayName("an unloaded world falls back to the global scheduler instead of throwing")
    void location_without_world_falls_back_to_global() {
        when(globalScheduler.run(eq(plugin), any())).thenReturn(scheduledTask);
        Location location = mock(Location.class);
        when(location.getWorld()).thenReturn(null);

        adapter.runTask(location, () -> {
        });

        verify(globalScheduler).run(eq(plugin), any());
        verifyNoInteractions(regionScheduler);
    }

    @Test
    @DisplayName("a null location falls back to the global scheduler instead of throwing")
    void null_location_falls_back_to_global() {
        when(globalScheduler.runAtFixedRate(eq(plugin), any(), anyLong(), anyLong())).thenReturn(scheduledTask);

        adapter.runTaskTimer(null, () -> {
        }, 0L, 20L);

        verify(globalScheduler).runAtFixedRate(eq(plugin), any(), anyLong(), anyLong());
        verifyNoInteractions(regionScheduler);
    }

    @Test
    @DisplayName("a removed entity yields Task.NONE rather than a null Task")
    void removed_entity_yields_task_none() {
        Entity entity = mock(Entity.class);
        EntityScheduler entityScheduler = mock(EntityScheduler.class);
        when(entity.getScheduler()).thenReturn(entityScheduler);
        when(entityScheduler.run(eq(plugin), any(), any())).thenReturn(null);

        Task task = adapter.runTask(entity, () -> {
        });

        assertThat(task).isSameAs(Task.NONE);
        assertThat(task.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("a null entity falls back to the global scheduler")
    void null_entity_falls_back_to_global() {
        when(globalScheduler.run(eq(plugin), any())).thenReturn(scheduledTask);

        adapter.runTask((Entity) null, () -> {
        });

        verify(globalScheduler).run(eq(plugin), any());
    }

    @Test
    @DisplayName("zero delays are raised to one tick, which Folia requires")
    void zero_delays_are_coerced_to_one_tick() {
        when(globalScheduler.runAtFixedRate(eq(plugin), any(), anyLong(), anyLong())).thenReturn(scheduledTask);

        adapter.runTaskTimer(() -> {
        }, 0L, 0L);

        verify(globalScheduler).runAtFixedRate(eq(plugin), any(), eq(1L), eq(1L));
    }

    @Test
    @DisplayName("runNow executes inline when the caller already owns the region")
    void run_now_executes_inline_when_region_is_owned() {
        Location location = locationWithWorld();
        bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(location)).thenReturn(true);
        AtomicBoolean ran = new AtomicBoolean(false);

        adapter.runNow(location, () -> ran.set(true));

        assertThat(ran).isTrue();
        verifyNoInteractions(regionScheduler);
    }

    @Test
    @DisplayName("runNow hands off when the region belongs to another thread")
    void run_now_hands_off_when_region_is_foreign() {
        Location location = locationWithWorld();
        bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(location)).thenReturn(false);
        AtomicBoolean ran = new AtomicBoolean(false);

        adapter.runNow(location, () -> ran.set(true));

        assertThat(ran).isFalse();
        verify(regionScheduler).execute(eq(plugin), eq(location), any());
    }

    @Test
    @DisplayName("runNow on an unloaded world executes inline rather than throwing")
    void run_now_without_world_executes_inline() {
        Location location = mock(Location.class);
        when(location.getWorld()).thenReturn(null);
        AtomicBoolean ran = new AtomicBoolean(false);

        adapter.runNow(location, () -> ran.set(true));

        assertThat(ran).isTrue();
        verifyNoInteractions(regionScheduler);
    }

    @Test
    @DisplayName("runNow hands off to the entity scheduler when it belongs to another thread")
    void run_now_entity_hands_off_when_foreign() {
        Entity entity = mock(Entity.class);
        EntityScheduler entityScheduler = mock(EntityScheduler.class);
        when(entity.getScheduler()).thenReturn(entityScheduler);
        bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(entity)).thenReturn(false);
        AtomicBoolean ran = new AtomicBoolean(false);

        adapter.runNow(entity, () -> ran.set(true));

        assertThat(ran).isFalse();
        verify(entityScheduler).execute(eq(plugin), any(), isNull(), anyLong());
    }

    @Test
    @DisplayName("cancelAllTasks covers the global and async schedulers")
    void cancel_all_tasks_covers_global_and_async() {
        adapter.cancelAllTasks();

        verify(globalScheduler).cancelTasks(plugin);
        verify(asyncScheduler).cancelTasks(plugin);
    }

    @Test
    @DisplayName("cancel and isCancelled are forwarded to the wrapped task")
    void task_wrapper_forwards_cancellation() {
        when(globalScheduler.run(eq(plugin), any())).thenReturn(scheduledTask);
        when(scheduledTask.isCancelled()).thenReturn(true);

        Task task = adapter.runTask(() -> {
        });
        task.cancel();

        verify(scheduledTask).cancel();
        assertThat(task.isCancelled()).isTrue();
    }
}
