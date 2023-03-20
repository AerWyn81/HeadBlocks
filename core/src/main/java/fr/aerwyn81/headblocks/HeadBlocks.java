package fr.aerwyn81.headblocks;

import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import fr.aerwyn81.headblocks.commands.newCommands.AdminCommands;
import fr.aerwyn81.headblocks.databases.EnumTypeDatabase;
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
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.BukkitCommandHandler;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings("ConstantConditions")
public final class HeadBlocks extends JavaPlugin {

    public static ConsoleCommandSender log;
    private static HeadBlocks instance;
    public static boolean isPlaceholderApiActive;
    public static boolean isReloadInProgress;
    public static boolean isProtocolLibActive;
    public static boolean isDecentHologramsActive;
    public static boolean isHolographicDisplaysActive;

    private GlobalTask globalTask;
    private HeadDatabaseHook headDatabaseHook;

    @Override
    public void onEnable() {
        instance = this;
        log = Bukkit.getConsoleSender();

        log.sendMessage(MessageUtils.colorize("[HeadBlocks] &eInitializing..."));

        MinecraftVersion.disableBStats();

        try {
            Class.forName("org.sqlite.JDBC").getDeclaredConstructor().newInstance();
        } catch (Exception ignored) { }

        log.sendMessage(MessageUtils.colorize("&6&lH&e&lead&6&lB&e&llocks &einitializing..."));

        File configFile = new File(getDataFolder(), "config.yml");

        saveDefaultConfig();
        try {
            ConfigUpdater.update(this, "config.yml", configFile, Arrays.asList("tieredRewards", "heads"));
        } catch (IOException e) {
            log.sendMessage(MessageUtils.colorize("[HeadBlocks] &cCritical error while loading config file: " + e.getMessage() + ". Disabling plugin..."));
            this.setEnabled(false);
            return;
        }
        reloadConfig();

        if (VersionUtils.getCurrent().isOlderThan(VersionUtils.v1_16)) {
            log.sendMessage(MessageUtils.colorize("[HeadBlocks] &c***** --------------------------------------- *****"));
            log.sendMessage(MessageUtils.colorize("[HeadBlocks] &cHeadBlocks version 2 does not support your Minecraft Server version: " + VersionUtils.getCurrentFormatted()));
            log.sendMessage(MessageUtils.colorize("[HeadBlocks] &cIf you are using a version below Minecraft 1.16.5, use the version 1.6 of the plugin"));
            log.sendMessage(MessageUtils.colorize("[HeadBlocks] &cVersion 1.6 will not receive any new features but may receive corrective updates."));
            log.sendMessage(MessageUtils.colorize("[HeadBlocks] &c***** --------------------------------------- *****"));
            this.setEnabled(false);
            return;
        }

        isPlaceholderApiActive = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        if (isPlaceholderApiActive) {
            new PlaceholderHook().register();
        }

        isProtocolLibActive = Bukkit.getPluginManager().isPluginEnabled("ProtocolLib");
        isDecentHologramsActive = Bukkit.getPluginManager().isPluginEnabled("DecentHolograms");
        isHolographicDisplaysActive = Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays");

        ConfigService.initialize(configFile);

        if (ConfigService.isMetricsEnabled()) {
            new Metrics(this, 15495);
        }

        LanguageService.initialize(ConfigService.getLanguage());
        LanguageService.pushMessages();

        StorageService.initialize();
        HeadService.initialize();
        HologramService.load();
        TrackService.initialize();
        GuiService.initialize();
        ConversationService.initialize();

        this.globalTask = new GlobalTask();
        globalTask.runTaskTimer(this, 0, ConfigService.getDelayGlobalTask());

        if (isHeadDatabaseActive()) {
            this.headDatabaseHook = new HeadDatabaseHook();
        }

        //getCommand("headblocks").setExecutor(new HBCommandExecutor());
        BukkitCommandHandler commandHandler = BukkitCommandHandler.create(this);
        commandHandler.getAutoCompleter().registerParameterSuggestions(EnumTypeDatabase.class, (args, sender, command) -> EnumTypeDatabase.toStringList());
        commandHandler.getAutoCompleter().registerSuggestion("giveCommand", (args, sender, command) -> {
            var headCount = HeadService.getHeads().size();

            if (headCount > 1) {
                List<String> c = IntStream.range(1, headCount + 1).boxed()
                        .map(Object::toString)
                        .collect(Collectors.toList());
                c.add(0, "*");

                return SuggestionProvider.of(c).getSuggestions(args, sender, command);
            }

            return SuggestionProvider.EMPTY.getSuggestions(args, sender, command);
        });
        commandHandler.setHelpWriter((command, actor) ->
                String.format(" &8• &e/%s %s &7- &f%s", command.getPath().toRealString(), command.getUsage(), command.getDescription()));
        commandHandler.register(new AdminCommands());
        commandHandler.registerBrigadier();

        Bukkit.getPluginManager().registerEvents(new OnPlayerInteractEvent(), this);
        Bukkit.getPluginManager().registerEvents(new OnPlayerBreakBlockEvent(), this);
        Bukkit.getPluginManager().registerEvents(new OnPlayerPlaceBlockEvent(), this);
        Bukkit.getPluginManager().registerEvents(new OthersEvent(), this);
        Bukkit.getPluginManager().registerEvents(new OnPlayerClickInventoryEvent(), this);

        log.sendMessage(MessageUtils.colorize("[HeadBlocks] &6&lH&e&lead&6&lB&e&llocks &asuccessfully started!"));
    }

    @Override
    public void onDisable() {
        StorageService.close();
        HeadService.clearHeadMoves();
        GuiService.clearCache();

        Bukkit.getScheduler().cancelTasks(this);

        log.sendMessage(MessageUtils.colorize("&6&lH&e&lead&6&lB&e&llocks &cdisabled!"));
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

    public boolean isHeadDatabaseActive() {
        return Bukkit.getPluginManager().isPluginEnabled("HeadDatabase");
    }
}
