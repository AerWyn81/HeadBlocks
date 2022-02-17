package fr.aerwyn81.headblocks.handlers;

import de.tr7zw.changeme.nbtapi.NBTItem;
import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.head.Head;
import fr.aerwyn81.headblocks.data.head.HeadType;
import fr.aerwyn81.headblocks.utils.FormatUtils;
import fr.aerwyn81.headblocks.utils.HeadUtils;
import fr.aerwyn81.headblocks.utils.Version;
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
import org.javatuples.Pair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class HeadHandler {

    private final HeadBlocks main;

    private final File configFile;
    private final LanguageHandler languageHandler;
    private FileConfiguration config;

    private final ArrayList<Head> heads;
    private ArrayList<Pair<UUID, Location>> headLocations;

    public HeadHandler(HeadBlocks main, File configFile) {
        this.main = main;
        this.configFile = configFile;
        this.languageHandler = main.getLanguageHandler();

        this.heads = new ArrayList<>();
        this.headLocations = new ArrayList<>();
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
            Bukkit.getLogger().log(Level.SEVERE, "Cannot save to {0}", configFile.getName());
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
                Map<String, Object> serializedLoc = configSection.getValues(false);

                try {
                    headLocations.add(new Pair<>(UUID.fromString(uuid), Location.deserialize(serializedLoc)));
                } catch (Exception e) {
                    HeadBlocks.log.sendMessage(FormatUtils.translate("&cCannot deserialize location of head " + uuid));
                }
            }
        });
    }

    public UUID addLocation(Location location) {
        UUID uniqueUuid = UUID.randomUUID();
        while (getHeadByUUID(uniqueUuid) != null) {
            uniqueUuid = UUID.randomUUID();
        }

        headLocations.add(new Pair<>(uniqueUuid, location));
        saveLocations();
        saveConfig();

        return uniqueUuid;
    }

    public void removeHead(UUID headUuid) {
        Pair<UUID, Location> head = getHeadByUUID(headUuid);

        if (head != null) {
            head.getValue1().getBlock().setType(Material.AIR);

            headLocations.remove(head);
            saveLocations();
            saveConfig();
        }
    }

    public Pair<UUID, Location> getHeadByUUID(UUID headUuid) {
        return headLocations.stream().filter(p -> p.getValue0().equals(headUuid)).findFirst().orElse(null);
    }

    public UUID getHeadAt(Location location) {
        return headLocations.stream().filter(l -> HeadUtils.areEquals(l.getValue1(), location))
                .map(Pair::getValue0)
                .findFirst().orElse(null);
    }

    public void saveLocations() {
        config.set("locations", new ArrayList<>());

        headLocations.forEach(key -> config.set("locations." + key.getValue0().toString(), key.getValue1().serialize()));

        saveConfig();
    }

    private void loadHeads() {
        List<String> headsConfig = main.getConfigHandler().getHeads();

        for (int i = 0; i < headsConfig.size(); i++) {
            String configHead = headsConfig.get(i);
            String[] parts = configHead.split(":");

            if (parts.length != 2) {
                HeadBlocks.log.sendMessage(FormatUtils.translate("&cInvalid format for " + configHead + " in heads configuration section (l." + i + 1 + ")"));
                continue;
            }

            if (parts[1].trim().equals("")) {
                HeadBlocks.log.sendMessage(FormatUtils.translate("&cValue cannot be empty for " + configHead + " in heads configuration section (l." + i + 1 + ")"));
                continue;
            }

            ItemStack head;

            if (Version.getCurrent().isOlderOrSameThan(Version.v1_12)) {
                head = new ItemStack(Material.valueOf("SKULL_ITEM"), 1, (short) 3);
            } else {
                head = new ItemStack(Material.PLAYER_HEAD, 1);
            }

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
                        p = Bukkit.getOfflinePlayer(UUID.fromString(parts[1]));
                    } catch (Exception ex) {
                        HeadBlocks.log.sendMessage(FormatUtils.translate("&cCannot parse the player UUID " + configHead + ". Please provide a correct UUID"));
                        continue;
                    }

                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    meta.setOwningPlayer(p);
                    head.setItemMeta(meta);

                    heads.add(new Head(null, head, null, HeadType.PLAYER));
                    break;
                case "default":
                    heads.add(HeadUtils.applyTexture(new Head(null, head, parts[1], HeadType.DEFAULT)));
                    break;
                case "hdb":
                    if (!HeadBlocks.isHeadDatabaseActive) {
                       HeadBlocks.log.sendMessage(FormatUtils.translate("&cCannot load hdb head type " + configHead + " without HeadDatabase installed"));
                       continue;
                    }

                    heads.add(new Head(parts[1], head, null, HeadType.HDB));
                    break;
                default:
                    HeadBlocks.log.sendMessage(FormatUtils.translate("&cUnknown type " + parts[0] + " in heads configuration section"));
            }
        }
    }

    public ArrayList<Head> getHeads() {
        return heads;
    }

    public ArrayList<Pair<UUID, Location>> getHeadLocations() {
        return headLocations;
    }
}
