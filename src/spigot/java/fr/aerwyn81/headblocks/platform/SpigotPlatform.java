package fr.aerwyn81.headblocks.platform;

import fr.aerwyn81.headblocks.utils.scheduler.BukkitSchedulerAdapter;
import fr.aerwyn81.headblocks.utils.scheduler.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SpigotPlatform implements Platform {

    @Override
    public String name() {
        return "Spigot";
    }

    @Override
    public SchedulerAdapter createScheduler(Plugin plugin) {
        return new BukkitSchedulerAdapter(plugin);
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Entity entity, Location location) {
        return CompletableFuture.completedFuture(entity.teleport(location));
    }

    // Declared in plugin.yml, so aliases and description are already registered by the server.
    @Override
    public <T extends CommandExecutor & TabCompleter> boolean registerCommand(
            Plugin plugin, String name, List<String> aliases, String description, T handler) {
        var command = Bukkit.getPluginCommand(name);
        if (command == null) {
            return false;
        }

        command.setExecutor(handler);
        command.setTabCompleter(handler);

        return true;
    }

    @Override
    public void unregisterCommands() {
        // The server owns the plugin.yml registration and drops it on disable.
    }
}
