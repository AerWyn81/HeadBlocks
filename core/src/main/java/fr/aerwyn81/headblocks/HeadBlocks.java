package fr.aerwyn81.headblocks;

import fr.aerwyn81.headblocks.api.HeadBlocksAPI;
import fr.aerwyn81.headblocks.commands.HBCommands;
import fr.aerwyn81.headblocks.commands.HBTabCompleter;
import fr.aerwyn81.headblocks.events.OnPlayerInteractEvent;
import fr.aerwyn81.headblocks.events.OnPlayerPlaceBlockEvent;
import fr.aerwyn81.headblocks.handlers.*;
import fr.aerwyn81.headblocks.placeholders.PlaceholderHook;
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

    private IVersionCompatibility versionCompatibility;

    private ConfigHandler configHandler;
    private LanguageHandler languageHandler;
    private HeadHandler headHandler;
    private RewardHandler rewardHandler;
    private StorageHandler storageHandler;

    private HeadBlocksAPI headBlocksAPI;

    @Override
    public void onEnable() {
        instance = this;
        log = Bukkit.getConsoleSender();

        log.sendMessage(FormatUtils.translate("&6HeadBlocks &einitializing..."));

        File configFile = new File(getDataFolder(), "config.yml");
        File locationFile = new File(getDataFolder(), "locations.yml");

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
            this.rewardHandler.loadRewards();
        }, 1L);

        this.headBlocksAPI = new HeadBlocksAPI();

        registerCommands();

        Bukkit.getPluginManager().registerEvents(new OnPlayerInteractEvent(this), this);
        Bukkit.getPluginManager().registerEvents(new OnPlayerPlaceBlockEvent(this), this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderHook(this).register();
            isPlaceholderApiActive = true;
        }

        log.sendMessage(FormatUtils.translate("&a&6HeadBlocks &asuccessfully loaded!"));
    }

    @Override
    public void onDisable() {
        if (storageHandler != null) {
            storageHandler.getStorage().close();
            storageHandler.getDatabase().close();
        }

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

    public IVersionCompatibility getVersionCompatibility() {
        return versionCompatibility;
    }

    private boolean setupVersionCompatibility() {
        String currentVersionFormatted = Version.getCurrentFormatted();

        switch (Version.getCurrent()) {
            case v1_8:
                versionCompatibility = new Helper18();
                break;
            case v1_9:
                versionCompatibility = new Helper19();
                break;
            case v1_10:
                versionCompatibility = new Helper110();
                break;
            case v1_11:
                versionCompatibility = new Helper111();
                break;
            case v1_12:
                versionCompatibility = new Helper112();
                break;
            case v1_13:
                versionCompatibility = new Helper113();
                break;
            case v1_14:
                versionCompatibility = new Helper114();
                break;
            case v1_15:
                versionCompatibility = new Helper115();
                break;
            case v1_16:
                versionCompatibility = new Helper116();
                break;
            case v1_17:
                versionCompatibility = new Helper117();
                break;
            case v1_18:
                versionCompatibility = new Helper118();
                break;
            default:
                log.sendMessage(FormatUtils.translate("&cError initializing compatibility version support, version " + currentVersionFormatted + " not yet supported!"));
                return false;
        }

        log.sendMessage(FormatUtils.translate("&aSuccessfully loaded version compatibility for v" + currentVersionFormatted));
        return true;
    }
}
