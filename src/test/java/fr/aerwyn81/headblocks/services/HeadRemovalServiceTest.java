package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.api.events.HeadDeletedEvent;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeadRemovalServiceTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private LanguageService languageService;

    @Mock
    private StorageService storageService;

    @Mock
    private HeadService headService;

    @Mock
    private ConfigService configService;

    @Mock
    private Player player;

    @Mock
    private HeadLocation head;

    @Mock
    private Location location;

    private HeadRemovalService removalService;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(registry.getStorageService()).thenReturn(storageService);
        lenient().when(registry.getHeadService()).thenReturn(headService);
        lenient().when(registry.getConfigService()).thenReturn(configService);
        lenient().when(languageService.message(anyString())).thenReturn("mock-message");

        removalService = new HeadRemovalService(registry);
    }

    @AfterEach
    void tearDown() {
        HeadBlocks.isReloadInProgress = false;
    }

    @Test
    void canRemove_duringReload_isRefused() {
        HeadBlocks.isReloadInProgress = true;

        assertThat(removalService.canRemove(player)).isFalse();
        verify(languageService).message("Messages.PluginReloading");
    }

    @Test
    void canRemove_withoutPermission_isRefused() {
        try (MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(false);

            assertThat(removalService.canRemove(player)).isFalse();
            verify(player).sendMessage("mock-message");
        }
    }

    @Test
    void canRemove_notSneakingInCreative_isRefused() {
        when(player.isSneaking()).thenReturn(false);

        try (MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(true);

            assertThat(removalService.canRemove(player)).isFalse();
            verify(languageService).message("Messages.CreativeSneakRemoveHead");
        }
    }

    @Test
    void canRemove_sneakingAdminInCreative_isAllowed() {
        when(player.isSneaking()).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);

        try (MockedStatic<PlayerUtils> playerUtils = mockStatic(PlayerUtils.class)) {
            playerUtils.when(() -> PlayerUtils.hasPermission(player, "headblocks.admin")).thenReturn(true);

            assertThat(removalService.canRemove(player)).isTrue();
            verify(player, never()).sendMessage(anyString());
        }
    }

    @Test
    void remove_storageError_keepsTheHead() throws InternalException {
        when(storageService.isStorageError()).thenReturn(true);

        assertThat(removalService.remove(player, head, location)).isFalse();
        verify(headService, never()).removeHeadLocation(any(), anyBoolean());
    }

    @Test
    void remove_success_removesAndFiresTheEvent() throws InternalException {
        UUID uuid = UUID.randomUUID();
        when(head.getUuid()).thenReturn(uuid);
        when(head.getHuntId()).thenReturn("default");
        when(configService.resetPlayerData()).thenReturn(true);
        PluginManager pluginManager = mock(PluginManager.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<LocationUtils> locationUtils = mockStatic(LocationUtils.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            locationUtils.when(() -> LocationUtils.parseLocationPlaceholders("mock-message", location)).thenReturn("removed");

            assertThat(removalService.remove(player, head, location)).isTrue();

            verify(headService).removeHeadLocation(head, true);
            verify(player).sendMessage("removed");
            verify(pluginManager).callEvent(any(HeadDeletedEvent.class));
        }
    }

    @Test
    void remove_storageFailure_reportsIt() throws InternalException {
        doThrow(new InternalException("db down")).when(headService).removeHeadLocation(head, false);

        try (MockedStatic<LogUtil> ignored = mockStatic(LogUtil.class)) {
            assertThat(removalService.remove(player, head, location)).isFalse();
            verify(languageService).message("Messages.StorageError");
        }
    }
}
