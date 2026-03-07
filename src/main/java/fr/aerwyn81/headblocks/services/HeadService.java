package fr.aerwyn81.headblocks.services;

import de.tr7zw.changeme.nbtapi.NBT;
import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.HeadMove;
import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.data.head.types.HBHeadDefault;
import fr.aerwyn81.headblocks.data.head.types.HBHeadHDB;
import fr.aerwyn81.headblocks.data.head.types.HBHeadPlayer;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.bukkit.PluginProvider;
import fr.aerwyn81.headblocks.utils.bukkit.SchedulerAdapter;
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
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class HeadService {
    private final ConfigService configService;
    private final StorageService storageService;
    private final LanguageService languageService;
    private final SchedulerAdapter scheduler;
    private final PluginProvider pluginProvider;
    private HologramService hologramService; // setter-injected (circular dep)
    private HuntService huntService; // setter-injected

    private File configFile;
    private YamlConfiguration config;

    private ArrayList<HBHead> heads;
    private HashMap<UUID, HeadMove> headMoves;
    private ArrayList<HeadLocation> headLocations;
    private HashMap<UUID, Integer> tasksHeadSpin;
    private boolean savePending;

    public static String HB_KEY = "HB_HEAD";

    // --- Constructor + instance lifecycle ---

    public HeadService(ConfigService configService, StorageService storageService,
                       LanguageService languageService, SchedulerAdapter scheduler,
                       PluginProvider pluginProvider) {
        this.configService = configService;
        this.storageService = storageService;
        this.languageService = languageService;
        this.scheduler = scheduler;
        this.pluginProvider = pluginProvider;
    }

    public void setHologramService(HologramService hologramService) {
        this.hologramService = hologramService;
    }

    public void setHuntService(HuntService huntService) {
        this.huntService = huntService;
    }

    // --- Instance lifecycle ---

    public void initialize(File file) {
        configFile = file;

        heads = new ArrayList<>();
        headLocations = new ArrayList<>();
        headMoves = new HashMap<>();
        tasksHeadSpin = new HashMap<>();

        load();
    }

    public void load() {
        config = YamlConfiguration.loadConfiguration(configFile);

        heads.clear();
        headLocations.clear();
        headMoves.clear();
        cancelAllSpinTasks();

        loadHeads();
        loadLocations();
    }

    private void cancelAllSpinTasks() {
        if (tasksHeadSpin != null) {
            tasksHeadSpin.values().forEach(scheduler::cancelTask);
        }
    }

    private void saveConfig() {
        if (savePending) {
            return;
        }
        savePending = true;
        scheduler.runTaskLater(() -> {
            savePending = false;
            String yamlContent = config.saveToString();
            scheduler.runTaskAsync(() -> {
                try {
                    Files.writeString(configFile.toPath(), yamlContent);
                } catch (Exception e) {
                    LogUtil.error("Cannot save the config file to {0}: {1}", configFile.getName(), e.getMessage());
                }
            });
        }, 1L);
    }

    public void loadLocations() {
        headLocations.clear();

        ConfigurationSection locations = config.getConfigurationSection("locations");

        if (locations == null) {
            headLocations = new ArrayList<>();
            return;
        }

        if (storageService.isStorageError()) {
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
                        boolean isExist = storageService.isHeadExist(headUuid);
                        if (!isExist) {
                            var headTexture = headLoc.getLocation() != null ? HeadUtils.getHeadTexture(headLoc.getLocation().getBlock()) : "";
                            storageService.createOrUpdateHead(headUuid, headTexture);
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
        if (configService.databaseEnabled()) {
            try {
                var dbHeads = storageService.getHeadsByServerId();
                if (dbHeads.isEmpty()) {
                    for (var headLoc : getHeadLocations()) {
                        storageService.createOrUpdateHead(headLoc.getUuid(), HeadUtils.getHeadTexture(headLoc.getLocation().getBlock()));
                    }
                } else {
                    dbHeads.removeAll(getHeadLocations().stream().map(HeadLocation::getUuid).toList());

                    if (!dbHeads.isEmpty()) {
                        LogUtil.error("Found {0} heads ({1}) out of sync with the server, deleting...",
                                dbHeads.size(),
                                String.join(", ", dbHeads.stream().map(UUID::toString).toList()));

                        for (var head : dbHeads) {
                            storageService.removeHead(head, true);
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

    private void addHeadToSpin(HeadLocation headLoc, int offset) {
        if (!configService.spinEnabled() || configService.spinLinked()) {
            return;
        }

        var taskId = scheduler.runTaskTimer(
                () -> rotateHead(headLoc), 5L * offset, configService.spinSpeed());
        tasksHeadSpin.put(headLoc.getUuid(), taskId);
    }

    public UUID saveHeadLocation(Location location, String texture) throws InternalException {
        UUID uniqueUuid = InternalUtils.generateNewUUID(headLocations.stream().map(HeadLocation::getUuid).collect(Collectors.toList()));

        storageService.createOrUpdateHead(uniqueUuid, texture);

        if (configService.hologramsEnabled() && hologramService != null) {
            hologramService.createHolograms(location);
        }

        var headLocation = new HeadLocation("", uniqueUuid, location);
        saveHeadInConfig(headLocation);

        headLocations.add(headLocation);
        addHeadToSpin(headLocation, 1);

        return uniqueUuid;
    }

    public void saveHeadInConfig(HeadLocation headLocation) {
        headLocation.saveInConfig(config);
        saveConfig();
    }

    public void saveAllHeadsInConfig() {
        for (var headLocation : headLocations) {
            headLocation.saveInConfig(config);
        }

        saveConfig();
    }

    public void removeHeadLocation(HeadLocation headLocation, boolean withDelete) throws InternalException {
        if (headLocation != null) {
            storageService.removeHead(headLocation.getUuid(), withDelete);

            headLocation.getLocation().getBlock().setType(Material.AIR);

            if (configService.hologramsEnabled() && hologramService != null) {
                hologramService.removeHolograms(headLocation.getLocation());
            }

            for (Hunt hunt : huntService.getAllHunts()) {
                hunt.removeHead(headLocation.getUuid());
            }
            huntService.rebuildHeadToHuntsCache();

            headLocations.remove(headLocation);

            headLocation.removeFromConfig(config);
            saveConfig();

            headMoves.entrySet().removeIf(hM -> headLocation.getUuid().equals(hM.getKey()));
            var spinTaskId = tasksHeadSpin.get(headLocation.getUuid());
            if (spinTaskId != null) {
                scheduler.cancelTask(spinTaskId);
                tasksHeadSpin.remove(headLocation.getUuid());
            }
        }
    }

    public void removeAllHeadLocationsAsync(ArrayList<HeadLocation> headsToRemove, boolean withDelete,
                                            java.util.function.Consumer<Integer> onComplete) {
        scheduler.runTaskAsync(() -> {
            int removed = 0;

            for (HeadLocation headLocation : headsToRemove) {
                if (headLocation == null) {
                    continue;
                }

                try {
                    storageService.removeHead(headLocation.getUuid(), withDelete);
                } catch (InternalException ex) {
                    LogUtil.error("Error removing head {0} from storage: {1}", headLocation.getNameOrUuid(), ex.getMessage());
                    continue;
                }

                scheduler.runTask(() -> {
                    headLocation.getLocation().getBlock().setType(Material.AIR);

                    if (configService.hologramsEnabled() && hologramService != null) {
                        hologramService.removeHolograms(headLocation.getLocation());
                    }
                });

                headLocations.remove(headLocation);
                headLocation.removeFromConfig(config);

                headMoves.entrySet().removeIf(hM -> headLocation.getUuid().equals(hM.getKey()));
                var spinTaskId = tasksHeadSpin.get(headLocation.getUuid());
                if (spinTaskId != null) {
                    scheduler.cancelTask(spinTaskId);
                    tasksHeadSpin.remove(headLocation.getUuid());
                }

                removed++;
            }

            saveConfig();

            final int finalRemoved = removed;
            scheduler.runTask(() -> {
                for (Hunt hunt : huntService.getAllHunts()) {
                    for (HeadLocation hl : headsToRemove) {
                        if (hl != null) {
                            hunt.removeHead(hl.getUuid());
                        }
                    }
                }
                huntService.rebuildHeadToHuntsCache();

                onComplete.accept(finalRemoved);
            });
        });
    }

    public HeadLocation getHeadByUUID(UUID headUuid) {
        return headLocations.stream().filter(h -> h.getUuid().equals(headUuid))
                .findFirst()
                .orElse(null);
    }

    public HeadLocation getHeadByName(String name) {
        return headLocations.stream().filter(h -> h.getRawNameOrUuid().equals(name))
                .findFirst()
                .orElse(null);
    }

    public HeadLocation getHeadAt(Location location) {
        return headLocations.stream().filter(h -> LocationUtils.areEquals(h.getLocation(), location))
                .findFirst()
                .orElse(null);
    }

    private void loadHeads() {
        List<String> headsConfig;

        if (configService.headsThemeEnabled()) {
            var selectedTheme = configService.headsThemeSelected().trim();
            var themeHeads = configService.headsTheme().get(selectedTheme);

            if (selectedTheme.isEmpty() || themeHeads == null) {
                LogUtil.error("Error when trying to use heads theme, selected theme is empty or don't match any theme.");
                return;
            }

            headsConfig = themeHeads;
        } else {
            headsConfig = configService.heads();
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

            headMeta.setDisplayName(languageService.message("Head.Name"));
            headMeta.setLore(languageService.messageList("Head.Lore"));
            headMeta.getPersistentDataContainer().set(new NamespacedKey(HeadBlocks.getInstance(), HB_KEY), PersistentDataType.STRING, "");

            switch (parts[0]) {
                case "player":
                    OfflinePlayer p;

                    try {
                        p = Bukkit.getOfflinePlayer(UUID.fromString(parts[1]));
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
                    if (!pluginProvider.isHeadDatabaseActive()) {
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

    public ArrayList<HBHead> getHeads() {
        return heads;
    }

    public ArrayList<HeadLocation> getChargedHeadLocations() {
        return headLocations.stream().filter(HeadLocation::isCharged).collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<HeadLocation> getHeadLocations() {
        return headLocations;
    }

    public ArrayList<HeadLocation> getHeadLocationsForHunt(Hunt hunt) {
        return headLocations.stream()
                .filter(h -> hunt.containsHead(h.getUuid()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<String> getHeadRawNameOrUuid() {
        return headLocations.stream().map(HeadLocation::getRawNameOrUuid).collect(Collectors.toCollection(ArrayList::new));
    }

    public HashMap<UUID, HeadMove> getHeadMoves() {
        return headMoves;
    }

    public void clearHeadMoves() {
        if (headMoves == null) {
            return;
        }

        headMoves.clear();
    }

    public HeadLocation resolveHeadIdentifier(String headIdentifier) {
        try {
            var headUuid = UUID.fromString(headIdentifier);
            return getHeadByUUID(headUuid);
        } catch (IllegalArgumentException e) {
            return getHeadByName(headIdentifier);
        }
    }

    public void changeHeadLocation(UUID hUuid, @NotNull Block oldBlock, Block newBlock) {
        Skull oldSkull = (Skull) oldBlock.getState();
        Rotatable skullRotation = (Rotatable) oldSkull.getBlockData();

        newBlock.setType(Material.PLAYER_HEAD);

        Skull newSkull = (Skull) newBlock.getState();

        Rotatable rotatable = (Rotatable) newSkull.getBlockData();
        rotatable.setRotation(skullRotation.getRotation());
        newSkull.setBlockData(rotatable);

        NBT.modify(newSkull, nbt -> {
            NBT.get(oldSkull, oldNbt -> {
                nbt.mergeCompound(oldNbt);
                return null;
            });
        });

        newSkull.setOwnerProfile(oldSkull.getOwnerProfile());

        newSkull.update(true);

        oldBlock.setType(Material.AIR);

        var headLocation = getHeadByUUID(hUuid);
        var indexOfOld = headLocations.indexOf(headLocation);

        var centeredLoc = newBlock.getLocation().clone().add(0.5, 0, 0.5);

        headLocation.setLocation(centeredLoc);
        saveHeadInConfig(headLocation);

        headLocations.set(indexOfOld, headLocation);

        if (hologramService != null) {
            hologramService.removeHolograms(oldBlock.getLocation());
            hologramService.createHolograms(centeredLoc);
        }

        addHeadToSpin(headLocation, 1);
    }

    public void rotateHead(HeadLocation headLocation) {
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
