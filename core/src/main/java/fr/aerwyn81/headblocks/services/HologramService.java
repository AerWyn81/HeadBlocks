package fr.aerwyn81.headblocks.services;

import com.github.unldenis.hologram.Hologram;
import com.github.unldenis.hologram.HologramPool;
import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class HologramService {
    private static HologramPool hologramPool;

    private static HashMap<Location, Hologram> foundHolograms;
    private static HashMap<Location, Hologram> notFoundHolograms;

    private static boolean isEnabled() {
        return HeadBlocks.isProtocolLibActive && ConfigService.isHologramsEnabled();
    }

    public static void initialize() {
        foundHolograms = new HashMap<>();
        notFoundHolograms = new HashMap<>();

        load();
    }

    public static void load() {
        if (!isEnabled()) {
            return;
        }

        unload();
        hologramPool = new HologramPool(HeadBlocks.getInstance(), 16, 0, 0);
    }

    public static void unload() {
        if (!isEnabled()) {
            return;
        }

        foundHolograms.values().forEach(h -> hologramPool.remove(h));
        notFoundHolograms.values().forEach(h -> hologramPool.remove(h));
    }

    public static void createHolograms(Location location) {
        if (location == null || !isEnabled()) {
            return;
        }

        Hologram hologramFound = null;
        if (ConfigService.isHologramsFoundEnabled()) {
            hologramFound = internalCreateHologram(location, ConfigService.getHologramsFoundLines());
            foundHolograms.put(location, hologramFound);
        }

        Hologram hologramNotFound = null;
        if (ConfigService.isHologramsNotFoundEnabled()) {
            hologramNotFound = internalCreateHologram(location, ConfigService.getHologramsNotFoundLines());
            notFoundHolograms.put(location, hologramNotFound);
        }

        for (Player player : Collections.unmodifiableCollection(Bukkit.getOnlinePlayers())) {
            if (hologramFound != null) {
                hologramFound.addExcludedPlayer(player);
            }

            if (hologramNotFound != null) {
                hologramNotFound.addExcludedPlayer(player);
            }
        }
    }

    private static Hologram internalCreateHologram(Location location, ArrayList<String> lines) {
        location = location.clone();
        location.add(0.5, -0.9 + ConfigService.getHologramsHeightAboveHead(), 0.5);

        Hologram.Builder holoBuilder = Hologram.builder()
                .location(location);

        for (String line : lines) {
            holoBuilder.addLine(MessageUtils.colorize(line), false);
        }

        return holoBuilder.build(hologramPool);
    }

    public static void removeHolograms(Location location) {
        if (location == null || !isEnabled()) {
            return;
        }

        Hologram foundHolo = foundHolograms.get(location);
        Hologram notFoundHolo = notFoundHolograms.get(location);

        if (foundHolo != null) {
            hologramPool.remove(foundHolo);
            foundHolograms.remove(location);
        }

        if (notFoundHolo != null) {
            hologramPool.remove(notFoundHolo);
            notFoundHolograms.remove(location);
        }
    }

    public static Hologram getHoloFound(Location loc) {
        return foundHolograms.get(loc);
    }

    public static Hologram getHoloNotFound(Location loc) {
        return notFoundHolograms.get(loc);
    }

    public static void showFoundTo(Player player, Location location) {
        Hologram holo = getHoloFound(location);
        if (holo != null) {
            holo.removeExcludedPlayer(player);
        }

        holo = getHoloNotFound(location);
        if (holo != null) {
            holo.addExcludedPlayer(player);
        }
    }

    public static void showNotFoundTo(Player player, Location location) {
        Hologram holo = getHoloNotFound(location);
        if (holo != null) {
            holo.removeExcludedPlayer(player);
        }

        holo = getHoloFound(location);
        if (holo != null) {
            holo.addExcludedPlayer(player);
        }
    }

    public static HashMap<Location, Hologram> getFoundHolograms() {
        return foundHolograms;
    }

    public static HashMap<Location, Hologram> getNotFoundHolograms() {
        return notFoundHolograms;
    }

    public static void addExcludedPlayer(Player player) {
        if (!isEnabled()) {
            return;
        }

        getFoundHolograms().values().forEach(h -> h.addExcludedPlayer(player));
        getNotFoundHolograms().values().forEach(h -> h.addExcludedPlayer(player));
    }

    public static void removeExcludedPlayer(Player player) {
        if (!isEnabled()) {
            return;
        }

        getFoundHolograms().values().forEach(h -> h.removeExcludedPlayer(player));
        getNotFoundHolograms().values().forEach(h -> h.removeExcludedPlayer(player));
    }
}
