package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.hooks.HeadHidingPacketListener;
import fr.aerwyn81.headblocks.hooks.PacketEventsHook;
import fr.aerwyn81.headblocks.utils.runnables.BukkitFutureResult;
import fr.aerwyn81.headblocks.utils.scheduler.SchedulerAdapter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeadVisibilityServiceTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private ConfigService configService;

    @Mock
    private StorageService storageService;

    @Mock
    private HeadService headService;

    @Mock
    private HeadVisualService visualService;

    @Mock
    private SchedulerAdapter scheduler;

    @Mock
    private HeadBlocks plugin;

    @Mock
    private PacketEventsHook packetEventsHook;

    @Mock
    private HeadHidingPacketListener packetHiding;

    @Mock
    private Player player;

    @Mock
    private HeadLocation head;

    @Mock
    private Entity entity;

    private MockedStatic<HeadBlocks> headBlocks;
    private HeadVisibilityService visibilityService;
    private final UUID playerUuid = UUID.randomUUID();
    private final UUID headUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(registry.getConfigService()).thenReturn(configService);
        lenient().when(registry.getStorageService()).thenReturn(storageService);
        lenient().when(registry.getHeadService()).thenReturn(headService);
        lenient().when(registry.getVisualService()).thenReturn(visualService);
        lenient().when(registry.getScheduler()).thenReturn(scheduler);
        lenient().when(player.getUniqueId()).thenReturn(playerUuid);
        lenient().when(head.getUuid()).thenReturn(headUuid);
        lenient().when(configService.isHideFoundHeads()).thenReturn(true);
        lenient().when(plugin.getPacketEventsHook()).thenReturn(packetEventsHook);
        lenient().when(packetEventsHook.isEnabled()).thenReturn(true);
        lenient().when(packetEventsHook.getHeadHidingListener()).thenReturn(packetHiding);

        headBlocks = mockStatic(HeadBlocks.class);
        headBlocks.when(HeadBlocks::getInstance).thenReturn(plugin);

        visibilityService = new HeadVisibilityService(registry);
    }

    @AfterEach
    void tearDown() {
        headBlocks.close();
    }

    @Test
    void found_entityHead_isHiddenWithoutPackets() {
        when(visualService.isEntityRendered(head)).thenReturn(true);
        when(visualService.entitiesOf(head)).thenReturn(List.of(entity));

        visibilityService.onHeadFound(player, head);

        verify(player).hideEntity(plugin, entity);
        verify(visualService).setVisible(player, head, false);
        verifyNoInteractions(packetHiding);
    }

    @Test
    void found_entityHead_hidingDisabled_staysVisible() {
        when(visualService.isEntityRendered(head)).thenReturn(true);
        when(configService.isHideFoundHeads()).thenReturn(false);

        visibilityService.onHeadFound(player, head);

        verify(player, never()).hideEntity(any(), any());
    }

    @Test
    void found_blockHead_goesThroughPackets() {
        when(visualService.isEntityRendered(head)).thenReturn(false);

        visibilityService.onHeadFound(player, head);

        verify(packetHiding).addFoundHead(player, headUuid);
        verify(player, never()).hideEntity(any(), any());
    }

    @Test
    void found_blockHead_withoutPacketEvents_doesNothing() {
        when(visualService.isEntityRendered(head)).thenReturn(false);
        when(packetEventsHook.isEnabled()).thenReturn(false);

        visibilityService.onHeadFound(player, head);

        verifyNoInteractions(packetHiding);
    }

    @Test
    void headReset_showsBothKinds() {
        when(headService.getHeadByUUID(headUuid)).thenReturn(head);
        when(visualService.entitiesOf(head)).thenReturn(List.of(entity));

        visibilityService.onHeadReset(player, headUuid);

        verify(packetHiding).removeFoundHead(player, headUuid);
        verify(player).showEntity(plugin, entity);
        verify(visualService).setVisible(player, head, true);
    }

    @SuppressWarnings("unchecked")
    private void joinWith(Player joining, Set<UUID> found) {
        BukkitFutureResult<Set<UUID>> future = mock(BukkitFutureResult.class);
        when(storageService.getHeadsPlayer(joining.getUniqueId())).thenReturn(future);
        lenient().when(joining.isOnline()).thenReturn(true);
        doAnswer(invocation -> {
            ((Consumer<Set<UUID>>) invocation.getArgument(1)).accept(found);
            return null;
        }).when(future).whenComplete(eq(joining), any());

        visibilityService.onJoin(joining);
    }

    @SuppressWarnings("unchecked")
    private void spawnedHeads(Map<HeadLocation, List<Entity>> heads) {
        lenient().doAnswer(invocation -> {
            var action = (BiConsumer<HeadLocation, List<Entity>>) invocation.getArgument(0);
            heads.forEach(action);
            return null;
        }).when(visualService).forEachSpawned(any());
    }

    @Test
    void join_loadsTheFoundHeadsThenRefreshes() {
        HeadLocation other = mock(HeadLocation.class);
        when(other.getUuid()).thenReturn(UUID.randomUUID());
        Entity otherEntity = mock(Entity.class);
        spawnedHeads(Map.of(head, List.of(entity), other, List.of(otherEntity)));

        joinWith(player, Set.of(headUuid));

        verify(packetHiding).onPlayerJoin(player);
        verify(player).hideEntity(plugin, entity);
        verify(player).showEntity(plugin, otherEntity);
        verifyNoMoreInteractions(scheduler);
    }

    @Test
    void join_hidingDisabled_loadsNothing() {
        when(configService.isHideFoundHeads()).thenReturn(false);

        visibilityService.onJoin(player);

        verify(storageService, never()).getHeadsPlayer(any());
    }

    @Test
    void progressReset_forgetsTheFoundHeadsAndShowsEverything() {
        spawnedHeads(Map.of(head, List.of(entity)));
        joinWith(player, Set.of(headUuid));
        clearInvocations(player);

        visibilityService.onProgressReset(player);

        verify(packetHiding).showAllPreviousHeads(player);
        verify(player).showEntity(plugin, entity);
    }

    @Test
    void headFound_isRememberedForLaterSpawns() {
        joinWith(player, Set.of());
        when(visualService.isEntityRendered(head)).thenReturn(false);
        visibilityService.onHeadFound(player, head);
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(1)).run();
            return null;
        }).when(scheduler).runNow(any(Player.class), any(Runnable.class));

        try (MockedStatic<org.bukkit.Bukkit> bukkit = mockStatic(org.bukkit.Bukkit.class)) {
            bukkit.when(org.bukkit.Bukkit::getOnlinePlayers).thenAnswer(invocation -> List.of(player));

            visibilityService.onSpawned(head, List.of(entity));
        }

        verify(player).hideEntity(plugin, entity);
    }

    @Test
    void spawned_isHiddenOnlyForPlayersWhoFoundIt() {
        Player other = mock(Player.class);
        when(other.getUniqueId()).thenReturn(UUID.randomUUID());
        joinWith(player, Set.of(headUuid));
        joinWith(other, Set.of());
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(1)).run();
            return null;
        }).when(scheduler).runNow(any(Player.class), any(Runnable.class));

        try (MockedStatic<org.bukkit.Bukkit> bukkit = mockStatic(org.bukkit.Bukkit.class)) {
            bukkit.when(org.bukkit.Bukkit::getOnlinePlayers).thenAnswer(invocation -> List.of(player, other));

            visibilityService.onSpawned(head, List.of(entity));
        }

        verify(player).hideEntity(plugin, entity);
        verify(other, never()).hideEntity(any(), any());
    }

    @Test
    void spawned_hidingDisabled_touchesNoPlayer() {
        when(configService.isHideFoundHeads()).thenReturn(false);

        visibilityService.onSpawned(head, List.of(entity));

        verifyNoInteractions(scheduler);
    }

    @Test
    void quit_forgetsThePlayer() {
        joinWith(player, Set.of(headUuid));

        visibilityService.onQuit(playerUuid);
        visibilityService.refreshEntities(player);

        verify(packetHiding).invalidatePlayerCache(playerUuid);
    }

    @Test
    void loadOnlinePlayers_loadsEveryConnectedPlayer() {
        BukkitFutureResult<Set<UUID>> future = mock(BukkitFutureResult.class);
        when(storageService.getHeadsPlayer(playerUuid)).thenReturn(future);

        try (MockedStatic<org.bukkit.Bukkit> bukkit = mockStatic(org.bukkit.Bukkit.class)) {
            bukkit.when(org.bukkit.Bukkit::getOnlinePlayers).thenAnswer(invocation -> List.of(player));

            visibilityService.loadOnlinePlayers();
        }

        verify(future).whenComplete(eq(player), any());
    }

    @Test
    void huntReset_onlyShowsTheHeadsOfThatHunt() {
        fr.aerwyn81.headblocks.data.hunt.HBHunt hunt = mock(fr.aerwyn81.headblocks.data.hunt.HBHunt.class);
        UUID otherUuid = UUID.randomUUID();
        when(headService.getHeadLocationsForHunt(hunt)).thenReturn(new java.util.ArrayList<>(List.of(head)));
        when(headService.getHeadByUUID(headUuid)).thenReturn(head);
        when(visualService.entitiesOf(head)).thenReturn(List.of(entity));
        joinWith(player, new java.util.HashSet<>(Set.of(headUuid, otherUuid)));
        clearInvocations(player);

        visibilityService.onHuntReset(player, hunt);

        verify(packetHiding).removeFoundHead(player, headUuid);
        verify(packetHiding, never()).removeFoundHead(player, otherUuid);
        verify(packetHiding, never()).showAllPreviousHeads(any());
        verify(player).showEntity(plugin, entity);
    }
}
