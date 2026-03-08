package fr.aerwyn81.headblocks;

import fr.aerwyn81.headblocks.databases.Requests;
import fr.aerwyn81.headblocks.services.*;
import fr.aerwyn81.headblocks.utils.bukkit.CommandDispatcher;
import fr.aerwyn81.headblocks.utils.bukkit.PluginProvider;
import fr.aerwyn81.headblocks.utils.bukkit.SchedulerAdapter;
import org.holoeasy.HoloEasy;

import java.io.File;
import java.util.Objects;

public class ServiceRegistry {
    private final PluginProvider pluginProvider;
    private final SchedulerAdapter scheduler;
    private final CommandDispatcher commandDispatcher;
    private final HoloEasy holoEasyLib;

    private ConfigService configService;
    private LanguageService languageService;
    private StorageService storageService;
    private HuntConfigService huntConfigService;
    private HuntService huntService;
    private PlaceholdersService placeholdersService;
    private HeadService headService;
    private RewardService rewardService;
    private HologramService hologramService;
    private GuiService guiService;

    private final File configFile;
    private final File locationFile;
    private final ConfigService existingConfigService;

    public ServiceRegistry(PluginProvider pluginProvider, SchedulerAdapter scheduler,
                           CommandDispatcher commandDispatcher, File configFile, File locationFile,
                           HoloEasy holoEasyLib) {
        this(pluginProvider, scheduler, commandDispatcher, configFile, locationFile, holoEasyLib, null);
    }

    public ServiceRegistry(PluginProvider pluginProvider, SchedulerAdapter scheduler,
                           CommandDispatcher commandDispatcher, File configFile, File locationFile,
                           HoloEasy holoEasyLib, ConfigService existingConfigService) {
        this.pluginProvider = pluginProvider;
        this.scheduler = scheduler;
        this.commandDispatcher = commandDispatcher;
        this.configFile = configFile;
        this.locationFile = locationFile;
        this.holoEasyLib = holoEasyLib;
        this.existingConfigService = existingConfigService;

        initializeAll();
    }

    private void initializeAll() {
        this.configService = Objects.requireNonNullElseGet(existingConfigService, () -> new ConfigService(configFile));

        this.languageService = new LanguageService(pluginProvider, configService);
        languageService.setLang(configService.language());
        languageService.pushMessages();

        this.huntConfigService = new HuntConfigService(pluginProvider, configService, this, scheduler);
        huntConfigService.initialize();

        Requests.init(configService);

        this.storageService = new StorageService(configService, pluginProvider.getDataFolder());

        // Migrate locations.yml → default hunt YAML (idempotent, runs once)
        huntConfigService.migrateLocationsFromLegacy(locationFile);

        this.huntService = new HuntService(configService, huntConfigService, storageService);

        this.placeholdersService = new PlaceholdersService(storageService, configService, languageService, pluginProvider, huntService);

        this.headService = new HeadService(configService, storageService, languageService, scheduler, pluginProvider);
        headService.setHuntService(huntService);
        headService.setHuntConfigService(huntConfigService);
        headService.initialize();

        this.rewardService = new RewardService(configService, placeholdersService, scheduler, commandDispatcher);

        this.hologramService = new HologramService(configService, huntService, pluginProvider, holoEasyLib);

        this.headService.setHologramService(hologramService);
        this.hologramService.setHeadService(headService);
        this.hologramService.setServiceRegistry(this);

        hologramService.load();

        this.guiService = new GuiService(configService, languageService, huntService, headService, pluginProvider, this);
    }

    public void reload() {
        // Shutdown what needs shutting
        guiService.clearCache();
        hologramService.unload();
        storageService.close();

        // Reload config
        configService.reloadConfig();
        Requests.init(configService);

        // Reinitialize language
        languageService.setLang(configService.language());
        languageService.pushMessages();

        // Reinitialize storage, hunts, heads
        storageService.initialize();
        huntConfigService.initialize();
        huntService.initialize();
        headService.load();

        hologramService.load();
    }

    public void shutdown() {
        hologramService.unload();
        storageService.close();
        headService.clearHeadMoves();
        guiService.clearCache();
        TimedRunManager.clearAll();
    }

    // --- Getters ---

    public PluginProvider getPluginProvider() {
        return pluginProvider;
    }

    public SchedulerAdapter getScheduler() {
        return scheduler;
    }

    public CommandDispatcher getCommandDispatcher() {
        return commandDispatcher;
    }

    public ConfigService getConfigService() {
        return configService;
    }

    public LanguageService getLanguageService() {
        return languageService;
    }

    public StorageService getStorageService() {
        return storageService;
    }

    public HuntConfigService getHuntConfigService() {
        return huntConfigService;
    }

    public HuntService getHuntService() {
        return huntService;
    }

    public PlaceholdersService getPlaceholdersService() {
        return placeholdersService;
    }

    public HeadService getHeadService() {
        return headService;
    }

    public RewardService getRewardService() {
        return rewardService;
    }

    public HologramService getHologramService() {
        return hologramService;
    }

    public GuiService getGuiService() {
        return guiService;
    }

    public HoloEasy getHoloEasyLib() {
        return holoEasyLib;
    }
}
