package fr.aerwyn81.headblocks.commands;

import fr.aerwyn81.headblocks.commands.list.*;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
        this.register(new Me());
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
    }

    private void register(Cmd c) {
        HBCommand command = new HBCommand(c);

        registeredCommands.put(command.getCommand(), command);

        if (command.isVisible()) {
            helpCommand.addCommand(command);
        }
    }

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command c, @Nonnull String s, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(LanguageService.getMessage("Messages.ErrorCommand"));
            return false;
        }

        HBCommand command = registeredCommands.get(args[0].toLowerCase());

        if (command == null) {
            sender.sendMessage(LanguageService.getMessage("Messages.ErrorCommand"));
            return false;
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

        return registeredCommands.get(args[0].toLowerCase()).getCmdClass().perform(sender, args);
    }

    @Override
    public ArrayList<String> onTabComplete(@Nonnull CommandSender sender, @Nonnull Command c, @Nonnull String s, String[] args) {
        if(args.length == 1) {
            return registeredCommands.keySet().stream()
                    .filter(arg -> arg.startsWith(args[0].toLowerCase()))
                    .filter(arg -> registeredCommands.get(arg).isVisible())
                    .filter(arg -> PlayerUtils.hasPermission(sender, registeredCommands.get(arg).getPermission())).distinct()
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        if (!registeredCommands.containsKey(args[0].toLowerCase())) {
            return new ArrayList<>();
        }

        HBCommand command = registeredCommands.get(args[0].toLowerCase());

        if (!PlayerUtils.hasPermission(sender, command.getPermission())) {
            return new ArrayList<>();
        }

        return command.getCmdClass().tabComplete(sender, args);
    }
}