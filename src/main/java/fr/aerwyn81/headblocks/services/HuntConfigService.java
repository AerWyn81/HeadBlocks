package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.TieredReward;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.data.hunt.HuntConfig;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.data.hunt.behavior.Behavior;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HuntConfigService {
    private static File huntsDir;

    public static void initialize() {
        huntsDir = new File(HeadBlocks.getInstance().getDataFolder(), "hunts");
        if (!huntsDir.exists() && !huntsDir.mkdirs()) {
            LogUtil.error("Failed to create hunts directory: {0}", huntsDir.getAbsolutePath());
        }

        // On first run after migration: generate default.yml from existing config.yml
        if (!huntFileExists("default")) {
            generateDefaultFromConfig();
        }
    }

    public static List<Hunt> loadHunts() {
        List<Hunt> hunts = new ArrayList<>();
        File[] files = huntsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return hunts;

        for (File file : files) {
            try {
                Hunt hunt = loadHunt(file);
                if (hunt != null) {
                    hunts.add(hunt);
                }
            } catch (Exception e) {
                LogUtil.error("Failed to load hunt file {0}: {1}", file.getName(), e.getMessage());
            }
        }

        return hunts;
    }

    public static Hunt loadHunt(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        String id = yaml.getString("id");
        if (id == null || id.isEmpty()) {
            LogUtil.warning("Hunt file {0} has no 'id' field, skipping.", file.getName());
            return null;
        }

        String displayName = yaml.getString("displayName", id);
        HuntState state = HuntState.of(yaml.getString("state", "ACTIVE"));
        int priority = yaml.getInt("priority", 1);
        String icon = yaml.getString("icon", "PLAYER_HEAD");

        Hunt hunt = new Hunt(id, displayName, state, priority, icon);

        List<Behavior> behaviors = loadBehaviors(yaml);
        hunt.setBehaviors(behaviors);

        HuntConfig huntConfig = loadHuntConfig(yaml);
        hunt.setConfig(huntConfig);

        return hunt;
    }

    public static void saveHunt(Hunt hunt) {
        File file = new File(huntsDir, hunt.getId() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();

        yaml.set("id", hunt.getId());
        yaml.set("displayName", hunt.getDisplayName());
        yaml.set("state", hunt.getState().name());
        yaml.set("priority", hunt.getPriority());
        yaml.set("icon", hunt.getIcon());

        saveBehaviors(yaml, hunt.getBehaviors());
        saveHuntConfig(yaml, hunt.getConfig());

        try {
            yaml.save(file);
        } catch (IOException e) {
            LogUtil.error("Failed to save hunt file {0}: {1}", file.getName(), e.getMessage());
        }
    }

    public static boolean huntFileExists(String huntId) {
        return new File(huntsDir, huntId + ".yml").exists();
    }

    public static void deleteHuntFile(String huntId) {
        File file = new File(huntsDir, huntId + ".yml");
        if (file.exists()) {
            if (!file.delete()) {
                LogUtil.error("Failed to delete hunt file {0}", file.getName());
            }
        }
    }

    // --- Migration: generate default.yml from existing config.yml ---

    public static void generateDefaultFromConfig() {
        File dir = new File(HeadBlocks.getInstance().getDataFolder(), "hunts");
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

        // Hunt identity
        yaml.set("id", "default");
        yaml.set("displayName", "Default");
        yaml.set("state", "ACTIVE");
        yaml.set("priority", 0);
        yaml.set("icon", "PLAYER_HEAD");

        // Behavior
        yaml.createSection("behaviors.free");

        // HeadClick
        List<String> messages = ConfigService.getHeadClickMessages();
        if (!messages.isEmpty())
            yaml.set(p + "headClick.messages", messages);

        yaml.set(p + "headClick.title.enabled", ConfigService.isHeadClickTitleEnabled());
        String titleFirst = ConfigService.getHeadClickTitleFirstLine();
        if (!titleFirst.isEmpty()) yaml.set(p + "headClick.title.firstLine", titleFirst);
        String titleSub = ConfigService.getHeadClickTitleSubTitle();
        if (!titleSub.isEmpty()) yaml.set(p + "headClick.title.subTitle", titleSub);
        yaml.set(p + "headClick.title.fadeIn", ConfigService.getHeadClickTitleFadeIn());
        yaml.set(p + "headClick.title.stay", ConfigService.getHeadClickTitleStay());
        yaml.set(p + "headClick.title.fadeOut", ConfigService.getHeadClickTitleFadeOut());

        String soundFound = ConfigService.getHeadClickNotOwnSound();
        if (soundFound != null) yaml.set(p + "headClick.sound.found", soundFound);
        String soundOwn = ConfigService.getHeadClickAlreadyOwnSound();
        if (soundOwn != null) yaml.set(p + "headClick.sound.alreadyOwn", soundOwn);

        yaml.set(p + "headClick.firework.enabled", ConfigService.isFireworkEnabled());

        List<String> commands = ConfigService.getHeadClickCommands();
        if (!commands.isEmpty())
            yaml.set(p + "headClick.commands", commands);

        yaml.set(p + "headClick.eject.enabled", ConfigService.isHeadClickEjectEnabled());
        yaml.set(p + "headClick.eject.power", ConfigService.getHeadClickEjectPower());

        // Holograms
        yaml.set(p + "holograms.found.enabled", ConfigService.isHologramsFoundEnabled());
        yaml.set(p + "holograms.notFound.enabled", ConfigService.isHologramsNotFoundEnabled());
        ArrayList<String> foundLines = ConfigService.getHologramsFoundLines();
        if (!foundLines.isEmpty()) yaml.set(p + "holograms.found.lines", foundLines);
        ArrayList<String> notFoundLines = ConfigService.getHologramsNotFoundLines();
        if (!notFoundLines.isEmpty()) yaml.set(p + "holograms.notFound.lines", notFoundLines);

        // Hints
        yaml.set(p + "hints.distance", ConfigService.getHintDistanceBlocks());
        yaml.set(p + "hints.frequency", ConfigService.getHintFrequency());

        // Spin
        yaml.set(p + "spin.enabled", ConfigService.isSpinEnabled());
        yaml.set(p + "spin.speed", ConfigService.getSpinSpeed());
        yaml.set(p + "spin.linked", ConfigService.isSpinLinked());

        // Particles
        yaml.set(p + "particles.found.enabled", ConfigService.isParticlesFoundEnabled());
        yaml.set(p + "particles.found.type", ConfigService.getParticlesFoundType());
        yaml.set(p + "particles.found.amount", ConfigService.getParticlesFoundAmount());
        yaml.set(p + "particles.notFound.enabled", ConfigService.isParticlesNotFoundEnabled());
        yaml.set(p + "particles.notFound.type", ConfigService.getParticlesNotFoundType());
        yaml.set(p + "particles.notFound.amount", ConfigService.getParticlesNotFoundAmount());

        // TieredRewards
        List<TieredReward> tieredRewards = ConfigService.getTieredRewards();
        if (!tieredRewards.isEmpty()) {
            for (TieredReward reward : tieredRewards) {
                String key = p + "tieredRewards." + reward.level();
                if (!reward.messages().isEmpty()) yaml.set(key + ".messages", reward.messages());
                if (!reward.commands().isEmpty()) yaml.set(key + ".commands", reward.commands());
                if (!reward.broadcastMessages().isEmpty())
                    yaml.set(key + ".broadcast", reward.broadcastMessages());
                if (reward.slotsRequired() != -1) yaml.set(key + ".slotsRequired", reward.slotsRequired());
                if (reward.isRandom()) yaml.set(key + ".randomizeCommands", true);
            }
        }

        try {
            yaml.save(defaultFile);
            LogUtil.info("hunts/default.yml generated successfully.");
        } catch (IOException e) {
            LogUtil.error("Failed to generate hunts/default.yml: {0}", e.getMessage());
        }
    }

    // --- Behaviors ---

    private static List<Behavior> loadBehaviors(YamlConfiguration yaml) {
        List<Behavior> behaviors = new ArrayList<>();

        ConfigurationSection section = yaml.getConfigurationSection("behaviors");
        if (section == null) return behaviors;

        for (String type : section.getKeys(false)) {
            ConfigurationSection behaviorSection = section.getConfigurationSection(type);
            behaviors.add(Behavior.fromConfig(type, behaviorSection));
        }

        return behaviors;
    }

    private static void saveBehaviors(YamlConfiguration yaml, List<Behavior> behaviors) {
        for (Behavior behavior : behaviors) {
            String key = "behaviors." + behavior.getId();
            yaml.createSection(key);

            if (behavior instanceof fr.aerwyn81.headblocks.data.hunt.behavior.ScheduledBehavior sb) {
                if (sb.start() != null) yaml.set(key + ".start", sb.start().toString());
                if (sb.end() != null) yaml.set(key + ".end", sb.end().toString());
            }
        }
    }

    // --- HuntConfig loading ---

    private static HuntConfig loadHuntConfig(YamlConfiguration yaml) {
        HuntConfig hc = new HuntConfig();
        String p = "config.";

        // HeadClick
        if (yaml.contains(p + "headClick.messages"))
            hc.setHeadClickMessages(yaml.getStringList(p + "headClick.messages"));
        if (yaml.contains(p + "headClick.title.enabled"))
            hc.setHeadClickTitleEnabled(yaml.getBoolean(p + "headClick.title.enabled"));
        if (yaml.contains(p + "headClick.title.firstLine"))
            hc.setHeadClickTitleFirstLine(yaml.getString(p + "headClick.title.firstLine"));
        if (yaml.contains(p + "headClick.title.subTitle"))
            hc.setHeadClickTitleSubTitle(yaml.getString(p + "headClick.title.subTitle"));
        if (yaml.contains(p + "headClick.title.fadeIn"))
            hc.setHeadClickTitleFadeIn(yaml.getInt(p + "headClick.title.fadeIn"));
        if (yaml.contains(p + "headClick.title.stay"))
            hc.setHeadClickTitleStay(yaml.getInt(p + "headClick.title.stay"));
        if (yaml.contains(p + "headClick.title.fadeOut"))
            hc.setHeadClickTitleFadeOut(yaml.getInt(p + "headClick.title.fadeOut"));
        if (yaml.contains(p + "headClick.sound.found"))
            hc.setHeadClickSoundFound(yaml.getString(p + "headClick.sound.found"));
        if (yaml.contains(p + "headClick.sound.alreadyOwn"))
            hc.setHeadClickSoundAlreadyOwn(yaml.getString(p + "headClick.sound.alreadyOwn"));
        if (yaml.contains(p + "headClick.firework.enabled"))
            hc.setFireworkEnabled(yaml.getBoolean(p + "headClick.firework.enabled"));
        if (yaml.contains(p + "headClick.commands"))
            hc.setHeadClickCommands(yaml.getStringList(p + "headClick.commands"));
        if (yaml.contains(p + "headClick.eject.enabled"))
            hc.setHeadClickEjectEnabled(yaml.getBoolean(p + "headClick.eject.enabled"));
        if (yaml.contains(p + "headClick.eject.power"))
            hc.setHeadClickEjectPower(yaml.getDouble(p + "headClick.eject.power"));

        // Holograms
        if (yaml.contains(p + "holograms.enabled"))
            hc.setHologramsEnabled(yaml.getBoolean(p + "holograms.enabled"));
        if (yaml.contains(p + "holograms.found.enabled"))
            hc.setHologramsFoundEnabled(yaml.getBoolean(p + "holograms.found.enabled"));
        if (yaml.contains(p + "holograms.notFound.enabled"))
            hc.setHologramsNotFoundEnabled(yaml.getBoolean(p + "holograms.notFound.enabled"));
        if (yaml.contains(p + "holograms.found.lines"))
            hc.setHologramsFoundLines(new ArrayList<>(yaml.getStringList(p + "holograms.found.lines")));
        if (yaml.contains(p + "holograms.notFound.lines"))
            hc.setHologramsNotFoundLines(new ArrayList<>(yaml.getStringList(p + "holograms.notFound.lines")));

        // Hints
        if (yaml.contains(p + "hints.enabled"))
            hc.setHintsEnabled(yaml.getBoolean(p + "hints.enabled"));
        if (yaml.contains(p + "hints.distance"))
            hc.setHintDistance(yaml.getInt(p + "hints.distance"));
        if (yaml.contains(p + "hints.frequency"))
            hc.setHintFrequency(yaml.getInt(p + "hints.frequency"));

        // Spin
        if (yaml.contains(p + "spin.enabled"))
            hc.setSpinEnabled(yaml.getBoolean(p + "spin.enabled"));
        if (yaml.contains(p + "spin.speed"))
            hc.setSpinSpeed(yaml.getInt(p + "spin.speed"));
        if (yaml.contains(p + "spin.linked"))
            hc.setSpinLinked(yaml.getBoolean(p + "spin.linked"));

        // Particles
        if (yaml.contains(p + "particles.found.enabled"))
            hc.setParticlesFoundEnabled(yaml.getBoolean(p + "particles.found.enabled"));
        if (yaml.contains(p + "particles.notFound.enabled"))
            hc.setParticlesNotFoundEnabled(yaml.getBoolean(p + "particles.notFound.enabled"));
        if (yaml.contains(p + "particles.found.type"))
            hc.setParticlesFoundType(yaml.getString(p + "particles.found.type"));
        if (yaml.contains(p + "particles.found.amount"))
            hc.setParticlesFoundAmount(yaml.getInt(p + "particles.found.amount"));
        if (yaml.contains(p + "particles.notFound.type"))
            hc.setParticlesNotFoundType(yaml.getString(p + "particles.notFound.type"));
        if (yaml.contains(p + "particles.notFound.amount"))
            hc.setParticlesNotFoundAmount(yaml.getInt(p + "particles.notFound.amount"));

        // TieredRewards
        loadTieredRewards(yaml, hc, p);

        return hc;
    }

    private static void loadTieredRewards(YamlConfiguration yaml, HuntConfig hc, String prefix) {
        ConfigurationSection section = yaml.getConfigurationSection(prefix + "tieredRewards");
        if (section == null) return;

        List<TieredReward> rewards = new ArrayList<>();
        for (String level : section.getKeys(false)) {
            try {
                List<String> messages = new ArrayList<>();
                if (section.contains(level + ".messages"))
                    messages = section.getStringList(level + ".messages");

                List<String> commands = new ArrayList<>();
                if (section.contains(level + ".commands"))
                    commands = section.getStringList(level + ".commands");

                List<String> broadcast = new ArrayList<>();
                if (section.contains(level + ".broadcast"))
                    broadcast = section.getStringList(level + ".broadcast");

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

    // --- HuntConfig saving ---

    private static void saveHuntConfig(YamlConfiguration yaml, HuntConfig hc) {
        String p = "config.";

        // HeadClick
        if (hc.hasHeadClickMessages())
            yaml.set(p + "headClick.messages", hc.getHeadClickMessages());

        // Holograms
        if (hc.hasHologramsFoundLines())
            yaml.set(p + "holograms.found.lines", hc.getHologramsFoundLines());
        if (hc.hasHologramsNotFoundLines())
            yaml.set(p + "holograms.notFound.lines", hc.getHologramsNotFoundLines());

        // TieredRewards
        if (hc.hasTieredRewards()) {
            saveTieredRewards(yaml, hc.getTieredRewards(), p);
        }
    }

    private static void saveTieredRewards(YamlConfiguration yaml, List<TieredReward> rewards, String prefix) {
        for (TieredReward reward : rewards) {
            String key = prefix + "tieredRewards." + reward.level();
            if (!reward.messages().isEmpty())
                yaml.set(key + ".messages", reward.messages());
            if (!reward.commands().isEmpty())
                yaml.set(key + ".commands", reward.commands());
            if (!reward.broadcastMessages().isEmpty())
                yaml.set(key + ".broadcast", reward.broadcastMessages());
            if (reward.slotsRequired() != -1)
                yaml.set(key + ".slotsRequired", reward.slotsRequired());
            if (reward.isRandom())
                yaml.set(key + ".randomizeCommands", true);
        }
    }
}
