package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.handlers.ConfigService;
import fr.aerwyn81.headblocks.handlers.HeadService;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import fr.aerwyn81.headblocks.utils.InternalException;
import fr.aerwyn81.headblocks.utils.MessageUtils;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;

@HBAnnotations(command = "removeall", permission = "headblocks.admin")
public class RemoveAll implements Cmd {
    private final LanguageHandler languageHandler;

    public RemoveAll(HeadBlocks main) {
        this.languageHandler = main.getLanguageHandler();
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        ArrayList<HeadLocation> headLocations = new ArrayList<>(HeadService.getChargedHeadLocations());
        int headCount = headLocations.size();

        if (headLocations.size() == 0) {
            sender.sendMessage(languageHandler.getMessage("Messages.ListHeadEmpty"));
            return true;
        }

        int headRemoved = 0;
        boolean hasConfirmInCommand = args.length > 1 && args[1].equals("--confirm");
        if (hasConfirmInCommand) {

            for (HeadLocation head : new ArrayList<>(headLocations)) {
                try {
                    HeadService.removeHeadLocation(head, ConfigService.shouldResetPlayerData());
                    headRemoved++;
                } catch (InternalException ex) {
                    sender.sendMessage(languageHandler.getMessage("Messages.StorageError"));
                    HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while removing the head (" + head.getUuid().toString() + " at " + head.getLocation().toString() + ") from the storage: " + ex.getMessage()));
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
