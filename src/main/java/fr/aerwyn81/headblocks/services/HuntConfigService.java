package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.TieredReward;
import fr.aerwyn81.headblocks.data.head.visual.RenderMode;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntConfig;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.data.hunt.behavior.Behavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.OrderedBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.SpawnBehavior;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementMode;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementSet;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementType;
import fr.aerwyn81.headblocks.data.hunt.requirement.types.AreaRequirement;
import fr.aerwyn81.headblocks.utils.bukkit.PluginProvider;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.utils.scheduler.SchedulerAdapter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HuntConfigService {
    private static final String RENDERING_MODE = "rendering.mode";
    private static final String RENDERING_SCALE = "rendering.scale";
    private static final String RENDERING_GLOW = "rendering.glow";
    private static final String RENDERING = "rendering";
    private static final String REQUIREMENTS = "requirements";
    private static final String DEFAULT = "default";
    private static final String HEAD_CLICK_TITLE_ENABLED = "headClick.title.enabled";
    private static final String HEAD_CLICK_TITLE_FIRST_LINE = "headClick.title.firstLine";
    private static final String HEAD_CLICK_TITLE_SUB_TITLE = "headClick.title.subTitle";
    private static final String HEAD_CLICK_TITLE_FADE_IN = "headClick.title.fadeIn";
    private static final String HEAD_CLICK_TITLE_STAY = "headClick.title.stay";
    private static final String HEAD_CLICK_TITLE_FADE_OUT = "headClick.title.fadeOut";
    private static final String HEAD_CLICK_SOUND_FOUND = "headClick.sound.found";
    private static final String HEAD_CLICK_SOUND_ALREADY_OWN = "headClick.sound.alreadyOwn";
    private static final String HEAD_CLICK_FIREWORK_ENABLED = "headClick.firework.enabled";
    private static final String HEAD_CLICK_EJECT_ENABLED = "headClick.eject.enabled";
    private static final String HEAD_CLICK_EJECT_POWER = "headClick.eject.power";
    private static final String HOLOGRAMS_FOUND_ENABLED = "holograms.found.enabled";
    private static final String HOLOGRAMS_NOT_FOUND_ENABLED = "holograms.notFound.enabled";
    private static final String HOLOGRAMS_FOUND_LINES = "holograms.found.lines";
    private static final String HOLOGRAMS_NOT_FOUND_LINES = "holograms.notFound.lines";
    private static final String HINTS_DISTANCE = "hints.distance";
    private static final String HINTS_FREQUENCY = "hints.frequency";
    private static final String SPIN_ENABLED = "spin.enabled";
    private static final String SPIN_SPEED = "spin.speed";
    private static final String SPIN_LINKED = "spin.linked";
    private static final String PARTICLES_FOUND_ENABLED = "particles.found.enabled";
    private static final String PARTICLES_FOUND_TYPE = "particles.found.type";
    private static final String PARTICLES_FOUND_AMOUNT = "particles.found.amount";
    private static final String PARTICLES_NOT_FOUND_ENABLED = "particles.notFound.enabled";
    private static final String PARTICLES_NOT_FOUND_TYPE = "particles.notFound.type";
    private static final String PARTICLES_NOT_FOUND_AMOUNT = "particles.notFound.amount";
    private static final String MESSAGES = ".messages";
    private static final String COMMANDS = ".commands";
    private static final String SLOTS_REQUIRED = ".slotsRequired";
    private static final String RANDOMIZE_COMMANDS = ".randomizeCommands";
    private static final String DISPLAY_NAME = "displayName";
    private static final String STATE = "state";
    private static final String PRIORITY = "priority";
    private static final String CONFIG = "config.";
    private static final String HEAD_CLICK_MESSAGES = "headClick.messages";
    private static final String HEAD_CLICK_COMMANDS = "headClick.commands";
    private static final String BROADCAST = ".broadcast";

    private final PluginProvider pluginProvider;
    private final ConfigService configService;
    private final ServiceRegistry registry;
    private final SchedulerAdapter scheduler;
    private File huntsDir;

    private final Map<String, YamlConfiguration> yamlCache = new ConcurrentHashMap<>();
    private final Set<String> savePendingHunts = ConcurrentHashMap.newKeySet();

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
        // A reload must read the hunt files back from disk, so the cache is dropped here. Pending
        // location writes are flushed first: invalidating alone would discard them.
        flushPendingSaves();
        invalidateAllYamlCaches();

        huntsDir = new File(pluginProvider.getDataFolder(), "hunts");
        if (!huntsDir.exists() && !huntsDir.mkdirs()) {
            LogUtil.error("Failed to create hunts directory: {0}", huntsDir.getAbsolutePath());
        }

        if (!huntFileExists(DEFAULT)) {
            generateDefaultFromConfig();
        }
    }

    private void flushPendingSaves() {
        for (String huntId : savePendingHunts) {
            YamlConfiguration yaml = yamlCache.get(huntId);
            if (yaml == null) {
                continue;
            }

            String content;
            synchronized (yaml) {
                content = yaml.saveToString();
            }

            writeAtomically(new File(huntsDir, huntId + ".yml"), content);
        }

        savePendingHunts.clear();
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

        String displayName = yaml.getString(DISPLAY_NAME, id);
        HuntState state = HuntState.of(yaml.getString(STATE, "ACTIVE"));
        int priority = yaml.getInt(PRIORITY, 1);
        String icon = yaml.getString("icon", "CHEST_MINECART");

        HBHunt hunt = new HBHunt(configService, id, displayName, state, priority, icon);

        List<Behavior> behaviors = loadBehaviors(yaml);
        hunt.setBehaviors(behaviors);

        boolean hasRequirements = yaml.contains(REQUIREMENTS);
        RequirementSet requirements = RequirementSet.fromSection(
                registry, yaml.getConfigurationSection(REQUIREMENTS));

        boolean hadLegacyZone = yaml.contains("behaviors.zone");
        if (hadLegacyZone) {
            if (hasRequirements) {
                LogUtil.warning("Hunt {0}: the leftover bounded zone was dropped, "
                        + "the requirements section takes precedence.", id);
            } else {
                requirements = migrateLegacyZone(yaml, id);
            }
        }

        hunt.setRequirements(requirements);
        if (behaviors.stream().anyMatch(b -> b instanceof SpawnBehavior spawn && spawn.placement() == SpawnBehavior.Placement.AREA)
                && SpawnBehavior.areaOf(hunt) == null) {
            LogUtil.warning("Hunt {0}: heads spawning in the area need an area requirement, no head will appear.", id);
        }

        HuntConfig huntConfig = loadHuntConfig(yaml);
        hunt.setConfig(huntConfig);

        if (hadLegacyZone) {
            yamlCache.put(id, yaml);
            saveHunt(hunt);
        }

        return hunt;
    }

    private RequirementSet migrateLegacyZone(YamlConfiguration yaml, String huntId) {
        ConfigurationSection zone = yaml.getConfigurationSection("behaviors.zone");
        if (zone == null) {
            LogUtil.warning("Hunt {0}: the bounded zone is not a section, it was dropped.", huntId);
            return new RequirementSet(registry);
        }

        AreaRequirement area = AreaRequirement.fromConfig(registry, zone);
        if (area != null && area.isComplete()) {
            LogUtil.success("Hunt {0}: the bounded zone became an area requirement.", huntId);
            return new RequirementSet(registry, RequirementMode.ALL, List.of(area));
        }

        LogUtil.warning("Hunt {0}: the bounded zone could not be read, it was carried over as is.", huntId);

        Map<String, Object> raw = RequirementSet.snapshot(zone);
        raw.put("type", RequirementType.AREA.getId());

        return new RequirementSet(registry, RequirementMode.ALL, List.of(), List.of(raw));
    }

    public void saveHunt(HBHunt hunt) {
        YamlConfiguration yaml = getOrCreateHuntYaml(hunt.getId());
        String content;

        synchronized (yaml) {
            yaml.set("id", hunt.getId());
            yaml.set(DISPLAY_NAME, hunt.getDisplayName());
            yaml.set(STATE, hunt.getState().name());
            yaml.set(PRIORITY, hunt.getPriority());
            yaml.set("icon", hunt.getIcon());

            saveBehaviors(yaml, hunt.getBehaviors());
            saveRequirements(yaml, hunt.getRequirements());
            saveHuntConfig(yaml, hunt.getConfig());

            content = yaml.saveToString();
        }

        writeAtomically(new File(huntsDir, hunt.getId() + ".yml"), content);
    }

    public boolean huntFileExists(String huntId) {
        return new File(huntsDir, huntId + ".yml").exists();
    }

    public void deleteHuntFile(String huntId) {
        yamlCache.remove(huntId);
        savePendingHunts.remove(huntId);

        File file = new File(huntsDir, huntId + ".yml");
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            LogUtil.error("Failed to delete hunt file {0}", file.getName());
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
                var headLocation = HeadLocation.fromConfig(legacyYaml, headUuid, DEFAULT);
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
        LogUtil.success("Migration complete: {0} head(s) migrated to default hunt.", migrated);
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
        String p = CONFIG;

        yaml.set("id", DEFAULT);
        yaml.set(DISPLAY_NAME, "Default");
        yaml.set(STATE, "ACTIVE");
        yaml.set(PRIORITY, 0);
        yaml.set("icon", "CHEST_MINECART");

        yaml.createSection("behaviors.free");

        writeDefaultHeadClick(yaml, p);
        writeDefaultDisplay(yaml, p);
        writeDefaultTieredRewards(yaml, p);

        try {
            yaml.save(defaultFile);
            LogUtil.success("hunts/default.yml generated successfully.");
        } catch (IOException e) {
            LogUtil.error("Failed to generate hunts/default.yml: {0}", e.getMessage());
        }
    }

    private void writeDefaultHeadClick(YamlConfiguration yaml, String p) {
        List<String> messages = configService.headClickMessages();
        if (!messages.isEmpty()) {
            yaml.set(p + HEAD_CLICK_MESSAGES, messages);
        }

        yaml.set(p + HEAD_CLICK_TITLE_ENABLED, configService.headClickTitleEnabled());
        String titleFirst = configService.headClickTitleFirstLine();
        if (!titleFirst.isEmpty()) {
            yaml.set(p + HEAD_CLICK_TITLE_FIRST_LINE, titleFirst);
        }
        String titleSub = configService.headClickTitleSubTitle();
        if (!titleSub.isEmpty()) {
            yaml.set(p + HEAD_CLICK_TITLE_SUB_TITLE, titleSub);
        }
        yaml.set(p + HEAD_CLICK_TITLE_FADE_IN, configService.headClickTitleFadeIn());
        yaml.set(p + HEAD_CLICK_TITLE_STAY, configService.headClickTitleStay());
        yaml.set(p + HEAD_CLICK_TITLE_FADE_OUT, configService.headClickTitleFadeOut());

        String soundFound = configService.headClickNotOwnSound();
        if (soundFound != null) {
            yaml.set(p + HEAD_CLICK_SOUND_FOUND, soundFound);
        }
        String soundOwn = configService.headClickAlreadyOwnSound();
        if (soundOwn != null) {
            yaml.set(p + HEAD_CLICK_SOUND_ALREADY_OWN, soundOwn);
        }

        yaml.set(p + HEAD_CLICK_FIREWORK_ENABLED, configService.fireworkEnabled());

        List<String> commands = configService.headClickCommands();
        if (!commands.isEmpty()) {
            yaml.set(p + HEAD_CLICK_COMMANDS, commands);
        }

        yaml.set(p + HEAD_CLICK_EJECT_ENABLED, configService.headClickEjectEnabled());
        yaml.set(p + HEAD_CLICK_EJECT_POWER, configService.headClickEjectPower());
    }

    private void writeDefaultDisplay(YamlConfiguration yaml, String p) {
        yaml.set(p + HOLOGRAMS_FOUND_ENABLED, configService.hologramsFoundEnabled());
        yaml.set(p + HOLOGRAMS_NOT_FOUND_ENABLED, configService.hologramsNotFoundEnabled());
        var foundLines = configService.hologramsFoundLines();
        if (!foundLines.isEmpty()) {
            yaml.set(p + HOLOGRAMS_FOUND_LINES, foundLines);
        }
        var notFoundLines = configService.hologramsNotFoundLines();
        if (!notFoundLines.isEmpty()) {
            yaml.set(p + HOLOGRAMS_NOT_FOUND_LINES, notFoundLines);
        }

        yaml.set(p + HINTS_DISTANCE, configService.hintDistanceBlocks());
        yaml.set(p + HINTS_FREQUENCY, configService.hintFrequency());

        yaml.set(p + SPIN_ENABLED, configService.spinEnabled());
        yaml.set(p + SPIN_SPEED, configService.spinSpeed());
        yaml.set(p + SPIN_LINKED, configService.spinLinked());

        yaml.set(p + PARTICLES_FOUND_ENABLED, configService.particlesFoundEnabled());
        yaml.set(p + PARTICLES_FOUND_TYPE, configService.particlesFoundType());
        yaml.set(p + PARTICLES_FOUND_AMOUNT, configService.particlesFoundAmount());
        yaml.set(p + PARTICLES_NOT_FOUND_ENABLED, configService.particlesNotFoundEnabled());
        yaml.set(p + PARTICLES_NOT_FOUND_TYPE, configService.particlesNotFoundType());
        yaml.set(p + PARTICLES_NOT_FOUND_AMOUNT, configService.particlesNotFoundAmount());
    }

    private void writeDefaultTieredRewards(YamlConfiguration yaml, String p) {
        List<TieredReward> tieredRewards = configService.tieredRewards();
        for (TieredReward reward : tieredRewards) {
            writeTieredReward(yaml, p + "tieredRewards." + reward.level(), reward);
        }
    }

    private static void writeTieredReward(YamlConfiguration yaml, String key, TieredReward reward) {
        if (!reward.messages().isEmpty()) {
            yaml.set(key + MESSAGES, reward.messages());
        }
        if (!reward.commands().isEmpty()) {
            yaml.set(key + COMMANDS, reward.commands());
        }
        if (!reward.broadcastMessages().isEmpty()) {
            yaml.set(key + BROADCAST, reward.broadcastMessages());
        }
        if (reward.slotsRequired() != -1) {
            yaml.set(key + SLOTS_REQUIRED, reward.slotsRequired());
        }
        if (reward.isRandom()) {
            yaml.set(key + RANDOMIZE_COMMANDS, true);
        }
    }

    // --- Location management ---

    public List<HeadLocation> loadLocationsFromHunt(String huntId) {
        List<HeadLocation> locations = new ArrayList<>();
        YamlConfiguration yaml = getOrLoadHuntYaml(huntId);
        if (yaml == null) {
            return locations;
        }

        synchronized (yaml) {
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
        }

        return locations;
    }

    public void saveLocationInHunt(String huntId, HeadLocation headLocation) {
        YamlConfiguration yaml = getOrLoadHuntYaml(huntId);
        if (yaml == null) {
            LogUtil.error("Cannot save location in hunt {0}: hunt file not found.", huntId);
            return;
        }

        synchronized (yaml) {
            headLocation.saveInConfig(yaml);
        }

        debouncedSave(huntId, yaml);
    }

    public void removeLocationFromHunt(String huntId, UUID headUuid) {
        YamlConfiguration yaml = getOrLoadHuntYaml(huntId);
        if (yaml == null) {
            return;
        }

        synchronized (yaml) {
            yaml.set("locations." + headUuid, null);
        }

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

    private YamlConfiguration getOrCreateHuntYaml(String huntId) {
        return yamlCache.computeIfAbsent(huntId, id -> {
            File file = new File(huntsDir, id + ".yml");
            if (!file.exists()) {
                return new YamlConfiguration();
            }
            return YamlConfiguration.loadConfiguration(file);
        });
    }

    public void invalidateAllYamlCaches() {
        yamlCache.clear();
    }

    private void debouncedSave(String huntId, YamlConfiguration yaml) {
        if (!savePendingHunts.add(huntId)) {
            return;
        }

        scheduler.runTaskLater(() -> {
            savePendingHunts.remove(huntId);

            // Superseded since it was queued: either the hunt was deleted, or initialize() flushed
            // this write and dropped the cache. Only the former loses data worth reporting.
            if (yamlCache.get(huntId) != yaml) {
                if (!huntFileExists(huntId)) {
                    LogUtil.warning("Dropped a pending save for hunt {0}: the hunt was deleted.", huntId);
                }
                return;
            }

            String content;
            synchronized (yaml) {
                content = yaml.saveToString();
            }

            File file = new File(huntsDir, huntId + ".yml");
            scheduler.runTaskAsync(() -> writeAtomically(file, content));
        }, 1L);
    }

    private void writeAtomically(File file, String content) {
        Path target = file.toPath();
        Path temp = null;

        try {
            temp = Files.createTempFile(target.getParent(), file.getName(), ".tmp");
            Files.writeString(temp, content);

            moveReplacing(temp, target);
        } catch (Exception e) {
            LogUtil.error("Cannot save hunt file {0}: {1}", file.getName(), e.getMessage());
            deleteQuietly(temp);
        }
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }

        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // ignored: best effort cleanup of a temporary file
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
            if ("zone".equalsIgnoreCase(type)) {
                continue;
            }

            ConfigurationSection behaviorSection = section.getConfigurationSection(type);
            behaviors.add(Behavior.fromConfig(type, registry, behaviorSection));
        }

        if (behaviors.stream().anyMatch(SpawnBehavior.class::isInstance)
                && behaviors.removeIf(OrderedBehavior.class::isInstance)) {
            LogUtil.warning("Hunt {0}: the ordered behavior cannot be combined with spawned heads, it was ignored.",
                    yaml.getString("id"));
        }

        return behaviors;
    }

    private void saveRequirements(YamlConfiguration yaml, RequirementSet requirements) {
        yaml.set(REQUIREMENTS, null);

        if (requirements == null || (requirements.isEmpty() && requirements.getPreserved().isEmpty())) {
            return;
        }

        requirements.saveTo(yaml.createSection(REQUIREMENTS));
    }

    private void saveBehaviors(YamlConfiguration yaml, List<Behavior> behaviors) {
        yaml.set("behaviors", null);

        for (Behavior behavior : behaviors) {
            behavior.saveTo(yaml.createSection("behaviors." + behavior.getId()));
        }
    }

    private HuntConfig loadHuntConfig(YamlConfiguration yaml) {
        HuntConfig hc = new HuntConfig(configService);
        String p = CONFIG;

        loadHeadClickConfig(yaml, hc, p);
        loadHologramsHintsAndSpin(yaml, hc, p);
        loadRenderingAndParticles(yaml, hc, p);
        loadTieredRewards(yaml, hc, p);

        return hc;
    }

    private static void loadHeadClickConfig(YamlConfiguration yaml, HuntConfig hc, String p) {
        if (yaml.contains(p + HEAD_CLICK_MESSAGES)) {
            hc.setHeadClickMessages(yaml.getStringList(p + HEAD_CLICK_MESSAGES));
        }
        if (yaml.contains(p + HEAD_CLICK_TITLE_ENABLED)) {
            hc.setHeadClickTitleEnabled(yaml.getBoolean(p + HEAD_CLICK_TITLE_ENABLED));
        }
        if (yaml.contains(p + HEAD_CLICK_TITLE_FIRST_LINE)) {
            hc.setHeadClickTitleFirstLine(yaml.getString(p + HEAD_CLICK_TITLE_FIRST_LINE));
        }
        if (yaml.contains(p + HEAD_CLICK_TITLE_SUB_TITLE)) {
            hc.setHeadClickTitleSubTitle(yaml.getString(p + HEAD_CLICK_TITLE_SUB_TITLE));
        }
        if (yaml.contains(p + HEAD_CLICK_TITLE_FADE_IN)) {
            hc.setHeadClickTitleFadeIn(yaml.getInt(p + HEAD_CLICK_TITLE_FADE_IN));
        }
        if (yaml.contains(p + HEAD_CLICK_TITLE_STAY)) {
            hc.setHeadClickTitleStay(yaml.getInt(p + HEAD_CLICK_TITLE_STAY));
        }
        if (yaml.contains(p + HEAD_CLICK_TITLE_FADE_OUT)) {
            hc.setHeadClickTitleFadeOut(yaml.getInt(p + HEAD_CLICK_TITLE_FADE_OUT));
        }
        if (yaml.contains(p + HEAD_CLICK_SOUND_FOUND)) {
            hc.setHeadClickSoundFound(yaml.getString(p + HEAD_CLICK_SOUND_FOUND));
        }
        if (yaml.contains(p + HEAD_CLICK_SOUND_ALREADY_OWN)) {
            hc.setHeadClickSoundAlreadyOwn(yaml.getString(p + HEAD_CLICK_SOUND_ALREADY_OWN));
        }
        if (yaml.contains(p + HEAD_CLICK_FIREWORK_ENABLED)) {
            hc.setFireworkEnabled(yaml.getBoolean(p + HEAD_CLICK_FIREWORK_ENABLED));
        }
        if (yaml.contains(p + HEAD_CLICK_COMMANDS)) {
            hc.setHeadClickCommands(yaml.getStringList(p + HEAD_CLICK_COMMANDS));
        }
        if (yaml.contains(p + HEAD_CLICK_EJECT_ENABLED)) {
            hc.setHeadClickEjectEnabled(yaml.getBoolean(p + HEAD_CLICK_EJECT_ENABLED));
        }
        if (yaml.contains(p + HEAD_CLICK_EJECT_POWER)) {
            hc.setHeadClickEjectPower(yaml.getDouble(p + HEAD_CLICK_EJECT_POWER));
        }
    }

    private static void loadHologramsHintsAndSpin(YamlConfiguration yaml, HuntConfig hc, String p) {
        if (yaml.contains(p + "holograms.enabled")) {
            hc.setHologramsEnabled(yaml.getBoolean(p + "holograms.enabled"));
        }
        if (yaml.contains(p + HOLOGRAMS_FOUND_ENABLED)) {
            hc.setHologramsFoundEnabled(yaml.getBoolean(p + HOLOGRAMS_FOUND_ENABLED));
        }
        if (yaml.contains(p + HOLOGRAMS_NOT_FOUND_ENABLED)) {
            hc.setHologramsNotFoundEnabled(yaml.getBoolean(p + HOLOGRAMS_NOT_FOUND_ENABLED));
        }
        if (yaml.contains(p + HOLOGRAMS_FOUND_LINES)) {
            hc.setHologramsFoundLines(new ArrayList<>(yaml.getStringList(p + HOLOGRAMS_FOUND_LINES)));
        }
        if (yaml.contains(p + HOLOGRAMS_NOT_FOUND_LINES)) {
            hc.setHologramsNotFoundLines(new ArrayList<>(yaml.getStringList(p + HOLOGRAMS_NOT_FOUND_LINES)));
        }

        if (yaml.contains(p + "hints.enabled")) {
            hc.setHintsEnabled(yaml.getBoolean(p + "hints.enabled"));
        }
        if (yaml.contains(p + HINTS_DISTANCE)) {
            hc.setHintDistance(yaml.getInt(p + HINTS_DISTANCE));
        }
        if (yaml.contains(p + HINTS_FREQUENCY)) {
            hc.setHintFrequency(yaml.getInt(p + HINTS_FREQUENCY));
        }

        if (yaml.contains(p + SPIN_ENABLED)) {
            hc.setSpinEnabled(yaml.getBoolean(p + SPIN_ENABLED));
        }
        if (yaml.contains(p + SPIN_SPEED)) {
            hc.setSpinSpeed(yaml.getInt(p + SPIN_SPEED));
        }
        if (yaml.contains(p + SPIN_LINKED)) {
            hc.setSpinLinked(yaml.getBoolean(p + SPIN_LINKED));
        }
    }

    private static void loadRenderingAndParticles(YamlConfiguration yaml, HuntConfig hc, String p) {
        if (yaml.contains(p + RENDERING_MODE)) {
            var mode = RenderMode.of(yaml.getString(p + RENDERING_MODE));
            if (mode == null) {
                LogUtil.warning("Unknown rendering mode {0}, falling back to the global one.", yaml.getString(p + RENDERING_MODE));
            }
            hc.setRenderMode(mode);
        }
        if (yaml.contains(p + RENDERING_SCALE)) {
            hc.setRenderScale(Math.max(0.1, yaml.getDouble(p + RENDERING_SCALE)));
        }
        if (yaml.contains(p + RENDERING_GLOW)) {
            hc.setRenderGlow(yaml.getBoolean(p + RENDERING_GLOW));
        }

        if (yaml.contains(p + PARTICLES_FOUND_ENABLED)) {
            hc.setParticlesFoundEnabled(yaml.getBoolean(p + PARTICLES_FOUND_ENABLED));
        }
        if (yaml.contains(p + PARTICLES_NOT_FOUND_ENABLED)) {
            hc.setParticlesNotFoundEnabled(yaml.getBoolean(p + PARTICLES_NOT_FOUND_ENABLED));
        }
        if (yaml.contains(p + PARTICLES_FOUND_TYPE)) {
            hc.setParticlesFoundType(yaml.getString(p + PARTICLES_FOUND_TYPE));
        }
        if (yaml.contains(p + PARTICLES_FOUND_AMOUNT)) {
            hc.setParticlesFoundAmount(yaml.getInt(p + PARTICLES_FOUND_AMOUNT));
        }
        if (yaml.contains(p + PARTICLES_NOT_FOUND_TYPE)) {
            hc.setParticlesNotFoundType(yaml.getString(p + PARTICLES_NOT_FOUND_TYPE));
        }
        if (yaml.contains(p + PARTICLES_NOT_FOUND_AMOUNT)) {
            hc.setParticlesNotFoundAmount(yaml.getInt(p + PARTICLES_NOT_FOUND_AMOUNT));
        }
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
                if (section.contains(level + MESSAGES)) {
                    messages = section.getStringList(level + MESSAGES);
                }

                List<String> commands = new ArrayList<>();
                if (section.contains(level + COMMANDS)) {
                    commands = section.getStringList(level + COMMANDS);
                }

                List<String> broadcast = new ArrayList<>();
                if (section.contains(level + BROADCAST)) {
                    broadcast = section.getStringList(level + BROADCAST);
                }

                int slotsRequired = section.getInt(level + SLOTS_REQUIRED, -1);
                boolean isRandom = section.getBoolean(level + RANDOMIZE_COMMANDS, false);

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
        String p = CONFIG;

        if (hc.hasHeadClickMessages()) {
            yaml.set(p + HEAD_CLICK_MESSAGES, hc.getHeadClickMessages());
        }

        if (hc.hasHologramsFoundLines()) {
            yaml.set(p + HOLOGRAMS_FOUND_LINES, hc.getHologramsFoundLines());
        }
        if (hc.hasHologramsNotFoundLines()) {
            yaml.set(p + HOLOGRAMS_NOT_FOUND_LINES, hc.getHologramsNotFoundLines());
        }

        if (hc.hasTieredRewards()) {
            saveTieredRewards(yaml, hc.getTieredRewards(), p);
        }

        yaml.set(p + RENDERING_MODE, hc.hasRenderMode() ? hc.getRenderMode().name() : null);
        yaml.set(p + RENDERING_SCALE, hc.hasRenderScale() ? hc.getRenderScale() : null);
        yaml.set(p + RENDERING_GLOW, hc.hasRenderGlow() ? hc.isRenderGlow() : null);
        if (yaml.getConfigurationSection(p + RENDERING) != null
                && yaml.getConfigurationSection(p + RENDERING).getKeys(false).isEmpty()) {
            yaml.set(p + RENDERING, null);
        }
    }

    private void saveTieredRewards(YamlConfiguration yaml, List<TieredReward> rewards, String prefix) {
        for (TieredReward reward : rewards) {
            String key = prefix + "tieredRewards." + reward.level();
            if (!reward.messages().isEmpty()) {
                yaml.set(key + MESSAGES, reward.messages());
            }
            if (!reward.commands().isEmpty()) {
                yaml.set(key + COMMANDS, reward.commands());
            }
            if (!reward.broadcastMessages().isEmpty()) {
                yaml.set(key + BROADCAST, reward.broadcastMessages());
            }
            if (reward.slotsRequired() != -1) {
                yaml.set(key + SLOTS_REQUIRED, reward.slotsRequired());
            }
            if (reward.isRandom()) {
                yaml.set(key + RANDOMIZE_COMMANDS, true);
            }
        }
    }
}
