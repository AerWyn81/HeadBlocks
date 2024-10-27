package fr.aerwyn81.headblocks;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import fr.aerwyn81.headblocks.commands.HBCommandExecutor;
import fr.aerwyn81.headblocks.events.*;
import fr.aerwyn81.headblocks.hooks.HeadDatabaseHook;
import fr.aerwyn81.headblocks.hooks.PlaceholderHook;
import fr.aerwyn81.headblocks.runnables.GlobalTask;
import fr.aerwyn81.headblocks.services.*;
import fr.aerwyn81.headblocks.utils.bukkit.VersionUtils;
import fr.aerwyn81.headblocks.utils.config.ConfigUpdater;
import fr.aerwyn81.headblocks.utils.internal.Metrics;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
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
    public static boolean isReloadInProgress;
    public static boolean isDecentHologramsActive;
    public static boolean isFancyHologramsActive;
    public static boolean isCMIActive;
    public static boolean isHeadDatabaseActive;

    private GlobalTask globalTask;
    private HeadDatabaseHook headDatabaseHook;

    @Override
    public void onEnable() {
        instance = this;
        log = Bukkit.getConsoleSender();

        initializeExternals();

        log.sendMessage(MessageUtils.colorize("&6&lH&e&lead&6&lB&e&llocks &einitializing..."));

        File configFile = new File(getDataFolder(), "config.yml");
        File locationFile = new File(getDataFolder(), "locations.yml");

        saveDefaultConfig();
        try {
            ConfigUpdater.update(this, "config.yml", configFile, Arrays.asList("tieredRewards", "heads", "headsTheme"));
        } catch (IOException e) {
            log.sendMessage(MessageUtils.colorize("&cError while loading config file: " + e.getMessage()));
            getPluginLoader().disablePlugin(this);
            return;
        }
        reloadConfig();

        if (!VersionUtils.isAtLeastVersion(VersionUtils.v1_20_R1)) {
            log.sendMessage(MessageUtils.colorize("&c***** --------------------------------------- *****"));
            log.sendMessage(MessageUtils.colorize("&cHeadBlocks does not support your Minecraft Server version: " + VersionUtils.getVersion()));
            log.sendMessage(MessageUtils.colorize("&c***** --------------------------------------- *****"));
            getPluginLoader().disablePlugin(this);
            return;
        }

        isHeadDatabaseActive = Bukkit.getPluginManager().isPluginEnabled("HeadDatabase");

        isPlaceholderApiActive = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        if (isPlaceholderApiActive) {
            new PlaceholderHook().register();
        }

        isDecentHologramsActive = Bukkit.getPluginManager().isPluginEnabled("DecentHolograms");
        isCMIActive = Bukkit.getPluginManager().isPluginEnabled("CMI");
        isFancyHologramsActive = Bukkit.getPluginManager().isPluginEnabled("FancyHolograms");

        ConfigService.initialize(configFile);

        LanguageService.initialize(ConfigService.getLanguage());
        LanguageService.pushMessages();

        StorageService.initialize();
        HeadService.initialize(locationFile);
        HologramService.load();
        GuiService.initialize();

        startInternalTaskTimer();

        if (isHeadDatabaseActive) {
            this.headDatabaseHook = new HeadDatabaseHook();
            if (!this.headDatabaseHook.init()) {
                isHeadDatabaseActive = false;
            }
        }

        getCommand("headblocks").setExecutor(new HBCommandExecutor());

        Bukkit.getPluginManager().registerEvents(new OnPlayerInteractEvent(), this);
        Bukkit.getPluginManager().registerEvents(new OnPlayerBreakBlockEvent(), this);
        Bukkit.getPluginManager().registerEvents(new OnPlayerPlaceBlockEvent(), this);
        Bukkit.getPluginManager().registerEvents(new OthersEvent(), this);
        Bukkit.getPluginManager().registerEvents(new OnPlayerClickInventoryEvent(), this);

        if (ConfigService.isMetricsEnabled()) {
            var m = new Metrics(this, 15495);
            m.addCustomChart(new Metrics.SimplePie("database_type", StorageService::selectedStorageType));
            m.addCustomChart(new Metrics.SingleLineChart("heads", () -> HeadService.getChargedHeadLocations().size()));
            m.addCustomChart(new Metrics.SimplePie("lang", LanguageService::getLanguage));
            m.addCustomChart(new Metrics.SimplePie("feat_order", () -> {
                var anyOrder = HeadService.getChargedHeadLocations().stream().anyMatch(h -> h.getOrderIndex() != -1);
                return anyOrder ? "True" : "False";
            }));
            m.addCustomChart(new Metrics.SimplePie("feat_hit", () -> {
                var anyHit = HeadService.getChargedHeadLocations().stream().anyMatch(h -> h.getHitCount() != -1);
                return anyHit ? "True" : "False";
            }));
        }

        log.sendMessage(MessageUtils.colorize("&6&lH&e&lead&6&lB&e&llocks &asuccessfully loaded!"));
    }

    private void initializeExternals() {
        if (!NBT.preloadApi()) {
            log.sendMessage(MessageUtils.colorize("&cNBT-API wasn't initialized properly, disabling the plugin..."));
            getPluginLoader().disablePlugin(this);
            return;
        }

        MinecraftVersion.disableBStats();

        try {
            Class.forName("org.sqlite.JDBC").getDeclaredConstructor().newInstance();
        } catch (Exception ignored) { }
    }

    @Override
    public void onDisable() {
        StorageService.close();
        HeadService.clearHeadMoves();
        GuiService.clearCache();

        Bukkit.getScheduler().cancelTasks(this);

        log.sendMessage(MessageUtils.colorize("&6&lH&e&lead&6&lB&e&llocks &cdisabled!"));
    }

    public void startInternalTaskTimer() {
        if (this.globalTask != null) {
            try {
                this.globalTask.cancel();
            } catch (IllegalStateException ignored) {
            } // Not scheduled yet
            finally {
                this.globalTask = null;
            }
        }

        this.globalTask = new GlobalTask();

        if (!ConfigService.isHologramsEnabled() && !ConfigService.isParticlesEnabled()) {
            return;
        }

        globalTask.runTaskTimer(this, 0, ConfigService.getDelayGlobalTask());
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

    public HeadDatabaseHook getHeadDatabaseHook() {
        return headDatabaseHook;
    }

    public void setHeadDatabaseHook(HeadDatabaseHook headDatabaseHook) {
        this.headDatabaseHook = headDatabaseHook;
    }
}
