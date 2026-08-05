package fr.aerwyn81.headblocks.platform;

import fr.aerwyn81.headblocks.utils.scheduler.SchedulerAdapter;
import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface Platform {

    String name();

    SchedulerAdapter createScheduler(Plugin plugin);

    CompletableFuture<Boolean> teleportAsync(Entity entity, Location location);

    /**
     * Binds the plugin command. Spigot declares it in plugin.yml and only needs the handler wired;
     * paper-plugin.yml has no commands section at all, so the Paper build registers it by hand and
     * needs the name, aliases and description too.
     *
     * @return false when the command could not be bound, leaving the plugin without commands.
     */
    <T extends CommandExecutor & TabCompleter> boolean registerCommand(
            Plugin plugin, String name, List<String> aliases, String description, T handler);

    /**
     * Releases anything {@link #registerCommand} claimed outside the server's own plugin lifecycle.
     * A no-op where the server already owns the registration.
     */
    void unregisterCommands();
}
