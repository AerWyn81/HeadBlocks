package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.handlers.LanguageService;
import fr.aerwyn81.headblocks.handlers.StorageHandler;
import fr.aerwyn81.headblocks.utils.InternalException;
import fr.aerwyn81.headblocks.utils.MessageUtils;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@HBAnnotations(command = "resetall", permission = "headblocks.admin")
public class ResetAll implements Cmd {
    private final StorageHandler storageHandler;

    public ResetAll(HeadBlocks main) {
        this.storageHandler = main.getStorageHandler();
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        List<UUID> allPlayers;

        try {
            allPlayers = storageHandler.getAllPlayers();
        } catch (InternalException ex) {
            sender.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while retrieving all players from the storage: " + ex.getMessage()));
            return true;
        }

        if (allPlayers.size() == 0) {
            sender.sendMessage(LanguageService.getMessage("Messages.ResetAllNoData"));
            return true;
        }

        boolean hasConfirmInCommand = args.length > 1 && args[1].equals("--confirm");
        if (hasConfirmInCommand) {

            for (UUID uuid : allPlayers) {
                try {
                    storageHandler.resetPlayer(uuid);
                } catch (InternalException ex) {
                    sender.sendMessage(LanguageService.getMessage("Messages.StorageError"));
                    HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while resetting the player UUID " + uuid.toString() + " from the storage: " + ex.getMessage()));
                    return true;
                }
            }

            sender.sendMessage(LanguageService.getMessage("Messages.ResetAllSuccess")
                    .replaceAll("%playerCount%", String.valueOf(allPlayers.size())));
            return true;
        }

        sender.sendMessage(LanguageService.getMessage("Messages.ResetAllConfirm")
                .replaceAll("%playerCount%", String.valueOf(allPlayers.size())));

        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 ? new ArrayList<>(Collections.singleton("--confirm")) : new ArrayList<>();
    }
}
