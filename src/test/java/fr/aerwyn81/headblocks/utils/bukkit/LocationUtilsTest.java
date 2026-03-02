package fr.aerwyn81.headblocks.utils.bukkit;

import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LocationUtilsTest {

    private Location mockLocation(World world, double x, double y, double z) {
        Location loc = mock(Location.class);
        when(loc.getWorld()).thenReturn(world);
        when(loc.getX()).thenReturn(x);
        when(loc.getY()).thenReturn(y);
        when(loc.getZ()).thenReturn(z);
        when(loc.getBlockX()).thenReturn((int) Math.floor(x));
        when(loc.getBlockY()).thenReturn((int) Math.floor(y));
        when(loc.getBlockZ()).thenReturn((int) Math.floor(z));
        return loc;
    }

    // --- areEquals ---

    @Test
    void areEquals_sameBlockCoords_sameWorld_returnsTrue() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("overworld");

        Location loc1 = mockLocation(world, 10.3, 64.7, 20.1);
        Location loc2 = mockLocation(world, 10.9, 64.2, 20.8);

        assertThat(LocationUtils.areEquals(loc1, loc2)).isTrue();
    }

    @Test
    void areEquals_differentX_returnsFalse() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("overworld");

        Location loc1 = mockLocation(world, 10.0, 64.0, 20.0);
        Location loc2 = mockLocation(world, 11.0, 64.0, 20.0);

        assertThat(LocationUtils.areEquals(loc1, loc2)).isFalse();
    }

    @Test
    void areEquals_differentWorld_returnsFalse() {
        World world1 = mock(World.class);
        when(world1.getName()).thenReturn("overworld");
        World world2 = mock(World.class);
        when(world2.getName()).thenReturn("nether");

        Location loc1 = mockLocation(world1, 10.0, 64.0, 20.0);
        Location loc2 = mockLocation(world2, 10.0, 64.0, 20.0);

        assertThat(LocationUtils.areEquals(loc1, loc2)).isFalse();
    }

    @Test
    void areEquals_firstNull_returnsFalse() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("overworld");
        Location loc2 = mockLocation(world, 10.0, 64.0, 20.0);

        assertThat(LocationUtils.areEquals(null, loc2)).isFalse();
    }

    @Test
    void areEquals_secondNull_returnsFalse() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("overworld");
        Location loc1 = mockLocation(world, 10.0, 64.0, 20.0);

        assertThat(LocationUtils.areEquals(loc1, null)).isFalse();
    }

    @Test
    void areEquals_nullWorld_returnsFalse() {
        Location loc1 = mockLocation(null, 10.0, 64.0, 20.0);
        Location loc2 = mockLocation(null, 10.0, 64.0, 20.0);

        assertThat(LocationUtils.areEquals(loc1, loc2)).isFalse();
    }

    // --- parseLocationPlaceholders ---

    @Test
    void parseLocationPlaceholders_replacesAllPlaceholders() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("overworld");
        Location loc = mockLocation(world, 10.5, 64.0, 20.5);

        try (MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class)) {
            mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));

            String result = LocationUtils.parseLocationPlaceholders(
                    "x=%x% y=%y% z=%z% world=%world% name=%worldName%", loc);

            assertThat(result).isEqualTo("x=10.5 y=64.0 z=20.5 world=overworld name=overworld");
        }
    }

    @Test
    void parseLocationPlaceholders_nullWorld_usesUnknownWorld() {
        Location loc = mockLocation(null, 5.0, 10.0, 15.0);

        try (MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class)) {
            mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));

            String result = LocationUtils.parseLocationPlaceholders("world=%world%", loc);

            assertThat(result).contains("&cUnknownWorld");
        }
    }

    // --- toFormattedString ---

    @Test
    void toFormattedString_nullWorld_containsUnknownWorld() {
        Location loc = mockLocation(null, 1.0, 2.0, 3.0);

        try (MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class)) {
            mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));

            String result = LocationUtils.toFormattedString(loc);

            assertThat(result).contains("&cUnknownWorld");
        }
    }

    @Test
    void toFormattedString_withValidWorld_containsWorldName() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("overworld");
        Location loc = mockLocation(world, 10.5, 64.0, 20.5);

        try (MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class)) {
            mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));

            String result = LocationUtils.toFormattedString(loc);

            assertThat(result).contains("overworld");
            assertThat(result).contains("10.5");
            assertThat(result).contains("64.0");
            assertThat(result).contains("20.5");
        }
    }
}
