package fr.aerwyn81.headblocks.data.hunt.behavior.zone;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public class WorldGuardZoneProvider implements ZoneProvider {

    public static final String TYPE = "worldguard";

    private final String worldName;
    private final String regionId;

    public WorldGuardZoneProvider(String worldName, String regionId) {
        this.worldName = worldName;
        this.regionId = regionId;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean contains(Location location) {
        if (!isWorldGuardPresent() || location == null || location.getWorld() == null) {
            return false;
        }

        if (!worldName.equals(location.getWorld().getName())) {
            return false;
        }

        ProtectedRegion region = getRegion(location.getWorld());
        if (region == null) {
            return false;
        }

        return region.contains(BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
    }

    @Override
    public boolean isAvailable() {
        if (!isWorldGuardPresent()) {
            return false;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return false;
        }

        return getRegion(world) != null;
    }

    @Override
    public void saveTo(ConfigurationSection section) {
        section.set("type", TYPE);
        section.set("world", worldName);
        section.set("region", regionId);
    }

    public String getWorldName() {
        return worldName;
    }

    public String getRegionId() {
        return regionId;
    }

    public int[] getBounds() {
        if (!isWorldGuardPresent()) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        ProtectedRegion region = getRegion(world);
        if (region == null) {
            return null;
        }

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        return new int[]{min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ()};
    }

    private boolean isWorldGuardPresent() {
        return Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
    }

    private ProtectedRegion getRegion(World world) {
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regionManager = container.get(BukkitAdapter.adapt(world));
            if (regionManager == null) {
                return null;
            }
            return regionManager.getRegion(regionId);
        } catch (Exception e) {
            return null;
        }
    }

    public static WorldGuardZoneProvider fromSection(ConfigurationSection section) {
        String worldName = section.getString("world", "");
        String regionId = section.getString("region", "");

        return new WorldGuardZoneProvider(worldName, regionId);
    }
}
