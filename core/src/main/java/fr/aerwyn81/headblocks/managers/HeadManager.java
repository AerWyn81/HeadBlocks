package fr.aerwyn81.headblocks.managers;

import de.tr7zw.changeme.nbtapi.NBTTileEntity;
import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.head.HBTrack;
import fr.aerwyn81.headblocks.services.ConfigService;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.HologramService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.InternalUtils;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.block.data.Rotatable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class HeadManager {
    private final ArrayList<HeadLocation> headLocations;
    private final YamlConfiguration config;

    private HBTrack track;

    public HeadManager(YamlConfiguration config) {
        this.headLocations = new ArrayList<>();
        this.config = config;
    }

    public ArrayList<HeadLocation> getHeadLocations() {
        return headLocations;
    }

    public HBTrack getTrack() {
        return track;
    }

    public void setTrack(HBTrack track) {
        this.track = track;
    }

    public void loadHeadLocations() {
        var section = "track.locations";
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

                if (ConfigService.isHologramsEnabled() && headLocation.getLocation() != null) {
                    HologramService.createHolograms(headLocation.getLocation());
                }

                try {
                    StorageService.createNewHead(headUuid);
                } catch (Exception ex) {
                    HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to create a head (" + headUuid + ") in the storage: " + ex.getMessage()));
                    headLocation.setCharged(false);
                }
            }
        }
    }

    public void saveHeadLocations() {
        for (HeadLocation headLocation : headLocations) {
            headLocation.saveInConfig(config);
        }
    }

    public UUID addHeadLocation(Location location, String texture) throws InternalException {
        UUID uniqueUuid = InternalUtils.generateNewUUID(headLocations.stream().map(HeadLocation::getUuid).collect(Collectors.toList()));

        StorageService.createNewHead(uniqueUuid);

        if (ConfigService.isHologramsEnabled()) {
            HologramService.createHolograms(location);
        }

        var headLocation = new HeadLocation("", new ArrayList<>(), uniqueUuid, texture, location, this);
        headLocations.add(headLocation);

        return uniqueUuid;
    }

    public void removeHeadLocation(HeadLocation headLocation) throws InternalException {
        StorageService.removeHead(headLocation.getUuid());

        headLocation.getLocation().getBlock().setType(Material.AIR);

        headLocation.removeFromConfig(config);

        if (ConfigService.isHologramsEnabled()) {
            HologramService.removeHolograms(headLocation.getLocation());
        }

        headLocations.remove(headLocation);

        HeadService.getHeadMoves().entrySet().removeIf(hM -> headLocation.getUuid().equals(hM.getKey()));
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

        var optHeadLocation = getHeadByUUID(hUuid);
        if (optHeadLocation.isEmpty()) {
            return;
        }

        var headLocation = optHeadLocation.get();

        var indexOfOld = headLocations.indexOf(headLocation);

        headLocation.setLocation(newBlock.getLocation());
        headLocation.saveInConfig(config);

        headLocations.set(indexOfOld, headLocation);

        HologramService.removeHolograms(oldBlock.getLocation());
        HologramService.createHolograms(newBlock.getLocation());
    }

    public Optional<HeadLocation> getHeadByUUID(UUID headUuid) {
        return headLocations.stream().filter(h -> h.getUuid().equals(headUuid)).findFirst();
    }

    public Optional<HeadLocation> getHeadAt(Location location) {
        return headLocations.stream().filter(h -> LocationUtils.areEquals(h.getLocation(), location)).findFirst();
    }

    public ArrayList<HeadLocation> getChargedHeadLocations(boolean onlyCharged) {
        if (onlyCharged)
            return headLocations.stream().filter(HeadLocation::isCharged).collect(Collectors.toCollection(ArrayList::new));

        return headLocations;
    }
}
