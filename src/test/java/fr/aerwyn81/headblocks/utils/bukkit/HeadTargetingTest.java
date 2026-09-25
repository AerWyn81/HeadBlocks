package fr.aerwyn81.headblocks.utils.bukkit;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.HeadVisualService;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeadTargetingTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private HeadVisualService visualService;

    @Mock
    private HeadService headService;

    @Mock
    private Player player;

    @Mock
    private Block block;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getVisualService()).thenReturn(visualService);
        lenient().when(registry.getHeadService()).thenReturn(headService);
    }

    @Test
    void entityHead_winsOverTheBlockBehindIt() {
        HeadLocation head = mock(HeadLocation.class);
        when(visualService.lookedAtHead(player, 50)).thenReturn(head);

        assertThat(HeadTargeting.lookedAt(player, registry, 50)).isSameAs(head);
        verify(player, never()).getTargetBlock(any(), anyInt());
    }

    @Test
    void blockHead_isFoundWhenNoEntityIsTargeted() {
        HeadLocation head = mock(HeadLocation.class);
        Location location = mock(Location.class);
        when(player.getTargetBlock(null, 50)).thenReturn(block);
        when(block.getLocation()).thenReturn(location);
        when(headService.getHeadAt(location)).thenReturn(head);

        assertThat(HeadTargeting.lookedAt(player, registry, 50)).isSameAs(head);
    }

    @Test
    void lookingAtTheSky_findsNothing() {
        when(player.getTargetBlock(null, 50)).thenReturn(block);
        when(block.isEmpty()).thenReturn(true);

        assertThat(HeadTargeting.lookedAt(player, registry, 50)).isNull();
        verifyNoInteractions(headService);
    }
}
