package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.TieredReward;
import fr.aerwyn81.headblocks.utils.bukkit.ItemBuilder;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import redis.clients.jedis.Protocol;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ConfigService {
    private static File configFile;
    private static FileConfiguration config;

    public static void initialize(File file) {
        configFile = file;
        load();

        HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks] &eConfigurations loaded!"));
    }

    public static void load() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public static String getLanguage() {
        return config.getString("language", "en").toLowerCase();
    }

    public static boolean isMetricsEnabled() { return config.getBoolean("metrics", true); }

    public static List<String> getHeads() {
        return config.getStringList("heads");
    }

    public static String getHeadClickAlreadyOwnSound() {
        return config.getString("headClick.sounds.alreadyOwn");
    }

    public static String getHeadClickNotOwnSound() {
        return config.getString("headClick.sounds.notOwn");
    }

    public static List<String> getHeadClickMessages() {
        return config.getStringList("headClick.messages");
    }

    public static boolean isHeadClickTitleEnabled() {
        return config.getBoolean("headClick.title.enabled", false);
    }

    public static String getHeadClickTitleFirstLine() {
        return config.getString("headClick.title.firstLine", "");
    }

    public static String getHeadClickTitleSubTitle() {
        return config.getString("headClick.title.subTitle", "");
    }

    public static int getHeadClickTitleFadeIn() {
        return config.getInt("headClick.title.fadeIn", 0);
    }

    public static int getHeadClickTitleStay() {
        return config.getInt("headClick.title.stay", 50);
    }

    public static int getHeadClickTitleFadeOut() {
        return config.getInt("headClick.title.fadeOut", 0);
    }

    public static boolean isFireworkEnabled() {
        return config.getBoolean("headClick.firework.enabled", false);
    }

    public static List<Color> getHeadClickFireworkColors() {
        List<Color> colors = new ArrayList<>();

        if (!config.contains("headClick.firework.colors")) {
            return colors;
        }

        config.getStringList("headClick.firework.colors").forEach(color -> {
            try {
                String[] s = color.split(",");
                colors.add(Color.fromRGB(Integer.parseInt(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2])));
            } catch (Exception ex) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize(
                        "&cCannot parse RGB color of " + color + ". Format is : r,g,b"));
            }
        });

        return colors;
    }

    public static List<Color> getHeadClickFireworkFadeColors() {
        List<Color> colors = new ArrayList<>();

        if (!config.contains("headClick.firework.fadeColors")) {
            return colors;
        }

        config.getStringList("headClick.firework.fadeColors").forEach(color -> {
            try {
                String[] s = color.split(",");
                colors.add(Color.fromRGB(Integer.parseInt(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2])));
            } catch (Exception ex) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize(
                        "&cCannot parse RGB color of " + color + ". Format is : r,g,b"));
            }
        });

        return colors;
    }

    public static boolean isFireworkFlickerEnabled() {
        return config.getBoolean("headClick.firework.flicker", true);
    }

    public static int getHeadClickFireworkPower() {
        return config.getInt("headClick.firework.power", 0);
    }

    public static boolean isHeadClickParticlesEnabled() {
        return config.getBoolean("headClick.particles.enabled", false);
    }

    public static String getHeadClickParticlesAlreadyOwnType() {
        return config.getString("headClick.particles.alreadyOwn.type", "VILLAGER_ANGRY");
    }

    public static int getHeadClickParticlesAmount() {
        return config.getInt("headClick.particles.alreadyOwn.amount", 1);
    }

    public static ArrayList<String> getHeadClickParticlesColors() { return new ArrayList<>(config.getStringList("headClick.particles.alreadyOwn.colors")); }

    public static List<String> getHeadClickCommands() {
        return config.getStringList("headClick.commands");
    }

    public static boolean shouldResetPlayerData() {
        return config.getBoolean("shouldResetPlayerData", true);
    }

    public static int getProgressBarBars() {
        return config.getInt("progressBar.totalBars", 100);
    }

    public static String getProgressBarSymbol() {
        return config.getString("progressBar.symbol");
    }

    public static String getProgressBarNotCompletedColor() {
        return config.getString("progressBar.notCompletedColor");
    }

    public static String getProgressBarCompletedColor() {
        return config.getString("progressBar.completedColor");
    }

    public static boolean isRedisEnabled() {
        return config.getBoolean("redis.enable", false);
    }

    public static String getRedisHostname() {
        return config.getString("redis.settings.hostname", Protocol.DEFAULT_HOST);
    }

    public static int getRedisDatabase() {
        return config.getInt("redis.settings.database", Protocol.DEFAULT_DATABASE);
    }

    public static String getRedisPassword() {
        return config.getString("redis.settings.password", "");
    }

    public static int getRedisPort() {
        return config.getInt("redis.settings.port", Protocol.DEFAULT_PORT);
    }

    public static boolean isDatabaseEnabled() {
        return config.getBoolean("database.enable", false);
    }

    public static String getDatabaseHostname() {
        return config.getString("database.settings.hostname", "localhost");
    }

    public static String getDatabaseName() {
        return config.getString("database.settings.database");
    }

    public static String getDatabaseUsername() {
        return config.getString("database.settings.username");
    }

    public static String getDatabasePassword() {
        return config.getString("database.settings.password");
    }

    public static int getDatabasePort() {
        return config.getInt("database.settings.port", 3306);
    }

    public static boolean getDatabaseSsl() { return config.getBoolean("database.settings.ssl", false); }

    public static boolean isPreventCommandsOnTieredRewardsLevel() {
        return config.getBoolean("preventCommandsOnTieredRewardsLevel", false);
    }

    public static boolean isParticlesEnabled() {
        return isParticlesFoundEnabled() || isParticlesNotFoundEnabled();
    }

    public static boolean isParticlesFoundEnabled() {
        return config.getBoolean("floatingParticles.found.enabled", true);
    }

    public static boolean isParticlesNotFoundEnabled() {
        return config.getBoolean("floatingParticles.notFound.enabled", false);
    }

    public static String getParticlesNotFoundType() {
        return config.getString("floatingParticles.notFound.type", "REDSTONE");
    }

    public static ArrayList<String> getParticlesNotFoundColors() {
        return new ArrayList<>(config.getStringList("floatingParticles.notFound.colors"));
    }

    public static int getParticlesNotFoundAmount() {
        return config.getInt("floatingParticles.notFound.amount", 3);
    }

    public static String getParticlesFoundType() {
        return config.getString("floatingParticles.found.type", "REDSTONE");
    }

    public static ArrayList<String> getParticlesFoundColors() {
        return new ArrayList<>(config.getStringList("floatingParticles.found.colors"));
    }

    public static int getParticlesFoundAmount() {
        return config.getInt("floatingParticles.found.amount", 3);
    }

    public static List<TieredReward> getTieredRewards() {
        List<TieredReward> tieredRewards = new ArrayList<>();

        if (!config.contains("tieredRewards")) {
            return tieredRewards;
        }

        var tieredSection = config.getConfigurationSection("tieredRewards");
        if (tieredSection == null) {
            return tieredRewards;
        }

        for (String level : tieredSection.getKeys(false)) {
            try {
                List<String> messages = new ArrayList<>();
                if (config.contains("tieredRewards." + level + ".messages")) {
                    messages = config.getStringList("tieredRewards." + level + ".messages");
                }

                List<String> commands = new ArrayList<>();
                if (config.contains("tieredRewards." + level + ".commands")) {
                    commands = config.getStringList("tieredRewards." + level + ".commands");
                }

                List<String> broadcastMessages = new ArrayList<>();
                if (config.contains("tieredRewards." + level + ".broadcast")) {
                    broadcastMessages = config.getStringList("tieredRewards." + level + ".broadcast");
                }

                if (messages.size() != 0 || commands.size() != 0 || broadcastMessages.size() != 0) {
                    tieredRewards.add(new TieredReward(Integer.parseInt(level), messages, commands, broadcastMessages));
                }
            } catch (Exception ex) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize(
                        "&cCannot read tiered rewards of \"" + level + "\". Error message :" + ex.getMessage()));
            }
        }

        return tieredRewards;
    }

    public static int getHologramParticlePlayerViewDistance() {
        return config.getInt("internalTask.hologramParticlePlayerViewDistance", 16);
    }

    public static int getDelayGlobalTask() {
        return config.getInt("internalTask.delay", 20);
    }

    public static double getHologramsHeightAboveHead() {
        return config.getDouble("holograms.heightAboveHead", 0.5);
    }

    public static boolean isHologramsEnabled() {
        return isHologramsFoundEnabled() || isHologramsNotFoundEnabled();
    }

    public static boolean isHologramsFoundEnabled() {
        return config.getBoolean("holograms.found.enabled", true);
    }

    public static boolean isHologramsNotFoundEnabled() {
        return config.getBoolean("holograms.notFound.enabled", true);
    }

    public static ArrayList<String> getHologramsFoundLines() {
        return new ArrayList<>(config.getStringList("holograms.found.lines"));
    }

    public static ArrayList<String> getHologramsNotFoundLines() {
        return new ArrayList<>(config.getStringList("holograms.notFound.lines"));
    }

    public static ItemBuilder getGuiBackIcon() {
        return new ItemBuilder(Material.valueOf(config.getString("gui.backIcon.type", Material.SPRUCE_DOOR.name())));
    }

    public static ItemBuilder getGuiBorderIcon() {
        return new ItemBuilder(Material.valueOf(config.getString("gui.borderIcon.type", Material.GRAY_STAINED_GLASS_PANE.name())));
    }

    public static ItemBuilder getGuiPreviousIcon() {
        return new ItemBuilder(Material.valueOf(config.getString("gui.previousIcon.type", Material.ARROW.name())));
    }

    public static ItemBuilder getGuiNextIcon() {
        return new ItemBuilder(Material.valueOf(config.getString("gui.nextIcon.type", Material.ARROW.name())));
    }

    public static ItemBuilder getGuiCloseIcon() {
        return new ItemBuilder(Material.valueOf(config.getString("gui.closeIcon.type", Material.BARRIER.name())));
    }
}
