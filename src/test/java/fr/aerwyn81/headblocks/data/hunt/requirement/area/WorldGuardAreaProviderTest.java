package fr.aerwyn81.headblocks.data.hunt.requirement.area;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * The provider that decides whether a WorldGuard area can be enforced at all.
 * <p>
 * Its {@code isAvailable()} is what makes {@code AreaRequirement} answer {@code unresolvable}, so an
 * unresolvable region silently stops gating a hunt. Every path that returns false matters.
 */
class WorldGuardAreaProviderTest {

    private static final String WORLD = "world";
    private static final String REGION = "spawn_hunt";

    private MockedStaticBundle statics;

    /**
     * The three static entry points the provider goes through, opened and closed as one so no test
     * can leak a mock into the next.
     */
    private static final class MockedStaticBundle implements AutoCloseable {
        private final org.mockito.MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
        private final org.mockito.MockedStatic<WorldGuard> worldGuard = mockStatic(WorldGuard.class);
        private final org.mockito.MockedStatic<BukkitAdapter> adapter = mockStatic(BukkitAdapter.class);

        @Override
        public void close() {
            adapter.close();
            worldGuard.close();
            bukkit.close();
        }
    }

    @BeforeEach
    void setUp() {
        statics = new MockedStaticBundle();
    }

    @AfterEach
    void tearDown() {
        statics.close();
    }

    // --- Helpers ---

    private void worldGuardEnabled(boolean enabled) {
        PluginManager pluginManager = mock(PluginManager.class);
        when(pluginManager.isPluginEnabled("WorldGuard")).thenReturn(enabled);
        statics.bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
    }

    private World loadedWorld(String name) {
        World world = mock(World.class);
        lenient().when(world.getName()).thenReturn(name);
        statics.bukkit.when(() -> Bukkit.getWorld(name)).thenReturn(world);
        return world;
    }

    /**
     * Wires the whole WorldGuard chain down to a region, or to its absence when {@code region} is null.
     */
    private void regionContainerReturns(World world, ProtectedRegion region) {
        RegionManager regionManager = mock(RegionManager.class);
        lenient().when(regionManager.getRegion(REGION)).thenReturn(region);

        RegionContainer container = mock(RegionContainer.class);
        RegionManager finalManager = regionManager;
        lenient().when(container.get(any())).thenReturn(finalManager);

        WorldGuardPlatform platform = mock(WorldGuardPlatform.class);
        lenient().when(platform.getRegionContainer()).thenReturn(container);

        WorldGuard instance = mock(WorldGuard.class);
        lenient().when(instance.getPlatform()).thenReturn(platform);
        statics.worldGuard.when(WorldGuard::getInstance).thenReturn(instance);

        statics.adapter.when(() -> BukkitAdapter.adapt(world))
                .thenReturn(mock(com.sk89q.worldedit.world.World.class));
    }

    private ProtectedRegion region(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        ProtectedRegion region = mock(ProtectedRegion.class);
        lenient().when(region.getMinimumPoint()).thenReturn(BlockVector3.at(minX, minY, minZ));
        lenient().when(region.getMaximumPoint()).thenReturn(BlockVector3.at(maxX, maxY, maxZ));
        return region;
    }

    private Location locationIn(World world, int x, int y, int z) {
        return new Location(world, x, y, z);
    }

    private WorldGuardAreaProvider provider() {
        return new WorldGuardAreaProvider(WORLD, REGION);
    }

    // =========================================================================
    // 1. Serialization, no WorldGuard involved
    // =========================================================================

    @Nested
    class Serialization {

        @Test
        void getType_isTheWorldGuardType() {
            assertThat(provider().getType()).isEqualTo(WorldGuardAreaProvider.TYPE);
        }

        @Test
        void getDescription_namesTheRegionAndItsWorld() {
            assertThat(provider().getDescription()).isEqualTo("spawn_hunt (world)");
        }

        @Test
        void saveTo_thenFromSection_roundTrips() {
            YamlConfiguration yaml = new YamlConfiguration();
            ConfigurationSection section = yaml.createSection("area");
            provider().saveTo(section);

            assertThat(section.getString("type")).isEqualTo(WorldGuardAreaProvider.TYPE);

            WorldGuardAreaProvider loaded = WorldGuardAreaProvider.fromSection(section);
            assertThat(loaded.getWorldName()).isEqualTo(WORLD);
            assertThat(loaded.getRegionId()).isEqualTo(REGION);
        }

        @Test
        void fromSection_missingKeys_yieldsEmptyStringsRatherThanNull() {
            YamlConfiguration yaml = new YamlConfiguration();
            WorldGuardAreaProvider loaded = WorldGuardAreaProvider.fromSection(yaml.createSection("area"));

            assertThat(loaded.getWorldName()).isEmpty();
            assertThat(loaded.getRegionId()).isEmpty();
        }
    }

    // =========================================================================
    // 2. WorldGuard missing — everything must fail closed on contains, and
    //    report unavailable so the requirement can fail open on its own terms
    // =========================================================================

    @Nested
    class WithoutWorldGuard {

        @BeforeEach
        void noWorldGuard() {
            worldGuardEnabled(false);
        }

        @Test
        void isAvailable_isFalse() {
            assertThat(provider().isAvailable()).isFalse();
        }

        @Test
        void contains_isFalse() {
            World world = mock(World.class);
            lenient().when(world.getName()).thenReturn(WORLD);

            assertThat(provider().contains(locationIn(world, 5, 65, 5))).isFalse();
        }

        @Test
        void getBounds_isNull() {
            assertThat(provider().getBounds()).isNull();
        }
    }

    // =========================================================================
    // 3. WorldGuard present
    // =========================================================================

    @Nested
    class WithWorldGuard {

        @BeforeEach
        void worldGuardIsThere() {
            worldGuardEnabled(true);
        }

        @Test
        void contains_nullLocation_isFalse() {
            assertThat(provider().contains(null)).isFalse();
        }

        @Test
        void contains_locationWithoutWorld_isFalse() {
            assertThat(provider().contains(new Location(null, 5, 65, 5))).isFalse();
        }

        @Test
        void contains_locationInAnotherWorld_isFalse() {
            World other = mock(World.class);
            when(other.getName()).thenReturn("nether");

            assertThat(provider().contains(locationIn(other, 5, 65, 5))).isFalse();
        }

        @Test
        void contains_insideTheRegion_isTrue() {
            World world = loadedWorld(WORLD);
            ProtectedRegion region = region(0, 60, 0, 10, 70, 10);
            when(region.contains(any(BlockVector3.class))).thenReturn(true);
            regionContainerReturns(world, region);

            assertThat(provider().contains(locationIn(world, 5, 65, 5))).isTrue();
        }

        @Test
        void contains_outsideTheRegion_isFalse() {
            World world = loadedWorld(WORLD);
            ProtectedRegion region = region(0, 60, 0, 10, 70, 10);
            when(region.contains(any(BlockVector3.class))).thenReturn(false);
            regionContainerReturns(world, region);

            assertThat(provider().contains(locationIn(world, 500, 65, 500))).isFalse();
        }

        @Test
        void contains_usesBlockCoordinates() {
            World world = loadedWorld(WORLD);
            ProtectedRegion region = region(0, 60, 0, 10, 70, 10);
            when(region.contains(any(BlockVector3.class))).thenReturn(true);
            regionContainerReturns(world, region);

            provider().contains(new Location(world, 5.9, 65.4, -0.2));

            // -0.2 floors to -1, not truncates to 0: a player standing just outside must not be
            // reported one block inside.
            verify(region).contains(BlockVector3.at(5, 65, -1));
        }

        @Test
        void isAvailable_regionResolves_isTrue() {
            World world = loadedWorld(WORLD);
            regionContainerReturns(world, region(0, 60, 0, 10, 70, 10));

            assertThat(provider().isAvailable()).isTrue();
        }

        @Test
        void isAvailable_worldNotLoaded_isFalse() {
            statics.bukkit.when(() -> Bukkit.getWorld(WORLD)).thenReturn(null);

            assertThat(provider().isAvailable()).isFalse();
        }

        @Test
        void isAvailable_regionDeleted_isFalse() {
            World world = loadedWorld(WORLD);
            regionContainerReturns(world, null);

            assertThat(provider().isAvailable()).isFalse();
        }

        @Test
        void isAvailable_worldGuardThrows_isFalse() {
            World world = loadedWorld(WORLD);
            statics.worldGuard.when(WorldGuard::getInstance).thenThrow(new IllegalStateException("not ready"));
            statics.adapter.when(() -> BukkitAdapter.adapt(world))
                    .thenReturn(mock(com.sk89q.worldedit.world.World.class));

            // A WorldGuard that is present but not initialized must not propagate out of a click.
            assertThat(provider().isAvailable()).isFalse();
        }

        @Test
        void getBounds_returnsMinThenMax() {
            World world = loadedWorld(WORLD);
            regionContainerReturns(world, region(0, 60, -5, 10, 70, 25));

            assertThat(provider().getBounds()).containsExactly(0, 60, -5, 10, 70, 25);
        }

        @Test
        void getBounds_regionDeleted_isNull() {
            World world = loadedWorld(WORLD);
            regionContainerReturns(world, null);

            assertThat(provider().getBounds()).isNull();
        }

        @Test
        void getBounds_worldNotLoaded_isNull() {
            statics.bukkit.when(() -> Bukkit.getWorld(WORLD)).thenReturn(null);

            assertThat(provider().getBounds()).isNull();
        }
    }
}
