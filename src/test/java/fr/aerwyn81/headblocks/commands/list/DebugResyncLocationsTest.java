package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.utils.scheduler.SchedulerAdapter;
import fr.aerwyn81.headblocks.utils.scheduler.Task;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DebugResyncLocationsTest {

    private MockedStatic<HeadBlocks> headBlocks;
    private ServiceRegistry registry;
    private HeadService headService;
    private LanguageService languageService;
    private StorageService storageService;
    private SchedulerAdapter scheduler;
    private CommandSender sender;
    private Debug command;

    private final List<Runnable> scheduled = new ArrayList<>();

    @BeforeEach
    void setUp() {
        registry = mock(ServiceRegistry.class);
        headService = mock(HeadService.class);
        languageService = mock(LanguageService.class);
        storageService = mock(StorageService.class);
        scheduler = mock(SchedulerAdapter.class);
        sender = mock(CommandSender.class);

        when(registry.getHeadService()).thenReturn(headService);
        when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(registry.getStorageService()).thenReturn(storageService);
        lenient().when(languageService.message(anyString())).thenAnswer(i -> i.getArgument(0));

        headBlocks = mockStatic(HeadBlocks.class);
        headBlocks.when(HeadBlocks::getScheduler).thenReturn(scheduler);

        lenient().when(scheduler.runTask(nullable(Location.class), any(Runnable.class))).thenAnswer(invocation -> {
            scheduled.add(invocation.getArgument(1));
            return Task.NONE;
        });

        command = new Debug(registry);
    }

    @AfterEach
    void tearDown() {
        headBlocks.close();
    }

    /**
     * Reports one element more than it yields, reproducing what the live head list does when a head
     * is removed between the size read and the iteration.
     */
    private static final class DriftingList extends AbstractList<HeadLocation> {
        private final List<HeadLocation> delegate;

        private DriftingList(List<HeadLocation> delegate) {
            this.delegate = delegate;
        }

        @Override
        public HeadLocation get(int index) {
            return delegate.get(index);
        }

        @Override
        public int size() {
            return delegate.size() + 1;
        }

        @Override
        public Iterator<HeadLocation> iterator() {
            return delegate.iterator();
        }
    }

    private HeadLocation headAt(World world) {
        HeadLocation head = mock(HeadLocation.class);
        Location location = mock(Location.class);
        lenient().when(location.getWorld()).thenReturn(world);
        lenient().when(head.getLocation()).thenReturn(location);
        lenient().when(head.getUuid()).thenReturn(UUID.randomUUID());
        return head;
    }

    private void drain() {
        for (Runnable task : new ArrayList<>(scheduled)) {
            task.run();
        }
    }

    @Test
    @DisplayName("the completion counter follows the heads actually scheduled, not a stale size")
    void counter_matches_the_scheduled_heads() throws Exception {
        World world = mock(World.class);
        DriftingList drifting = new DriftingList(List.of(headAt(world), headAt(world)));

        when(headService.getHeadLocations()).thenReturn(drifting);
        when(storageService.getHeadTexture(any(UUID.class))).thenReturn(null);

        command.perform(sender, new String[]{"debug", "resync", "locations"});
        drain();

        assertThat(scheduled).hasSize(2);
        verify(languageService, times(1)).message("Messages.ResyncLocationsSuccess");
    }

    @Test
    @DisplayName("mutating the live list after the snapshot neither duplicates nor loses the report")
    void mutating_the_live_list_does_not_disturb_the_report() throws Exception {
        World world = mock(World.class);
        List<HeadLocation> live = new CopyOnWriteArrayList<>(List.of(headAt(world), headAt(world)));
        when(headService.getHeadLocations()).thenReturn(live);
        when(storageService.getHeadTexture(any(UUID.class))).thenAnswer(invocation -> {
            live.add(headAt(world));
            live.remove(0);
            return null;
        });

        command.perform(sender, new String[]{"debug", "resync", "locations"});
        drain();

        assertThat(scheduled).hasSize(2);
        verify(languageService, times(1)).message("Messages.ResyncLocationsSuccess");
    }

    @Test
    @DisplayName("heads whose world is unloaded are counted without being scheduled")
    void unloaded_heads_are_counted_and_reported() {
        HeadLocation unloaded = mock(HeadLocation.class);
        when(unloaded.getLocation()).thenReturn(null);
        when(headService.getHeadLocations()).thenReturn(new CopyOnWriteArrayList<>(List.of(unloaded)));

        command.perform(sender, new String[]{"debug", "resync", "locations"});

        assertThat(scheduled).isEmpty();
        verify(scheduler, never()).runTask(nullable(Location.class), any(Runnable.class));
        verify(languageService).message("Messages.ResyncLocationsSuccess");
    }

    @Test
    @DisplayName("an empty head list reports the empty message and schedules nothing")
    void empty_list_reports_empty() {
        when(headService.getHeadLocations()).thenReturn(new CopyOnWriteArrayList<>());

        command.perform(sender, new String[]{"debug", "resync", "locations"});

        assertThat(scheduled).isEmpty();
        verify(languageService).message("Messages.ListHeadEmpty");
        verify(languageService, never()).message("Messages.ResyncLocationsSuccess");
    }
}
