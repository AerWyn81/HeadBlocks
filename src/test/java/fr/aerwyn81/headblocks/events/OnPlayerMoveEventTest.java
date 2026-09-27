package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.services.AreaEnforcementService;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnPlayerMoveEventTest {

    @Mock
    ServiceRegistry registry;

    @Mock
    AreaEnforcementService area;

    @Mock
    Player player;

    @Mock
    World world;

    private OnPlayerMoveEvent listener;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getAreaEnforcementService()).thenReturn(area);
        lenient().when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        listener = new OnPlayerMoveEvent(registry);
    }

    private Location at(double x, double y, double z) {
        return new Location(world, x, y, z, 0f, 0f);
    }

    private PlayerMoveEvent move(Location from, Location to) {
        var event = mock(PlayerMoveEvent.class);
        lenient().when(event.getFrom()).thenReturn(from);
        lenient().when(event.getTo()).thenReturn(to);
        lenient().when(event.getPlayer()).thenReturn(player);
        return event;
    }

    @Nested
    class Move {

        @Test
        void noDestination_isIgnored() {
            var event = move(at(0, 64, 0), null);

            listener.onMove(event);

            verifyNoInteractions(area);
        }

        @Test
        void sameBlock_isIgnored() {
            var event = move(at(0.1, 64, 0.1), at(0.9, 64.5, 0.9));

            listener.onMove(event);

            verifyNoInteractions(area);
        }

        @Test
        void otherWorld_countsAsACrossing() {
            World other = mock(World.class);
            var event = move(at(0, 64, 0), new Location(other, 0, 64, 0));
            when(area.hasNothingToEnforce(player)).thenReturn(true);

            listener.onMove(event);

            verify(area).hasNothingToEnforce(player);
        }

        @Test
        void nothingToEnforce_letsThePlayerMove() {
            var event = move(at(0, 64, 0), at(1, 64, 0));
            when(area.hasNothingToEnforce(player)).thenReturn(true);

            listener.onMove(event);

            verify(area, never()).evaluate(any(), any());
            verify(event, never()).setTo(any());
        }

        @Test
        void spectator_isExempt() {
            var event = move(at(0, 64, 0), at(1, 64, 0));
            when(player.getGameMode()).thenReturn(GameMode.SPECTATOR);

            listener.onMove(event);

            verify(area, never()).evaluate(any(), any());
        }

        @Test
        void bypassPermission_isExempt() {
            var event = move(at(0, 64, 0), at(1, 64, 0));
            when(player.hasPermission("headblocks.area.bypass")).thenReturn(true);

            listener.onMove(event);

            verify(area, never()).evaluate(any(), any());
        }

        @Test
        void legacyZoneBypassPermission_isExempt() {
            var event = move(at(0, 64, 0), at(1, 64, 0));
            lenient().when(player.hasPermission(anyString())).thenReturn(false);
            when(player.hasPermission("headblocks.zone.bypass")).thenReturn(true);

            listener.onMove(event);

            verify(area, never()).evaluate(any(), any());
        }

        @Test
        void notConfined_letsThePlayerMove() {
            var to = at(1, 64, 0);
            var event = move(at(0, 64, 0), to);
            when(area.evaluate(player, to)).thenReturn(AreaEnforcementService.Decision.NONE);

            listener.onMove(event);

            verify(event, never()).setTo(any());
        }

        @Test
        void confined_withARecoveryPoint_sendsThePlayerThere() {
            var from = at(0, 64, 0);
            var to = at(1, 64, 0);
            var recovery = at(5, 64, 5);
            var event = move(from, to);
            when(area.evaluate(player, to)).thenReturn(AreaEnforcementService.Decision.CONFINE);
            when(area.getRecoveryPoint(player, from)).thenReturn(recovery);

            listener.onMove(event);

            verify(event).setTo(recovery);
        }

        @Test
        void confined_inTheAirWithoutRecovery_usesTheReturnPoint() {
            var from = at(0, 64, 0);
            var to = at(1, 64, 0);
            var returnPoint = at(9, 70, 9);
            var event = move(from, to);
            Block below = mock(Block.class);
            when(world.getBlockAt(any(Location.class))).thenReturn(below);
            when(below.getType()).thenReturn(Material.AIR);
            when(area.evaluate(player, to)).thenReturn(AreaEnforcementService.Decision.CONFINE);
            when(area.getReturnPoint(player)).thenReturn(returnPoint);

            listener.onMove(event);

            verify(event).setTo(returnPoint);
        }

        @Test
        void confined_onTheGroundWithoutRecovery_isHeldInPlaceKeepingTheView() {
            var from = at(0, 64, 0);
            var to = new Location(world, 1, 64, 0, 90f, 30f);
            var event = move(from, to);
            Block below = mock(Block.class);
            when(world.getBlockAt(any(Location.class))).thenReturn(below);
            when(below.getType()).thenReturn(Material.STONE);
            when(area.evaluate(player, to)).thenReturn(AreaEnforcementService.Decision.CONFINE);

            listener.onMove(event);

            var captor = ArgumentCaptor.forClass(Location.class);
            verify(event).setTo(captor.capture());
            assertThat(captor.getValue().getX()).isZero();
            assertThat(captor.getValue().getYaw()).isEqualTo(90f);
            assertThat(captor.getValue().getPitch()).isEqualTo(30f);
            verify(area, never()).getReturnPoint(any());
        }

        @Test
        void confined_inTheAirWithoutAnyPoint_isHeldInPlace() {
            var from = at(0, 64, 0);
            var to = at(1, 64, 0);
            var event = move(from, to);
            Block below = mock(Block.class);
            when(world.getBlockAt(any(Location.class))).thenReturn(below);
            when(below.getType()).thenReturn(Material.AIR);
            when(area.evaluate(player, to)).thenReturn(AreaEnforcementService.Decision.CONFINE);

            listener.onMove(event);

            var captor = ArgumentCaptor.forClass(Location.class);
            verify(event).setTo(captor.capture());
            assertThat(captor.getValue().getBlockX()).isZero();
        }
    }

    @Nested
    class Teleport {

        private PlayerTeleportEvent teleport(Location from, Location to) {
            var event = mock(PlayerTeleportEvent.class);
            lenient().when(event.getFrom()).thenReturn(from);
            lenient().when(event.getTo()).thenReturn(to);
            lenient().when(event.getPlayer()).thenReturn(player);
            return event;
        }

        @Test
        void noDestination_isIgnored() {
            listener.onTeleport(teleport(at(0, 64, 0), null));

            verifyNoInteractions(area);
        }

        @Test
        void nothingToEnforce_isAllowed() {
            var event = teleport(at(0, 64, 0), at(100, 64, 0));
            when(area.hasNothingToEnforce(player)).thenReturn(true);

            listener.onTeleport(event);

            verify(event, never()).setCancelled(anyBoolean());
        }

        @Test
        void notConfined_isAllowed() {
            var to = at(100, 64, 0);
            var event = teleport(at(0, 64, 0), to);
            when(area.evaluate(player, to)).thenReturn(AreaEnforcementService.Decision.NONE);

            listener.onTeleport(event);

            verify(event, never()).setCancelled(anyBoolean());
            verify(event, never()).setTo(any());
        }

        @Test
        void confined_withARecoveryPoint_isRedirected() {
            var from = at(0, 64, 0);
            var to = at(100, 64, 0);
            var recovery = at(5, 64, 5);
            var event = teleport(from, to);
            when(area.evaluate(player, to)).thenReturn(AreaEnforcementService.Decision.CONFINE);
            when(area.getRecoveryPoint(player, from)).thenReturn(recovery);

            listener.onTeleport(event);

            verify(event).setTo(recovery);
            verify(event, never()).setCancelled(anyBoolean());
        }

        @Test
        void confined_withoutRecovery_isCancelled() {
            var to = at(100, 64, 0);
            var event = teleport(at(0, 64, 0), to);
            when(area.evaluate(player, to)).thenReturn(AreaEnforcementService.Decision.CONFINE);

            listener.onTeleport(event);

            verify(event).setCancelled(true);
        }
    }

    @Nested
    class Respawn {

        private PlayerRespawnEvent respawn(Location location) {
            var event = mock(PlayerRespawnEvent.class);
            lenient().when(event.getPlayer()).thenReturn(player);
            lenient().when(event.getRespawnLocation()).thenReturn(location);
            return event;
        }

        @Test
        void nothingToEnforce_keepsTheRespawnPoint() {
            var event = respawn(at(0, 64, 0));
            when(area.hasNothingToEnforce(player)).thenReturn(true);

            listener.onRespawn(event);

            verify(event, never()).setRespawnLocation(any());
        }

        @Test
        void exemptPlayer_keepsTheRespawnPoint() {
            var event = respawn(at(0, 64, 0));
            when(player.getGameMode()).thenReturn(GameMode.SPECTATOR);

            listener.onRespawn(event);

            verify(event, never()).setRespawnLocation(any());
        }

        @Test
        void withARecoveryPoint_respawnsThere() {
            var bed = at(0, 64, 0);
            var recovery = at(5, 64, 5);
            var event = respawn(bed);
            when(area.getRecoveryPoint(player, bed)).thenReturn(recovery);

            listener.onRespawn(event);

            verify(event).setRespawnLocation(recovery);
        }

        @Test
        void withoutRecoveryPoint_keepsTheRespawnPoint() {
            var event = respawn(at(0, 64, 0));

            listener.onRespawn(event);

            verify(event, never()).setRespawnLocation(any());
        }
    }
}
