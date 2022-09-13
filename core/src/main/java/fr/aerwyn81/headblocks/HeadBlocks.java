package fr.aerwyn81.headblocks;

import fr.aerwyn81.headblocks.commands.HBCommandExecutor;
import fr.aerwyn81.headblocks.data.head.types.HBHeadHDB;
import fr.aerwyn81.headblocks.events.*;
import fr.aerwyn81.headblocks.hooks.PlaceholderHook;
import fr.aerwyn81.headblocks.runnables.GlobalTask;
import fr.aerwyn81.headblocks.services.*;
import fr.aerwyn81.headblocks.utils.*;
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
    public static boolean isReloadInProgress;
    public static boolean isProtocolLibActive;

    private GlobalTask globalTask;
    private HeadDatabaseAPI headDatabaseAPI;

    @Override
    public void onEnable() {
        instance = this;
        log = Bukkit.getConsoleSender();

        log.sendMessage(MessageUtils.colorize("&6&lH&e&lead&6&lB&e&llocks &einitializing..."));

        File configFile = new File(getDataFolder(), "config.yml");
        File locationFile = new File(getDataFolder(), "locations.yml");

        saveDefaultConfig();
        try {
            ConfigUpdater.update(this, "config.yml", configFile, Arrays.asList("tieredRewards", "heads"));
        } catch (IOException e) {
            log.sendMessage(MessageUtils.colorize("&cError while loading config file: " + e.getMessage()));
            this.setEnabled(false);
            return;
        }
        reloadConfig();

        if (VersionUtils.getCurrent().isOlderOrSameThan(VersionUtils.v1_16)) {
            log.sendMessage(MessageUtils.colorize("&c***** --------------------------------------- *****"));
            log.sendMessage(MessageUtils.colorize("&cHeadBlocks version 2 does not support your Minecraft Server version: " + VersionUtils.getCurrentFormatted()));
            log.sendMessage(MessageUtils.colorize("&cIf you are using a version below Minecraft 1.17, use the version 1.6 of the plugin"));
            log.sendMessage(MessageUtils.colorize("&cVersion 1.6 will not receive any new features but may receive corrective updates."));
            log.sendMessage(MessageUtils.colorize("&c***** --------------------------------------- *****"));
            this.setEnabled(false);
            return;
        }

        isPlaceholderApiActive = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        isHeadDatabaseActive = Bukkit.getPluginManager().isPluginEnabled("HeadDatabase");
        isProtocolLibActive = Bukkit.getPluginManager().isPluginEnabled("ProtocolLib");

        if (ConfigService.isMetricsEnabled()) {
            new Metrics(this, 15495);
        }

        ConfigService.initialize(configFile);

        LanguageService.initialize(ConfigService.getLanguage());
        LanguageService.pushMessages();

        StorageService.initialize();
        HologramService.initialize();
        HeadService.initialize(locationFile);

        this.globalTask = new GlobalTask();
        globalTask.runTaskTimer(this, 0, ConfigService.getDelayGlobalTask());

        if (isPlaceholderApiActive) {
            new PlaceholderHook().register();
        }

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

        getCommand("headblocks").setExecutor(new HBCommandExecutor());

        Bukkit.getPluginManager().registerEvents(new OnPlayerInteractEvent(), this);
        Bukkit.getPluginManager().registerEvents(new OnPlayerBreakBlockEvent(), this);
        Bukkit.getPluginManager().registerEvents(new OnPlayerPlaceBlockEvent(), this);
        Bukkit.getPluginManager().registerEvents(new OthersEvent(), this);

        log.sendMessage(MessageUtils.colorize("&6&lH&e&lead&6&lB&e&llocks &asuccessfully loaded!"));
    }

    @Override
    public void onDisable() {
        StorageService.close();
        HeadService.clearHeadMoves();

        Bukkit.getScheduler().cancelTasks(this);

        log.sendMessage(MessageUtils.colorize("&6&lH&e&lead&6&lB&e&llocks &cdisabled!"));
    }

    public void loadHeadsHDB() {
        HeadService.getHeads().stream()
                .filter(h -> h instanceof HBHeadHDB)
                .map(h -> (HBHeadHDB) h)
                .filter(h -> !h.isLoaded())
                .forEach(h -> {
                    HeadUtils.createHead(h, headDatabaseAPI.getBase64(h.getId()));
                    h.setLoaded(true);
                });
    }

    public static HeadBlocks getInstance() {
        return instance;
    }

    public void setParticlesTask(GlobalTask globalTask) {
        this.globalTask = globalTask;
    }

    public GlobalTask getParticlesTask() {
        return globalTask;
    }
}
