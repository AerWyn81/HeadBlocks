package fr.aerwyn81.headblocks.handlers;

import de.tr7zw.changeme.nbtapi.NBTTileEntity;
import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.HeadMove;
import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.data.head.types.HBHeadDefault;
import fr.aerwyn81.headblocks.data.head.types.HBHeadHDB;
import fr.aerwyn81.headblocks.data.head.types.HBHeadPlayer;
import fr.aerwyn81.headblocks.utils.HeadUtils;
import fr.aerwyn81.headblocks.utils.InternalException;
import fr.aerwyn81.headblocks.utils.LocationUtils;
import fr.aerwyn81.headblocks.utils.MessageUtils;
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

public class HeadHandler {

    private final HeadBlocks main;
    private final LanguageHandler languageHandler;
    private final StorageHandler storageHandler;
    private final HologramHandler hologramHandler;

    private final File configFile;
    private YamlConfiguration config;

    private final ArrayList<HBHead> heads;
    private final HashMap<UUID, HeadMove> headMoves;
    private ArrayList<HeadLocation> headLocations;

    public static String HB_KEY = "HB_HEAD";

    public HeadHandler(HeadBlocks main, File configFile) {
        this.main = main;
        this.configFile = configFile;
        this.languageHandler = main.getLanguageHandler();
        this.storageHandler = main.getStorageHandler();
        this.hologramHandler = main.getHologramHandler();

        this.heads = new ArrayList<>();
        this.headLocations = new ArrayList<>();
        this.headMoves = new HashMap<>();
    }

    public void loadConfiguration() {
        config = YamlConfiguration.loadConfiguration(configFile);

        heads.clear();
        loadHeads();
    }

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Cannot save the config file to {0}", configFile.getName());
        }
    }

    public void loadLocations() {
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
                    boolean isExist = storageHandler.isHeadExist(headUuid);
                    if (!isExist) {
                        storageHandler.createNewHead(headUuid);
                    }
                } catch (Exception ex) {
                    HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to create a head (" + headUuid + ") in the storage: " + ex.getMessage()));
                    return;
                }

                try {
                    var headLocation = HeadLocation.fromConfig(config, headUuid);
                    headLocations.add(headLocation);

                    if (main.getConfigHandler().isHologramsEnabled()) {
                        hologramHandler.createHolograms(headLocation.getLocation());
                    }
                } catch (Exception e) {
                    HeadBlocks.log.sendMessage(MessageUtils.colorize("&cCannot deserialize location of head " + uuid));
                }
            }
        });
    }

    public UUID saveHeadLocation(Location location) throws InternalException {
        UUID uniqueUuid = UUID.randomUUID();
        while (getHeadByUUID(uniqueUuid) != null) {
            uniqueUuid = UUID.randomUUID();
        }

        storageHandler.createNewHead(uniqueUuid);

        if (main.getConfigHandler().isHologramsEnabled()) {
            hologramHandler.createHolograms(location);
        }

        var headLocation = new HeadLocation("", uniqueUuid, location);
        headLocation.saveInConfig(config);
        saveConfig();

        headLocations.add(headLocation);
        return uniqueUuid;
    }

    public void removeHeadLocation(HeadLocation headLocation, boolean withDelete) throws InternalException {
        if (headLocation != null) {
            storageHandler.removeHead(headLocation.getUuid(), withDelete);

            headLocation.getLocation().getBlock().setType(Material.AIR);

            if (main.getConfigHandler().isHologramsEnabled()) {
                hologramHandler.removeHologram(headLocation.getLocation());
            }

            headLocations.remove(headLocation);

            headLocation.removeFromConfig(config);
            saveConfig();

            headMoves.entrySet().removeIf(hM -> headLocation.getUuid().equals(hM.getKey()));
        }
    }

    public HeadLocation getHeadByUUID(UUID headUuid) {
        return headLocations.stream().filter(h -> h.getUuid().equals(headUuid))
                .findFirst()
                .orElse(null);
    }

    public HeadLocation getHeadAt(Location location) {
        return headLocations.stream().filter(h -> LocationUtils.areEquals(h.getLocation(), location))
                .findFirst()
                .orElse(null);
    }

    private void loadHeads() {
        List<String> headsConfig = main.getConfigHandler().getHeads();

        for (int i = 0; i < headsConfig.size(); i++) {
            String configHead = headsConfig.get(i);
            String[] parts = configHead.split(":");

            if (parts.length != 2) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&cInvalid format for " + configHead + " in HBHeads configuration section (l." + i + 1 + ")"));
                continue;
            }

            if (parts[1].trim().equals("")) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&cValue cannot be empty for " + configHead + " in HBHeads configuration section (l." + i + 1 + ")"));
                continue;
            }

            ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);

            ItemMeta headMeta = head.getItemMeta();
            if (headMeta == null) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError trying to get meta of the head " + head + ". Is your server version supported?"));
                continue;
            }

            headMeta.setDisplayName(languageHandler.getMessage("Head.Name"));
            headMeta.setLore(languageHandler.getMessages("Head.Lore"));
            headMeta.getPersistentDataContainer().set(new NamespacedKey(main, HB_KEY), PersistentDataType.STRING, "");

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

    public HashMap<UUID, HeadMove> getHeadMoves() {
        return headMoves;
    }

    public void changeHeadLocation(UUID hUuid, Block oldBlock, Block newBlock) {
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

        var headLocation = getHeadByUUID(hUuid);
        var indexOfOld = headLocations.indexOf(headLocation);

        headLocation.setLocation(newBlock.getLocation());
        headLocation.saveInConfig(config);
        saveConfig();

        headLocations.set(indexOfOld, headLocation);

        hologramHandler.removeHologram(oldBlock.getLocation());
        hologramHandler.createHolograms(newBlock.getLocation());
    }
}
