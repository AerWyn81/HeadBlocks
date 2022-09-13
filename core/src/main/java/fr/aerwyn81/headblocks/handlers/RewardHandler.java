package fr.aerwyn81.headblocks.handlers;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.TieredReward;
import fr.aerwyn81.headblocks.utils.InternalException;
import fr.aerwyn81.headblocks.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class RewardHandler {

    private final HeadBlocks main;

    public RewardHandler(HeadBlocks main) {
        this.main = main;
    }

    public void giveReward(Player p) {
        if (ConfigService.getTieredRewards().size() == 0) {
            return;
        }

        TieredReward tieredReward = ConfigService.getTieredRewards().stream().filter(t -> {
            try {
                return t.getLevel() ==
                        main.getStorageHandler().getHeadsPlayer(p.getUniqueId()).size();
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
            p.sendMessage(PlaceholdersHandler.parse(p, messages));
        }

        Bukkit.getScheduler().runTaskLater(main, () -> {
            List<String> commands = tieredReward.getCommands();
            commands.forEach(command ->
                    main.getServer().dispatchCommand(main.getServer().getConsoleSender(), PlaceholdersHandler.parse(p, command)));

            List<String> broadcastMessages = tieredReward.getBroadcastMessages();
            if (broadcastMessages.size() != 0) {
                for (String message : broadcastMessages) {
                    main.getServer().broadcastMessage(PlaceholdersHandler.parse(p, message));
                }
            }
        }, 1L);
    }

    public boolean currentIsContainedInTiered(int playerHeads) {
        return ConfigService.getTieredRewards().stream().anyMatch(t -> t.getLevel() == playerHeads);
    }
}
