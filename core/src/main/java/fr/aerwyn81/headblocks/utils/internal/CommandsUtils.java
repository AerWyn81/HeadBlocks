package fr.aerwyn81.headblocks.utils.internal;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.PlayerUuidName;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.UUID;

public class CommandsUtils {

    public static PlayerUuidName extractAndGetPlayerUuidByName(CommandSender sender, String[] args) {
        var pName = "";

        if (args.length >= 2 && !NumberUtils.isDigits(args[1])) {
            pName = args[1];
        } else {
            if (sender instanceof ConsoleCommandSender) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&cThis command cannot be performed by console without player in argument"));
                return null;
            }

            pName = sender.getName();
        }

        UUID playerUuid;

        try {
            playerUuid = StorageService.getPlayer(pName);
        } catch (Exception ex) {
            sender.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while retrieving player " + pName + " from the storage: " + ex.getMessage()));
            return null;
        }

        if (playerUuid == null) {
            sender.sendMessage(LanguageService.getMessage("Messages.PlayerNotFound", args[1]));
            return null;
        }

        return new PlayerUuidName(playerUuid, pName);
    }
}
