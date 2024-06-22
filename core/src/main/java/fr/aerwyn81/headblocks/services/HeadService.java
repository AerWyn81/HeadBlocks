package fr.aerwyn81.headblocks.services;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NBTTileEntity;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
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
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class HeadService {
    private static File configFile;
    private static YamlConfiguration config;

    private static ArrayList<HBHead> heads;
    private static HashMap<UUID, HeadMove> headMoves;
    private static ArrayList<HeadLocation> headLocations;

    public static String HB_KEY = "HB_HEAD";

    public static void initialize(File file) {
        configFile = file;

        heads = new ArrayList<>();
        headLocations = new ArrayList<>();
        headMoves = new HashMap<>();

        load();
    }

    public static void load() {
        config = YamlConfiguration.loadConfiguration(configFile);

        heads.clear();
        headLocations.clear();
        headMoves.clear();

        loadHeads();
        loadLocations();
    }

    private static void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Cannot save the config file to {0}", configFile.getName());
        }
    }

    public static void loadLocations() {
        headLocations.clear();

        ConfigurationSection locations = config.getConfigurationSection("locations");

        if (locations == null) {
            headLocations = new ArrayList<>();
            return;
        }

        locations.getKeys(false).forEach(uuid -> {
            ConfigurationSection configSection = config.getConfigurationSection("locations." + uuid);

            if (configSection != null) {
                UUID headUuid = UUID.fromString(uuid);

                try {
                    boolean isExist = StorageService.isHeadExist(headUuid);
                    if (!isExist) {
                        StorageService.createNewHead(headUuid, "");
                    }
                } catch (Exception ex) {
                    HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to create a head (" + headUuid + ") in the storage: " + ex.getMessage()));
                    return;
                }

                try {
                    headLocations.add(HeadLocation.fromConfig(config, headUuid));
                } catch (Exception e) {
                    HeadBlocks.log.sendMessage(MessageUtils.colorize("&cCannot deserialize location of head &e" + uuid + "&c. Cause: &e" + e.getMessage()));
                }
            }
        });

        HeadBlocks.log.sendMessage(MessageUtils.colorize("&aLoaded &e" + headLocations.size() + " locations!"));
    }

    public static UUID saveHeadLocation(Location location, String texture) throws InternalException {
        UUID uniqueUuid = InternalUtils.generateNewUUID(headLocations.stream().map(HeadLocation::getUuid).collect(Collectors.toList()));

        StorageService.createNewHead(uniqueUuid, texture);

        if (ConfigService.isHologramsEnabled()) {
            HologramService.createHolograms(location);
        }

        var headLocation = new HeadLocation("", uniqueUuid, location);
        saveHeadInConfig(headLocation);

        headLocations.add(headLocation);
        return uniqueUuid;
    }

    public static void saveHeadInConfig(HeadLocation headLocation) {
        headLocation.saveInConfig(config);
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
        }
    }

    public static HeadLocation getHeadByUUID(UUID headUuid) {
        return headLocations.stream().filter(h -> h.getUuid().equals(headUuid))
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
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError when trying to use heads theme, selected theme is empty or don't match any theme."));
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
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&cInvalid format for " + configHead + " in HBHeads configuration section (l." + i + 1 + ")"));
                continue;
            }

            if (parts[1].trim().isEmpty()) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&cValue cannot be empty for " + configHead + " in HBHeads configuration section (l." + i + 1 + ")"));
                continue;
            }

            ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);

            ItemMeta headMeta = head.getItemMeta();
            if (headMeta == null) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError trying to get meta of the head " + head + ". Is your server version supported?"));
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
                        HeadBlocks.log.sendMessage(MessageUtils.colorize("&cCannot parse the player UUID " + configHead + ". Please provide a correct UUID"));
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
                        HeadBlocks.log.sendMessage(MessageUtils.colorize("&cCannot load hdb head " + configHead + " without HeadDatabase installed"));
                        continue;
                    }

                    head.setItemMeta(headMeta);
                    heads.add(new HBHeadHDB(head, parts[1]));
                    break;
                default:
                    HeadBlocks.log.sendMessage(MessageUtils.colorize("&cThe " + parts[0] + " type is not yet supported!"));
            }
        }

        var headsHdb = heads.stream().filter(HBHeadHDB.class::isInstance).count();

        HeadBlocks.log.sendMessage(MessageUtils.colorize("&aLoaded &e" + (Math.abs(heads.size() - headsHdb)) + " &8&o(+" + headsHdb + " HeadDatabase heads) &aconfiguration heads!"));
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

    public static HashMap<UUID, HeadMove> getHeadMoves() {
        return headMoves;
    }

    public static void clearHeadMoves() {
        if (headMoves == null) {
            return;
        }

        headMoves.clear();
    }

    public static void changeHeadLocation(UUID hUuid, Block oldBlock, Block newBlock) {
        Skull oldSkull = (Skull) oldBlock.getState();
        Rotatable skullRotation = (Rotatable) oldSkull.getBlockData();

        newBlock.setType(Material.PLAYER_HEAD);

        Skull newSkull = (Skull) newBlock.getState();

        Rotatable rotatable = (Rotatable) newSkull.getBlockData();
        rotatable.setRotation(skullRotation.getRotation());
        newSkull.setBlockData(rotatable);

        if (VersionUtils.isNewerThan(VersionUtils.v1_20_R5)) {
            NBT.modify(newSkull, nbt -> {
                nbt.mergeCompound(new NBTTileEntity(oldSkull));
            });
            newSkull.setOwnerProfile(oldSkull.getOwnerProfile());
        } else {
            new NBTTileEntity(newSkull).mergeCompound(new NBTTileEntity(oldSkull));
        }

        newSkull.update(true);

        oldBlock.setType(Material.AIR);

        var headLocation = getHeadByUUID(hUuid);
        var indexOfOld = headLocations.indexOf(headLocation);

        headLocation.setLocation(newBlock.getLocation());
        saveHeadInConfig(headLocation);

        headLocations.set(indexOfOld, headLocation);

        HologramService.removeHolograms(oldBlock.getLocation());
        HologramService.createHolograms(newBlock.getLocation());
    }
}
