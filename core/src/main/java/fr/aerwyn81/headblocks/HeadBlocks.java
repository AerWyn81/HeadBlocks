package fr.aerwyn81.headblocks;

import fr.aerwyn81.headblocks.api.HeadBlocksAPI;
import fr.aerwyn81.headblocks.commands.HBCommands;
import fr.aerwyn81.headblocks.commands.HBTabCompleter;
import fr.aerwyn81.headblocks.events.OnPlayerInteractEvent;
import fr.aerwyn81.headblocks.events.OnPlayerPlaceBlockEvent;
import fr.aerwyn81.headblocks.handlers.*;
import fr.aerwyn81.headblocks.placeholders.PlaceholderHook;
import fr.aerwyn81.headblocks.runnables.ParticlesTask;
import fr.aerwyn81.headblocks.utils.ConfigUpdater;
import fr.aerwyn81.headblocks.utils.FormatUtils;
import fr.aerwyn81.headblocks.utils.Version;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

@SuppressWarnings("ConstantConditions")
public final class HeadBlocks extends JavaPlugin {

    public static ConsoleCommandSender log;
    private static HeadBlocks instance;
    public static boolean isPlaceholderApiActive;
    public static boolean isTitleApiActive;

    private LegacySupport legacySupport;

    private ConfigHandler configHandler;
    private LanguageHandler languageHandler;
    private HeadHandler headHandler;
    private RewardHandler rewardHandler;
    private StorageHandler storageHandler;

    private HeadBlocksAPI headBlocksAPI;

    private ParticlesTask particlesTask;

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
            ConfigUpdater.update(this, "config.yml", configFile, Collections.singletonList("tieredRewards"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        reloadConfig();

        if (!setupVersionCompatibility()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.configHandler = new ConfigHandler(configFile);
        this.configHandler.loadConfiguration();

        this.languageHandler = new LanguageHandler(this, configHandler.getLanguage());
        this.languageHandler.pushMessages();

        this.headHandler = new HeadHandler(this, locationFile);
        this.headHandler.loadConfiguration();

        this.storageHandler = new StorageHandler(this);
        this.storageHandler.openConnection();
        this.storageHandler.getStorage().init();
        this.storageHandler.getDatabase().load();

        this.rewardHandler = new RewardHandler(this);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            this.headHandler.loadLocations();
        }, 1L);

        this.headBlocksAPI = new HeadBlocksAPI();

        if (configHandler.isParticlesEnabled()) {
            if (Version.getCurrent().isOlderThan(Version.v1_13)) {
                log.sendMessage(FormatUtils.translate("&cParticles is enabled but not supported before 1.13 included."));
            } else {
                this.particlesTask = new ParticlesTask(this);
                particlesTask.runTaskTimer(this, 0, configHandler.getParticlesDelay());
            }
        }

        registerCommands();

        Bukkit.getPluginManager().registerEvents(new OnPlayerInteractEvent(this), this);
        Bukkit.getPluginManager().registerEvents(new OnPlayerPlaceBlockEvent(this), this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderHook(this).register();
            isPlaceholderApiActive = true;
        }

        if (Bukkit.getPluginManager().isPluginEnabled("TitleAPI")) {
            isTitleApiActive = true;
        }

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

    private void registerCommands() {
        getCommand("headblocks").setExecutor(new HBCommands(this));
        getCommand("headblocks").setTabCompleter(new HBTabCompleter(this));
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

    private boolean setupVersionCompatibility() {
        String currentVersionFormatted = Version.getCurrentFormatted();

        if (Version.getCurrent() == Version.v1_8) {
            legacySupport = new LegacyHelper();
        }

        log.sendMessage(FormatUtils.translate("&aSuccessfully loaded version compatibility for v" + currentVersionFormatted));
        return true;
    }
}
