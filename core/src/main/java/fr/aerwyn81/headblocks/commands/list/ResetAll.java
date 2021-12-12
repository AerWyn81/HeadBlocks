package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import fr.aerwyn81.headblocks.handlers.StorageHandler;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@HBAnnotations(command = "resetall", permission = "headblocks.admin")
public class ResetAll implements Cmd {
    private final LanguageHandler languageHandler;
    private final StorageHandler storageHandler;

    public ResetAll(HeadBlocks main) {
        this.languageHandler = main.getLanguageHandler();
        this.storageHandler = main.getStorageHandler();
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        List<UUID> allPlayers = storageHandler.getAllPlayers();

        if (allPlayers.size() == 0) {
            sender.sendMessage(languageHandler.getMessage("Messages.ResetAllNoData"));
            return true;
        }

        boolean hasConfirmInCommand = args.length > 1 && args[1].equals("--confirm");
        if (hasConfirmInCommand) {
            allPlayers.forEach(storageHandler::resetPlayer);
            sender.sendMessage(languageHandler.getMessage("Messages.ResetAllSuccess")
                    .replaceAll("%playerCount%", String.valueOf(allPlayers.size())));
            return true;
        }

        sender.sendMessage(languageHandler.getMessage("Messages.ResetAllConfirm")
                .replaceAll("%playerCount%", String.valueOf(allPlayers.size())));

        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 ? new ArrayList<>(Collections.singleton("--confirm")) : new ArrayList<>();
    }
}
