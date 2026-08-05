package fr.aerwyn81.headblocks.platform;

import fr.aerwyn81.headblocks.commands.HBCommandExecutor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PaperCommandRegistrationTest {

    private MockedStatic<Bukkit> bukkit;
    private CommandMap commandMap;
    private Plugin plugin;
    private HBCommandExecutor handler;
    private PaperPlatform platform;

    @BeforeEach
    void setUp() {
        commandMap = mock(CommandMap.class);
        plugin = mock(Plugin.class);
        handler = mock(HBCommandExecutor.class);

        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getCommandMap).thenReturn(commandMap);

        lenient().when(plugin.getName()).thenReturn("HeadBlocks");

        platform = new PaperPlatform();
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    private Command registerAndCapture() {
        when(commandMap.register(anyString(), any(Command.class))).thenReturn(true);

        boolean registered = platform.registerCommand(
                plugin, "headblocks", List.of("hb"), "Plugin command", handler);
        assertThat(registered).isTrue();

        ArgumentCaptor<Command> captor = ArgumentCaptor.forClass(Command.class);
        verify(commandMap).register(eq("headblocks"), captor.capture());

        return captor.getValue();
    }

    @Test
    @DisplayName("the command is registered into the CommandMap under a lowercased plugin prefix")
    void registers_into_the_command_map() {
        Command command = registerAndCapture();

        assertThat(command.getName()).isEqualTo("headblocks");
        assertThat(command.getAliases()).containsExactly("hb");
        assertThat(command.getDescription()).isEqualTo("Plugin command");
    }

    @Test
    @DisplayName("execution is delegated to the handler, preserving its return value")
    void execution_is_delegated_to_the_handler() {
        Command command = registerAndCapture();
        CommandSender sender = mock(CommandSender.class);
        String[] args = {"list"};

        when(handler.onCommand(sender, command, "hb", args)).thenReturn(true);

        assertThat(command.execute(sender, "hb", args)).isTrue();
        verify(handler).onCommand(sender, command, "hb", args);
    }

    @Test
    @DisplayName("tab completion is delegated to the handler")
    void tab_completion_is_delegated_to_the_handler() {
        Command command = registerAndCapture();
        CommandSender sender = mock(CommandSender.class);
        String[] args = {"li"};

        when(handler.onTabComplete(sender, command, "hb", args)).thenReturn(new ArrayList<>(List.of("list")));

        assertThat(command.tabComplete(sender, "hb", args)).containsExactly("list");
    }

    @Test
    @DisplayName("a null completion becomes an empty list, which Bukkit requires")
    void null_completion_becomes_empty() {
        Command command = registerAndCapture();
        CommandSender sender = mock(CommandSender.class);
        String[] args = {"zz"};

        when(handler.onTabComplete(sender, command, "hb", args)).thenReturn(null);

        assertThat(command.tabComplete(sender, "hb", args)).isEmpty();
    }

    @Test
    @DisplayName("a label already taken by another plugin still counts as registered")
    void a_taken_label_is_not_a_failure() {
        when(commandMap.register(anyString(), any(Command.class))).thenReturn(false);

        boolean registered = platform.registerCommand(
                plugin, "headblocks", List.of("hb"), "Plugin command", handler);

        assertThat(registered).isTrue();
    }

    @Test
    @DisplayName("unregisterCommands releases the manual registration, so a hot reload cannot double it")
    void unregister_releases_the_registration() {
        Command command = registerAndCapture();

        platform.unregisterCommands();

        assertThat(command.isRegistered()).isFalse();
    }

    @Test
    @DisplayName("unregisterCommands without a prior registration does nothing")
    void unregister_without_registration_is_a_no_op() {
        platform.unregisterCommands();

        verify(commandMap, never()).getKnownCommands();
    }

    @Test
    @DisplayName("unregisterCommands is idempotent")
    void unregister_twice_is_safe() {
        registerAndCapture();

        platform.unregisterCommands();
        platform.unregisterCommands();
    }
}
