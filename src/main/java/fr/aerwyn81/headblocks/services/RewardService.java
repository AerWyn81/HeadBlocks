package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.TieredReward;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class RewardService {
    public static void giveReward(Player p, List<UUID> playerHeads, HeadLocation headLocation) {
        var plugin = HeadBlocks.getInstance();

        TieredReward tieredReward;
        if (!ConfigService.getTieredRewards().isEmpty()) {
            tieredReward = ConfigService.getTieredRewards().stream()
                    .filter(t -> t.getLevel() == playerHeads.size())
                    .findFirst()
                    .orElse(null);

            if (tieredReward != null) {
                List<String> messages = tieredReward.getMessages();
                if (!messages.isEmpty()) {
                    p.sendMessage(PlaceholdersService.parse(p, headLocation, messages));
                }

                HeadBlocks.getScheduler().runAtEntityLater(p, () -> {
                    List<String> tieredCommands = tieredReward.getCommands();
                    if (!tieredCommands.isEmpty()) {
                        if (tieredReward.isRandom()) {
                            String randomCommand = tieredCommands.get(new Random().nextInt(tieredCommands.size()));
                            HeadBlocks.getScheduler().runLater(() -> {
                                String parsedCommand = PlaceholdersService.parse(p.getName(), p.getUniqueId(), headLocation, randomCommand);
                                if (!parsedCommand.isBlank()) {
                                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), parsedCommand);
                                }
                            }, 1L);
                        } else {
                            HeadBlocks.getScheduler().runLater(() -> {
                                tieredCommands.forEach(command -> {
                                    String parsedCommand = PlaceholdersService.parse(p.getName(), p.getUniqueId(), headLocation, command);
                                    if (!parsedCommand.isBlank()) {
                                        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), parsedCommand);
                                    }
                                });
                            }, 1L);
                        }
                    }

                    List<String> broadcastMessages = tieredReward.getBroadcastMessages();
                    if (!broadcastMessages.isEmpty()) {
                        for (String message : broadcastMessages) {
                            plugin.getServer().broadcastMessage(PlaceholdersService.parse(p.getName(), p.getUniqueId(), headLocation, message));
                        }
                    }
                }, 1L);
            }
        } else {
            tieredReward = null;
        }

        if (!ConfigService.isPreventMessagesOnTieredRewardsLevel() || tieredReward == null) {
            // Success messages if not empty
            List<String> messages = ConfigService.getHeadClickMessages();
            if (!messages.isEmpty()) {
                p.sendMessage(PlaceholdersService.parse(p, headLocation, messages));
            }
        }

        // Only the tieredReward was given
        if (ConfigService.isPreventCommandsOnTieredRewardsLevel() && tieredReward != null) {
            return;
        }

        var isRandomCommand = ConfigService.isHeadClickCommandsRandomized();
        var headClickCommands = ConfigService.getHeadClickCommands();

        if (headClickCommands.isEmpty()) {
            return;
        }

        if (isRandomCommand) {
            String randomCommand = headClickCommands.get(new Random().nextInt(headClickCommands.size()));
            HeadBlocks.getScheduler().runLater(() -> {
                String parsedCommand = PlaceholdersService.parse(p.getName(), p.getUniqueId(), headLocation, randomCommand);
                if (!parsedCommand.isBlank()) {
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), parsedCommand);
                }
            }, 1L);
        } else {
            HeadBlocks.getScheduler().runLater(() -> headClickCommands.forEach(reward -> {
                String parsedCommand = PlaceholdersService.parse(p.getName(), p.getUniqueId(), headLocation, reward);
                if (!parsedCommand.isBlank()) {
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), parsedCommand);
                }
            }), 1L);
        }
    }

    public static boolean hasPlayerSlotsRequired(Player player, List<UUID> playerHeads) {
        var slotsRequired = ConfigService.getHeadClickCommandsSlotsRequired();

        if (slotsRequired != -1 && PlayerUtils.getEmptySlots(player) < slotsRequired) {
            return false;
        }

        if (ConfigService.getTieredRewards().isEmpty()) {
            return true;
        }

        var tieredReward = ConfigService.getTieredRewards().stream()
                .filter(t -> t.getLevel() == playerHeads.size())
                .findFirst()
                .orElse(null);

        return tieredReward == null ||
                tieredReward.getSlotsRequired() == -1 || PlayerUtils.getEmptySlots(player) >= tieredReward.getSlotsRequired();
    }
}
