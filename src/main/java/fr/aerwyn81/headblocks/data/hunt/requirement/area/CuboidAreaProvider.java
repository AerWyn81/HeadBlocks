package fr.aerwyn81.headblocks.data.hunt.requirement.area;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

/**
 * An area drawn from two corners: self contained, and resolvable as long as its world is loaded.
 */
public class CuboidAreaProvider implements AreaProvider {

    public static final String TYPE = "cuboid";

    private final String worldName;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    public CuboidAreaProvider(String worldName, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.worldName = worldName;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        if (!worldName.equals(location.getWorld().getName())) {
            return false;
        }

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    @Override
    public boolean isAvailable() {
        return Bukkit.getWorld(worldName) != null;
    }

    @Override
    public String getDescription() {
        return worldName + " (" + minX + ", " + minY + ", " + minZ + " → " + maxX + ", " + maxY + ", " + maxZ + ")";
    }

    @Override
    public void saveTo(ConfigurationSection section) {
        section.set("type", TYPE);
        section.set("world", worldName);
        section.set("min.x", minX);
        section.set("min.y", minY);
        section.set("min.z", minZ);
        section.set("max.x", maxX);
        section.set("max.y", maxY);
        section.set("max.z", maxZ);
    }

    public String getWorldName() {
        return worldName;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public static CuboidAreaProvider fromSection(ConfigurationSection section) {
        String worldName = section.getString("world", "");
        int minX = section.getInt("min.x");
        int minY = section.getInt("min.y");
        int minZ = section.getInt("min.z");
        int maxX = section.getInt("max.x");
        int maxY = section.getInt("max.y");
        int maxZ = section.getInt("max.z");

        return new CuboidAreaProvider(worldName, minX, minY, minZ, maxX, maxY, maxZ);
    }
}
