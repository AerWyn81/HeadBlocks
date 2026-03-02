package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TpCommandTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private Player player;

    private Tp command;

    @BeforeEach
    void setUp() {
        command = new Tp(registry);
    }

    @Test
    void perform_validArgs_teleportsPlayer() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            World world = mock(World.class);
            bukkit.when(() -> Bukkit.getWorld("overworld")).thenReturn(world);

            boolean result = command.perform(player, new String[]{
                    "tp", "overworld", "10.5", "64.0", "20.5", "0.0", "0.0"
            });

            assertThat(result).isTrue();

            ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);
            verify(player).teleport(captor.capture());

            Location loc = captor.getValue();
            assertThat(loc.getWorld()).isSameAs(world);
            assertThat(loc.getX()).isEqualTo(10.5);
            assertThat(loc.getY()).isEqualTo(64.0);
            assertThat(loc.getZ()).isEqualTo(20.5);
        }
    }

    @Test
    void perform_invalidArgs_catchesException() {
        boolean result = command.perform(player, new String[]{"tp", "bad"});

        assertThat(result).isTrue();
        verify(player, never()).teleport(any(Location.class));
    }

    @Test
    void tabComplete_returnsEmpty() {
        assertThat(command.tabComplete(player, new String[]{"tp"})).isEmpty();
    }
}
