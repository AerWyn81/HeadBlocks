package fr.aerwyn81.headblocks.data.hunt.behavior.spawn;

import fr.aerwyn81.headblocks.data.hunt.requirement.area.AreaProvider;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public record AreaOptions(boolean surface, int maxTries, BlockFilter filter, Set<Material> blocks) {

    public static final AreaOptions DEFAULT = new AreaOptions(true, 20, BlockFilter.BLACKLIST, Set.of());

    public enum BlockFilter {
        BLACKLIST, WHITELIST;

        public static BlockFilter of(String value) {
            return "WHITELIST".equalsIgnoreCase(value) ? WHITELIST : BLACKLIST;
        }
    }

    public AreaOptions {
        maxTries = Math.max(1, maxTries);
        blocks = Set.copyOf(blocks);
    }

    public AreaOptions(boolean surface, int maxTries, BlockFilter filter, Collection<Material> blocks) {
        this(surface, maxTries, filter, Set.copyOf(blocks));
    }

    public Location pickColumn(AreaProvider area) {
        var bounds = area == null ? null : area.getBounds();
        var world = area == null ? null : Bukkit.getWorld(area.getWorldName());
        if (bounds == null || world == null) {
            return null;
        }

        var random = ThreadLocalRandom.current();
        for (int i = 0; i < maxTries; i++) {
            int x = random.nextInt(bounds[0], bounds[3] + 1);
            int z = random.nextInt(bounds[2], bounds[5] + 1);
            if (world.isChunkLoaded(x >> 4, z >> 4)) {
                return new Location(world, x, bounds[1], z);
            }
        }
        return null;
    }

    public Location pickInChunk(AreaProvider area, Location column, Predicate<Location> isFree) {
        var bounds = area == null ? null : area.getBounds();
        var world = column.getWorld();
        if (bounds == null || world == null) {
            return null;
        }

        int chunkX = column.getBlockX() >> 4 << 4;
        int chunkZ = column.getBlockZ() >> 4 << 4;
        int minY = Math.max(bounds[1], world.getMinHeight() + 1);
        int maxY = Math.min(bounds[4], world.getMaxHeight() - 1);
        if (minY > maxY) {
            return null;
        }

        var random = ThreadLocalRandom.current();
        for (int i = 0; i < maxTries; i++) {
            int x = random.nextInt(Math.max(bounds[0], chunkX), Math.min(bounds[3], chunkX + 15) + 1);
            int z = random.nextInt(Math.max(bounds[2], chunkZ), Math.min(bounds[5], chunkZ + 15) + 1);
            int y = surface ? world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1 : random.nextInt(minY, maxY + 1);

            var location = firstSpotFrom(world, x, z, Math.max(y, minY), surface ? y : maxY, maxY);
            if (location != null && area.contains(location) && isFree.test(location)) {
                return location;
            }
        }
        return null;
    }

    private Location firstSpotFrom(World world, int x, int z, int from, int to, int maxY) {
        for (int y = from; y <= Math.min(to, maxY); y++) {
            var block = world.getBlockAt(x, y, z);
            if (block.isEmpty() && canStandOn(block.getRelative(0, -1, 0))) {
                return block.getLocation();
            }
        }
        return null;
    }

    private boolean canStandOn(Block block) {
        var type = block.getType();
        if (!type.isSolid()) {
            return false;
        }
        return filter == BlockFilter.WHITELIST ? blocks.contains(type) : !blocks.contains(type);
    }

    public static float randomYaw() {
        return ThreadLocalRandom.current().nextInt(4) * 90f;
    }

    public void saveTo(ConfigurationSection section) {
        section.set("surface", surface);
        section.set("maxTries", maxTries);
        section.set("blocks.mode", filter.name());
        section.set("blocks.list", blocks.stream().map(Material::name).sorted().toList());
    }

    public static AreaOptions fromConfig(ConfigurationSection section) {
        if (section == null) {
            return DEFAULT;
        }
        return new AreaOptions(section.getBoolean("surface", true), section.getInt("maxTries", 20),
                BlockFilter.of(section.getString("blocks.mode")), parseBlocks(section.getStringList("blocks.list")));
    }

    public static List<Material> parseBlocks(Collection<String> names) {
        var materials = new ArrayList<Material>();
        for (String name : names) {
            var material = Material.matchMaterial(name.trim());
            if (material != null && material.isBlock()) {
                materials.add(material);
            }
        }
        return materials;
    }
}
