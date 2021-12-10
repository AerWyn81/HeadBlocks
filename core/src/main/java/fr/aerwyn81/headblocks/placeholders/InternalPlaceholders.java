package fr.aerwyn81.headblocks.placeholders;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.api.HeadBlocksAPI;
import fr.aerwyn81.headblocks.handlers.ConfigHandler;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import fr.aerwyn81.headblocks.utils.FormatUtils;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class InternalPlaceholders {
    private static final HeadBlocksAPI headBlocksAPI;
    private static final ConfigHandler configHandler;
    private static final LanguageHandler languageHandler;

    static {
        HeadBlocks instance = HeadBlocks.getInstance();
        configHandler = instance.getConfigHandler();
        languageHandler = instance.getLanguageHandler();
        headBlocksAPI = instance.getHeadBlocksAPI();
    }

    public static String parse(Player player, String message) {
        int current = headBlocksAPI.getPlayerHeads(player.getUniqueId()).size();
        int total = headBlocksAPI.getTotalHeadSpawnCount();
        int left = headBlocksAPI.getLeftPlayerHeadToMax(player.getUniqueId());
        String progress = FormatUtils.createProgressBar(current, total,
                configHandler.getProgressBarBars(),
                configHandler.getProgressBarSymbol(),
                configHandler.getProgressBarCompletedColor(),
                configHandler.getProgressBarNotCompletedColor());

        return FormatUtils.TryToFormatPlaceholders(player, message
                .replaceAll("%current%", String.valueOf(current))
                .replaceAll("%max%", String.valueOf(total))
                .replaceAll("%left%", String.valueOf(left))
                .replaceAll("%player%", player.getName())
                .replaceAll("%progress%", progress)
                .replaceAll("%prefix%", languageHandler.getPrefix()));
    }

    public static String[] parse(Player player, List<String> messages) {
        List<String> msgs = new ArrayList<>();

        messages.forEach(message -> msgs.add(parse(player, message)));

        return msgs.toArray(new String[0]);
    }
}
