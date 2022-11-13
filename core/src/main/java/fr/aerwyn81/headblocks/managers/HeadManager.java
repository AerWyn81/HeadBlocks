package fr.aerwyn81.headblocks.managers;

import de.tr7zw.changeme.nbtapi.NBTTileEntity;
import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.ConfigService;
import fr.aerwyn81.headblocks.services.HologramService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.block.data.Rotatable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

public class HeadManager {
    private final ArrayList<HeadLocation> headLocations;

    public HeadManager() {
        this.headLocations = new ArrayList<>();
    }

    public ArrayList<HeadLocation> getHeadLocations() {
        return headLocations;
    }

    public void loadHeadLocations(YamlConfiguration config) {
        var section = "race.locations";
        ConfigurationSection locations = config.getConfigurationSection(section);

        if (locations == null) {
            //To handle backward compatibility with HeadBlocks pre 2.5
            section = "locations";
            locations = config.getConfigurationSection(section);
        }

        if (locations == null) {
            return;
        }

        for (String uuid : locations.getKeys(false)) {
            ConfigurationSection configSection = config.getConfigurationSection(section + "." + uuid);

            if (configSection != null) {
                UUID headUuid = UUID.fromString(uuid);

                HeadLocation headLocation;

                try {
                    headLocation = HeadLocation.fromConfig(config, headUuid);
                } catch (Exception e) {
                    HeadBlocks.log.sendMessage(MessageUtils.colorize("&cCannot deserialize location of head " + uuid));
                    continue;
                }

                headLocations.add(headLocation);

                if (ConfigService.isHologramsEnabled()) {
                    HologramService.createHolograms(headLocation.getLocation());
                }

                try {
                    //Todo retirer le double appel et insert into uniquement s'il n'existe pas déjà
                    boolean isExist = StorageService.isHeadExist(headUuid);
                    if (!isExist) {
                        StorageService.createNewHead(headUuid, "");
                    }
                } catch (Exception ex) {
                    HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to create a head (" + headUuid + ") in the storage: " + ex.getMessage()));
                    headLocation.setCharged(false);
                }
            }
        }
    }

    public void saveHeadLocations(FileConfiguration config) {
        for (HeadLocation headLocation : headLocations) {
            headLocation.saveInConfig(config);
        }
    }

    public UUID addHeadLocation(Location location, String texture) throws InternalException {
        UUID uniqueUuid = UUID.randomUUID();
        while (getHeadByUUID(uniqueUuid) != null) {
            uniqueUuid = UUID.randomUUID();
        }

        StorageService.createNewHead(uniqueUuid, texture);

        if (ConfigService.isHologramsEnabled()) {
            HologramService.createHolograms(location);
        }

        var headLocation = new HeadLocation("", uniqueUuid, location);
        headLocations.add(headLocation);

        return uniqueUuid;
    }

    public void removeHeadLocation(HeadLocation headLocation, boolean withDelete) throws InternalException {
        if (headLocation != null) {
            StorageService.removeHead(headLocation.getUuid(), withDelete);

            headLocation.getLocation().getBlock().setType(Material.AIR);

            if (ConfigService.isHologramsEnabled()) {
                HologramService.removeHolograms(headLocation.getLocation());
            }

            headLocations.remove(headLocation);

            //headMoves.entrySet().removeIf(hM -> headLocation.getUuid().equals(hM.getKey()));
        }
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
        //headLocation.saveInConfig(config);

        headLocations.set(indexOfOld, headLocation);

        HologramService.removeHolograms(oldBlock.getLocation());
        HologramService.createHolograms(newBlock.getLocation());
    }

    public HeadLocation getHeadByUUID(UUID headUuid) {
        return headLocations.stream().filter(h -> h.getUuid().equals(headUuid)).findFirst().orElse(null);
    }

    public HeadLocation getHeadAt(Location location) {
        return headLocations.stream().filter(h -> LocationUtils.areEquals(h.getLocation(), location)).findFirst().orElse(null);
    }

    public ArrayList<HeadLocation> getChargedHeadLocations() {
        return headLocations.stream().filter(HeadLocation::isCharged).collect(Collectors.toCollection(ArrayList::new));
    }
}
