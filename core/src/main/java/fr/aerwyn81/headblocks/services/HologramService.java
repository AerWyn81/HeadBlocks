package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.holograms.EnumTypeHologram;
import fr.aerwyn81.headblocks.holograms.InternalHologram;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalUtils;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class HologramService {
    private static HashMap<UUID, InternalHologram> foundHolograms;
    private static HashMap<UUID, InternalHologram> notFoundHolograms;
    private static boolean enable;
    private static EnumTypeHologram enumTypeHologram;

    static {
        enable = true;
    }

    public static void load() {
        foundHolograms = new HashMap<>();
        notFoundHolograms = new HashMap<>();

        enable = ConfigService.isHologramsEnabled();

        var holoPlugin = ConfigService.getHologramPlugin();
        enumTypeHologram = EnumTypeHologram.fromString(holoPlugin);
        if (enumTypeHologram == null) {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks] &cPlugin &e" + holoPlugin + " &cnot yet supported for holograms!"));
            enable = false;
            return;
        }

        if (!enable ||
                (enumTypeHologram == EnumTypeHologram.DECENT && !HeadBlocks.isDecentHologramsActive) ||
                (enumTypeHologram == EnumTypeHologram.HD && !HeadBlocks.isHolographicDisplaysActive) ||
                (enumTypeHologram == EnumTypeHologram.DEFAULT && !HeadBlocks.isProtocolLibActive)) {
            return;
        }


        //for (HeadLocation loc : ) {
        //    if (loc == null) {
        //        continue;
        //    }
//
        //    createHolograms(loc.getLocation());
        //}

        HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks] &eHolograms loaded!"));
    }

    public static void createHolograms(Location location) {
        if (!enable) {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("[HeadBlocks] &cCannot create a hologram. Are the necessary plugin installed?"));
            return;
        }

        if (ConfigService.isHologramsFoundEnabled()) {
            var holoFound = internalCreateHologram(location, ConfigService.getHologramsFoundLines().stream().map(MessageUtils::colorize).collect(Collectors.toList()));
            foundHolograms.put(holoFound.getUuid(), holoFound);
        }

        if (ConfigService.isHologramsNotFoundEnabled()) {
            var holoNotFound = internalCreateHologram(location, ConfigService.getHologramsNotFoundLines().stream().map(MessageUtils::colorize).collect(Collectors.toList()));
            notFoundHolograms.put(holoNotFound.getUuid(), holoNotFound);
        }
    }

    private static InternalHologram internalCreateHologram(Location location, List<String> lines) {
        var allUUIDs = new ArrayList<>(foundHolograms.keySet());
        allUUIDs.addAll(notFoundHolograms.keySet());

        var uuid = InternalUtils.generateNewUUID(allUUIDs);
        var internalHologram = new InternalHologram(enumTypeHologram);
        internalHologram.createHologram(uuid, location, lines, ConfigService.getHologramsHeightAboveHead(), ConfigService.getHologramParticlePlayerViewDistance());

        return internalHologram;
    }

    public static void showFoundTo(Player player, Location location) {
        if (!enable) {
            return;
        }

        var holoFound = getHologramByLocation(foundHolograms, location);
        if (holoFound != null && !holoFound.isHologramVisible(player)) {
            holoFound.show(player);
        }

        var holoNotFound = getHologramByLocation(notFoundHolograms, location);
        if (holoNotFound != null && holoNotFound.isHologramVisible(player)) {
            holoNotFound.hide(player);
        }
    }

    public static void showNotFoundTo(Player player, Location location) {
        if (!enable) {
            return;
        }

        var holoFound = getHologramByLocation(foundHolograms, location);
        if (holoFound != null && holoFound.isHologramVisible(player)) {
            holoFound.hide(player);
        }

        var holoNotFound = getHologramByLocation(notFoundHolograms, location);
        if (holoNotFound != null && !holoNotFound.isHologramVisible(player)) {
            holoNotFound.show(player);
        }
    }

    public static void removeHolograms(Location location) {
        if (location == null) {
            return;
        }

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

        foundHolograms.clear();
        notFoundHolograms.clear();
    }
}
