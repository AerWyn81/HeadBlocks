package fr.aerwyn81.headblocks;

import fr.aerwyn81.headblocks.api.HeadBlocksAPI;
import fr.aerwyn81.headblocks.commands.HBCommandExecutor;
import fr.aerwyn81.headblocks.data.head.HeadType;
import fr.aerwyn81.headblocks.events.OnHeadDatabaseLoaded;
import fr.aerwyn81.headblocks.events.OnPlayerInteractEvent;
import fr.aerwyn81.headblocks.events.OnPlayerPlaceBlockEvent;
import fr.aerwyn81.headblocks.handlers.*;
import fr.aerwyn81.headblocks.placeholders.PlaceholderHook;
import fr.aerwyn81.headblocks.runnables.ParticlesTask;
import fr.aerwyn81.headblocks.utils.ConfigUpdater;
import fr.aerwyn81.headblocks.utils.FormatUtils;
import fr.aerwyn81.headblocks.utils.HeadUtils;
import fr.aerwyn81.headblocks.utils.Version;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

@SuppressWarnings("ConstantConditions")
public final class HeadBlocks extends JavaPlugin {

    public static ConsoleCommandSender log;
    private static HeadBlocks instance;
    public static boolean isPlaceholderApiActive;
    public static boolean isHeadDatabaseActive;

    private LegacySupport legacySupport;

    private ConfigHandler configHandler;
    private LanguageHandler languageHandler;
    private HeadHandler headHandler;
    private RewardHandler rewardHandler;
    private StorageHandler storageHandler;

    private HeadBlocksAPI headBlocksAPI;

    private ParticlesTask particlesTask;
    private HeadDatabaseAPI headDatabaseAPI;

    @Override
    public void onEnable() {
        instance = this;
        log = Bukkit.getConsoleSender();

        log.sendMessage(FormatUtils.translate("&6HeadBlocks &einitializing..."));

        File configFile = new File(getDataFolder(), "config.yml");
        File backupConfigFile = new File(getDataFolder(), "config.yml.save");
        File locationFile = new File(getDataFolder(), "locations.yml");

        ConfigUpdater.saveOldConfig(backupConfigFile, configFile);
        saveDefaultConfig();
        try {
            ConfigUpdater.update(this, "config.yml", configFile, Arrays.asList("tieredRewards", "heads"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        reloadConfig();

        if (Version.getCurrent() == Version.v1_8) {
            legacySupport = new LegacyHelper();
        }

        this.configHandler = new ConfigHandler(configFile);
        this.configHandler.loadConfiguration();

        this.languageHandler = new LanguageHandler(this, configHandler.getLanguage());
        this.languageHandler.pushMessages();

        this.headHandler = new HeadHandler(this, locationFile);

        this.storageHandler = new StorageHandler(this);
        this.storageHandler.openConnection();
        this.storageHandler.getStorage().init();
        this.storageHandler.getDatabase().load();

        this.rewardHandler = new RewardHandler(this);

        this.headBlocksAPI = new HeadBlocksAPI();

        if (configHandler.isParticlesEnabled()) {
            if (Version.getCurrent().isOlderThan(Version.v1_13)) {
                log.sendMessage(FormatUtils.translate("&cParticles is enabled but not supported before 1.13 included."));
            } else {
                this.particlesTask = new ParticlesTask(this);
                particlesTask.runTaskTimer(this, 0, configHandler.getParticlesDelay());
            }
        }

        getCommand("headblocks").setExecutor(new HBCommandExecutor(this));

        Bukkit.getPluginManager().registerEvents(new OnPlayerInteractEvent(this), this);
        Bukkit.getPluginManager().registerEvents(new OnPlayerPlaceBlockEvent(this), this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderHook(this).register();
            isPlaceholderApiActive = true;
        }

        isHeadDatabaseActive = Bukkit.getPluginManager().isPluginEnabled("HeadDatabase");
        if (isHeadDatabaseActive) {
            headDatabaseAPI = new HeadDatabaseAPI();
            Bukkit.getPluginManager().registerEvents(new OnHeadDatabaseLoaded(this), this);

            this.headHandler.loadConfiguration();

            // Plugman/HeadDatabase issue.
            // OnHeadDatabaseLoaded is not called on plugman load, it's not clean, it should be done better
            this.loadHeadsHDB();

        } else {
            this.headHandler.loadConfiguration();
        }

        Bukkit.getScheduler().runTaskLater(this, () -> getHeadHandler().loadLocations(), 1L);

        log.sendMessage(FormatUtils.translate("&a&6HeadBlocks &asuccessfully loaded!"));
    }

    @Override
    public void onDisable() {
        if (storageHandler != null) {
            storageHandler.getStorage().close();
            storageHandler.getDatabase().close();
        }

        Bukkit.getScheduler().cancelTasks(this);

        log.sendMessage(FormatUtils.translate("&6HeadBlocks &cdisabled!"));
    }

    public void loadHeadsHDB() {
        this.getHeadHandler().getHeads().stream()
                .filter(h -> !h.isLoaded() && h.getHeadType() == HeadType.HDB)
                .forEach(h -> {
                    h.setTexture(this.getHeadDatabaseAPI().getBase64(h.getId()));
                    HeadUtils.applyTexture(h);
                });
    }

    public static HeadBlocks getInstance() {
        return instance;
    }

    public ConfigHandler getConfigHandler() {
        return configHandler;
    }

    public LanguageHandler getLanguageHandler() {
        return languageHandler;
    }

    public HeadHandler getHeadHandler() {
        return headHandler;
    }

    public RewardHandler getRewardHandler() {
        return rewardHandler;
    }

    public HeadBlocksAPI getHeadBlocksAPI() {
        return headBlocksAPI;
    }

    public StorageHandler getStorageHandler() {
        return storageHandler;
    }

    public LegacySupport getLegacySupport() {
        return legacySupport;
    }

    public void setParticlesTask(ParticlesTask particlesTask) {
        this.particlesTask = particlesTask;
    }

    public ParticlesTask getParticlesTask() {
        return particlesTask;
    }

    public HeadDatabaseAPI getHeadDatabaseAPI() {
        return headDatabaseAPI;
    }
}
