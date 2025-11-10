package fr.aerwyn81.headblocks.utils.internal;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

public class CommandsUtils {

    public static PlayerProfileLight extractAndGetPlayerUuidByName(CommandSender sender, String[] args, boolean canSeeOther) {
        var pName = "";

        if (args.length >= 2 && !NumberUtils.isDigits(args[1])) {
            if (!canSeeOther) {
                sender.sendMessage(LanguageService.getMessage("Messages.NoPermission"));
                return null;
            }

            pName = args[1];
        } else {
            if (sender instanceof ConsoleCommandSender) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&cThis command cannot be performed by console without player in argument"));
                return null;
            }

            pName = sender.getName();
        }

        PlayerProfileLight profile;

        try {
            profile = StorageService.getPlayerByName(pName);
        } catch (Exception ex) {
            sender.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while retrieving player " + pName + " from the storage: " + ex.getMessage()));
            return null;
        }

        if (profile == null) {
            sender.sendMessage(LanguageService.getMessage("Messages.PlayerNotFound", args[1]));
            return null;
        }

        return profile;
    }
}
