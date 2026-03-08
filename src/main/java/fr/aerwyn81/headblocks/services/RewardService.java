package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.TieredReward;
import fr.aerwyn81.headblocks.data.hunt.HuntConfig;
import fr.aerwyn81.headblocks.utils.bukkit.CommandDispatcher;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.bukkit.SchedulerAdapter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class RewardService {
    private final ConfigService configService;
    private final PlaceholdersService placeholdersService;
    private final SchedulerAdapter scheduler;
    private final CommandDispatcher cmdDispatcher;

    // --- Constructor ---

    public RewardService(ConfigService configService, PlaceholdersService placeholdersService,
                         SchedulerAdapter scheduler, CommandDispatcher cmdDispatcher) {
        this.configService = configService;
        this.placeholdersService = placeholdersService;
        this.scheduler = scheduler;
        this.cmdDispatcher = cmdDispatcher;
    }

    // --- Instance methods ---

    public void giveReward(Player p, List<UUID> playerHeads, HeadLocation headLocation) {
        TieredReward tieredReward;
        if (!configService.tieredRewards().isEmpty()) {
            tieredReward = configService.tieredRewards().stream()
                    .filter(t -> t.level() == playerHeads.size())
                    .findFirst()
                    .orElse(null);

            if (tieredReward != null) {
                List<String> messages = tieredReward.messages();
                if (!messages.isEmpty()) {
                    p.sendMessage(placeholdersService.parse(p, headLocation, messages));
                }

                scheduler.runTaskLater(() -> {
                    List<String> tieredCommands = tieredReward.commands();
                    if (!tieredCommands.isEmpty()) {
                        if (tieredReward.isRandom()) {
                            String randomCommand = tieredCommands.get(new Random().nextInt(tieredCommands.size()));
                            scheduler.runTaskLater(() -> {
                                String parsedCommand = placeholdersService.parse(p.getName(), p.getUniqueId(), headLocation, randomCommand);
                                if (!parsedCommand.isBlank()) {
                                    cmdDispatcher.dispatchConsoleCommand(parsedCommand);
                                }
                            }, 1L);
                        } else {
                            tieredCommands.forEach(command -> {
                                String parsedCommand = placeholdersService.parse(p.getName(), p.getUniqueId(), headLocation, command);
                                if (!parsedCommand.isBlank()) {
                                    cmdDispatcher.dispatchConsoleCommand(parsedCommand);
                                }
                            });
                        }
                    }

                    List<String> broadcastMessages = tieredReward.broadcastMessages();
                    if (!broadcastMessages.isEmpty()) {
                        for (String message : broadcastMessages) {
                            p.getServer().broadcastMessage(placeholdersService.parse(p.getName(), p.getUniqueId(), headLocation, message));
                        }
                    }
                }, 1L);
            }
        } else {
            tieredReward = null;
        }

        if (!configService.preventMessagesOnTieredRewardsLevel() || tieredReward == null) {
            List<String> messages = configService.headClickMessages();
            if (!messages.isEmpty()) {
                p.sendMessage(placeholdersService.parse(p, headLocation, messages));
            }
        }

        if (configService.preventCommandsOnTieredRewardsLevel() && tieredReward != null) {
            return;
        }

        var isRandomCommand = configService.headClickCommandsRandomized();
        var headClickCommands = configService.headClickCommands();

        if (headClickCommands.isEmpty()) {
            return;
        }

        if (isRandomCommand) {
            String randomCommand = headClickCommands.get(new Random().nextInt(headClickCommands.size()));
            scheduler.runTaskLater(() -> {
                String parsedCommand = placeholdersService.parse(p.getName(), p.getUniqueId(), headLocation, randomCommand);
                if (!parsedCommand.isBlank()) {
                    cmdDispatcher.dispatchConsoleCommand(parsedCommand);
                }
            }, 1L);
        } else {
            scheduler.runTaskLater(() -> headClickCommands.forEach(reward -> {
                String parsedCommand = placeholdersService.parse(p.getName(), p.getUniqueId(), headLocation, reward);
                if (!parsedCommand.isBlank()) {
                    cmdDispatcher.dispatchConsoleCommand(parsedCommand);
                }
            }), 1L);
        }
    }

    public boolean hasPlayerSlotsRequired(Player player, List<UUID> playerHeads) {
        var slotsRequired = configService.headClickCommandsSlotsRequired();

        if (slotsRequired != -1 && PlayerUtils.getEmptySlots(player) < slotsRequired) {
            return false;
        }

        if (configService.tieredRewards().isEmpty()) {
            return true;
        }

        var tieredReward = configService.tieredRewards().stream()
                .filter(t -> t.level() == playerHeads.size())
                .findFirst()
                .orElse(null);

        return tieredReward == null ||
                tieredReward.slotsRequired() == -1 || PlayerUtils.getEmptySlots(player) >= tieredReward.slotsRequired();
    }

    // --- Hunt-aware overloads ---

    public void giveReward(Player p, List<UUID> playerHeads, HeadLocation headLocation, HuntConfig huntConfig) {
        giveReward(p, playerHeads, headLocation, huntConfig, null);
    }

    public void giveReward(Player p, List<UUID> playerHeads, HeadLocation headLocation, HuntConfig huntConfig, String huntId) {
        TieredReward tieredReward;
        if (!huntConfig.getTieredRewards().isEmpty()) {
            tieredReward = huntConfig.getTieredRewards().stream()
                    .filter(t -> t.level() == playerHeads.size())
                    .findFirst()
                    .orElse(null);

            if (tieredReward != null) {
                List<String> messages = tieredReward.messages();
                if (!messages.isEmpty()) {
                    p.sendMessage(placeholdersService.parse(p, headLocation, messages, huntId));
                }

                scheduler.runTaskLater(() -> {
                    List<String> tieredCommands = tieredReward.commands();
                    if (!tieredCommands.isEmpty()) {
                        if (tieredReward.isRandom()) {
                            String randomCommand = tieredCommands.get(new Random().nextInt(tieredCommands.size()));
                            scheduler.runTaskLater(() -> {
                                String parsedCommand = placeholdersService.parse(p.getName(), p.getUniqueId(), headLocation, randomCommand, huntId);
                                if (!parsedCommand.isBlank()) {
                                    cmdDispatcher.dispatchConsoleCommand(parsedCommand);
                                }
                            }, 1L);
                        } else {
                            tieredCommands.forEach(command -> {
                                String parsedCommand = placeholdersService.parse(p.getName(), p.getUniqueId(), headLocation, command, huntId);
                                if (!parsedCommand.isBlank()) {
                                    cmdDispatcher.dispatchConsoleCommand(parsedCommand);
                                }
                            });
                        }
                    }

                    List<String> broadcastMessages = tieredReward.broadcastMessages();
                    if (!broadcastMessages.isEmpty()) {
                        for (String message : broadcastMessages) {
                            p.getServer().broadcastMessage(placeholdersService.parse(p.getName(), p.getUniqueId(), headLocation, message, huntId));
                        }
                    }
                }, 1L);
            }
        } else {
            tieredReward = null;
        }

        if (!configService.preventMessagesOnTieredRewardsLevel() || tieredReward == null) {
            List<String> messages = huntConfig.getHeadClickMessages();
            if (!messages.isEmpty()) {
                p.sendMessage(placeholdersService.parse(p, headLocation, messages, huntId));
            }
        }

        if (configService.preventCommandsOnTieredRewardsLevel() && tieredReward != null) {
            return;
        }

        var isRandomCommand = configService.headClickCommandsRandomized();
        var headClickCommands = huntConfig.getHeadClickCommands();

        if (headClickCommands.isEmpty()) {
            return;
        }

        if (isRandomCommand) {
            String randomCommand = headClickCommands.get(new Random().nextInt(headClickCommands.size()));
            scheduler.runTaskLater(() -> {
                String parsedCommand = placeholdersService.parse(p.getName(), p.getUniqueId(), headLocation, randomCommand, huntId);
                if (!parsedCommand.isBlank()) {
                    cmdDispatcher.dispatchConsoleCommand(parsedCommand);
                }
            }, 1L);
        } else {
            scheduler.runTaskLater(() -> headClickCommands.forEach(reward -> {
                String parsedCommand = placeholdersService.parse(p.getName(), p.getUniqueId(), headLocation, reward, huntId);
                if (!parsedCommand.isBlank()) {
                    cmdDispatcher.dispatchConsoleCommand(parsedCommand);
                }
            }), 1L);
        }
    }

    public boolean hasPlayerSlotsRequired(Player player, List<UUID> playerHeads, HuntConfig huntConfig) {
        var slotsRequired = configService.headClickCommandsSlotsRequired();

        if (slotsRequired != -1 && PlayerUtils.getEmptySlots(player) < slotsRequired) {
            return false;
        }

        if (huntConfig.getTieredRewards().isEmpty()) {
            return true;
        }

        var tieredReward = huntConfig.getTieredRewards().stream()
                .filter(t -> t.level() == playerHeads.size())
                .findFirst()
                .orElse(null);

        return tieredReward == null ||
                tieredReward.slotsRequired() == -1 || PlayerUtils.getEmptySlots(player) >= tieredReward.slotsRequired();
    }
}
