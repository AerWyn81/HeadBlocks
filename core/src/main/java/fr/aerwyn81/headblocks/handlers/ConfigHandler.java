package fr.aerwyn81.headblocks.handlers;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.utils.FormatUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import redis.clients.jedis.Protocol;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class ConfigHandler {

    private final File configFile;
    private FileConfiguration config;

    public ConfigHandler(File configFile) {
        this.configFile = configFile;
    }

    public void loadConfiguration() {
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    public String getLanguage() {
        return config.getString("language", "en").toLowerCase();
    }

    public String getHeadTexture() {
        return config.getString("headTexture", "");
    }

    public String getHeadClickAlreadyOwnSound() {
        return config.getString("headClick.sounds.alreadyOwn", "block_anvil_break");
    }

    public String getHeadClickNotOwnSound() {
        return config.getString("headClick.sounds.notOwn", "block_note_block_bell");
    }

    public List<String> getHeadClickCommands() {
        return config.getStringList("headClick.commands");
    }

    public boolean shouldResetPlayerData() {
        return config.getBoolean("shouldResetPlayerData", true);
    }

    public int getProgressBarBars() {
        return config.getInt("progressBar.totalBars");
    }

    public String getProgressBarSymbol() {
        return config.getString("progressBar.symbol");
    }

    public String getProgressBarNotCompletedColor() {
        return config.getString("progressBar.notCompletedColor");
    }

    public String getProgressBarCompletedColor() {
        return config.getString("progressBar.completedColor");
    }

    public boolean isRedisEnabled() {
        return config.getBoolean("redis.enable", false);
    }

    public String getRedisHostname() {
        return config.getString("redis.settings.hostname", Protocol.DEFAULT_HOST);
    }

    public int getRedisDatabase() {
        return config.getInt("redis.settings.database", Protocol.DEFAULT_DATABASE);
    }

    public String getRedisPassword() {
        return config.getString("redis.settings.password", "");
    }

    public int getRedisPort() {
        return config.getInt("redis.settings.port", Protocol.DEFAULT_PORT);
    }

    public boolean isDatabaseEnabled() {
        return config.getBoolean("database.enable", false);
    }

    public String getDatabaseHostname() {
        return config.getString("database.settings.hostname", "localhost");
    }

    public String getDatabaseName() {
        return config.getString("database.settings.database");
    }

    public String getDatabaseUsername() {
        return config.getString("database.settings.username");
    }

    public String getDatabasePassword() {
        return config.getString("database.settings.password");
    }

    public int getDatabasePort() {
        return config.getInt("database.settings.port", 3306);
    }

    public HashMap<Integer, List<String>> getTieredRewards() {
        HashMap<Integer, List<String>> tieredRewards = new HashMap<>();

        if (!config.contains("tieredRewards")) {
            return tieredRewards;
        }

        config.getConfigurationSection("tieredRewards").getKeys(false).forEach(tieredReward -> {
            try {
                tieredRewards.put(Integer.valueOf(tieredReward), config.getStringList("tieredRewards." + tieredReward));
            } catch (Exception ex) {
                HeadBlocks.log.sendMessage(FormatUtils.translate(
                        "&cCannot read tiered rewards of \"" + tieredReward + "\". Error message :" + ex.getMessage()));
            }
        });

        return tieredRewards;
    }
}
