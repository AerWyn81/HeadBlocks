package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.HeadMove;
import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.bukkit.PluginProvider;
import fr.aerwyn81.headblocks.utils.bukkit.SchedulerAdapter;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.InternalUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeadServiceTest {

    @Mock
    private ConfigService configService;

    @Mock
    private StorageService storageService;

    @Mock
    private LanguageService languageService;

    @Mock
    private SchedulerAdapter scheduler;

    @Mock
    private PluginProvider pluginProvider;

    @Mock
    private HologramService hologramService;

    @Mock
    private HuntService huntService;

    @Mock
    private HuntConfigService huntConfigService;

    private HeadService headService;

    @BeforeEach
    void setUp() throws Exception {
        headService = new HeadService(configService, storageService, languageService, scheduler, pluginProvider);
        headService.setHologramService(hologramService);
        headService.setHuntService(huntService);
        headService.setHuntConfigService(huntConfigService);

        setField("headLocations", new ArrayList<HeadLocation>());
        setField("headMoves", new HashMap<UUID, HeadMove>());
        setField("tasksHeadSpin", new HashMap<UUID, Integer>());

        // saveConfig uses runTaskLater + runTaskAsync — execute immediately in tests
        lenient().doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(scheduler).runTaskLater(any(Runnable.class), anyLong());

        lenient().doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(scheduler).runTaskAsync(any(Runnable.class));
    }

    // --- Helpers ---

    private void setField(String name, Object value) throws Exception {
        Field field = HeadService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(headService, value);
    }

    @SuppressWarnings("unchecked")
    private ArrayList<HeadLocation> headLocations() throws Exception {
        Field field = HeadService.class.getDeclaredField("headLocations");
        field.setAccessible(true);
        return (ArrayList<HeadLocation>) field.get(headService);
    }

    @SuppressWarnings("unchecked")
    private HashMap<UUID, HeadMove> headMoves() throws Exception {
        Field field = HeadService.class.getDeclaredField("headMoves");
        field.setAccessible(true);
        return (HashMap<UUID, HeadMove>) field.get(headService);
    }

    @SuppressWarnings("unchecked")
    private HashMap<UUID, Integer> tasksHeadSpin() throws Exception {
        Field field = HeadService.class.getDeclaredField("tasksHeadSpin");
        field.setAccessible(true);
        return (HashMap<UUID, Integer>) field.get(headService);
    }

    private HeadLocation createHeadLocation(UUID uuid, String name, Location location, boolean charged) {
        var hl = mock(HeadLocation.class);
        lenient().when(hl.getUuid()).thenReturn(uuid);
        lenient().when(hl.getName()).thenReturn(name);
        lenient().when(hl.getRawNameOrUuid()).thenReturn(name.isEmpty() ? uuid.toString() : name);
        lenient().when(hl.getLocation()).thenReturn(location);
        lenient().when(hl.isCharged()).thenReturn(charged);
        return hl;
    }

    // =========================================================================
    // getHeadByUUID
    // =========================================================================

    @Nested
    class GetHeadByUUID {

        @Test
        void found_returns_matching_head() throws Exception {
            UUID uuid = UUID.randomUUID();
            HeadLocation hl = createHeadLocation(uuid, "Alpha", null, true);
            headLocations().add(hl);

            assertThat(headService.getHeadByUUID(uuid)).isSameAs(hl);
        }

        @Test
        void not_found_returns_null() throws Exception {
            UUID uuid = UUID.randomUUID();
            HeadLocation hl = createHeadLocation(UUID.randomUUID(), "Beta", null, true);
            headLocations().add(hl);

            assertThat(headService.getHeadByUUID(uuid)).isNull();
        }

        @Test
        void empty_list_returns_null() {
            assertThat(headService.getHeadByUUID(UUID.randomUUID())).isNull();
        }

        @Test
        void multiple_heads_returns_first_match() throws Exception {
            UUID uuid = UUID.randomUUID();
            HeadLocation hl1 = createHeadLocation(uuid, "First", null, true);
            HeadLocation hl2 = createHeadLocation(uuid, "Duplicate", null, true);
            headLocations().add(hl1);
            headLocations().add(hl2);

            assertThat(headService.getHeadByUUID(uuid)).isSameAs(hl1);
        }
    }

    // =========================================================================
    // getHeadByName
    // =========================================================================

    @Nested
    class GetHeadByName {

        @Test
        void found_returns_matching_head() throws Exception {
            UUID uuid = UUID.randomUUID();
            HeadLocation hl = createHeadLocation(uuid, "MyHead", null, true);
            headLocations().add(hl);

            assertThat(headService.getHeadByName("MyHead")).isSameAs(hl);
        }

        @Test
        void not_found_returns_null() throws Exception {
            HeadLocation hl = createHeadLocation(UUID.randomUUID(), "Existing", null, true);
            headLocations().add(hl);

            assertThat(headService.getHeadByName("NonExistent")).isNull();
        }

        @Test
        void uses_rawNameOrUuid_for_unnamed_head() throws Exception {
            UUID uuid = UUID.randomUUID();
            HeadLocation hl = createHeadLocation(uuid, "", null, true);
            headLocations().add(hl);

            assertThat(headService.getHeadByName(uuid.toString())).isSameAs(hl);
        }

        @Test
        void empty_list_returns_null() {
            assertThat(headService.getHeadByName("Any")).isNull();
        }

        @Test
        void case_sensitive_match() throws Exception {
            HeadLocation hl = createHeadLocation(UUID.randomUUID(), "CaseSensitive", null, true);
            headLocations().add(hl);

            assertThat(headService.getHeadByName("casesensitive")).isNull();
        }
    }

    // =========================================================================
    // getHeadAt
    // =========================================================================

    @Nested
    class GetHeadAt {

        @Test
        void found_returns_matching_head() throws Exception {
            Location loc = mock(Location.class);
            HeadLocation hl = createHeadLocation(UUID.randomUUID(), "At", loc, true);
            headLocations().add(hl);

            try (MockedStatic<LocationUtils> mocked = mockStatic(LocationUtils.class)) {
                mocked.when(() -> LocationUtils.areEquals(loc, loc)).thenReturn(true);

                assertThat(headService.getHeadAt(loc)).isSameAs(hl);
            }
        }

        @Test
        void not_found_returns_null() throws Exception {
            Location loc1 = mock(Location.class);
            Location loc2 = mock(Location.class);
            HeadLocation hl = createHeadLocation(UUID.randomUUID(), "Far", loc1, true);
            headLocations().add(hl);

            try (MockedStatic<LocationUtils> mocked = mockStatic(LocationUtils.class)) {
                mocked.when(() -> LocationUtils.areEquals(loc1, loc2)).thenReturn(false);

                assertThat(headService.getHeadAt(loc2)).isNull();
            }
        }

        @Test
        void empty_list_returns_null() {
            try (MockedStatic<LocationUtils> ignored = mockStatic(LocationUtils.class)) {
                assertThat(headService.getHeadAt(mock(Location.class))).isNull();
            }
        }

        @Test
        void multiple_heads_returns_first_matching() throws Exception {
            Location loc = mock(Location.class);
            Location otherLoc = mock(Location.class);
            HeadLocation hl1 = createHeadLocation(UUID.randomUUID(), "First", loc, true);
            HeadLocation hl2 = createHeadLocation(UUID.randomUUID(), "Second", otherLoc, true);
            HeadLocation hl3 = createHeadLocation(UUID.randomUUID(), "Third", loc, true);
            headLocations().add(hl1);
            headLocations().add(hl2);
            headLocations().add(hl3);

            try (MockedStatic<LocationUtils> mocked = mockStatic(LocationUtils.class)) {
                mocked.when(() -> LocationUtils.areEquals(loc, loc)).thenReturn(true);
                mocked.when(() -> LocationUtils.areEquals(otherLoc, loc)).thenReturn(false);

                assertThat(headService.getHeadAt(loc)).isSameAs(hl1);
            }
        }
    }

    // =========================================================================
    // resolveHeadIdentifier
    // =========================================================================

    @Nested
    class ResolveHeadIdentifier {

        @Test
        void valid_uuid_returns_head_by_uuid() throws Exception {
            UUID uuid = UUID.randomUUID();
            HeadLocation hl = createHeadLocation(uuid, "Resolved", null, true);
            headLocations().add(hl);

            assertThat(headService.resolveHeadIdentifier(uuid.toString())).isSameAs(hl);
        }

        @Test
        void valid_uuid_not_in_list_returns_null() {
            UUID uuid = UUID.randomUUID();

            assertThat(headService.resolveHeadIdentifier(uuid.toString())).isNull();
        }

        @Test
        void invalid_uuid_falls_back_to_name() throws Exception {
            HeadLocation hl = createHeadLocation(UUID.randomUUID(), "ByName", null, true);
            headLocations().add(hl);

            assertThat(headService.resolveHeadIdentifier("ByName")).isSameAs(hl);
        }

        @Test
        void invalid_uuid_no_name_match_returns_null() {
            assertThat(headService.resolveHeadIdentifier("NoMatch")).isNull();
        }

        @Test
        void empty_string_falls_back_to_name_lookup() throws Exception {
            HeadLocation hl = createHeadLocation(UUID.randomUUID(), "", null, true);
            headLocations().add(hl);

            // Empty string is not a valid UUID, so it falls back to name lookup
            // An unnamed head has getRawNameOrUuid returning the uuid string, not ""
            assertThat(headService.resolveHeadIdentifier("")).isNull();
        }

        @Test
        void uuid_format_but_unknown_returns_null_not_name_fallback() throws Exception {
            // A valid UUID format goes through getHeadByUUID, not getHeadByName
            UUID uuid = UUID.randomUUID();
            // Create a head whose name happens to be a UUID string
            HeadLocation hl = createHeadLocation(UUID.randomUUID(), uuid.toString(), null, true);
            headLocations().add(hl);

            // Since the identifier is a valid UUID, it tries getHeadByUUID first.
            // That UUID doesn't match the head's actual UUID, so null is returned.
            assertThat(headService.resolveHeadIdentifier(uuid.toString())).isNull();
        }
    }

    // =========================================================================
    // getHeadRawNameOrUuid
    // =========================================================================

    @Nested
    class GetHeadRawNameOrUuid {

        @Test
        void maps_names_correctly() throws Exception {
            UUID uuid1 = UUID.randomUUID();
            HeadLocation named = createHeadLocation(uuid1, "Named", null, true);
            UUID uuid2 = UUID.randomUUID();
            HeadLocation unnamed = createHeadLocation(uuid2, "", null, true);

            headLocations().add(named);
            headLocations().add(unnamed);

            ArrayList<String> result = headService.getHeadRawNameOrUuid();

            assertThat(result).containsExactly("Named", uuid2.toString());
        }

        @Test
        void empty_list_returns_empty() {
            assertThat(headService.getHeadRawNameOrUuid()).isEmpty();
        }

        @Test
        void returns_arraylist_type() throws Exception {
            HeadLocation hl = createHeadLocation(UUID.randomUUID(), "Test", null, true);
            headLocations().add(hl);

            assertThat(headService.getHeadRawNameOrUuid()).isInstanceOf(ArrayList.class);
        }

        @Test
        void preserves_order() throws Exception {
            HeadLocation hl1 = createHeadLocation(UUID.randomUUID(), "Zebra", null, true);
            HeadLocation hl2 = createHeadLocation(UUID.randomUUID(), "Apple", null, true);
            HeadLocation hl3 = createHeadLocation(UUID.randomUUID(), "Mango", null, true);

            headLocations().add(hl1);
            headLocations().add(hl2);
            headLocations().add(hl3);

            ArrayList<String> result = headService.getHeadRawNameOrUuid();

            assertThat(result).containsExactly("Zebra", "Apple", "Mango");
        }
    }

    // =========================================================================
    // getChargedHeadLocations
    // =========================================================================

    @Nested
    class GetChargedHeadLocations {

        @Test
        void returns_only_charged_heads() throws Exception {
            HeadLocation charged1 = createHeadLocation(UUID.randomUUID(), "C1", null, true);
            HeadLocation uncharged = createHeadLocation(UUID.randomUUID(), "U1", null, false);
            HeadLocation charged2 = createHeadLocation(UUID.randomUUID(), "C2", null, true);

            headLocations().add(charged1);
            headLocations().add(uncharged);
            headLocations().add(charged2);

            ArrayList<HeadLocation> result = headService.getChargedHeadLocations();

            assertThat(result).containsExactly(charged1, charged2);
        }

        @Test
        void empty_when_none_charged() throws Exception {
            HeadLocation uncharged = createHeadLocation(UUID.randomUUID(), "U", null, false);
            headLocations().add(uncharged);

            assertThat(headService.getChargedHeadLocations()).isEmpty();
        }

        @Test
        void empty_list_returns_empty() {
            assertThat(headService.getChargedHeadLocations()).isEmpty();
        }

        @Test
        void returns_arraylist_type() throws Exception {
            HeadLocation hl = createHeadLocation(UUID.randomUUID(), "C", null, true);
            headLocations().add(hl);

            assertThat(headService.getChargedHeadLocations()).isInstanceOf(ArrayList.class);
        }

        @Test
        void all_charged_returns_all() throws Exception {
            HeadLocation c1 = createHeadLocation(UUID.randomUUID(), "C1", null, true);
            HeadLocation c2 = createHeadLocation(UUID.randomUUID(), "C2", null, true);
            headLocations().add(c1);
            headLocations().add(c2);

            assertThat(headService.getChargedHeadLocations()).containsExactly(c1, c2);
        }
    }

    // =========================================================================
    // getHeadLocationsForHunt
    // =========================================================================

    @Nested
    class GetHeadLocationsForHBHunt {

        @Test
        void returns_heads_belonging_to_hunt() throws Exception {
            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();
            UUID uuid3 = UUID.randomUUID();

            HeadLocation hl1 = createHeadLocation(uuid1, "H1", null, true);
            lenient().when(hl1.getHuntId()).thenReturn("hunt1");
            HeadLocation hl2 = createHeadLocation(uuid2, "H2", null, true);
            lenient().when(hl2.getHuntId()).thenReturn("other");
            HeadLocation hl3 = createHeadLocation(uuid3, "H3", null, true);
            lenient().when(hl3.getHuntId()).thenReturn("hunt1");

            headLocations().add(hl1);
            headLocations().add(hl2);
            headLocations().add(hl3);

            HBHunt hunt = new HBHunt(configService, "hunt1", "Test Hunt", HuntState.ACTIVE, 1, "D");

            ArrayList<HeadLocation> result = headService.getHeadLocationsForHunt(hunt);

            assertThat(result).containsExactly(hl1, hl3);
        }

        @Test
        void empty_hunt_returns_empty() throws Exception {
            HeadLocation hl = createHeadLocation(UUID.randomUUID(), "H", null, true);
            lenient().when(hl.getHuntId()).thenReturn("other");
            headLocations().add(hl);

            HBHunt hunt = new HBHunt(configService, "empty", "Empty", HuntState.ACTIVE, 1, "D");

            assertThat(headService.getHeadLocationsForHunt(hunt)).isEmpty();
        }

        @Test
        void empty_headLocations_returns_empty() {
            HBHunt hunt = new HBHunt(configService, "h1", "Test", HuntState.ACTIVE, 1, "D");

            assertThat(headService.getHeadLocationsForHunt(hunt)).isEmpty();
        }

        @Test
        void returns_arraylist_type() throws Exception {
            HBHunt hunt = new HBHunt(configService, "h1", "Test", HuntState.ACTIVE, 1, "D");

            assertThat(headService.getHeadLocationsForHunt(hunt)).isInstanceOf(ArrayList.class);
        }
    }

    // =========================================================================
    // getHeadMoves / clearHeadMoves
    // =========================================================================

    @Nested
    class HeadMovesTests {

        @Test
        void getHeadMoves_returns_map() throws Exception {
            UUID uuid = UUID.randomUUID();
            Location loc = mock(Location.class);
            HeadMove move = new HeadMove(uuid, loc);
            headMoves().put(uuid, move);

            HashMap<UUID, HeadMove> result = headService.getHeadMoves();

            assertThat(result).hasSize(1);
            assertThat(result.get(uuid)).isEqualTo(move);
        }

        @Test
        void getHeadMoves_empty_returns_empty_map() {
            assertThat(headService.getHeadMoves()).isEmpty();
        }

        @Test
        void clearHeadMoves_empties_the_map() throws Exception {
            UUID uuid = UUID.randomUUID();
            headMoves().put(uuid, new HeadMove(uuid, mock(Location.class)));

            headService.clearHeadMoves();

            assertThat(headService.getHeadMoves()).isEmpty();
        }

        @Test
        void clearHeadMoves_on_empty_map_does_not_throw() {
            headService.clearHeadMoves(); // should not throw
            assertThat(headService.getHeadMoves()).isEmpty();
        }

        @Test
        void clearHeadMoves_with_null_map_does_not_throw() throws Exception {
            setField("headMoves", null);

            headService.clearHeadMoves(); // should not throw due to null guard
        }

        @Test
        void getHeadMoves_returns_same_reference() throws Exception {
            HashMap<UUID, HeadMove> map = headMoves();

            assertThat(headService.getHeadMoves()).isSameAs(map);
        }

        @Test
        void multiple_moves_are_tracked() throws Exception {
            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();
            Location loc1 = mock(Location.class);
            Location loc2 = mock(Location.class);
            headMoves().put(uuid1, new HeadMove(uuid1, loc1));
            headMoves().put(uuid2, new HeadMove(uuid2, loc2));

            HashMap<UUID, HeadMove> result = headService.getHeadMoves();

            assertThat(result).hasSize(2);
            assertThat(result).containsKey(uuid1);
            assertThat(result).containsKey(uuid2);
        }

        @Test
        void clearHeadMoves_with_multiple_entries_empties_all() throws Exception {
            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();
            headMoves().put(uuid1, new HeadMove(uuid1, mock(Location.class)));
            headMoves().put(uuid2, new HeadMove(uuid2, mock(Location.class)));

            headService.clearHeadMoves();

            assertThat(headService.getHeadMoves()).isEmpty();
        }
    }

    // =========================================================================
    // getHeadLocations
    // =========================================================================

    @Nested
    class GetHeadLocations {

        @Test
        void returns_all_heads() throws Exception {
            HeadLocation hl1 = createHeadLocation(UUID.randomUUID(), "A", null, true);
            HeadLocation hl2 = createHeadLocation(UUID.randomUUID(), "B", null, false);
            headLocations().add(hl1);
            headLocations().add(hl2);

            assertThat(headService.getHeadLocations()).containsExactly(hl1, hl2);
        }

        @Test
        void empty_returns_empty_list() {
            assertThat(headService.getHeadLocations()).isEmpty();
        }

        @Test
        void returns_same_reference() throws Exception {
            ArrayList<HeadLocation> list = headLocations();

            assertThat(headService.getHeadLocations()).isSameAs(list);
        }
    }

    // =========================================================================
    // saveHeadInConfig
    // =========================================================================

    @Nested
    class SaveHeadInConfig {

        @Test
        void delegates_to_huntConfigService_saveLocationInHunt() {
            HeadLocation hl = mock(HeadLocation.class);
            when(hl.getHuntId()).thenReturn("default");

            headService.saveHeadInConfig(hl);

            verify(huntConfigService).saveLocationInHunt("default", hl);
        }

        @Test
        void passes_correct_huntId_from_headLocation() {
            HeadLocation hl = mock(HeadLocation.class);
            when(hl.getHuntId()).thenReturn("custom-hunt");

            headService.saveHeadInConfig(hl);

            verify(huntConfigService).saveLocationInHunt("custom-hunt", hl);
        }
    }

    // =========================================================================
    // removeHeadLocation
    // =========================================================================

    @Nested
    class RemoveHeadLocation {

        @Test
        void removes_head_from_storage_and_list() throws Exception {
            UUID uuid = UUID.randomUUID();
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            when(loc.getBlock()).thenReturn(block);

            HeadLocation hl = createHeadLocation(uuid, "ToRemove", loc, true);
            lenient().when(hl.getHuntId()).thenReturn("default");
            headLocations().add(hl);

            lenient().when(configService.hologramsEnabled()).thenReturn(false);

            headService.removeHeadLocation(hl, true);

            verify(storageService).removeHead(uuid, true);
            verify(block).setType(Material.AIR);
            assertThat(headLocations()).doesNotContain(hl);
        }

        @Test
        void removes_head_from_hunt_and_config() throws Exception {
            UUID uuid = UUID.randomUUID();
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            when(loc.getBlock()).thenReturn(block);

            HeadLocation hl = createHeadLocation(uuid, "HuntHead", loc, true);
            when(hl.getHuntId()).thenReturn("hunt1");
            headLocations().add(hl);

            HBHunt hunt = mock(HBHunt.class);
            lenient().when(configService.hologramsEnabled()).thenReturn(false);
            when(huntService.getHuntById("hunt1")).thenReturn(hunt);

            headService.removeHeadLocation(hl, false);

            verify(hunt).removeHead(uuid);
            verify(huntConfigService).removeLocationFromHunt("hunt1", uuid);
        }

        @Test
        void removes_hologram_when_holograms_enabled() throws Exception {
            UUID uuid = UUID.randomUUID();
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            when(loc.getBlock()).thenReturn(block);

            HeadLocation hl = createHeadLocation(uuid, "HoloHead", loc, true);
            lenient().when(hl.getHuntId()).thenReturn("default");
            headLocations().add(hl);

            when(configService.hologramsEnabled()).thenReturn(true);

            headService.removeHeadLocation(hl, true);

            verify(hologramService).removeHolograms(loc);
        }

        @Test
        void does_not_remove_hologram_when_holograms_disabled() throws Exception {
            UUID uuid = UUID.randomUUID();
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            when(loc.getBlock()).thenReturn(block);

            HeadLocation hl = createHeadLocation(uuid, "NoHolo", loc, true);
            lenient().when(hl.getHuntId()).thenReturn("default");
            headLocations().add(hl);

            when(configService.hologramsEnabled()).thenReturn(false);

            headService.removeHeadLocation(hl, true);

            verify(hologramService, never()).removeHolograms(any());
        }

        @Test
        void cancels_spin_task_if_exists() throws Exception {
            UUID uuid = UUID.randomUUID();
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            when(loc.getBlock()).thenReturn(block);

            HeadLocation hl = createHeadLocation(uuid, "SpinHead", loc, true);
            lenient().when(hl.getHuntId()).thenReturn("default");
            headLocations().add(hl);
            tasksHeadSpin().put(uuid, 42);

            lenient().when(configService.hologramsEnabled()).thenReturn(false);

            headService.removeHeadLocation(hl, true);

            verify(scheduler).cancelTask(42);
            assertThat(tasksHeadSpin()).doesNotContainKey(uuid);
        }

        @Test
        void no_spin_task_does_not_call_cancel() throws Exception {
            UUID uuid = UUID.randomUUID();
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            when(loc.getBlock()).thenReturn(block);

            HeadLocation hl = createHeadLocation(uuid, "NoSpin", loc, true);
            lenient().when(hl.getHuntId()).thenReturn("default");
            headLocations().add(hl);

            lenient().when(configService.hologramsEnabled()).thenReturn(false);

            headService.removeHeadLocation(hl, true);

            verify(scheduler, never()).cancelTask(anyInt());
        }

        @Test
        void removes_associated_headMoves() throws Exception {
            UUID uuid = UUID.randomUUID();
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            when(loc.getBlock()).thenReturn(block);

            HeadLocation hl = createHeadLocation(uuid, "MovedHead", loc, true);
            lenient().when(hl.getHuntId()).thenReturn("default");
            headLocations().add(hl);
            headMoves().put(uuid, new HeadMove(uuid, mock(Location.class)));

            // Also add another move that should NOT be removed
            UUID otherUuid = UUID.randomUUID();
            headMoves().put(otherUuid, new HeadMove(otherUuid, mock(Location.class)));

            lenient().when(configService.hologramsEnabled()).thenReturn(false);

            headService.removeHeadLocation(hl, true);

            assertThat(headMoves()).doesNotContainKey(uuid);
            assertThat(headMoves()).containsKey(otherUuid);
        }

        @Test
        void removes_from_hunt_config() throws Exception {
            UUID uuid = UUID.randomUUID();
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            when(loc.getBlock()).thenReturn(block);

            HeadLocation hl = createHeadLocation(uuid, "ConfigHead", loc, true);
            when(hl.getHuntId()).thenReturn("default");
            headLocations().add(hl);

            lenient().when(configService.hologramsEnabled()).thenReturn(false);

            headService.removeHeadLocation(hl, true);

            verify(huntConfigService).removeLocationFromHunt("default", uuid);
        }

        @Test
        void null_headLocation_does_nothing() throws Exception {
            headService.removeHeadLocation(null, true);

            verify(storageService, never()).removeHead(any(), anyBoolean());
        }

        @Test
        void withDelete_false_passes_false_to_storage() throws Exception {
            UUID uuid = UUID.randomUUID();
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            when(loc.getBlock()).thenReturn(block);

            HeadLocation hl = createHeadLocation(uuid, "NoDelete", loc, true);
            lenient().when(hl.getHuntId()).thenReturn("default");
            headLocations().add(hl);

            lenient().when(configService.hologramsEnabled()).thenReturn(false);

            headService.removeHeadLocation(hl, false);

            verify(storageService).removeHead(uuid, false);
        }

        @Test
        void storage_exception_propagates() throws Exception {
            UUID uuid = UUID.randomUUID();
            Location loc = mock(Location.class);

            HeadLocation hl = createHeadLocation(uuid, "ErrHead", loc, true);
            headLocations().add(hl);

            doThrow(new InternalException("storage error")).when(storageService).removeHead(uuid, true);

            assertThatThrownBy(() -> headService.removeHeadLocation(hl, true))
                    .isInstanceOf(InternalException.class)
                    .hasMessageContaining("storage error");
        }
    }

    // =========================================================================
    // saveHeadLocation
    // =========================================================================

    @Nested
    class SaveHeadLocation {

        @Test
        void creates_head_in_storage() throws Exception {
            Location loc = mock(Location.class);

            lenient().when(configService.hologramsEnabled()).thenReturn(false);
            lenient().when(configService.spinEnabled()).thenReturn(false);

            try (MockedStatic<InternalUtils> mocked = mockStatic(InternalUtils.class)) {
                UUID generatedUuid = UUID.randomUUID();
                mocked.when(() -> InternalUtils.generateNewUUID(anyList())).thenReturn(generatedUuid);

                UUID result = headService.saveHeadLocation(loc, "texture123", "default");

                assertThat(result).isEqualTo(generatedUuid);
                verify(storageService).createOrUpdateHead(generatedUuid, "texture123");
            }
        }

        @Test
        void adds_head_to_headLocations_list() throws Exception {
            Location loc = mock(Location.class);

            lenient().when(configService.hologramsEnabled()).thenReturn(false);
            lenient().when(configService.spinEnabled()).thenReturn(false);

            try (MockedStatic<InternalUtils> mocked = mockStatic(InternalUtils.class)) {
                UUID generatedUuid = UUID.randomUUID();
                mocked.when(() -> InternalUtils.generateNewUUID(anyList())).thenReturn(generatedUuid);

                headService.saveHeadLocation(loc, "tex", "default");

                assertThat(headLocations()).hasSize(1);
                assertThat(headLocations().getFirst().getUuid()).isEqualTo(generatedUuid);
            }
        }

        @Test
        void creates_hologram_when_enabled() throws Exception {
            Location loc = mock(Location.class);

            when(configService.hologramsEnabled()).thenReturn(true);
            lenient().when(configService.spinEnabled()).thenReturn(false);

            try (MockedStatic<InternalUtils> mocked = mockStatic(InternalUtils.class)) {
                mocked.when(() -> InternalUtils.generateNewUUID(anyList())).thenReturn(UUID.randomUUID());

                headService.saveHeadLocation(loc, "tex", "default");

                verify(hologramService).createHolograms(loc);
            }
        }

        @Test
        void does_not_create_hologram_when_disabled() throws Exception {
            Location loc = mock(Location.class);

            when(configService.hologramsEnabled()).thenReturn(false);
            lenient().when(configService.spinEnabled()).thenReturn(false);

            try (MockedStatic<InternalUtils> mocked = mockStatic(InternalUtils.class)) {
                mocked.when(() -> InternalUtils.generateNewUUID(anyList())).thenReturn(UUID.randomUUID());

                headService.saveHeadLocation(loc, "tex", "default");

                verify(hologramService, never()).createHolograms(any(Location.class));
            }
        }

        @Test
        void does_not_create_hologram_when_hologramService_is_null() throws Exception {
            headService.setHologramService(null);

            Location loc = mock(Location.class);

            when(configService.hologramsEnabled()).thenReturn(true);
            lenient().when(configService.spinEnabled()).thenReturn(false);

            try (MockedStatic<InternalUtils> mocked = mockStatic(InternalUtils.class)) {
                mocked.when(() -> InternalUtils.generateNewUUID(anyList())).thenReturn(UUID.randomUUID());

                headService.saveHeadLocation(loc, "tex", "default");

                // hologramService is null so createHolograms is never called (no NPE)
            }
        }

        @Test
        void storage_exception_propagates() throws Exception {
            Location loc = mock(Location.class);

            try (MockedStatic<InternalUtils> mocked = mockStatic(InternalUtils.class)) {
                UUID generatedUuid = UUID.randomUUID();
                mocked.when(() -> InternalUtils.generateNewUUID(anyList())).thenReturn(generatedUuid);

                doThrow(new InternalException("db error")).when(storageService).createOrUpdateHead(generatedUuid, "tex");

                assertThatThrownBy(() -> headService.saveHeadLocation(loc, "tex", "default"))
                        .isInstanceOf(InternalException.class)
                        .hasMessageContaining("db error");
            }
        }

        @Test
        void returns_unique_uuid() throws Exception {
            Location loc = mock(Location.class);

            lenient().when(configService.hologramsEnabled()).thenReturn(false);
            lenient().when(configService.spinEnabled()).thenReturn(false);

            try (MockedStatic<InternalUtils> mocked = mockStatic(InternalUtils.class)) {
                UUID uuid1 = UUID.randomUUID();
                UUID uuid2 = UUID.randomUUID();
                mocked.when(() -> InternalUtils.generateNewUUID(anyList())).thenReturn(uuid1, uuid2);

                UUID result1 = headService.saveHeadLocation(loc, "t1", "default");
                UUID result2 = headService.saveHeadLocation(loc, "t2", "default");

                assertThat(result1).isNotEqualTo(result2);
            }
        }
    }

    // =========================================================================
    // saveAllHeadsInConfig
    // =========================================================================

    @Nested
    class SaveAllHeadsInConfig {

        @Test
        void saves_all_heads_via_huntConfigService() throws Exception {
            HeadLocation hl1 = mock(HeadLocation.class);
            when(hl1.getHuntId()).thenReturn("hunt1");
            HeadLocation hl2 = mock(HeadLocation.class);
            when(hl2.getHuntId()).thenReturn("hunt2");
            headLocations().add(hl1);
            headLocations().add(hl2);

            headService.saveAllHeadsInConfig();

            verify(huntConfigService).saveLocationInHunt("hunt1", hl1);
            verify(huntConfigService).saveLocationInHunt("hunt2", hl2);
        }

        @Test
        void empty_list_does_not_call_huntConfigService() {
            headService.saveAllHeadsInConfig();

            verify(huntConfigService, never()).saveLocationInHunt(any(), any());
        }
    }

    // =========================================================================
    // setHologramService / setHuntService (setter injection)
    // =========================================================================

    @Nested
    class SetterInjection {

        @Test
        void setHologramService_sets_value() throws Exception {
            HologramService newHologramService = mock(HologramService.class);
            headService.setHologramService(newHologramService);

            Field field = HeadService.class.getDeclaredField("hologramService");
            field.setAccessible(true);
            assertThat(field.get(headService)).isSameAs(newHologramService);
        }

        @Test
        void setHuntService_sets_value() throws Exception {
            HuntService newHuntService = mock(HuntService.class);
            headService.setHuntService(newHuntService);

            Field field = HeadService.class.getDeclaredField("huntService");
            field.setAccessible(true);
            assertThat(field.get(headService)).isSameAs(newHuntService);
        }

        @Test
        void setHologramService_allows_null() throws Exception {
            headService.setHologramService(null);

            Field field = HeadService.class.getDeclaredField("hologramService");
            field.setAccessible(true);
            assertThat(field.get(headService)).isNull();
        }

        @Test
        void setHuntService_allows_null() throws Exception {
            headService.setHuntService(null);

            Field field = HeadService.class.getDeclaredField("huntService");
            field.setAccessible(true);
            assertThat(field.get(headService)).isNull();
        }
    }

    // =========================================================================
    // HB_KEY static field
    // =========================================================================

    @Nested
    class StaticFields {

        @Test
        void hb_key_has_expected_value() {
            assertThat(HeadService.HB_KEY).isEqualTo("HB_HEAD");
        }
    }

    // =========================================================================
    // cancelAllSpinTasks (indirectly through removeHeadLocation)
    // =========================================================================

    @Nested
    class SpinTaskManagement {

        @Test
        void removing_head_with_spin_task_cancels_only_that_task() throws Exception {
            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            when(loc.getBlock()).thenReturn(block);

            HeadLocation hl1 = createHeadLocation(uuid1, "Spin1", loc, true);
            lenient().when(hl1.getHuntId()).thenReturn("default");
            headLocations().add(hl1);
            tasksHeadSpin().put(uuid1, 10);
            tasksHeadSpin().put(uuid2, 20);

            lenient().when(configService.hologramsEnabled()).thenReturn(false);

            headService.removeHeadLocation(hl1, true);

            verify(scheduler).cancelTask(10);
            verify(scheduler, never()).cancelTask(20);
            assertThat(tasksHeadSpin()).doesNotContainKey(uuid1);
            assertThat(tasksHeadSpin()).containsEntry(uuid2, 20);
        }
    }

    // =========================================================================
    // Edge cases and integration-like scenarios
    // =========================================================================

    @Nested
    class EdgeCases {

        @Test
        void getHeadByUUID_after_removal_returns_null() throws Exception {
            UUID uuid = UUID.randomUUID();
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            when(loc.getBlock()).thenReturn(block);

            HeadLocation hl = createHeadLocation(uuid, "Removed", loc, true);
            lenient().when(hl.getHuntId()).thenReturn("default");
            headLocations().add(hl);

            lenient().when(configService.hologramsEnabled()).thenReturn(false);

            headService.removeHeadLocation(hl, true);

            assertThat(headService.getHeadByUUID(uuid)).isNull();
        }

        @Test
        void getChargedHeadLocations_returns_new_list_not_same_reference() throws Exception {
            HeadLocation hl = createHeadLocation(UUID.randomUUID(), "C", null, true);
            headLocations().add(hl);

            ArrayList<HeadLocation> result1 = headService.getChargedHeadLocations();
            ArrayList<HeadLocation> result2 = headService.getChargedHeadLocations();

            assertThat(result1).isNotSameAs(result2);
            assertThat(result1).isEqualTo(result2);
        }

        @Test
        void getHeadRawNameOrUuid_returns_new_list_not_same_reference() throws Exception {
            HeadLocation hl = createHeadLocation(UUID.randomUUID(), "Test", null, true);
            headLocations().add(hl);

            ArrayList<String> result1 = headService.getHeadRawNameOrUuid();
            ArrayList<String> result2 = headService.getHeadRawNameOrUuid();

            assertThat(result1).isNotSameAs(result2);
        }

        @Test
        void resolveHeadIdentifier_with_valid_uuid_prefers_uuid_over_name() throws Exception {
            UUID uuid = UUID.randomUUID();
            // One head with UUID matching, another with name matching the UUID string
            HeadLocation hlByUuid = createHeadLocation(uuid, "NotByName", null, true);
            headLocations().add(hlByUuid);

            assertThat(headService.resolveHeadIdentifier(uuid.toString())).isSameAs(hlByUuid);
        }

        @Test
        void multiple_heads_at_same_location_getHeadAt_returns_first() throws Exception {
            Location loc = mock(Location.class);
            HeadLocation hl1 = createHeadLocation(UUID.randomUUID(), "First", loc, true);
            HeadLocation hl2 = createHeadLocation(UUID.randomUUID(), "Second", loc, true);
            headLocations().add(hl1);
            headLocations().add(hl2);

            try (MockedStatic<LocationUtils> mocked = mockStatic(LocationUtils.class)) {
                mocked.when(() -> LocationUtils.areEquals(loc, loc)).thenReturn(true);

                assertThat(headService.getHeadAt(loc)).isSameAs(hl1);
            }
        }
    }

    // =========================================================================
    // loadLocations
    // =========================================================================

    class LoadLocations {

        @Test
        void no_hunts_resets_to_empty_list() throws Exception {
            headLocations().add(createHeadLocation(UUID.randomUUID(), "Old", null, true));

            when(storageService.isStorageError()).thenReturn(false);
            when(huntService.getAllHunts()).thenReturn(Collections.emptyList());
            lenient().when(configService.databaseEnabled()).thenReturn(false);

            headService.loadLocations();

            assertThat(headService.getHeadLocations()).isEmpty();
        }

        @Test
        void storage_error_prevents_loading() {
            when(storageService.isStorageError()).thenReturn(true);

            headService.loadLocations();

            assertThat(headService.getHeadLocations()).isEmpty();
            verify(huntService, never()).getAllHunts();
        }

        @Test
        void loads_head_that_already_exists_in_storage() throws Exception {
            UUID uuid = UUID.randomUUID();
            HeadLocation mockHL = createHeadLocation(uuid, "Loaded", null, true);

            HBHunt hunt = mock(HBHunt.class);
            when(hunt.getId()).thenReturn("hunt1");

            when(storageService.isStorageError()).thenReturn(false);
            when(huntService.getAllHunts()).thenReturn(List.of(hunt));
            when(huntConfigService.loadLocationsFromHunt("hunt1")).thenReturn(List.of(mockHL));
            when(storageService.isHeadExist(uuid)).thenReturn(true);
            lenient().when(configService.databaseEnabled()).thenReturn(false);
            lenient().when(configService.spinEnabled()).thenReturn(false);

            headService.loadLocations();

            assertThat(headService.getHeadLocations()).hasSize(1);
            verify(storageService, never()).createOrUpdateHead(any(), any());
        }

        @Test
        void creates_head_in_storage_when_not_exists_and_location_is_null() throws Exception {
            UUID uuid = UUID.randomUUID();
            HeadLocation mockHL = createHeadLocation(uuid, "NoLoc", null, false);
            lenient().when(mockHL.getLocation()).thenReturn(null);

            HBHunt hunt = mock(HBHunt.class);
            when(hunt.getId()).thenReturn("hunt1");

            when(storageService.isStorageError()).thenReturn(false);
            when(huntService.getAllHunts()).thenReturn(List.of(hunt));
            when(huntConfigService.loadLocationsFromHunt("hunt1")).thenReturn(List.of(mockHL));
            when(storageService.isHeadExist(uuid)).thenReturn(false);
            lenient().when(configService.databaseEnabled()).thenReturn(false);
            lenient().when(configService.spinEnabled()).thenReturn(false);

            headService.loadLocations();

            verify(storageService).createOrUpdateHead(uuid, "");
            assertThat(headService.getHeadLocations()).hasSize(1);
        }

        @Test
        void creates_head_in_storage_when_not_exists_and_location_is_present() throws Exception {
            UUID uuid = UUID.randomUUID();
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            when(loc.getBlock()).thenReturn(block);

            HeadLocation mockHL = createHeadLocation(uuid, "WithLoc", loc, true);

            HBHunt hunt = mock(HBHunt.class);
            when(hunt.getId()).thenReturn("hunt1");

            when(storageService.isStorageError()).thenReturn(false);
            when(huntService.getAllHunts()).thenReturn(List.of(hunt));
            when(huntConfigService.loadLocationsFromHunt("hunt1")).thenReturn(List.of(mockHL));
            when(storageService.isHeadExist(uuid)).thenReturn(false);
            lenient().when(configService.databaseEnabled()).thenReturn(false);
            lenient().when(configService.spinEnabled()).thenReturn(false);

            try (MockedStatic<HeadUtils> huStatic = mockStatic(HeadUtils.class)) {
                huStatic.when(() -> HeadUtils.getHeadTexture(block)).thenReturn("texture123");

                headService.loadLocations();

                verify(storageService).createOrUpdateHead(uuid, "texture123");
                assertThat(headService.getHeadLocations()).hasSize(1);
            }
        }

        @Test
        void storage_exception_during_isHeadExist_skips_head_and_continues() throws Exception {
            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();
            HeadLocation mockHL1 = createHeadLocation(uuid1, "Fail", null, true);
            HeadLocation mockHL2 = createHeadLocation(uuid2, "Pass", null, true);

            HBHunt hunt = mock(HBHunt.class);
            when(hunt.getId()).thenReturn("hunt1");

            when(storageService.isStorageError()).thenReturn(false);
            when(huntService.getAllHunts()).thenReturn(List.of(hunt));
            when(huntConfigService.loadLocationsFromHunt("hunt1")).thenReturn(List.of(mockHL1, mockHL2));
            when(storageService.isHeadExist(uuid1)).thenThrow(new RuntimeException("storage failure"));
            when(storageService.isHeadExist(uuid2)).thenReturn(true);
            lenient().when(configService.databaseEnabled()).thenReturn(false);
            lenient().when(configService.spinEnabled()).thenReturn(false);

            headService.loadLocations();

            // uuid1 is skipped due to exception, uuid2 loaded successfully
            assertThat(headService.getHeadLocations()).hasSize(1);
            assertThat(headService.getHeadLocations().getFirst().getUuid()).isEqualTo(uuid2);
        }

        @Test
        void adds_spin_task_when_spin_enabled_and_not_linked() throws Exception {
            UUID uuid = UUID.randomUUID();
            HeadLocation mockHL = createHeadLocation(uuid, "SpinHead", null, true);

            HBHunt hunt = mock(HBHunt.class);
            when(hunt.getId()).thenReturn("hunt1");

            when(storageService.isStorageError()).thenReturn(false);
            when(huntService.getAllHunts()).thenReturn(List.of(hunt));
            when(huntConfigService.loadLocationsFromHunt("hunt1")).thenReturn(List.of(mockHL));
            when(storageService.isHeadExist(uuid)).thenReturn(true);
            lenient().when(configService.databaseEnabled()).thenReturn(false);
            when(configService.spinEnabled()).thenReturn(true);
            when(configService.spinLinked()).thenReturn(false);
            when(configService.spinSpeed()).thenReturn(5);
            when(scheduler.runTaskTimer(any(Runnable.class), eq(5L), eq(5L))).thenReturn(99);

            headService.loadLocations();

            verify(scheduler).runTaskTimer(any(Runnable.class), eq(5L), eq(5L));
            assertThat(tasksHeadSpin()).containsEntry(uuid, 99);
        }

        @Test
        void does_not_add_spin_task_when_spin_disabled() throws Exception {
            UUID uuid = UUID.randomUUID();
            HeadLocation mockHL = createHeadLocation(uuid, "NoSpin", null, true);

            HBHunt hunt = mock(HBHunt.class);
            when(hunt.getId()).thenReturn("hunt1");

            when(storageService.isStorageError()).thenReturn(false);
            when(huntService.getAllHunts()).thenReturn(List.of(hunt));
            when(huntConfigService.loadLocationsFromHunt("hunt1")).thenReturn(List.of(mockHL));
            when(storageService.isHeadExist(uuid)).thenReturn(true);
            lenient().when(configService.databaseEnabled()).thenReturn(false);
            when(configService.spinEnabled()).thenReturn(false);

            headService.loadLocations();

            verify(scheduler, never()).runTaskTimer(any(Runnable.class), anyLong(), anyLong());
            assertThat(tasksHeadSpin()).isEmpty();
        }

        @Test
        void does_not_add_spin_task_when_spin_linked() throws Exception {
            UUID uuid = UUID.randomUUID();
            HeadLocation mockHL = createHeadLocation(uuid, "LinkedSpin", null, true);

            HBHunt hunt = mock(HBHunt.class);
            when(hunt.getId()).thenReturn("hunt1");

            when(storageService.isStorageError()).thenReturn(false);
            when(huntService.getAllHunts()).thenReturn(List.of(hunt));
            when(huntConfigService.loadLocationsFromHunt("hunt1")).thenReturn(List.of(mockHL));
            when(storageService.isHeadExist(uuid)).thenReturn(true);
            lenient().when(configService.databaseEnabled()).thenReturn(false);
            when(configService.spinEnabled()).thenReturn(true);
            when(configService.spinLinked()).thenReturn(true);

            headService.loadLocations();

            verify(scheduler, never()).runTaskTimer(any(Runnable.class), anyLong(), anyLong());
        }

        @Test
        void multiple_locations_get_incrementing_spin_offsets() throws Exception {
            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();
            HeadLocation mockHL1 = createHeadLocation(uuid1, "S1", null, true);
            HeadLocation mockHL2 = createHeadLocation(uuid2, "S2", null, true);

            HBHunt hunt = mock(HBHunt.class);
            when(hunt.getId()).thenReturn("hunt1");

            when(storageService.isStorageError()).thenReturn(false);
            when(huntService.getAllHunts()).thenReturn(List.of(hunt));
            when(huntConfigService.loadLocationsFromHunt("hunt1")).thenReturn(List.of(mockHL1, mockHL2));
            when(storageService.isHeadExist(uuid1)).thenReturn(true);
            when(storageService.isHeadExist(uuid2)).thenReturn(true);
            lenient().when(configService.databaseEnabled()).thenReturn(false);
            when(configService.spinEnabled()).thenReturn(true);
            when(configService.spinLinked()).thenReturn(false);
            when(configService.spinSpeed()).thenReturn(3);
            when(scheduler.runTaskTimer(any(Runnable.class), eq(5L), eq(3L))).thenReturn(10);
            when(scheduler.runTaskTimer(any(Runnable.class), eq(10L), eq(3L))).thenReturn(20);

            headService.loadLocations();

            // offset 1 -> 5L*1 = 5, offset 2 -> 5L*2 = 10
            verify(scheduler).runTaskTimer(any(Runnable.class), eq(5L), eq(3L));
            verify(scheduler).runTaskTimer(any(Runnable.class), eq(10L), eq(3L));
        }

        @Test
        void clears_existing_headLocations_before_loading() throws Exception {
            HeadLocation existing = createHeadLocation(UUID.randomUUID(), "Existing", null, true);
            headLocations().add(existing);

            when(storageService.isStorageError()).thenReturn(false);
            when(huntService.getAllHunts()).thenReturn(Collections.emptyList());
            lenient().when(configService.databaseEnabled()).thenReturn(false);

            headService.loadLocations();

            assertThat(headService.getHeadLocations()).isEmpty();
        }

        @Test
        void database_purge_removes_out_of_sync_heads() throws Exception {
            UUID inSyncUuid = UUID.randomUUID();
            UUID outOfSyncUuid = UUID.randomUUID();
            HeadLocation mockHL = createHeadLocation(inSyncUuid, "InSync", null, true);

            HBHunt hunt = mock(HBHunt.class);
            when(hunt.getId()).thenReturn("hunt1");

            when(storageService.isStorageError()).thenReturn(false);
            when(huntService.getAllHunts()).thenReturn(List.of(hunt));
            when(huntConfigService.loadLocationsFromHunt("hunt1")).thenReturn(List.of(mockHL));
            when(storageService.isHeadExist(inSyncUuid)).thenReturn(true);
            when(configService.databaseEnabled()).thenReturn(true);
            lenient().when(configService.spinEnabled()).thenReturn(false);

            // Database returns both in-sync and out-of-sync heads
            ArrayList<UUID> dbHeads = new ArrayList<>(List.of(inSyncUuid, outOfSyncUuid));
            when(storageService.getHeadsByServerId()).thenReturn(dbHeads);

            headService.loadLocations();

            // The out-of-sync head should be removed
            verify(storageService).removeHead(outOfSyncUuid, true);
        }

        @Test
        void database_purge_syncs_all_heads_when_db_returns_empty() throws Exception {
            UUID uuid = UUID.randomUUID();
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            when(loc.getBlock()).thenReturn(block);

            HeadLocation mockHL = createHeadLocation(uuid, "SyncMe", loc, true);

            HBHunt hunt = mock(HBHunt.class);
            when(hunt.getId()).thenReturn("hunt1");

            when(storageService.isStorageError()).thenReturn(false);
            when(huntService.getAllHunts()).thenReturn(List.of(hunt));
            when(huntConfigService.loadLocationsFromHunt("hunt1")).thenReturn(List.of(mockHL));
            when(storageService.isHeadExist(uuid)).thenReturn(true);
            when(configService.databaseEnabled()).thenReturn(true);
            lenient().when(configService.spinEnabled()).thenReturn(false);

            // DB returns empty -> all heads should be created
            when(storageService.getHeadsByServerId()).thenReturn(new ArrayList<>());

            try (MockedStatic<HeadUtils> huStatic = mockStatic(HeadUtils.class)) {
                huStatic.when(() -> HeadUtils.getHeadTexture(block)).thenReturn("tex_sync");

                headService.loadLocations();

                verify(storageService).createOrUpdateHead(uuid, "tex_sync");
            }
        }

        @Test
        void database_purge_exception_is_caught_silently() throws Exception {
            when(storageService.isStorageError()).thenReturn(false);
            when(huntService.getAllHunts()).thenReturn(Collections.emptyList());
            when(configService.databaseEnabled()).thenReturn(true);
            when(storageService.getHeadsByServerId()).thenThrow(new InternalException("db down"));

            // Should not throw
            headService.loadLocations();

            assertThat(headService.getHeadLocations()).isEmpty();
        }

        @Test
        void database_purge_no_out_of_sync_heads_does_not_remove() throws Exception {
            UUID uuid = UUID.randomUUID();
            HeadLocation mockHL = createHeadLocation(uuid, "Synced", null, true);

            HBHunt hunt = mock(HBHunt.class);
            when(hunt.getId()).thenReturn("hunt1");

            when(storageService.isStorageError()).thenReturn(false);
            when(huntService.getAllHunts()).thenReturn(List.of(hunt));
            when(huntConfigService.loadLocationsFromHunt("hunt1")).thenReturn(List.of(mockHL));
            when(storageService.isHeadExist(uuid)).thenReturn(true);
            when(configService.databaseEnabled()).thenReturn(true);
            lenient().when(configService.spinEnabled()).thenReturn(false);

            // DB contains only the same head -- no out-of-sync
            ArrayList<UUID> dbHeads = new ArrayList<>(List.of(uuid));
            when(storageService.getHeadsByServerId()).thenReturn(dbHeads);

            headService.loadLocations();

            verify(storageService, never()).removeHead(any(), anyBoolean());
        }

        @Test
        void no_database_purge_when_database_disabled() throws Exception {
            UUID uuid = UUID.randomUUID();
            HeadLocation mockHL = createHeadLocation(uuid, "NoPurge", null, true);

            HBHunt hunt = mock(HBHunt.class);
            when(hunt.getId()).thenReturn("hunt1");

            when(storageService.isStorageError()).thenReturn(false);
            when(huntService.getAllHunts()).thenReturn(List.of(hunt));
            when(huntConfigService.loadLocationsFromHunt("hunt1")).thenReturn(List.of(mockHL));
            when(storageService.isHeadExist(uuid)).thenReturn(true);
            when(configService.databaseEnabled()).thenReturn(false);
            lenient().when(configService.spinEnabled()).thenReturn(false);

            headService.loadLocations();

            verify(storageService, never()).getHeadsByServerId();
        }
    }

    // =========================================================================
    // removeAllHeadLocationsAsync
    // =========================================================================

    @Nested
    class RemoveAllHeadLocationsAsync {

        @Test
        void removes_all_heads_and_calls_onComplete() throws Exception {
            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();
            Location loc1 = mock(Location.class);
            Location loc2 = mock(Location.class);
            Block block1 = mock(Block.class);
            Block block2 = mock(Block.class);
            lenient().when(loc1.getBlock()).thenReturn(block1);
            lenient().when(loc2.getBlock()).thenReturn(block2);

            HeadLocation hl1 = createHeadLocation(uuid1, "R1", loc1, true);
            lenient().when(hl1.getHuntId()).thenReturn("default");
            HeadLocation hl2 = createHeadLocation(uuid2, "R2", loc2, true);
            lenient().when(hl2.getHuntId()).thenReturn("default");
            headLocations().add(hl1);
            headLocations().add(hl2);

            lenient().when(configService.hologramsEnabled()).thenReturn(false);

            // Capture the async runnable
            doAnswer(invocation -> {
                Runnable asyncTask = invocation.getArgument(0);
                asyncTask.run();
                return null;
            }).when(scheduler).runTaskAsync(any(Runnable.class));

            // Capture sync tasks
            doAnswer(invocation -> {
                Runnable syncTask = invocation.getArgument(0);
                syncTask.run();
                return null;
            }).when(scheduler).runTask(any(Runnable.class));

            @SuppressWarnings("unchecked")
            Consumer<Integer> onComplete = mock(Consumer.class);

            ArrayList<HeadLocation> headsToRemove = new ArrayList<>(List.of(hl1, hl2));
            headService.removeAllHeadLocationsAsync(headsToRemove, true, onComplete);

            verify(storageService).removeHead(uuid1, true);
            verify(storageService).removeHead(uuid2, true);
            verify(onComplete).accept(2);
        }

        @Test
        void skips_null_entries() throws Exception {
            UUID uuid = UUID.randomUUID();
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            lenient().when(loc.getBlock()).thenReturn(block);

            HeadLocation hl = createHeadLocation(uuid, "Valid", loc, true);
            lenient().when(hl.getHuntId()).thenReturn("default");
            headLocations().add(hl);

            lenient().when(configService.hologramsEnabled()).thenReturn(false);

            doAnswer(invocation -> {
                Runnable asyncTask = invocation.getArgument(0);
                asyncTask.run();
                return null;
            }).when(scheduler).runTaskAsync(any(Runnable.class));

            doAnswer(invocation -> {
                Runnable syncTask = invocation.getArgument(0);
                syncTask.run();
                return null;
            }).when(scheduler).runTask(any(Runnable.class));

            @SuppressWarnings("unchecked")
            Consumer<Integer> onComplete = mock(Consumer.class);

            ArrayList<HeadLocation> headsToRemove = new ArrayList<>();
            headsToRemove.add(null);
            headsToRemove.add(hl);
            headService.removeAllHeadLocationsAsync(headsToRemove, false, onComplete);

            verify(storageService).removeHead(uuid, false);
            verify(onComplete).accept(1);
        }

        @Test
        void storage_exception_skips_head_and_continues() throws Exception {
            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();
            Location loc1 = mock(Location.class);
            Location loc2 = mock(Location.class);
            Block block1 = mock(Block.class);
            Block block2 = mock(Block.class);
            lenient().when(loc1.getBlock()).thenReturn(block1);
            lenient().when(loc2.getBlock()).thenReturn(block2);

            HeadLocation hl1 = createHeadLocation(uuid1, "Fail", loc1, true);
            HeadLocation hl2 = createHeadLocation(uuid2, "Pass", loc2, true);
            lenient().when(hl1.getNameOrUuid()).thenReturn("Fail");
            lenient().when(hl1.getHuntId()).thenReturn("default");
            lenient().when(hl2.getHuntId()).thenReturn("default");
            headLocations().add(hl1);
            headLocations().add(hl2);

            lenient().when(configService.hologramsEnabled()).thenReturn(false);

            doThrow(new InternalException("error")).when(storageService).removeHead(uuid1, true);

            doAnswer(invocation -> {
                Runnable asyncTask = invocation.getArgument(0);
                asyncTask.run();
                return null;
            }).when(scheduler).runTaskAsync(any(Runnable.class));

            doAnswer(invocation -> {
                Runnable syncTask = invocation.getArgument(0);
                syncTask.run();
                return null;
            }).when(scheduler).runTask(any(Runnable.class));

            @SuppressWarnings("unchecked")
            Consumer<Integer> onComplete = mock(Consumer.class);

            ArrayList<HeadLocation> headsToRemove = new ArrayList<>(List.of(hl1, hl2));
            headService.removeAllHeadLocationsAsync(headsToRemove, true, onComplete);

            // hl1 failed, hl2 succeeded
            verify(onComplete).accept(1);
        }

        @Test
        void removes_holograms_when_enabled() throws Exception {
            UUID uuid = UUID.randomUUID();
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            lenient().when(loc.getBlock()).thenReturn(block);

            HeadLocation hl = createHeadLocation(uuid, "Holo", loc, true);
            lenient().when(hl.getHuntId()).thenReturn("default");
            headLocations().add(hl);

            when(configService.hologramsEnabled()).thenReturn(true);

            doAnswer(invocation -> {
                Runnable asyncTask = invocation.getArgument(0);
                asyncTask.run();
                return null;
            }).when(scheduler).runTaskAsync(any(Runnable.class));

            doAnswer(invocation -> {
                Runnable syncTask = invocation.getArgument(0);
                syncTask.run();
                return null;
            }).when(scheduler).runTask(any(Runnable.class));

            @SuppressWarnings("unchecked")
            Consumer<Integer> onComplete = mock(Consumer.class);

            ArrayList<HeadLocation> headsToRemove = new ArrayList<>(List.of(hl));
            headService.removeAllHeadLocationsAsync(headsToRemove, true, onComplete);

            verify(hologramService).removeHolograms(loc);
        }

        @Test
        void does_not_remove_hologram_when_disabled() throws Exception {
            UUID uuid = UUID.randomUUID();
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            lenient().when(loc.getBlock()).thenReturn(block);

            HeadLocation hl = createHeadLocation(uuid, "NoHolo", loc, true);
            lenient().when(hl.getHuntId()).thenReturn("default");
            headLocations().add(hl);

            when(configService.hologramsEnabled()).thenReturn(false);

            doAnswer(invocation -> {
                Runnable asyncTask = invocation.getArgument(0);
                asyncTask.run();
                return null;
            }).when(scheduler).runTaskAsync(any(Runnable.class));

            doAnswer(invocation -> {
                Runnable syncTask = invocation.getArgument(0);
                syncTask.run();
                return null;
            }).when(scheduler).runTask(any(Runnable.class));

            @SuppressWarnings("unchecked")
            Consumer<Integer> onComplete = mock(Consumer.class);

            ArrayList<HeadLocation> headsToRemove = new ArrayList<>(List.of(hl));
            headService.removeAllHeadLocationsAsync(headsToRemove, true, onComplete);

            verify(hologramService, never()).removeHolograms(any());
        }

        @Test
        void does_not_remove_hologram_when_hologramService_is_null() throws Exception {
            headService.setHologramService(null);

            UUID uuid = UUID.randomUUID();
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            lenient().when(loc.getBlock()).thenReturn(block);

            HeadLocation hl = createHeadLocation(uuid, "NullHolo", loc, true);
            lenient().when(hl.getHuntId()).thenReturn("default");
            headLocations().add(hl);

            lenient().when(configService.hologramsEnabled()).thenReturn(true);

            doAnswer(invocation -> {
                Runnable asyncTask = invocation.getArgument(0);
                asyncTask.run();
                return null;
            }).when(scheduler).runTaskAsync(any(Runnable.class));

            doAnswer(invocation -> {
                Runnable syncTask = invocation.getArgument(0);
                syncTask.run();
                return null;
            }).when(scheduler).runTask(any(Runnable.class));

            @SuppressWarnings("unchecked")
            Consumer<Integer> onComplete = mock(Consumer.class);

            ArrayList<HeadLocation> headsToRemove = new ArrayList<>(List.of(hl));
            headService.removeAllHeadLocationsAsync(headsToRemove, true, onComplete);

            // No NPE should occur
            verify(onComplete).accept(1);
        }

        @Test
        void cancels_spin_task_for_removed_heads() throws Exception {
            UUID uuid = UUID.randomUUID();
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            lenient().when(loc.getBlock()).thenReturn(block);

            HeadLocation hl = createHeadLocation(uuid, "SpinRemove", loc, true);
            lenient().when(hl.getHuntId()).thenReturn("default");
            headLocations().add(hl);
            tasksHeadSpin().put(uuid, 77);

            lenient().when(configService.hologramsEnabled()).thenReturn(false);

            doAnswer(invocation -> {
                Runnable asyncTask = invocation.getArgument(0);
                asyncTask.run();
                return null;
            }).when(scheduler).runTaskAsync(any(Runnable.class));

            doAnswer(invocation -> {
                Runnable syncTask = invocation.getArgument(0);
                syncTask.run();
                return null;
            }).when(scheduler).runTask(any(Runnable.class));

            @SuppressWarnings("unchecked")
            Consumer<Integer> onComplete = mock(Consumer.class);

            ArrayList<HeadLocation> headsToRemove = new ArrayList<>(List.of(hl));
            headService.removeAllHeadLocationsAsync(headsToRemove, true, onComplete);

            verify(scheduler).cancelTask(77);
            assertThat(tasksHeadSpin()).doesNotContainKey(uuid);
        }

        @Test
        void removes_associated_headMoves() throws Exception {
            UUID uuid = UUID.randomUUID();
            UUID otherUuid = UUID.randomUUID();
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            lenient().when(loc.getBlock()).thenReturn(block);

            HeadLocation hl = createHeadLocation(uuid, "MoveRemove", loc, true);
            lenient().when(hl.getHuntId()).thenReturn("default");
            headLocations().add(hl);
            headMoves().put(uuid, new HeadMove(uuid, mock(Location.class)));
            headMoves().put(otherUuid, new HeadMove(otherUuid, mock(Location.class)));

            lenient().when(configService.hologramsEnabled()).thenReturn(false);

            doAnswer(invocation -> {
                Runnable asyncTask = invocation.getArgument(0);
                asyncTask.run();
                return null;
            }).when(scheduler).runTaskAsync(any(Runnable.class));

            doAnswer(invocation -> {
                Runnable syncTask = invocation.getArgument(0);
                syncTask.run();
                return null;
            }).when(scheduler).runTask(any(Runnable.class));

            @SuppressWarnings("unchecked")
            Consumer<Integer> onComplete = mock(Consumer.class);

            ArrayList<HeadLocation> headsToRemove = new ArrayList<>(List.of(hl));
            headService.removeAllHeadLocationsAsync(headsToRemove, true, onComplete);

            assertThat(headMoves()).doesNotContainKey(uuid);
            assertThat(headMoves()).containsKey(otherUuid);
        }

        @Test
        void empty_list_calls_onComplete_with_zero() throws Exception {
            doAnswer(invocation -> {
                Runnable asyncTask = invocation.getArgument(0);
                asyncTask.run();
                return null;
            }).when(scheduler).runTaskAsync(any(Runnable.class));

            doAnswer(invocation -> {
                Runnable syncTask = invocation.getArgument(0);
                syncTask.run();
                return null;
            }).when(scheduler).runTask(any(Runnable.class));

            @SuppressWarnings("unchecked")
            Consumer<Integer> onComplete = mock(Consumer.class);

            headService.removeAllHeadLocationsAsync(new ArrayList<>(), true, onComplete);

            verify(onComplete).accept(0);
        }

        @Test
        void removes_from_hunt_config() throws Exception {
            UUID uuid = UUID.randomUUID();
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            lenient().when(loc.getBlock()).thenReturn(block);

            HeadLocation hl = createHeadLocation(uuid, "ConfigRemove", loc, true);
            when(hl.getHuntId()).thenReturn("default");
            headLocations().add(hl);

            lenient().when(configService.hologramsEnabled()).thenReturn(false);

            doAnswer(invocation -> {
                Runnable asyncTask = invocation.getArgument(0);
                asyncTask.run();
                return null;
            }).when(scheduler).runTaskAsync(any(Runnable.class));

            doAnswer(invocation -> {
                Runnable syncTask = invocation.getArgument(0);
                syncTask.run();
                return null;
            }).when(scheduler).runTask(any(Runnable.class));

            @SuppressWarnings("unchecked")
            Consumer<Integer> onComplete = mock(Consumer.class);

            ArrayList<HeadLocation> headsToRemove = new ArrayList<>(List.of(hl));
            headService.removeAllHeadLocationsAsync(headsToRemove, true, onComplete);

            verify(huntConfigService).removeLocationFromHunt("default", uuid);
        }
    }

    // =========================================================================
    // saveHeadLocation - spin integration
    // =========================================================================

    @Nested
    class SaveHeadLocationSpinIntegration {

        @Test
        void adds_spin_task_when_spin_enabled_and_not_linked() throws Exception {
            Location loc = mock(Location.class);

            lenient().when(configService.hologramsEnabled()).thenReturn(false);
            when(configService.spinEnabled()).thenReturn(true);
            when(configService.spinLinked()).thenReturn(false);
            when(configService.spinSpeed()).thenReturn(7);
            when(scheduler.runTaskTimer(any(Runnable.class), eq(5L), eq(7L))).thenReturn(55);

            try (MockedStatic<InternalUtils> mocked = mockStatic(InternalUtils.class)) {
                UUID generatedUuid = UUID.randomUUID();
                mocked.when(() -> InternalUtils.generateNewUUID(anyList())).thenReturn(generatedUuid);

                headService.saveHeadLocation(loc, "tex", "default");

                verify(scheduler).runTaskTimer(any(Runnable.class), eq(5L), eq(7L));
                assertThat(tasksHeadSpin()).containsEntry(generatedUuid, 55);
            }
        }

        @Test
        void does_not_add_spin_task_when_spin_disabled() throws Exception {
            Location loc = mock(Location.class);

            lenient().when(configService.hologramsEnabled()).thenReturn(false);
            when(configService.spinEnabled()).thenReturn(false);

            try (MockedStatic<InternalUtils> mocked = mockStatic(InternalUtils.class)) {
                UUID generatedUuid = UUID.randomUUID();
                mocked.when(() -> InternalUtils.generateNewUUID(anyList())).thenReturn(generatedUuid);

                headService.saveHeadLocation(loc, "tex", "default");

                verify(scheduler, never()).runTaskTimer(any(Runnable.class), anyLong(), anyLong());
                assertThat(tasksHeadSpin()).isEmpty();
            }
        }

        @Test
        void does_not_add_spin_task_when_spin_linked() throws Exception {
            Location loc = mock(Location.class);

            lenient().when(configService.hologramsEnabled()).thenReturn(false);
            when(configService.spinEnabled()).thenReturn(true);
            when(configService.spinLinked()).thenReturn(true);

            try (MockedStatic<InternalUtils> mocked = mockStatic(InternalUtils.class)) {
                UUID generatedUuid = UUID.randomUUID();
                mocked.when(() -> InternalUtils.generateNewUUID(anyList())).thenReturn(generatedUuid);

                headService.saveHeadLocation(loc, "tex", "default");

                verify(scheduler, never()).runTaskTimer(any(Runnable.class), anyLong(), anyLong());
            }
        }
    }

    // =========================================================================
    // getHeads
    // =========================================================================

    @Nested
    class GetHeads {

        @Test
        void returns_heads_list() throws Exception {
            ArrayList<HBHead> heads = new ArrayList<>();
            heads.add(mock(HBHead.class));
            setField("heads", heads);

            assertThat(headService.getHeads()).isSameAs(heads);
        }

        @Test
        void returns_empty_when_no_heads() throws Exception {
            setField("heads", new ArrayList<HBHead>());

            assertThat(headService.getHeads()).isEmpty();
        }
    }

    // =========================================================================
    // removeHeadLocation - hologramService null path
    // =========================================================================

    @Nested
    class RemoveHeadLocationHologramServiceNull {

        @Test
        void does_not_throw_when_hologramService_is_null_and_holograms_enabled() throws Exception {
            headService.setHologramService(null);

            UUID uuid = UUID.randomUUID();
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            when(loc.getBlock()).thenReturn(block);

            HeadLocation hl = createHeadLocation(uuid, "NullHolo", loc, true);
            lenient().when(hl.getHuntId()).thenReturn("default");
            headLocations().add(hl);

            when(configService.hologramsEnabled()).thenReturn(true);

            // Should not throw NPE
            headService.removeHeadLocation(hl, true);

            assertThat(headLocations()).doesNotContain(hl);
        }
    }

    // =========================================================================
    // rotateHead
    // =========================================================================

    @Nested
    class RotateHead {

        @Test
        void does_nothing_when_block_is_not_player_head() {
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            when(loc.getBlock()).thenReturn(block);
            when(block.getType()).thenReturn(Material.STONE);

            HeadLocation hl = createHeadLocation(UUID.randomUUID(), "NotHead", loc, true);

            try (MockedStatic<HeadUtils> huStatic = mockStatic(HeadUtils.class)) {
                headService.rotateHead(hl);

                huStatic.verify(() -> HeadUtils.rotateHead(any(), any()), never());
            }
        }

        @Test
        void rotates_head_to_next_direction() {
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            when(loc.getBlock()).thenReturn(block);
            when(block.getType()).thenReturn(Material.PLAYER_HEAD);

            HeadLocation hl = createHeadLocation(UUID.randomUUID(), "Spin", loc, true);

            try (MockedStatic<HeadUtils> huStatic = mockStatic(HeadUtils.class)) {
                huStatic.when(() -> HeadUtils.getRotation(block)).thenReturn(BlockFace.NORTH);

                headService.rotateHead(hl);

                // NORTH is key 0, next is key 1 -> NORTH_NORTH_EAST
                huStatic.verify(() -> HeadUtils.rotateHead(block, BlockFace.NORTH_NORTH_EAST));
            }
        }

        @Test
        void wraps_around_from_last_rotation() {
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            when(loc.getBlock()).thenReturn(block);
            when(block.getType()).thenReturn(Material.PLAYER_HEAD);

            HeadLocation hl = createHeadLocation(UUID.randomUUID(), "WrapSpin", loc, true);

            try (MockedStatic<HeadUtils> huStatic = mockStatic(HeadUtils.class)) {
                // NORTH_NORTH_WEST is key 15 (last), next should be key 0 -> NORTH
                huStatic.when(() -> HeadUtils.getRotation(block)).thenReturn(BlockFace.NORTH_NORTH_WEST);

                headService.rotateHead(hl);

                huStatic.verify(() -> HeadUtils.rotateHead(block, BlockFace.NORTH));
            }
        }

        @Test
        void handles_unknown_rotation_defaults_to_zero() {
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            when(loc.getBlock()).thenReturn(block);
            when(block.getType()).thenReturn(Material.PLAYER_HEAD);

            HeadLocation hl = createHeadLocation(UUID.randomUUID(), "UnknownRot", loc, true);

            try (MockedStatic<HeadUtils> huStatic = mockStatic(HeadUtils.class)) {
                // Return a BlockFace that is NOT in skullRotationList
                huStatic.when(() -> HeadUtils.getRotation(block)).thenReturn(BlockFace.UP);

                headService.rotateHead(hl);

                // getKeyByValue returns null, defaults to 0, (0+1)%16 = 1 -> NORTH_NORTH_EAST
                huStatic.verify(() -> HeadUtils.rotateHead(block, BlockFace.NORTH_NORTH_EAST));
            }
        }
    }

    // =========================================================================
    // cancelAllSpinTasks (through load-like scenarios)
    // =========================================================================

    @Nested
    class CancelAllSpinTasks {

        @Test
        void cancels_all_existing_tasks() throws Exception {
            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();
            tasksHeadSpin().put(uuid1, 100);
            tasksHeadSpin().put(uuid2, 200);

            // Use loadLocations with null locations section to trigger cancelAllSpinTasks indirectly
            // Actually cancelAllSpinTasks is private and called by load(),
            // but load() also calls YamlConfiguration.loadConfiguration which is static.
            // We'll test it through the removeAllHeadLocationsAsync path instead.
            // Let's just test it via reflection.
            java.lang.reflect.Method cancelMethod = HeadService.class.getDeclaredMethod("cancelAllSpinTasks");
            cancelMethod.setAccessible(true);
            cancelMethod.invoke(headService);

            verify(scheduler).cancelTask(100);
            verify(scheduler).cancelTask(200);
        }

        @Test
        void handles_null_tasksHeadSpin_without_error() throws Exception {
            setField("tasksHeadSpin", null);

            java.lang.reflect.Method cancelMethod = HeadService.class.getDeclaredMethod("cancelAllSpinTasks");
            cancelMethod.setAccessible(true);
            cancelMethod.invoke(headService);

            verify(scheduler, never()).cancelTask(anyInt());
        }

        @Test
        void handles_empty_tasks_without_error() throws Exception {
            java.lang.reflect.Method cancelMethod = HeadService.class.getDeclaredMethod("cancelAllSpinTasks");
            cancelMethod.setAccessible(true);
            cancelMethod.invoke(headService);

            verify(scheduler, never()).cancelTask(anyInt());
        }
    }

    // =========================================================================
    // doAddHeadToSpin (via saveHeadLocation)
    // =========================================================================

    @Nested
    class DoAddHeadToSpin {

        @Test
        void spin_task_uses_correct_delay_formula() throws Exception {
            // Testing that offset is applied: delay = 5L * offset
            Location loc = mock(Location.class);

            lenient().when(configService.hologramsEnabled()).thenReturn(false);
            when(configService.spinEnabled()).thenReturn(true);
            when(configService.spinLinked()).thenReturn(false);
            when(configService.spinSpeed()).thenReturn(10);
            when(scheduler.runTaskTimer(any(Runnable.class), eq(5L), eq(10L))).thenReturn(42);

            try (MockedStatic<InternalUtils> mocked = mockStatic(InternalUtils.class)) {
                UUID generatedUuid = UUID.randomUUID();
                mocked.when(() -> InternalUtils.generateNewUUID(anyList())).thenReturn(generatedUuid);

                // saveHeadLocation calls doAddHeadToSpin with offset=1
                headService.saveHeadLocation(loc, "tex", "default");

                // delay = 5L * 1 = 5
                verify(scheduler).runTaskTimer(any(Runnable.class), eq(5L), eq(10L));
            }
        }
    }

    // =========================================================================
    // getHeadLocationsForHunt - additional coverage
    // =========================================================================

    @Nested
    class GetHeadLocationsForHBHuntAdditional {

        @Test
        void all_heads_in_hunt_returns_all() throws Exception {
            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();
            UUID uuid3 = UUID.randomUUID();

            HeadLocation hl1 = createHeadLocation(uuid1, "H1", null, true);
            lenient().when(hl1.getHuntId()).thenReturn("all");
            HeadLocation hl2 = createHeadLocation(uuid2, "H2", null, true);
            lenient().when(hl2.getHuntId()).thenReturn("all");
            HeadLocation hl3 = createHeadLocation(uuid3, "H3", null, true);
            lenient().when(hl3.getHuntId()).thenReturn("all");

            headLocations().add(hl1);
            headLocations().add(hl2);
            headLocations().add(hl3);

            HBHunt hunt = new HBHunt(configService, "all", "All Heads", HuntState.ACTIVE, 1, "D");

            ArrayList<HeadLocation> result = headService.getHeadLocationsForHunt(hunt);

            assertThat(result).containsExactly(hl1, hl2, hl3);
        }

        @Test
        void hunt_with_no_matching_heads_returns_empty() throws Exception {
            UUID uuid = UUID.randomUUID();
            HeadLocation hl = createHeadLocation(uuid, "H", null, true);
            lenient().when(hl.getHuntId()).thenReturn("other");
            headLocations().add(hl);

            HBHunt hunt = new HBHunt(configService, "unknown", "Unknown", HuntState.ACTIVE, 1, "D");

            assertThat(headService.getHeadLocationsForHunt(hunt)).isEmpty();
        }
    }

    // =========================================================================
    // getChargedHeadLocations - mixed ordering
    // =========================================================================

    @Nested
    class GetChargedHeadLocationsAdditional {

        @Test
        void preserves_order_of_charged_heads() throws Exception {
            HeadLocation c1 = createHeadLocation(UUID.randomUUID(), "C1", null, true);
            HeadLocation u1 = createHeadLocation(UUID.randomUUID(), "U1", null, false);
            HeadLocation c2 = createHeadLocation(UUID.randomUUID(), "C2", null, true);
            HeadLocation u2 = createHeadLocation(UUID.randomUUID(), "U2", null, false);
            HeadLocation c3 = createHeadLocation(UUID.randomUUID(), "C3", null, true);

            headLocations().add(c1);
            headLocations().add(u1);
            headLocations().add(c2);
            headLocations().add(u2);
            headLocations().add(c3);

            ArrayList<HeadLocation> result = headService.getChargedHeadLocations();

            assertThat(result).containsExactly(c1, c2, c3);
        }

        @Test
        void all_uncharged_returns_empty() throws Exception {
            HeadLocation u1 = createHeadLocation(UUID.randomUUID(), "U1", null, false);
            HeadLocation u2 = createHeadLocation(UUID.randomUUID(), "U2", null, false);

            headLocations().add(u1);
            headLocations().add(u2);

            assertThat(headService.getChargedHeadLocations()).isEmpty();
        }
    }

    // =========================================================================
    // removeHeadLocation - additional edge cases
    // =========================================================================

    @Nested
    class RemoveHeadLocationAdditional {

        @Test
        void removes_multiple_headMoves_for_same_head() throws Exception {
            UUID uuid = UUID.randomUUID();
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            when(loc.getBlock()).thenReturn(block);

            HeadLocation hl = createHeadLocation(uuid, "MultiMove", loc, true);
            lenient().when(hl.getHuntId()).thenReturn("default");
            headLocations().add(hl);
            headMoves().put(uuid, new HeadMove(uuid, mock(Location.class)));

            lenient().when(configService.hologramsEnabled()).thenReturn(false);

            headService.removeHeadLocation(hl, true);

            assertThat(headMoves()).doesNotContainKey(uuid);
        }

        @Test
        void removes_from_hunt_correctly() throws Exception {
            UUID uuid = UUID.randomUUID();
            Location loc = mock(Location.class);
            Block block = mock(Block.class);
            when(loc.getBlock()).thenReturn(block);

            HeadLocation hl = createHeadLocation(uuid, "HuntHead", loc, true);
            when(hl.getHuntId()).thenReturn("hunt1");
            headLocations().add(hl);

            HBHunt hunt = mock(HBHunt.class);
            lenient().when(configService.hologramsEnabled()).thenReturn(false);
            when(huntService.getHuntById("hunt1")).thenReturn(hunt);

            headService.removeHeadLocation(hl, true);

            verify(hunt).removeHead(uuid);
            verify(huntConfigService).removeLocationFromHunt("hunt1", uuid);
        }
    }

    // =========================================================================
    // resolveHeadIdentifier - additional coverage
    // =========================================================================

    @Nested
    class ResolveHeadIdentifierAdditional {

        @Test
        void name_lookup_finds_correct_head_among_many() throws Exception {
            HeadLocation hl1 = createHeadLocation(UUID.randomUUID(), "Alpha", null, true);
            HeadLocation hl2 = createHeadLocation(UUID.randomUUID(), "Beta", null, true);
            HeadLocation hl3 = createHeadLocation(UUID.randomUUID(), "Gamma", null, true);

            headLocations().add(hl1);
            headLocations().add(hl2);
            headLocations().add(hl3);

            assertThat(headService.resolveHeadIdentifier("Beta")).isSameAs(hl2);
        }

        @Test
        void uuid_lookup_finds_correct_head_among_many() throws Exception {
            UUID targetUuid = UUID.randomUUID();
            HeadLocation hl1 = createHeadLocation(UUID.randomUUID(), "A", null, true);
            HeadLocation hl2 = createHeadLocation(targetUuid, "B", null, true);
            HeadLocation hl3 = createHeadLocation(UUID.randomUUID(), "C", null, true);

            headLocations().add(hl1);
            headLocations().add(hl2);
            headLocations().add(hl3);

            assertThat(headService.resolveHeadIdentifier(targetUuid.toString())).isSameAs(hl2);
        }
    }

    // =========================================================================
    // saveHeadLocation - config integration
    // =========================================================================

    @Nested
    class SaveHeadLocationConfigIntegration {

        @Test
        void saves_head_in_hunt_config_after_creation() throws Exception {
            Location loc = mock(Location.class);

            lenient().when(configService.hologramsEnabled()).thenReturn(false);
            lenient().when(configService.spinEnabled()).thenReturn(false);

            try (MockedStatic<InternalUtils> mocked = mockStatic(InternalUtils.class)) {
                UUID generatedUuid = UUID.randomUUID();
                mocked.when(() -> InternalUtils.generateNewUUID(anyList())).thenReturn(generatedUuid);

                headService.saveHeadLocation(loc, "tex", "default");

                // Verify that huntConfigService is called to save the location
                verify(huntConfigService).saveLocationInHunt(eq("default"), any(HeadLocation.class));
            }
        }

        @Test
        void head_location_is_added_with_empty_name() throws Exception {
            Location loc = mock(Location.class);

            lenient().when(configService.hologramsEnabled()).thenReturn(false);
            lenient().when(configService.spinEnabled()).thenReturn(false);

            try (MockedStatic<InternalUtils> mocked = mockStatic(InternalUtils.class)) {
                UUID generatedUuid = UUID.randomUUID();
                mocked.when(() -> InternalUtils.generateNewUUID(anyList())).thenReturn(generatedUuid);

                headService.saveHeadLocation(loc, "tex", "default");

                HeadLocation added = headLocations().getFirst();
                // The HeadLocation is created with empty name ""
                assertThat(added.getName()).isEmpty();
            }
        }
    }

    // =========================================================================
    // loadLocations - clears headMoves
    // =========================================================================

    @Nested
    class LoadLocationsHeadMovesClear {

        @Test
        void loadLocations_clears_headLocations() throws Exception {
            // loadLocations clears headLocations at the start
            UUID existingUuid = UUID.randomUUID();
            headLocations().add(createHeadLocation(existingUuid, "Old", null, true));

            when(storageService.isStorageError()).thenReturn(false);
            when(huntService.getAllHunts()).thenReturn(Collections.emptyList());
            lenient().when(configService.databaseEnabled()).thenReturn(false);

            headService.loadLocations();

            assertThat(headService.getHeadLocations()).isEmpty();
        }
    }
}
