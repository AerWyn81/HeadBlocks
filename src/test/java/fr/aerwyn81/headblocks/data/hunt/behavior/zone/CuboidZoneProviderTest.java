package fr.aerwyn81.headblocks.data.hunt.behavior.zone;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CuboidZoneProviderTest {

    private Location locationIn(String worldName, double x, double y, double z) {
        World world = mock(World.class);
        lenient().when(world.getName()).thenReturn(worldName);
        return new Location(world, x, y, z);
    }

    @Test
    void constructor_normalizesCorners() {
        CuboidZoneProvider provider = new CuboidZoneProvider("world", 10, 70, 30, 0, 60, 5);

        assertThat(provider.getMinX()).isEqualTo(0);
        assertThat(provider.getMinY()).isEqualTo(60);
        assertThat(provider.getMinZ()).isEqualTo(5);
        assertThat(provider.getMaxX()).isEqualTo(10);
        assertThat(provider.getMaxY()).isEqualTo(70);
        assertThat(provider.getMaxZ()).isEqualTo(30);
    }

    @Test
    void contains_pointInside_returnsTrue() {
        CuboidZoneProvider provider = new CuboidZoneProvider("world", 0, 60, 0, 10, 70, 10);

        assertThat(provider.contains(locationIn("world", 5, 65, 5))).isTrue();
    }

    @Test
    void contains_pointOnBorder_returnsTrue() {
        CuboidZoneProvider provider = new CuboidZoneProvider("world", 0, 60, 0, 10, 70, 10);

        assertThat(provider.contains(locationIn("world", 0, 60, 0))).isTrue();
        assertThat(provider.contains(locationIn("world", 10, 70, 10))).isTrue();
    }

    @Test
    void contains_pointOutside_returnsFalse() {
        CuboidZoneProvider provider = new CuboidZoneProvider("world", 0, 60, 0, 10, 70, 10);

        assertThat(provider.contains(locationIn("world", 11, 65, 5))).isFalse();
        assertThat(provider.contains(locationIn("world", 5, 59, 5))).isFalse();
    }

    @Test
    void contains_wrongWorld_returnsFalse() {
        CuboidZoneProvider provider = new CuboidZoneProvider("world", 0, 60, 0, 10, 70, 10);

        assertThat(provider.contains(locationIn("nether", 5, 65, 5))).isFalse();
    }

    @Test
    void contains_nullLocation_returnsFalse() {
        CuboidZoneProvider provider = new CuboidZoneProvider("world", 0, 60, 0, 10, 70, 10);

        assertThat(provider.contains(null)).isFalse();
    }

    @Test
    void isAvailable_worldLoaded_returnsTrue() {
        CuboidZoneProvider provider = new CuboidZoneProvider("world", 0, 60, 0, 10, 70, 10);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(mock(World.class));
            assertThat(provider.isAvailable()).isTrue();
        }
    }

    @Test
    void isAvailable_worldMissing_returnsFalse() {
        CuboidZoneProvider provider = new CuboidZoneProvider("world", 0, 60, 0, 10, 70, 10);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(null);
            assertThat(provider.isAvailable()).isFalse();
        }
    }

    @Test
    void saveTo_andFromSection_roundTrip() {
        CuboidZoneProvider provider = new CuboidZoneProvider("world", 0, 60, 5, 10, 70, 30);

        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("zone");
        provider.saveTo(section);

        assertThat(section.getString("type")).isEqualTo("cuboid");

        CuboidZoneProvider loaded = CuboidZoneProvider.fromSection(section);
        assertThat(loaded.getWorldName()).isEqualTo("world");
        assertThat(loaded.getMinX()).isEqualTo(0);
        assertThat(loaded.getMinY()).isEqualTo(60);
        assertThat(loaded.getMinZ()).isEqualTo(5);
        assertThat(loaded.getMaxX()).isEqualTo(10);
        assertThat(loaded.getMaxY()).isEqualTo(70);
        assertThat(loaded.getMaxZ()).isEqualTo(30);
    }
}
