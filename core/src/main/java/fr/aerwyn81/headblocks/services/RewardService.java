package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.TieredReward;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class RewardService {
    public static boolean giveReward(Player p, List<UUID> playerHeads) {
        var plugin = HeadBlocks.getInstance();

        TieredReward tieredReward;
        if (!ConfigService.getTieredRewards().isEmpty()) {
            tieredReward = ConfigService.getTieredRewards().stream()
                    .filter(t -> t.getLevel() == playerHeads.size())
                    .findFirst()
                    .orElse(null);

            if (tieredReward != null) {
                if (tieredReward.getSlotsRequired() != -1 && PlayerUtils.getEmptySlots(p) < tieredReward.getSlotsRequired()) {
                    var message = LanguageService.getMessage("Messages.InventoryFullReward");
                    if (!message.trim().isEmpty()) {
                        p.sendMessage(message);
                    }

                    return false;
                }

                List<String> messages = tieredReward.getMessages();
                if (!messages.isEmpty()) {
                    p.sendMessage(PlaceholdersService.parse(p, messages));
                }

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    List<String> commands = tieredReward.getCommands();
                    commands.forEach(command ->
                            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), PlaceholdersService.parse(p.getName(), p.getUniqueId(), command)));

                    List<String> broadcastMessages = tieredReward.getBroadcastMessages();
                    if (!broadcastMessages.isEmpty()) {
                        for (String message : broadcastMessages) {
                            plugin.getServer().broadcastMessage(PlaceholdersService.parse(p.getName(), p.getUniqueId(), message));
                        }
                    }
                }, 1L);
            }
        } else {
            tieredReward = null;
        }

        if (!ConfigService.isPreventHeadClickMessageOnTieredRewardsLevel() || tieredReward == null) {
            // Success messages if not empty
            List<String> messages = ConfigService.getHeadClickMessages();
            if (!messages.isEmpty()) {
                p.sendMessage(PlaceholdersService.parse(p, messages));
            }
        }

        // Only the tieredReward was given
        if (ConfigService.isPreventCommandsOnTieredRewardsLevel() && tieredReward != null) {
            return true;
        }

        var isRandomCommand = ConfigService.isHeadClickCommandsRandomized();
        var slotsRequired = ConfigService.getHeadClickCommandsSlotsRequired();

        if (slotsRequired != -1 && PlayerUtils.getEmptySlots(p) < slotsRequired) {
            var message = LanguageService.getMessage("Messages.InventoryFullReward");
            if (!message.trim().isEmpty()) {
                p.sendMessage(message);
            }

            return false;
        }

        if (isRandomCommand) {
            String randomCommand = ConfigService.getHeadClickCommands().get(new Random().nextInt(ConfigService.getHeadClickCommands().size()));
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), PlaceholdersService.parse(p.getName(), p.getUniqueId(), randomCommand)), 1L);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> ConfigService.getHeadClickCommands().forEach(reward ->
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), PlaceholdersService.parse(p.getName(), p.getUniqueId(), reward))), 1L);
        }

        return true;
    }
}
