package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.utils.bukkit.PluginProvider;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlaceholdersService {
    private final StorageService storageService;
    private final ConfigService configService;
    private final LanguageService languageService;
    private final PluginProvider pluginProvider;

    // --- Constructor ---

    public PlaceholdersService(StorageService storageService, ConfigService configService,
                               LanguageService languageService, PluginProvider pluginProvider) {
        this.storageService = storageService;
        this.configService = configService;
        this.languageService = languageService;
        this.pluginProvider = pluginProvider;
    }

    // --- Instance methods ---

    public String parse(String pName, UUID pUuid, String message) {
        return parse(pName, pUuid, null, message);
    }

    public String parse(String pName, UUID pUuid, HeadLocation headLocation, String message) {
        message = message.replace("%player%", pName)
                .replace("%prefix%", languageService.prefix());

        if (message.contains("%progress%") || message.contains("%current%") || message.contains("%max%") || message.contains("%left%") || message.contains("%headName%")) {
            message = parseInternal(pUuid, message, headLocation);
        } else {
            message = MessageUtils.colorize(message);
        }

        if (pluginProvider.isPlaceholderApiActive()) {
            return PlaceholderAPI.setPlaceholders(Bukkit.getOfflinePlayer(pUuid), MessageUtils.centerMessage(message));
        }

        return MessageUtils.centerMessage(message);
    }

    public String[] parse(Player player, HeadLocation headLocation, List<String> messages) {
        List<String> msgs = new ArrayList<>();

        messages.forEach(message -> msgs.add(parse(player.getName(), player.getUniqueId(), headLocation, message)));

        return msgs.toArray(new String[0]);
    }

    public String parseInternal(UUID pUuid, String message, HeadLocation headLocation) {
        String progress;

        var future = storageService.getHeadsPlayer(pUuid).asFuture();

        try {
            var current = future.get().size();

            int total = storageService.getHeads().size();

            message = message.replace("%current%", String.valueOf(current))
                    .replace("%max%", String.valueOf(total));

            if (message.contains("%progress%")) {
                progress = MessageUtils.createProgressBar(current, total,
                        configService.progressBarBars(),
                        configService.progressBarSymbol(),
                        configService.progressBarCompletedColor(),
                        configService.progressBarNotCompletedColor());

                message = message.replace("%progress%", progress);
            }

            if (message.contains("%left%")) {
                message = message.replace("%left%", String.valueOf(total - current));
            }
        } catch (Exception ignored) {
            LogUtil.error("Error retrieving heads from storage, cannot parse all HeadBlocks placeholders");
        }

        if (message.contains("%headName%")) {
            String headName;
            if (headLocation == null) {
                headName = languageService.message("Other.NameNotSet");
            } else {
                headName = headLocation.getName().isEmpty() ? headLocation.getUuid().toString() : headLocation.getName();
            }

            message = message.replace("%headName%", headName);
        }

        return MessageUtils.colorize(message);
    }
}
