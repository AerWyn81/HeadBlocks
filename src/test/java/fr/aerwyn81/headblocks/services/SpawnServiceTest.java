package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.api.events.HuntStateChangeEvent;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.data.head.visual.RenderMode;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntConfig;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.data.hunt.behavior.FixedPositionBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.FreeBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.*;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.scheduler.SchedulerAdapter;
import fr.aerwyn81.headblocks.utils.scheduler.Task;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpawnServiceTest {

    @TempDir
    Path dataFolder;

    @Mock
    ServiceRegistry registry;

    @Mock
    HuntService huntService;

    @Mock
    HeadService headService;

    @Mock
    StorageService storageService;

    @Mock
    LanguageService languageService;

    @Mock
    HeadVisualService visualService;

    @Mock
    SchedulerAdapter scheduler;

    @Mock
    ConfigService configService;

    @Mock
    World world;

    private MockedStatic<Bukkit> bukkit;
    private final List<Delayed> later = new ArrayList<>();
    private final List<Runnable> timers = new ArrayList<>();
    private final Set<Location> occupied = new HashSet<>();
    private HBHunt hunt;
    private SpawnService service;

    private record Delayed(Runnable task, long ticks) {
    }

    @BeforeEach
    void setUp() {
        lenient().when(registry.getHuntService()).thenReturn(huntService);
        lenient().when(registry.getHeadService()).thenReturn(headService);
        lenient().when(registry.getStorageService()).thenReturn(storageService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(registry.getVisualService()).thenReturn(visualService);
        lenient().when(registry.getScheduler()).thenReturn(scheduler);
        lenient().when(huntService.configOf(any())).thenAnswer(invocation -> new HuntConfig(configService));
        lenient().when(world.getName()).thenReturn("world");

        lenient().doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return mock(Task.class);
        }).when(scheduler).runTaskAsync(any(Runnable.class));
        lenient().doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return mock(Task.class);
        }).when(scheduler).runTask(any(Runnable.class));
        lenient().doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(1)).run();
            return null;
        }).when(scheduler).runNow(any(Location.class), any(Runnable.class));
        lenient().doAnswer(invocation -> {
            later.add(new Delayed(invocation.getArgument(0), invocation.getArgument(1)));
            return mock(Task.class);
        }).when(scheduler).runTaskLater(any(Runnable.class), anyLong());
        lenient().doAnswer(invocation -> {
            timers.add(invocation.getArgument(0));
            return mock(Task.class);
        }).when(scheduler).runTaskTimer(any(Runnable.class), anyLong(), anyLong());

        lenient().when(headService.getHeadAt(any())).thenAnswer(invocation -> {
            Location location = invocation.getArgument(0);
            return occupied.stream().anyMatch(o -> o.getBlockX() == location.getBlockX() && o.getBlockZ() == location.getBlockZ())
                    ? mock(HeadLocation.class) : null;
        });
        lenient().when(headService.addSpawnedHead(any())).thenAnswer(invocation -> {
            HeadLocation head = invocation.getArgument(0);
            occupied.add(head.getLocation());
            return true;
        });
        lenient().when(headService.removeSpawnedHead(any())).thenAnswer(invocation -> {
            HeadLocation head = invocation.getArgument(0);
            occupied.removeIf(o -> o.getBlockX() == head.getLocation().getBlockX() && o.getBlockZ() == head.getLocation().getBlockZ());
            return true;
        });

        bukkit = mockStatic(Bukkit.class);
        bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);
        bukkit.when(Bukkit::getOnlinePlayers).thenAnswer(invocation -> List.of());
        bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));

        service = new SpawnService(registry, dataFolder.toFile());
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    private FixedPositionBehavior fixed(int points, int active, int max, RespawnPolicy respawn) {
        var spawnPoints = new ArrayList<SpawnPoint>();
        for (int i = 0; i < points; i++) {
            spawnPoints.add(new SpawnPoint("world", i * 10, 64, 0, 0f));
        }
        return new FixedPositionBehavior(registry, spawnPoints, active, 10, max, SpawnCompletion.PER_PLAYER, AfterGoal.DENY,
                respawn, List.of(new SpawnTemplate("basic", "Basic", 1, HeadContent.head("tex"), List.of())));
    }

    private void useHunt(FixedPositionBehavior behavior, HuntState state) {
        hunt = new HBHunt(configService, "spawnhunt", "Spawn", state, 1, "D");
        hunt.setBehaviors(List.of(new FreeBehavior(), behavior));
        lenient().when(huntService.getAllHunts()).thenReturn(List.of(hunt));
        lenient().when(huntService.getHuntById("spawnhunt")).thenReturn(hunt);
    }

    private List<HeadLocation> spawned() {
        var captor = ArgumentCaptor.forClass(HeadLocation.class);
        verify(headService, atLeast(0)).addSpawnedHead(captor.capture());
        return captor.getAllValues();
    }

    private RespawnPolicy onFind(int delay) {
        return new RespawnPolicy(true, delay, delay, false, 3600, false, true);
    }

    @Test
    void start_activeHunt_spawnsTheActiveCountOnDistinctPoints() throws Exception {
        useHunt(fixed(5, 3, -1, onFind(0)), HuntState.ACTIVE);

        service.start();

        var heads = spawned();
        assertThat(heads).hasSize(3);
        assertThat(heads.stream().map(h -> h.getLocation().getBlockX()).distinct()).hasSize(3);
        assertThat(heads).allSatisfy(head -> {
            assertThat(head.getHuntId()).isEqualTo("spawnhunt");
            assertThat(head.getContent()).isEqualTo(HeadContent.head("tex"));
            assertThat(head.getName()).isEqualTo("Basic");
            assertThat(head.getLocation().getX() % 1).isEqualTo(0.5);
        });
        assertThat(service.getActiveHeads("spawnhunt")).hasSize(3);
        verify(storageService, atLeast(3)).createSpawnHead(any(), eq(""));
    }

    @Test
    void start_inactiveHunt_spawnsNothing() {
        useHunt(fixed(5, 3, -1, onFind(0)), HuntState.INACTIVE);

        service.start();

        assertThat(spawned()).isEmpty();
    }

    @Test
    void start_onStartDisabled_spawnsNothing() {
        useHunt(fixed(5, 3, -1, new RespawnPolicy(true, 0, 0, false, 3600, false, false)), HuntState.ACTIVE);

        service.start();

        assertThat(spawned()).isEmpty();
    }

    @Test
    void consume_isAtomic() {
        useHunt(fixed(5, 1, -1, new RespawnPolicy(false, 0, 0, false, 3600, false, true)), HuntState.ACTIVE);
        service.start();
        var head = spawned().get(0);

        assertThat(service.consume(hunt, head)).isTrue();
        assertThat(service.consume(hunt, head)).isFalse();

        verify(headService, times(1)).removeSpawnedHead(head);
        assertThat(service.getActiveHeads("spawnhunt")).isEmpty();
    }

    @Test
    void consume_noDelay_respawnsInstantlyOnAnotherPoint() {
        useHunt(fixed(2, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var first = spawned().get(0);

        service.consume(hunt, first);

        var heads = spawned();
        assertThat(heads).hasSize(2);
        assertThat(heads.get(1).getLocation().getBlockX()).isNotEqualTo(first.getLocation().getBlockX());
        assertThat(heads.get(1).getUuid()).isNotEqualTo(first.getUuid());
    }

    @Test
    void consume_singlePoint_respawnsOnTheSamePoint() {
        useHunt(fixed(1, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var first = spawned().get(0);

        service.consume(hunt, first);

        var heads = spawned();
        assertThat(heads).hasSize(2);
        assertThat(heads.get(1).getLocation().getBlockX()).isEqualTo(first.getLocation().getBlockX());
    }

    @Test
    void consume_withDelay_respawnsWhenTheDelayElapses() {
        useHunt(fixed(3, 1, -1, onFind(5)), HuntState.ACTIVE);
        service.start();

        service.consume(hunt, spawned().get(0));

        assertThat(spawned()).hasSize(1);
        assertThat(later).hasSize(1);
        assertThat(later.get(0).ticks()).isEqualTo(100L);

        later.get(0).task().run();

        assertThat(spawned()).hasSize(2);
    }

    @Test
    void consume_doesNotDeleteTheFoundRow() throws Exception {
        useHunt(fixed(3, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();

        service.consume(hunt, spawned().get(0));

        verify(storageService, never()).deleteSpawnHeads(any());
    }

    @Test
    void maxTotalSpawns_stopsSpawning() {
        useHunt(fixed(5, 1, 2, onFind(0)), HuntState.ACTIVE);
        service.start();

        service.consume(hunt, spawned().get(0));
        service.consume(hunt, spawned().get(1));

        assertThat(spawned()).hasSize(2);
    }

    @Test
    void deactivation_removesEveryHeadAndDeletesTheirRows() throws Exception {
        useHunt(fixed(5, 2, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var heads = spawned();

        hunt.setState(HuntState.INACTIVE);
        service.onStateChanged(hunt);

        heads.forEach(head -> verify(headService).removeSpawnedHead(head));
        verify(storageService).deleteSpawnHeads(argThat(uuids ->
                uuids.containsAll(heads.stream().map(HeadLocation::getUuid).toList())));
        assertThat(service.getActiveHeads("spawnhunt")).isEmpty();
    }

    @Test
    void reactivation_spawnsAgain() {
        useHunt(fixed(5, 2, -1, onFind(0)), HuntState.INACTIVE);
        service.start();

        hunt.setState(HuntState.ACTIVE);
        service.onStateChanged(hunt);

        assertThat(spawned()).hasSize(2);
    }

    @Test
    void lostHead_blocksThePointAndRetriesLater() throws Exception {
        useHunt(fixed(2, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var lost = spawned().get(0);
        occupied.clear();

        service.onLost(lost);

        verify(storageService).deleteSpawnHeads(List.of(lost.getUuid()));
        assertThat(spawned()).hasSize(1);
        assertThat(later).hasSize(1);

        later.get(0).task().run();

        var heads = spawned();
        assertThat(heads).hasSize(2);
        assertThat(heads.get(1).getLocation().getBlockX()).isNotEqualTo(lost.getLocation().getBlockX());
    }

    @Test
    void noFreePoint_retriesLater() {
        useHunt(fixed(1, 1, -1, onFind(0)), HuntState.ACTIVE);
        occupied.add(new Location(world, 0.5, 64, 0.5));

        service.start();

        assertThat(spawned()).isEmpty();
        assertThat(later).hasSize(1);
    }

    @Test
    void reroll_withReset_replacesTheHeadsAndResetsProgress() throws Exception {
        useHunt(fixed(5, 2, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var before = spawned();

        service.reroll(hunt, true);

        before.forEach(head -> verify(headService).removeSpawnedHead(head));
        verify(storageService).deletePlayerProgressForHunt("spawnhunt");
        assertThat(spawned()).hasSize(4);
        assertThat(service.getActiveHeads("spawnhunt")).hasSize(2);
    }

    @Test
    void intervalReroll_isScheduled() {
        useHunt(fixed(5, 1, -1, new RespawnPolicy(true, 0, 0, true, 60, false, true)), HuntState.ACTIVE);
        service.start();

        assertThat(timers).hasSize(2);
        timers.get(0).run();

        assertThat(spawned()).hasSize(2);
    }

    @Test
    void restart_restoresTheSameHeadsWithoutNewRows() throws Exception {
        useHunt(fixed(5, 2, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var before = spawned().stream().map(HeadLocation::getUuid).toList();
        service.stop();
        assertThat(Files.exists(dataFolder.resolve("spawns").resolve("spawnhunt.yml"))).isTrue();

        clearInvocations(headService, storageService);
        occupied.clear();
        new SpawnService(registry, dataFolder.toFile()).start();

        assertThat(spawned().stream().map(HeadLocation::getUuid).toList()).containsExactlyInAnyOrderElementsOf(before);
        verify(storageService, never()).createSpawnHead(argThat(before::contains), any());
    }

    @Test
    void restart_templateRemoved_dropsTheHeadAndItsRow() throws Exception {
        useHunt(fixed(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var head = spawned().get(0);
        service.stop();

        var renamed = new FixedPositionBehavior(registry, fixed(5, 1, -1, onFind(0)).points(), 1, 10, -1,
                SpawnCompletion.PER_PLAYER, AfterGoal.DENY, new RespawnPolicy(true, 0, 0, false, 3600, false, false),
                List.of(new SpawnTemplate("other", "", 1, HeadContent.head("x"), List.of())));
        useHunt(renamed, HuntState.ACTIVE);
        clearInvocations(headService, storageService);
        occupied.clear();

        new SpawnService(registry, dataFolder.toFile()).start();

        assertThat(spawned()).isEmpty();
        verify(storageService).deleteSpawnHeads(List.of(head.getUuid()));
    }

    @Test
    void restart_worldNotLoaded_keepsTheHeadUntilTheWorldLoads() throws Exception {
        useHunt(fixed(5, 1, -1, new RespawnPolicy(true, 0, 0, false, 3600, false, true)), HuntState.ACTIVE);
        service.start();
        var head = spawned().get(0);
        service.stop();

        bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(null);
        clearInvocations(headService, storageService);
        occupied.clear();
        var restarted = new SpawnService(registry, dataFolder.toFile());
        restarted.start();

        assertThat(spawned()).isEmpty();
        verify(storageService, never()).deleteSpawnHeads(any());

        bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);
        restarted.onWorldLoaded(world);

        assertThat(spawned()).extracting(HeadLocation::getUuid).containsExactly(head.getUuid());
    }

    @Test
    void win_closesTheHunt() throws Exception {
        useHunt(fixed(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        when(languageService.message("Messages.SpawnHuntWon")).thenReturn("%player% won %hunt%");
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Steve");

        service.win(hunt, player);

        bukkit.verify(() -> Bukkit.broadcastMessage("Steve won Spawn"));
        verify(huntService).changeState(hunt, HuntState.INACTIVE);
    }

    @Test
    void startup_purgesOrphansButKeepsActiveAndPooledRows() throws Exception {
        useHunt(fixed(5, 2, -1, onFind(0)), HuntState.ACTIVE);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.function.Supplier<Set<UUID>>> keep = ArgumentCaptor.forClass(java.util.function.Supplier.class);

        service.start();

        verify(storageService).purgeOrphanSpawnHeads(keep.capture());
        var kept = keep.getValue().get();
        assertThat(kept).containsAll(spawned().stream().map(HeadLocation::getUuid).toList());
    }

    @Test
    void discardedByAnAdmin_deletesTheRowAndRespawnsElsewhere() throws Exception {
        useHunt(fixed(3, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var head = spawned().get(0);
        occupied.clear();

        service.onDiscarded(head);

        verify(storageService).deleteSpawnHeads(List.of(head.getUuid()));
        var heads = spawned();
        assertThat(heads).hasSize(2);
        assertThat(heads.get(1).getLocation().getBlockX()).isNotEqualTo(head.getLocation().getBlockX());
    }

    @Test
    void deleteHunt_removesTheHeadsAndTheFileForGood() throws Exception {
        useHunt(fixed(5, 2, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var heads = spawned();
        service.stop();
        service.start();
        var file = dataFolder.resolve("spawns").resolve("spawnhunt.yml");
        assertThat(Files.exists(file)).isTrue();

        service.deleteHunt("spawnhunt");
        service.stop();

        heads.forEach(head -> verify(headService, atLeastOnce()).removeSpawnedHead(argThat(h -> h.getUuid().equals(head.getUuid()))));
        assertThat(Files.exists(file)).isFalse();
        assertThat(service.getActiveHeads("spawnhunt")).isEmpty();
    }

    @Test
    void reloadingTheSameService_keepsPersistingChanges() throws Exception {
        useHunt(fixed(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        service.stop();
        occupied.clear();

        service.start();
        var head = service.getActiveHeads("spawnhunt").iterator().next();
        service.consume(hunt, head);
        service.stop();

        var yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                dataFolder.resolve("spawns").resolve("spawnhunt.yml").toFile());
        assertThat(yaml.getConfigurationSection("active").getKeys(false)).doesNotContain(head.getUuid().toString());
    }

    private Path stateFile() {
        return dataFolder.resolve("spawns").resolve("spawnhunt.yml");
    }

    @Test
    void refresh_unknownActiveHunt_startsSpawning() {
        useHunt(fixed(3, 2, -1, onFind(0)), HuntState.ACTIVE);

        service.refresh(hunt);

        assertThat(spawned()).hasSize(2);
    }

    @Test
    void refresh_knownHunt_fillsTheMissingHeadsOnly() {
        useHunt(fixed(1, 2, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        assertThat(spawned()).hasSize(1);

        useHunt(fixed(3, 2, -1, onFind(0)), HuntState.ACTIVE);
        service.refresh(hunt);

        assertThat(service.getActiveHeads("spawnhunt")).hasSize(2);
    }

    @Test
    void refresh_inactiveOrNotSpawnHunt_doesNothing() {
        useHunt(fixed(3, 2, -1, onFind(0)), HuntState.INACTIVE);
        service.refresh(hunt);

        var plain = new HBHunt(configService, "plain", "Plain", HuntState.ACTIVE, 1, "D");
        service.refresh(plain);

        assertThat(spawned()).isEmpty();
    }

    @Test
    void clear_removesTheHeadsWithoutRespawning() throws Exception {
        useHunt(fixed(5, 2, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var heads = spawned();

        service.clear(hunt);

        heads.forEach(head -> verify(headService).removeSpawnedHead(head));
        verify(storageService).deleteSpawnHeads(any());
        assertThat(service.getActiveHeads("spawnhunt")).isEmpty();
        assertThat(spawned()).hasSize(2);
    }

    @Test
    void clear_unknownHunt_isNoOp() {
        useHunt(fixed(5, 2, -1, onFind(0)), HuntState.ACTIVE);

        service.clear(hunt);

        verifyNoInteractions(headService);
    }

    @Test
    void pendingRespawn_survivesARestart() {
        useHunt(fixed(3, 1, -1, onFind(30)), HuntState.ACTIVE);
        service.start();
        service.consume(hunt, spawned().get(0));
        service.stop();
        later.clear();

        new SpawnService(registry, dataFolder.toFile()).start();

        assertThat(later).hasSize(1);
        assertThat(later.get(0).ticks()).isBetween(500L, 600L);
    }

    @Test
    void flushTimer_writesTheStateFile() {
        useHunt(fixed(3, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        assertThat(Files.exists(stateFile())).isFalse();

        timers.get(timers.size() - 1).run();

        assertThat(Files.exists(stateFile())).isTrue();
    }

    @Test
    void flushTimer_nothingDirty_writesNothing() {
        var plain = new HBHunt(configService, "plain", "Plain", HuntState.ACTIVE, 1, "D");
        when(huntService.getAllHunts()).thenReturn(List.of(plain));
        service.start();

        timers.get(timers.size() - 1).run();

        assertThat(dataFolder.resolve("spawns").toFile().list()).isEmpty();
    }

    @Test
    void databaseDown_noHeadAppearsAndNothingBreaks() throws Exception {
        useHunt(fixed(3, 2, -1, onFind(0)), HuntState.ACTIVE);
        doThrow(new InternalException("down")).when(storageService).createSpawnHead(any(), any());

        service.start();

        assertThat(spawned()).isEmpty();
    }

    @Test
    void failingDeletesAndPurges_areOnlyLogged() throws Exception {
        useHunt(fixed(3, 1, -1, onFind(0)), HuntState.ACTIVE);
        doThrow(new InternalException("down")).when(storageService).deleteSpawnHeads(any());
        doThrow(new InternalException("down")).when(storageService).purgeOrphanSpawnHeads(any());

        service.start();
        service.clear(hunt);

        assertThat(service.getActiveHeads("spawnhunt")).isEmpty();
    }

    @Test
    void purge_reportsTheCleanedRows() throws Exception {
        useHunt(fixed(3, 1, -1, onFind(0)), HuntState.ACTIVE);
        when(storageService.purgeOrphanSpawnHeads(any())).thenReturn(4);

        service.start();

        verify(storageService).purgeOrphanSpawnHeads(any());
    }

    @Test
    void restart_huntNowInactive_removesTheLeftoverBlocks() throws Exception {
        useHunt(fixed(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var head = spawned().get(0);
        service.stop();

        hunt.setState(HuntState.INACTIVE);
        clearInvocations(headService, storageService);
        when(visualService.isBlockRendered(any())).thenReturn(true);
        new SpawnService(registry, dataFolder.toFile()).start();

        verify(visualService).removeBlock(argThat(h -> h.getUuid().equals(head.getUuid())));
        verify(storageService).deleteSpawnHeads(List.of(head.getUuid()));
        assertThat(spawned()).isEmpty();
    }

    @Test
    void dormantHeads_arePersistedAgain() {
        useHunt(fixed(5, 1, -1, new RespawnPolicy(true, 0, 0, false, 3600, false, true)), HuntState.ACTIVE);
        service.start();
        var head = spawned().get(0);
        service.stop();

        bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(null);
        var dormant = new SpawnService(registry, dataFolder.toFile());
        dormant.start();
        dormant.stop();

        bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);
        clearInvocations(headService);
        occupied.clear();
        new SpawnService(registry, dataFolder.toFile()).start();

        assertThat(spawned()).extracting(HeadLocation::getUuid).containsExactly(head.getUuid());
    }

    @Test
    void corruptedStateFile_isIgnored() throws Exception {
        useHunt(fixed(3, 1, -1, onFind(0)), HuntState.ACTIVE);
        Files.createDirectories(stateFile().getParent());
        Files.writeString(stateFile(), String.join(System.lineSeparator(),
                "active:", "  not-a-uuid:", "    template: basic", "    world: world", "pool:", "  - also-bad", ""));

        service.start();

        assertThat(spawned()).hasSize(1);
    }

    @Test
    void win_cancelledEvent_keepsTheHuntOpen() throws Exception {
        useHunt(fixed(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        PluginManager pluginManager = mock(PluginManager.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
        doAnswer(invocation -> {
            ((HuntStateChangeEvent) invocation.getArgument(0)).setCancelled(true);
            return null;
        }).when(pluginManager).callEvent(any());

        service.win(hunt, mock(Player.class));

        verify(huntService, never()).changeState(any(), any());
    }

    @Test
    void win_storageError_isOnlyLogged() throws Exception {
        useHunt(fixed(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        when(languageService.message("Messages.SpawnHuntWon")).thenReturn("");
        doThrow(new InternalException("down")).when(huntService).changeState(any(), any());
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Steve");

        service.win(hunt, player);

        bukkit.verify(() -> Bukkit.broadcastMessage(anyString()), never());
    }

    @Test
    void unknownHeads_areIgnored() {
        useHunt(fixed(3, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var stranger = new HeadLocation("", UUID.randomUUID(), new Location(world, 0.5, 64, 0.5), "spawnhunt");

        service.onLost(stranger);
        service.onDiscarded(stranger);

        assertThat(service.consume(hunt, stranger)).isFalse();
        assertThat(spawned()).hasSize(1);
    }

    @Test
    void discard_withoutRespawnOnFind_leavesTheSlotEmpty() {
        useHunt(fixed(3, 1, -1, new RespawnPolicy(false, 0, 0, false, 3600, false, true)), HuntState.ACTIVE);
        service.start();

        service.onDiscarded(spawned().get(0));

        assertThat(spawned()).hasSize(1);
        assertThat(later).isEmpty();
    }

    @Test
    void notSpawnHunts_areIgnoredEverywhere() {
        var plain = new HBHunt(configService, "plain", "Plain", HuntState.ACTIVE, 1, "D");
        when(huntService.getAllHunts()).thenReturn(List.of(plain));

        service.start();
        service.onStateChanged(plain);
        service.reroll(plain, true);
        service.deleteHunt("plain");

        assertThat(spawned()).isEmpty();
    }

    @Test
    void reconfigure_appliesTheNewActiveCount() {
        useHunt(fixed(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();

        useHunt(fixed(5, 3, -1, onFind(0)), HuntState.ACTIVE);
        service.reconfigure(hunt);

        assertThat(service.getActiveHeads("spawnhunt")).hasSize(3);
    }

    @Test
    void onWorldLoaded_otherWorld_keepsTheHeadsDormant() {
        useHunt(fixed(5, 1, -1, new RespawnPolicy(true, 0, 0, false, 3600, false, true)), HuntState.ACTIVE);
        service.start();
        service.stop();
        bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(null);
        var restarted = new SpawnService(registry, dataFolder.toFile());
        restarted.start();
        clearInvocations(headService);

        World other = mock(World.class);
        when(other.getName()).thenReturn("nether");
        restarted.onWorldLoaded(other);

        assertThat(spawned()).isEmpty();
    }

    @Test
    void restart_keepsTheRenderModeTheHeadAppearedWith() {
        when(configService.renderingMode()).thenReturn(RenderMode.BLOCK);
        useHunt(fixed(5, 1, -1, new RespawnPolicy(true, 0, 0, false, 3600, false, true)), HuntState.ACTIVE);
        service.start();
        service.stop();

        when(configService.renderingMode()).thenReturn(RenderMode.DISPLAY);
        clearInvocations(headService);
        occupied.clear();
        new SpawnService(registry, dataFolder.toFile()).start();

        assertThat(spawned()).singleElement().extracting(HeadLocation::getRenderMode).isEqualTo(RenderMode.BLOCK);
    }

    @Test
    void consumedRow_isKeptFromThePurgeUntilTheProgressIsStored() throws Exception {
        useHunt(fixed(3, 1, -1, new RespawnPolicy(false, 0, 0, false, 3600, false, true)), HuntState.ACTIVE);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.function.Supplier<Set<UUID>>> keep = ArgumentCaptor.forClass(java.util.function.Supplier.class);
        service.start();
        verify(storageService).purgeOrphanSpawnHeads(keep.capture());
        var head = spawned().get(0);

        service.consume(hunt, head);
        assertThat(keep.getValue().get()).contains(head.getUuid());

        service.release(hunt, head);
        assertThat(keep.getValue().get()).doesNotContain(head.getUuid());
    }

    @Test
    void rerollWithReset_spawnsOnlyAfterTheProgressIsDeleted() throws Exception {
        useHunt(fixed(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        List<Runnable> asyncTasks = new ArrayList<>();
        doAnswer(invocation -> {
            asyncTasks.add(invocation.getArgument(0));
            return mock(Task.class);
        }).when(scheduler).runTaskAsync(any(Runnable.class));

        service.reroll(hunt, true);

        assertThat(service.getActiveHeads("spawnhunt")).isEmpty();
        verify(storageService, never()).deletePlayerProgressForHunt(any());

        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return mock(Task.class);
        }).when(scheduler).runTaskAsync(any(Runnable.class));
        new ArrayList<>(asyncTasks).forEach(Runnable::run);

        verify(storageService).deletePlayerProgressForHunt("spawnhunt");
        assertThat(service.getActiveHeads("spawnhunt")).hasSize(1);
    }

    @Test
    void stop_duringAnAsyncRowCreation_neverRegistersAGhostHead() {
        useHunt(fixed(5, 1, -1, new RespawnPolicy(true, 0, 0, false, 3600, false, false)), HuntState.ACTIVE);
        List<Runnable> asyncTasks = new ArrayList<>();
        doAnswer(invocation -> {
            asyncTasks.add(invocation.getArgument(0));
            return mock(Task.class);
        }).when(scheduler).runTaskAsync(any(Runnable.class));
        service.start();

        service.reroll(hunt, false);
        service.stop();
        new ArrayList<>(asyncTasks).forEach(Runnable::run);

        assertThat(spawned()).isEmpty();
    }

    @Test
    void restart_huntNoLongerUsingSpawns_removesItsHeadsAndFile() {
        useHunt(fixed(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var head = spawned().get(0);
        service.stop();
        assertThat(Files.exists(stateFile())).isTrue();

        hunt.setBehaviors(List.of(new FreeBehavior()));
        when(visualService.isBlockRendered(any())).thenReturn(true);
        new SpawnService(registry, dataFolder.toFile()).start();

        verify(visualService).removeBlock(argThat(h -> h.getUuid().equals(head.getUuid())));
        assertThat(Files.exists(stateFile())).isFalse();
    }
}
