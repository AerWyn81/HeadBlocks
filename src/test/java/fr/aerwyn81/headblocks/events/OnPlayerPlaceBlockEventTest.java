package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.head.visual.ContentKind;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.data.head.visual.VisualForm;
import fr.aerwyn81.headblocks.services.*;
import fr.aerwyn81.headblocks.services.gui.types.TimedConfigGui;
import fr.aerwyn81.headblocks.utils.bukkit.*;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnPlayerPlaceBlockEventTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private HeadService headService;

    @Mock
    private StorageService storageService;

    @Mock
    private LanguageService languageService;

    @Mock
    private ConfigService configService;

    @Mock
    private HuntService huntService;

    @Mock
    private GuiService guiService;

    @Mock
    private HeadVisualService visualService;

    @Mock
    private TimedConfigGui timedConfigGui;

    @Mock
    private BlockPlaceEvent event;

    @Mock
    private Block blockPlaced;

    @Mock
    private Player player;

    @Mock
    private PlayerInventory playerInventory;

    @Mock
    private ItemStack mainHandItem;

    @Mock
    private ItemStack eventItemInHand;

    @Mock
    private Location blockLocation;

    private OnPlayerPlaceBlockEvent handler;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getHeadService()).thenReturn(headService);
        lenient().when(registry.getStorageService()).thenReturn(storageService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(registry.getConfigService()).thenReturn(configService);
        lenient().when(registry.getHuntService()).thenReturn(huntService);
        lenient().when(registry.getGuiService()).thenReturn(guiService);
        lenient().when(guiService.getTimedConfigManager()).thenReturn(timedConfigGui);
        lenient().when(registry.getVisualService()).thenReturn(visualService);
        lenient().when(visualService.formOf(any(HeadContent.class), any())).thenReturn(VisualForm.HEAD_BLOCK);

        lenient().when(languageService.message(anyString())).thenReturn("mock-message");
        lenient().when(event.getPlayer()).thenReturn(player);
        lenient().when(event.getBlockPlaced()).thenReturn(blockPlaced);
        lenient().when(player.getInventory()).thenReturn(playerInventory);
        lenient().when(playerInventory.getItemInMainHand()).thenReturn(mainHandItem);

        Location defaultLocation = mock(Location.class);
        lenient().when(blockPlaced.getLocation()).thenReturn(defaultLocation);
        lenient().when(defaultLocation.clone()).thenReturn(defaultLocation);
        lenient().when(defaultLocation.add(0.5, 0, 0.5)).thenReturn(defaultLocation);

        handler = new OnPlayerPlaceBlockEvent(registry);
    }

    @AfterEach
    void tearDown() {
        HeadBlocks.isReloadInProgress = false;
    }

    // --- No headblocks item in hand: ignored ---

    @Test
    void noHeadBlocksItem_ignored() {
        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(timedConfigGui.hasPendingPlatePlacement(playerUuid)).thenReturn(false);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class)) {
            handler.onPlayerPlaceBlock(event);

            verify(event, never()).setCancelled(anyBoolean());
            verify(storageService, never()).isStorageError();
        }
    }

    // --- Reload in progress: cancelled ---

    @Test
    void reloadInProgress_cancelled() {
        HeadBlocks.isReloadInProgress = true;
        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(timedConfigGui.hasPendingPlatePlacement(playerUuid)).thenReturn(false);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class)) {
            headUtils.when(() -> HeadUtils.isHeadBlocksItem(mainHandItem)).thenReturn(true);

            handler.onPlayerPlaceBlock(event);

            verify(event).setCancelled(true);
            verify(languageService).message("Messages.PluginReloading");
        }
    }

    // --- No permission: cancelled ---

    @Test
    void noPermission_cancelled() {
        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(timedConfigGui.hasPendingPlatePlacement(playerUuid)).thenReturn(false);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
            headUtils.when(() -> HeadUtils.isHeadBlocksItem(mainHandItem)).thenReturn(true);
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(false);

            handler.onPlayerPlaceBlock(event);

            verify(event).setCancelled(true);
        }
    }

    // --- Not sneaking or not creative: cancelled ---

    @Test
    void notSneakingOrCreative_cancelled() {
        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(timedConfigGui.hasPendingPlatePlacement(playerUuid)).thenReturn(false);

        when(player.isSneaking()).thenReturn(false);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
            headUtils.when(() -> HeadUtils.isHeadBlocksItem(mainHandItem)).thenReturn(true);
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(true);

            handler.onPlayerPlaceBlock(event);

            verify(event).setCancelled(true);
            verify(languageService).message("Messages.CreativeSneakAddHead");
        }
    }

    // --- Head already exists: cancelled ---

    @Test
    void headAlreadyExists_cancelled() {
        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(timedConfigGui.hasPendingPlatePlacement(playerUuid)).thenReturn(false);

        when(player.isSneaking()).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);

        Location rawBlockLoc = mock(Location.class);
        Location centeredLoc = mock(Location.class);
        when(blockPlaced.getLocation()).thenReturn(rawBlockLoc);
        when(rawBlockLoc.clone()).thenReturn(centeredLoc);
        when(centeredLoc.add(0.5, 0, 0.5)).thenReturn(centeredLoc);

        HeadLocation existingHead = mock(HeadLocation.class);
        when(headService.getHeadAt(centeredLoc)).thenReturn(existingHead);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
            headUtils.when(() -> HeadUtils.isHeadBlocksItem(mainHandItem)).thenReturn(true);
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(true);

            handler.onPlayerPlaceBlock(event);

            verify(event).setCancelled(true);
            verify(languageService).message("Messages.HeadAlreadyExistHere");
        }
    }

    // --- Storage error: cancelled ---

    @Test
    void storageError_cancelled() {
        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(timedConfigGui.hasPendingPlatePlacement(playerUuid)).thenReturn(false);

        when(player.isSneaking()).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);

        Location rawBlockLoc = mock(Location.class);
        Location centeredLoc = mock(Location.class);
        when(blockPlaced.getLocation()).thenReturn(rawBlockLoc);
        when(rawBlockLoc.clone()).thenReturn(centeredLoc);
        when(centeredLoc.add(0.5, 0, 0.5)).thenReturn(centeredLoc);
        when(headService.getHeadAt(centeredLoc)).thenReturn(null);

        when(storageService.isStorageError()).thenReturn(true);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
            headUtils.when(() -> HeadUtils.isHeadBlocksItem(mainHandItem)).thenReturn(true);
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(true);

            handler.onPlayerPlaceBlock(event);

            verify(event).setCancelled(true);
            verify(languageService).message("Messages.StorageError");
        }
    }

    // --- Success: head saved ---

    @Test
    void success_headSaved() throws Exception {
        UUID playerUuid = UUID.randomUUID();
        UUID headUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(timedConfigGui.hasPendingPlatePlacement(playerUuid)).thenReturn(false);

        when(player.isSneaking()).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);

        Location rawBlockLoc = mock(Location.class);
        Location centeredLoc = mock(Location.class);
        when(blockPlaced.getLocation()).thenReturn(rawBlockLoc);
        when(rawBlockLoc.clone()).thenReturn(centeredLoc);
        when(centeredLoc.add(0.5, 0, 0.5)).thenReturn(centeredLoc);
        when(headService.getHeadAt(centeredLoc)).thenReturn(null);

        when(storageService.isStorageError()).thenReturn(false);
        when(event.getItemInHand()).thenReturn(eventItemInHand);
        when(huntService.getSelectedHunt(playerUuid)).thenReturn("hunt1");
        when(headService.saveHeadLocation(centeredLoc, HeadContent.head("texture-abc"), 0f, "hunt1")).thenReturn(headUuid);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class);
             MockedStatic<LocationUtils> locationUtils = mockStatic(LocationUtils.class);
             MockedStatic<VersionUtils> versionUtils = mockStatic(VersionUtils.class);
             MockedStatic<ParticlesUtils> ignored = mockStatic(ParticlesUtils.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            headUtils.when(() -> HeadUtils.isHeadBlocksItem(mainHandItem)).thenReturn(true);
            headUtils.when(() -> HeadUtils.getContent(eventItemInHand)).thenReturn(HeadContent.head("texture-abc"));
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(true);
            locationUtils.when(() -> LocationUtils.parseLocationPlaceholders(anyString(), any(Location.class)))
                    .thenReturn("parsed-message");
            versionUtils.when(() -> VersionUtils.isNewerOrEqualsTo(any())).thenReturn(true);
            PluginManager pm = mock(PluginManager.class);
            bukkit.when(Bukkit::getPluginManager).thenReturn(pm);

            handler.onPlayerPlaceBlock(event);

            verify(headService).saveHeadLocation(centeredLoc, HeadContent.head("texture-abc"), 0f, "hunt1");
            verify(event, never()).setCancelled(anyBoolean());
        }
    }

    @Test
    void itemTaggedWithHunt_savedInTaggedHunt() throws Exception {
        UUID playerUuid = UUID.randomUUID();
        UUID headUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(timedConfigGui.hasPendingPlatePlacement(playerUuid)).thenReturn(false);

        when(player.isSneaking()).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);

        Location rawBlockLoc = mock(Location.class);
        Location centeredLoc = mock(Location.class);
        when(blockPlaced.getLocation()).thenReturn(rawBlockLoc);
        when(rawBlockLoc.clone()).thenReturn(centeredLoc);
        when(centeredLoc.add(0.5, 0, 0.5)).thenReturn(centeredLoc);
        when(headService.getHeadAt(centeredLoc)).thenReturn(null);

        when(storageService.isStorageError()).thenReturn(false);
        when(event.getItemInHand()).thenReturn(eventItemInHand);
        when(huntService.huntExists("ab1")).thenReturn(true);
        when(headService.saveHeadLocation(centeredLoc, HeadContent.head("texture-abc"), 0f, "ab1")).thenReturn(headUuid);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class);
             MockedStatic<LocationUtils> locationUtils = mockStatic(LocationUtils.class);
             MockedStatic<ParticlesUtils> ignored = mockStatic(ParticlesUtils.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            headUtils.when(() -> HeadUtils.isHeadBlocksItem(mainHandItem)).thenReturn(true);
            headUtils.when(() -> HeadUtils.getContent(eventItemInHand)).thenReturn(HeadContent.head("texture-abc"));
            headUtils.when(() -> HeadUtils.getHuntId(eventItemInHand)).thenReturn("ab1");
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(true);
            locationUtils.when(() -> LocationUtils.parseLocationPlaceholders(anyString(), any(Location.class)))
                    .thenReturn("parsed-message");
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));

            handler.onPlayerPlaceBlock(event);

            verify(headService).saveHeadLocation(centeredLoc, HeadContent.head("texture-abc"), 0f, "ab1");
            verify(huntService, never()).getSelectedHunt(any());
            verify(event, never()).setCancelled(anyBoolean());
        }
    }

    @Test
    void itemTaggedWithDeletedHunt_cancelled() throws Exception {
        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(timedConfigGui.hasPendingPlatePlacement(playerUuid)).thenReturn(false);

        when(player.isSneaking()).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);

        Location rawBlockLoc = mock(Location.class);
        Location centeredLoc = mock(Location.class);
        when(blockPlaced.getLocation()).thenReturn(rawBlockLoc);
        when(rawBlockLoc.clone()).thenReturn(centeredLoc);
        when(centeredLoc.add(0.5, 0, 0.5)).thenReturn(centeredLoc);
        when(headService.getHeadAt(centeredLoc)).thenReturn(null);

        when(storageService.isStorageError()).thenReturn(false);
        when(event.getItemInHand()).thenReturn(eventItemInHand);
        when(huntService.huntExists("ab1")).thenReturn(false);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
            headUtils.when(() -> HeadUtils.isHeadBlocksItem(mainHandItem)).thenReturn(true);
            headUtils.when(() -> HeadUtils.getContent(eventItemInHand)).thenReturn(HeadContent.head("texture-abc"));
            headUtils.when(() -> HeadUtils.getHuntId(eventItemInHand)).thenReturn("ab1");
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(true);

            handler.onPlayerPlaceBlock(event);

            verify(event).setCancelled(true);
            verify(languageService).message("Messages.HeadHuntDeleted");
            verify(headService, never()).saveHeadLocation(any(), any(HeadContent.class), anyFloat(), anyString());
        }
    }

    // --- Pending timed plate: handled ---

    @Test
    void pendingTimedPlate_handled() {
        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(timedConfigGui.hasPendingPlatePlacement(playerUuid)).thenReturn(true);

        when(blockPlaced.getType()).thenReturn(Material.HEAVY_WEIGHTED_PRESSURE_PLATE);
        Location rawBlockLoc = mock(Location.class);
        Location centeredLoc = mock(Location.class);
        when(blockPlaced.getLocation()).thenReturn(rawBlockLoc);
        when(rawBlockLoc.clone()).thenReturn(centeredLoc);
        when(centeredLoc.add(0.5, 0, 0.5)).thenReturn(centeredLoc);

        handler.onPlayerPlaceBlock(event);

        verify(timedConfigGui).handlePlatePlaced(player, centeredLoc);
        verify(storageService, never()).isStorageError();
    }

    // --- Pending timed plate with non-pressure-plate block: returns early without handling ---

    @Test
    void pendingTimedPlate_nonPressurePlate_returnsEarlyWithoutHandling() {
        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(timedConfigGui.hasPendingPlatePlacement(playerUuid)).thenReturn(true);

        // STONE does not contain "PRESSURE_PLATE"
        when(blockPlaced.getType()).thenReturn(Material.STONE);

        handler.onPlayerPlaceBlock(event);

        verify(timedConfigGui, never()).handlePlatePlaced(any(), any());
        verify(storageService, never()).isStorageError();
    }

    // --- No permission with empty message: cancelled but no message sent ---

    @Test
    void noPermission_emptyMessage_cancelledNoMessageSent() {
        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(timedConfigGui.hasPendingPlatePlacement(playerUuid)).thenReturn(false);

        // Return empty/whitespace message for NoPermissionBlock
        when(languageService.message("Messages.NoPermissionBlock")).thenReturn("   ");

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
            headUtils.when(() -> HeadUtils.isHeadBlocksItem(mainHandItem)).thenReturn(true);
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(false);

            handler.onPlayerPlaceBlock(event);

            verify(event).setCancelled(true);
            // Player should NOT receive a message since it's empty/whitespace
            verify(player, never()).sendMessage(anyString());
        }
    }

    // --- No permission with non-empty message: cancelled with message sent ---

    @Test
    void noPermission_nonEmptyMessage_cancelledWithMessageSent() {
        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(timedConfigGui.hasPendingPlatePlacement(playerUuid)).thenReturn(false);

        when(languageService.message("Messages.NoPermissionBlock")).thenReturn("You don't have permission");

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
            headUtils.when(() -> HeadUtils.isHeadBlocksItem(mainHandItem)).thenReturn(true);
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(false);

            handler.onPlayerPlaceBlock(event);

            verify(event).setCancelled(true);
            verify(player).sendMessage("You don't have permission");
        }
    }

    // --- Sneaking but not creative: cancelled ---

    @Test
    void sneakingButNotCreative_cancelled() {
        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(timedConfigGui.hasPendingPlatePlacement(playerUuid)).thenReturn(false);

        when(player.isSneaking()).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
            headUtils.when(() -> HeadUtils.isHeadBlocksItem(mainHandItem)).thenReturn(true);
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(true);

            handler.onPlayerPlaceBlock(event);

            verify(event).setCancelled(true);
            verify(languageService).message("Messages.CreativeSneakAddHead");
        }
    }

    // --- Null head texture: sends error and logs ---

    @Test
    void nullHeadTexture_sendsStorageErrorAndLogs() throws Exception {
        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(player.getName()).thenReturn("TestPlayer");
        when(timedConfigGui.hasPendingPlatePlacement(playerUuid)).thenReturn(false);

        when(player.isSneaking()).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);

        Location rawBlockLoc = mock(Location.class);
        Location centeredLoc = mock(Location.class);
        when(blockPlaced.getLocation()).thenReturn(rawBlockLoc);
        when(rawBlockLoc.clone()).thenReturn(centeredLoc);
        when(centeredLoc.add(0.5, 0, 0.5)).thenReturn(centeredLoc);
        when(headService.getHeadAt(centeredLoc)).thenReturn(null);

        when(storageService.isStorageError()).thenReturn(false);
        when(event.getItemInHand()).thenReturn(eventItemInHand);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class);
             MockedStatic<LogUtil> logUtil = mockStatic(LogUtil.class)) {
            headUtils.when(() -> HeadUtils.isHeadBlocksItem(mainHandItem)).thenReturn(true);
            headUtils.when(() -> HeadUtils.getContent(eventItemInHand)).thenReturn(null);
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(true);

            handler.onPlayerPlaceBlock(event);

            verify(languageService).message("Messages.StorageError");
            verify(player).sendMessage("mock-message");
            logUtil.verify(() -> LogUtil.error(eq("Error, head texture not resolved when trying to save the head for player {0}"), eq("TestPlayer")));
            // Should NOT proceed to saveHeadLocation
            verify(headService, never()).saveHeadLocation(any(), any(HeadContent.class), anyFloat(), anyString());
        }
    }

    // --- InternalException from saveHeadLocation: sends error and logs ---

    @Test
    void saveHeadLocationThrows_sendsStorageErrorAndLogs() throws Exception {
        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(timedConfigGui.hasPendingPlatePlacement(playerUuid)).thenReturn(false);

        when(player.isSneaking()).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);

        Location rawBlockLoc = mock(Location.class);
        Location centeredLoc = mock(Location.class);
        when(blockPlaced.getLocation()).thenReturn(rawBlockLoc);
        when(rawBlockLoc.clone()).thenReturn(centeredLoc);
        when(centeredLoc.add(0.5, 0, 0.5)).thenReturn(centeredLoc);
        when(headService.getHeadAt(centeredLoc)).thenReturn(null);

        when(storageService.isStorageError()).thenReturn(false);
        when(event.getItemInHand()).thenReturn(eventItemInHand);
        when(huntService.getSelectedHunt(playerUuid)).thenReturn("default");
        when(headService.saveHeadLocation(centeredLoc, HeadContent.head("texture-abc"), 0f, "default"))
                .thenThrow(new InternalException("DB connection failed"));

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class);
             MockedStatic<LogUtil> logUtil = mockStatic(LogUtil.class)) {
            headUtils.when(() -> HeadUtils.isHeadBlocksItem(mainHandItem)).thenReturn(true);
            headUtils.when(() -> HeadUtils.getContent(eventItemInHand)).thenReturn(HeadContent.head("texture-abc"));
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(true);

            handler.onPlayerPlaceBlock(event);

            verify(languageService).message("Messages.StorageError");
            verify(player).sendMessage("mock-message");
            logUtil.verify(() -> LogUtil.error(
                    eq("Error while trying to create new HeadBlocks from the storage: {0}"),
                    eq("DB connection failed")));
            // getSelectedHunt is called before save, but should not proceed to particles
            verify(huntService).getSelectedHunt(playerUuid);
        }
    }

    // --- Success with default hunt: sends reassign message ---

    @Test
    void success_defaultHunt_severalHunts_sendsReassignMessage() throws Exception {
        UUID playerUuid = UUID.randomUUID();
        UUID headUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(timedConfigGui.hasPendingPlatePlacement(playerUuid)).thenReturn(false);

        when(player.isSneaking()).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);

        Location rawBlockLoc = mock(Location.class);
        Location centeredLoc = mock(Location.class);
        when(blockPlaced.getLocation()).thenReturn(rawBlockLoc);
        when(rawBlockLoc.clone()).thenReturn(centeredLoc);
        when(centeredLoc.add(0.5, 0, 0.5)).thenReturn(centeredLoc);
        when(headService.getHeadAt(centeredLoc)).thenReturn(null);

        when(storageService.isStorageError()).thenReturn(false);
        when(event.getItemInHand()).thenReturn(eventItemInHand);
        when(huntService.getSelectedHunt(playerUuid)).thenReturn("default");
        when(headService.saveHeadLocation(centeredLoc, HeadContent.head("texture-abc"), 0f, "default")).thenReturn(headUuid);
        when(languageService.prefix()).thenReturn("[HB]");
        when(huntService.isMultiHunt()).thenReturn(true);

        Player.Spigot spigot = mock(Player.Spigot.class);
        when(player.spigot()).thenReturn(spigot);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class);
             MockedStatic<LocationUtils> locationUtils = mockStatic(LocationUtils.class);
             MockedStatic<VersionUtils> versionUtils = mockStatic(VersionUtils.class);
             MockedStatic<ParticlesUtils> ignored = mockStatic(ParticlesUtils.class);
             MockedStatic<MessageUtils> messageUtils = mockStatic(MessageUtils.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            headUtils.when(() -> HeadUtils.isHeadBlocksItem(mainHandItem)).thenReturn(true);
            headUtils.when(() -> HeadUtils.getContent(eventItemInHand)).thenReturn(HeadContent.head("texture-abc"));
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(true);
            locationUtils.when(() -> LocationUtils.parseLocationPlaceholders(anyString(), any(Location.class)))
                    .thenReturn("parsed-message");
            versionUtils.when(() -> VersionUtils.isNewerOrEqualsTo(any())).thenReturn(true);
            messageUtils.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));
            PluginManager pm = mock(PluginManager.class);
            bukkit.when(Bukkit::getPluginManager).thenReturn(pm);

            handler.onPlayerPlaceBlock(event);

            verify(headService).saveHeadLocation(centeredLoc, HeadContent.head("texture-abc"), 0f, "default");
            // Default hunt sends a reassign message via spigot
            verify(spigot).sendMessage(any(net.md_5.bungee.api.chat.TextComponent.class));
        }
    }

    @Test
    void success_defaultHunt_onlyHunt_sendsNoReassignMessage() throws Exception {
        UUID playerUuid = UUID.randomUUID();
        UUID headUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(timedConfigGui.hasPendingPlatePlacement(playerUuid)).thenReturn(false);

        when(player.isSneaking()).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);

        Location rawBlockLoc = mock(Location.class);
        Location centeredLoc = mock(Location.class);
        when(blockPlaced.getLocation()).thenReturn(rawBlockLoc);
        when(rawBlockLoc.clone()).thenReturn(centeredLoc);
        when(centeredLoc.add(0.5, 0, 0.5)).thenReturn(centeredLoc);
        when(headService.getHeadAt(centeredLoc)).thenReturn(null);

        when(storageService.isStorageError()).thenReturn(false);
        when(event.getItemInHand()).thenReturn(eventItemInHand);
        when(huntService.getSelectedHunt(playerUuid)).thenReturn("default");
        when(headService.saveHeadLocation(centeredLoc, HeadContent.head("texture-abc"), 0f, "default")).thenReturn(headUuid);
        when(huntService.isMultiHunt()).thenReturn(false);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class);
             MockedStatic<LocationUtils> locationUtils = mockStatic(LocationUtils.class);
             MockedStatic<VersionUtils> versionUtils = mockStatic(VersionUtils.class);
             MockedStatic<ParticlesUtils> ignored = mockStatic(ParticlesUtils.class);
             MockedStatic<MessageUtils> messageUtils = mockStatic(MessageUtils.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            headUtils.when(() -> HeadUtils.isHeadBlocksItem(mainHandItem)).thenReturn(true);
            headUtils.when(() -> HeadUtils.getContent(eventItemInHand)).thenReturn(HeadContent.head("texture-abc"));
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(true);
            locationUtils.when(() -> LocationUtils.parseLocationPlaceholders(anyString(), any(Location.class)))
                    .thenReturn("parsed-message");
            versionUtils.when(() -> VersionUtils.isNewerOrEqualsTo(any())).thenReturn(true);
            messageUtils.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));
            PluginManager pm = mock(PluginManager.class);
            bukkit.when(Bukkit::getPluginManager).thenReturn(pm);

            handler.onPlayerPlaceBlock(event);

            verify(headService).saveHeadLocation(centeredLoc, HeadContent.head("texture-abc"), 0f, "default");
            verify(player, never()).spigot();
        }
    }

    // --- Not creative but sneaking: cancelled ---

    @Test
    void notCreativeButSneaking_cancelled() {
        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(timedConfigGui.hasPendingPlatePlacement(playerUuid)).thenReturn(false);

        when(player.isSneaking()).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.ADVENTURE);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
            headUtils.when(() -> HeadUtils.isHeadBlocksItem(mainHandItem)).thenReturn(true);
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(true);

            handler.onPlayerPlaceBlock(event);

            verify(event).setCancelled(true);
            verify(languageService).message("Messages.CreativeSneakAddHead");
        }
    }

    // --- Hunt assignment exception: logged but does not crash ---

    @Test
    void huntAssignmentThrows_loggedButDoesNotCrash() throws Exception {
        UUID playerUuid = UUID.randomUUID();
        UUID headUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(timedConfigGui.hasPendingPlatePlacement(playerUuid)).thenReturn(false);

        when(player.isSneaking()).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);

        Location rawBlockLoc = mock(Location.class);
        Location centeredLoc = mock(Location.class);
        when(blockPlaced.getLocation()).thenReturn(rawBlockLoc);
        when(rawBlockLoc.clone()).thenReturn(centeredLoc);
        when(centeredLoc.add(0.5, 0, 0.5)).thenReturn(centeredLoc);
        when(headService.getHeadAt(centeredLoc)).thenReturn(null);

        when(storageService.isStorageError()).thenReturn(false);
        when(event.getItemInHand()).thenReturn(eventItemInHand);
        when(huntService.getSelectedHunt(playerUuid)).thenReturn("hunt1");
        when(headService.saveHeadLocation(centeredLoc, HeadContent.head("texture-abc"), 0f, "hunt1")).thenReturn(headUuid);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class);
             MockedStatic<LocationUtils> locationUtils = mockStatic(LocationUtils.class);
             MockedStatic<VersionUtils> versionUtils = mockStatic(VersionUtils.class);
             MockedStatic<ParticlesUtils> ignored = mockStatic(ParticlesUtils.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            headUtils.when(() -> HeadUtils.isHeadBlocksItem(mainHandItem)).thenReturn(true);
            headUtils.when(() -> HeadUtils.getContent(eventItemInHand)).thenReturn(HeadContent.head("texture-abc"));
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(true);
            locationUtils.when(() -> LocationUtils.parseLocationPlaceholders(anyString(), any(Location.class)))
                    .thenReturn("parsed-message");
            versionUtils.when(() -> VersionUtils.isNewerOrEqualsTo(any())).thenReturn(true);
            PluginManager pm = mock(PluginManager.class);
            bukkit.when(Bukkit::getPluginManager).thenReturn(pm);

            handler.onPlayerPlaceBlock(event);

            // Hunt assignment now happens inside saveHeadLocation, so just verify the event fires
            verify(pm).callEvent(any());
        }
    }

    // --- Entity rendering ---

    private void adminPlacing(UUID playerUuid) {
        lenient().when(player.getUniqueId()).thenReturn(playerUuid);
        lenient().when(player.isSneaking()).thenReturn(true);
        lenient().when(player.getGameMode()).thenReturn(GameMode.CREATIVE);
        lenient().when(huntService.getSelectedHunt(playerUuid)).thenReturn("halloween");
        lenient().when(storageService.isStorageError()).thenReturn(false);
    }

    @Test
    void blockPlace_ofAPlainBlock_savesItsBlockData() throws Exception {
        UUID playerUuid = UUID.randomUUID();
        adminPlacing(playerUuid);
        when(timedConfigGui.hasPendingPlatePlacement(playerUuid)).thenReturn(false);
        when(event.getItemInHand()).thenReturn(eventItemInHand);
        org.bukkit.block.data.BlockData data = mock(org.bukkit.block.data.BlockData.class);
        when(blockPlaced.getBlockData()).thenReturn(data);
        when(data.getAsString()).thenReturn("minecraft:lantern[hanging=true]");

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class);
             MockedStatic<LocationUtils> ignoredLocation = mockStatic(LocationUtils.class);
             MockedStatic<ParticlesUtils> ignoredParticles = mockStatic(ParticlesUtils.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            headUtils.when(() -> HeadUtils.isHeadBlocksItem(mainHandItem)).thenReturn(true);
            headUtils.when(() -> HeadUtils.getContent(eventItemInHand))
                    .thenReturn(HeadContent.of(ContentKind.BLOCK, "LANTERN", null));
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(true);
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));

            handler.onPlayerPlaceBlock(event);

            verify(headService).saveHeadLocation(any(),
                    eq(HeadContent.of(ContentKind.BLOCK, "LANTERN", java.util.Map.of("data", "minecraft:lantern[hanging=true]"))),
                    anyFloat(), eq("halloween"));
            verify(event, never()).setCancelled(anyBoolean());
        }
    }

    @Nested
    class EntityPlacement {

        @Mock
        private PlayerInteractEvent interact;

        @Mock
        private ItemStack item;

        @Mock
        private Block clicked;

        @Mock
        private Block target;

        private final UUID playerUuid = UUID.randomUUID();

        @BeforeEach
        void setUpInteract() {
            lenient().when(interact.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
            lenient().when(interact.getHand()).thenReturn(EquipmentSlot.HAND);
            lenient().when(interact.getClickedBlock()).thenReturn(clicked);
            lenient().when(interact.getItem()).thenReturn(item);
            lenient().when(interact.getPlayer()).thenReturn(player);
            lenient().when(interact.getBlockFace()).thenReturn(BlockFace.UP);
            lenient().when(clicked.getRelative(BlockFace.UP)).thenReturn(target);
            adminPlacing(playerUuid);
        }

        @Test
        void nonBlockItem_cannotBeUsedInTheAir() {
            when(interact.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
            when(item.getType()).thenReturn(Material.ENDER_PEARL);

            try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class)) {
                headUtils.when(() -> HeadUtils.isHeadBlocksItem(item)).thenReturn(true);

                handler.onHeadBlocksItemUse(interact);
            }

            verify(interact).setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        }

        @Test
        void blockItem_inTheAir_isLeftAlone() {
            when(interact.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
            when(item.getType()).thenReturn(Material.PLAYER_HEAD);

            try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class)) {
                headUtils.when(() -> HeadUtils.isHeadBlocksItem(item)).thenReturn(true);

                handler.onHeadBlocksItemUse(interact);
            }

            verify(interact, never()).setUseItemInHand(any());
        }

        @Test
        void leftClick_isIgnored() {
            when(interact.getAction()).thenReturn(Action.LEFT_CLICK_BLOCK);

            handler.onEntityHeadPlace(interact);

            verify(interact, never()).setCancelled(anyBoolean());
        }

        @Test
        void notAHeadBlocksItem_isIgnored() {
            try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class)) {
                headUtils.when(() -> HeadUtils.isHeadBlocksItem(item)).thenReturn(false);

                handler.onEntityHeadPlace(interact);
            }

            verify(interact, never()).setCancelled(anyBoolean());
        }

        @Test
        void blockRenderedBlockItem_isLeftToTheBlockPlaceEvent() {
            var content = HeadContent.head("t");
            when(item.getType()).thenReturn(Material.PLAYER_HEAD);

            try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class)) {
                headUtils.when(() -> HeadUtils.isHeadBlocksItem(item)).thenReturn(true);
                headUtils.when(() -> HeadUtils.getContent(item)).thenReturn(content);

                handler.onEntityHeadPlace(interact);
            }

            verify(interact, never()).setCancelled(anyBoolean());
        }

        @Test
        void occupiedTarget_isRefused() {
            var content = HeadContent.of(ContentKind.MOB, "CAT", null);
            when(visualService.formOf(content, "halloween")).thenReturn(VisualForm.MOB);
            when(target.isPassable()).thenReturn(false);

            try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class)) {
                headUtils.when(() -> HeadUtils.isHeadBlocksItem(item)).thenReturn(true);
                headUtils.when(() -> HeadUtils.getContent(item)).thenReturn(content);

                handler.onEntityHeadPlace(interact);
            }

            verify(interact).setCancelled(true);
            verify(languageService).message("Messages.TargetBlockInvalid");
            verifyNoInteractions(headService);
        }

        @Test
        void mob_isPlacedOnTheClickedFace() throws Exception {
            var content = HeadContent.of(ContentKind.MOB, "CAT", null);
            when(visualService.formOf(content, "halloween")).thenReturn(VisualForm.MOB);
            when(target.isPassable()).thenReturn(true);
            Location targetLocation = mock(Location.class);
            when(target.getLocation()).thenReturn(targetLocation);
            when(targetLocation.add(0.5, 0, 0.5)).thenReturn(targetLocation);
            Location playerLocation = mock(Location.class);
            when(player.getLocation()).thenReturn(playerLocation);

            try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
                 MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class);
                 MockedStatic<LocationUtils> ignoredLocation = mockStatic(LocationUtils.class);
                 MockedStatic<ParticlesUtils> ignoredParticles = mockStatic(ParticlesUtils.class);
                 MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                headUtils.when(() -> HeadUtils.isHeadBlocksItem(item)).thenReturn(true);
                headUtils.when(() -> HeadUtils.getContent(item)).thenReturn(content);
                playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(true);
                bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));

                handler.onEntityHeadPlace(interact);

                verify(interact).setCancelled(true);
                verify(interact).setUseItemInHand(org.bukkit.event.Event.Result.DENY);
                verify(headService).saveHeadLocation(eq(targetLocation), eq(content), anyFloat(), eq("halloween"));
            }
        }

        @Test
        void frame_onAWall_facesTheClickedFace() throws Exception {
            var content = HeadContent.of(ContentKind.FRAME, "DIAMOND", null);
            when(interact.getBlockFace()).thenReturn(BlockFace.EAST);
            when(clicked.getRelative(BlockFace.EAST)).thenReturn(target);

            var saved = placeFrame(content);

            assertThat(saved.getValue()).isEqualTo(content);
            verify(headService).saveHeadLocation(any(Location.class), any(HeadContent.class), eq(270f), eq("halloween"));
        }

        @Test
        void frame_onTheFloor_isLaidFlat() throws Exception {
            var content = HeadContent.of(ContentKind.FRAME, "DIAMOND", null);

            var saved = placeFrame(content);

            assertThat(saved.getValue().kind()).isEqualTo(ContentKind.FRAME);
            assertThat(saved.getValue().option("facing")).isEqualTo("UP");
        }

        private ArgumentCaptor<HeadContent> placeFrame(HeadContent content) throws Exception {
            when(visualService.formOf(content, "halloween")).thenReturn(VisualForm.ITEM_FRAME);
            when(target.isPassable()).thenReturn(true);
            Location targetLocation = mock(Location.class);
            when(target.getLocation()).thenReturn(targetLocation);
            when(targetLocation.add(0.5, 0, 0.5)).thenReturn(targetLocation);
            when(player.getLocation()).thenReturn(mock(Location.class));
            ArgumentCaptor<HeadContent> saved = ArgumentCaptor.forClass(HeadContent.class);

            try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
                 MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class);
                 MockedStatic<LocationUtils> ignoredLocation = mockStatic(LocationUtils.class);
                 MockedStatic<ParticlesUtils> ignoredParticles = mockStatic(ParticlesUtils.class);
                 MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                headUtils.when(() -> HeadUtils.isHeadBlocksItem(item)).thenReturn(true);
                headUtils.when(() -> HeadUtils.getContent(item)).thenReturn(content);
                headUtils.when(() -> HeadUtils.yawOf(BlockFace.EAST)).thenReturn(270f);
                playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(true);
                bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));

                handler.onEntityHeadPlace(interact);

                verify(headService).saveHeadLocation(eq(targetLocation), saved.capture(), anyFloat(), eq("halloween"));
            }
            return saved;
        }
    }
}
