package fr.aerwyn81.headblocks;

import fr.aerwyn81.headblocks.api.HeadBlocksAPI;
import fr.aerwyn81.headblocks.commands.HBCommandExecutor;
import fr.aerwyn81.headblocks.data.head.types.HBHeadHDB;
import fr.aerwyn81.headblocks.events.OnHeadDatabaseLoaded;
import fr.aerwyn81.headblocks.events.OnPlayerInteractEvent;
import fr.aerwyn81.headblocks.events.OnPlayerPlaceBlockEvent;
import fr.aerwyn81.headblocks.events.OthersEvent;
import fr.aerwyn81.headblocks.handlers.*;
import fr.aerwyn81.headblocks.hooks.PlaceholderHook;
import fr.aerwyn81.headblocks.runnables.ParticlesTask;
import fr.aerwyn81.headblocks.utils.ConfigUpdater;
import fr.aerwyn81.headblocks.utils.HeadUtils;
import fr.aerwyn81.headblocks.utils.MessageUtils;
import fr.aerwyn81.headblocks.utils.Version;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import me.arcaniax.hdb.enums.CategoryEnum;
import me.arcaniax.hdb.object.head.Head;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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

        log.sendMessage(MessageUtils.translate("&6&lH&e&lead&6&lB&e&llocks &einitializing..."));

        File configFile = new File(getDataFolder(), "config.yml");
        File backupConfigFile = new File(getDataFolder(), "config.yml.save");
        File locationFile = new File(getDataFolder(), "locations.yml");

        ConfigUpdater.saveOldConfig(backupConfigFile, configFile);
        saveDefaultConfig();
        try {
            ConfigUpdater.update(this, "config.yml", configFile, Arrays.asList("tieredRewards", "heads"));
        } catch (IOException e) {
            log.sendMessage(MessageUtils.translate("&cError while loading config file: " + e.getMessage()));
            this.setEnabled(false);
            return;
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
        this.headHandler.loadConfiguration();

        this.storageHandler = new StorageHandler(this);
        this.storageHandler.initStorage();

        this.rewardHandler = new RewardHandler(this);

        this.headBlocksAPI = new HeadBlocksAPI();

        if (configHandler.isParticlesEnabled()) {
            if (Version.getCurrent().isOlderThan(Version.v1_13)) {
                log.sendMessage(MessageUtils.translate("&cParticles are enabled but not supported before 1.13 included."));
            } else {
                this.particlesTask = new ParticlesTask(this);
                particlesTask.runTaskTimer(this, 0, configHandler.getParticlesDelay());
            }
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderHook(this).register();
            isPlaceholderApiActive = true;
        }

        isHeadDatabaseActive = Bukkit.getPluginManager().isPluginEnabled("HeadDatabase");
        if (isHeadDatabaseActive) {
            headDatabaseAPI = new HeadDatabaseAPI();
            Bukkit.getPluginManager().registerEvents(new OnHeadDatabaseLoaded(this), this);

            // Plugman/HeadDatabase issue
            // OnHeadDatabaseLoaded event is not fired on plugman reload command
            try {
                // If the list is not empty, then the database is already loaded
                List<Head> heads = headDatabaseAPI.getHeads(CategoryEnum.ALPHABET);
                if (heads != null & heads.size() > 0) {
                    this.loadHeadsHDB();
                }
            } catch (Exception ignored) { }
        }

        Bukkit.getScheduler().runTaskLater(this, () -> getHeadHandler().loadLocations(), 1L);

        getCommand("headblocks").setExecutor(new HBCommandExecutor(this));

        Bukkit.getPluginManager().registerEvents(new OnPlayerInteractEvent(this), this);
        Bukkit.getPluginManager().registerEvents(new OnPlayerPlaceBlockEvent(this), this);
        Bukkit.getPluginManager().registerEvents(new OthersEvent(this), this);

        log.sendMessage(MessageUtils.translate("&6&lH&e&lead&6&lB&e&llocks &asuccessfully loaded!"));
    }

    @Override
    public void onDisable() {
        if (storageHandler != null) {
            storageHandler.close();
        }

        Bukkit.getScheduler().cancelTasks(this);

        log.sendMessage(MessageUtils.translate("&6HeadBlocks &cdisabled!"));
    }

    public void loadHeadsHDB() {
        this.getHeadHandler().getHeads().stream()
                .filter(h -> h instanceof HBHeadHDB)
                .map(h -> (HBHeadHDB) h)
                .filter(h -> !h.isLoaded())
                .forEach(h -> {
                    HeadUtils.applyTexture(h, headDatabaseAPI.getBase64(h.getId()));
                    h.setLoaded(true);
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
}
