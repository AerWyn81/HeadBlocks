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
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

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
        ArrayList<Pair<UUID, Location>> headLocations = headHandler.getHeadLocations();
        int headCount = headLocations.size();

        if (headLocations.size() == 0) {
            sender.sendMessage(languageHandler.getMessage("Messages.ListHeadEmpty"));
            return true;
        }

        AtomicInteger headRemoved = new AtomicInteger();
        boolean hasConfirmInCommand = args.length > 1 && args[1].equals("--confirm");
        if (hasConfirmInCommand) {
            headLocations.forEach(head -> {
                if (configHandler.shouldResetPlayerData()) {
                    try {
                        storageHandler.removeHead(head.getValue0());
                    } catch (InternalException ex) {
                        sender.sendMessage(languageHandler.getMessage("Messages.StorageError"));
                        HeadBlocks.log.sendMessage(MessageUtils.translate("&cError while removing the head (" + head.getValue0() + " at " + head.getValue1().toString() + ") from the storage: " + ex.getMessage()));
                        return;
                    }
                }

                headHandler.removeHead(head.getValue0());
                headRemoved.getAndIncrement();
            });

            if (headRemoved.get() == 0) {
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
