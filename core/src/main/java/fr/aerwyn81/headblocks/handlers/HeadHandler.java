package fr.aerwyn81.headblocks.handlers;

import de.tr7zw.changeme.nbtapi.NBTItem;
import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.data.head.types.HBHeadDefault;
import fr.aerwyn81.headblocks.data.head.types.HBHeadHDB;
import fr.aerwyn81.headblocks.data.head.types.HBHeadPlayer;
import fr.aerwyn81.headblocks.utils.HeadUtils;
import fr.aerwyn81.headblocks.utils.InternalException;
import fr.aerwyn81.headblocks.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class HeadHandler {

    private final HeadBlocks main;
    private final LanguageHandler languageHandler;
    private final StorageHandler storageHandler;

    private final File configFile;
    private FileConfiguration config;

    private final ArrayList<HBHead> heads;
    private LinkedHashMap<UUID, Location> headLocations;

    public HeadHandler(HeadBlocks main, File configFile) {
        this.main = main;
        this.configFile = configFile;
        this.languageHandler = main.getLanguageHandler();
        this.storageHandler = main.getStorageHandler();

        this.heads = new ArrayList<>();
        this.headLocations = new LinkedHashMap<>();
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
            headLocations = new LinkedHashMap<>();
            return;
        }

        locations.getKeys(false).forEach(uuid -> {
            ConfigurationSection configSection = config.getConfigurationSection("locations." + uuid);

            if (configSection != null) {
                Map<String, Object> serializedLoc = configSection.getValues(false);

                try {
                    headLocations.put(UUID.fromString(uuid), Location.deserialize(serializedLoc));
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

        headLocations.put(uniqueUuid, location);
        saveLocations();
        saveConfig();

        return uniqueUuid;
    }

    public void removeHeadLocation(UUID headUuid, boolean withDelete) throws InternalException {
        Map.Entry<UUID, Location> head = getHeadByUUID(headUuid);

        if (head != null) {
            storageHandler.removeHead(head.getKey(), withDelete);

            head.getValue().getBlock().setType(Material.AIR);

            headLocations.remove(head.getKey());
            saveLocations();
            saveConfig();
        }
    }

    public void saveLocations() {
        config.set("locations", new ArrayList<>());

        headLocations.forEach((uuid, location) -> config.set("locations." + uuid.toString(), location.serialize()));
        saveConfig();
    }

    public Map.Entry<UUID, Location> getHeadByUUID(UUID headUuid) {
        return headLocations.entrySet().stream().filter(p -> p.getKey().equals(headUuid)).findFirst().orElse(null);
    }

    public UUID getHeadAt(Location location) {
        return headLocations.entrySet().stream()
                .filter(l -> HeadUtils.areEquals(l.getValue(), location))
                .findFirst()
                .map(Map.Entry::getKey)
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
            headMeta.setDisplayName(languageHandler.getMessage("Head.Name"));
            headMeta.setLore(languageHandler.getMessages("Head.Lore"));

            head.setItemMeta(headMeta);

            NBTItem nbtItem = new NBTItem(head, true);
            nbtItem.setBoolean("HB_HEAD", true);

            switch (parts[0]) {
                case "player":
                    OfflinePlayer p;

                    try {
                        p = Bukkit.getOfflinePlayer(parts[1]);
                    } catch (Exception ex) {
                        HeadBlocks.log.sendMessage(MessageUtils.colorize("&cCannot parse the player UUID " + configHead + ". Please provide a correct UUID"));
                        continue;
                    }

                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    meta.setOwningPlayer(p);
                    head.setItemMeta(meta);

                    heads.add(new HBHeadPlayer(head));
                    break;
                case "default":
                    heads.add(HeadUtils.applyTexture(new HBHeadDefault(head), parts[1]));
                    break;
                case "hdb":
                    if (!HeadBlocks.isHeadDatabaseActive) {
                        HeadBlocks.log.sendMessage(MessageUtils.colorize("&cCannot load hdb head type " + configHead + " without HeadDatabase installed"));
                        continue;
                    }

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

    public LinkedHashMap<UUID, Location> getHeadLocations() {
        return headLocations;
    }
}
