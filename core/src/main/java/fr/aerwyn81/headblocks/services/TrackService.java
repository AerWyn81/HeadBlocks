package fr.aerwyn81.headblocks.services;

import de.tr7zw.changeme.nbtapi.NBTTileEntity;
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
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.block.data.Rotatable;
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

    public static Optional<HBTrack> getTrackByName(String track) {
        return tracks.values().stream().filter(t -> t.getName().equals(track)).findFirst();
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

        try {
            if (!file.exists()) {
                file.createNewFile();
            }

            StorageService.createTrack(id, name);
        } catch (Exception ex) {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to create track " + id + " from the storage: " + ex.getMessage()));
            return null;
        }

        trackFiles.put(id, file);
        trackConfigs.put(id, YamlConfiguration.loadConfiguration(file));

        var config = trackConfigs.get(id);
        var manager = new HeadManager(config);

        HBTrack track;
        track = new HBTrack(id, MessageUtils.uncolorize(name), name, new ArrayList<>(), new ItemStack(Material.PAPER), manager);

        manager.setTrack(track);

        track.getHeadManager().loadHeadLocations();

        tracks.put(id, track);

        return track;
    }

    public static boolean removeTrack(HBTrack track) {
        var id = track.getId();

        if (trackFiles.containsKey(id) && trackConfigs.containsKey(id)) {
            File file = trackFiles.get(id);

            try {
                file.delete();
                StorageService.removeTrack(id);
            } catch (Exception ex) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to remove track " + id + " from the storage: " + ex.getMessage()));
                return false;
            }

            for (HeadLocation headLocation : new ArrayList<>(track.getHeadManager().getHeadLocations())) {
                try {
                    track.getHeadManager().removeHeadLocation(headLocation);
                } catch (Exception ex) {
                    HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to remove head " + headLocation.getUuid() + ": " + ex.getMessage()));
                    return false;
                }
            }

            trackFiles.remove(id);
            trackConfigs.remove(id);
            tracks.remove(id);
        }

        return true;
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

            if (!Character.isDigit(id.charAt(0))) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks]   &cCannot load track " + id + " because filename must be a number"));
                continue;
            }

            HBTrack track = HBTrack.fromConfig(config, id);
            if (track.getName() == null) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks]   &cCannot load track " + id + " because name is null"));
                continue;
            }

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
                    StorageService.createTrack("1", "Default");
                } catch (Exception ex) {
                    HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to create track 1 from the storage: " + ex.getMessage()));
                }
            }

            var config = YamlConfiguration.loadConfiguration(oldLocationsFile);
            var oldManager = new HeadManager(config);
            oldManager.loadHeadLocations();

            var manager = new HeadManager(trackConfigs.get("1"));
            manager.getHeadLocations().addAll(oldManager.getHeadLocations());

            trackCreated = new HBTrack("1", "Default", "&7Default", new ArrayList<>(), new ItemStack(Material.PAPER), manager);

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
                trackCreated.getHeadCount() + " locations in a default track."));
    }

    public static Optional<HeadLocation> getHeadAt(Location location) {
        return getTracks().values().stream()
                .map(HBTrack::getHeadManager)
                .map(headManager -> headManager.getHeadAt(location))
                .flatMap(Optional::stream)
                .findFirst();
    }

    public static Optional<HeadLocation> getHeadByUUID(UUID uuid) {
        return getTracks().values().stream()
                .map(HBTrack::getHeadManager)
                .map(headManager -> headManager.getHeadByUUID(uuid))
                .flatMap(Optional::stream)
                .findFirst();
    }

    public static void removeHead(HeadLocation headLocation) throws InternalException {
        var headManager = headLocation.getHeadManager();
        headManager.removeHeadLocation(headLocation);
        headManager.getTrack().saveTrack();

        Bukkit.getPluginManager().callEvent(new HeadDeletedEvent(headLocation.getUuid(), headLocation.getLocation()));
    }

    public static UUID addHead(Player player, HBTrack track, Location location, String texture) throws InternalException {
        var headUuid = track.getHeadManager().addHeadLocation(location, texture);
        track.saveTrack();

        Bukkit.getPluginManager().callEvent(new HeadCreatedEvent(headUuid, location));

        ParticlesUtils.spawn(location, Particle.VILLAGER_HAPPY, 10, null, player);
        HologramService.showNotFoundTo(player, location);

        return headUuid;
    }

    public static void changeHeadLocation(HeadLocation headLocation, Block oldBlock, Block newBlock, String trackId) {
        Skull oldSkull = (Skull) oldBlock.getState();
        Rotatable skullRotation = (Rotatable) oldSkull.getBlockData();

        newBlock.setType(Material.PLAYER_HEAD);

        Skull newSkull = (Skull) newBlock.getState();

        Rotatable rotatable = (Rotatable) newSkull.getBlockData();
        rotatable.setRotation(skullRotation.getRotation());
        newSkull.setBlockData(rotatable);
        newSkull.update(true);

        new NBTTileEntity(newSkull).mergeCompound(new NBTTileEntity(oldSkull));

        oldBlock.setType(Material.AIR);

        if (trackId == null) {
            headLocation.setLocation(newBlock.getLocation());
            headLocation.getHeadManager().getTrack().saveTrack();
        } else {
            var optTrack = TrackService.getTrackById(trackId);

            if (optTrack.isPresent()) {
                var track = optTrack.get();

                headLocation.getHeadManager().getHeadLocations().remove(headLocation);
                headLocation.getHeadManager().getTrack().saveTrack();

                track.getHeadManager().getHeadLocations().add(headLocation);
                track.getHeadManager().saveHeadLocations();
            }
        }

        HologramService.removeHolograms(oldBlock.getLocation());
        HologramService.createHolograms(newBlock.getLocation());
    }

    //endregion
}