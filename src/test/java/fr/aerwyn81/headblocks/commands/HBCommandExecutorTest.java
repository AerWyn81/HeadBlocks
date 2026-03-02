package fr.aerwyn81.headblocks.commands;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.services.*;
import fr.aerwyn81.headblocks.utils.bukkit.CommandDispatcher;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.bukkit.SchedulerAdapter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HBCommandExecutorTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private LanguageService languageService;

    @Mock
    private CommandSender consoleSender;

    @Mock
    private Player playerSender;

    @Mock
    private Command command;

    private HBCommandExecutor executor;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(languageService.message(anyString())).thenReturn("mock-message");

        lenient().when(registry.getHeadService()).thenReturn(mock(HeadService.class));
        lenient().when(registry.getStorageService()).thenReturn(mock(StorageService.class));
        lenient().when(registry.getConfigService()).thenReturn(mock(ConfigService.class));
        lenient().when(registry.getHuntService()).thenReturn(mock(HuntService.class));
        lenient().when(registry.getPlaceholdersService()).thenReturn(mock(PlaceholdersService.class));
        lenient().when(registry.getRewardService()).thenReturn(mock(RewardService.class));
        lenient().when(registry.getGuiService()).thenReturn(mock(GuiService.class));
        lenient().when(registry.getHologramService()).thenReturn(mock(HologramService.class));
        lenient().when(registry.getHuntConfigService()).thenReturn(mock(HuntConfigService.class));
        lenient().when(registry.getScheduler()).thenReturn(mock(SchedulerAdapter.class));
        lenient().when(registry.getCommandDispatcher()).thenReturn(mock(CommandDispatcher.class));

        executor = new HBCommandExecutor(registry);
    }

    // --- No args: error message ---

    @Test
    void noArgs_sendsErrorMessage() {
        boolean result = executor.onCommand(consoleSender, command, "hb", new String[]{});

        verify(consoleSender).sendMessage("mock-message");
        assertThat(result).isFalse();
    }

    // --- Unknown command: error message ---

    @Test
    void unknownCommand_sendsErrorMessage() {
        try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class)) {
            boolean result = executor.onCommand(consoleSender, command, "hb", new String[]{"nonexistent"});

            verify(consoleSender).sendMessage("mock-message");
            assertThat(result).isFalse();
        }
    }

    // --- Permission denied ---

    @Test
    void permissionDenied_sendsMessage() {
        try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class)) {
            pu.when(() -> PlayerUtils.hasPermission(any(), anyString())).thenReturn(false);

            boolean result = executor.onCommand(consoleSender, command, "hb", new String[]{"hunt"});

            verify(consoleSender).sendMessage("mock-message");
            verify(languageService).message("Messages.NoPermission");
            assertThat(result).isFalse();
        }
    }

    // --- Player-only command from console ---

    @Test
    void playerOnlyCommand_fromConsole_refused() {
        try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class)) {
            pu.when(() -> PlayerUtils.hasPermission(any(), anyString())).thenReturn(true);

            // "give" is player-only
            boolean result = executor.onCommand(consoleSender, command, "hb", new String[]{"give"});

            verify(consoleSender).sendMessage("mock-message");
            verify(languageService).message("Messages.PlayerOnly");
            assertThat(result).isFalse();
        }
    }

    // --- Tab completion: first arg returns command names ---

    @Test
    void tabComplete_firstArg_returnsCommands() {
        try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class)) {
            pu.when(() -> PlayerUtils.hasPermission(any(), anyString())).thenReturn(true);

            ArrayList<String> result = executor.onTabComplete(consoleSender, command, "hb", new String[]{"h"});

            assertThat(result).contains("help", "hunt");
        }
    }

    // --- Tab completion: unknown command returns empty ---

    @Test
    void tabComplete_unknownCommand_returnsEmpty() {
        ArrayList<String> result = executor.onTabComplete(consoleSender, command, "hb", new String[]{"nonexistent", ""});

        assertThat(result).isEmpty();
    }

    // --- Tab completion: filters by permission ---

    @Test
    void tabComplete_filtersByPermission() {
        try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class)) {
            pu.when(() -> PlayerUtils.hasPermission(any(), anyString())).thenReturn(false);

            ArrayList<String> result = executor.onTabComplete(consoleSender, command, "hb", new String[]{""});

            assertThat(result).isEmpty();
        }
    }

    // --- Tab completion: delegates to command ---

    @Test
    void tabComplete_delegatesToCommand() {
        try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class)) {
            pu.when(() -> PlayerUtils.hasPermission(any(), anyString())).thenReturn(true);

            ArrayList<String> result = executor.onTabComplete(consoleSender, command, "hb", new String[]{"hunt", ""});

            assertThat(result).contains("create", "delete", "enable", "disable", "list");
        }
    }
}
