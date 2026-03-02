package fr.aerwyn81.headblocks.utils.internal;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.StorageService;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommandsUtilsTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private StorageService storageService;

    @Mock
    private LanguageService languageService;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getStorageService()).thenReturn(storageService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(languageService.message(anyString())).thenReturn("mock-message");
        lenient().when(languageService.message(anyString(), anyString())).thenReturn("mock-message");
    }

    @Nested
    class WithPlayerName {

        @Mock
        private Player sender;

        @Test
        void otherPlayerName_canSeeOther_returnsProfile() throws InternalException {
            UUID uuid = UUID.randomUUID();
            PlayerProfileLight profile = new PlayerProfileLight(uuid, "otherPlayer", "");
            when(storageService.getPlayerByName("otherPlayer")).thenReturn(profile);

            var result = CommandsUtils.extractAndGetPlayerUuidByName(
                    registry, sender, new String[]{"cmd", "otherPlayer"}, true);

            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("otherPlayer");
        }

        @Test
        void otherPlayerName_cannotSeeOther_sendsNoPermission() throws InternalException {
            var result = CommandsUtils.extractAndGetPlayerUuidByName(
                    registry, sender, new String[]{"cmd", "otherPlayer"}, false);

            assertThat(result).isNull();
            verify(languageService).message("Messages.NoPermission");
        }

        @Test
        void playerNotFound_sendsNotFoundMessage() throws InternalException {
            when(storageService.getPlayerByName("unknown")).thenReturn(null);

            var result = CommandsUtils.extractAndGetPlayerUuidByName(
                    registry, sender, new String[]{"cmd", "unknown"}, true);

            assertThat(result).isNull();
            verify(languageService).message("Messages.PlayerNotFound", "unknown");
        }

        @Test
        void storageError_sendsErrorMessage() throws InternalException {
            when(storageService.getPlayerByName("player1")).thenThrow(new RuntimeException("db error"));

            var result = CommandsUtils.extractAndGetPlayerUuidByName(
                    registry, sender, new String[]{"cmd", "player1"}, true);

            assertThat(result).isNull();
            verify(languageService).message("Messages.StorageError");
        }
    }

    @Nested
    class WithoutPlayerName {

        @Mock
        private Player playerSender;

        @Test
        void playerSender_usesSenderName() throws InternalException {
            UUID uuid = UUID.randomUUID();
            when(playerSender.getName()).thenReturn("me");
            PlayerProfileLight profile = new PlayerProfileLight(uuid, "me", "");
            when(storageService.getPlayerByName("me")).thenReturn(profile);

            var result = CommandsUtils.extractAndGetPlayerUuidByName(
                    registry, playerSender, new String[]{"cmd"}, true);

            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("me");
        }

        @Test
        void consoleSender_returnsNull() {
            ConsoleCommandSender console = mock(ConsoleCommandSender.class);

            var result = CommandsUtils.extractAndGetPlayerUuidByName(
                    registry, console, new String[]{"cmd"}, true);

            assertThat(result).isNull();
        }

        @Test
        void numericArg_treatedAsNoPlayerName() throws InternalException {
            // args[1] is a number (page number), so it's treated as "no player name"
            when(playerSender.getName()).thenReturn("me");
            UUID uuid = UUID.randomUUID();
            PlayerProfileLight profile = new PlayerProfileLight(uuid, "me", "");
            when(storageService.getPlayerByName("me")).thenReturn(profile);

            var result = CommandsUtils.extractAndGetPlayerUuidByName(
                    registry, playerSender, new String[]{"cmd", "2"}, true);

            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("me");
        }
    }
}
