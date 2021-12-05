package fr.aerwyn81.headblocks.handlers;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.placeholders.InternalPlaceholders;
import fr.aerwyn81.headblocks.utils.FormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RewardHandler {

    private final HeadBlocks main;

    private final HashMap<Integer, List<String>> rewards;

    private boolean hadRewardsSet;

    public RewardHandler(HeadBlocks main) {
        this.main = main;

        this.rewards = new HashMap<>();
    }

    public void loadRewards() {
        HashMap<Integer, List<String>> trConfig = main.getConfigHandler().getTieredRewards();

        if (trConfig.size() == 0) {
            hadRewardsSet = false;
            return;
        }

        for (Map.Entry<Integer, List<String>> reward : trConfig.entrySet()) {
            try {
                rewards.put(reward.getKey(), reward.getValue().stream().map(FormatUtils::translate).collect(Collectors.toList()));
            } catch (Exception ex) {
                HeadBlocks.log.sendMessage(FormatUtils.translate(
                        "&cCannot parse reward command \"" + reward.getValue() + "\" from config. Error message :" + ex.getMessage()));
            }
        }

        hadRewardsSet = true;
    }

    public void giveReward(Player p) {
        if (!hadRewardsSet) {
            return;
        }

        int headFound = main.getStorageHandler().getHeadsPlayer(p.getUniqueId()).size();
        if (!rewards.containsKey(headFound)) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(main, () -> {
            List<String> rewardsCommands = rewards.get(headFound);

            rewardsCommands.forEach(reward ->
                    main.getServer().dispatchCommand(main.getServer().getConsoleSender(), InternalPlaceholders.parse(p, reward)));
        }, 1L);
    }

    public boolean currentIsContainedInTiered(int playerHeads) {
        return rewards.containsKey(playerHeads);
    }
}
