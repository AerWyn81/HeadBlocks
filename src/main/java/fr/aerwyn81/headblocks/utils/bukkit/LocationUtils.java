package fr.aerwyn81.headblocks.utils.bukkit;

import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Location;

public class LocationUtils {

    public static boolean areEquals(Location loc1, Location loc2) {
        return loc1 != null && loc2 != null && loc1.getBlockX() == loc2.getBlockX()
                && loc1.getBlockY() == loc2.getBlockY()
                && loc1.getBlockZ() == loc2.getBlockZ()
                && loc1.getWorld() != null && loc2.getWorld() != null &&
                loc1.getWorld().getName().equals(loc2.getWorld().getName());
    }

    public static String toFormattedString(Location loc) {
        return MessageUtils.colorize("&7" + parseWorld(loc) +
                " &8[&8X: &7" + loc.getX() +
                "&8, &8Y: &7" + loc.getY() +
                "&8, &8Z: &7" + loc.getZ() + "&8]");
    }

    /**
     * Replace placeholders x y z world by location in string
     *
     * @param message  string
     * @param location location for replace
     * @return parsed string
     */
    public static String parseLocationPlaceholders(String message, Location location) {
        return message.replaceAll("%x%", String.valueOf(location.getX()))
                .replaceAll("%y%", String.valueOf(location.getY()))
                .replaceAll("%z%", String.valueOf(location.getZ()))
                .replaceAll("%world%", parseWorld(location))
                .replaceAll("%worldName%", parseWorld(location));
    }

    private static String parseWorld(Location location) {
        return location.getWorld() != null ? location.getWorld().getName() : MessageUtils.colorize("&cUnknownWorld");
    }
}
