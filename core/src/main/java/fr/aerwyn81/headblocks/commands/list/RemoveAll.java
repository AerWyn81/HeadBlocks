package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.handlers.ConfigHandler;
import fr.aerwyn81.headblocks.handlers.HeadHandler;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import fr.aerwyn81.headblocks.handlers.StorageHandler;
import fr.aerwyn81.headblocks.utils.InternalException;
import fr.aerwyn81.headblocks.utils.MessageUtils;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@HBAnnotations(command = "removeall", permission = "headblocks.admin")
public class RemoveAll implements Cmd {
    private final ConfigHandler configHandler;
    private final LanguageHandler languageHandler;
    private final HeadHandler headHandler;
    private final StorageHandler storageHandler;

    public RemoveAll(HeadBlocks main) {
        this.configHandler = main.getConfigHandler();
        this.languageHandler = main.getLanguageHandler();
        this.headHandler = main.getHeadHandler();
        this.storageHandler = main.getStorageHandler();
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        ArrayList<Map.Entry<UUID, Location>> headLocations = new ArrayList<>(headHandler.getHeadLocations().entrySet());
        int headCount = headLocations.size();

        if (headLocations.size() == 0) {
            sender.sendMessage(languageHandler.getMessage("Messages.ListHeadEmpty"));
            return true;
        }

        int headRemoved = 0;
        boolean hasConfirmInCommand = args.length > 1 && args[1].equals("--confirm");
        if (hasConfirmInCommand) {

            for (Map.Entry<UUID, Location> head : new ArrayList<>(headLocations)) {
                try {
                    headHandler.removeHeadLocation(head.getKey(), configHandler.shouldResetPlayerData());
                    headRemoved++;
                } catch (InternalException ex) {
                    sender.sendMessage(languageHandler.getMessage("Messages.StorageError"));
                    HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while removing the head (" + head.getKey().toString() + " at " + head.getValue().toString() + ") from the storage: " + ex.getMessage()));
                }
            }

            if (headRemoved == 0) {
                sender.sendMessage(languageHandler.getMessage("Messages.RemoveAllError")
                        .replaceAll("%headCount%", String.valueOf(headCount)));
                return true;
            }

            sender.sendMessage(languageHandler.getMessage("Messages.RemoveAllSuccess")
                    .replaceAll("%headCount%", String.valueOf(headCount)));
            return true;
        }

        sender.sendMessage(languageHandler.getMessage("Messages.RemoveAllConfirm")
                .replaceAll("%headCount%", String.valueOf(headCount)));

        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 ? new ArrayList<>(Collections.singleton("--confirm")) : new ArrayList<>();
    }
}
