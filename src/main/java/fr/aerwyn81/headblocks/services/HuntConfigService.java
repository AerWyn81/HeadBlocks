package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.TieredReward;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntConfig;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.data.hunt.behavior.Behavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.ScheduledBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.TimedBehavior;
import fr.aerwyn81.headblocks.utils.bukkit.PluginProvider;
import fr.aerwyn81.headblocks.utils.bukkit.SchedulerAdapter;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class HuntConfigService {
    private final PluginProvider pluginProvider;
    private final ConfigService configService;
    private final ServiceRegistry registry;
    private final SchedulerAdapter scheduler;
    private File huntsDir;

    private final Map<String, YamlConfiguration> yamlCache = new HashMap<>();
    private final Set<String> savePendingHunts = new HashSet<>();

    // --- Constructor ---

    public HuntConfigService(PluginProvider pluginProvider, ConfigService configService,
                             ServiceRegistry registry, SchedulerAdapter scheduler) {
        this.pluginProvider = pluginProvider;
        this.configService = configService;
        this.registry = registry;
        this.scheduler = scheduler;

        initialize();
    }

    // --- Instance methods ---

    public void initialize() {
        huntsDir = new File(pluginProvider.getDataFolder(), "hunts");
        if (!huntsDir.exists() && !huntsDir.mkdirs()) {
            LogUtil.error("Failed to create hunts directory: {0}", huntsDir.getAbsolutePath());
        }

        if (!huntFileExists("default")) {
            generateDefaultFromConfig();
        }
    }

    public List<HBHunt> loadHunts() {
        List<HBHunt> hunts = new ArrayList<>();
        File[] files = huntsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return hunts;
        }

        for (File file : files) {
            try {
                HBHunt hunt = loadHunt(file);
                if (hunt != null) {
                    hunts.add(hunt);
                }
            } catch (Exception e) {
                LogUtil.error("Failed to load hunt file {0}: {1}", file.getName(), e.getMessage());
            }
        }

        return hunts;
    }

    public HBHunt loadHunt(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        String id = yaml.getString("id");
        if (id == null || id.isEmpty()) {
            LogUtil.warning("Hunt file {0} has no 'id' field, skipping.", file.getName());
            return null;
        }

        String displayName = yaml.getString("displayName", id);
        HuntState state = HuntState.of(yaml.getString("state", "ACTIVE"));
        int priority = yaml.getInt("priority", 1);
        String icon = yaml.getString("icon", "CHEST_MINECART");

        HBHunt hunt = new HBHunt(configService, id, displayName, state, priority, icon);

        List<Behavior> behaviors = loadBehaviors(yaml);
        hunt.setBehaviors(behaviors);

        HuntConfig huntConfig = loadHuntConfig(yaml);
        hunt.setConfig(huntConfig);

        return hunt;
    }

    public void saveHunt(HBHunt hunt) {
        File file = new File(huntsDir, hunt.getId() + ".yml");
        YamlConfiguration yaml;

        // Load existing file to preserve locations section
        if (file.exists()) {
            yaml = YamlConfiguration.loadConfiguration(file);
        } else {
            yaml = new YamlConfiguration();
        }

        yaml.set("id", hunt.getId());
        yaml.set("displayName", hunt.getDisplayName());
        yaml.set("state", hunt.getState().name());
        yaml.set("priority", hunt.getPriority());
        yaml.set("icon", hunt.getIcon());

        saveBehaviors(yaml, hunt.getBehaviors());
        saveHuntConfig(yaml, hunt.getConfig());

        saveYamlFile(yaml, file);
    }

    public boolean huntFileExists(String huntId) {
        return new File(huntsDir, huntId + ".yml").exists();
    }

    public void deleteHuntFile(String huntId) {
        File file = new File(huntsDir, huntId + ".yml");
        if (file.exists()) {
            if (!file.delete()) {
                LogUtil.error("Failed to delete hunt file {0}", file.getName());
            }
        }
    }

    public void migrateLocationsFromLegacy(File locationFile) {
        if (locationFile == null || !locationFile.exists()) {
            return;
        }

        LogUtil.info("Migrating locations.yml to default hunt YAML file...");

        YamlConfiguration legacyYaml = YamlConfiguration.loadConfiguration(locationFile);
        var locationsSection = legacyYaml.getConfigurationSection("locations");
        if (locationsSection == null) {
            LogUtil.info("No locations found in locations.yml, skipping migration.");
            renameLegacyFile(locationFile);
            return;
        }

        File huntFile = new File(huntsDir, "default.yml");
        YamlConfiguration huntYaml = YamlConfiguration.loadConfiguration(huntFile);

        int migrated = 0;
        for (String uuidStr : locationsSection.getKeys(false)) {
            try {
                UUID headUuid = UUID.fromString(uuidStr);
                var headLocation = HeadLocation.fromConfig(legacyYaml, headUuid, "default");
                headLocation.saveInConfig(huntYaml);
                migrated++;
            } catch (Exception e) {
                LogUtil.error("Failed to migrate head {0}: {1}", uuidStr, e.getMessage());
            }
        }

        try {
            huntYaml.save(huntFile);
        } catch (IOException e) {
            LogUtil.error("Failed to save default hunt file after migration: {0}", e.getMessage());
        }

        renameLegacyFile(locationFile);
        invalidateAllYamlCaches();
        LogUtil.info("Migration complete: {0} head(s) migrated to default hunt.", migrated);
    }

    private void renameLegacyFile(File locationFile) {
        File migrated = new File(locationFile.getParent(), "locations.yml.migrated");
        if (!locationFile.renameTo(migrated)) {
            LogUtil.error("Failed to rename locations.yml to locations.yml.migrated");
        }
    }

    public void generateDefaultFromConfig() {
        File dir = new File(pluginProvider.getDataFolder(), "hunts");
        if (!dir.exists() && !dir.mkdirs()) {
            LogUtil.error("Failed to create hunts directory: {0}", dir.getAbsolutePath());
        }

        File defaultFile = new File(dir, "default.yml");
        if (defaultFile.exists()) {
            return;
        }

        LogUtil.info("Generating hunts/default.yml from existing config.yml...");

        YamlConfiguration yaml = new YamlConfiguration();
        String p = "config.";

        yaml.set("id", "default");
        yaml.set("displayName", "Default");
        yaml.set("state", "ACTIVE");
        yaml.set("priority", 0);
        yaml.set("icon", "CHEST_MINECART");

        yaml.createSection("behaviors.free");

        List<String> messages = configService.headClickMessages();
        if (!messages.isEmpty()) {
            yaml.set(p + "headClick.messages", messages);
        }

        yaml.set(p + "headClick.title.enabled", configService.headClickTitleEnabled());
        String titleFirst = configService.headClickTitleFirstLine();
        if (!titleFirst.isEmpty()) {
            yaml.set(p + "headClick.title.firstLine", titleFirst);
        }
        String titleSub = configService.headClickTitleSubTitle();
        if (!titleSub.isEmpty()) {
            yaml.set(p + "headClick.title.subTitle", titleSub);
        }
        yaml.set(p + "headClick.title.fadeIn", configService.headClickTitleFadeIn());
        yaml.set(p + "headClick.title.stay", configService.headClickTitleStay());
        yaml.set(p + "headClick.title.fadeOut", configService.headClickTitleFadeOut());

        String soundFound = configService.headClickNotOwnSound();
        if (soundFound != null) {
            yaml.set(p + "headClick.sound.found", soundFound);
        }
        String soundOwn = configService.headClickAlreadyOwnSound();
        if (soundOwn != null) {
            yaml.set(p + "headClick.sound.alreadyOwn", soundOwn);
        }

        yaml.set(p + "headClick.firework.enabled", configService.fireworkEnabled());

        List<String> commands = configService.headClickCommands();
        if (!commands.isEmpty()) {
            yaml.set(p + "headClick.commands", commands);
        }

        yaml.set(p + "headClick.eject.enabled", configService.headClickEjectEnabled());
        yaml.set(p + "headClick.eject.power", configService.headClickEjectPower());

        yaml.set(p + "holograms.found.enabled", configService.hologramsFoundEnabled());
        yaml.set(p + "holograms.notFound.enabled", configService.hologramsNotFoundEnabled());
        ArrayList<String> foundLines = configService.hologramsFoundLines();
        if (!foundLines.isEmpty()) {
            yaml.set(p + "holograms.found.lines", foundLines);
        }
        ArrayList<String> notFoundLines = configService.hologramsNotFoundLines();
        if (!notFoundLines.isEmpty()) {
            yaml.set(p + "holograms.notFound.lines", notFoundLines);
        }

        yaml.set(p + "hints.distance", configService.hintDistanceBlocks());
        yaml.set(p + "hints.frequency", configService.hintFrequency());

        yaml.set(p + "spin.enabled", configService.spinEnabled());
        yaml.set(p + "spin.speed", configService.spinSpeed());
        yaml.set(p + "spin.linked", configService.spinLinked());

        yaml.set(p + "particles.found.enabled", configService.particlesFoundEnabled());
        yaml.set(p + "particles.found.type", configService.particlesFoundType());
        yaml.set(p + "particles.found.amount", configService.particlesFoundAmount());
        yaml.set(p + "particles.notFound.enabled", configService.particlesNotFoundEnabled());
        yaml.set(p + "particles.notFound.type", configService.particlesNotFoundType());
        yaml.set(p + "particles.notFound.amount", configService.particlesNotFoundAmount());

        List<TieredReward> tieredRewards = configService.tieredRewards();
        if (!tieredRewards.isEmpty()) {
            for (TieredReward reward : tieredRewards) {
                String key = p + "tieredRewards." + reward.level();
                if (!reward.messages().isEmpty()) {
                    yaml.set(key + ".messages", reward.messages());
                }
                if (!reward.commands().isEmpty()) {
                    yaml.set(key + ".commands", reward.commands());
                }
                if (!reward.broadcastMessages().isEmpty()) {
                    yaml.set(key + ".broadcast", reward.broadcastMessages());
                }
                if (reward.slotsRequired() != -1) {
                    yaml.set(key + ".slotsRequired", reward.slotsRequired());
                }
                if (reward.isRandom()) {
                    yaml.set(key + ".randomizeCommands", true);
                }
            }
        }

        try {
            yaml.save(defaultFile);
            LogUtil.info("hunts/default.yml generated successfully.");
        } catch (IOException e) {
            LogUtil.error("Failed to generate hunts/default.yml: {0}", e.getMessage());
        }
    }

    // --- Location management ---

    public List<HeadLocation> loadLocationsFromHunt(String huntId) {
        List<HeadLocation> locations = new ArrayList<>();
        YamlConfiguration yaml = getOrLoadHuntYaml(huntId);
        if (yaml == null) {
            return locations;
        }

        ConfigurationSection section = yaml.getConfigurationSection("locations");
        if (section == null) {
            return locations;
        }

        for (String uuid : section.getKeys(false)) {
            try {
                UUID headUuid = UUID.fromString(uuid);
                HeadLocation headLoc = HeadLocation.fromConfig(yaml, headUuid, huntId);
                locations.add(headLoc);
            } catch (Exception e) {
                LogUtil.error("Cannot deserialize location {0} in hunt {1}: {2}", uuid, huntId, e.getMessage());
            }
        }

        return locations;
    }

    public void saveLocationInHunt(String huntId, HeadLocation headLocation) {
        YamlConfiguration yaml = getOrLoadHuntYaml(huntId);
        if (yaml == null) {
            LogUtil.error("Cannot save location in hunt {0}: hunt file not found.", huntId);
            return;
        }

        headLocation.saveInConfig(yaml);
        debouncedSave(huntId, yaml);
    }

    public void removeLocationFromHunt(String huntId, UUID headUuid) {
        YamlConfiguration yaml = getOrLoadHuntYaml(huntId);
        if (yaml == null) {
            return;
        }

        yaml.set("locations." + headUuid, null);
        debouncedSave(huntId, yaml);
    }

    private YamlConfiguration getOrLoadHuntYaml(String huntId) {
        return yamlCache.computeIfAbsent(huntId, id -> {
            File file = new File(huntsDir, id + ".yml");
            if (!file.exists()) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(file);
        });
    }

    public void invalidateAllYamlCaches() {
        yamlCache.clear();
    }

    private void debouncedSave(String huntId, YamlConfiguration yaml) {
        if (savePendingHunts.contains(huntId)) {
            return;
        }
        savePendingHunts.add(huntId);

        scheduler.runTaskLater(() -> {
            savePendingHunts.remove(huntId);
            String content = yaml.saveToString();
            File file = new File(huntsDir, huntId + ".yml");
            scheduler.runTaskAsync(() -> {
                try {
                    Files.writeString(file.toPath(), content);
                } catch (Exception e) {
                    LogUtil.error("Cannot save hunt file {0}: {1}", file.getName(), e.getMessage());
                }
            });
        }, 1L);
    }

    private void saveYamlFile(YamlConfiguration yaml, File file) {
        try {
            yaml.save(file);
        } catch (IOException e) {
            LogUtil.error("Failed to save hunt file {0}: {1}", file.getName(), e.getMessage());
        }
    }

    // --- Private helpers ---

    private List<Behavior> loadBehaviors(YamlConfiguration yaml) {
        List<Behavior> behaviors = new ArrayList<>();

        ConfigurationSection section = yaml.getConfigurationSection("behaviors");
        if (section == null) {
            return behaviors;
        }

        for (String type : section.getKeys(false)) {
            ConfigurationSection behaviorSection = section.getConfigurationSection(type);
            behaviors.add(Behavior.fromConfig(type, registry, behaviorSection));
        }

        return behaviors;
    }

    private void saveBehaviors(YamlConfiguration yaml, List<Behavior> behaviors) {
        for (Behavior behavior : behaviors) {
            String key = "behaviors." + behavior.getId();
            yaml.createSection(key);

            if (behavior instanceof ScheduledBehavior sb) {
                var section = yaml.getConfigurationSection(key);
                if (section != null) {
                    section.set("mode", sb.getScheduleMode().getModeId());
                    sb.getScheduleMode().saveTo(section);
                }
            }

            if (behavior instanceof TimedBehavior tb) {
                yaml.set(key + ".repeatable", tb.repeatable());
                if (tb.startPlateLocation() != null) {
                    var loc = tb.startPlateLocation();
                    if (loc.getWorld() != null) {
                        yaml.set(key + ".startPlate.world", loc.getWorld().getName());
                        yaml.set(key + ".startPlate.x", loc.getBlockX());
                        yaml.set(key + ".startPlate.y", loc.getBlockY());
                        yaml.set(key + ".startPlate.z", loc.getBlockZ());
                    }
                }
            }
        }
    }

    private HuntConfig loadHuntConfig(YamlConfiguration yaml) {
        HuntConfig hc = new HuntConfig(configService);
        String p = "config.";

        if (yaml.contains(p + "headClick.messages")) {
            hc.setHeadClickMessages(yaml.getStringList(p + "headClick.messages"));
        }
        if (yaml.contains(p + "headClick.title.enabled")) {
            hc.setHeadClickTitleEnabled(yaml.getBoolean(p + "headClick.title.enabled"));
        }
        if (yaml.contains(p + "headClick.title.firstLine")) {
            hc.setHeadClickTitleFirstLine(yaml.getString(p + "headClick.title.firstLine"));
        }
        if (yaml.contains(p + "headClick.title.subTitle")) {
            hc.setHeadClickTitleSubTitle(yaml.getString(p + "headClick.title.subTitle"));
        }
        if (yaml.contains(p + "headClick.title.fadeIn")) {
            hc.setHeadClickTitleFadeIn(yaml.getInt(p + "headClick.title.fadeIn"));
        }
        if (yaml.contains(p + "headClick.title.stay")) {
            hc.setHeadClickTitleStay(yaml.getInt(p + "headClick.title.stay"));
        }
        if (yaml.contains(p + "headClick.title.fadeOut")) {
            hc.setHeadClickTitleFadeOut(yaml.getInt(p + "headClick.title.fadeOut"));
        }
        if (yaml.contains(p + "headClick.sound.found")) {
            hc.setHeadClickSoundFound(yaml.getString(p + "headClick.sound.found"));
        }
        if (yaml.contains(p + "headClick.sound.alreadyOwn")) {
            hc.setHeadClickSoundAlreadyOwn(yaml.getString(p + "headClick.sound.alreadyOwn"));
        }
        if (yaml.contains(p + "headClick.firework.enabled")) {
            hc.setFireworkEnabled(yaml.getBoolean(p + "headClick.firework.enabled"));
        }
        if (yaml.contains(p + "headClick.commands")) {
            hc.setHeadClickCommands(yaml.getStringList(p + "headClick.commands"));
        }
        if (yaml.contains(p + "headClick.eject.enabled")) {
            hc.setHeadClickEjectEnabled(yaml.getBoolean(p + "headClick.eject.enabled"));
        }
        if (yaml.contains(p + "headClick.eject.power")) {
            hc.setHeadClickEjectPower(yaml.getDouble(p + "headClick.eject.power"));
        }

        if (yaml.contains(p + "holograms.enabled")) {
            hc.setHologramsEnabled(yaml.getBoolean(p + "holograms.enabled"));
        }
        if (yaml.contains(p + "holograms.found.enabled")) {
            hc.setHologramsFoundEnabled(yaml.getBoolean(p + "holograms.found.enabled"));
        }
        if (yaml.contains(p + "holograms.notFound.enabled")) {
            hc.setHologramsNotFoundEnabled(yaml.getBoolean(p + "holograms.notFound.enabled"));
        }
        if (yaml.contains(p + "holograms.found.lines")) {
            hc.setHologramsFoundLines(new ArrayList<>(yaml.getStringList(p + "holograms.found.lines")));
        }
        if (yaml.contains(p + "holograms.notFound.lines")) {
            hc.setHologramsNotFoundLines(new ArrayList<>(yaml.getStringList(p + "holograms.notFound.lines")));
        }

        if (yaml.contains(p + "hints.enabled")) {
            hc.setHintsEnabled(yaml.getBoolean(p + "hints.enabled"));
        }
        if (yaml.contains(p + "hints.distance")) {
            hc.setHintDistance(yaml.getInt(p + "hints.distance"));
        }
        if (yaml.contains(p + "hints.frequency")) {
            hc.setHintFrequency(yaml.getInt(p + "hints.frequency"));
        }

        if (yaml.contains(p + "spin.enabled")) {
            hc.setSpinEnabled(yaml.getBoolean(p + "spin.enabled"));
        }
        if (yaml.contains(p + "spin.speed")) {
            hc.setSpinSpeed(yaml.getInt(p + "spin.speed"));
        }
        if (yaml.contains(p + "spin.linked")) {
            hc.setSpinLinked(yaml.getBoolean(p + "spin.linked"));
        }

        if (yaml.contains(p + "particles.found.enabled")) {
            hc.setParticlesFoundEnabled(yaml.getBoolean(p + "particles.found.enabled"));
        }
        if (yaml.contains(p + "particles.notFound.enabled")) {
            hc.setParticlesNotFoundEnabled(yaml.getBoolean(p + "particles.notFound.enabled"));
        }
        if (yaml.contains(p + "particles.found.type")) {
            hc.setParticlesFoundType(yaml.getString(p + "particles.found.type"));
        }
        if (yaml.contains(p + "particles.found.amount")) {
            hc.setParticlesFoundAmount(yaml.getInt(p + "particles.found.amount"));
        }
        if (yaml.contains(p + "particles.notFound.type")) {
            hc.setParticlesNotFoundType(yaml.getString(p + "particles.notFound.type"));
        }
        if (yaml.contains(p + "particles.notFound.amount")) {
            hc.setParticlesNotFoundAmount(yaml.getInt(p + "particles.notFound.amount"));
        }

        loadTieredRewards(yaml, hc, p);

        return hc;
    }

    private void loadTieredRewards(YamlConfiguration yaml, HuntConfig hc, String prefix) {
        ConfigurationSection section = yaml.getConfigurationSection(prefix + "tieredRewards");
        if (section == null) {
            return;
        }

        List<TieredReward> rewards = new ArrayList<>();
        for (String level : section.getKeys(false)) {
            try {
                List<String> messages = new ArrayList<>();
                if (section.contains(level + ".messages")) {
                    messages = section.getStringList(level + ".messages");
                }

                List<String> commands = new ArrayList<>();
                if (section.contains(level + ".commands")) {
                    commands = section.getStringList(level + ".commands");
                }

                List<String> broadcast = new ArrayList<>();
                if (section.contains(level + ".broadcast")) {
                    broadcast = section.getStringList(level + ".broadcast");
                }

                int slotsRequired = section.getInt(level + ".slotsRequired", -1);
                boolean isRandom = section.getBoolean(level + ".randomizeCommands", false);

                if (!messages.isEmpty() || !commands.isEmpty() || !broadcast.isEmpty() || slotsRequired != -1) {
                    rewards.add(new TieredReward(Integer.parseInt(level), messages, commands, broadcast, slotsRequired, isRandom));
                }
            } catch (Exception ex) {
                LogUtil.error("Cannot read tiered reward level \"{0}\": {1}", level, ex.getMessage());
            }
        }

        if (!rewards.isEmpty()) {
            hc.setTieredRewards(rewards);
        }
    }

    private void saveHuntConfig(YamlConfiguration yaml, HuntConfig hc) {
        String p = "config.";

        if (hc.hasHeadClickMessages()) {
            yaml.set(p + "headClick.messages", hc.getHeadClickMessages());
        }

        if (hc.hasHologramsFoundLines()) {
            yaml.set(p + "holograms.found.lines", hc.getHologramsFoundLines());
        }
        if (hc.hasHologramsNotFoundLines()) {
            yaml.set(p + "holograms.notFound.lines", hc.getHologramsNotFoundLines());
        }

        if (hc.hasTieredRewards()) {
            saveTieredRewards(yaml, hc.getTieredRewards(), p);
        }
    }

    private void saveTieredRewards(YamlConfiguration yaml, List<TieredReward> rewards, String prefix) {
        for (TieredReward reward : rewards) {
            String key = prefix + "tieredRewards." + reward.level();
            if (!reward.messages().isEmpty()) {
                yaml.set(key + ".messages", reward.messages());
            }
            if (!reward.commands().isEmpty()) {
                yaml.set(key + ".commands", reward.commands());
            }
            if (!reward.broadcastMessages().isEmpty()) {
                yaml.set(key + ".broadcast", reward.broadcastMessages());
            }
            if (reward.slotsRequired() != -1) {
                yaml.set(key + ".slotsRequired", reward.slotsRequired());
            }
            if (reward.isRandom()) {
                yaml.set(key + ".randomizeCommands", true);
            }
        }
    }

}
