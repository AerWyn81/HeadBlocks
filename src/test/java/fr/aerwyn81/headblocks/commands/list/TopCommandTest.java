package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TopCommandTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private StorageService storageService;

    @Mock
    private LanguageService languageService;

    @Mock
    private Player playerSender;

    @Mock
    private ConsoleCommandSender consoleSender;

    private Top command;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getStorageService()).thenReturn(storageService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(languageService.message(anyString())).thenReturn("mock-message");
        lenient().when(languageService.message(anyString(), anyString())).thenReturn("mock-message");
        command = new Top(registry);
    }

    @Nested
    class StorageError {

        @Test
        void getTopPlayersThrowsException_sendsStorageError() throws InternalException {
            when(storageService.getTopPlayers()).thenThrow(new InternalException("db error"));

            try (MockedStatic<LogUtil> logUtil = mockStatic(LogUtil.class)) {
                boolean result = command.perform(playerSender, new String[]{"top"});

                assertThat(result).isTrue();
                verify(languageService).message("Messages.StorageError");
                verify(playerSender).sendMessage("mock-message");
            }
        }

        @Test
        void consoleSender_storageError_sendsErrorMessage() throws InternalException {
            when(storageService.getTopPlayers()).thenThrow(new InternalException("db error"));

            try (MockedStatic<LogUtil> logUtil = mockStatic(LogUtil.class)) {
                boolean result = command.perform(consoleSender, new String[]{"top"});

                assertThat(result).isTrue();
                verify(languageService).message("Messages.StorageError");
                verify(consoleSender).sendMessage("mock-message");
            }
        }
    }

    @Nested
    class EmptyLeaderboard {

        @Test
        void noPlayers_sendsTopEmpty() throws InternalException {
            when(storageService.getTopPlayers()).thenReturn(new LinkedHashMap<>());

            boolean result = command.perform(playerSender, new String[]{"top"});

            assertThat(result).isTrue();
            verify(languageService).message("Messages.TopEmpty");
            verify(playerSender).sendMessage("mock-message");
        }

        @Test
        void consoleSender_noPlayers_sendsTopEmpty() throws InternalException {
            when(storageService.getTopPlayers()).thenReturn(new LinkedHashMap<>());

            boolean result = command.perform(consoleSender, new String[]{"top"});

            assertThat(result).isTrue();
            verify(languageService).message("Messages.TopEmpty");
            verify(consoleSender).sendMessage("mock-message");
        }
    }

    @Nested
    class GlobalLeaderboard {

        @Test
        void consoleSender_singlePlayer_sendsLeaderboardLine() throws InternalException {
            UUID pUuid = UUID.randomUUID();
            PlayerProfileLight profile = new PlayerProfileLight(pUuid, "Alice", "Alice");
            LinkedHashMap<PlayerProfileLight, Integer> topMap = new LinkedHashMap<>();
            topMap.put(profile, 5);

            when(storageService.getTopPlayers()).thenReturn(topMap);

            try (MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class)) {
                mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));

                boolean result = command.perform(consoleSender, new String[]{"top"});

                assertThat(result).isTrue();
                // Console sender gets title + line
                verify(consoleSender).sendMessage("mock-message"); // title
                verify(languageService).message("Chat.TopTitle");
                verify(languageService).message("Chat.LineTop", "Alice");
            }
        }

        @Test
        void consoleSender_multiplePlayers_sendsAllLines() throws InternalException {
            PlayerProfileLight alice = new PlayerProfileLight(UUID.randomUUID(), "Alice", "Alice");
            PlayerProfileLight bob = new PlayerProfileLight(UUID.randomUUID(), "Bob", "Bob");
            PlayerProfileLight charlie = new PlayerProfileLight(UUID.randomUUID(), "Charlie", "Charlie");

            LinkedHashMap<PlayerProfileLight, Integer> topMap = new LinkedHashMap<>();
            topMap.put(alice, 10);
            topMap.put(bob, 7);
            topMap.put(charlie, 3);

            when(storageService.getTopPlayers()).thenReturn(topMap);

            try (MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class)) {
                mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));

                boolean result = command.perform(consoleSender, new String[]{"top"});

                assertThat(result).isTrue();
                // Title + 3 player lines (each colorized with &6)
                verify(consoleSender, times(4)).sendMessage(anyString());
                verify(languageService).message("Chat.LineTop", "Alice");
                verify(languageService).message("Chat.LineTop", "Bob");
                verify(languageService).message("Chat.LineTop", "Charlie");
            }
        }

        @Test
        void playerSender_withAdminPermission_sendsRichClickableMessages() throws InternalException {
            UUID pUuid = UUID.randomUUID();
            PlayerProfileLight profile = new PlayerProfileLight(pUuid, "Alice", "Alice");
            LinkedHashMap<PlayerProfileLight, Integer> topMap = new LinkedHashMap<>();
            topMap.put(profile, 5);

            when(storageService.getTopPlayers()).thenReturn(topMap);

            try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class)) {
                pu.when(() -> PlayerUtils.hasPermission(playerSender, "headblocks.admin")).thenReturn(true);

                Player.Spigot spigot = mock(Player.Spigot.class);
                when(playerSender.spigot()).thenReturn(spigot);

                boolean result = command.perform(playerSender, new String[]{"top"});

                assertThat(result).isTrue();
                verify(languageService).message("Chat.TopTitle");
                verify(languageService).message("Chat.LineTop", "Alice");
                verify(languageService).message("Chat.Hover.LineTop");
                // Rich message sent via spigot
                verify(spigot, atLeastOnce()).sendMessage(any(net.md_5.bungee.api.chat.BaseComponent.class));
            }
        }

        @Test
        void playerSender_withoutAdminPermission_sendsNonClickableMessages() throws InternalException {
            UUID pUuid = UUID.randomUUID();
            PlayerProfileLight profile = new PlayerProfileLight(pUuid, "Alice", "Alice");
            LinkedHashMap<PlayerProfileLight, Integer> topMap = new LinkedHashMap<>();
            topMap.put(profile, 5);

            when(storageService.getTopPlayers()).thenReturn(topMap);

            try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class)) {
                pu.when(() -> PlayerUtils.hasPermission(playerSender, "headblocks.admin")).thenReturn(false);

                Player.Spigot spigot = mock(Player.Spigot.class);
                when(playerSender.spigot()).thenReturn(spigot);

                boolean result = command.perform(playerSender, new String[]{"top"});

                assertThat(result).isTrue();
                verify(languageService).message("Chat.LineTop", "Alice");
                // No hover for line top since no admin permission
                verify(languageService, never()).message("Chat.Hover.LineTop");
                verify(spigot, atLeastOnce()).sendMessage(any(net.md_5.bungee.api.chat.BaseComponent.class));
            }
        }
    }

    @Nested
    class PaginationTests {

        @Test
        void withPageArg_paginatesCorrectly() throws InternalException {
            PlayerProfileLight alice = new PlayerProfileLight(UUID.randomUUID(), "Alice", "Alice");
            LinkedHashMap<PlayerProfileLight, Integer> topMap = new LinkedHashMap<>();
            topMap.put(alice, 10);

            when(storageService.getTopPlayers()).thenReturn(topMap);

            try (MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class)) {
                mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));

                boolean result = command.perform(consoleSender, new String[]{"top", "1"});

                assertThat(result).isTrue();
                // Title + entry
                verify(consoleSender, times(2)).sendMessage(anyString());
            }
        }

        @Test
        void invalidPageArg_defaultsToPageOne() throws InternalException {
            PlayerProfileLight alice = new PlayerProfileLight(UUID.randomUUID(), "Alice", "Alice");
            LinkedHashMap<PlayerProfileLight, Integer> topMap = new LinkedHashMap<>();
            topMap.put(alice, 10);

            when(storageService.getTopPlayers()).thenReturn(topMap);

            try (MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class)) {
                mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));

                boolean result = command.perform(consoleSender, new String[]{"top", "abc"});

                assertThat(result).isTrue();
                // Still shows results (defaults to page 1)
                verify(consoleSender, times(2)).sendMessage(anyString());
            }
        }
    }

    @Nested
    class TabCompletion {

        @Test
        void anyArgs_returnsEmptyList() {
            ArrayList<String> result = command.tabComplete(playerSender, new String[]{"top"});

            assertThat(result).isEmpty();
        }

        @Test
        void withSecondArg_returnsEmptyList() {
            ArrayList<String> result = command.tabComplete(playerSender, new String[]{"top", ""});

            assertThat(result).isEmpty();
        }

        @Test
        void withThirdArg_returnsEmptyList() {
            ArrayList<String> result = command.tabComplete(playerSender, new String[]{"top", "1", ""});

            assertThat(result).isEmpty();
        }
    }
}
