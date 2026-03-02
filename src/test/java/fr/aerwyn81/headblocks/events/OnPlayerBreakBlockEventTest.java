package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.*;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnPlayerBreakBlockEventTest {

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
    private BlockBreakEvent event;

    @Mock
    private Block block;

    @Mock
    private Player player;

    @Mock
    private Location location;

    private OnPlayerBreakBlockEvent handler;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getHeadService()).thenReturn(headService);
        lenient().when(registry.getStorageService()).thenReturn(storageService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(registry.getConfigService()).thenReturn(configService);
        lenient().when(registry.getHuntService()).thenReturn(huntService);

        lenient().when(languageService.message(anyString())).thenReturn("mock-message");
        lenient().when(event.getPlayer()).thenReturn(player);
        lenient().when(event.getBlock()).thenReturn(block);

        handler = new OnPlayerBreakBlockEvent(registry);
    }

    @AfterEach
    void tearDown() {
        HeadBlocks.isReloadInProgress = false;
    }

    // --- Not a player head: ignored ---

    @Test
    void nonPlayerHead_ignored() {
        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class)) {
            headUtils.when(() -> HeadUtils.isPlayerHead(block)).thenReturn(false);

            handler.OnBlockBreakEvent(event);

            verify(event, never()).setCancelled(anyBoolean());
            verify(headService, never()).getHeadAt(any());
        }
    }

    // --- Not a plugin head: ignored ---

    @Test
    void notPluginHead_ignored() {
        when(block.getLocation()).thenReturn(location);
        when(headService.getHeadAt(location)).thenReturn(null);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class)) {
            headUtils.when(() -> HeadUtils.isPlayerHead(block)).thenReturn(true);

            handler.OnBlockBreakEvent(event);

            verify(event, never()).setCancelled(anyBoolean());
        }
    }

    // --- Reload in progress: cancelled ---

    @Test
    void reloadInProgress_cancelled() {
        HeadBlocks.isReloadInProgress = true;
        HeadLocation headLocation = mock(HeadLocation.class);

        when(block.getLocation()).thenReturn(location);
        when(headService.getHeadAt(location)).thenReturn(headLocation);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class)) {
            headUtils.when(() -> HeadUtils.isPlayerHead(block)).thenReturn(true);

            handler.OnBlockBreakEvent(event);

            verify(event).setCancelled(true);
            verify(languageService).message("Messages.PluginReloading");
        }
    }

    // --- No permission: cancelled ---

    @Test
    void noPermission_cancelled() {
        HeadLocation headLocation = mock(HeadLocation.class);

        when(block.getLocation()).thenReturn(location);
        when(headService.getHeadAt(location)).thenReturn(headLocation);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
            headUtils.when(() -> HeadUtils.isPlayerHead(block)).thenReturn(true);
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(false);

            handler.OnBlockBreakEvent(event);

            verify(event).setCancelled(true);
        }
    }

    // --- Not sneaking: cancelled ---

    @Test
    void notSneaking_cancelled() {
        HeadLocation headLocation = mock(HeadLocation.class);

        when(block.getLocation()).thenReturn(location);
        when(headService.getHeadAt(location)).thenReturn(headLocation);
        when(player.isSneaking()).thenReturn(false);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
            headUtils.when(() -> HeadUtils.isPlayerHead(block)).thenReturn(true);
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(true);

            handler.OnBlockBreakEvent(event);

            verify(event).setCancelled(true);
            verify(languageService).message("Messages.CreativeSneakRemoveHead");
        }
    }

    // --- Not creative: cancelled ---

    @Test
    void notCreative_cancelled() {
        HeadLocation headLocation = mock(HeadLocation.class);

        when(block.getLocation()).thenReturn(location);
        when(headService.getHeadAt(location)).thenReturn(headLocation);
        when(player.isSneaking()).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
            headUtils.when(() -> HeadUtils.isPlayerHead(block)).thenReturn(true);
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(true);

            handler.OnBlockBreakEvent(event);

            verify(event).setCancelled(true);
            verify(languageService).message("Messages.CreativeSneakRemoveHead");
        }
    }

    // --- Storage error: cancelled ---

    @Test
    void storageError_cancelled() {
        HeadLocation headLocation = mock(HeadLocation.class);

        when(block.getLocation()).thenReturn(location);
        when(headService.getHeadAt(location)).thenReturn(headLocation);
        when(player.isSneaking()).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);
        when(storageService.isStorageError()).thenReturn(true);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
            headUtils.when(() -> HeadUtils.isPlayerHead(block)).thenReturn(true);
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(true);

            handler.OnBlockBreakEvent(event);

            verify(event).setCancelled(true);
            verify(languageService).message("Messages.StorageError");
        }
    }

    // --- Success: head removed ---

    @Test
    void success_headRemoved() throws InternalException {
        HeadLocation headLocation = mock(HeadLocation.class);
        UUID headUuid = UUID.randomUUID();
        when(headLocation.getUuid()).thenReturn(headUuid);

        when(block.getLocation()).thenReturn(location);
        when(headService.getHeadAt(location)).thenReturn(headLocation);
        when(player.isSneaking()).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);
        when(storageService.isStorageError()).thenReturn(false);
        when(configService.resetPlayerData()).thenReturn(true);

        try (MockedStatic<HeadUtils> headUtils = mockStatic(HeadUtils.class);
             MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class);
             MockedStatic<LocationUtils> locationUtils = mockStatic(LocationUtils.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            headUtils.when(() -> HeadUtils.isPlayerHead(block)).thenReturn(true);
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(true);
            locationUtils.when(() -> LocationUtils.parseLocationPlaceholders(anyString(), any(Location.class)))
                    .thenReturn("parsed-message");
            PluginManager pm = mock(PluginManager.class);
            bukkit.when(Bukkit::getPluginManager).thenReturn(pm);

            handler.OnBlockBreakEvent(event);

            verify(headService).removeHeadLocation(headLocation, true);
            verify(event).setCancelled(false);
        }
    }
}
