package fr.aerwyn81.headblocks.handlers;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;
import de.tr7zw.changeme.nbtapi.NBTListCompound;
import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.utils.FormatUtils;
import fr.aerwyn81.headblocks.utils.Version;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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

    private final ArrayList<ItemStack> heads;
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
        loadHeads();

        Bukkit.getScheduler().runTaskLater(main, this::loadLocations, 1L);
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
        return headLocations.stream().filter(l -> l.getValue1().getBlockX() == location.getBlockX()
                        && l.getValue1().getBlockY() == location.getBlockY()
                        && l.getValue1().getBlockZ() == location.getBlockZ()
                        && l.getValue1().getWorld() != null && location.getWorld() != null &&
                        l.getValue1().getWorld().getName().equals(location.getWorld().getName()))
                .map(Pair::getValue0)
                .findFirst().orElse(null);
    }

    public void saveLocations() {
        config.set("locations", new ArrayList<>());

        headLocations.forEach(key -> config.set("locations." + key.getValue0().toString(), key.getValue1().serialize()));

        saveConfig();
    }

    private void loadHeads() {
        heads.clear();

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

            NBTItem nbti = new NBTItem(head);
            NBTCompound skull = nbti.addCompound("SkullOwner");
            skull.setString("Name", "HeadBlocks");

            if (Version.getCurrent().isOlderOrSameThan(Version.v1_15)) {
                skull.setString("Id", "f032de26-fde9-469f-a6eb-c453470894a5");
            } else {
                skull.setUUID("Id", UUID.fromString("f032de26-fde9-469f-a6eb-c453470894a5"));
            }

            NBTListCompound texture = skull.addCompound("Properties").getCompoundList("textures").addCompound();

            switch (parts[0]) {
                case "default":
                    texture.setString("Value", parts[1]);
                    break;
                case "hdb":
                    if (!HeadBlocks.isHeadDatabaseLoaded) {
                       HeadBlocks.log.sendMessage(FormatUtils.translate("&cCannot load hdb head type " + configHead + " without HeadDatabase installed"));
                        continue;
                    }

                    texture.setString("Value", main.getHeadDatabaseAPI().getBase64(parts[1]));
                    break;
                default:
                    HeadBlocks.log.sendMessage(FormatUtils.translate("&cUnknown type " + parts[0] + " in heads configuration section"));
                    continue;
            }

            heads.add(nbti.getItem());
        }
    }

    public ArrayList<ItemStack> getHeads() {
        return heads;
    }

    public ArrayList<Pair<UUID, Location>> getHeadLocations() {
        return headLocations;
    }
}
