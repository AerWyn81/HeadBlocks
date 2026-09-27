package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.api.events.HuntStateChangeEvent;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.data.head.visual.RenderMode;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntConfig;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.data.hunt.behavior.FreeBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.SpawnBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.SpawnBehaviors;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Mock
    Player player;

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
            if (occupied.stream().anyMatch(o -> o.getBlockX() == head.getLocation().getBlockX() && o.getBlockZ() == head.getLocation().getBlockZ())) {
                return false;
            }
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
        service.stop();
        bukkit.close();
    }

    private SpawnBehavior spawnPoints(int points, int active, int max, RespawnPolicy respawn) {
        var spawnPoints = new ArrayList<SpawnPoint>();
        for (int i = 0; i < points; i++) {
            spawnPoints.add(new SpawnPoint("world", i * 10, 64, 0, 0f));
        }
        return SpawnBehaviors.points(registry, spawnPoints, active, 10, max, SpawnCompletion.PER_PLAYER, AfterGoal.DENY,
                respawn, List.of(new SpawnTemplate("basic", "Basic", 1, HeadContent.head("tex"), List.of())));
    }

    private void useHunt(fr.aerwyn81.headblocks.data.hunt.behavior.SpawnBehavior behavior, HuntState state) {
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

    private RespawnPolicy noRespawn() {
        return new RespawnPolicy(false, 0, 0, false, 3600, false, true);
    }

    private Path stateFile() {
        return dataFolder.resolve("spawns").resolve("spawnhunt.yml");
    }

    private SpawnService restart() {
        service.stop();
        clearInvocations(headService, storageService, visualService);
        occupied.clear();
        later.clear();
        service = new SpawnService(registry, dataFolder.toFile());
        service.start();
        return service;
    }

    // --- Spawning ---

    @Test
    void start_activeHunt_spawnsTheActiveCountOnDistinctPoints_withoutTouchingTheDatabase() throws Exception {
        useHunt(spawnPoints(5, 3, -1, onFind(0)), HuntState.ACTIVE);

        service.start();

        var heads = spawned();
        assertThat(heads).hasSize(3);
        assertThat(heads.stream().map(h -> h.getLocation().getBlockX()).distinct()).hasSize(3);
        assertThat(heads).allSatisfy(head -> {
            assertThat(head.getHuntId()).isEqualTo("spawnhunt");
            assertThat(head.getContent()).isEqualTo(HeadContent.head("tex"));
            assertThat(head.getName()).isEqualTo("Basic");
            assertThat(head.getLocation().getX() % 1).isZero();
        });
        verify(storageService, never()).createSpawnHead(any(), any(), anyDouble());
    }

    @Test
    void start_purgesOrphanRowsBeforeAnyHeadAppears() throws Exception {
        useHunt(spawnPoints(5, 1, -1, onFind(0)), HuntState.ACTIVE);

        service.start();

        var order = inOrder(storageService, headService);
        order.verify(storageService).purgeOrphanSpawnHeads();
        order.verify(headService).addSpawnedHead(any());
    }

    @Test
    void start_purgeFailing_isOnlyLogged() throws Exception {
        useHunt(spawnPoints(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        when(storageService.purgeOrphanSpawnHeads()).thenThrow(new InternalException("down"));

        service.start();

        assertThat(spawned()).hasSize(1);
    }

    @Test
    void start_purgeReportsCleanedRows() throws Exception {
        useHunt(spawnPoints(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        when(storageService.purgeOrphanSpawnHeads()).thenReturn(4);

        service.start();

        verify(storageService).purgeOrphanSpawnHeads();
    }

    @Test
    void start_inactiveHunt_spawnsNothing() {
        useHunt(spawnPoints(5, 3, -1, onFind(0)), HuntState.INACTIVE);

        service.start();

        assertThat(spawned()).isEmpty();
    }

    @Test
    void start_onStartDisabled_spawnsNothing() {
        useHunt(spawnPoints(5, 3, -1, new RespawnPolicy(true, 0, 0, false, 3600, false, false)), HuntState.ACTIVE);

        service.start();

        assertThat(spawned()).isEmpty();
    }

    @Test
    void noFreePoint_retriesLater() {
        useHunt(spawnPoints(1, 1, -1, onFind(0)), HuntState.ACTIVE);
        occupied.add(new Location(world, 0.5, 64, 0.5));

        service.start();

        assertThat(spawned()).isEmpty();
        assertThat(later).hasSize(1);
    }

    @Test
    void refusedByTheHeadService_retriesLater() {
        useHunt(spawnPoints(1, 1, -1, onFind(0)), HuntState.ACTIVE);
        doReturn(false).when(headService).addSpawnedHead(any());

        service.start();

        assertThat(service.getActiveHeads("spawnhunt")).isEmpty();
        assertThat(later).hasSize(1);
    }

    @Test
    void noTemplateWithWeight_spawnsNothing() {
        hunt = new HBHunt(configService, "spawnhunt", "Spawn", HuntState.ACTIVE, 1, "D");
        hunt.setBehaviors(List.of(SpawnBehaviors.points(registry, List.of(new SpawnPoint("world", 0, 64, 0, 0f)), 1, 1, -1,
                SpawnCompletion.PER_PLAYER, AfterGoal.DENY, RespawnPolicy.DEFAULT, List.of())));
        when(huntService.getAllHunts()).thenReturn(List.of(hunt));
        when(huntService.getHuntById("spawnhunt")).thenReturn(hunt);

        service.start();

        assertThat(spawned()).isEmpty();
        assertThat(later).isEmpty();
    }

    // --- Finding ---

    @Test
    void claim_isAtomic_andStoresTheRowOnce() throws Exception {
        useHunt(spawnPoints(5, 1, -1, noRespawn()), HuntState.ACTIVE);
        service.start();
        var head = spawned().get(0);

        assertThat(service.claim(hunt, head, player)).isEqualTo(ClaimOutcome.FOUND);
        assertThat(service.claim(hunt, head, player)).isEqualTo(ClaimOutcome.TAKEN);
        service.storeFound(head);
        service.storeFound(head);

        verify(storageService, times(1)).createSpawnHead(head.getUuid(), "tex", 1.0);
        verify(headService, times(1)).removeSpawnedHead(head);
        assertThat(service.getActiveHeads("spawnhunt")).isEmpty();
    }

    @Test
    void claim_doesNotTouchTheDatabase_theRowIsStoredLater() throws Exception {
        useHunt(spawnPoints(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var head = spawned().get(0);

        service.claim(hunt, head, player);
        verify(storageService, never()).createSpawnHead(any(), any(), anyDouble());

        doThrow(new InternalException("down")).when(storageService).createSpawnHead(any(), any(), anyDouble());
        assertThatThrownBy(() -> service.storeFound(head)).isInstanceOf(InternalException.class);
    }

    @Test
    void storeFound_placedHead_writesNothing() throws Exception {
        var placed = mock(HeadLocation.class);
        when(placed.getUuid()).thenReturn(UUID.randomUUID());

        service.storeFound(placed);

        verify(storageService, never()).createSpawnHead(any(), any(), anyDouble());
    }

    @Test
    void claim_noDelay_respawnsInstantlyOnAnotherPoint() {
        useHunt(spawnPoints(2, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var first = spawned().get(0);

        service.claim(hunt, first, player);

        var heads = spawned();
        assertThat(heads).hasSize(2);
        assertThat(heads.get(1).getLocation().getBlockX()).isNotEqualTo(first.getLocation().getBlockX());
        assertThat(heads.get(1).getUuid()).isNotEqualTo(first.getUuid());
    }

    @Test
    void claim_singlePoint_respawnsOnTheSamePoint() {
        useHunt(spawnPoints(1, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var first = spawned().get(0);

        service.claim(hunt, first, player);

        var heads = spawned();
        assertThat(heads).hasSize(2);
        assertThat(heads.get(1).getLocation().getBlockX()).isEqualTo(first.getLocation().getBlockX());
    }

    @Test
    void claim_withDelay_respawnsWhenTheDelayElapses() {
        useHunt(spawnPoints(3, 1, -1, onFind(5)), HuntState.ACTIVE);
        service.start();

        service.claim(hunt, spawned().get(0), player);

        assertThat(spawned()).hasSize(1);
        assertThat(later).singleElement().extracting(Delayed::ticks).isEqualTo(100L);

        later.get(0).task().run();

        assertThat(spawned()).hasSize(2);
    }

    @Test
    void maxTotalSpawns_stopsSpawning() {
        useHunt(spawnPoints(5, 1, 2, onFind(0)), HuntState.ACTIVE);
        service.start();

        service.claim(hunt, spawned().get(0), player);
        service.claim(hunt, spawned().get(1), player);

        assertThat(spawned()).hasSize(2);
    }

    @Test
    void unknownHeads_areIgnored() {
        useHunt(spawnPoints(3, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var stranger = new HeadLocation("", UUID.randomUUID(), new Location(world, 0.5, 64, 0.5), "spawnhunt");

        service.onLost(stranger);
        service.onDiscarded(stranger);

        assertThat(service.claim(hunt, stranger, player)).isEqualTo(ClaimOutcome.TAKEN);
        assertThat(spawned()).hasSize(1);
    }

    // --- Lost and discarded heads ---

    @Test
    void lostHead_retriesLaterPreferringAnotherPoint() {
        useHunt(spawnPoints(2, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var lost = spawned().get(0);
        occupied.clear();

        service.onLost(lost);

        assertThat(spawned()).hasSize(1);
        assertThat(later).hasSize(1);

        later.get(0).task().run();

        var heads = spawned();
        assertThat(heads).hasSize(2);
        assertThat(heads.get(1).getLocation().getBlockX()).isNotEqualTo(lost.getLocation().getBlockX());
    }

    @Test
    void discardedByAnAdmin_respawnsElsewhere() {
        useHunt(spawnPoints(3, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var head = spawned().get(0);
        occupied.clear();

        service.onDiscarded(head);

        var heads = spawned();
        assertThat(heads).hasSize(2);
        assertThat(heads.get(1).getLocation().getBlockX()).isNotEqualTo(head.getLocation().getBlockX());
    }

    @Test
    void discard_withoutRespawnOnFind_leavesTheSlotEmpty() {
        useHunt(spawnPoints(3, 1, -1, noRespawn()), HuntState.ACTIVE);
        service.start();

        service.onDiscarded(spawned().get(0));

        assertThat(spawned()).hasSize(1);
        assertThat(later).isEmpty();
    }

    // --- Hunt state and commands ---

    @Test
    void deactivation_removesEveryHead() {
        useHunt(spawnPoints(5, 2, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var heads = spawned();

        hunt.setState(HuntState.INACTIVE);
        service.onStateChanged(hunt);

        heads.forEach(head -> verify(headService).removeSpawnedHead(head));
        assertThat(service.getActiveHeads("spawnhunt")).isEmpty();
    }

    @Test
    void reactivation_spawnsAgain() {
        useHunt(spawnPoints(5, 2, -1, onFind(0)), HuntState.INACTIVE);
        service.start();

        hunt.setState(HuntState.ACTIVE);
        service.onStateChanged(hunt);

        assertThat(spawned()).hasSize(2);
    }

    @Test
    void reactivation_keepsTheProgressByDefault() throws Exception {
        useHunt(spawnPoints(5, 2, -1, onFind(0)), HuntState.INACTIVE);
        service.start();

        hunt.setState(HuntState.ACTIVE);
        service.onStateChanged(hunt);

        verify(storageService, never()).deletePlayerProgressForHunt(any());
    }

    @Test
    void reactivation_withResetOnActivate_resetsTheProgressThenSpawns() throws Exception {
        useHunt(withTemplate(5, 2, basic(), new SpawnOptions(false, false, false, SpawnOptions.Scoring.HEADS, true)),
                HuntState.INACTIVE);
        service.start();

        hunt.setState(HuntState.ACTIVE);
        service.onStateChanged(hunt);

        var order = inOrder(storageService, headService);
        order.verify(storageService).deletePlayerProgressForHunt("spawnhunt");
        order.verify(headService, times(2)).addSpawnedHead(any());
    }

    @Test
    void deactivation_withResetOnActivate_keepsTheProgress() throws Exception {
        useHunt(withTemplate(5, 2, basic(), new SpawnOptions(false, false, false, SpawnOptions.Scoring.HEADS, true)),
                HuntState.ACTIVE);
        service.start();

        hunt.setState(HuntState.INACTIVE);
        service.onStateChanged(hunt);

        verify(storageService, never()).deletePlayerProgressForHunt(any());
        assertThat(service.getActiveHeads("spawnhunt")).isEmpty();
    }

    @Test
    void reconfigure_appliesTheNewActiveCount() {
        useHunt(spawnPoints(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();

        useHunt(spawnPoints(5, 3, -1, onFind(0)), HuntState.ACTIVE);
        service.reconfigure(hunt);

        assertThat(service.getActiveHeads("spawnhunt")).hasSize(3);
    }

    @Test
    void reroll_withoutReset_replacesTheHeads() throws Exception {
        useHunt(spawnPoints(5, 2, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var before = spawned();

        service.reroll(hunt, false);

        before.forEach(head -> verify(headService).removeSpawnedHead(head));
        verify(storageService, never()).deletePlayerProgressForHunt(any());
        assertThat(service.getActiveHeads("spawnhunt")).hasSize(2);
    }

    @Test
    void reroll_withReset_spawnsOnlyAfterTheProgressIsDeleted() throws Exception {
        useHunt(spawnPoints(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        List<Runnable> asyncTasks = new ArrayList<>();
        doAnswer(invocation -> {
            asyncTasks.add(invocation.getArgument(0));
            return mock(Task.class);
        }).when(scheduler).runTaskAsync(any(Runnable.class));

        service.reroll(hunt, true);

        assertThat(service.getActiveHeads("spawnhunt")).isEmpty();
        verify(storageService, never()).deletePlayerProgressForHunt(any());

        asyncTasks.forEach(Runnable::run);

        verify(storageService).deletePlayerProgressForHunt("spawnhunt");
        assertThat(service.getActiveHeads("spawnhunt")).hasSize(1);
    }

    @Test
    void reroll_withReset_afterAReload_neverRegistersAGhostHead() throws Exception {
        useHunt(spawnPoints(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        List<Runnable> asyncTasks = new ArrayList<>();
        doAnswer(invocation -> {
            asyncTasks.add(invocation.getArgument(0));
            return mock(Task.class);
        }).when(scheduler).runTaskAsync(any(Runnable.class));

        service.reroll(hunt, true);
        service.stop();
        clearInvocations(headService);
        asyncTasks.forEach(Runnable::run);

        assertThat(spawned()).isEmpty();
    }

    @Test
    void reroll_resetFailing_stillSpawns() throws Exception {
        useHunt(spawnPoints(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        doThrow(new InternalException("down")).when(storageService).deletePlayerProgressForHunt(any());

        service.reroll(hunt, true);

        assertThat(service.getActiveHeads("spawnhunt")).hasSize(1);
    }

    @Test
    void intervalRedraw_isScheduledAndRuns() {
        useHunt(spawnPoints(5, 1, -1, new RespawnPolicy(true, 0, 0, true, 60, false, true)), HuntState.ACTIVE);
        service.start();

        timers.get(0).run();

        assertThat(spawned()).hasSize(2);
        assertThat(service.getActiveHeads("spawnhunt")).hasSize(1);
    }

    @Test
    void refresh_unknownActiveHunt_startsSpawning() {
        useHunt(spawnPoints(3, 2, -1, onFind(0)), HuntState.ACTIVE);

        service.refresh(hunt);

        assertThat(spawned()).hasSize(2);
    }

    @Test
    void refresh_knownHunt_fillsTheMissingHeads() {
        useHunt(spawnPoints(1, 2, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        assertThat(spawned()).hasSize(1);

        useHunt(spawnPoints(3, 2, -1, onFind(0)), HuntState.ACTIVE);
        service.refresh(hunt);

        assertThat(service.getActiveHeads("spawnhunt")).hasSize(2);
    }

    @Test
    void refresh_inactiveOrNotSpawnHunt_doesNothing() {
        useHunt(spawnPoints(3, 2, -1, onFind(0)), HuntState.INACTIVE);
        service.refresh(hunt);

        service.refresh(new HBHunt(configService, "plain", "Plain", HuntState.ACTIVE, 1, "D"));

        assertThat(spawned()).isEmpty();
    }

    @Test
    void clear_removesTheHeadsWithoutRespawning() {
        useHunt(spawnPoints(5, 2, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var heads = spawned();

        service.clear(hunt);

        heads.forEach(head -> verify(headService).removeSpawnedHead(head));
        assertThat(service.getActiveHeads("spawnhunt")).isEmpty();
        assertThat(spawned()).hasSize(2);
    }

    @Test
    void clear_unknownHunt_isNoOp() {
        useHunt(spawnPoints(5, 2, -1, onFind(0)), HuntState.ACTIVE);

        service.clear(hunt);

        verifyNoInteractions(headService);
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
    void deleteHunt_removesTheHeadsAndTheFileForGood() {
        useHunt(spawnPoints(5, 2, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var heads = spawned();
        restart();
        assertThat(Files.exists(stateFile())).isTrue();

        service.deleteHunt("spawnhunt");
        service.stop();

        heads.forEach(head -> verify(headService).removeSpawnedHead(argThat(h -> h.getUuid().equals(head.getUuid()))));
        assertThat(Files.exists(stateFile())).isFalse();
    }

    // --- Winning ---

    @Test
    void win_closesTheHunt() throws Exception {
        useHunt(spawnPoints(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        when(languageService.message("Messages.SpawnHuntWon")).thenReturn("%player% won %hunt%");
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Steve");

        service.win(hunt, player);

        bukkit.verify(() -> Bukkit.broadcastMessage("Steve won Spawn"));
        verify(huntService).changeState(hunt, HuntState.INACTIVE);
    }

    @Test
    void win_cancelledEvent_keepsTheHuntOpen() throws Exception {
        useHunt(spawnPoints(5, 1, -1, onFind(0)), HuntState.ACTIVE);
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
        useHunt(spawnPoints(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        when(languageService.message("Messages.SpawnHuntWon")).thenReturn("");
        doThrow(new InternalException("down")).when(huntService).changeState(any(), any());
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Steve");

        service.win(hunt, player);

        bukkit.verify(() -> Bukkit.broadcastMessage(anyString()), never());
    }

    // --- Persistence ---

    @Test
    void restart_restoresTheSameHeads() throws Exception {
        useHunt(spawnPoints(5, 2, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var before = spawned().stream().map(HeadLocation::getUuid).toList();

        restart();

        assertThat(spawned().stream().map(HeadLocation::getUuid).toList()).containsExactlyInAnyOrderElementsOf(before);
        verify(storageService, never()).createSpawnHead(any(), any(), anyDouble());
    }

    @Test
    void restart_keepsTheTotalSpawnedForTheLimit() {
        useHunt(spawnPoints(5, 1, 2, onFind(0)), HuntState.ACTIVE);
        service.start();
        service.claim(hunt, spawned().get(0), player);

        restart();
        service.claim(hunt, spawned().get(0), player);

        assertThat(spawned()).hasSize(1);
    }

    @Test
    void restart_keepsTheRenderModeTheHeadAppearedWith() {
        when(configService.renderingMode()).thenReturn(RenderMode.BLOCK);
        useHunt(spawnPoints(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        when(configService.renderingMode()).thenReturn(RenderMode.DISPLAY);

        restart();

        assertThat(spawned()).singleElement().extracting(HeadLocation::getRenderMode).isEqualTo(RenderMode.BLOCK);
    }

    @Test
    void restart_templateRemoved_replacesTheHeadWithAnExistingTemplate() {
        useHunt(spawnPoints(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var head = spawned().get(0);
        service.stop();

        useHunt(SpawnBehaviors.points(registry, spawnPoints(5, 1, -1, onFind(0)).points(), 1, 10, -1,
                SpawnCompletion.PER_PLAYER, AfterGoal.DENY, new RespawnPolicy(true, 0, 0, false, 3600, false, false),
                List.of(new SpawnTemplate("other", "", 1, HeadContent.head("x"), List.of()))), HuntState.ACTIVE);
        when(visualService.isBlockRendered(any())).thenReturn(true);
        restart();

        verify(visualService).removeBlock(argThat(h -> h.getUuid().equals(head.getUuid())));
        assertThat(spawned()).singleElement().satisfies(replacement -> {
            assertThat(replacement.getUuid()).isNotEqualTo(head.getUuid());
            assertThat(replacement.getContent()).isEqualTo(HeadContent.head("x"));
        });
    }

    @Test
    void restart_huntNowInactive_removesTheLeftoverBlock() {
        useHunt(spawnPoints(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var head = spawned().get(0);
        service.stop();

        hunt.setState(HuntState.INACTIVE);
        when(visualService.isBlockRendered(any())).thenReturn(true);
        restart();

        assertThat(spawned()).isEmpty();
        verify(visualService).removeBlock(argThat(h -> h.getUuid().equals(head.getUuid())));
    }

    @Test
    void restart_huntNoLongerUsingSpawns_removesItsHeadsAndFile() {
        useHunt(spawnPoints(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var head = spawned().get(0);
        service.stop();

        hunt.setBehaviors(List.of(new FreeBehavior()));
        when(visualService.isBlockRendered(any())).thenReturn(true);
        restart();

        verify(visualService).removeBlock(argThat(h -> h.getUuid().equals(head.getUuid())));
        assertThat(Files.exists(stateFile())).isFalse();
    }

    @Test
    void restart_worldNotLoaded_keepsTheHeadUntilTheWorldLoads() {
        useHunt(spawnPoints(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var head = spawned().get(0);

        bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(null);
        restart();
        assertThat(spawned()).isEmpty();

        bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);
        service.onWorldLoaded(world);

        assertThat(spawned()).extracting(HeadLocation::getUuid).containsExactly(head.getUuid());
    }

    @Test
    void restart_dormantHeads_arePersistedAgain() {
        useHunt(spawnPoints(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var head = spawned().get(0);

        bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(null);
        restart();
        bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);
        restart();

        assertThat(spawned()).extracting(HeadLocation::getUuid).containsExactly(head.getUuid());
    }

    @Test
    void onWorldLoaded_otherWorld_keepsTheHeadsDormant() {
        useHunt(spawnPoints(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(null);
        restart();

        World other = mock(World.class);
        when(other.getName()).thenReturn("nether");
        service.onWorldLoaded(other);

        assertThat(spawned()).isEmpty();
    }

    @Test
    void corruptedStateFile_invalidEntriesAreIgnored() throws Exception {
        useHunt(spawnPoints(3, 1, -1, onFind(0)), HuntState.ACTIVE);
        Files.createDirectories(stateFile().getParent());
        Files.writeString(stateFile(), String.join(System.lineSeparator(),
                "pending: 1", "active:", "  not-a-uuid:", "    template: basic", "    world: world", ""));

        service.start();

        assertThat(spawned()).hasSize(1);
    }

    @Test
    void flushTimer_writesTheChangedStates() {
        useHunt(spawnPoints(3, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();

        timers.get(timers.size() - 1).run();
        service.stop();

        assertThat(Files.exists(stateFile())).isTrue();
    }

    @Test
    void reloadingTheSameService_keepsPersistingChanges() {
        useHunt(spawnPoints(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        service.stop();
        occupied.clear();

        service.start();
        var head = service.getActiveHeads("spawnhunt").iterator().next();
        service.claim(hunt, head, player);
        service.stop();

        var yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(stateFile().toFile());
        assertThat(yaml.getConfigurationSection("active").getKeys(false)).doesNotContain(head.getUuid().toString());
    }

    @Test
    void restart_waitingRespawn_comesBackEvenWithoutSpawnOnStart() {
        useHunt(spawnPoints(5, 2, -1, new RespawnPolicy(true, 60, 60, false, 3600, false, true)), HuntState.ACTIVE);
        service.start();
        service.claim(hunt, spawned().get(0), player);
        useHunt(spawnPoints(5, 2, -1, new RespawnPolicy(true, 60, 60, false, 3600, false, false)), HuntState.ACTIVE);

        restart();

        assertThat(spawned()).hasSize(2);
        assertThat(service.getActiveHeads("spawnhunt")).hasSize(2);
    }

    @Test
    void restart_allHeadsPresent_spawnsNothingNew() {
        useHunt(spawnPoints(5, 3, -1, onFind(60)), HuntState.ACTIVE);
        service.start();
        var before = spawned().stream().map(HeadLocation::getUuid).toList();

        restart();

        assertThat(spawned()).extracting(HeadLocation::getUuid).containsExactlyInAnyOrderElementsOf(before);
    }

    @Test
    void restart_foundHeadWithoutRespawnOnFind_isNotRevived() {
        useHunt(spawnPoints(5, 2, -1, noRespawn()), HuntState.ACTIVE);
        service.start();
        service.claim(hunt, spawned().get(0), player);

        restart();

        assertThat(service.getActiveHeads("spawnhunt")).hasSize(1);
    }

    @Test
    void restart_spotTakenMeanwhile_headComesBackElsewhere() {
        useHunt(spawnPoints(3, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var head = spawned().get(0);
        service.stop();

        clearInvocations(headService);
        occupied.clear();
        occupied.add(head.getLocation());
        service = new SpawnService(registry, dataFolder.toFile());
        service.start();

        assertThat(service.getActiveHeads("spawnhunt")).singleElement().satisfies(replacement ->
                assertThat(replacement.getLocation().getBlockX()).isNotEqualTo(head.getLocation().getBlockX()));
    }

    @Test
    void restart_waitingSlots_areCappedByTheActiveCount() {
        useHunt(spawnPoints(5, 2, -1, onFind(60)), HuntState.ACTIVE);
        service.start();
        service.claim(hunt, spawned().get(0), player);
        useHunt(spawnPoints(5, 1, -1, onFind(60)), HuntState.ACTIVE);

        restart();

        assertThat(service.getActiveHeads("spawnhunt")).hasSize(1);
    }

    @Test
    void reconfigure_keepsTheWaitingRespawns_withoutSpawnOnStart() {
        var policy = new RespawnPolicy(true, 60, 60, false, 3600, false, true);
        useHunt(spawnPoints(5, 2, -1, policy), HuntState.ACTIVE);
        service.start();
        service.claim(hunt, spawned().get(0), player);

        useHunt(spawnPoints(5, 2, -1, new RespawnPolicy(true, 60, 60, false, 3600, false, false)), HuntState.ACTIVE);
        service.reconfigure(hunt);

        assertThat(service.getActiveHeads("spawnhunt")).hasSize(2);
    }

    @Test
    void onWorldLoaded_templateRemoved_spawnsAReplacement() {
        useHunt(spawnPoints(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        var head = spawned().get(0);
        bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(null);
        restart();

        useHunt(SpawnBehaviors.points(registry, spawnPoints(5, 1, -1, onFind(0)).points(), 1, 10, -1,
                SpawnCompletion.PER_PLAYER, AfterGoal.DENY, onFind(0),
                List.of(new SpawnTemplate("other", "", 1, HeadContent.head("x"), List.of()))), HuntState.ACTIVE);
        bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);
        service.onWorldLoaded(world);

        assertThat(service.getActiveHeads("spawnhunt")).singleElement().satisfies(replacement ->
                assertThat(replacement.getUuid()).isNotEqualTo(head.getUuid()));
    }

    // --- Templates, announcements, extra heads, logs ---

    private SpawnBehavior withTemplate(int points, int active, SpawnTemplate template, SpawnOptions options) {
        var spawnPoints = new ArrayList<SpawnPoint>();
        for (int i = 0; i < points; i++) {
            spawnPoints.add(new SpawnPoint("world", i * 10, 64, 0, 0f));
        }
        return SpawnBehaviors.points(registry, spawnPoints, active, 10, -1, SpawnCompletion.PER_PLAYER, AfterGoal.DENY,
                onFind(0), options, List.of(template));
    }

    private SpawnTemplate basic() {
        return new SpawnTemplate("basic", "Basic", 1, HeadContent.head("tex"), List.of());
    }

    @Test
    void trap_breaksTheHeadWithoutStoringItAndRunsTheCommands() throws Exception {
        var dispatcher = mock(fr.aerwyn81.headblocks.utils.bukkit.CommandDispatcher.class);
        var placeholders = mock(PlaceholdersService.class);
        when(registry.getCommandDispatcher()).thenReturn(dispatcher);
        when(registry.getPlaceholdersService()).thenReturn(placeholders);
        when(placeholders.parse(any(), any(), any(), anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(3));
        when(player.getName()).thenReturn("Steve");
        useHunt(withTemplate(3, 1, basic().withTrap(100, List.of("say boom")), SpawnOptions.DEFAULT), HuntState.ACTIVE);
        service.start();
        var head = spawned().get(0);

        assertThat(service.claim(hunt, head, player)).isEqualTo(ClaimOutcome.TRAPPED);

        verify(dispatcher).dispatchConsoleCommand("say boom");
        verify(storageService, never()).createSpawnHead(any(), any(), anyDouble());
        verify(headService).removeSpawnedHead(head);
        assertThat(spawned()).hasSize(2);
    }

    @Test
    void rewards_areDrawnWhenTheHeadAppears() {
        var rewards = List.of(new fr.aerwyn81.headblocks.data.reward.Reward(fr.aerwyn81.headblocks.data.reward.RewardType.MESSAGE, "a"),
                new fr.aerwyn81.headblocks.data.reward.Reward(fr.aerwyn81.headblocks.data.reward.RewardType.MESSAGE, "b"),
                new fr.aerwyn81.headblocks.data.reward.Reward(fr.aerwyn81.headblocks.data.reward.RewardType.MESSAGE, "c"));
        useHunt(withTemplate(3, 1, basic().withRewards(rewards).withRewardDraw(true, 100), SpawnOptions.DEFAULT), HuntState.ACTIVE);

        service.start();

        assertThat(spawned().get(0).getRewards()).hasSize(1).isSubsetOf(rewards);
    }

    @Test
    void rewards_zeroChance_giveNothing() {
        var reward = new fr.aerwyn81.headblocks.data.reward.Reward(fr.aerwyn81.headblocks.data.reward.RewardType.MESSAGE, "a");
        useHunt(withTemplate(3, 1, basic().withRewards(List.of(reward)).withRewardDraw(false, 0), SpawnOptions.DEFAULT), HuntState.ACTIVE);

        service.start();

        assertThat(spawned().get(0).getRewards()).isEmpty();
    }

    @Test
    void announce_broadcastsTheFirstFillThenEachRespawn() {
        when(languageService.message("Messages.SpawnHeadsAppeared")).thenReturn("%count% in %hunt%");
        when(languageService.message("Messages.SpawnHeadAppeared")).thenReturn("one in %hunt%");
        useHunt(withTemplate(5, 2, basic(), new SpawnOptions(true, false, false, SpawnOptions.Scoring.HEADS)), HuntState.ACTIVE);

        service.start();
        service.claim(hunt, spawned().get(0), player);

        bukkit.verify(() -> Bukkit.broadcastMessage("2 in Spawn"));
        bukkit.verify(() -> Bukkit.broadcastMessage("one in Spawn"));
    }

    @Test
    void announce_disabled_broadcastsNothing() {
        useHunt(spawnPoints(5, 2, -1, onFind(0)), HuntState.ACTIVE);

        service.start();

        bukkit.verify(() -> Bukkit.broadcastMessage(anyString()), never());
    }

    @Test
    void addHeads_spawnsExtraHeadsThatDoNotComeBack() {
        useHunt(spawnPoints(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();

        assertThat(service.addHeads(hunt, 2)).isEqualTo(2);
        assertThat(service.getActiveHeads("spawnhunt")).hasSize(3);

        service.claim(hunt, spawned().get(1), player);
        service.claim(hunt, spawned().get(2), player);

        assertThat(service.getActiveHeads("spawnhunt")).hasSize(1);
        assertThat(spawned()).hasSize(3);
    }

    @Test
    void addHeads_moreThanTheFreeSpots_placesWhatFits_withoutLeavingRetries() {
        useHunt(spawnPoints(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        later.clear();

        assertThat(service.addHeads(hunt, 10000)).isEqualTo(4);

        assertThat(service.getActiveHeads("spawnhunt")).hasSize(5);
        assertThat(later).isEmpty();
    }

    @Test
    void addHeads_inactiveOrUnknownHunt_addsNothing() {
        useHunt(spawnPoints(5, 1, -1, onFind(0)), HuntState.INACTIVE);
        service.start();

        assertThat(service.addHeads(hunt, 3)).isZero();
        assertThat(service.addHeads(new HBHunt(configService, "plain", "Plain", HuntState.ACTIVE, 1, "D"), 3)).isZero();
    }

    @Test
    void addHeads_announcesTheCount() {
        when(languageService.message("Messages.SpawnHeadsAppeared")).thenReturn("%count% in %hunt%");
        useHunt(withTemplate(5, 1, basic(), new SpawnOptions(true, false, false, SpawnOptions.Scoring.HEADS)), HuntState.ACTIVE);
        when(languageService.message("Messages.SpawnHeadAppeared")).thenReturn("one");
        service.start();

        service.addHeads(hunt, 3);

        bukkit.verify(() -> Bukkit.broadcastMessage("3 in Spawn"));
    }

    @Test
    void loweredActiveCount_foundHeadsAreNotReplaced() {
        useHunt(spawnPoints(5, 3, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        useHunt(spawnPoints(5, 1, -1, onFind(0)), HuntState.ACTIVE);

        service.claim(hunt, spawned().get(0), player);
        service.claim(hunt, spawned().get(1), player);

        assertThat(service.getActiveHeads("spawnhunt")).hasSize(1);
    }

    @Test
    void particleOf_returnsTheTemplateParticle() {
        var particle = new SpawnParticle("FLAME", 4, List.of());
        useHunt(withTemplate(3, 1, basic().withParticle(particle), SpawnOptions.DEFAULT), HuntState.ACTIVE);
        service.start();

        assertThat(service.particleOf(spawned().get(0))).isEqualTo(particle);
        assertThat(service.particleOf(new HeadLocation("", UUID.randomUUID(), new Location(world, 0, 0, 0), "spawnhunt"))).isNull();
        assertThat(service.particleOf(new HeadLocation("", UUID.randomUUID(), new Location(world, 0, 0, 0), "other"))).isNull();
    }

    @Test
    void totalSpawned_countsEverySpawn() {
        useHunt(spawnPoints(5, 2, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        service.claim(hunt, spawned().get(0), player);

        assertThat(service.totalSpawned("spawnhunt")).isEqualTo(3);
        assertThat(service.totalSpawned("unknown")).isZero();
    }

    @Test
    void log_writesSpawnsFindsTrapsAndRemovals() throws Exception {
        when(player.getName()).thenReturn("Steve");
        useHunt(withTemplate(5, 2, basic(), new SpawnOptions(false, true, false, SpawnOptions.Scoring.HEADS)), HuntState.ACTIVE);
        service.start();
        service.claim(hunt, spawned().get(0), player);
        service.onDiscarded(spawned().get(1));
        service.stop();

        var lines = Files.readAllLines(dataFolder.resolve("spawns").resolve("spawnhunt.log"));
        assertThat(lines).anyMatch(line -> line.contains(" SPAWN world "));
        assertThat(lines).anyMatch(line -> line.contains(" FOUND world ") && line.endsWith(" Steve"));
        assertThat(lines).anyMatch(line -> line.contains(" REMOVED world "));
    }

    @Test
    void log_disabled_writesNothing() {
        useHunt(spawnPoints(5, 2, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        service.stop();

        assertThat(Files.exists(dataFolder.resolve("spawns").resolve("spawnhunt.log"))).isFalse();
    }

    @Test
    void debug_tellsTheAdminsWithATeleportLink() {
        when(languageService.message("Messages.SpawnDebugAppeared")).thenReturn("%hunt% %world% %x% %y% %z%");
        Player admin = mock(Player.class);
        Player regular = mock(Player.class);
        Player.Spigot spigot = mock(Player.Spigot.class);
        when(admin.hasPermission("headblocks.admin")).thenReturn(true);
        when(admin.spigot()).thenReturn(spigot);
        bukkit.when(Bukkit::getOnlinePlayers).thenAnswer(invocation -> List.of(admin, regular));
        useHunt(withTemplate(5, 1, basic(), new SpawnOptions(false, false, true, SpawnOptions.Scoring.HEADS)), HuntState.ACTIVE);

        service.start();

        var captor = ArgumentCaptor.forClass(net.md_5.bungee.api.chat.BaseComponent.class);
        verify(spigot).sendMessage(captor.capture());
        assertThat(captor.getValue().getClickEvent().getValue()).startsWith("/headblocks tp world ");
        verify(regular, never()).spigot();
    }

    @Test
    void teleportCommand_targetsTheHead() {
        assertThat(SpawnService.teleportCommand(new Location(world, 1.5, 64, -2.5)))
                .isEqualTo("/headblocks tp world 1.5 64.0 -2.5 0.0 90.0");
    }

    @Test
    void claim_storesTheTemplatePoints() throws Exception {
        useHunt(withTemplate(3, 1, basic().withPoints(2.5), SpawnOptions.DEFAULT), HuntState.ACTIVE);
        service.start();
        var head = spawned().get(0);

        service.claim(hunt, head, player);
        service.storeFound(head);

        verify(storageService).createSpawnHead(head.getUuid(), "tex", 2.5);
    }

    // --- Area placement ---

    private fr.aerwyn81.headblocks.data.hunt.behavior.SpawnBehavior randomBehavior(int active, SpawnOptions options) {
        return fr.aerwyn81.headblocks.data.hunt.behavior.SpawnBehaviors.area(registry, true, 20,
                fr.aerwyn81.headblocks.data.hunt.behavior.spawn.AreaOptions.BlockFilter.BLACKLIST, List.of(),
                active, 10, -1, SpawnCompletion.PER_PLAYER, AfterGoal.DENY, onFind(0), options,
                List.of(new SpawnTemplate("basic", "Basic", 1, HeadContent.head("tex"), List.of())));
    }

    private void useRandomHunt(int active, SpawnOptions options) {
        useHunt(randomBehavior(active, options), HuntState.ACTIVE);
        hunt.setRequirements(new fr.aerwyn81.headblocks.data.hunt.requirement.RequirementSet(registry,
                fr.aerwyn81.headblocks.data.hunt.requirement.RequirementMode.ALL,
                List.of(new fr.aerwyn81.headblocks.data.hunt.requirement.types.AreaRequirement(registry,
                        new fr.aerwyn81.headblocks.data.hunt.requirement.area.CuboidAreaProvider("world", 0, 50, 0, 63, 80, 63),
                        null, false, false, null))));

        lenient().when(world.getMinHeight()).thenReturn(-64);
        lenient().when(world.getMaxHeight()).thenReturn(320);
        lenient().when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        lenient().when(world.getHighestBlockYAt(anyInt(), anyInt(), any(org.bukkit.HeightMap.class))).thenReturn(63);
        lenient().when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(invocation -> {
            int x = invocation.getArgument(0);
            int y = invocation.getArgument(1);
            int z = invocation.getArgument(2);
            var type = y <= 63 ? org.bukkit.Material.STONE : org.bukkit.Material.AIR;
            var block = mock(org.bukkit.block.Block.class);
            lenient().when(block.getType()).thenReturn(type);
            lenient().when(block.isEmpty()).thenReturn(type == org.bukkit.Material.AIR);
            lenient().when(block.getLocation()).thenReturn(new Location(world, x, y, z));
            lenient().when(block.getRelative(0, -1, 0)).thenAnswer(below -> world.getBlockAt(x, y - 1, z));
            return block;
        });
    }

    @Test
    void random_start_spawnsTheActiveCountOnTheGround() {
        useRandomHunt(3, SpawnOptions.DEFAULT);

        service.start();

        assertThat(spawned()).hasSize(3);
        assertThat(spawned()).allSatisfy(head -> {
            assertThat(head.getLocation().getBlockY()).isEqualTo(64);
            assertThat(head.getLocation().getBlockX()).isBetween(0, 63);
        });
        assertThat(service.getActiveHeads("spawnhunt")).hasSize(3);
    }

    @Test
    void random_foundHead_respawnsElsewhere() {
        useRandomHunt(1, SpawnOptions.DEFAULT);
        service.start();
        var head = spawned().get(0);

        service.claim(hunt, head, player);

        assertThat(service.getActiveHeads("spawnhunt")).hasSize(1);
        assertThat(service.getActiveHeads("spawnhunt")).extracting(HeadLocation::getUuid).doesNotContain(head.getUuid());
    }

    @Test
    void random_noLoadedChunk_retriesLater() {
        useRandomHunt(2, SpawnOptions.DEFAULT);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(false);

        service.start();

        assertThat(spawned()).isEmpty();
        assertThat(later).extracting(Delayed::ticks).contains(60L);
    }

    @Test
    void random_withoutArea_retriesLater() {
        useRandomHunt(1, SpawnOptions.DEFAULT);
        hunt.setRequirements(new fr.aerwyn81.headblocks.data.hunt.requirement.RequirementSet(registry));

        service.start();

        assertThat(spawned()).isEmpty();
        assertThat(later).extracting(Delayed::ticks).contains(60L);
    }

    @Test
    void random_regionTaskRunningLater_countsTheHeadsBeingPlaced() {
        when(languageService.message("Messages.SpawnHeadsAppeared")).thenReturn("%count% in %hunt%");
        when(languageService.message("Messages.SpawnHeadAppeared")).thenReturn("one in %hunt%");
        useRandomHunt(2, new SpawnOptions(true, false, false, SpawnOptions.Scoring.HEADS));
        var deferred = new ArrayList<Runnable>();
        doAnswer(invocation -> deferred.add(invocation.getArgument(1)))
                .when(scheduler).runNow(any(Location.class), any(Runnable.class));

        service.start();
        assertThat(spawned()).isEmpty();

        bukkit.verify(() -> Bukkit.broadcastMessage("2 in Spawn"));
        assertThat(service.addHeads(hunt, 1)).isEqualTo(1);
        bukkit.verify(() -> Bukkit.broadcastMessage("one in Spawn"));

        List.copyOf(deferred).forEach(Runnable::run);

        assertThat(spawned()).hasSize(3);
    }

    @Test
    void random_regionTaskAfterTheHuntChanged_placesNothing() {
        useRandomHunt(1, SpawnOptions.DEFAULT);
        var deferred = new ArrayList<Runnable>();
        doAnswer(invocation -> deferred.add(invocation.getArgument(1)))
                .when(scheduler).runNow(any(Location.class), any(Runnable.class));
        service.start();

        hunt.setBehaviors(List.of(new FreeBehavior(), randomBehavior(1, SpawnOptions.DEFAULT)));
        deferred.forEach(Runnable::run);

        assertThat(spawned()).isEmpty();
    }

    @Test
    void random_regionTaskOutdatedByAReroll_isDropped() {
        useRandomHunt(1, SpawnOptions.DEFAULT);
        var deferred = new ArrayList<Runnable>();
        doAnswer(invocation -> deferred.add(invocation.getArgument(1)))
                .when(scheduler).runNow(any(Location.class), any(Runnable.class));
        service.start();

        service.reroll(hunt, false);
        List.copyOf(deferred).forEach(Runnable::run);

        assertThat(spawned()).hasSize(1);
        assertThat(service.getActiveHeads("spawnhunt")).hasSize(1);
    }

    @Test
    void restart_headSavedOnABlockCenter_comesBackOnTheBlock() throws Exception {
        useHunt(spawnPoints(5, 1, -1, onFind(0)), HuntState.ACTIVE);
        service.start();
        service.stop();

        var yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(stateFile().toFile());
        var active = yaml.getConfigurationSection("active");
        for (String key : active.getKeys(false)) {
            active.set(key + ".x", active.getDouble(key + ".x") + 0.5);
            active.set(key + ".z", active.getDouble(key + ".z") + 0.5);
        }
        yaml.save(stateFile().toFile());

        restart();

        assertThat(spawned()).singleElement().satisfies(head -> {
            assertThat(head.getLocation().getX() % 1).isZero();
            assertThat(head.getLocation().getZ() % 1).isZero();
        });
    }
}
