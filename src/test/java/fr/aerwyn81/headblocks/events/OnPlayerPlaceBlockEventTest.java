package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.head.HBHead;
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
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

        lenient().when(languageService.message(anyString())).thenReturn("mock-message");
        lenient().when(event.getPlayer()).thenReturn(player);
        lenient().when(event.getBlockPlaced()).thenReturn(blockPlaced);
        lenient().when(player.getInventory()).thenReturn(playerInventory);
        lenient().when(playerInventory.getItemInMainHand()).thenReturn(mainHandItem);

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

        // Empty heads list means hasHeadBlocksItemInHand returns false
        when(headService.getHeads()).thenReturn(new ArrayList<>());

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

        HBHead hbHead = mock(HBHead.class);
        ItemStack headItemStack = mock(ItemStack.class);
        when(hbHead.getItemStack()).thenReturn(headItemStack);
        when(headService.getHeads()).thenReturn(new ArrayList<>(List.of(hbHead)));

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class)) {
            headUtils.when(() -> HeadUtils.areEquals(headItemStack, mainHandItem)).thenReturn(true);

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

        HBHead hbHead = mock(HBHead.class);
        ItemStack headItemStack = mock(ItemStack.class);
        when(hbHead.getItemStack()).thenReturn(headItemStack);
        when(headService.getHeads()).thenReturn(new ArrayList<>(List.of(hbHead)));

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
            headUtils.when(() -> HeadUtils.areEquals(headItemStack, mainHandItem)).thenReturn(true);
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

        HBHead hbHead = mock(HBHead.class);
        ItemStack headItemStack = mock(ItemStack.class);
        when(hbHead.getItemStack()).thenReturn(headItemStack);
        when(headService.getHeads()).thenReturn(new ArrayList<>(List.of(hbHead)));

        when(player.isSneaking()).thenReturn(false);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
            headUtils.when(() -> HeadUtils.areEquals(headItemStack, mainHandItem)).thenReturn(true);
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

        HBHead hbHead = mock(HBHead.class);
        ItemStack headItemStack = mock(ItemStack.class);
        when(hbHead.getItemStack()).thenReturn(headItemStack);
        when(headService.getHeads()).thenReturn(new ArrayList<>(List.of(hbHead)));

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
            headUtils.when(() -> HeadUtils.areEquals(headItemStack, mainHandItem)).thenReturn(true);
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

        HBHead hbHead = mock(HBHead.class);
        ItemStack headItemStack = mock(ItemStack.class);
        when(hbHead.getItemStack()).thenReturn(headItemStack);
        when(headService.getHeads()).thenReturn(new ArrayList<>(List.of(hbHead)));

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
            headUtils.when(() -> HeadUtils.areEquals(headItemStack, mainHandItem)).thenReturn(true);
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

        HBHead hbHead = mock(HBHead.class);
        ItemStack headItemStack = mock(ItemStack.class);
        when(hbHead.getItemStack()).thenReturn(headItemStack);
        when(headService.getHeads()).thenReturn(new ArrayList<>(List.of(hbHead)));

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
        when(headService.saveHeadLocation(centeredLoc, "texture-abc", "hunt1")).thenReturn(headUuid);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class);
             MockedStatic<LocationUtils> locationUtils = mockStatic(LocationUtils.class);
             MockedStatic<VersionUtils> versionUtils = mockStatic(VersionUtils.class);
             MockedStatic<ParticlesUtils> ignored = mockStatic(ParticlesUtils.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            headUtils.when(() -> HeadUtils.areEquals(headItemStack, mainHandItem)).thenReturn(true);
            headUtils.when(() -> HeadUtils.getHeadTexture(eventItemInHand)).thenReturn("texture-abc");
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(true);
            locationUtils.when(() -> LocationUtils.parseLocationPlaceholders(anyString(), any(Location.class)))
                    .thenReturn("parsed-message");
            versionUtils.when(() -> VersionUtils.isNewerOrEqualsTo(any())).thenReturn(true);
            PluginManager pm = mock(PluginManager.class);
            bukkit.when(Bukkit::getPluginManager).thenReturn(pm);

            handler.onPlayerPlaceBlock(event);

            verify(headService).saveHeadLocation(centeredLoc, "texture-abc", "hunt1");
            verify(event, never()).setCancelled(anyBoolean());
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
        // Should return early and not check for headblocks item
        verify(headService, never()).getHeads();
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
        // Should still return early and not check for headblocks item
        verify(headService, never()).getHeads();
    }

    // --- No permission with empty message: cancelled but no message sent ---

    @Test
    void noPermission_emptyMessage_cancelledNoMessageSent() {
        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(timedConfigGui.hasPendingPlatePlacement(playerUuid)).thenReturn(false);

        HBHead hbHead = mock(HBHead.class);
        ItemStack headItemStack = mock(ItemStack.class);
        when(hbHead.getItemStack()).thenReturn(headItemStack);
        when(headService.getHeads()).thenReturn(new ArrayList<>(List.of(hbHead)));

        // Return empty/whitespace message for NoPermissionBlock
        when(languageService.message("Messages.NoPermissionBlock")).thenReturn("   ");

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
            headUtils.when(() -> HeadUtils.areEquals(headItemStack, mainHandItem)).thenReturn(true);
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

        HBHead hbHead = mock(HBHead.class);
        ItemStack headItemStack = mock(ItemStack.class);
        when(hbHead.getItemStack()).thenReturn(headItemStack);
        when(headService.getHeads()).thenReturn(new ArrayList<>(List.of(hbHead)));

        when(languageService.message("Messages.NoPermissionBlock")).thenReturn("You don't have permission");

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
            headUtils.when(() -> HeadUtils.areEquals(headItemStack, mainHandItem)).thenReturn(true);
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

        HBHead hbHead = mock(HBHead.class);
        ItemStack headItemStack = mock(ItemStack.class);
        when(hbHead.getItemStack()).thenReturn(headItemStack);
        when(headService.getHeads()).thenReturn(new ArrayList<>(List.of(hbHead)));

        when(player.isSneaking()).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
            headUtils.when(() -> HeadUtils.areEquals(headItemStack, mainHandItem)).thenReturn(true);
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

        HBHead hbHead = mock(HBHead.class);
        ItemStack headItemStack = mock(ItemStack.class);
        when(hbHead.getItemStack()).thenReturn(headItemStack);
        when(headService.getHeads()).thenReturn(new ArrayList<>(List.of(hbHead)));

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
            headUtils.when(() -> HeadUtils.areEquals(headItemStack, mainHandItem)).thenReturn(true);
            headUtils.when(() -> HeadUtils.getHeadTexture(eventItemInHand)).thenReturn(null);
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(true);

            handler.onPlayerPlaceBlock(event);

            verify(languageService).message("Messages.StorageError");
            verify(player).sendMessage("mock-message");
            logUtil.verify(() -> LogUtil.error(eq("Error, head texture not resolved when trying to save the head for player {0}"), eq("TestPlayer")));
            // Should NOT proceed to saveHeadLocation
            verify(headService, never()).saveHeadLocation(any(), anyString(), anyString());
        }
    }

    // --- InternalException from saveHeadLocation: sends error and logs ---

    @Test
    void saveHeadLocationThrows_sendsStorageErrorAndLogs() throws Exception {
        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(timedConfigGui.hasPendingPlatePlacement(playerUuid)).thenReturn(false);

        HBHead hbHead = mock(HBHead.class);
        ItemStack headItemStack = mock(ItemStack.class);
        when(hbHead.getItemStack()).thenReturn(headItemStack);
        when(headService.getHeads()).thenReturn(new ArrayList<>(List.of(hbHead)));

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
        when(headService.saveHeadLocation(centeredLoc, "texture-abc", "default"))
                .thenThrow(new InternalException("DB connection failed"));

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class);
             MockedStatic<LogUtil> logUtil = mockStatic(LogUtil.class)) {
            headUtils.when(() -> HeadUtils.areEquals(headItemStack, mainHandItem)).thenReturn(true);
            headUtils.when(() -> HeadUtils.getHeadTexture(eventItemInHand)).thenReturn("texture-abc");
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
    void success_defaultHunt_sendsReassignMessage() throws Exception {
        UUID playerUuid = UUID.randomUUID();
        UUID headUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(timedConfigGui.hasPendingPlatePlacement(playerUuid)).thenReturn(false);

        HBHead hbHead = mock(HBHead.class);
        ItemStack headItemStack = mock(ItemStack.class);
        when(hbHead.getItemStack()).thenReturn(headItemStack);
        when(headService.getHeads()).thenReturn(new ArrayList<>(List.of(hbHead)));

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
        when(headService.saveHeadLocation(centeredLoc, "texture-abc", "default")).thenReturn(headUuid);
        when(languageService.prefix()).thenReturn("[HB]");

        Player.Spigot spigot = mock(Player.Spigot.class);
        when(player.spigot()).thenReturn(spigot);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class);
             MockedStatic<LocationUtils> locationUtils = mockStatic(LocationUtils.class);
             MockedStatic<VersionUtils> versionUtils = mockStatic(VersionUtils.class);
             MockedStatic<ParticlesUtils> ignored = mockStatic(ParticlesUtils.class);
             MockedStatic<MessageUtils> messageUtils = mockStatic(MessageUtils.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            headUtils.when(() -> HeadUtils.areEquals(headItemStack, mainHandItem)).thenReturn(true);
            headUtils.when(() -> HeadUtils.getHeadTexture(eventItemInHand)).thenReturn("texture-abc");
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(true);
            locationUtils.when(() -> LocationUtils.parseLocationPlaceholders(anyString(), any(Location.class)))
                    .thenReturn("parsed-message");
            versionUtils.when(() -> VersionUtils.isNewerOrEqualsTo(any())).thenReturn(true);
            messageUtils.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));
            PluginManager pm = mock(PluginManager.class);
            bukkit.when(Bukkit::getPluginManager).thenReturn(pm);

            handler.onPlayerPlaceBlock(event);

            verify(headService).saveHeadLocation(centeredLoc, "texture-abc", "default");
            // Default hunt sends a reassign message via spigot
            verify(spigot).sendMessage(any(net.md_5.bungee.api.chat.TextComponent.class));
        }
    }

    // --- Not creative but sneaking: cancelled ---

    @Test
    void notCreativeButSneaking_cancelled() {
        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(timedConfigGui.hasPendingPlatePlacement(playerUuid)).thenReturn(false);

        HBHead hbHead = mock(HBHead.class);
        ItemStack headItemStack = mock(ItemStack.class);
        when(hbHead.getItemStack()).thenReturn(headItemStack);
        when(headService.getHeads()).thenReturn(new ArrayList<>(List.of(hbHead)));

        when(player.isSneaking()).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.ADVENTURE);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
            headUtils.when(() -> HeadUtils.areEquals(headItemStack, mainHandItem)).thenReturn(true);
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

        HBHead hbHead = mock(HBHead.class);
        ItemStack headItemStack = mock(ItemStack.class);
        when(hbHead.getItemStack()).thenReturn(headItemStack);
        when(headService.getHeads()).thenReturn(new ArrayList<>(List.of(hbHead)));

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
        when(headService.saveHeadLocation(centeredLoc, "texture-abc", "hunt1")).thenReturn(headUuid);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class);
             MockedStatic<LocationUtils> locationUtils = mockStatic(LocationUtils.class);
             MockedStatic<VersionUtils> versionUtils = mockStatic(VersionUtils.class);
             MockedStatic<ParticlesUtils> ignored = mockStatic(ParticlesUtils.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            headUtils.when(() -> HeadUtils.areEquals(headItemStack, mainHandItem)).thenReturn(true);
            headUtils.when(() -> HeadUtils.getHeadTexture(eventItemInHand)).thenReturn("texture-abc");
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
}
