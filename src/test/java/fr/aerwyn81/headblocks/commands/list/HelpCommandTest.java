package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.commands.HBCommand;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HelpCommandTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private LanguageService languageService;

    @Mock
    private Player playerSender;

    @Mock
    private ConsoleCommandSender consoleSender;

    private Help command;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(languageService.message(anyString())).thenReturn("mock-message");
        command = new Help(registry);
    }

    // --- addCommand ---

    @Test
    void addCommand_registersCommandForListing() {
        HBCommand hbCmd = mock(HBCommand.class);
        when(hbCmd.getPermission()).thenReturn("headblocks.use");

        command.addCommand(hbCmd);

        try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class)) {
            pu.when(() -> PlayerUtils.hasPermission(eq(consoleSender), anyString())).thenReturn(true);

            // The command should be listed when perform is called
            when(hbCmd.getCommand()).thenReturn("help");
            when(languageService.containsMessage("Help.Help")).thenReturn(true);

            boolean result = command.perform(consoleSender, new String[]{"help"});

            assertThat(result).isTrue();
        }
    }

    // --- tabComplete ---

    @Test
    void tabComplete_returnsEmptyList() {
        ArrayList<String> result = command.tabComplete(playerSender, new String[]{"help"});

        assertThat(result).isEmpty();
    }

    @Test
    void tabComplete_withExtraArgs_returnsEmptyList() {
        ArrayList<String> result = command.tabComplete(playerSender, new String[]{"help", "2"});

        assertThat(result).isEmpty();
    }

    // --- perform with no commands ---

    @Nested
    class NoCommandsRegistered {

        @Test
        void consoleSender_noCommands_performsSuccessfully() {
            try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class)) {
                pu.when(() -> PlayerUtils.hasPermission(eq(consoleSender), anyString())).thenReturn(true);

                boolean result = command.perform(consoleSender, new String[]{"help"});

                assertThat(result).isTrue();
                verify(languageService).message("Chat.LineTitle");
                verify(consoleSender).sendMessage("mock-message");
            }
        }

        @Test
        void playerSender_noCommands_performsSuccessfully() {
            try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class)) {
                pu.when(() -> PlayerUtils.hasPermission(eq(playerSender), anyString())).thenReturn(true);

                Player.Spigot spigot = mock(Player.Spigot.class);
                when(playerSender.spigot()).thenReturn(spigot);

                boolean result = command.perform(playerSender, new String[]{"help"});

                assertThat(result).isTrue();
                verify(languageService).message("Chat.LineTitle");
            }
        }
    }

    // --- perform with commands registered ---

    @Nested
    class WithCommandsRegistered {

        private HBCommand cmd1;
        private HBCommand cmd2;

        @BeforeEach
        void setUpCommands() {
            cmd1 = mock(HBCommand.class);
            lenient().when(cmd1.getPermission()).thenReturn("headblocks.admin");
            lenient().when(cmd1.getCommand()).thenReturn("give");

            cmd2 = mock(HBCommand.class);
            lenient().when(cmd2.getPermission()).thenReturn("headblocks.use");
            lenient().when(cmd2.getCommand()).thenReturn("help");

            command.addCommand(cmd1);
            command.addCommand(cmd2);
        }

        @Test
        void consoleSender_withPermission_displaysAllCommands() {
            when(languageService.containsMessage("Help.Give")).thenReturn(true);
            when(languageService.containsMessage("Help.Help")).thenReturn(true);

            try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class)) {
                pu.when(() -> PlayerUtils.hasPermission(eq(consoleSender), anyString())).thenReturn(true);

                boolean result = command.perform(consoleSender, new String[]{"help"});

                assertThat(result).isTrue();
                // Title + 2 command help messages
                verify(consoleSender, times(3)).sendMessage("mock-message");
            }
        }

        @Test
        void consoleSender_filteredByPermission_displaysOnlyPermitted() {
            when(languageService.containsMessage("Help.Help")).thenReturn(true);

            try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class)) {
                // Only has permission for cmd2 (help), not cmd1 (give)
                pu.when(() -> PlayerUtils.hasPermission(consoleSender, "headblocks.admin")).thenReturn(false);
                pu.when(() -> PlayerUtils.hasPermission(consoleSender, "headblocks.use")).thenReturn(true);

                boolean result = command.perform(consoleSender, new String[]{"help"});

                assertThat(result).isTrue();
                // Title + 1 command help message
                verify(consoleSender, times(2)).sendMessage("mock-message");
            }
        }

        @Test
        void consoleSender_missingHelpMessage_sendsNoHelpMessageFound() {
            when(languageService.containsMessage("Help.Give")).thenReturn(false);
            when(languageService.containsMessage("Help.Help")).thenReturn(true);

            try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class);
                 MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class)) {
                pu.when(() -> PlayerUtils.hasPermission(eq(consoleSender), anyString())).thenReturn(true);
                mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));

                boolean result = command.perform(consoleSender, new String[]{"help"});

                assertThat(result).isTrue();
                // "give" has no help message, so should get the "no help message found" text
                verify(consoleSender).sendMessage(contains("No help message found"));
            }
        }

        @Test
        void playerSender_withPermission_displaysAllCommands() {
            when(languageService.containsMessage("Help.Give")).thenReturn(true);
            when(languageService.containsMessage("Help.Help")).thenReturn(true);

            try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class)) {
                pu.when(() -> PlayerUtils.hasPermission(eq(playerSender), anyString())).thenReturn(true);

                Player.Spigot spigot = mock(Player.Spigot.class);
                when(playerSender.spigot()).thenReturn(spigot);

                boolean result = command.perform(playerSender, new String[]{"help"});

                assertThat(result).isTrue();
                verify(languageService).message("Chat.LineTitle");
            }
        }

        @Test
        void playerSender_missingHelpMessage_sendsNoHelpMessageFoundViaSendMessage() {
            when(languageService.containsMessage("Help.Give")).thenReturn(false);
            when(languageService.containsMessage("Help.Help")).thenReturn(true);

            try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class);
                 MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class)) {
                pu.when(() -> PlayerUtils.hasPermission(eq(playerSender), anyString())).thenReturn(true);
                mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));

                Player.Spigot spigot = mock(Player.Spigot.class);
                when(playerSender.spigot()).thenReturn(spigot);

                boolean result = command.perform(playerSender, new String[]{"help"});

                assertThat(result).isTrue();
                // Even for Player sender, missing help message is sent via sendMessage, not spigot
                verify(playerSender).sendMessage(contains("No help message found"));
            }
        }

        @Test
        void commandWithAllSuffix_capitalizesCorrectly() {
            HBCommand removeAllCmd = mock(HBCommand.class);
            when(removeAllCmd.getPermission()).thenReturn("headblocks.admin");
            when(removeAllCmd.getCommand()).thenReturn("removeall");
            command.addCommand(removeAllCmd);

            when(languageService.containsMessage("Help.Give")).thenReturn(true);
            when(languageService.containsMessage("Help.Help")).thenReturn(true);
            // "removeall" -> capitalize -> "Removeall" -> replaceAll("all","All") -> "RemoveAll"
            when(languageService.containsMessage("Help.RemoveAll")).thenReturn(true);

            try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class)) {
                pu.when(() -> PlayerUtils.hasPermission(eq(consoleSender), anyString())).thenReturn(true);

                boolean result = command.perform(consoleSender, new String[]{"help"});

                assertThat(result).isTrue();
                verify(languageService).containsMessage("Help.RemoveAll");
                verify(languageService).message("Help.RemoveAll");
            }
        }
    }

    // --- Pagination ---

    @Nested
    class Pagination {

        @Test
        void secondArg_asPageNumber_accepted() {
            try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class)) {
                pu.when(() -> PlayerUtils.hasPermission(eq(consoleSender), anyString())).thenReturn(true);

                boolean result = command.perform(consoleSender, new String[]{"help", "2"});

                assertThat(result).isTrue();
            }
        }

        @Test
        void playerSender_withPageNumber_accepted() {
            try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class)) {
                pu.when(() -> PlayerUtils.hasPermission(eq(playerSender), anyString())).thenReturn(true);

                Player.Spigot spigot = mock(Player.Spigot.class);
                when(playerSender.spigot()).thenReturn(spigot);

                boolean result = command.perform(playerSender, new String[]{"help", "1"});

                assertThat(result).isTrue();
            }
        }
    }

    // --- Permission filtering ---

    @Nested
    class PermissionFiltering {

        @Test
        void noPermission_showsNoCommands() {
            HBCommand cmd1 = mock(HBCommand.class);
            when(cmd1.getPermission()).thenReturn("headblocks.admin");
            command.addCommand(cmd1);

            try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class)) {
                pu.when(() -> PlayerUtils.hasPermission(eq(consoleSender), anyString())).thenReturn(false);

                boolean result = command.perform(consoleSender, new String[]{"help"});

                assertThat(result).isTrue();
                // Title is sent, but no command help messages
                verify(consoleSender).sendMessage("mock-message");
                verify(languageService, never()).containsMessage(anyString());
            }
        }
    }
}
