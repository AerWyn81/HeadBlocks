package fr.aerwyn81.headblocks.services;

import com.cryptomorin.xseries.XSound;
import fr.aerwyn81.headblocks.data.TieredReward;
import fr.aerwyn81.headblocks.databases.EnumTypeDatabase;
import fr.aerwyn81.headblocks.utils.bukkit.ItemBuilder;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import redis.clients.jedis.Protocol;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConfigService {
    private final File configFile;
    private FileConfiguration config;

    // --- Constructor ---

    public ConfigService(File configFile) {
        this.configFile = configFile;
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    // --- For tests: access to underlying config ---

    public FileConfiguration getConfig() {
        return config;
    }

    // --- Instance methods ---

    public String language() {
        return config.getString("language", "en").toLowerCase();
    }

    public boolean metricsEnabled() {
        return config.getBoolean("metrics", true);
    }

    public List<String> heads() {
        return config.getStringList("heads");
    }

    public boolean headsThemeEnabled() {
        return config.getBoolean("headsTheme.enabled", false);
    }

    public String headsThemeSelected() {
        return config.getString("headsTheme.selected", "");
    }

    public HashMap<String, List<String>> headsTheme() {
        var headsTheme = new HashMap<String, List<String>>();

        var headsThemeSection = config.getConfigurationSection("headsTheme.theme");
        if (headsThemeSection == null) {
            return new HashMap<>();
        }

        for (String theme : headsThemeSection.getKeys(false)) {
            headsTheme.put(theme, new ArrayList<>(config.getStringList("headsTheme.theme." + theme)));
        }

        return headsTheme;
    }

    public String headClickAlreadyOwnSound() {
        return config.getString("headClick.sounds.alreadyOwn");
    }

    public String headClickNotOwnSound() {
        return config.getString("headClick.sounds.notOwn");
    }

    public List<String> headClickMessages() {
        return config.getStringList("headClick.messages");
    }

    public boolean headClickTitleEnabled() {
        return config.getBoolean("headClick.title.enabled", false);
    }

    public String headClickTitleFirstLine() {
        return config.getString("headClick.title.firstLine", "");
    }

    public String headClickTitleSubTitle() {
        return config.getString("headClick.title.subTitle", "");
    }

    public int headClickTitleFadeIn() {
        return config.getInt("headClick.title.fadeIn", 0);
    }

    public int headClickTitleStay() {
        return config.getInt("headClick.title.stay", 50);
    }

    public int headClickTitleFadeOut() {
        return config.getInt("headClick.title.fadeOut", 0);
    }

    public boolean fireworkEnabled() {
        return config.getBoolean("headClick.firework.enabled", false);
    }

    public List<Color> headClickFireworkColors() {
        List<Color> colors = new ArrayList<>();

        if (!config.contains("headClick.firework.colors")) {
            return colors;
        }

        config.getStringList("headClick.firework.colors").forEach(color -> {
            try {
                String[] s = color.split(",");
                colors.add(Color.fromRGB(Integer.parseInt(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2])));
            } catch (Exception ex) {
                LogUtil.error("Cannot parse RGB color of {0}. Format is : r,g,b", color);
            }
        });

        return colors;
    }

    public List<Color> headClickFireworkFadeColors() {
        List<Color> colors = new ArrayList<>();

        if (!config.contains("headClick.firework.fadeColors")) {
            return colors;
        }

        config.getStringList("headClick.firework.fadeColors").forEach(color -> {
            try {
                String[] s = color.split(",");
                colors.add(Color.fromRGB(Integer.parseInt(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2])));
            } catch (Exception ex) {
                LogUtil.error("Cannot parse RGB color of {0}. Format is : r,g,b", color);
            }
        });

        return colors;
    }

    public boolean fireworkFlickerEnabled() {
        return config.getBoolean("headClick.firework.flicker", true);
    }

    public int headClickFireworkPower() {
        return config.getInt("headClick.firework.power", 0);
    }

    public boolean headClickParticlesEnabled() {
        return config.getBoolean("headClick.particles.enabled", false);
    }

    public String headClickParticlesAlreadyOwnType() {
        return config.getString("headClick.particles.alreadyOwn.type", "VILLAGER_ANGRY");
    }

    public int headClickParticlesAmount() {
        return config.getInt("headClick.particles.alreadyOwn.amount", 1);
    }

    public ArrayList<String> headClickParticlesColors() {
        return new ArrayList<>(config.getStringList("headClick.particles.alreadyOwn.colors"));
    }

    public List<String> headClickCommands() {
        return config.getStringList("headClick.commands");
    }

    public boolean headClickEjectEnabled() {
        return config.getBoolean("headClick.pushBack.enabled", false);
    }

    public double headClickEjectPower() {
        return config.getDouble("headClick.pushBack.power", 1D);
    }

    public boolean resetPlayerData() {
        return config.getBoolean("shouldResetPlayerData", true);
    }

    public boolean isHideFoundHeads() {
        return config.getBoolean("hideFoundHeads", false);
    }

    public int progressBarBars() {
        return config.getInt("progressBar.totalBars", 100);
    }

    public String progressBarSymbol() {
        return config.getString("progressBar.symbol");
    }

    public String progressBarNotCompletedColor() {
        return config.getString("progressBar.notCompletedColor");
    }

    public String progressBarCompletedColor() {
        return config.getString("progressBar.completedColor");
    }

    public boolean redisEnabled() {
        return config.getBoolean("redis.enable", false);
    }

    public String redisHostname() {
        return config.getString("redis.settings.hostname", Protocol.DEFAULT_HOST);
    }

    public int redisDatabase() {
        return config.getInt("redis.settings.database", Protocol.DEFAULT_DATABASE);
    }

    public String redisPassword() {
        return config.getString("redis.settings.password", "");
    }

    public int redisPort() {
        return config.getInt("redis.settings.port", Protocol.DEFAULT_PORT);
    }

    public boolean databaseEnabled() {
        return config.getBoolean("database.enable", false);
    }

    public EnumTypeDatabase databaseType() {
        var type = EnumTypeDatabase.of(config.getString("database.type", "MySQL"));
        if (type == null) {
            type = EnumTypeDatabase.MySQL;
        }

        return type;
    }

    public String databaseHostname() {
        return config.getString("database.settings.hostname", "localhost");
    }

    public String databaseName() {
        return config.getString("database.settings.database");
    }

    public String databaseUsername() {
        return config.getString("database.settings.username");
    }

    public String databasePassword() {
        return config.getString("database.settings.password");
    }

    public int databasePort() {
        return config.getInt("database.settings.port", 3306);
    }

    public boolean databaseSsl() {
        return config.getBoolean("database.settings.ssl", false);
    }

    public String databasePrefix() {
        return config.getString("database.settings.prefix", "");
    }

    public int databaseMaxConnections() {
        return config.getInt("database.settings.pool.maxConnections", 10);
    }

    public int databaseMinIdleConnections() {
        return config.getInt("database.settings.pool.minIdleConnections", 2);
    }

    public long databaseConnectionTimeout() {
        return config.getLong("database.settings.pool.connectionTimeout", 5) * 1000;
    }

    public long databaseIdleTimeout() {
        return config.getLong("database.settings.pool.idleTimeout", 300) * 1000;
    }

    public long databaseMaxLifetime() {
        return config.getLong("database.settings.pool.maxLifetime", 1800) * 1000;
    }

    public boolean preventCommandsOnTieredRewardsLevel() {
        return config.getBoolean("preventCommandsOnTieredRewardsLevel", false);
    }

    public boolean preventMessagesOnTieredRewardsLevel() {
        return config.getBoolean("preventMessagesOnTieredRewardsLevel", false);
    }

    public boolean particlesEnabled() {
        return particlesFoundEnabled() || particlesNotFoundEnabled();
    }

    public boolean particlesFoundEnabled() {
        return config.getBoolean("floatingParticles.found.enabled", true);
    }

    public boolean particlesNotFoundEnabled() {
        return config.getBoolean("floatingParticles.notFound.enabled", false);
    }

    public String particlesNotFoundType() {
        return config.getString("floatingParticles.notFound.type", "REDSTONE");
    }

    public ArrayList<String> particlesNotFoundColors() {
        return new ArrayList<>(config.getStringList("floatingParticles.notFound.colors"));
    }

    public int particlesNotFoundAmount() {
        return config.getInt("floatingParticles.notFound.amount", 3);
    }

    public String particlesFoundType() {
        return config.getString("floatingParticles.found.type", "REDSTONE");
    }

    public ArrayList<String> particlesFoundColors() {
        return new ArrayList<>(config.getStringList("floatingParticles.found.colors"));
    }

    public int particlesFoundAmount() {
        return config.getInt("floatingParticles.found.amount", 3);
    }

    public List<TieredReward> tieredRewards() {
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

                int slotsRequired = -1;
                if (config.contains("tieredRewards." + level + ".slotsRequired")) {
                    slotsRequired = config.getInt("tieredRewards." + level + ".slotsRequired", -1);
                }

                boolean isRandom = false;
                if (config.contains("tieredRewards." + level + ".randomizeCommands")) {
                    isRandom = config.getBoolean("tieredRewards." + level + ".randomizeCommands", false);
                }

                if (!messages.isEmpty() || !commands.isEmpty() || !broadcastMessages.isEmpty() || slotsRequired != -1) {
                    tieredRewards.add(new TieredReward(Integer.parseInt(level), messages, commands, broadcastMessages, slotsRequired, isRandom));
                }
            } catch (Exception ex) {
                LogUtil.error("Cannot read tiered rewards of \"{0}\". Error message :{1}", level, ex.getMessage());
            }
        }

        return tieredRewards;
    }

    public int hologramParticlePlayerViewDistance() {
        return config.getInt("internalTask.hologramParticlePlayerViewDistance", 16);
    }

    public int delayGlobalTask() {
        return config.getInt("internalTask.delay", 20);
    }

    public String hologramPlugin() {
        return config.getString("holograms.plugin");
    }

    public double hologramsHeightAboveHead() {
        return config.getDouble("holograms.heightAboveHead", 0.5);
    }

    public boolean hologramsEnabled() {
        return hologramsFoundEnabled() || hologramsNotFoundEnabled();
    }

    public boolean hologramsFoundEnabled() {
        return config.getBoolean("holograms.found.enabled", true);
    }

    public boolean hologramsNotFoundEnabled() {
        return config.getBoolean("holograms.notFound.enabled", true);
    }

    public ArrayList<String> hologramsFoundLines() {
        return new ArrayList<>(config.getStringList("holograms.found.lines"));
    }

    public ArrayList<String> hologramsNotFoundLines() {
        return new ArrayList<>(config.getStringList("holograms.notFound.lines"));
    }

    public String hologramAdvancedFoundPlaceholder() {
        return config.getString("holograms.advanced.foundPlaceholder", "&a&lFound");
    }

    public String hologramAdvancedNotFoundPlaceholder() {
        return config.getString("holograms.advanced.notFoundPlaceholder", "&c&lNot found");
    }

    public ArrayList<String> hologramsAdvancedLines() {
        return new ArrayList<>(config.getStringList("holograms.advanced.lines"));
    }

    public ItemBuilder guiBackIcon() {
        return new ItemBuilder(Material.valueOf(config.getString("gui.backIcon.type", Material.SPRUCE_DOOR.name())));
    }

    public ItemBuilder guiBorderIcon() {
        return new ItemBuilder(Material.valueOf(config.getString("gui.borderIcon.type", Material.GRAY_STAINED_GLASS_PANE.name())));
    }

    public ItemBuilder guiPreviousIcon() {
        return new ItemBuilder(Material.valueOf(config.getString("gui.previousIcon.type", Material.ARROW.name())));
    }

    public ItemBuilder guiNextIcon() {
        return new ItemBuilder(Material.valueOf(config.getString("gui.nextIcon.type", Material.ARROW.name())));
    }

    public ItemBuilder guiCloseIcon() {
        return new ItemBuilder(Material.valueOf(config.getString("gui.closeIcon.type", Material.BARRIER.name())));
    }

    public boolean headClickCommandsRandomized() {
        return config.getBoolean("headClick.randomizeCommands", false);
    }

    public int headClickCommandsSlotsRequired() {
        return config.getInt("headClick.slotsRequired", -1);
    }

    public boolean preventPistonExtension() {
        return config.getBoolean("externalInteractions.piston", true);
    }

    public boolean preventLiquidFlow() {
        return config.getBoolean("externalInteractions.water", true);
    }

    public boolean preventExplosion() {
        return config.getBoolean("externalInteractions.explosion", true);
    }

    public String placeholdersLeaderboardPrefix() {
        return config.getString("placeholders.leaderboard.prefix", "");
    }

    public String placeholdersLeaderboardSuffix() {
        return config.getString("placeholders.leaderboard.suffix", "");
    }

    public boolean placeholdersLeaderboardUseNickname() {
        return config.getBoolean("placeholders.leaderboard.nickname", false);
    }

    public boolean spinEnabled() {
        return config.getBoolean("spin.enabled", true);
    }

    public int spinSpeed() {
        return config.getInt("spin.speed", 1);
    }

    public boolean spinLinked() {
        return config.getBoolean("spin.linked", true);
    }

    public int hintDistanceBlocks() {
        return config.getInt("hint.distance", 16);
    }

    public int hintFrequency() {
        return config.getInt("hint.frequency", 5);
    }

    public int hintSoundVolume() {
        return config.getInt("hint.sound.volume", 1);
    }

    public XSound hintSoundType() {
        var soundInConfig = config.getString("hint.sound.sound", "BLOCK_AMETHYST_BLOCK_CHIME");

        var sound = XSound.BLOCK_AMETHYST_BLOCK_CHIME;

        var optSound = XSound.of(soundInConfig);
        if (optSound.isPresent()) {
            sound = optSound.get();
        } else {
            LogUtil.error("Error, cannot parse sound of \"{0}\".", soundInConfig);
        }

        return sound;
    }

    public String hintActionBarMessage() {
        return config.getString("hint.actionBarMessage", "%prefix% &aPssst, a mystery block is near! &7(%arrow%)");
    }

}
