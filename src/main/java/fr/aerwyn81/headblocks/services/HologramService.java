package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.holograms.EnumTypeHologram;
import fr.aerwyn81.headblocks.holograms.InternalHologram;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalUtils;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.holoeasy.hologram.Hologram;
import org.holoeasy.pool.IHologramPool;

import java.util.*;
import java.util.stream.Collectors;

public class HologramService {
    private static HashMap<UUID, InternalHologram> foundHolograms;
    private static HashMap<UUID, InternalHologram> notFoundHolograms;
    private static HashMap<UUID, InternalHologram> holograms;
    private static boolean enable;
    private static EnumTypeHologram enumTypeHologram;

    public static IHologramPool<Hologram> hologramPool;

    static {
        enable = true;
    }

    public static void load() {
        foundHolograms = new HashMap<>();
        notFoundHolograms = new HashMap<>();
        holograms = new HashMap<>();

        enable = ConfigService.isHologramsEnabled();
        if (!enable) {
            return;
        }

        enumTypeHologram = getHologramTypeFromConfig();
        if (enumTypeHologram == null) {
            if (HeadBlocks.isPacketEventsActive) {
                enumTypeHologram = EnumTypeHologram.ADVANCED;
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&6" + ConfigService.getHologramPlugin() + " plugin support removed. " +
                        "PacketEvents detected, switching to internal advanced hologram type."));
            } else {
                enumTypeHologram = EnumTypeHologram.DEFAULT;
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&6" + ConfigService.getHologramPlugin() + " plugin support removed. " +
                        "Switching to default hologram type. " +
                        "For placeholder support, install PacketEvents plugin and set hologram type to ADVANCED in config."));
            }
        }

        var holoEasyLib = HeadBlocks.getInstance().getHoloEasyLib();
        if (holoEasyLib != null) {
            hologramPool = holoEasyLib.startPool(ConfigService.getHologramParticlePlayerViewDistance(), false, true);
        }

        if (enumTypeHologram == EnumTypeHologram.ADVANCED && !HeadBlocks.isPacketEventsActive) {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cCannot use ADVANCED hologram type without PacketEvents plugin. " +
                    "Please install PacketEvents plugin or change hologram type to DEFAULT in config.yml"));
            enable = false;
            return;
        }

        for (HeadLocation loc : HeadService.getHeadLocations()) {
            if (loc.isCharged()) {
                createHolograms(loc.getLocation());
            }
        }
    }

    public static EnumTypeHologram getHologramTypeFromConfig() {
        return EnumTypeHologram.getEnumFromText(ConfigService.getHologramPlugin());
    }

    public static void createHolograms(Location location) {
        if (!enable) {
            var suffix = "";

            if (enumTypeHologram != EnumTypeHologram.DEFAULT && enumTypeHologram != EnumTypeHologram.ADVANCED) {
                suffix = "Is " + EnumTypeHologram.getPluginName(enumTypeHologram) + " plugin enabled?";
            }

            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cCannot create an hologram above the head at " + location.toString() + ". " + suffix));
            return;
        }

        if (enumTypeHologram == EnumTypeHologram.DEFAULT) {
            if (ConfigService.isHologramsFoundEnabled()) {
                var holoFound = internalCreateHologram(location, ConfigService.getHologramsFoundLines().stream().map(MessageUtils::colorize).collect(Collectors.toList()));
                foundHolograms.put(holoFound.getUuid(), holoFound);
            }

            if (ConfigService.isHologramsNotFoundEnabled()) {
                var holoNotFound = internalCreateHologram(location, ConfigService.getHologramsNotFoundLines().stream().map(MessageUtils::colorize).collect(Collectors.toList()));
                notFoundHolograms.put(holoNotFound.getUuid(), holoNotFound);
            }
            return;
        }

        if (enumTypeHologram == EnumTypeHologram.ADVANCED) {
            var holo = internalCreateHologram(location, Collections.emptyList());
            holograms.put(holo.getUuid(), holo);
        }
    }

    private static InternalHologram internalCreateHologram(Location location, List<String> lines) {
        var allUUIDs = new ArrayList<>(foundHolograms.keySet());
        allUUIDs.addAll(notFoundHolograms.keySet());
        allUUIDs.addAll(holograms.keySet());

        var uuid = InternalUtils.generateNewUUID(allUUIDs);
        var internalHologram = new InternalHologram(enumTypeHologram);
        internalHologram.createHologram(uuid, location, lines, ConfigService.getHologramsHeightAboveHead());

        return internalHologram;
    }

    public static void showFoundTo(Player player, Location location) {
        if (!enable) {
            return;
        }

        if (enumTypeHologram == EnumTypeHologram.ADVANCED || !ConfigService.isHologramsFoundEnabled()) {
            return;
        }

        var holoFound = getHologramByLocation(foundHolograms, location);
        if (holoFound != null) {
            if (!holoFound.isHologramVisible(player)) {
                holoFound.show(player);
            }
        }

        var holoNotFound = getHologramByLocation(notFoundHolograms, location);
        if (holoNotFound != null) {
            if (holoNotFound.isHologramVisible(player)) {
                holoNotFound.hide(player);
            }
        }
    }

    public static void showNotFoundTo(Player player, Location location) {
        if (!enable) {
            return;
        }

        if (enumTypeHologram == EnumTypeHologram.ADVANCED || !ConfigService.isHologramsNotFoundEnabled()) {
            return;
        }

        var holoFound = getHologramByLocation(foundHolograms, location);
        if (holoFound != null) {
            if (holoFound.isHologramVisible(player)) {
                holoFound.hide(player);
            }
        }

        var holoNotFound = getHologramByLocation(notFoundHolograms, location);
        if (holoNotFound != null) {
            if (!holoNotFound.isHologramVisible(player)) {
                holoNotFound.show(player);
            }
        }
    }

    public static void hideHolograms(HeadLocation headLocation, Player player) {
        if (!enable) {
            return;
        }

        if (enumTypeHologram == EnumTypeHologram.ADVANCED) {
            var holo = getHologramByLocation(holograms, headLocation.getLocation());
            if (holo != null && holo.isHologramVisible(player)) {
                holo.hide(player);
            }
            return;
        }

        if (ConfigService.isHologramsFoundEnabled()) {
            var holoFound = getHologramByLocation(foundHolograms, headLocation.getLocation());
            if (holoFound != null && holoFound.isHologramVisible(player)) {
                holoFound.hide(player);
            }
        }

        if (ConfigService.isHologramsNotFoundEnabled()) {
            var holoNotFound = getHologramByLocation(notFoundHolograms, headLocation.getLocation());
            if (holoNotFound != null && holoNotFound.isHologramVisible(player)) {
                holoNotFound.hide(player);
            }
        }
    }

    public static void removeHolograms(Location location) {
        if (location == null) {
            return;
        }

        if (enumTypeHologram == EnumTypeHologram.DEFAULT) {
            var foundHolo = getHologramByLocation(foundHolograms, location);
            var notFoundHolo = getHologramByLocation(notFoundHolograms, location);

            if (foundHolo != null) {
                foundHolograms.remove(foundHolo.getUuid());
                foundHolo.deleteHologram();
            }

            if (notFoundHolo != null) {
                notFoundHolograms.remove(notFoundHolo.getUuid());
                notFoundHolo.deleteHologram();
            }
            return;
        }

        if (enumTypeHologram == EnumTypeHologram.ADVANCED) {
            var uniqueHolo = getHologramByLocation(holograms, location);

            if (uniqueHolo != null) {
                holograms.remove(uniqueHolo.getUuid());
                uniqueHolo.deleteHologram();
            }
        }
    }

    private static InternalHologram getHologramByLocation(HashMap<UUID, InternalHologram> list, Location location) {
        var holo = list.entrySet().stream()
                .filter(entry -> LocationUtils.areEquals(entry.getValue().getLocation(), location))
                .findFirst()
                .orElse(null);

        if (holo != null) {
            return holo.getValue();
        }

        return null;
    }

    public static void unload() {
        if (!enable) {
            return;
        }

        foundHolograms.forEach((uuid, hologram) -> hologram.deleteHologram());
        notFoundHolograms.forEach((uuid, hologram) -> hologram.deleteHologram());
        holograms.forEach((uuid, hologram) -> hologram.deleteHologram());

        foundHolograms.clear();
        notFoundHolograms.clear();
        holograms.clear();

        if (hologramPool != null) {
            hologramPool.destroy();
        }
    }

    public static void refresh(Player player, Location location) {
        if (location == null || enumTypeHologram == EnumTypeHologram.DEFAULT) {
            return;
        }

        var uniqueHolo = getHologramByLocation(holograms, location);
        if (uniqueHolo != null) {
            uniqueHolo.refresh(player);
        }
    }
}
