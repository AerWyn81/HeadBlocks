package fr.aerwyn81.headblocks.handlers;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.TieredReward;
import fr.aerwyn81.headblocks.utils.FormatUtils;
import org.bukkit.Color;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import redis.clients.jedis.Protocol;

import java.io.File;
import java.util.ArrayList;
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
        return config.getString("headClick.sounds.alreadyOwn");
    }

    public String getHeadClickNotOwnSound() {
        return config.getString("headClick.sounds.notOwn");
    }

    public List<String> getHeadClickMessages() {
        return config.getStringList("headClick.messages");
    }

    public boolean isHeadClickTitleEnabled() {
        return config.getBoolean("headClick.title.enabled", false);
    }

    public String getHeadClickTitleFirstLine() {
        return config.getString("headClick.title.firstLine", "");
    }

    public String getHeadClickTitleSubTitle() {
        return config.getString("headClick.title.subTitle", "");
    }

    public int getHeadClickTitleFadeIn() {
        return config.getInt("headClick.title.fadeIn", 0);
    }

    public int getHeadClickTitleStay() {
        return config.getInt("headClick.title.stay", 50);
    }

    public int getHeadClickTitleFadeOut() {
        return config.getInt("headClick.title.fadeOut", 0);
    }

    public boolean isFireworkEnabled() {
        return config.getBoolean("headClick.firework.enabled", false);
    }

    public List<Color> getHeadClickFireworkColors() {
        List<Color> colors = new ArrayList<>();

        if (!config.contains("headClick.firework.colors")) {
            return colors;
        }

        config.getStringList("headClick.firework.colors").forEach(color -> {
            try {
                String[] s = color.split(",");
                colors.add(Color.fromRGB(Integer.parseInt(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2])));
            } catch (Exception ex) {
                HeadBlocks.log.sendMessage(FormatUtils.translate(
                        "&cCannot parse RGB color of " + color + ". Format is : r,g,b"));
            }
        });

        return colors;
    }

    public List<Color> getHeadClickFireworkFadeColors() {
        List<Color> colors = new ArrayList<>();

        if (!config.contains("headClick.firework.fadeColors")) {
            return colors;
        }

        config.getStringList("headClick.firework.fadeColors").forEach(color -> {
            try {
                String[] s = color.split(",");
                colors.add(Color.fromRGB(Integer.parseInt(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2])));
            } catch (Exception ex) {
                HeadBlocks.log.sendMessage(FormatUtils.translate(
                        "&cCannot parse RGB color of " + color + ". Format is : r,g,b"));
            }
        });

        return colors;
    }

    public boolean isFireworkFlickerEnabled() {
        return config.getBoolean("headClick.firework.flicker", true);
    }

    public int getHeadClickFireworkPower() {
        return config.getInt("headClick.firework.power", 0);
    }

    public boolean isHeadClickParticlesEnabled() {
        return config.getBoolean("headClick.particles.enabled", false);
    }

    public String getHeadClickParticlesAlreadyOwnType() {
        return config.getString("headClick.particles.alreadyOwn.type", "VILLAGER_ANGRY");
    }

    public int getHeadClickParticlesAmount() {
        return config.getInt("headClick.particles.alreadyOwn.amount", 1);
    }

    public ArrayList<String> getHeadClickParticlesColors() { return new ArrayList<>(config.getStringList("headClick.particles.alreadyOwn.colors")); }

    public List<String> getHeadClickCommands() {
        return config.getStringList("headClick.commands");
    }

    public boolean shouldResetPlayerData() {
        return config.getBoolean("shouldResetPlayerData", true);
    }

    public int getProgressBarBars() {
        return config.getInt("progressBar.totalBars", 100);
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

    public boolean isPreventCommandsOnTieredRewardsLevel() {
        return config.getBoolean("preventCommandsOnTieredRewardsLevel", false);
    }

    public boolean isParticlesEnabled() {
        return config.getBoolean("particles.enabled", false);
    }

    public int getParticlesDelay() {
        return config.getInt("particles.notFound.delay", 20);
    }

    public int getParticlesNotFoundPlayerViewDistance() { return config.getInt("particles.playerViewDistance", 16); }

    public String getParticlesNotFoundType() {
        return config.getString("particles.notFound.type", "REDSTONE");
    }

    public ArrayList<String> getParticlesNotFoundColors() {
        return new ArrayList<>(config.getStringList("particles.notFound.colors"));
    }

    public int getParticlesNotFoundAmount() {
        return config.getInt("particles.notFound.amount", 3);
    }

    public List<TieredReward> getTieredRewards() {
        List<TieredReward> tieredRewards = new ArrayList<>();

        if (!config.contains("tieredRewards")) {
            return tieredRewards;
        }

        config.getConfigurationSection("tieredRewards").getKeys(false).forEach(level -> {
            try {
                List<String> messages = config.getStringList("tieredRewards." + level + ".messages");
                List<String> commandes = config.getStringList("tieredRewards." + level + ".commands");

                tieredRewards.add(new TieredReward(Integer.parseInt(level), messages, commandes));
            } catch (Exception ex) {
                HeadBlocks.log.sendMessage(FormatUtils.translate(
                        "&cCannot read tiered rewards of \"" + level + "\". Error message :" + ex.getMessage()));
            }
        });

        return tieredRewards;
    }
}
