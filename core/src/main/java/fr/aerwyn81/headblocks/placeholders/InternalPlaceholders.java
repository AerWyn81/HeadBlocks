package fr.aerwyn81.headblocks.placeholders;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.api.HeadBlocksAPI;
import fr.aerwyn81.headblocks.handlers.LanguageHandler;
import org.bukkit.entity.Player;

public class InternalPlaceholders {
    private static final HeadBlocksAPI headBlocksAPI;
    private static final LanguageHandler languageHandler;

    static {
        HeadBlocks instance = HeadBlocks.getInstance();
        headBlocksAPI = instance.getHeadBlocksAPI();
        languageHandler = instance.getLanguageHandler();
    }

    public static String parse(Player player, String message) {
        int current = headBlocksAPI.getPlayerHeads(player.getUniqueId()).size();
        int max = headBlocksAPI.getTotalHeadSpawnCount();
        int left = headBlocksAPI.getLeftPlayerHeadToMax(player.getUniqueId());
        String playerName = player.getName();
        String prefix = languageHandler.getPrefix();

        return message.replaceAll("%current%", String.valueOf(current)
                .replaceAll("%max%", String.valueOf(max))
                .replaceAll("%left%", String.valueOf(left))
                .replaceAll("%player%", playerName)
                .replaceAll("%prefix%", prefix));
    }
}
