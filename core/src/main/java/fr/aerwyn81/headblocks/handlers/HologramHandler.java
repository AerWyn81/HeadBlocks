package fr.aerwyn81.headblocks.handlers;

import com.github.unldenis.hologram.Hologram;
import com.github.unldenis.hologram.HologramPool;
import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class HologramHandler {
    private final HeadBlocks main;

    private HologramPool hologramPool;

    private final HashMap<Location, Hologram> foundHolograms;
    private final HashMap<Location, Hologram> notFoundHolograms;

    public HologramHandler(HeadBlocks main) {
        this.main = main;

        this.foundHolograms = new HashMap<>();
        this.notFoundHolograms = new HashMap<>();
    }

    public void load() {
        if (!HeadBlocks.isProtocolLibActive) {
            return;
        }

        this.hologramPool = new HologramPool(main, 16, 0, 0);
    }

    public void unload() {
        if (!HeadBlocks.isProtocolLibActive) {
            return;
        }

        foundHolograms.values().forEach(h -> hologramPool.remove(h));
        notFoundHolograms.values().forEach(h -> hologramPool.remove(h));
    }

    public void createHolograms(Location location) {
        if (location == null || !HeadBlocks.isProtocolLibActive) {
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

    private Hologram internalCreateHologram(Location location, ArrayList<String> lines) {
        location = location.clone();
        location.add(0.5, -0.9 + ConfigService.getHologramsHeightAboveHead(), 0.5);

        Hologram.Builder holoBuilder = Hologram.builder()
                .location(location);

        for (String line : lines) {
            holoBuilder.addLine(MessageUtils.colorize(line), false);
        }

        return holoBuilder.build(hologramPool);
    }

    public void removeHologram(Location location) {
        if (location == null || !HeadBlocks.isProtocolLibActive) {
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

    public Hologram getHoloFound(Location loc) {
        return foundHolograms.get(loc);
    }

    public Hologram getHoloNotFound(Location loc) {
        return notFoundHolograms.get(loc);
    }

    public void showFoundTo(Player player, Location location) {
        Hologram holo = getHoloFound(location);
        if (holo != null) {
            holo.removeExcludedPlayer(player);
        }

        holo = getHoloNotFound(location);
        if (holo != null) {
            holo.addExcludedPlayer(player);
        }
    }

    public void showNotFoundTo(Player player, Location location) {
        Hologram holo = getHoloNotFound(location);
        if (holo != null) {
            holo.removeExcludedPlayer(player);
        }

        holo = getHoloFound(location);
        if (holo != null) {
            holo.addExcludedPlayer(player);
        }
    }

    public HashMap<Location, Hologram> getFoundHolograms() {
        return foundHolograms;
    }

    public HashMap<Location, Hologram> getNotFoundHolograms() {
        return notFoundHolograms;
    }

    public void addExcludedPlayer(Player player) {
        if (!HeadBlocks.isProtocolLibActive) {
            return;
        }

        getFoundHolograms().values().forEach(h -> h.addExcludedPlayer(player));
        getNotFoundHolograms().values().forEach(h -> h.addExcludedPlayer(player));
    }

    public void removeExcludedPlayer(Player player) {
        if (!HeadBlocks.isProtocolLibActive) {
            return;
        }

        getFoundHolograms().values().forEach(h -> h.removeExcludedPlayer(player));
        getNotFoundHolograms().values().forEach(h -> h.removeExcludedPlayer(player));
    }
}
