package fr.aerwyn81.headblocks.services;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NBTTileEntity;
import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.HeadMove;
import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.data.head.types.HBHeadDefault;
import fr.aerwyn81.headblocks.data.head.types.HBHeadHDB;
import fr.aerwyn81.headblocks.data.head.types.HBHeadPlayer;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.bukkit.VersionUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.InternalUtils;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.block.data.Rotatable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class HeadService {
    private static File configFile;
    private static YamlConfiguration config;

    private static ArrayList<HBHead> heads;
    private static HashMap<UUID, HeadMove> headMoves;
    private static ArrayList<HeadLocation> headLocations;

    private static HashMap<UUID, BukkitTask> tasksHeadSpin;

    public static String HB_KEY = "HB_HEAD";

    public static void initialize(File file) {
        configFile = file;

        heads = new ArrayList<>();
        headLocations = new ArrayList<>();
        headMoves = new HashMap<>();
        tasksHeadSpin = new HashMap<>();

        load();
    }

    public static void load() {
        config = YamlConfiguration.loadConfiguration(configFile);

        heads.clear();
        headLocations.clear();
        headMoves.clear();
        tasksHeadSpin.values().forEach(BukkitTask::cancel);

        loadHeads();
        loadLocations();
    }

    private static void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            LogUtil.error("Cannot save the config file to {0}", configFile.getName());
        }
    }

    public static void loadLocations() {
        headLocations.clear();

        ConfigurationSection locations = config.getConfigurationSection("locations");

        if (locations == null) {
            headLocations = new ArrayList<>();
            return;
        }

        if (StorageService.hasStorageError()) {
            LogUtil.error("Cannot load locations from storage, theres an issue with the database.");
            return;
        }

        var i = 0;
        for (String uuid : locations.getKeys(false)) {
            i++;

            ConfigurationSection configSection = config.getConfigurationSection("locations." + uuid);

            if (configSection != null) {
                UUID headUuid = UUID.fromString(uuid);

                try {
                    var headLoc = HeadLocation.fromConfig(config, headUuid);

                    try {
                        boolean isExist = StorageService.isHeadExist(headUuid);
                        if (!isExist) {
                            var headTexture = headLoc.getLocation() != null ? HeadUtils.getHeadTexture(headLoc.getLocation().getBlock()) : "";
                            StorageService.createOrUpdateHead(headUuid, headTexture);
                        }
                    } catch (Exception ex) {
                        LogUtil.error("Error while trying to create a head ({0}) in the storage: {1}", headUuid, ex.getMessage());
                        continue;
                    }

                    addHeadToSpin(headLoc, i);

                    headLocations.add(headLoc);
                } catch (Exception e) {
                    LogUtil.error("Cannot deserialize location of head {0}. Cause: {1}", uuid, e.getMessage());
                }
            }
        }

        // Purge for remote database
        if (ConfigService.isDatabaseEnabled()) {
            try {
                var heads = StorageService.getHeadsByServerId();
                if (heads.isEmpty()) {
                    for (var headLoc : getHeadLocations()) {
                        StorageService.createOrUpdateHead(headLoc.getUuid(), HeadUtils.getHeadTexture(headLoc.getLocation().getBlock()));
                    }
                } else {
                    heads.removeAll(getHeadLocations().stream().map(HeadLocation::getUuid).toList());

                    if (!heads.isEmpty()) {
                        LogUtil.error("Found {0} heads ({1}) out of sync with the server, deleting...",
                                heads.size(),
                                String.join(", ", heads.stream().map(UUID::toString).toList()));

                        for (var head : heads) {
                            StorageService.removeHead(head, true);
                        }

                        LogUtil.info("Headblocks heads table cleaned!");
                    }
                }
            } catch (Exception e) {
                LogUtil.error("Error when purging heads out of sync in the database: {0}", e.getMessage());
            }
        }

        LogUtil.info("Loaded {0} locations!", headLocations.size());
    }

    private static void addHeadToSpin(HeadLocation headLoc, int offset) {
        if (!ConfigService.isSpinEnabled() || ConfigService.isSpinLinked()) {
            return;
        }

        var task = Bukkit.getScheduler().runTaskTimer(HeadBlocks.getInstance(),
                () -> rotateHead(headLoc), 5L * offset, ConfigService.getSpinSpeed());
        tasksHeadSpin.put(headLoc.getUuid(), task);
    }

    public static UUID saveHeadLocation(Location location, String texture) throws InternalException {
        UUID uniqueUuid = InternalUtils.generateNewUUID(headLocations.stream().map(HeadLocation::getUuid).collect(Collectors.toList()));

        StorageService.createOrUpdateHead(uniqueUuid, texture);

        if (ConfigService.isHologramsEnabled()) {
            HologramService.createHolograms(location);
        }

        var headLocation = new HeadLocation("", uniqueUuid, location);
        saveHeadInConfig(headLocation);

        headLocations.add(headLocation);
        addHeadToSpin(headLocation, 1);

        return uniqueUuid;
    }

    public static void saveHeadInConfig(HeadLocation headLocation) {
        headLocation.saveInConfig(config);
        saveConfig();
    }

    public static void saveAllHeadsInConfig() {
        for (var headLocation : headLocations) {
            headLocation.saveInConfig(config);
        }

        saveConfig();
    }

    public static void removeHeadLocation(HeadLocation headLocation, boolean withDelete) throws InternalException {
        if (headLocation != null) {
            StorageService.removeHead(headLocation.getUuid(), withDelete);

            headLocation.getLocation().getBlock().setType(Material.AIR);

            if (ConfigService.isHologramsEnabled()) {
                HologramService.removeHolograms(headLocation.getLocation());
            }

            headLocations.remove(headLocation);

            headLocation.removeFromConfig(config);
            saveConfig();

            headMoves.entrySet().removeIf(hM -> headLocation.getUuid().equals(hM.getKey()));
            var spinTask = tasksHeadSpin.get(headLocation.getUuid());
            if (spinTask != null) {
                spinTask.cancel();
                tasksHeadSpin.remove(headLocation.getUuid());
            }
        }
    }

    public static void removeAllHeadLocationsAsync(ArrayList<HeadLocation> headsToRemove, boolean withDelete,
                                                   java.util.function.Consumer<Integer> onComplete) {
        Bukkit.getScheduler().runTaskAsynchronously(HeadBlocks.getInstance(), () -> {
            int removed = 0;

            for (HeadLocation headLocation : headsToRemove) {
                if (headLocation == null) continue;

                try {
                    StorageService.removeHead(headLocation.getUuid(), withDelete);
                } catch (InternalException ex) {
                    LogUtil.error("Error removing head {0} from storage: {1}", headLocation.getNameOrUuid(), ex.getMessage());
                    continue;
                }

                Bukkit.getScheduler().runTask(HeadBlocks.getInstance(), () -> {
                    headLocation.getLocation().getBlock().setType(Material.AIR);

                    if (ConfigService.isHologramsEnabled()) {
                        HologramService.removeHolograms(headLocation.getLocation());
                    }
                });

                headLocations.remove(headLocation);
                headLocation.removeFromConfig(config);

                headMoves.entrySet().removeIf(hM -> headLocation.getUuid().equals(hM.getKey()));
                var spinTask = tasksHeadSpin.get(headLocation.getUuid());
                if (spinTask != null) {
                    spinTask.cancel();
                    tasksHeadSpin.remove(headLocation.getUuid());
                }

                removed++;
            }

            saveConfig();

            final int finalRemoved = removed;
            Bukkit.getScheduler().runTask(HeadBlocks.getInstance(), () -> onComplete.accept(finalRemoved));
        });
    }

    public static HeadLocation getHeadByUUID(UUID headUuid) {
        return headLocations.stream().filter(h -> h.getUuid().equals(headUuid))
                .findFirst()
                .orElse(null);
    }

    public static HeadLocation getHeadByName(String name) {
        return headLocations.stream().filter(h -> h.getRawNameOrUuid().equals(name))
                .findFirst()
                .orElse(null);
    }

    public static HeadLocation getHeadAt(Location location) {
        return headLocations.stream().filter(h -> LocationUtils.areEquals(h.getLocation(), location))
                .findFirst()
                .orElse(null);
    }

    private static void loadHeads() {
        List<String> headsConfig;

        if (ConfigService.isHeadsThemeEnabled()) {
            var selectedTheme = ConfigService.getHeadsThemeSelected().trim();
            var themeHeads = ConfigService.getHeadsTheme().get(selectedTheme);

            if (selectedTheme.isEmpty() || themeHeads == null) {
                LogUtil.error("Error when trying to use heads theme, selected theme is empty or don't match any theme.");
                return;
            }

            headsConfig = themeHeads;
        } else {
            headsConfig = ConfigService.getHeads();
        }

        for (int i = 0; i < headsConfig.size(); i++) {
            String configHead = headsConfig.get(i);
            String[] parts = configHead.split(":");

            if (parts.length != 2) {
                LogUtil.error("Invalid format for {0} in HBHeads configuration section (l.{1})", configHead, (i + 1));
                continue;
            }

            if (parts[1].trim().isEmpty()) {
                LogUtil.error("Value cannot be empty for {0} in HBHeads configuration section (l.{1})", configHead, (i + 1));
                continue;
            }

            ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);

            ItemMeta headMeta = head.getItemMeta();
            if (headMeta == null) {
                LogUtil.error("Error trying to get meta of the head {0}. Is your server version supported?", head);
                continue;
            }

            headMeta.setDisplayName(LanguageService.getMessage("Head.Name"));
            headMeta.setLore(LanguageService.getMessages("Head.Lore"));
            headMeta.getPersistentDataContainer().set(new NamespacedKey(HeadBlocks.getInstance(), HB_KEY), PersistentDataType.STRING, "");

            switch (parts[0]) {
                case "player":
                    OfflinePlayer p;

                    try {
                        p = Bukkit.getOfflinePlayer(parts[1]);
                    } catch (Exception ex) {
                        LogUtil.error("Cannot parse the player UUID {0}. Please provide a correct UUID", configHead);
                        continue;
                    }

                    SkullMeta meta = (SkullMeta) headMeta;
                    meta.setOwningPlayer(p);
                    head.setItemMeta(meta);

                    heads.add(new HBHeadPlayer(head));
                    break;
                case "default":
                    head.setItemMeta(headMeta);
                    heads.add(HeadUtils.createHead(new HBHeadDefault(head), parts[1]));
                    break;
                case "hdb":
                    if (!HeadBlocks.isHeadDatabaseActive) {
                        LogUtil.error("Cannot load hdb head {0} without HeadDatabase installed", configHead);
                        continue;
                    }

                    head.setItemMeta(headMeta);
                    heads.add(new HBHeadHDB(head, parts[1]));
                    break;
                default:
                    LogUtil.error("The {0} type is not yet supported!", parts[0]);
            }
        }

        var headsHdb = heads.stream()
                .filter(HBHeadHDB.class::isInstance).count();

        LogUtil.info("Loaded {0} (+{1} HeadDatabase heads) configuration heads!", Math.abs(heads.size() - headsHdb), headsHdb);
    }

    public static ArrayList<HBHead> getHeads() {
        return heads;
    }

    public static ArrayList<HeadLocation> getChargedHeadLocations() {
        return headLocations.stream().filter(HeadLocation::isCharged).collect(Collectors.toCollection(ArrayList::new));
    }

    public static ArrayList<HeadLocation> getHeadLocations() {
        return headLocations;
    }

    public static ArrayList<String> getHeadRawNameOrUuid() {
        return headLocations.stream().map(HeadLocation::getRawNameOrUuid).collect(Collectors.toCollection(ArrayList::new));
    }

    public static HashMap<UUID, HeadMove> getHeadMoves() {
        return headMoves;
    }

    public static void clearHeadMoves() {
        if (headMoves == null) {
            return;
        }

        headMoves.clear();
    }

    public static HeadLocation resolveHeadIdentifier(String headIdentifier) {
        try {
            var headUuid = UUID.fromString(headIdentifier);
            return getHeadByUUID(headUuid);
        } catch (IllegalArgumentException e) {
            return getHeadByName(headIdentifier);
        }
    }

    public static void changeHeadLocation(UUID hUuid, @NotNull Block oldBlock, Block newBlock) {
        Skull oldSkull = (Skull) oldBlock.getState();
        Rotatable skullRotation = (Rotatable) oldSkull.getBlockData();

        newBlock.setType(Material.PLAYER_HEAD);

        Skull newSkull = (Skull) newBlock.getState();

        Rotatable rotatable = (Rotatable) newSkull.getBlockData();
        rotatable.setRotation(skullRotation.getRotation());
        newSkull.setBlockData(rotatable);

        if (VersionUtils.isNewerOrEqualsTo(VersionUtils.v1_20_R5)) {
            NBT.modify(newSkull, nbt -> {
                nbt.mergeCompound(new NBTTileEntity(oldSkull));
            });
        } else {
            new NBTTileEntity(newSkull).mergeCompound(new NBTTileEntity(oldSkull));
        }
        newSkull.setOwnerProfile(oldSkull.getOwnerProfile());

        newSkull.update(true);

        oldBlock.setType(Material.AIR);

        var headLocation = getHeadByUUID(hUuid);
        var indexOfOld = headLocations.indexOf(headLocation);

        headLocation.setLocation(newBlock.getLocation());
        saveHeadInConfig(headLocation);

        headLocations.set(indexOfOld, headLocation);

        HologramService.removeHolograms(oldBlock.getLocation());
        HologramService.createHolograms(newBlock.getLocation());

        addHeadToSpin(headLocation, 1);
    }

    public static void rotateHead(HeadLocation headLocation) {
        var block = headLocation.getLocation().getBlock();
        if (block.getType() != Material.PLAYER_HEAD) {
            return;
        }

        var currentRotation = InternalUtils.getKeyByValue(HeadUtils.skullRotationList, HeadUtils.getRotation(block));
        if (currentRotation == null) {
            currentRotation = 0;
        }

        var rotation = HeadUtils.skullRotationList.get((currentRotation + 1) % HeadUtils.skullRotationList.size());
        HeadUtils.rotateHead(block, rotation);
    }
}
