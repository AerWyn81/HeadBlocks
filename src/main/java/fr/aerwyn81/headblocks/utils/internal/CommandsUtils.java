package fr.aerwyn81.headblocks.utils.internal;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

public class CommandsUtils {

    public static PlayerProfileLight extractAndGetPlayerUuidByName(ServiceRegistry registry, CommandSender sender, String[] args, boolean canSeeOther) {
        var pName = "";

        if (args.length >= 2 && !NumberUtils.isDigits(args[1])) {
            if (!canSeeOther) {
                sender.sendMessage(registry.getLanguageService().message("Messages.NoPermission"));
                return null;
            }

            pName = args[1];
        } else {
            if (sender instanceof ConsoleCommandSender) {
                LogUtil.error("This command cannot be performed by console without player in argument");
                return null;
            }

            pName = sender.getName();
        }

        PlayerProfileLight profile;

        try {
            profile = registry.getStorageService().getPlayerByName(pName);
        } catch (Exception ex) {
            sender.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
            LogUtil.error("Error while retrieving player {0} from the storage: {1}", pName, ex.getMessage());
            return null;
        }

        if (profile == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.PlayerNotFound", args[1]));
            return null;
        }

        return profile;
    }
}
