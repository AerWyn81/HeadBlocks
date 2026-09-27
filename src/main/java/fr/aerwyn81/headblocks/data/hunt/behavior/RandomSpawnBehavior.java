package fr.aerwyn81.headblocks.data.hunt.behavior;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.*;
import fr.aerwyn81.headblocks.data.hunt.requirement.area.AreaProvider;
import fr.aerwyn81.headblocks.data.hunt.requirement.types.AreaRequirement;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public class RandomSpawnBehavior extends SpawnBehavior {

    public static final String ID = "random_spawn";

    public enum BlockFilter {
        BLACKLIST, WHITELIST;

        public static BlockFilter of(String value) {
            return "WHITELIST".equalsIgnoreCase(value) ? WHITELIST : BLACKLIST;
        }
    }

    private final boolean surface;
    private final int maxTries;
    private final BlockFilter filter;
    private final Set<Material> blocks;

    public RandomSpawnBehavior(ServiceRegistry registry, boolean surface, int maxTries, BlockFilter filter,
                               Collection<Material> blocks, int active, int goal, int maxTotalSpawns,
                               SpawnCompletion completion, AfterGoal afterGoal, RespawnPolicy respawn,
                               SpawnOptions options, Collection<SpawnTemplate> templates) {
        super(registry, active, goal, maxTotalSpawns, completion, afterGoal, respawn, options, templates);
        this.surface = surface;
        this.maxTries = Math.max(1, maxTries);
        this.filter = filter;
        this.blocks = blocks.isEmpty() ? Set.of() : EnumSet.copyOf(blocks);
    }

    public boolean surface() {
        return surface;
    }

    public int maxTries() {
        return maxTries;
    }

    public BlockFilter filter() {
        return filter;
    }

    public Set<Material> blocks() {
        return blocks;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayInfo(Player player, HBHunt hunt) {
        return registry.getLanguageService().message("Hunt.Behavior.RandomSpawn");
    }

    public static AreaProvider areaOf(HBHunt hunt) {
        var requirement = hunt.getRequirements().findOrNull(AreaRequirement.class);
        return requirement == null ? null : requirement.area();
    }

    public Location pickColumn(HBHunt hunt) {
        var area = areaOf(hunt);
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

    public Location pickInChunk(HBHunt hunt, Location column, Predicate<Location> isFree) {
        var area = areaOf(hunt);
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
            if (isOpen(block) && canStandOn(block.getRelative(0, -1, 0))) {
                return block.getLocation();
            }
        }
        return null;
    }

    private static boolean isOpen(Block block) {
        if (block.isEmpty()) {
            return true;
        }
        var type = block.getType();
        return !block.isLiquid() && !type.isSolid() && !(block.getBlockData() instanceof Bisected)
                && Tag.REPLACEABLE.isTagged(type);
    }

    private boolean canStandOn(Block block) {
        var type = block.getType();
        if (!type.isSolid()) {
            return false;
        }
        return filter == BlockFilter.WHITELIST ? blocks.contains(type) : !blocks.contains(type);
    }

    public float randomYaw() {
        return ThreadLocalRandom.current().nextInt(4) * 90f;
    }

    @Override
    protected void saveSource(ConfigurationSection section) {
        section.set("surface", surface);
        section.set("maxTries", maxTries);
        section.set("blocks.mode", filter.name());
        section.set("blocks.list", blocks.stream().map(Material::name).sorted().toList());
    }

    public static RandomSpawnBehavior fromConfig(ServiceRegistry registry, ConfigurationSection section) {
        if (section == null) {
            return new RandomSpawnBehavior(registry, true, 20, BlockFilter.BLACKLIST, List.of(), 1, 1, -1,
                    SpawnCompletion.PER_PLAYER, AfterGoal.DENY, RespawnPolicy.DEFAULT, SpawnOptions.DEFAULT, List.of());
        }

        return new RandomSpawnBehavior(registry,
                section.getBoolean("surface", true),
                section.getInt("maxTries", 20),
                BlockFilter.of(section.getString("blocks.mode")),
                parseBlocks(section.getStringList("blocks.list")),
                section.getInt("active", 1),
                section.getInt("goal", 1),
                section.getInt("maxTotalSpawns", -1),
                SpawnCompletion.of(section.getString("completion")),
                AfterGoal.of(section.getString("afterGoal")),
                RespawnPolicy.fromConfig(section.getConfigurationSection("respawn")),
                SpawnOptions.fromConfig(section),
                readTemplates(section.getConfigurationSection("templates")));
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
