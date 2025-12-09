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
import com.tcoded.folialib.FoliaLib;
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
    private FoliaLib foliaLib;

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

        // Initialize FoliaLib - required for Folia support
        // FoliaLib automatically detects platform (Folia/Paper/Spigot) and uses appropriate scheduler
        try {
            foliaLib = new FoliaLib(this);
        } catch (Exception e) {
            LogUtil.error("CRITICAL: Failed to initialize FoliaLib: {0}", e.getMessage());
            LogUtil.error("Plugin cannot function without FoliaLib. Disabling...");
            getPluginLoader().disablePlugin(this);
            return;
        }

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

        // Compatibility warnings for external dependencies on Folia
        if (isPacketEventsActive && foliaLib.isFolia()) {
            // TODO: Verify PacketEvents Folia compatibility
            // If issues occur, may need to disable head hiding feature on Folia
            LogUtil.warning("PacketEvents detected on Folia - head hiding feature may have compatibility issues");
        }

        if (holoEasyLib != null && foliaLib.isFolia()) {
            // TODO: Verify HoloEasy Folia compatibility
            // If issues occur, may need to fallback to DEFAULT hologram type
            LogUtil.warning("HoloEasy detected on Folia - advanced holograms may have compatibility issues");
        }

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

        try {
            Class.forName("org.sqlite.JDBC").getDeclaredConstructor().newInstance();
        } catch (Exception | NoClassDefFoundError ignored) {
        }
    }

    @Override
    public void onDisable() {
        StorageService.close();
        HeadService.clearHeadMoves();
        GuiService.clearCache();

        if (foliaLib != null) {
            foliaLib.getScheduler().cancelAllTasks();
        } else {
            Bukkit.getScheduler().cancelTasks(this);
        }

        packetEventsHook.unload();

        if (holoEasyLib != null) {
            holoEasyLib.destroyPools();
        }

        LogUtil.info("HeadBlocks disabled!");
    }

    public void startInternalTaskTimer() {
        // Cancel all existing tasks (including previous global task iterations)
        if (foliaLib != null) {
            foliaLib.getScheduler().cancelAllTasks();
        }

        this.globalTask = new GlobalTask();

        if (!ConfigService.isHologramsEnabled() && !ConfigService.isParticlesEnabled()) {
            return;
        }

        // Use FoliaLib timer for global iteration, then schedule per-location tasks
        // This ensures each head's operations run on the correct region thread in Folia
        foliaLib.getScheduler().runTimer(() -> {
            if (HeadBlocks.isReloadInProgress)
                return;

            HeadService.getChargedHeadLocations().forEach(headLocation -> {
                var location = headLocation.getLocation();
                if (location.getWorld() == null || 
                    !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                    return;
                }

                // Schedule region-aware task for each location
                // On Folia: runs on the region thread for that location
                // On Paper/Spigot: runs on main thread (same behavior as before)
                foliaLib.getScheduler().runAtLocation(location, task -> {
                    GlobalTask.handleHeadLocation(headLocation);
                });
            });
        }, 0, ConfigService.getDelayGlobalTask());
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

    public FoliaLib getFoliaLib() {
        return foliaLib;
    }
}
