package fr.aerwyn81.headblocks.handlers;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.utils.InternalException;
import fr.aerwyn81.headblocks.utils.MessageUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PlaceholdersHandler {
    private static final LanguageHandler languageHandler;
    private static final StorageHandler storageHandler;

    static {
        HeadBlocks instance = HeadBlocks.getInstance();
        languageHandler = instance.getLanguageHandler();
        storageHandler = instance.getStorageHandler();
    }

    public static String parse(Player player, String message) {
        message = message.replaceAll("%player%", player.getName())
                .replaceAll("%prefix%", languageHandler.getPrefix());

        if (message.contains("%progress%") || message.contains("%current%") || message.contains("%max%") || message.contains("%left%")) {
            message = parseInternal(player, message);
        } else {
            message = MessageUtils.colorize(message);
        }

        if (HeadBlocks.isPlaceholderApiActive)
            return PlaceholderAPI.setPlaceholders(player, message);

        return MessageUtils.centerMessage(message);
    }

    public static String parseInternal(Player player, String message) {
        String progress;
        int current;

        try {
            current = storageHandler.getHeadsPlayer(player.getUniqueId()).size();
        } catch (InternalException ex) {
            player.sendMessage(languageHandler.getMessage("Messages.StorageError"));
            HeadBlocks.log.sendMessage(MessageUtils.colorize("Error while retrieving heads of " + player.getName() + ": " + ex.getMessage()));
            return MessageUtils.colorize(message);
        }

        int total = HeadService.getChargedHeadLocations().size();

        message = message.replaceAll("%current%", String.valueOf(current))
                .replaceAll("%max%", String.valueOf(total));

        if (message.contains("%progress%")) {
            progress = MessageUtils.createProgressBar(current, total,
                    ConfigService.getProgressBarBars(),
                    ConfigService.getProgressBarSymbol(),
                    ConfigService.getProgressBarCompletedColor(),
                    ConfigService.getProgressBarNotCompletedColor());

            message = message.replaceAll("%progress%", progress);
        }

        if (message.contains("%left%")) {
            message = message.replaceAll("%left%", String.valueOf(total - current));
        }

        return MessageUtils.colorize(message);
    }

    public static String[] parse(Player player, List<String> messages) {
        List<String> msgs = new ArrayList<>();

        messages.forEach(message -> msgs.add(parse(player, message)));

        return msgs.toArray(new String[0]);
    }
}
