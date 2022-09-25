package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.TieredReward;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class RewardService {

    public static void giveReward(Player p) {
        if (ConfigService.getTieredRewards().size() == 0) {
            return;
        }

        var plugin = HeadBlocks.getInstance();

        TieredReward tieredReward = ConfigService.getTieredRewards().stream().filter(t -> {
            try {
                return t.getLevel() == StorageService.getHeadsPlayer(p.getUniqueId(), p.getName()).size();
            } catch (InternalException ex) {
                p.sendMessage(LanguageService.getMessage("Messages.StorageError"));
                HeadBlocks.log.sendMessage(MessageUtils.colorize("Error while retrieving heads of " + p.getName() + ": " + ex.getMessage()));
                return false;
            }
        }).findFirst().orElse(null);

        if (tieredReward == null) {
            return;
        }

        List<String> messages = tieredReward.getMessages();
        if (messages.size() != 0) {
            p.sendMessage(PlaceholdersService.parse(p, messages));
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            List<String> commands = tieredReward.getCommands();
            commands.forEach(command ->
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), PlaceholdersService.parse(p.getName(), p.getUniqueId(), command)));

            List<String> broadcastMessages = tieredReward.getBroadcastMessages();
            if (broadcastMessages.size() != 0) {
                for (String message : broadcastMessages) {
                    plugin.getServer().broadcastMessage(PlaceholdersService.parse(p.getName(), p.getUniqueId(), message));
                }
            }
        }, 1L);
    }

    public static boolean currentIsContainedInTiered(int playerHeads) {
        return ConfigService.getTieredRewards().stream().anyMatch(t -> t.getLevel() == playerHeads);
    }
}
