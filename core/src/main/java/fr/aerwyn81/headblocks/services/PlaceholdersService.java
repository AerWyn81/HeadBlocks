package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlaceholdersService {

    public static String parse(String pName, UUID pUuid, String message) {
        message = message.replaceAll("%player%", pName)
                .replaceAll("%prefix%", LanguageService.getPrefix());

        if (message.contains("%progress%") || message.contains("%current%") || message.contains("%max%") || message.contains("%left%")|| message.contains("%head-name%"))   {
            message = parseInternal(pName, pUuid, message, null );
        } else {
            message = MessageUtils.colorize(message);
        }

        if (HeadBlocks.isPlaceholderApiActive)
            return PlaceholderAPI.setPlaceholders(Bukkit.getOfflinePlayer(pUuid), message);

        return MessageUtils.centerMessage(message);
    }
    public static String parse(String pName, UUID pUuid, String message, UUID hUuid) {
        message = message.replaceAll("%player%", pName)
                .replaceAll("%prefix%", LanguageService.getPrefix());

        if (message.contains("%progress%") || message.contains("%current%") || message.contains("%max%") || message.contains("%left%")|| message.contains("%head-name%"))   {
            message = parseInternal(pName, pUuid, message, hUuid);
        } else {
            message = MessageUtils.colorize(message);
        }

        if (HeadBlocks.isPlaceholderApiActive)
            return PlaceholderAPI.setPlaceholders(Bukkit.getOfflinePlayer(pUuid), message);

        return MessageUtils.centerMessage(message);
    }
    public static String[] parse(Player player, List<String> messages) {
        List<String> msgs = new ArrayList<>();

        messages.forEach(message -> msgs.add(parse(player.getName(), player.getUniqueId(), message)));

        return msgs.toArray(new String[0]);
    }

    public static String[] parse(Player player, List<String> messages, UUID hUuid) {
        List<String> msgs = new ArrayList<>();

        messages.forEach(message -> msgs.add(parse(player.getName(), player.getUniqueId(), message, hUuid)));

        return msgs.toArray(new String[0]);
    }

    public static String parseInternal(String pName, UUID pUuid, String message, UUID hUuid) {
        String progress;
        int current;

        try {
            current = StorageService.getHeadsPlayer(pUuid, pName).size();
        } catch (InternalException ex) {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("Error while retrieving heads of " + pName + ": " + ex.getMessage()));
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
        if (message.contains("%head-name%")) {
            String headName;
            if(hUuid == null)
            {
                headName = "(Name not set)";
            }
            else
            {
                HeadLocation headLocation = HeadService.getHeadByUUID(hUuid);
                headName = headLocation != null ? headLocation.getName() : hUuid.toString();
                if(headName == "")
                {
                    headName= hUuid.toString();
                }
            }
            message = message.replaceAll("%head-name%", headName);
        }
        return MessageUtils.colorize(message);
    }
}
