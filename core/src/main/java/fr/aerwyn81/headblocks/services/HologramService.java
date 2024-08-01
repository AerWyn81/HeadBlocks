package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.holograms.EnumTypeHologram;
import fr.aerwyn81.headblocks.holograms.InternalHologram;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalUtils;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HologramService {
    private static HashMap<UUID, InternalHologram> foundHolograms;
    private static HashMap<UUID, InternalHologram> notFoundHolograms;
    private static boolean enable;
    private static EnumTypeHologram enumTypeHologram;

    private static BukkitTask hideHoloTooFarTask;

    static {
        enable = true;
    }

    public static void load() {
        foundHolograms = new HashMap<>();
        notFoundHolograms = new HashMap<>();

        if (hideHoloTooFarTask != null) {
            hideHoloTooFarTask.cancel();
            Bukkit.getScheduler().cancelTask(hideHoloTooFarTask.getTaskId());
            hideHoloTooFarTask = null;
        }

        enable = ConfigService.isHologramsEnabled();
        if (!enable) {
            return;
        }

        var holoPlugin = ConfigService.getHologramPlugin();

        enumTypeHologram = EnumTypeHologram.getEnumFromText(holoPlugin);
        if (enumTypeHologram == null) {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cPlugin &e" + holoPlugin + " &cnot yet supported for holograms!"));
            enable = false;
            return;
        }

        if ((enumTypeHologram == EnumTypeHologram.DECENT && !HeadBlocks.isDecentHologramsActive) ||
                (enumTypeHologram == EnumTypeHologram.DEFAULT && !HeadBlocks.isProtocolLibActive) ||
                (enumTypeHologram == EnumTypeHologram.HD && !HeadBlocks.isHolographicDisplaysActive) ||
                (enumTypeHologram == EnumTypeHologram.FH && !HeadBlocks.isFancyHologramsActive) ||
                (enumTypeHologram == EnumTypeHologram.CMI && !HeadBlocks.isCMIActive)) {
            enable = false;
        }

        for (HeadLocation loc : HeadService.getHeadLocations()) {
            if (loc.isCharged()) {
                createHolograms(loc.getLocation());
            }
        }

        if (enumTypeHologram == EnumTypeHologram.FH) {
            hideHoloTooFarTask = startTimerHideHoloTooFar();
        }
    }

    private static BukkitTask startTimerHideHoloTooFar() {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(HeadBlocks.getInstance(), () -> {
            var players = Collections.synchronizedCollection(Bukkit.getOnlinePlayers());

            foundHolograms.values().forEach(h -> players.stream()
                    .filter(h::isHologramVisible)
                    .filter(p -> !isWithinVisibilityDistance(p, h.getLocation()))
                    .forEach(h::hide));

            notFoundHolograms.values().forEach(h -> players.stream()
                    .filter(h::isHologramVisible)
                    .filter(p -> !isWithinVisibilityDistance(p, h.getLocation()))
                    .forEach(h::hide));
        }, 0, 20L);
    }

    public static boolean isWithinVisibilityDistance(Player player, Location holoLoc) {
        if (!player.getWorld().equals(holoLoc.getWorld())) {
            return false;
        }

        int visibilityDistance = ConfigService.getHologramParticlePlayerViewDistance();
        double distanceSquared = holoLoc.distanceSquared(player.getLocation());

        return distanceSquared <= visibilityDistance * visibilityDistance;
    }

    public static void createHolograms(Location location) {
        if (!enable) {
            if (enumTypeHologram == EnumTypeHologram.DEFAULT) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&cCannot create an hologram above the head at " + location.toString() + ". Is ProtocolLib plugin enabled?"));
            } else {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&cCannot create an hologram above the head at " + location.toString() + ". Is " + EnumTypeHologram.getPluginName(enumTypeHologram) + " plugin enabled?"));
            }

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
        if (holoFound != null) {
            if (!holoFound.isSupportedPerPlayerView() || !holoFound.isHologramVisible(player)) {
                holoFound.show(player);
            }
        }

        var holoNotFound = getHologramByLocation(notFoundHolograms, location);
        if (holoNotFound != null) {
            if (!holoNotFound.isSupportedPerPlayerView() || holoNotFound.isHologramVisible(player)) {
                holoNotFound.hide(player);
            }
        }
    }

    public static void showNotFoundTo(Player player, Location location) {
        if (!enable) {
            return;
        }

        var holoFound = getHologramByLocation(foundHolograms, location);
        if (holoFound != null) {
            if (!holoFound.isSupportedPerPlayerView() || holoFound.isHologramVisible(player)) {
                holoFound.hide(player);
            }
        }

        var holoNotFound = getHologramByLocation(notFoundHolograms, location);
        if (holoNotFound != null) {
            if (!holoNotFound.isSupportedPerPlayerView() || !holoNotFound.isHologramVisible(player)) {
                holoNotFound.show(player);
            }
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
