package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.utils.bukkit.PluginProvider;
import fr.aerwyn81.headblocks.utils.config.ConfigUpdater;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored", "ConstantConditions"})
public class LanguageService {
    private final PluginProvider pluginProvider;
    private String lang;
    private final HashMap<String, Object> messageMap;

    // --- Constructor ---

    public LanguageService(PluginProvider pluginProvider, ConfigService configService) {
        this.pluginProvider = pluginProvider;
        this.messageMap = new HashMap<>();

        var langDir = new File(pluginProvider.getDataFolder() + "/language");
        if (!langDir.exists() && !langDir.mkdirs()) {
            LogUtil.error("Failed to create language directory: {0}", langDir.getAbsolutePath());
        }

        loadLanguage("en");
        loadLanguage("fr");
        this.lang = checkLanguage(configService.language());
    }

    // --- Instance methods ---

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String language() {
        return lang;
    }

    public String prefix() {
        Object raw = messageMap.get("Prefix");
        if (raw == null) {
            return "[HeadBlocks]";
        }
        return MessageUtils.colorize(raw.toString());
    }

    public boolean containsMessage(String message) {
        return messageMap.containsKey(message);
    }

    public String message(String message) {
        Object raw = messageMap.get(message);
        if (raw == null) {
            LogUtil.warning("Missing translation key: {0}", message);
            return prefix() + " " + message;
        }
        return MessageUtils.colorize(raw.toString()
                .replace("%prefix%", prefix()));
    }

    public String message(String message, String playerName) {
        return message(message)
                .replace("%player%", playerName);
    }

    @SuppressWarnings("unchecked")
    public List<String> messageList(String message) {
        Object raw = messageMap.get(message);
        if (raw == null) {
            LogUtil.warning("Missing translation list key: {0}", message);
            return Collections.emptyList();
        }
        if (!(raw instanceof List)) {
            return Collections.singletonList(MessageUtils.colorize(raw.toString()));
        }
        return ((List<String>) raw).stream().map(MessageUtils::colorize).collect(Collectors.toList());
    }

    public String checkLanguage(String lang) {
        File f = new File(pluginProvider.getDataFolder() + "/language/messages_" + lang + ".yml");
        if (f.exists()) {
            return lang;
        }
        return "en";
    }

    public void pushMessages() {
        File f = new File(pluginProvider.getDataFolder() + "/language/messages_" + lang + ".yml");
        YamlConfiguration c = YamlConfiguration.loadConfiguration(f);

        c.getKeys(true).stream().filter(key -> !(c.get(key) instanceof MemorySection)).forEach(key -> {
            if (c.get(key) instanceof List) {
                messageMap.put(key, c.getStringList(key));
            } else {
                messageMap.put(key, c.getString(key));
            }
        });
    }

    public void loadLanguage(String lang) {
        File file = new File(pluginProvider.getDataFolder() + "/language/messages_" + lang + ".yml");
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    LogUtil.error("Cannot create translation file: {0}", file.getAbsolutePath());
                }
            } catch (IOException e) {
                LogUtil.error("Error loading translation file: {0}", e.getMessage());
            }
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        cfg.options().setHeader(List.of(
                "",
                "This is the messsages file.",
                "You can change any messages that are in this file",
                "",
                "If you want to reset a message back to the default,",
                "delete the entire line the message is on and restart the server.",
                "\t"
        ));

        Map<String, Object> msgDefaults = new LinkedHashMap<>();

        FileConfiguration data;
        try (InputStreamReader input = new InputStreamReader(pluginProvider.getResource("language/messages_" + lang + ".yml"), StandardCharsets.UTF_8)) {
            data = YamlConfiguration.loadConfiguration(input);
        } catch (IOException e) {
            LogUtil.error("Error reading default language resource: {0}", e.getMessage());
            return;
        }

        for (String key : data.getKeys(true)) {
            if (!(data.get(key) instanceof MemorySection)) {
                if (data.get(key) instanceof List) {
                    msgDefaults.put(key, data.getStringList(key));
                } else {
                    msgDefaults.put(key, data.getString(key));
                }
            }
        }

        for (String key : msgDefaults.keySet()) {
            if (!cfg.isSet(key)) {
                cfg.set(key, msgDefaults.get(key));
            }
        }

        for (String key : cfg.getKeys(true)) {
            if (!(cfg.get(key) instanceof MemorySection)) {
                if (!data.isSet(key)) {
                    cfg.set(key, null);
                }
            }
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            LogUtil.error("Error loading translation file: {0}", e.getMessage());
        }

        try {
            ConfigUpdater.update(pluginProvider, "language/messages_" + lang + ".yml", new File(pluginProvider.getDataFolder() + "/language/messages_" + lang + ".yml"), Collections.emptyList());
        } catch (IOException e) {
            LogUtil.error("Error loading translation file: {0}", e.getMessage());
        }
    }
}
