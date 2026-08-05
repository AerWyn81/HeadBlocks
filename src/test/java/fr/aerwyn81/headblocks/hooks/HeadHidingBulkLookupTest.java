package fr.aerwyn81.headblocks.hooks;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.ConfigService;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.utils.runnables.BukkitFutureResult;
import fr.aerwyn81.headblocks.utils.scheduler.SchedulerAdapter;
import fr.aerwyn81.headblocks.utils.scheduler.Task;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HeadHidingBulkLookupTest {

    private MockedStatic<HeadBlocks> headBlocks;
    private MockedStatic<Bukkit> bukkit;
    private ServiceRegistry registry;
    private HeadService headService;
    private StorageService storageService;
    private ConfigService configService;
    private SchedulerAdapter scheduler;
    private Player player;
    private World world;
    private HeadHidingPacketListener listener;

    private final List<HeadLocation> allHeads = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() {
        registry = mock(ServiceRegistry.class);
        headService = mock(HeadService.class);
        storageService = mock(StorageService.class);
        configService = mock(ConfigService.class);
        scheduler = mock(SchedulerAdapter.class);
        player = mock(Player.class);
        world = mock(World.class);

        lenient().when(registry.getHeadService()).thenReturn(headService);
        lenient().when(registry.getStorageService()).thenReturn(storageService);
        lenient().when(registry.getConfigService()).thenReturn(configService);
        lenient().when(configService.isHideFoundHeads()).thenReturn(true);
        lenient().when(headService.getHeadLocations()).thenReturn(allHeads);
        lenient().when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        lenient().when(player.getWorld()).thenReturn(world);

        headBlocks = mockStatic(HeadBlocks.class);
        headBlocks.when(HeadBlocks::getScheduler).thenReturn(scheduler);

        bukkit = mockStatic(Bukkit.class);
        bukkit.when(() -> Bukkit.createBlockData(any(Material.class))).thenReturn(mock(BlockData.class));

        lenient().when(scheduler.runTaskLater(nullable(Player.class), any(Runnable.class), anyLong()))
                .thenAnswer(invocation -> {
                    ((Runnable) invocation.getArgument(1)).run();
                    return Task.NONE;
                });

        listener = new HeadHidingPacketListener(registry);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
        headBlocks.close();
    }

    private HeadLocation head(UUID uuid, World headWorld) {
        HeadLocation headLocation = mock(HeadLocation.class);
        lenient().when(headLocation.getUuid()).thenReturn(uuid);

        if (headWorld == null) {
            lenient().when(headLocation.getLocation()).thenReturn(null);
        } else {
            Location location = mock(Location.class);
            lenient().when(location.getWorld()).thenReturn(headWorld);
            lenient().when(headLocation.getLocation()).thenReturn(location);
            lenient().when(headLocation.getLocation().getBlockX()).thenReturn(0);
            lenient().when(headLocation.getLocation().getBlockZ()).thenReturn(0);
        }

        return headLocation;
    }

    @SuppressWarnings("unchecked")
    private void completeJoinWith(Set<UUID> foundHeads) {
        BukkitFutureResult<Set<UUID>> future = mock(BukkitFutureResult.class);
        when(storageService.getHeadsPlayer(any(UUID.class))).thenReturn(future);
        doAnswer(invocation -> {
            ((java.util.function.Consumer<Set<UUID>>) invocation.getArgument(1)).accept(foundHeads);
            return null;
        }).when(future).whenComplete(nullable(org.bukkit.entity.Entity.class), any());

        listener.onPlayerJoin(player);
    }

    @Test
    @DisplayName("only the player's found heads are hidden, not every head on the server")
    void resolves_only_the_found_heads() {
        UUID found = UUID.randomUUID();
        UUID otherPlayersHead = UUID.randomUUID();
        allHeads.add(head(found, world));
        allHeads.add(head(otherPlayersHead, world));

        completeJoinWith(new LinkedHashSet<>(List.of(found)));

        verify(player, times(1)).sendBlockChange(any(Location.class), any());
    }

    @Test
    @DisplayName("a found head whose world is unloaded is skipped instead of throwing")
    void skips_heads_with_no_location() {
        UUID unloaded = UUID.randomUUID();
        UUID loaded = UUID.randomUUID();
        allHeads.add(head(unloaded, null));
        allHeads.add(head(loaded, world));

        completeJoinWith(new LinkedHashSet<>(List.of(unloaded, loaded)));

        verify(player, times(1)).sendBlockChange(any(Location.class), any());
    }

    @Test
    @DisplayName("a found head that no longer exists on the server is ignored")
    void ignores_found_heads_that_are_gone() {
        allHeads.add(head(UUID.randomUUID(), world));

        completeJoinWith(new LinkedHashSet<>(List.of(UUID.randomUUID())));

        verify(player, never()).sendBlockChange(any(Location.class), any());
    }

    @Test
    @DisplayName("a head in another world is tracked for chunks but no packet is sent")
    void does_not_send_for_heads_in_another_world() {
        UUID elsewhere = UUID.randomUUID();
        allHeads.add(head(elsewhere, mock(World.class)));

        completeJoinWith(new LinkedHashSet<>(List.of(elsewhere)));

        verify(player, never()).sendBlockChange(any(Location.class), any());
    }

    @Test
    @DisplayName("the head list is scanned once per bulk loop, not once per found head")
    void scans_the_head_list_a_bounded_number_of_times() {
        Set<UUID> foundHeads = new LinkedHashSet<>();
        for (int i = 0; i < 25; i++) {
            UUID uuid = UUID.randomUUID();
            allHeads.add(head(uuid, world));
            foundHeads.add(uuid);
        }

        completeJoinWith(foundHeads);

        // Two loops (chunk map, then the delayed packet send) => two passes, not 25 x 2 lookups.
        verify(headService, times(2)).getHeadLocations();
        verify(player, times(25)).sendBlockChange(any(Location.class), any());
    }

    @Test
    @DisplayName("showAllPreviousHeads resolves the head list in a single pass")
    void show_all_previous_heads_scans_once() {
        Set<UUID> foundHeads = new LinkedHashSet<>();
        for (int i = 0; i < 10; i++) {
            UUID uuid = UUID.randomUUID();
            allHeads.add(head(uuid, world));
            foundHeads.add(uuid);
        }

        completeJoinWith(foundHeads);
        clearInvocations(headService);

        listener.showAllPreviousHeads(player);

        verify(headService, times(1)).getHeadLocations();
    }

    @Test
    @DisplayName("an empty found-head set touches neither the head list nor the player")
    void empty_found_heads_is_a_no_op() {
        allHeads.add(head(UUID.randomUUID(), world));

        completeJoinWith(new LinkedHashSet<>(new ArrayList<>()));

        verify(player, never()).sendBlockChange(any(Location.class), any());
    }
}
