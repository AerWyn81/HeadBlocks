package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.HuntService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResetCommandTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private HuntService huntService;

    @Mock
    private StorageService storageService;

    @Mock
    private LanguageService languageService;

    @Mock
    private HeadService headService;

    @Mock
    private Player playerSender;

    private Reset command;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getHuntService()).thenReturn(huntService);
        lenient().when(registry.getStorageService()).thenReturn(storageService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(registry.getHeadService()).thenReturn(headService);
        lenient().when(languageService.message(anyString())).thenReturn("mock-message");
        lenient().when(languageService.message(anyString(), anyString())).thenReturn("mock-message");
        command = new Reset(registry);
    }

    @Test
    void multiHuntMode_sendsRequireHuntMessage() {
        when(huntService.isMultiHunt()).thenReturn(true);

        boolean result = command.perform(playerSender, new String[]{"reset", "player1"});

        assertThat(result).isTrue();
        verify(languageService).message("Messages.HuntResetRequireHunt");
    }

    @Test
    void playerNotFound_sendsError() throws InternalException {
        when(huntService.isMultiHunt()).thenReturn(false);
        when(storageService.getPlayerByName("unknown")).thenReturn(null);

        boolean result = command.perform(playerSender, new String[]{"reset", "unknown"});

        assertThat(result).isTrue();
        verify(languageService).message("Messages.PlayerNotFound", "unknown");
    }

    @Test
    void storageErrorOnLookup_sendsError() throws InternalException {
        when(huntService.isMultiHunt()).thenReturn(false);
        when(storageService.getPlayerByName("player1")).thenThrow(new RuntimeException("db error"));

        boolean result = command.perform(playerSender, new String[]{"reset", "player1"});

        assertThat(result).isTrue();
        verify(languageService).message("Messages.StorageError");
    }

    @Nested
    class ResetFullPlayer {

        @Test
        void resetSuccess_sendsMessage() throws InternalException {
            UUID playerUuid = UUID.randomUUID();
            PlayerProfileLight profile = new PlayerProfileLight(playerUuid, "player1", "");
            when(huntService.isMultiHunt()).thenReturn(false);
            when(storageService.getPlayerByName("player1")).thenReturn(profile);

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
                 MockedStatic<HeadBlocks> hb = mockStatic(HeadBlocks.class)) {
                bukkit.when(() -> Bukkit.getPlayer(playerUuid)).thenReturn(null);

                boolean result = command.perform(playerSender, new String[]{"reset", "player1"});

                assertThat(result).isTrue();
                verify(storageService).resetPlayer(playerUuid);
                verify(languageService).message("Messages.PlayerReset", "player1");
            }
        }

        @Test
        void resetStorageError_sendsError() throws InternalException {
            UUID playerUuid = UUID.randomUUID();
            PlayerProfileLight profile = new PlayerProfileLight(playerUuid, "player1", "");
            when(huntService.isMultiHunt()).thenReturn(false);
            when(storageService.getPlayerByName("player1")).thenReturn(profile);
            doThrow(new InternalException("db error")).when(storageService).resetPlayer(playerUuid);

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {

                boolean result = command.perform(playerSender, new String[]{"reset", "player1"});

                assertThat(result).isTrue();
                verify(languageService).message("Messages.StorageError");
            }
        }
    }

    @Nested
    class ResetSpecificHead {

        @Test
        void headFound_resetsSingleHead() throws InternalException {
            UUID playerUuid = UUID.randomUUID();
            UUID headUuid = UUID.randomUUID();
            PlayerProfileLight profile = new PlayerProfileLight(playerUuid, "player1", "");
            HeadLocation headLocation = mock(HeadLocation.class);

            when(huntService.isMultiHunt()).thenReturn(false);
            when(storageService.getPlayerByName("player1")).thenReturn(profile);
            when(headService.resolveHeadIdentifier(headUuid.toString())).thenReturn(headLocation);
            when(headLocation.getUuid()).thenReturn(headUuid);
            when(headService.getHeadByUUID(headUuid)).thenReturn(headLocation);
            when(headLocation.getNameOrUnnamed(anyString())).thenReturn("myHead");

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
                 MockedStatic<HeadBlocks> hb = mockStatic(HeadBlocks.class)) {
                bukkit.when(() -> Bukkit.getPlayer(playerUuid)).thenReturn(null);

                boolean result = command.perform(playerSender, new String[]{"reset", "player1", "--head", headUuid.toString()});

                assertThat(result).isTrue();
                verify(storageService).resetPlayerHead(playerUuid, headUuid);
            }
        }

        @Test
        void headNotFound_sendsError() throws InternalException {
            UUID playerUuid = UUID.randomUUID();
            PlayerProfileLight profile = new PlayerProfileLight(playerUuid, "player1", "");

            when(huntService.isMultiHunt()).thenReturn(false);
            when(storageService.getPlayerByName("player1")).thenReturn(profile);
            when(headService.resolveHeadIdentifier("unknownHead")).thenReturn(null);

            boolean result = command.perform(playerSender, new String[]{"reset", "player1", "--head", "unknownHead"});

            assertThat(result).isTrue();
            verify(languageService).message("Messages.HeadNameNotFound");
        }
    }

    @Nested
    class TabCompletion {

        @Test
        void thirdArg_returnsHeadFlag() {
            var result = command.tabComplete(playerSender, new String[]{"reset", "player1", ""});

            assertThat(result).contains("--head");
        }

        @Test
        void fourthArgAfterHeadFlag_returnsHeadNames() {
            var names = new java.util.ArrayList<>(java.util.List.of("head1", "head2"));
            when(headService.getHeadRawNameOrUuid()).thenReturn(names);

            var result = command.tabComplete(playerSender, new String[]{"reset", "player1", "--head", ""});

            assertThat(result).containsExactly("head1", "head2");
        }
    }
}
