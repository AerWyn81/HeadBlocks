package fr.aerwyn81.headblocks.commands;

import fr.aerwyn81.headblocks.commands.list.*;
import fr.aerwyn81.headblocks.commands.list.List;
import fr.aerwyn81.headblocks.services.LanguageService;
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

    private final Help helpCommand;

    public HBCommandExecutor() {
        this.registeredCommands = new HashMap<>();

        this.helpCommand = new Help();

        this.register(helpCommand);
        this.register(new Give());
        this.register(new Reload());
        this.register(new List());
        this.register(new Progress());
        this.register(new Remove());
        this.register(new RemoveAll());
        this.register(new Reset());
        this.register(new ResetAll());
        this.register(new Version());
        this.register(new Stats());
        this.register(new Top());
        this.register(new Tp());
        this.register(new Move());
        this.register(new Options());
        this.register(new Export());
        this.register(new Info());
        this.register(new RenameHead());
        this.register(new Hunt());
        this.register(new Debug());
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
            sender.sendMessage(LanguageService.getMessage("Messages.ErrorCommand"));
            return false;
        }

        var input = args[0].toLowerCase();
        HBCommand command = registeredCommands.get(input);

        if (command == null) {
            var aliasCmd = registeredCommands.entrySet().stream().filter(cEntry -> Objects.equals(cEntry.getValue().getAlias(), input)).findAny();
            if (aliasCmd.isPresent()) {
                command = aliasCmd.get().getValue();
            } else {
                sender.sendMessage(LanguageService.getMessage("Messages.ErrorCommand"));
                return false;
            }
        }

        if (!PlayerUtils.hasPermission(sender, command.getPermission())) {
            sender.sendMessage(LanguageService.getMessage("Messages.NoPermission"));
            return false;
        }

        if (command.isPlayerCommand() && !(sender instanceof Player)) {
            sender.sendMessage(LanguageService.getMessage("Messages.PlayerOnly"));
            return false;
        }

        int argsWithoutCmd = Arrays.copyOfRange(args, 1, args.length).length;

        if (argsWithoutCmd < command.getArgs().length) {
            sender.sendMessage(LanguageService.getMessage("Messages.ErrorCommand"));
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