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
import fr.aerwyn81.headblocks.runnables.TimedRunTask;
import fr.aerwyn81.headblocks.services.ConfigService;
import fr.aerwyn81.headblocks.utils.bukkit.*;
import fr.aerwyn81.headblocks.utils.config.ConfigUpdater;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedBarChart;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.holoeasy.HoloEasy;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ConstantConditions")
public final class HeadBlocks extends JavaPlugin {

    private static volatile HeadBlocks instance;
    public static volatile boolean isPlaceholderApiActive;
    public static volatile boolean isReloadInProgress;
    public static volatile boolean isHeadDatabaseActive;
    public static volatile boolean isPacketEventsActive;

    private ServiceRegistry serviceRegistry;
    private ConfigService earlyConfigService;
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

        earlyConfigService = new ConfigService(configFile);

        if (earlyConfigService.hologramsEnabled()
                && EnumTypeHologram.getEnumFromText(earlyConfigService.hologramPlugin()) != EnumTypeHologram.DEFAULT) {
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

        File configFile = new File(getDataFolder(), "config.yml");
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

        isPacketEventsActive = Bukkit.getPluginManager().isPluginEnabled("packetevents");

        // --- Create ServiceRegistry (DI wiring) ---
        PluginProvider pluginProvider = new HeadBlocksPluginProvider(this);
        SchedulerAdapter scheduler = new BukkitSchedulerAdapter(this);
        CommandDispatcher commandDispatcher = new BukkitCommandDispatcher();

        this.serviceRegistry = new ServiceRegistry(
                pluginProvider, scheduler, commandDispatcher,
                configFile, locationFile, holoEasyLib, earlyConfigService);

        packetEventsHook.init(serviceRegistry);

        if (isPlaceholderApiActive) {
            new PlaceholderHook(serviceRegistry).register();
        }

        startInternalTaskTimer();

        if (isHeadDatabaseActive) {
            this.headDatabaseHook = new HeadDatabaseHook(serviceRegistry);
            if (!this.headDatabaseHook.init()) {
                isHeadDatabaseActive = false;
            }
        }

        getCommand("headblocks").setExecutor(new HBCommandExecutor(serviceRegistry));

        Bukkit.getPluginManager().registerEvents(new OnPlayerInteractEvent(serviceRegistry), this);
        Bukkit.getPluginManager().registerEvents(new OnPlayerBreakBlockEvent(serviceRegistry), this);
        Bukkit.getPluginManager().registerEvents(new OnPlayerPlaceBlockEvent(serviceRegistry), this);
        Bukkit.getPluginManager().registerEvents(new OthersEvent(serviceRegistry), this);
        Bukkit.getPluginManager().registerEvents(new OnPlayerClickInventoryEvent(serviceRegistry), this);
        Bukkit.getPluginManager().registerEvents(new OnPlayerChatEvent(serviceRegistry), this);
        Bukkit.getPluginManager().registerEvents(new OnPressurePlateEvent(serviceRegistry), this);

        new TimedRunTask(serviceRegistry).runTaskTimer(this, 0, 2);

        if (serviceRegistry.getConfigService().metricsEnabled()) {
            var m = new Metrics(this, 15495);
            m.addCustomChart(new SimplePie("database_type", () -> serviceRegistry.getStorageService().selectedStorageType()));
            m.addCustomChart(new SingleLineChart("heads", () -> serviceRegistry.getHeadService().getChargedHeadLocations().size()));
            m.addCustomChart(new SimplePie("lang", () -> serviceRegistry.getLanguageService().language()));
            m.addCustomChart(new SingleLineChart("hunts", () -> serviceRegistry.getHuntService().getAllHunts().size()));
            m.addCustomChart(new AdvancedBarChart("hunt_behaviors", () -> {
                Map<String, int[]> map = new HashMap<>();
                for (var hunt : serviceRegistry.getHuntService().getAllHunts()) {
                    for (var behavior : hunt.getBehaviors()) {
                        String name = behavior.getClass().getSimpleName().replace("Behavior", "");
                        map.merge(name, new int[]{1}, (a, b) -> new int[]{a[0] + b[0]});
                    }
                }
                return map;
            }));
            m.addCustomChart(new AdvancedBarChart("features", () -> {
                var heads = serviceRegistry.getHeadService().getChargedHeadLocations();
                Map<String, int[]> map = new HashMap<>();

                if (heads.stream().anyMatch(h -> h.getOrderIndex() != -1)) {
                    map.put("Order", new int[]{1});
                }
                if (heads.stream().anyMatch(HeadLocation::isHintSoundEnabled)) {
                    map.put("Hint sound", new int[]{1});
                }
                if (heads.stream().anyMatch(HeadLocation::isHintActionBarEnabled)) {
                    map.put("Hint action bar", new int[]{1});
                }
                if (heads.stream().anyMatch(h -> !h.getRewards().isEmpty())) {
                    map.put("Hint rewards", new int[]{1});
                }
                if (serviceRegistry.getConfigService().isHideFoundHeads()) {
                    map.put("Hide heads", new int[]{1});
                }

                return map;
            }));
        }

        LogUtil.info("HeadBlocks successfully loaded!");
    }

    private void initializeExternals() {
        NBT.preloadApi();

        MinecraftVersion.disableBStats();
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);

        if (serviceRegistry != null) {
            serviceRegistry.shutdown();
        }

        packetEventsHook.unload();

        if (holoEasyLib != null) {
            //noinspection UnstableApiUsage
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

        this.globalTask = new GlobalTask(serviceRegistry);

        var configSvc = serviceRegistry.getConfigService();
        if (!configSvc.hologramsEnabled() && !configSvc.particlesEnabled()) {
            return;
        }

        globalTask.runTaskTimer(this, 0, configSvc.delayGlobalTask());
    }

    public static HeadBlocks getInstance() {
        return instance;
    }

    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
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
