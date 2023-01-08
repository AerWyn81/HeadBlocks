package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.head.HBTrack;
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

public class TrackService {
    private static HashMap<String, HBTrack> tracks;
    private static HashMap<String, File> trackFiles;
    private static HashMap<String, YamlConfiguration> trackConfigs;

    public static void initialize() {
        tracks = new HashMap<>();
        trackFiles = new HashMap<>();
        trackConfigs = new HashMap<>();

        File directory = new File(HeadBlocks.getInstance().getDataFolder(), "tracks");
        if (!directory.exists()) {
            directory.mkdir();
        }

        migrateOldLocations();
        loadTracks();
    }

    public static boolean createTrack(String id) {
        File file = new File(HeadBlocks.getInstance().getDataFolder(), "tracks/" + id + ".yml");

        if (!file.exists()) {
            try {
                var fileCreated = file.createNewFile();
                if (!fileCreated) {
                    throw new InternalException("A file with the same id already exist. Should never append.");
                }

                trackFiles.put(id, file);
                trackConfigs.put(id, YamlConfiguration.loadConfiguration(file));

                try {
                    StorageService.createTrack(id);
                } catch (Exception ex) {
                    HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to remove track " + id + " from the storage: " + ex.getMessage()));
                }

            } catch (Exception ex) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while creating track " + id + ": " + ex.getMessage()));
                return false;
            }
        }

        return true;
    }

    public static boolean removeTrack(String id) {
        if (trackFiles.containsKey(id) && trackConfigs.containsKey(id)) {
            File file = trackFiles.get(id);
            var isDeleted = file.delete();
            trackFiles.remove(id);
            trackConfigs.remove(id);

            try {
                StorageService.removeTrack(id);
            } catch (Exception ex) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to remove track " + id + " from the storage: " + ex.getMessage()));
            }

            return isDeleted;
        }

        return false;
    }

    public static void saveConfig(String id) {
        try {
            File file = trackFiles.get(id);
            if (file.exists()) {
                trackConfigs.get(id).save(file);
            }
        } catch (IOException ex) {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while saving track in config: " + ex.getMessage()));
        }
    }

    private static void loadTracks() {
        tracks.clear();
        trackFiles.clear();
        trackConfigs.clear();

        HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks] &eLoading tracks:"));
        File[] allFiles = new File(HeadBlocks.getInstance().getDataFolder(), "tracks").listFiles();

        if (allFiles == null || allFiles.length == 0) {
            return;
        }

        for (File file : allFiles) {
            String id = file.getName().toLowerCase().replace(".yml", "");

            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String name = config.getString("track.name");

            if (!Character.isDigit(id.charAt(0))) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks]   &cCannot load track " + name + " because filename must be a number."));
                continue;
            }

            String description = config.getString("track.description");

            ItemStack iconItem;
            try {
                iconItem = new ItemStack(Material.valueOf(config.getString("track.icon", "PAPER")));
            } catch (Exception ex) {
                iconItem = new ItemStack(Material.PAPER);
            }

            var track = new HBTrack(id, name, description, iconItem);
            track.getHeadManager().loadHeadLocations(config);

            tracks.put(id, track);
            trackFiles.put(id, file);
            trackConfigs.put(id, config);
        }

        if (tracks.size() == 0) {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks]   &cNo track has been loaded!"));
        } else {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks]   &e" + tracks.size() + " &atracks has been loaded!"));
        }
    }

    private static void migrateOldLocations() {
        var locationsFile = new File(HeadBlocks.getInstance().getDataFolder(), "locations.yml");
        if (!locationsFile.exists()) {
            return;
        }

        HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks] &eFound old HeadBlocks locations configuration. Starting migration..."));

        String id;
        File[] allFiles = new File(HeadBlocks.getInstance().getDataFolder(), "tracks").listFiles();
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

        var fileCreated = createTrack(id);
        if (!fileCreated) {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks] &cCritical error, cannot to migrate the old HeadBlocks configuration. Please ask developer. Plugin disabling..."));
            Bukkit.getPluginManager().disablePlugin(HeadBlocks.getInstance());
        }

        var config = YamlConfiguration.loadConfiguration(locationsFile);

        var defaultTrack = new HBTrack(id, "Default", "", new ItemStack(Material.PAPER));
        defaultTrack.getHeadManager().loadHeadLocations(config);
        defaultTrack.saveTrack();

        var file = new File(HeadBlocks.getInstance().getDataFolder(), "locations.yml.backup");
        if (file.exists()) {
            file.delete();
        }

        locationsFile.renameTo(new File(HeadBlocks.getInstance().getDataFolder(), "locations.yml.backup"));

        HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks] &eMigration successfully completed of " +
                defaultTrack.getHeadManager().getHeadLocations().size() + " locations in a default track."));
    }

    private static UUID addHead(String trackId, Location location, String texture) throws InternalException {
        var headManager = getHeadManager(trackId);

        if (headManager == null) {
            throw new RuntimeException("Head cannot be add No track with id: " + trackId);
        }

        return headManager.addHeadLocation(location, texture);
    }

    private static void removeHead(String trackId, HeadLocation headLocation, boolean withDelete) throws InternalException {
        var headManager = getHeadManager(trackId);

        if (headManager == null) {
            throw new RuntimeException("No track with id: " + trackId);
        }

        headManager.removeHeadLocation(headLocation, withDelete);
    }

    public static HashMap<String, HBTrack> getTracks() {
        return tracks;
    }

    public static boolean isTrackExists(String trackId) {
        return tracks.containsKey(trackId);
    }

    public static FileConfiguration getConfig(String id) {
        return trackConfigs.get(id);
    }

    public static HBTrack getTrackById(String trackId) {
        return tracks.get(trackId);
    }

    public static ArrayList<HeadLocation> getHeads(String trackId, boolean onlyCharged) {
        var headManager = getHeadManager(trackId);

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
    public static HeadManager getHeadManager(String trackId) {
        if (!isTrackExists(trackId))
            return null;

        return getTrackById(trackId).getHeadManager();
    }
}