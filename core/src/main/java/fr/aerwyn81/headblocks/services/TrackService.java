package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.api.events.HeadCreatedEvent;
import fr.aerwyn81.headblocks.api.events.HeadDeletedEvent;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.head.HBTrack;
import fr.aerwyn81.headblocks.managers.HeadManager;
import fr.aerwyn81.headblocks.utils.bukkit.ParticlesUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrackService {
    private static HashMap<String, HBTrack> tracks;
    private static HashMap<String, File> trackFiles;
    private static HashMap<String, YamlConfiguration> trackConfigs;

    private static HashMap<UUID, HBTrack> playersTrackChoice;

    public static void initialize() {
        tracks = new HashMap<>();
        trackFiles = new HashMap<>();
        trackConfigs = new HashMap<>();

        File directory = new File(HeadBlocks.getInstance().getDataFolder(), "tracks");
        if (!directory.exists()) {
            directory.mkdir();
        }

        migrateOldLocations();
        load();
    }

    //region Config

    public static FileConfiguration getConfig(String id) {
        return trackConfigs.get(id);
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

    //endregion

    //region Getters

    public static HashMap<UUID, HBTrack> getPlayersTrackChoice() {
        return playersTrackChoice;
    }

    public static Optional<HBTrack> getTrackById(String trackId) {
        return Optional.ofNullable(tracks.get(trackId));
    }

    public static HashMap<String, HBTrack> getTracks() {
        return tracks;
    }

    public static boolean isTrackExists(String trackId) {
        return tracks.containsKey(trackId);
    }

    //endregion

    //region Methods

    public static HBTrack createTrack(String name) {
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

        File file = new File(HeadBlocks.getInstance().getDataFolder(), "tracks/" + id + ".yml");

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ignored) { }

            trackFiles.put(id, file);
            trackConfigs.put(id, YamlConfiguration.loadConfiguration(file));

            try {
                StorageService.createTrack(id);
            } catch (Exception ex) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to create track " + id + " from the storage: " + ex.getMessage()));
            }
        }

        var config = trackConfigs.get(id);
        var manager = new HeadManager(config);

        HBTrack track;
        track = new HBTrack(id, name, new ArrayList<>(), new ItemStack(Material.PAPER), manager);

        manager.setTrack(track);

        track.getHeadManager().loadHeadLocations();

        tracks.put(id, track);

        return track;
    }

    public static boolean removeTrack(String id) {
        if (trackFiles.containsKey(id) && trackConfigs.containsKey(id)) {
            File file = trackFiles.get(id);

            try {
                file.delete();
                StorageService.removeTrack(id);
            } catch (Exception ex) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to remove track " + id + " from the storage: " + ex.getMessage()));
                return false;
            }

            trackFiles.remove(id);
            trackConfigs.remove(id);
            tracks.remove(id);
        }

        return false;
    }

    public static void load() {
        tracks.clear();
        trackFiles.clear();
        trackConfigs.clear();
        playersTrackChoice = new HashMap<>();

        loadTracks();
    }

    private static void loadTracks() {
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

            ArrayList<String> description = new ArrayList<>(config.getStringList("track.description"));

            ItemStack iconItem;
            try {
                iconItem = new ItemStack(Material.valueOf(config.getString("track.icon", "PAPER")));
            } catch (Exception ex) {
                iconItem = new ItemStack(Material.PAPER);
            }

            var manager = new HeadManager(config);

            HBTrack track;
            track = new HBTrack(id, name, description, iconItem, manager);

            manager.setTrack(track);

            track.getHeadManager().loadHeadLocations();

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
        var oldLocationsFile = new File(HeadBlocks.getInstance().getDataFolder(), "locations.yml");
        if (!oldLocationsFile.exists()) {
            return;
        }

        HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks] &eFound old HeadBlocks locations configuration. Starting migration..."));

        HBTrack trackCreated;
        try {
            File file = new File(HeadBlocks.getInstance().getDataFolder(), "tracks/1.yml");

            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException ignored) { }

                trackFiles.put("1", file);
                trackConfigs.put("1", YamlConfiguration.loadConfiguration(file));

                try {
                    StorageService.createTrack("1");
                } catch (Exception ex) {
                    HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to create track 1 from the storage: " + ex.getMessage()));
                }
            }

            var config = YamlConfiguration.loadConfiguration(oldLocationsFile);
            var oldManager = new HeadManager(config);
            oldManager.loadHeadLocations();

            var manager = new HeadManager(trackConfigs.get("1"));
            manager.getHeadLocations().addAll(oldManager.getHeadLocations());

            trackCreated = new HBTrack("1", "&7Default", new ArrayList<>(), new ItemStack(Material.PAPER), manager);

            manager.setTrack(trackCreated);

            trackCreated.saveTrack();
        } catch (Exception ex) {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks] &cCritical error, cannot to migrate the old HeadBlocks configuration. Please ask developer. Plugin disabling..."));
            Bukkit.getPluginManager().disablePlugin(HeadBlocks.getInstance());
            return;
        }

        var file = new File(HeadBlocks.getInstance().getDataFolder(), "locations.yml.backup");
        if (file.exists()) {
            file.delete();
        }

        oldLocationsFile.renameTo(new File(HeadBlocks.getInstance().getDataFolder(), "locations.yml.backup"));

        HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks] &eMigration successfully completed of " +
                trackCreated.getHeadManager().getHeadLocations().size() + " locations in a default track."));
    }

    public static Optional<HeadLocation> getHeadAt(Location location) {
        return getTracks().values().stream()
                .map(HBTrack::getHeadManager)
                .map(headManager -> headManager.getHeadAt(location))
                .flatMap(Optional::stream)
                .findFirst();
    }

    public static void removeHead(Player player, HBTrack track, HeadManager headManager, HeadLocation headLocation) throws InternalException {
        headManager.removeHeadLocation(headLocation, ConfigService.shouldResetPlayerData());
        track.saveTrack();

        Bukkit.getPluginManager().callEvent(new HeadDeletedEvent(headLocation.getUuid(), headLocation.getLocation()));

        player.sendMessage(MessageUtils.parseLocationPlaceholders(LanguageService.getMessage("Messages.HeadRemoved"), headLocation.getLocation()));
    }

    public static UUID addHead(Player player, HBTrack track, Location location, String texture) throws InternalException {
        var headUuid = track.getHeadManager().addHeadLocation(location, texture);
        track.saveTrack();

        Bukkit.getPluginManager().callEvent(new HeadCreatedEvent(headUuid, location));

        ParticlesUtils.spawn(location, Particle.VILLAGER_HAPPY, 10, null, player);
        HologramService.showNotFoundTo(player, location);

        return headUuid;
    }

    //endregion
}