package fr.aerwyn81.headblocks.platform;

import fr.aerwyn81.headblocks.utils.scheduler.BukkitSchedulerAdapter;
import fr.aerwyn81.headblocks.utils.scheduler.FoliaSchedulerAdapter;
import fr.aerwyn81.headblocks.utils.scheduler.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class PaperPlatform implements Platform {

    private static final boolean FOLIA = classExists("io.papermc.paper.threadedregions.RegionizedServer");

    private Command registeredCommand;

    @Override
    public String name() {
        return FOLIA ? "Folia" : "Paper";
    }

    @Override
    public SchedulerAdapter createScheduler(Plugin plugin) {
        return FOLIA ? new FoliaSchedulerAdapter(plugin) : new BukkitSchedulerAdapter(plugin);
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Entity entity, Location location) {
        return entity.teleportAsync(location);
    }

    /**
     * paper-plugin.yml has no commands section, so nothing is registered for us: the command is
     * built here and pushed into the server's CommandMap, which is Paper-only API.
     */
    @Override
    public <T extends CommandExecutor & TabCompleter> boolean registerCommand(
            Plugin plugin, String name, List<String> aliases, String description, T handler) {
        var command = new HandlerBackedCommand(name, description, "/" + name, aliases, handler);

        // A false return means another plugin already holds the bare label and only the namespaced
        // form was claimed. The command is registered either way, so this is not a failure.
        Bukkit.getCommandMap().register(plugin.getName().toLowerCase(Locale.ROOT), command);
        registeredCommand = command;

        return true;
    }

    @Override
    public void unregisterCommands() {
        if (registeredCommand == null) {
            return;
        }

        registeredCommand.unregister(Bukkit.getCommandMap());
        registeredCommand = null;
    }

    private static final class HandlerBackedCommand extends Command {
        private final CommandExecutor executor;
        private final TabCompleter completer;

        private <T extends CommandExecutor & TabCompleter> HandlerBackedCommand(
                String name, String description, String usage, List<String> aliases, T handler) {
            super(name, description, usage, aliases);
            this.executor = handler;
            this.completer = handler;
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
            return executor.onCommand(sender, this, label, args);
        }

        @Override
        public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
            var completions = completer.onTabComplete(sender, this, alias, args);

            return completions == null ? Collections.emptyList() : completions;
        }
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
