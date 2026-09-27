package fr.aerwyn81.headblocks.data.hunt.behavior.spawn;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.LinkedHashMap;
import java.util.Map;

public record SpawnPoint(String world, int x, int y, int z, float yaw) {

    public static SpawnPoint of(Location location, float yaw) {
        return new SpawnPoint(location.getWorld().getName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ(), yaw);
    }

    public Location toLocation() {
        var bukkitWorld = Bukkit.getWorld(world);
        return bukkitWorld == null ? null : new Location(bukkitWorld, x, y, z);
    }

    public boolean matches(Location location) {
        return location != null && location.getWorld() != null
                && world.equals(location.getWorld().getName())
                && x == location.getBlockX() && y == location.getBlockY() && z == location.getBlockZ();
    }

    public Map<String, Object> serialize() {
        var map = new LinkedHashMap<String, Object>();
        map.put("world", world);
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        if (yaw != 0f) {
            map.put("yaw", (double) yaw);
        }
        return map;
    }

    public static SpawnPoint deserialize(Object raw) {
        if (!(raw instanceof Map<?, ?> map) || map.get("world") == null) {
            return null;
        }

        try {
            return new SpawnPoint(String.valueOf(map.get("world")),
                    toInt(map.get("x")), toInt(map.get("y")), toInt(map.get("z")),
                    map.get("yaw") == null ? 0f : Float.parseFloat(String.valueOf(map.get("yaw"))));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int toInt(Object value) {
        if (value instanceof Number number) {
            return (int) Math.floor(number.doubleValue());
        }
        return (int) Math.floor(Double.parseDouble(String.valueOf(value)));
    }
}
