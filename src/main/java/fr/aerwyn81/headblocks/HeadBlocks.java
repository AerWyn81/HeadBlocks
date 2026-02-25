package fr.aerwyn81.headblocks;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import fr.aerwyn81.headblocks.commands.HBCommandExecutor;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.events.*;
import fr.aerwyn81.headblocks.holograms.EnumTypeHologram;
import fr.aerwyn81.headblocks.hooks.HeadDatabaseHook;
import fr.aerwyn81.headblocks.hooks.PacketEventsHook;
import fr.aerwyn81.headblocks.hooks.PlaceholderHook;
import fr.aerwyn81.headblocks.runnables.GlobalTask;
import fr.aerwyn81.headblocks.services.*;
import fr.aerwyn81.headblocks.utils.bukkit.VersionUtils;
import fr.aerwyn81.headblocks.utils.config.ConfigUpdater;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.utils.internal.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.holoeasy.HoloEasy;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

@SuppressWarnings("ConstantConditions")
public final class HeadBlocks extends JavaPlugin {

    private static HeadBlocks instance;
    public static boolean isPlaceholderApiActive;
    public static boolean isReloadInProgress;
    public static boolean isHeadDatabaseActive;
    public static boolean isPacketEventsActive;

    private GlobalTask globalTask;
    private HeadDatabaseHook headDatabaseHook;
    private PacketEventsHook packetEventsHook;

    private HoloEasy holoEasyLib;

    @Override
    public void onLoad() {
        LogUtil.initialize(getLogger());

        packetEventsHook = new PacketEventsHook();
        var isPacketEventsLoaded = packetEventsHook.load(this);

        File configFile = new File(getDataFolder(), "config.yml");
        saveDefaultConfig();
        try {
            ConfigUpdater.update(this, "config.yml", configFile, Arrays.asList("tieredRewards", "heads", "headsTheme"));
        } catch (IOException e) {
            LogUtil.error("Error while loading config file: {0}", e.getMessage());
            getPluginLoader().disablePlugin(this);
            return;
        }
        reloadConfig();

        ConfigService.initialize(configFile);

        if (ConfigService.isHologramsEnabled()
                && HologramService.getHologramTypeFromConfig() != EnumTypeHologram.DEFAULT) {
            if (isPacketEventsLoaded) {
                holoEasyLib = new HoloEasy(this);
            } else {
                LogUtil.error("Error while loading Holograms: PacketEvents is not loaded.");
            }
        }
    }

    @Override
    public void onEnable() {
        instance = this;

        initializeExternals();

        LogUtil.info("HeadBlocks initializing...");

        File locationFile = new File(getDataFolder(), "locations.yml");

        if (!VersionUtils.isAtLeastVersion(VersionUtils.v1_20_R1)) {
            LogUtil.error("***** --------------------------------------- *****");
            LogUtil.error("HeadBlocks does not support your Minecraft Server version: {0}", VersionUtils.getVersion());
            LogUtil.error("***** --------------------------------------- *****");
            getPluginLoader().disablePlugin(this);
            return;
        }

        isHeadDatabaseActive = Bukkit.getPluginManager().isPluginEnabled("HeadDatabase");

        isPlaceholderApiActive = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        if (isPlaceholderApiActive) {
            new PlaceholderHook().register();
        }

        isPacketEventsActive = Bukkit.getPluginManager().isPluginEnabled("packetevents");

        LanguageService.initialize(ConfigService.getLanguage());
        LanguageService.pushMessages();

        StorageService.initialize();
        HeadService.initialize(locationFile);
        HologramService.load();

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
        Bukkit.getPluginManager().registerEvents(new OnPlayerChatEvent(), this);

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
            m.addCustomChart(new Metrics.SimplePie("feat_hint_sound", () -> {
                var anyHit = HeadService.getChargedHeadLocations().stream().anyMatch(HeadLocation::isHintSoundEnabled);
                return anyHit ? "True" : "False";
            }));
            m.addCustomChart(new Metrics.SimplePie("feat_hint_actionBarMessage", () -> {
                var anyHit = HeadService.getChargedHeadLocations().stream().anyMatch(HeadLocation::isHintActionBarEnabled);
                return anyHit ? "True" : "False";
            }));
            m.addCustomChart(new Metrics.SimplePie("feat_hint_rewards", () -> {
                var anyHit = HeadService.getChargedHeadLocations().stream().anyMatch(h -> !h.getRewards().isEmpty());
                return anyHit ? "True" : "False";
            }));
            m.addCustomChart(new Metrics.SimplePie("feat_hide_heads", () -> ConfigService.hideFoundHeads() ? "True" : "False"));
        }

        LogUtil.info("HeadBlocks successfully loaded!");
    }

    private void initializeExternals() {
        packetEventsHook.init();

        NBT.preloadApi();

        MinecraftVersion.disableBStats();
    }

    @Override
    public void onDisable() {
        StorageService.close();
        HeadService.clearHeadMoves();
        GuiService.clearCache();

        Bukkit.getScheduler().cancelTasks(this);

        packetEventsHook.unload();

        if (holoEasyLib != null) {
            holoEasyLib.destroyPools();
        }

        LogUtil.info("HeadBlocks disabled!");
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

    public HeadDatabaseHook getHeadDatabaseHook() {
        return headDatabaseHook;
    }

    public void setHeadDatabaseHook(HeadDatabaseHook headDatabaseHook) {
        this.headDatabaseHook = headDatabaseHook;
    }

    public HoloEasy getHoloEasyLib() {
        return holoEasyLib;
    }

    public PacketEventsHook getPacketEventsHook() {
        return packetEventsHook;
    }
}
