package fr.aerwyn81.headblocks.handlers;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.TieredReward;
import fr.aerwyn81.headblocks.placeholders.InternalPlaceholders;
import fr.aerwyn81.headblocks.utils.FormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class RewardHandler {

    private final HeadBlocks main;

    public RewardHandler(HeadBlocks main) {
        this.main = main;
    }

    public void giveReward(Player p) {
        if (main.getConfigHandler().getTieredRewards().size() == 0) {
            return;
        }

        TieredReward tieredReward = main.getConfigHandler().getTieredRewards().stream().filter(t -> t.getLevel() ==
                main.getStorageHandler().getHeadsPlayer(p.getUniqueId()).size()).findFirst().orElse(null);

        if (tieredReward == null) {
            return;
        }

        List<String> messages = tieredReward.getMessages();
        if (messages.size() != 0) {
            p.sendMessage(InternalPlaceholders.parse(p, messages));
        }

        Bukkit.getScheduler().runTaskLater(main, () -> {
            List<String> commands = tieredReward.getCommands();
            commands.forEach(command ->
                    main.getServer().dispatchCommand(main.getServer().getConsoleSender(), InternalPlaceholders.parse(p, command)));

            List<String> broadcastMessages = tieredReward.getBroadcastMessages();
            if (broadcastMessages.size() != 0) {
                for (String message : broadcastMessages) {
                    main.getServer().broadcastMessage(FormatUtils.formatMessage(message));
                }
            }
        }, 1L);
    }

    public boolean currentIsContainedInTiered(int playerHeads) {
        return main.getConfigHandler().getTieredRewards().stream().anyMatch(t -> t.getLevel() == playerHeads);
    }
}
