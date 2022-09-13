package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.commands.Cmd;
import fr.aerwyn81.headblocks.commands.HBAnnotations;
import fr.aerwyn81.headblocks.handlers.LanguageService;
import fr.aerwyn81.headblocks.handlers.StorageHandler;
import fr.aerwyn81.headblocks.utils.InternalException;
import fr.aerwyn81.headblocks.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.stream.Collectors;

@HBAnnotations(command = "reset", permission = "headblocks.admin", args = {"player"})
public class Reset implements Cmd {
    private final StorageHandler storageHandler;

    public Reset(HeadBlocks main) {
        this.storageHandler = main.getStorageHandler();
    }

    @Override
    public boolean perform(CommandSender sender, String[] args) {
        Player pTemp = Bukkit.getOfflinePlayer(args[1]).getPlayer();

        if (pTemp == null) {
            sender.sendMessage(LanguageService.getMessage("Messages.PlayerNotFound")
                    .replaceAll("%player%", args[1]));
            return true;
        }

        try {
            if (!storageHandler.containsPlayer(pTemp.getUniqueId())) {
                sender.sendMessage(LanguageService.getMessage("Messages.NoHeadFound"));
                return true;
            }

            storageHandler.resetPlayer(pTemp.getUniqueId());
        } catch (InternalException ex) {
            sender.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while resetting the player " + pTemp.getName() + " from the storage: " + ex.getMessage()));
            return true;
        }

        sender.sendMessage(LanguageService.getMessage("Messages.PlayerReset")
                .replaceAll("%player%", pTemp.getName()));

        return true;
    }

    @Override
    public ArrayList<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 ? Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toCollection(ArrayList::new)) : new ArrayList<>();
    }
}
