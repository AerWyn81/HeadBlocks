package fr.aerwyn81.headblocks.holograms.types;

import fr.aerwyn81.headblocks.HeadBlocks;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BasicHologramTest {

    private MockedStatic<HeadBlocks> headBlocks;
    private World world;
    private Location location;
    private TextDisplay display;

    @BeforeEach
    void setUp() {
        headBlocks = mockStatic(HeadBlocks.class);
        headBlocks.when(HeadBlocks::getInstance).thenReturn(mock(HeadBlocks.class));

        world = mock(World.class);
        location = mock(Location.class);
        display = mock(TextDisplay.class);

        lenient().when(location.getWorld()).thenReturn(world);
        lenient().when(world.spawn(location, TextDisplay.class)).thenReturn(display);
    }

    @AfterEach
    void tearDown() {
        headBlocks.close();
    }

    @Test
    @DisplayName("the hologram is spawned and fully configured")
    void spawns_and_configures_the_display() {
        new BasicHologram().create("holo", location, List.of("&aLine one", "Line two"));

        verify(world).spawn(location, TextDisplay.class);
        verify(display).setText("§aLine one\nLine two");
        verify(display).setVisibleByDefault(false);
        verify(display).setPersistent(false);
        verify(display).setBillboard(Display.Billboard.CENTER);
    }

    @Test
    @DisplayName("a null world is reported instead of spawning")
    void null_world_does_not_spawn() {
        when(location.getWorld()).thenReturn(null);

        new BasicHologram().create("holo", location, List.of("line"));

        verifyNoInteractions(world);
    }

    /**
     * The Consumer-taking spawn overload switched from org.bukkit.util.Consumer to
     * java.util.function.Consumer in 1.20.5, so a jar bound to either one throws NoSuchMethodError on
     * the other half of the supported range. Reading the constant pool is what actually proves which
     * overload was linked — a Mockito verification would only describe this run.
     */
    @Test
    @DisplayName("the compiled class links no Consumer-taking overload")
    void does_not_bind_to_the_consumer_spawn_overload() throws Exception {
        byte[] bytecode;
        try (var in = BasicHologram.class.getResourceAsStream("BasicHologram.class")) {
            assertThat(in).isNotNull();
            bytecode = in.readAllBytes();
        }

        assertThat(new String(bytecode, StandardCharsets.ISO_8859_1)).doesNotContain("Consumer");
    }
}
