package fr.aerwyn81.headblocks.commands;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.commands.list.*;
import fr.aerwyn81.headblocks.commands.list.List;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class HBCommandExecutor implements CommandExecutor, TabCompleter {
    private final HashMap<String, HBCommand> registeredCommands;
    private final ServiceRegistry registry;

    private final Help helpCommand;

    public HBCommandExecutor(ServiceRegistry registry) {
        this.registry = registry;
        this.registeredCommands = new HashMap<>();

        this.helpCommand = new Help(registry);

        this.register(helpCommand);
        this.register(new Give(registry));
        this.register(new Reload(registry));
        this.register(new List(registry));
        this.register(new Progress(registry));
        this.register(new Remove(registry));
        this.register(new RemoveAll(registry));
        this.register(new Reset(registry));
        this.register(new ResetAll(registry));
        this.register(new Version(registry));
        this.register(new Stats(registry));
        this.register(new Top(registry));
        this.register(new Tp(registry));
        this.register(new Move(registry));
        this.register(new Options(registry));
        this.register(new Export(registry));
        this.register(new Info(registry));
        this.register(new RenameHead(registry));
        this.register(new Hunt(registry));
        this.register(new Leave(registry));
        this.register(new Debug(registry));
    }

    private void register(Cmd c) {
        HBCommand command = new HBCommand(c);

        registeredCommands.put(command.getCommand(), command);

        if (command.isVisible()) {
            helpCommand.addCommand(command);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command c, @NotNull String s, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(registry.getLanguageService().message("Messages.ErrorCommand"));
            return false;
        }

        var input = args[0].toLowerCase();
        HBCommand command = registeredCommands.get(input);

        if (command == null) {
            var aliasCmd = registeredCommands.entrySet().stream().filter(cEntry -> Objects.equals(cEntry.getValue().getAlias(), input)).findAny();
            if (aliasCmd.isPresent()) {
                command = aliasCmd.get().getValue();
            } else {
                sender.sendMessage(registry.getLanguageService().message("Messages.ErrorCommand"));
                return false;
            }
        }

        if (!PlayerUtils.hasPermission(sender, command.getPermission())) {
            sender.sendMessage(registry.getLanguageService().message("Messages.NoPermission"));
            return false;
        }

        if (command.isPlayerCommand() && !(sender instanceof Player)) {
            sender.sendMessage(registry.getLanguageService().message("Messages.PlayerOnly"));
            return false;
        }

        int argsWithoutCmd = Arrays.copyOfRange(args, 1, args.length).length;

        if (argsWithoutCmd < command.getArgs().length) {
            sender.sendMessage(registry.getLanguageService().message("Messages.ErrorCommand"));
            return false;
        }

        return command.getCmdClass().perform(sender, args);
    }

    @Override
    public ArrayList<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command c, @NotNull String s, String[] args) {
        var input = args[0].toLowerCase();

        if (args.length == 1) {
            return registeredCommands.entrySet().stream()
                    .filter(cEntry -> cEntry.getKey().startsWith(input))
                    .filter(cEntry -> cEntry.getValue().isVisible())
                    .filter(cEntry -> PlayerUtils.hasPermission(sender, cEntry.getValue().getPermission()))
                    .map(Map.Entry::getKey)
                    .distinct()
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        HBCommand command = registeredCommands.get(args[0].toLowerCase());
        if (command == null) {
            var optCmd = registeredCommands.values().stream().filter(cmd -> Objects.equals(cmd.getAlias(), input)).findAny();
            if (optCmd.isEmpty()) {
                return new ArrayList<>();
            } else {
                command = optCmd.get();
            }
        }

        return command.getCmdClass().tabComplete(sender, args);
    }
}
