package fr.aerwyn81.headblocks.platform;

import fr.aerwyn81.headblocks.commands.HBCommandExecutor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpigotPlatformTest {

    @Mock
    private Entity entity;

    @Mock
    private Location location;

    @Mock
    private Plugin plugin;

    @Mock
    private HBCommandExecutor handler;

    @Test
    @DisplayName("the Spigot build resolves SpigotPlatform")
    void service_declaration_resolves_spigot_platform() {
        Platform platform = Platforms.load();

        assertThat(platform).isInstanceOf(SpigotPlatform.class);
        assertThat(platform.name()).isEqualTo("Spigot");
    }

    @Test
    @DisplayName("teleportAsync completes synchronously with the teleport outcome")
    void teleport_async_wraps_the_synchronous_teleport() {
        when(entity.teleport(location)).thenReturn(true);

        CompletableFuture<Boolean> result = new SpigotPlatform().teleportAsync(entity, location);

        assertThat(result).isCompletedWithValue(true);
        verify(entity).teleport(location);
    }

    @Test
    @DisplayName("the plugin.yml command is wired to the handler for both execution and completion")
    void register_command_binds_the_plugin_yml_command() {
        PluginCommand command = mock(PluginCommand.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPluginCommand("headblocks")).thenReturn(command);

            boolean registered = new SpigotPlatform()
                    .registerCommand(plugin, "headblocks", List.of("hb"), "Plugin command", handler);

            assertThat(registered).isTrue();
            verify(command).setExecutor(handler);
            verify(command).setTabCompleter(handler);
        }
    }

    @Test
    @DisplayName("a command missing from plugin.yml is reported rather than throwing")
    void register_command_reports_a_missing_declaration() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPluginCommand("headblocks")).thenReturn(null);

            boolean registered = new SpigotPlatform()
                    .registerCommand(plugin, "headblocks", List.of("hb"), "Plugin command", handler);

            assertThat(registered).isFalse();
        }
    }

    @Test
    @DisplayName("unregisterCommands is a no-op: the server owns the plugin.yml registration")
    void unregister_commands_does_nothing() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            new SpigotPlatform().unregisterCommands();

            bukkit.verifyNoInteractions();
        }
    }
}
