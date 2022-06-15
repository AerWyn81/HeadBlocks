package fr.aerwyn81.headblocks.handlers;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.api.HeadBlocksAPI;
import fr.aerwyn81.headblocks.utils.InternalException;
import fr.aerwyn81.headblocks.utils.MessageUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PlaceholdersHandler {
    private static final HeadBlocksAPI headBlocksAPI;
    private static final ConfigHandler configHandler;
    private static final LanguageHandler languageHandler;
    private static final HeadHandler headHandler;
    private static final StorageHandler storageHandler;

    static {
        HeadBlocks instance = HeadBlocks.getInstance();
        configHandler = instance.getConfigHandler();
        languageHandler = instance.getLanguageHandler();
        headBlocksAPI = instance.getHeadBlocksAPI();
        headHandler = instance.getHeadHandler();
        storageHandler = instance.getStorageHandler();
    }

    public static String parse(Player player, String message) {
        String progress;

        if (message.contains("%progress%") || message.contains("%current%") || message.contains("%max%") || message.contains("%left%")) {
            int current;
            try {
                current = storageHandler.getHeadsPlayer(player.getUniqueId()).size();
            } catch (InternalException ex) {
                HeadBlocks.log.sendMessage(MessageUtils.translate("Error while trying to communicate with the storage : " + ex.getMessage()));
                current = 0;
            }

            int total = headHandler.getHeadLocations().size();

            message = message.replaceAll("%current%", String.valueOf(current))
                    .replaceAll("%max%", String.valueOf(total));

            if (message.contains("%progress%")) {
                progress = MessageUtils.createProgressBar(current, total,
                        configHandler.getProgressBarBars(),
                        configHandler.getProgressBarSymbol(),
                        configHandler.getProgressBarCompletedColor(),
                        configHandler.getProgressBarNotCompletedColor());

                message = message.replaceAll("%progress%", progress);
            }

            if (message.contains("%left%")) {
                message = message.replaceAll("%left%", String.valueOf(total - current));
            }
        }

        message = message.replaceAll("%player%", player.getName())
                .replaceAll("%prefix%", languageHandler.getPrefix());

        if (!HeadBlocks.isPlaceholderApiActive)
            return MessageUtils.centerMessage(message);

        return PlaceholderAPI.setPlaceholders(player, message);
    }

    public static String[] parse(Player player, List<String> messages) {
        List<String> msgs = new ArrayList<>();

        messages.forEach(message -> msgs.add(parse(player, message)));

        return msgs.toArray(new String[0]);
    }
}
