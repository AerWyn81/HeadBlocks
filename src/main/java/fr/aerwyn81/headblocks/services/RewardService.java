package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.TieredReward;
import fr.aerwyn81.headblocks.data.hunt.HuntConfig;
import fr.aerwyn81.headblocks.utils.bukkit.CommandDispatcher;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.scheduler.SchedulerAdapter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

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
        UnaryOperator<String> parser = text -> placeholdersService.parse(p.getName(), p.getUniqueId(), headLocation, text);
        TieredReward tieredReward = findTieredReward(configService::tieredRewards, playerHeads);
        if (tieredReward != null) {
            List<String> messages = tieredReward.messages();
            if (!messages.isEmpty()) {
                p.sendMessage(placeholdersService.parse(p, headLocation, messages));
            }

            scheduler.runTaskLater(() -> runTieredReward(p, tieredReward, parser), 1L);
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

        runHeadClickCommands(headClickCommands, isRandomCommand, parser);
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
        UnaryOperator<String> parser = text -> placeholdersService.parse(p.getName(), p.getUniqueId(), headLocation, text, huntId);
        TieredReward tieredReward = findTieredReward(huntConfig::getTieredRewards, playerHeads);
        if (tieredReward != null) {
            List<String> messages = tieredReward.messages();
            if (!messages.isEmpty()) {
                p.sendMessage(placeholdersService.parse(p, headLocation, messages, huntId));
            }

            scheduler.runTaskLater(() -> runTieredReward(p, tieredReward, parser), 1L);
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

        runHeadClickCommands(headClickCommands, isRandomCommand, parser);
    }

    private static TieredReward findTieredReward(Supplier<List<TieredReward>> tieredRewards, List<UUID> playerHeads) {
        if (tieredRewards.get().isEmpty()) {
            return null;
        }

        return tieredRewards.get().stream()
                .filter(t -> t.level() == playerHeads.size())
                .findFirst()
                .orElse(null);
    }

    private void runTieredReward(Player p, TieredReward tieredReward, UnaryOperator<String> parser) {
        List<String> tieredCommands = tieredReward.commands();
        if (!tieredCommands.isEmpty()) {
            if (tieredReward.isRandom()) {
                String randomCommand = tieredCommands.get(ThreadLocalRandom.current().nextInt(tieredCommands.size()));
                scheduler.runTaskLater(() -> dispatchParsed(randomCommand, parser), 1L);
            } else {
                tieredCommands.forEach(command -> dispatchParsed(command, parser));
            }
        }

        List<String> broadcastMessages = tieredReward.broadcastMessages();
        if (!broadcastMessages.isEmpty()) {
            for (String message : broadcastMessages) {
                p.getServer().broadcastMessage(parser.apply(message));
            }
        }
    }

    private void runHeadClickCommands(List<String> headClickCommands, boolean isRandomCommand, UnaryOperator<String> parser) {
        if (isRandomCommand) {
            String randomCommand = headClickCommands.get(ThreadLocalRandom.current().nextInt(headClickCommands.size()));
            scheduler.runTaskLater(() -> dispatchParsed(randomCommand, parser), 1L);
        } else {
            scheduler.runTaskLater(() -> headClickCommands.forEach(reward -> dispatchParsed(reward, parser)), 1L);
        }
    }

    private void dispatchParsed(String command, UnaryOperator<String> parser) {
        String parsedCommand = parser.apply(command);
        if (!parsedCommand.isBlank()) {
            cmdDispatcher.dispatchConsoleCommand(parsedCommand);
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
