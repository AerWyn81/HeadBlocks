package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.head.HBRace;
import fr.aerwyn81.headblocks.managers.HeadManager;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RaceService {
    private static HashMap<String, HBRace> races;
    private static HashMap<String, File> raceFiles;
    private static HashMap<String, YamlConfiguration> raceConfigs;

    public static void initialize() {
        races = new HashMap<>();
        raceFiles = new HashMap<>();
        raceConfigs = new HashMap<>();

        File directory = new File(HeadBlocks.getInstance().getDataFolder(), "races");
        if (!directory.exists()) {
            directory.mkdir();
        }

        migrateOldLocations();
        loadRaces();
    }

    public static boolean createRace(String id) {
        File file = new File(HeadBlocks.getInstance().getDataFolder(), "races/" + id + ".yml");

        if (!file.exists()) {
            try {
                var fileCreated = file.createNewFile();
                if (!fileCreated) {
                    throw new InternalException("A file with the same id already exist. Should never append.");
                }

                raceFiles.put(id, file);
                raceConfigs.put(id, YamlConfiguration.loadConfiguration(file));

                try {
                    StorageService.createRace(id);
                } catch (Exception ex) {
                    HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to remove race " + id + " from the storage: " + ex.getMessage()));
                }

            } catch (Exception ex) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while creating race " + id + ": " + ex.getMessage()));
                return false;
            }
        }

        return true;
    }

    public static boolean removeRace(String id) {
        if (raceFiles.containsKey(id) && raceConfigs.containsKey(id)) {
            File file = raceFiles.get(id);
            var isDeleted = file.delete();
            raceFiles.remove(id);
            raceConfigs.remove(id);

            try {
                StorageService.removeRace(id);
            } catch (Exception ex) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to remove race " + id + " from the storage: " + ex.getMessage()));
            }

            return isDeleted;
        }

        return false;
    }

    public static void saveConfig(String id) {
        try {
            File file = raceFiles.get(id);
            if (file.exists()) {
                raceConfigs.get(id).save(file);
            }
        } catch (IOException ex) {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while saving race in config: " + ex.getMessage()));
        }
    }

    private static void loadRaces() {
        races.clear();
        raceFiles.clear();
        raceConfigs.clear();

        HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks] &eLoading races:"));
        File[] allFiles = new File(HeadBlocks.getInstance().getDataFolder(), "races").listFiles();

        if (allFiles == null || allFiles.length == 0) {
            return;
        }

        for (File file : allFiles) {
            String id = file.getName().toLowerCase().replace(".yml", "");

            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String name = config.getString("race.name");

            if (!Character.isDigit(id.charAt(0))) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks]   &cCannot load race " + name + " because filename must be a number."));
                continue;
            }

            String description = config.getString("race.description");

            ItemStack iconItem;
            try {
                iconItem = new ItemStack(Material.valueOf(config.getString("race.icon", "PAPER")));
            } catch (Exception ex) {
                iconItem = new ItemStack(Material.PAPER);
            }

            var race = new HBRace(id, name, description, iconItem);
            race.getHeadManager().loadHeadLocations(config);

            races.put(id, race);
            raceFiles.put(id, file);
            raceConfigs.put(id, config);
        }

        if (races.size() == 0) {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks]   &cNo race has been loaded!"));
        } else {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks]   &e" + races.size() + " &araces has been loaded!"));
        }
    }

    private static void migrateOldLocations() {
        var locationsFile = new File(HeadBlocks.getInstance().getDataFolder(), "locations.yml");
        if (!locationsFile.exists()) {
            return;
        }

        HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks] &eFound old HeadBlocks locations configuration. Starting migration..."));

        String id;
        File[] allFiles = new File(HeadBlocks.getInstance().getDataFolder(), "races").listFiles();
        if (allFiles == null) {
            id = "1";
        }
        else {
            var numberPattern = Pattern.compile("^(\\d+)\\.yml$");
            var optInt = Arrays.stream(allFiles)
                    .map(File::getName)
                    .map(numberPattern::matcher)
                    .filter(Matcher::matches)
                    .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
                    .max();

            if (optInt.isEmpty()) {
                id = "1";
            } else {
                id = String.valueOf(optInt.getAsInt()+ 1);
            }
        }

        var fileCreated = createRace(id);
        if (!fileCreated) {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks] &cCritical error, cannot to migrate the old HeadBlocks configuration. Please ask developer. Plugin disabling..."));
            Bukkit.getPluginManager().disablePlugin(HeadBlocks.getInstance());
        }

        var config = YamlConfiguration.loadConfiguration(locationsFile);

        var defaultRace = new HBRace(id, "Default", "", new ItemStack(Material.PAPER));
        defaultRace.getHeadManager().loadHeadLocations(config);
        defaultRace.saveRace();

        locationsFile.renameTo(new File(HeadBlocks.getInstance().getDataFolder(), "locations.yml.backup"));

        HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks] &eMigration successfully completed of " +
                defaultRace.getHeadManager().getHeadLocations().size() + " locations in a default race."));
    }

    private static UUID addHead(String raceId, Location location, String texture) throws InternalException {
        var headManager = getHeadManager(raceId);

        if (headManager == null) {
            throw new RuntimeException("Head cannot be add No race with id: " + raceId);
        }

        return headManager.addHeadLocation(location, texture);
    }

    private static void removeHead(String raceId, HeadLocation headLocation, boolean withDelete) throws InternalException {
        var headManager = getHeadManager(raceId);

        if (headManager == null) {
            throw new RuntimeException("No race with id: " + raceId);
        }

        headManager.removeHeadLocation(headLocation, withDelete);
    }

    public static HashMap<String, HBRace> getRaces() {
        return races;
    }

    public static boolean isRaceExists(String raceId) {
        return races.containsKey(raceId);
    }

    public static FileConfiguration getConfig(String id) {
        return raceConfigs.get(id);
    }

    public static HBRace getRaceById(String raceId) {
        return races.get(raceId);
    }

    public static ArrayList<HeadLocation> getHeads(String raceId, boolean onlyCharged) {
        var headManager = getHeadManager(raceId);

        if (headManager == null) {
            return new ArrayList<>();
        }

        var headLocations = headManager.getHeadLocations();
        if (!onlyCharged) {
            return headLocations;
        }

        return headLocations.stream().filter(HeadLocation::isCharged).collect(Collectors.toCollection(ArrayList::new));
    }

    @Nullable
    public static HeadManager getHeadManager(String raceId) {
        if (!isRaceExists(raceId))
            return null;

        return getRaceById(raceId).getHeadManager();
    }
}