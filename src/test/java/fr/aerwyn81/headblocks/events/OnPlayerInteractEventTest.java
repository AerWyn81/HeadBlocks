package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntConfig;
import fr.aerwyn81.headblocks.data.hunt.behavior.BehaviorResult;
import fr.aerwyn81.headblocks.data.reward.Reward;
import fr.aerwyn81.headblocks.hooks.PacketEventsHook;
import fr.aerwyn81.headblocks.services.*;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.runnables.BukkitFutureResult;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.function.Consumer;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnPlayerInteractEventTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private HeadService headService;

    @Mock
    private StorageService storageService;

    @Mock
    private LanguageService languageService;

    @Mock
    private HuntService huntService;

    @Mock
    private PlaceholdersService placeholdersService;

    @Mock
    private RewardService rewardService;

    @Mock
    private ConfigService configService;

    @Mock
    private PlayerInteractEvent event;

    @Mock
    private Block block;

    @Mock
    private Player player;

    @Mock
    private Location location;

    private OnPlayerInteractEvent handler;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getHeadService()).thenReturn(headService);
        lenient().when(registry.getStorageService()).thenReturn(storageService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(registry.getHuntService()).thenReturn(huntService);
        lenient().when(registry.getPlaceholdersService()).thenReturn(placeholdersService);
        lenient().when(registry.getRewardService()).thenReturn(rewardService);
        lenient().when(registry.getConfigService()).thenReturn(configService);

        lenient().when(languageService.message(anyString())).thenReturn("mock-message");

        handler = new OnPlayerInteractEvent(registry);
    }

    @AfterEach
    void tearDown() {
        HeadBlocks.isReloadInProgress = false;
    }

    // ================================================================
    // Early exit tests (onPlayerInteract guard clauses)
    // ================================================================

    @Nested
    class EarlyExitGuards {

        @Test
        void nullBlock_ignored() {
            when(event.getClickedBlock()).thenReturn(null);

            handler.onPlayerInteract(event);

            verify(event, never()).setCancelled(anyBoolean());
            verifyNoInteractions(headService);
        }

        @Test
        void offHand_ignored() {
            when(event.getClickedBlock()).thenReturn(block);
            when(event.getHand()).thenReturn(EquipmentSlot.OFF_HAND);

            handler.onPlayerInteract(event);

            verify(event, never()).setCancelled(anyBoolean());
            verifyNoInteractions(headService);
        }

        @Test
        void notPlayerHead_ignored() {
            when(event.getClickedBlock()).thenReturn(block);
            when(event.getHand()).thenReturn(EquipmentSlot.HAND);

            try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class)) {
                headUtils.when(() -> HeadUtils.isPlayerHead(block)).thenReturn(false);

                handler.onPlayerInteract(event);

                verify(event, never()).setCancelled(anyBoolean());
                verifyNoInteractions(headService);
            }
        }

        @Test
        void creativeLeftClick_ignored() {
            when(event.getClickedBlock()).thenReturn(block);
            when(event.getHand()).thenReturn(EquipmentSlot.HAND);
            when(event.getPlayer()).thenReturn(player);
            when(player.getGameMode()).thenReturn(GameMode.CREATIVE);
            when(event.getAction()).thenReturn(Action.LEFT_CLICK_BLOCK);

            try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class)) {
                headUtils.when(() -> HeadUtils.isPlayerHead(block)).thenReturn(true);

                handler.onPlayerInteract(event);

                verify(event, never()).setCancelled(anyBoolean());
                verifyNoInteractions(headService);
            }
        }

        @Test
        void creativeRightClick_notIgnored() {
            HeadLocation headLocation = mock(HeadLocation.class);
            UUID headUuid = UUID.randomUUID();
            when(headLocation.getUuid()).thenReturn(headUuid);

            when(event.getClickedBlock()).thenReturn(block);
            when(event.getHand()).thenReturn(EquipmentSlot.HAND);
            when(event.getPlayer()).thenReturn(player);
            when(player.getGameMode()).thenReturn(GameMode.CREATIVE);
            when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
            when(block.getLocation()).thenReturn(location);
            when(headService.getHeadAt(location)).thenReturn(headLocation);
            when(storageService.isStorageError()).thenReturn(false);
            when(huntService.getHuntsForHead(headUuid)).thenReturn(Collections.emptyList());

            try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
                 MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
                headUtils.when(() -> HeadUtils.isPlayerHead(block)).thenReturn(true);
                playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.use")).thenReturn(true);

                handler.onPlayerInteract(event);

                verify(huntService).getHuntsForHead(headUuid);
            }
        }

        @Test
        void survivalLeftClick_notIgnored() {
            HeadLocation headLocation = mock(HeadLocation.class);
            UUID headUuid = UUID.randomUUID();
            when(headLocation.getUuid()).thenReturn(headUuid);

            when(event.getClickedBlock()).thenReturn(block);
            when(event.getHand()).thenReturn(EquipmentSlot.HAND);
            when(event.getPlayer()).thenReturn(player);
            when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
            when(block.getLocation()).thenReturn(location);
            when(headService.getHeadAt(location)).thenReturn(headLocation);
            when(storageService.isStorageError()).thenReturn(false);
            when(huntService.getHuntsForHead(headUuid)).thenReturn(Collections.emptyList());

            try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
                 MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
                headUtils.when(() -> HeadUtils.isPlayerHead(block)).thenReturn(true);
                playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.use")).thenReturn(true);

                handler.onPlayerInteract(event);

                verify(huntService).getHuntsForHead(headUuid);
            }
        }
    }

    // ================================================================
    // Reload in progress
    // ================================================================

    @Nested
    class ReloadInProgress {

        @Test
        void reloadInProgress_cancelledAndSendsMessage() {
            HeadBlocks.isReloadInProgress = true;

            when(event.getClickedBlock()).thenReturn(block);
            when(event.getHand()).thenReturn(EquipmentSlot.HAND);
            when(event.getPlayer()).thenReturn(player);
            when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);

            try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class)) {
                headUtils.when(() -> HeadUtils.isPlayerHead(block)).thenReturn(true);

                handler.onPlayerInteract(event);

                verify(event).setCancelled(true);
                verify(player).sendMessage(anyString());
                verify(languageService).message("Messages.PluginReloading");
            }
        }
    }

    // ================================================================
    // Head not a plugin head
    // ================================================================

    @Nested
    class HeadNotPluginHead {

        @Test
        void headNotPluginHead_ignored() {
            when(event.getClickedBlock()).thenReturn(block);
            when(event.getHand()).thenReturn(EquipmentSlot.HAND);
            when(event.getPlayer()).thenReturn(player);
            when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
            when(block.getLocation()).thenReturn(location);
            when(headService.getHeadAt(location)).thenReturn(null);

            try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class)) {
                headUtils.when(() -> HeadUtils.isPlayerHead(block)).thenReturn(true);

                handler.onPlayerInteract(event);

                verify(event, never()).setCancelled(anyBoolean());
                verify(storageService, never()).isStorageError();
            }
        }
    }

    // ================================================================
    // Storage error
    // ================================================================

    @Nested
    class StorageError {

        @Test
        void storageError_cancelledAndSendsMessage() {
            HeadLocation headLocation = mock(HeadLocation.class);

            when(event.getClickedBlock()).thenReturn(block);
            when(event.getHand()).thenReturn(EquipmentSlot.HAND);
            when(event.getPlayer()).thenReturn(player);
            when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
            when(block.getLocation()).thenReturn(location);
            when(headService.getHeadAt(location)).thenReturn(headLocation);
            when(storageService.isStorageError()).thenReturn(true);

            try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class)) {
                headUtils.when(() -> HeadUtils.isPlayerHead(block)).thenReturn(true);

                handler.onPlayerInteract(event);

                verify(event).setCancelled(true);
                verify(player).sendMessage(anyString());
                verify(languageService).message("Messages.StorageError");
            }
        }
    }

    // ================================================================
    // No permission
    // ================================================================

    @Nested
    class NoPermission {

        @Test
        void noPermission_doesNotReachHuntLookup() {
            HeadLocation headLocation = mock(HeadLocation.class);

            when(event.getClickedBlock()).thenReturn(block);
            when(event.getHand()).thenReturn(EquipmentSlot.HAND);
            when(event.getPlayer()).thenReturn(player);
            when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
            when(block.getLocation()).thenReturn(location);
            when(headService.getHeadAt(location)).thenReturn(headLocation);
            when(storageService.isStorageError()).thenReturn(false);

            try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
                 MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
                headUtils.when(() -> HeadUtils.isPlayerHead(block)).thenReturn(true);
                playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.use")).thenReturn(false);

                handler.onPlayerInteract(event);

                verify(huntService, never()).getHuntsForHead(any());
            }
        }

        @Test
        void noPermission_sendsNoPermissionMessage() {
            HeadLocation headLocation = mock(HeadLocation.class);

            when(event.getClickedBlock()).thenReturn(block);
            when(event.getHand()).thenReturn(EquipmentSlot.HAND);
            when(event.getPlayer()).thenReturn(player);
            when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
            when(block.getLocation()).thenReturn(location);
            when(headService.getHeadAt(location)).thenReturn(headLocation);
            when(storageService.isStorageError()).thenReturn(false);

            try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
                 MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
                headUtils.when(() -> HeadUtils.isPlayerHead(block)).thenReturn(true);
                playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.use")).thenReturn(false);

                handler.onPlayerInteract(event);

                verify(player).sendMessage(anyString());
                verify(languageService).message("Messages.NoPermissionBlock");
            }
        }

        @Test
        void noPermission_emptyMessage_doesNotSend() {
            HeadLocation headLocation = mock(HeadLocation.class);

            when(event.getClickedBlock()).thenReturn(block);
            when(event.getHand()).thenReturn(EquipmentSlot.HAND);
            when(event.getPlayer()).thenReturn(player);
            when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
            when(block.getLocation()).thenReturn(location);
            when(headService.getHeadAt(location)).thenReturn(headLocation);
            when(storageService.isStorageError()).thenReturn(false);
            when(languageService.message("Messages.NoPermissionBlock")).thenReturn("   ");

            try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
                 MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
                headUtils.when(() -> HeadUtils.isPlayerHead(block)).thenReturn(true);
                playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.use")).thenReturn(false);

                handler.onPlayerInteract(event);

                verify(player, never()).sendMessage(anyString());
            }
        }
    }

    // ================================================================
    // Hunt lookup
    // ================================================================

    @Nested
    class HBHuntLookup {

        @Test
        void noActiveHunts_allInactive_sendMessage() {
            HeadLocation headLocation = mock(HeadLocation.class);
            UUID headUuid = UUID.randomUUID();
            when(headLocation.getUuid()).thenReturn(headUuid);

            HBHunt inactiveHunt = mock(HBHunt.class);
            when(inactiveHunt.isActive()).thenReturn(false);

            when(event.getClickedBlock()).thenReturn(block);
            when(event.getHand()).thenReturn(EquipmentSlot.HAND);
            when(event.getPlayer()).thenReturn(player);
            when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
            when(block.getLocation()).thenReturn(location);
            when(headService.getHeadAt(location)).thenReturn(headLocation);
            when(storageService.isStorageError()).thenReturn(false);
            when(huntService.getHuntsForHead(headUuid)).thenReturn(List.of(inactiveHunt));

            try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
                 MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
                headUtils.when(() -> HeadUtils.isPlayerHead(block)).thenReturn(true);
                playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.use")).thenReturn(true);

                handler.onPlayerInteract(event);

                verify(player).sendMessage(anyString());
                verify(languageService).message("Messages.HuntHeadInactive");
                verify(storageService, never()).getHeadsPlayer(any());
            }
        }

        @Test
        void noActiveHunts_emptyInactiveMessage_doesNotSend() {
            HeadLocation headLocation = mock(HeadLocation.class);
            UUID headUuid = UUID.randomUUID();
            when(headLocation.getUuid()).thenReturn(headUuid);

            HBHunt inactiveHunt = mock(HBHunt.class);
            when(inactiveHunt.isActive()).thenReturn(false);

            when(event.getClickedBlock()).thenReturn(block);
            when(event.getHand()).thenReturn(EquipmentSlot.HAND);
            when(event.getPlayer()).thenReturn(player);
            when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
            when(block.getLocation()).thenReturn(location);
            when(headService.getHeadAt(location)).thenReturn(headLocation);
            when(storageService.isStorageError()).thenReturn(false);
            when(huntService.getHuntsForHead(headUuid)).thenReturn(List.of(inactiveHunt));
            when(languageService.message("Messages.HuntHeadInactive")).thenReturn("   ");

            try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
                 MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
                headUtils.when(() -> HeadUtils.isPlayerHead(block)).thenReturn(true);
                playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.use")).thenReturn(true);

                handler.onPlayerInteract(event);

                verify(player, never()).sendMessage(anyString());
            }
        }

        @Test
        void noHuntsAssigned_ignored() {
            HeadLocation headLocation = mock(HeadLocation.class);
            UUID headUuid = UUID.randomUUID();
            when(headLocation.getUuid()).thenReturn(headUuid);

            when(event.getClickedBlock()).thenReturn(block);
            when(event.getHand()).thenReturn(EquipmentSlot.HAND);
            when(event.getPlayer()).thenReturn(player);
            when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
            when(block.getLocation()).thenReturn(location);
            when(headService.getHeadAt(location)).thenReturn(headLocation);
            when(storageService.isStorageError()).thenReturn(false);
            when(huntService.getHuntsForHead(headUuid)).thenReturn(Collections.emptyList());

            try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
                 MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
                headUtils.when(() -> HeadUtils.isPlayerHead(block)).thenReturn(true);
                playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.use")).thenReturn(true);

                handler.onPlayerInteract(event);

                verify(storageService, never()).getHeadsPlayer(any());
            }
        }

        @SuppressWarnings("unchecked")
        @Test
        void activeHunts_startProcessing() {
            HeadLocation headLocation = mock(HeadLocation.class);
            UUID headUuid = UUID.randomUUID();
            when(headLocation.getUuid()).thenReturn(headUuid);

            HBHunt activeHunt = mock(HBHunt.class);
            when(activeHunt.isActive()).thenReturn(true);

            UUID playerUuid = UUID.randomUUID();

            when(event.getClickedBlock()).thenReturn(block);
            when(event.getHand()).thenReturn(EquipmentSlot.HAND);
            when(event.getPlayer()).thenReturn(player);
            when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
            when(player.getUniqueId()).thenReturn(playerUuid);
            when(block.getLocation()).thenReturn(location);
            when(headService.getHeadAt(location)).thenReturn(headLocation);
            when(storageService.isStorageError()).thenReturn(false);
            when(huntService.getHuntsForHead(headUuid)).thenReturn(List.of(activeHunt));

            BukkitFutureResult<Set<UUID>> futureResult = mock(BukkitFutureResult.class);
            when(storageService.getHeadsPlayer(playerUuid)).thenReturn(futureResult);

            try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
                 MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
                headUtils.when(() -> HeadUtils.isPlayerHead(block)).thenReturn(true);
                playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.use")).thenReturn(true);

                handler.onPlayerInteract(event);

                verify(storageService).getHeadsPlayer(playerUuid);
            }
        }
    }

    // ================================================================
    // handleHuntClick tests (the async callback path)
    // ================================================================

    @Nested
    class HandleHBHuntClick {

        private UUID headUuid;
        private UUID playerUuid;
        private HeadLocation headLocation;
        private HBHunt activeHunt;
        private HuntConfig huntConfig;

        @BeforeEach
        void setUpHuntClick() {
            headUuid = UUID.randomUUID();
            playerUuid = UUID.randomUUID();

            headLocation = mock(HeadLocation.class);
            lenient().when(headLocation.getUuid()).thenReturn(headUuid);
            lenient().when(headLocation.getLocation()).thenReturn(location);
            lenient().when(headLocation.getRewards()).thenReturn(new ArrayList<>());

            activeHunt = mock(HBHunt.class);
            lenient().when(activeHunt.isActive()).thenReturn(true);
            lenient().when(activeHunt.getId()).thenReturn("default");

            huntConfig = mock(HuntConfig.class);
            lenient().when(activeHunt.getConfig()).thenReturn(huntConfig);

            lenient().when(event.getClickedBlock()).thenReturn(block);
            lenient().when(event.getHand()).thenReturn(EquipmentSlot.HAND);
            lenient().when(event.getPlayer()).thenReturn(player);
            lenient().when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
            lenient().when(player.getUniqueId()).thenReturn(playerUuid);
            lenient().when(player.getName()).thenReturn("TestPlayer");
            lenient().when(block.getLocation()).thenReturn(location);
            lenient().when(headService.getHeadAt(location)).thenReturn(headLocation);
            lenient().when(storageService.isStorageError()).thenReturn(false);
            lenient().when(huntService.getHuntsForHead(headUuid)).thenReturn(List.of(activeHunt));

            // Default HuntConfig stubs for the "already claimed" path
            lenient().when(huntConfig.getHeadClickSoundFound()).thenReturn("");
            lenient().when(huntConfig.getHeadClickSoundAlreadyOwn()).thenReturn("");
            lenient().when(huntConfig.isHeadClickTitleEnabled()).thenReturn(false);
            lenient().when(huntConfig.isFireworkEnabled()).thenReturn(false);
            lenient().when(huntConfig.isHeadClickEjectEnabled()).thenReturn(false);

            lenient().when(configService.headClickParticlesEnabled()).thenReturn(false);

            lenient().when(placeholdersService.parse(anyString(), any(UUID.class), any(HeadLocation.class), anyString(), nullable(String.class)))
                    .thenReturn("parsed-message");
        }

        /**
         * Triggers the full onPlayerInteract flow, captures the BukkitFutureResult.whenComplete consumer,
         * and invokes it synchronously with the given player heads set.
         * Verifications on regular mocks (non-static) can be done after calling this method.
         */
        @SuppressWarnings("unchecked")
        private void triggerHandleHuntClick(Set<UUID> playerHeads) {
            BukkitFutureResult<Set<UUID>> futureResult = mock(BukkitFutureResult.class);
            when(storageService.getHeadsPlayer(playerUuid)).thenReturn(futureResult);

            try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
                 MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class);
                 MockedStatic<HeadBlocks> headBlocksStatic = mockStatic(HeadBlocks.class);
                 MockedStatic<Bukkit> bukkitStatic = mockStatic(Bukkit.class)) {

                headUtils.when(() -> HeadUtils.isPlayerHead(block)).thenReturn(true);
                playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.use")).thenReturn(true);

                HeadBlocks pluginInstance = mock(HeadBlocks.class);
                headBlocksStatic.when(HeadBlocks::getInstance).thenReturn(pluginInstance);
                lenient().when(pluginInstance.getPacketEventsHook()).thenReturn(null);

                PluginManager pluginManager = mock(PluginManager.class);
                bukkitStatic.when(Bukkit::getPluginManager).thenReturn(pluginManager);

                handler.onPlayerInteract(event);

                // Capture the Consumer passed to whenComplete and invoke it
                ArgumentCaptor<Consumer<Set<UUID>>> captor = ArgumentCaptor.forClass(Consumer.class);
                verify(futureResult).whenComplete(captor.capture());
                captor.getValue().accept(playerHeads);
            }
        }

        // --- Already found head (in all active hunts) ---

        @Nested
        class AlreadyFoundHead {

            @Test
            void alreadyFound_sendsAlreadyClaimedMessage() throws InternalException {
                ArrayList<UUID> huntPlayerHeads = new ArrayList<>(List.of(headUuid));
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(huntPlayerHeads);

                triggerHandleHuntClick(Set.of(headUuid));

                verify(languageService).message("Messages.AlreadyClaimHead");
            }

            @Test
            void alreadyFound_ejectEnabled_setsVelocity() throws InternalException {
                ArrayList<UUID> huntPlayerHeads = new ArrayList<>(List.of(headUuid));
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(huntPlayerHeads);
                when(huntConfig.isHeadClickEjectEnabled()).thenReturn(true);
                when(huntConfig.getHeadClickEjectPower()).thenReturn(1.5);

                Location playerLoc = mock(Location.class);
                Vector direction = new Vector(1, 0, 0);
                when(player.getLocation()).thenReturn(playerLoc);
                when(playerLoc.getDirection()).thenReturn(direction);

                triggerHandleHuntClick(Set.of(headUuid));

                verify(player).setVelocity(any(Vector.class));
            }

            @Test
            void alreadyFound_ejectDisabled_doesNotSetVelocity() throws InternalException {
                ArrayList<UUID> huntPlayerHeads = new ArrayList<>(List.of(headUuid));
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(huntPlayerHeads);
                when(huntConfig.isHeadClickEjectEnabled()).thenReturn(false);

                triggerHandleHuntClick(Set.of(headUuid));

                verify(player, never()).setVelocity(any(Vector.class));
            }

            @Test
            void alreadyFound_doesNotAddHeadOrGiveReward() throws InternalException {
                ArrayList<UUID> huntPlayerHeads = new ArrayList<>(List.of(headUuid));
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(huntPlayerHeads);

                triggerHandleHuntClick(Set.of(headUuid));

                verify(storageService, never()).addHeadForHunt(any(), any(), anyString());
                verify(rewardService, never()).giveReward(any(), any(), any(), any(HuntConfig.class));
            }
        }

        // --- Successful new head find ---

        @Nested
        class SuccessfulFind {

            @Test
            void newFind_addsHeadForHunt() throws InternalException {
                ArrayList<UUID> huntPlayerHeads = new ArrayList<>();
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(huntPlayerHeads);
                when(activeHunt.evaluateBehaviors(player, headLocation)).thenReturn(BehaviorResult.allow());
                when(rewardService.hasPlayerSlotsRequired(eq(player), any(), eq(huntConfig))).thenReturn(true);

                triggerHandleHuntClick(new HashSet<>());

                verify(storageService).addHeadForHunt(playerUuid, headUuid, "default");
            }

            @Test
            void newFind_givesHuntReward() throws InternalException {
                ArrayList<UUID> huntPlayerHeads = new ArrayList<>();
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(huntPlayerHeads);
                when(activeHunt.evaluateBehaviors(player, headLocation)).thenReturn(BehaviorResult.allow());
                when(rewardService.hasPlayerSlotsRequired(eq(player), any(), eq(huntConfig))).thenReturn(true);

                triggerHandleHuntClick(new HashSet<>());

                verify(rewardService).giveReward(eq(player), any(), eq(headLocation), eq(huntConfig), eq("default"));
            }

            @Test
            void newFind_notifiesBehaviors() throws InternalException {
                ArrayList<UUID> huntPlayerHeads = new ArrayList<>();
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(huntPlayerHeads);
                when(activeHunt.evaluateBehaviors(player, headLocation)).thenReturn(BehaviorResult.allow());
                when(rewardService.hasPlayerSlotsRequired(eq(player), any(), eq(huntConfig))).thenReturn(true);

                triggerHandleHuntClick(new HashSet<>());

                verify(activeHunt).notifyHeadFound(player, headLocation);
            }

            @Test
            void newFind_withHeadRewards_executesHeadRewards() throws InternalException {
                Reward headReward = mock(Reward.class);
                when(headLocation.getRewards()).thenReturn(new ArrayList<>(List.of(headReward)));

                ArrayList<UUID> huntPlayerHeads = new ArrayList<>();
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(huntPlayerHeads);
                when(activeHunt.evaluateBehaviors(player, headLocation)).thenReturn(BehaviorResult.allow());
                when(rewardService.hasPlayerSlotsRequired(eq(player), any(), eq(huntConfig))).thenReturn(true);

                triggerHandleHuntClick(new HashSet<>());

                verify(headReward).execute(player, headLocation, registry);
            }

            @Test
            void newFind_titleEnabled_sendsTitle() throws InternalException {
                ArrayList<UUID> huntPlayerHeads = new ArrayList<>();
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(huntPlayerHeads);
                when(activeHunt.evaluateBehaviors(player, headLocation)).thenReturn(BehaviorResult.allow());
                when(rewardService.hasPlayerSlotsRequired(eq(player), any(), eq(huntConfig))).thenReturn(true);
                when(huntConfig.isHeadClickTitleEnabled()).thenReturn(true);
                when(huntConfig.getHeadClickTitleFirstLine()).thenReturn("&aFound!");
                when(huntConfig.getHeadClickTitleSubTitle()).thenReturn("&7You found a head");
                when(huntConfig.getHeadClickTitleFadeIn()).thenReturn(10);
                when(huntConfig.getHeadClickTitleStay()).thenReturn(40);
                when(huntConfig.getHeadClickTitleFadeOut()).thenReturn(10);

                triggerHandleHuntClick(new HashSet<>());

                verify(player).sendTitle(anyString(), anyString(), eq(10), eq(40), eq(10));
            }

            @Test
            void newFind_titleDisabled_doesNotSendTitle() throws InternalException {
                ArrayList<UUID> huntPlayerHeads = new ArrayList<>();
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(huntPlayerHeads);
                when(activeHunt.evaluateBehaviors(player, headLocation)).thenReturn(BehaviorResult.allow());
                when(rewardService.hasPlayerSlotsRequired(eq(player), any(), eq(huntConfig))).thenReturn(true);
                when(huntConfig.isHeadClickTitleEnabled()).thenReturn(false);

                triggerHandleHuntClick(new HashSet<>());

                verify(player, never()).sendTitle(anyString(), anyString(), anyInt(), anyInt(), anyInt());
            }

            @Test
            void newFind_packetEventsEnabled_addsFoundHead() throws InternalException {
                ArrayList<UUID> huntPlayerHeads = new ArrayList<>();
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(huntPlayerHeads);
                when(activeHunt.evaluateBehaviors(player, headLocation)).thenReturn(BehaviorResult.allow());
                when(rewardService.hasPlayerSlotsRequired(eq(player), any(), eq(huntConfig))).thenReturn(true);

                @SuppressWarnings("unchecked")
                BukkitFutureResult<Set<UUID>> futureResult = mock(BukkitFutureResult.class);
                when(storageService.getHeadsPlayer(playerUuid)).thenReturn(futureResult);

                try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
                     MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class);
                     MockedStatic<HeadBlocks> headBlocksStatic = mockStatic(HeadBlocks.class);
                     MockedStatic<Bukkit> bukkitStatic = mockStatic(Bukkit.class)) {

                    headUtils.when(() -> HeadUtils.isPlayerHead(block)).thenReturn(true);
                    playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.use")).thenReturn(true);

                    HeadBlocks pluginInstance = mock(HeadBlocks.class);
                    headBlocksStatic.when(HeadBlocks::getInstance).thenReturn(pluginInstance);

                    PacketEventsHook peHook = mock(PacketEventsHook.class, RETURNS_DEEP_STUBS);
                    when(pluginInstance.getPacketEventsHook()).thenReturn(peHook);
                    when(peHook.isEnabled()).thenReturn(true);

                    PluginManager pluginManager = mock(PluginManager.class);
                    bukkitStatic.when(Bukkit::getPluginManager).thenReturn(pluginManager);

                    handler.onPlayerInteract(event);

                    @SuppressWarnings("unchecked")
                    ArgumentCaptor<Consumer<Set<UUID>>> captor = ArgumentCaptor.forClass(Consumer.class);
                    verify(futureResult).whenComplete(captor.capture());
                    captor.getValue().accept(new HashSet<>());

                    verify(peHook.getHeadHidingListener()).addFoundHead(player, headUuid);
                }
            }

            @Test
            void newFind_packetEventsNull_doesNotCrash() throws InternalException {
                ArrayList<UUID> huntPlayerHeads = new ArrayList<>();
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(huntPlayerHeads);
                when(activeHunt.evaluateBehaviors(player, headLocation)).thenReturn(BehaviorResult.allow());
                when(rewardService.hasPlayerSlotsRequired(eq(player), any(), eq(huntConfig))).thenReturn(true);

                // Default triggerHandleHuntClick sets packetEventsHook to null
                triggerHandleHuntClick(new HashSet<>());

                // Should complete without NPE
                verify(storageService).addHeadForHunt(playerUuid, headUuid, "default");
            }

            @Test
            void newFind_packetEventsNotEnabled_doesNotCallHide() throws InternalException {
                ArrayList<UUID> huntPlayerHeads = new ArrayList<>();
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(huntPlayerHeads);
                when(activeHunt.evaluateBehaviors(player, headLocation)).thenReturn(BehaviorResult.allow());
                when(rewardService.hasPlayerSlotsRequired(eq(player), any(), eq(huntConfig))).thenReturn(true);

                @SuppressWarnings("unchecked")
                BukkitFutureResult<Set<UUID>> futureResult = mock(BukkitFutureResult.class);
                when(storageService.getHeadsPlayer(playerUuid)).thenReturn(futureResult);

                try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
                     MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class);
                     MockedStatic<HeadBlocks> headBlocksStatic = mockStatic(HeadBlocks.class);
                     MockedStatic<Bukkit> bukkitStatic = mockStatic(Bukkit.class)) {

                    headUtils.when(() -> HeadUtils.isPlayerHead(block)).thenReturn(true);
                    playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.use")).thenReturn(true);

                    HeadBlocks pluginInstance = mock(HeadBlocks.class);
                    headBlocksStatic.when(HeadBlocks::getInstance).thenReturn(pluginInstance);

                    PacketEventsHook peHook = mock(PacketEventsHook.class);
                    when(pluginInstance.getPacketEventsHook()).thenReturn(peHook);
                    when(peHook.isEnabled()).thenReturn(false);

                    PluginManager pluginManager = mock(PluginManager.class);
                    bukkitStatic.when(Bukkit::getPluginManager).thenReturn(pluginManager);

                    handler.onPlayerInteract(event);

                    @SuppressWarnings("unchecked")
                    ArgumentCaptor<Consumer<Set<UUID>>> captor = ArgumentCaptor.forClass(Consumer.class);
                    verify(futureResult).whenComplete(captor.capture());
                    captor.getValue().accept(new HashSet<>());

                    verify(peHook, never()).getHeadHidingListener();
                }
            }
        }

        // --- Behavior deny ---

        @Nested
        class BehaviorDeny {

            @Test
            void behaviorDenied_doesNotAddHead() throws InternalException {
                ArrayList<UUID> huntPlayerHeads = new ArrayList<>();
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(huntPlayerHeads);
                when(activeHunt.evaluateBehaviors(player, headLocation))
                        .thenReturn(BehaviorResult.deny("You cannot click this head yet"));

                triggerHandleHuntClick(new HashSet<>());

                verify(storageService, never()).addHeadForHunt(any(), any(), anyString());
                verify(rewardService, never()).giveReward(any(), any(), any(), any(HuntConfig.class));
            }

            @Test
            void behaviorDenied_sendsDenyMessage() throws InternalException {
                ArrayList<UUID> huntPlayerHeads = new ArrayList<>();
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(huntPlayerHeads);
                when(activeHunt.evaluateBehaviors(player, headLocation))
                        .thenReturn(BehaviorResult.deny("You must find head #1 first"));

                triggerHandleHuntClick(new HashSet<>());

                verify(player).sendMessage("You must find head #1 first");
            }

            @Test
            void behaviorDenied_nullMessage_doesNotSendDenyOrAlreadyClaimed() throws InternalException {
                ArrayList<UUID> huntPlayerHeads = new ArrayList<>();
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(huntPlayerHeads);
                when(activeHunt.evaluateBehaviors(player, headLocation))
                        .thenReturn(BehaviorResult.deny(null));

                triggerHandleHuntClick(new HashSet<>());

                // Deny message is null so not sent, and "already claimed" is NOT sent because behavior denied
                verify(player, never()).sendMessage((String) null);
                verify(languageService, never()).message("Messages.AlreadyClaimHead");
            }

            @Test
            void behaviorDenied_emptyMessage_doesNotSendDenyOrAlreadyClaimed() throws InternalException {
                ArrayList<UUID> huntPlayerHeads = new ArrayList<>();
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(huntPlayerHeads);
                when(activeHunt.evaluateBehaviors(player, headLocation))
                        .thenReturn(BehaviorResult.deny(""));

                triggerHandleHuntClick(new HashSet<>());

                // Empty deny message skipped, and "already claimed" is NOT sent because behavior denied
                verify(player, never()).sendMessage("");
                verify(languageService, never()).message("Messages.AlreadyClaimHead");
            }

            @Test
            void behaviorDenied_allHuntsDenied_doesNotShowAlreadyClaimed() throws InternalException {
                ArrayList<UUID> huntPlayerHeads = new ArrayList<>();
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(huntPlayerHeads);
                when(activeHunt.evaluateBehaviors(player, headLocation))
                        .thenReturn(BehaviorResult.deny("denied"));

                triggerHandleHuntClick(new HashSet<>());

                // Behavior denied, so "already claimed" path is NOT taken
                verify(languageService, never()).message("Messages.AlreadyClaimHead");
            }
        }

        // --- Insufficient inventory slots ---

        @Nested
        class InsufficientInventory {

            @Test
            void insufficientSlots_doesNotAddHead() throws InternalException {
                ArrayList<UUID> huntPlayerHeads = new ArrayList<>();
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(huntPlayerHeads);
                when(activeHunt.evaluateBehaviors(player, headLocation)).thenReturn(BehaviorResult.allow());
                when(rewardService.hasPlayerSlotsRequired(eq(player), any(), eq(huntConfig))).thenReturn(false);

                triggerHandleHuntClick(new HashSet<>());

                verify(storageService, never()).addHeadForHunt(any(), any(), anyString());
            }

            @Test
            void insufficientSlots_sendsInventoryFullMessage() throws InternalException {
                ArrayList<UUID> huntPlayerHeads = new ArrayList<>();
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(huntPlayerHeads);
                when(activeHunt.evaluateBehaviors(player, headLocation)).thenReturn(BehaviorResult.allow());
                when(rewardService.hasPlayerSlotsRequired(eq(player), any(), eq(huntConfig))).thenReturn(false);

                triggerHandleHuntClick(new HashSet<>());

                verify(languageService).message("Messages.InventoryFullReward");
            }

            @Test
            void insufficientSlots_emptyMessage_doesNotSendInventoryMessage() throws InternalException {
                ArrayList<UUID> huntPlayerHeads = new ArrayList<>();
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(huntPlayerHeads);
                when(activeHunt.evaluateBehaviors(player, headLocation)).thenReturn(BehaviorResult.allow());
                when(rewardService.hasPlayerSlotsRequired(eq(player), any(), eq(huntConfig))).thenReturn(false);
                when(languageService.message("Messages.InventoryFullReward")).thenReturn("   ");

                triggerHandleHuntClick(new HashSet<>());

                // The "already claimed" path sends its own message, so we verify inventory message not sent
                verify(player, never()).sendMessage("   ");
            }
        }

        // --- Multi-hunt scenarios ---

        @Nested
        class MultiHBHuntScenarios {

            @Test
            void multipleActiveHunts_findsInBoth() throws InternalException {
                HBHunt secondHunt = mock(HBHunt.class);
                when(secondHunt.isActive()).thenReturn(true);
                when(secondHunt.getId()).thenReturn("special");
                HuntConfig secondConfig = mock(HuntConfig.class);
                when(secondHunt.getConfig()).thenReturn(secondConfig);
                when(secondHunt.evaluateBehaviors(player, headLocation)).thenReturn(BehaviorResult.allow());

                when(huntService.getHuntsForHead(headUuid)).thenReturn(List.of(activeHunt, secondHunt));

                ArrayList<UUID> defaultHuntHeads = new ArrayList<>();
                ArrayList<UUID> specialHuntHeads = new ArrayList<>();
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(defaultHuntHeads);
                when(storageService.getHeadsPlayerForHunt(playerUuid, "special")).thenReturn(specialHuntHeads);
                when(activeHunt.evaluateBehaviors(player, headLocation)).thenReturn(BehaviorResult.allow());
                when(rewardService.hasPlayerSlotsRequired(eq(player), any(), eq(huntConfig))).thenReturn(true);
                when(rewardService.hasPlayerSlotsRequired(eq(player), any(), eq(secondConfig))).thenReturn(true);

                triggerHandleHuntClick(new HashSet<>());

                verify(storageService).addHeadForHunt(playerUuid, headUuid, "default");
                verify(storageService).addHeadForHunt(playerUuid, headUuid, "special");
            }

            @Test
            void multipleActiveHunts_oneAlreadyFound_findsOther() throws InternalException {
                HBHunt secondHunt = mock(HBHunt.class);
                when(secondHunt.isActive()).thenReturn(true);
                when(secondHunt.getId()).thenReturn("special");
                HuntConfig secondConfig = mock(HuntConfig.class);
                when(secondHunt.getConfig()).thenReturn(secondConfig);
                when(secondHunt.evaluateBehaviors(player, headLocation)).thenReturn(BehaviorResult.allow());

                when(huntService.getHuntsForHead(headUuid)).thenReturn(List.of(activeHunt, secondHunt));

                // Already found in default, not yet in special
                ArrayList<UUID> defaultHuntHeads = new ArrayList<>(List.of(headUuid));
                ArrayList<UUID> specialHuntHeads = new ArrayList<>();
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(defaultHuntHeads);
                when(storageService.getHeadsPlayerForHunt(playerUuid, "special")).thenReturn(specialHuntHeads);
                when(rewardService.hasPlayerSlotsRequired(eq(player), any(), eq(secondConfig))).thenReturn(true);

                triggerHandleHuntClick(Set.of(headUuid));

                verify(storageService, never()).addHeadForHunt(playerUuid, headUuid, "default");
                verify(storageService).addHeadForHunt(playerUuid, headUuid, "special");
            }

            @Test
            void multipleActiveHunts_headRewardsOnlyOnFirstNewFind() throws InternalException {
                Reward headReward = mock(Reward.class);
                when(headLocation.getRewards()).thenReturn(new ArrayList<>(List.of(headReward)));

                HBHunt secondHunt = mock(HBHunt.class);
                when(secondHunt.isActive()).thenReturn(true);
                when(secondHunt.getId()).thenReturn("special");
                HuntConfig secondConfig = mock(HuntConfig.class);
                when(secondHunt.getConfig()).thenReturn(secondConfig);
                when(secondHunt.evaluateBehaviors(player, headLocation)).thenReturn(BehaviorResult.allow());

                when(huntService.getHuntsForHead(headUuid)).thenReturn(List.of(activeHunt, secondHunt));

                ArrayList<UUID> defaultHuntHeads = new ArrayList<>();
                ArrayList<UUID> specialHuntHeads = new ArrayList<>();
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(defaultHuntHeads);
                when(storageService.getHeadsPlayerForHunt(playerUuid, "special")).thenReturn(specialHuntHeads);
                when(activeHunt.evaluateBehaviors(player, headLocation)).thenReturn(BehaviorResult.allow());
                when(rewardService.hasPlayerSlotsRequired(eq(player), any(), eq(huntConfig))).thenReturn(true);
                when(rewardService.hasPlayerSlotsRequired(eq(player), any(), eq(secondConfig))).thenReturn(true);

                triggerHandleHuntClick(new HashSet<>());

                // Head rewards should only execute once (on first new find), not per hunt
                verify(headReward, times(1)).execute(player, headLocation, registry);
            }

            @Test
            void multipleActiveHunts_eachHuntGetsOwnReward() throws InternalException {
                HBHunt secondHunt = mock(HBHunt.class);
                when(secondHunt.isActive()).thenReturn(true);
                when(secondHunt.getId()).thenReturn("special");
                HuntConfig secondConfig = mock(HuntConfig.class);
                when(secondHunt.getConfig()).thenReturn(secondConfig);
                when(secondHunt.evaluateBehaviors(player, headLocation)).thenReturn(BehaviorResult.allow());

                when(huntService.getHuntsForHead(headUuid)).thenReturn(List.of(activeHunt, secondHunt));

                ArrayList<UUID> defaultHuntHeads = new ArrayList<>();
                ArrayList<UUID> specialHuntHeads = new ArrayList<>();
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(defaultHuntHeads);
                when(storageService.getHeadsPlayerForHunt(playerUuid, "special")).thenReturn(specialHuntHeads);
                when(activeHunt.evaluateBehaviors(player, headLocation)).thenReturn(BehaviorResult.allow());
                when(rewardService.hasPlayerSlotsRequired(eq(player), any(), eq(huntConfig))).thenReturn(true);
                when(rewardService.hasPlayerSlotsRequired(eq(player), any(), eq(secondConfig))).thenReturn(true);

                triggerHandleHuntClick(new HashSet<>());

                verify(rewardService).giveReward(eq(player), any(), eq(headLocation), eq(huntConfig), eq("default"));
                verify(rewardService).giveReward(eq(player), any(), eq(headLocation), eq(secondConfig), eq("special"));
            }

            @Test
            void multipleActiveHunts_eachHuntNotifiesBehaviors() throws InternalException {
                HBHunt secondHunt = mock(HBHunt.class);
                when(secondHunt.isActive()).thenReturn(true);
                when(secondHunt.getId()).thenReturn("special");
                HuntConfig secondConfig = mock(HuntConfig.class);
                when(secondHunt.getConfig()).thenReturn(secondConfig);
                when(secondHunt.evaluateBehaviors(player, headLocation)).thenReturn(BehaviorResult.allow());

                when(huntService.getHuntsForHead(headUuid)).thenReturn(List.of(activeHunt, secondHunt));

                ArrayList<UUID> defaultHuntHeads = new ArrayList<>();
                ArrayList<UUID> specialHuntHeads = new ArrayList<>();
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(defaultHuntHeads);
                when(storageService.getHeadsPlayerForHunt(playerUuid, "special")).thenReturn(specialHuntHeads);
                when(activeHunt.evaluateBehaviors(player, headLocation)).thenReturn(BehaviorResult.allow());
                when(rewardService.hasPlayerSlotsRequired(eq(player), any(), eq(huntConfig))).thenReturn(true);
                when(rewardService.hasPlayerSlotsRequired(eq(player), any(), eq(secondConfig))).thenReturn(true);

                triggerHandleHuntClick(new HashSet<>());

                verify(activeHunt).notifyHeadFound(player, headLocation);
                verify(secondHunt).notifyHeadFound(player, headLocation);
            }
        }

        // --- InternalException handling ---

        @Nested
        class ExceptionHandling {

            @Test
            void internalException_continuesProcessing() throws InternalException {
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default"))
                        .thenThrow(new InternalException("Database error"));

                triggerHandleHuntClick(new HashSet<>());

                // Should not crash; shows "already claimed" since anyNewFind stays false
                verify(languageService).message("Messages.AlreadyClaimHead");
            }

            @Test
            void internalException_onSecondHunt_firstHuntStillProcessed() throws InternalException {
                HBHunt secondHunt = mock(HBHunt.class);
                lenient().when(secondHunt.isActive()).thenReturn(true);
                lenient().when(secondHunt.getId()).thenReturn("broken");
                HuntConfig secondConfig = mock(HuntConfig.class);
                lenient().when(secondHunt.getConfig()).thenReturn(secondConfig);

                when(huntService.getHuntsForHead(headUuid)).thenReturn(List.of(activeHunt, secondHunt));

                ArrayList<UUID> defaultHuntHeads = new ArrayList<>();
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(defaultHuntHeads);
                when(storageService.getHeadsPlayerForHunt(playerUuid, "broken"))
                        .thenThrow(new InternalException("DB error"));
                when(activeHunt.evaluateBehaviors(player, headLocation)).thenReturn(BehaviorResult.allow());
                when(rewardService.hasPlayerSlotsRequired(eq(player), any(), eq(huntConfig))).thenReturn(true);

                triggerHandleHuntClick(new HashSet<>());

                // First hunt should still process successfully
                verify(storageService).addHeadForHunt(playerUuid, headUuid, "default");
            }

            @Test
            void addHeadForHuntThrows_caughtGracefully() throws InternalException {
                ArrayList<UUID> huntPlayerHeads = new ArrayList<>();
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(huntPlayerHeads);
                when(activeHunt.evaluateBehaviors(player, headLocation)).thenReturn(BehaviorResult.allow());
                when(rewardService.hasPlayerSlotsRequired(eq(player), any(), eq(huntConfig))).thenReturn(true);
                doThrow(new InternalException("Write failed"))
                        .when(storageService).addHeadForHunt(playerUuid, headUuid, "default");

                triggerHandleHuntClick(new HashSet<>());

                // Should not crash, the exception is caught
                verify(storageService).addHeadForHunt(playerUuid, headUuid, "default");
            }
        }

        // --- Mixed scenarios ---

        @Nested
        class MixedScenarios {

            @Test
            void behaviorDeniedOnFirst_secondHuntSucceeds() throws InternalException {
                HBHunt secondHunt = mock(HBHunt.class);
                when(secondHunt.isActive()).thenReturn(true);
                when(secondHunt.getId()).thenReturn("special");
                HuntConfig secondConfig = mock(HuntConfig.class);
                when(secondHunt.getConfig()).thenReturn(secondConfig);
                when(secondHunt.evaluateBehaviors(player, headLocation)).thenReturn(BehaviorResult.allow());

                when(huntService.getHuntsForHead(headUuid)).thenReturn(List.of(activeHunt, secondHunt));

                ArrayList<UUID> defaultHuntHeads = new ArrayList<>();
                ArrayList<UUID> specialHuntHeads = new ArrayList<>();
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(defaultHuntHeads);
                when(storageService.getHeadsPlayerForHunt(playerUuid, "special")).thenReturn(specialHuntHeads);
                when(activeHunt.evaluateBehaviors(player, headLocation))
                        .thenReturn(BehaviorResult.deny("Not allowed"));
                when(rewardService.hasPlayerSlotsRequired(eq(player), any(), eq(secondConfig))).thenReturn(true);

                triggerHandleHuntClick(new HashSet<>());

                // First hunt denied, second succeeds
                verify(storageService, never()).addHeadForHunt(playerUuid, headUuid, "default");
                verify(storageService).addHeadForHunt(playerUuid, headUuid, "special");
            }

            @Test
            void insufficientSlotsOnFirst_secondHuntSucceeds() throws InternalException {
                HBHunt secondHunt = mock(HBHunt.class);
                when(secondHunt.isActive()).thenReturn(true);
                when(secondHunt.getId()).thenReturn("special");
                HuntConfig secondConfig = mock(HuntConfig.class);
                when(secondHunt.getConfig()).thenReturn(secondConfig);
                when(secondHunt.evaluateBehaviors(player, headLocation)).thenReturn(BehaviorResult.allow());

                when(huntService.getHuntsForHead(headUuid)).thenReturn(List.of(activeHunt, secondHunt));

                ArrayList<UUID> defaultHuntHeads = new ArrayList<>();
                ArrayList<UUID> specialHuntHeads = new ArrayList<>();
                when(storageService.getHeadsPlayerForHunt(playerUuid, "default")).thenReturn(defaultHuntHeads);
                when(storageService.getHeadsPlayerForHunt(playerUuid, "special")).thenReturn(specialHuntHeads);
                when(activeHunt.evaluateBehaviors(player, headLocation)).thenReturn(BehaviorResult.allow());
                when(rewardService.hasPlayerSlotsRequired(eq(player), any(), eq(huntConfig))).thenReturn(false);
                when(rewardService.hasPlayerSlotsRequired(eq(player), any(), eq(secondConfig))).thenReturn(true);

                triggerHandleHuntClick(new HashSet<>());

                // First hunt: insufficient slots, second succeeds
                verify(storageService, never()).addHeadForHunt(playerUuid, headUuid, "default");
                verify(storageService).addHeadForHunt(playerUuid, headUuid, "special");
            }
        }
    }
}
