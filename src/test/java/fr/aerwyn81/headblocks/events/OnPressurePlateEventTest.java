package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.TimedRunData;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.data.hunt.behavior.Behavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.FreeBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.TimedBehavior;
import fr.aerwyn81.headblocks.services.HuntService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.services.TimedRunManager;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnPressurePlateEventTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private HuntService huntService;

    @Mock
    private StorageService storageService;

    @Mock
    private LanguageService languageService;

    @Mock
    private PlayerInteractEvent event;

    @Mock
    private Player player;

    @Mock
    private Block clickedBlock;

    private OnPressurePlateEvent handler;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getHuntService()).thenReturn(huntService);
        lenient().when(registry.getStorageService()).thenReturn(storageService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(languageService.message(anyString())).thenReturn("mock-message");

        handler = new OnPressurePlateEvent(registry);
    }

    @AfterEach
    void tearDown() {
        TimedRunManager.clearAll();
    }

    // --- Early exit: not PHYSICAL action ---

    @Test
    void notPhysicalAction_ignored() {
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);

        handler.onPressurePlate(event);

        verifyNoInteractions(huntService);
    }

    // --- Early exit: null clicked block ---

    @Test
    void nullClickedBlock_ignored() {
        when(event.getAction()).thenReturn(Action.PHYSICAL);
        when(event.getClickedBlock()).thenReturn(null);

        handler.onPressurePlate(event);

        verifyNoInteractions(huntService);
    }

    // --- No matching start plate ---

    @Test
    void noMatchingStartPlate_ignored() throws InternalException {
        when(event.getAction()).thenReturn(Action.PHYSICAL);
        when(event.getClickedBlock()).thenReturn(clickedBlock);
        when(event.getPlayer()).thenReturn(player);

        Location blockLoc = mockLocation(mock(World.class), 10, 20, 30);
        when(clickedBlock.getLocation()).thenReturn(blockLoc);

        // Hunt has no timed behavior
        Hunt hunt = mock(Hunt.class);
        when(hunt.isActive()).thenReturn(true);
        when(hunt.getBehaviors()).thenReturn(List.of(new FreeBehavior()));
        when(huntService.getAllHunts()).thenReturn(List.of(hunt));

        handler.onPressurePlate(event);

        verify(storageService, never()).getHeadsPlayerForHunt(any(), anyString());
    }

    // --- Matching plate but different world ---

    @Test
    void matchingPlate_differentWorld_ignored() throws InternalException {
        when(event.getAction()).thenReturn(Action.PHYSICAL);
        when(event.getClickedBlock()).thenReturn(clickedBlock);
        when(event.getPlayer()).thenReturn(player);

        World world1 = mock(World.class);
        World world2 = mock(World.class);

        Location blockLoc = mockLocation(world1, 10, 20, 30);
        when(clickedBlock.getLocation()).thenReturn(blockLoc);

        Location plateLoc = mockLocation(world2, 10, 20, 30);

        TimedBehavior timed = mock(TimedBehavior.class);
        when(timed.startPlateLocation()).thenReturn(plateLoc);

        Hunt hunt = mock(Hunt.class);
        when(hunt.isActive()).thenReturn(true);
        when(hunt.getBehaviors()).thenReturn(List.<Behavior>of(timed));
        when(huntService.getAllHunts()).thenReturn(List.of(hunt));

        handler.onPressurePlate(event);

        verify(storageService, never()).getHeadsPlayerForHunt(any(), anyString());
    }

    // --- Matching plate but different coordinates ---

    @Test
    void matchingPlate_differentCoords_ignored() throws InternalException {
        when(event.getAction()).thenReturn(Action.PHYSICAL);
        when(event.getClickedBlock()).thenReturn(clickedBlock);
        when(event.getPlayer()).thenReturn(player);

        World world = mock(World.class);

        Location blockLoc = mockLocation(world, 10, 20, 30);
        when(clickedBlock.getLocation()).thenReturn(blockLoc);

        Location plateLoc = mockLocation(world, 50, 60, 70);

        TimedBehavior timed = mock(TimedBehavior.class);
        when(timed.startPlateLocation()).thenReturn(plateLoc);

        Hunt hunt = mock(Hunt.class);
        when(hunt.isActive()).thenReturn(true);
        when(hunt.getBehaviors()).thenReturn(List.<Behavior>of(timed));
        when(huntService.getAllHunts()).thenReturn(List.of(hunt));

        handler.onPressurePlate(event);

        verify(storageService, never()).getHeadsPlayerForHunt(any(), anyString());
    }

    // --- Matching plate: already completed ---

    @Test
    void matchingPlate_alreadyCompleted_sendsMessage() throws InternalException {
        UUID playerUuid = UUID.randomUUID();

        when(event.getAction()).thenReturn(Action.PHYSICAL);
        when(event.getClickedBlock()).thenReturn(clickedBlock);
        when(event.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(playerUuid);

        World world = mock(World.class);
        Location blockLoc = mockLocation(world, 10, 20, 30);
        when(clickedBlock.getLocation()).thenReturn(blockLoc);

        Location plateLoc = mockLocation(world, 10, 20, 30);
        TimedBehavior timed = mock(TimedBehavior.class);
        when(timed.startPlateLocation()).thenReturn(plateLoc);

        Hunt hunt = mock(Hunt.class);
        when(hunt.isActive()).thenReturn(true);
        when(hunt.getBehaviors()).thenReturn(List.<Behavior>of(timed));
        when(hunt.getId()).thenReturn("hunt1");
        when(hunt.getHeadCount()).thenReturn(3);
        when(huntService.getAllHunts()).thenReturn(List.of(hunt));

        // Player already found all 3 heads
        ArrayList<UUID> foundHeads = new ArrayList<>(List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
        when(storageService.getHeadsPlayerForHunt(playerUuid, "hunt1")).thenReturn(foundHeads);

        try (MockedStatic<TimedRunManager> trm = mockStatic(TimedRunManager.class)) {
            handler.onPressurePlate(event);

            verify(player).sendMessage(anyString());
            verify(languageService).message("Messages.TimedAlreadyCompleted");
            trm.verify(() -> TimedRunManager.startRun(any(), anyString()), never());
        }
    }

    // --- Matching plate: new run starts ---

    @Test
    void matchingPlate_newRun_startsRun() throws InternalException {
        UUID playerUuid = UUID.randomUUID();

        when(event.getAction()).thenReturn(Action.PHYSICAL);
        when(event.getClickedBlock()).thenReturn(clickedBlock);
        when(event.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(playerUuid);

        World world = mock(World.class);
        Location blockLoc = mockLocation(world, 10, 20, 30);
        when(clickedBlock.getLocation()).thenReturn(blockLoc);

        Location plateLoc = mockLocation(world, 10, 20, 30);
        TimedBehavior timed = mock(TimedBehavior.class);
        when(timed.startPlateLocation()).thenReturn(plateLoc);

        Hunt hunt = mock(Hunt.class);
        when(hunt.isActive()).thenReturn(true);
        when(hunt.getBehaviors()).thenReturn(List.<Behavior>of(timed));
        when(hunt.getId()).thenReturn("hunt1");
        when(hunt.getHeadCount()).thenReturn(5);
        when(hunt.getDisplayName()).thenReturn("Test Hunt");
        when(huntService.getAllHunts()).thenReturn(List.of(hunt));

        // Player has found 2 of 5 heads
        ArrayList<UUID> foundHeads = new ArrayList<>(List.of(UUID.randomUUID(), UUID.randomUUID()));
        when(storageService.getHeadsPlayerForHunt(playerUuid, "hunt1")).thenReturn(foundHeads);

        try (MockedStatic<TimedRunManager> trm = mockStatic(TimedRunManager.class)) {
            trm.when(() -> TimedRunManager.getRun(playerUuid)).thenReturn(null);
            trm.when(() -> TimedRunManager.isInRun(playerUuid, "hunt1")).thenReturn(false);

            handler.onPressurePlate(event);

            trm.verify(() -> TimedRunManager.startRun(playerUuid, "hunt1"));
            verify(player).sendMessage(anyString());
            verify(languageService).message("Messages.TimedStarted");
        }
    }

    // --- Matching plate: restart resets and starts ---

    @Test
    void matchingPlate_restart_resetsAndStarts() throws InternalException {
        UUID playerUuid = UUID.randomUUID();

        when(event.getAction()).thenReturn(Action.PHYSICAL);
        when(event.getClickedBlock()).thenReturn(clickedBlock);
        when(event.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(playerUuid);

        World world = mock(World.class);
        Location blockLoc = mockLocation(world, 10, 20, 30);
        when(clickedBlock.getLocation()).thenReturn(blockLoc);

        Location plateLoc = mockLocation(world, 10, 20, 30);
        TimedBehavior timed = mock(TimedBehavior.class);
        when(timed.startPlateLocation()).thenReturn(plateLoc);

        Hunt hunt = mock(Hunt.class);
        when(hunt.isActive()).thenReturn(true);
        when(hunt.getBehaviors()).thenReturn(List.<Behavior>of(timed));
        when(hunt.getId()).thenReturn("hunt1");
        when(hunt.getHeadCount()).thenReturn(5);
        when(hunt.getDisplayName()).thenReturn("Test Hunt");
        when(huntService.getAllHunts()).thenReturn(List.of(hunt));

        // Player has found 2 of 5 heads
        ArrayList<UUID> foundHeads = new ArrayList<>(List.of(UUID.randomUUID(), UUID.randomUUID()));
        when(storageService.getHeadsPlayerForHunt(playerUuid, "hunt1")).thenReturn(foundHeads);

        try (MockedStatic<TimedRunManager> trm = mockStatic(TimedRunManager.class)) {
            trm.when(() -> TimedRunManager.getRun(playerUuid)).thenReturn(new TimedRunData("hunt1", System.currentTimeMillis()));
            trm.when(() -> TimedRunManager.isInRun(playerUuid, "hunt1")).thenReturn(true);

            handler.onPressurePlate(event);

            verify(storageService).resetPlayerHunt(playerUuid, "hunt1");
            trm.verify(() -> TimedRunManager.startRun(playerUuid, "hunt1"));
            verify(languageService).message("Messages.TimedRestarted");
        }
    }

    // --- Inactive hunt is skipped ---

    @Test
    void matchingPlate_inactiveHunt_ignored() throws InternalException {
        when(event.getAction()).thenReturn(Action.PHYSICAL);
        when(event.getClickedBlock()).thenReturn(clickedBlock);

        Location blockLoc = mockLocation(mock(World.class), 10, 20, 30);
        when(clickedBlock.getLocation()).thenReturn(blockLoc);

        Hunt hunt = mock(Hunt.class);
        when(hunt.isActive()).thenReturn(false);
        when(huntService.getAllHunts()).thenReturn(List.of(hunt));

        handler.onPressurePlate(event);

        verify(storageService, never()).getHeadsPlayerForHunt(any(), anyString());
    }

    // --- Different hunt run: leaves old run ---

    @Test
    void matchingPlate_differentHuntRun_leavesOldRun() throws InternalException {
        UUID playerUuid = UUID.randomUUID();

        when(event.getAction()).thenReturn(Action.PHYSICAL);
        when(event.getClickedBlock()).thenReturn(clickedBlock);
        when(event.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(playerUuid);

        World world = mock(World.class);
        Location blockLoc = mockLocation(world, 10, 20, 30);
        when(clickedBlock.getLocation()).thenReturn(blockLoc);

        Location plateLoc = mockLocation(world, 10, 20, 30);
        TimedBehavior timed = mock(TimedBehavior.class);
        when(timed.startPlateLocation()).thenReturn(plateLoc);

        Hunt hunt = mock(Hunt.class);
        when(hunt.isActive()).thenReturn(true);
        when(hunt.getBehaviors()).thenReturn(List.<Behavior>of(timed));
        when(hunt.getId()).thenReturn("hunt2");
        when(hunt.getHeadCount()).thenReturn(5);
        when(hunt.getDisplayName()).thenReturn("Hunt Two");
        when(huntService.getAllHunts()).thenReturn(List.of(hunt));

        ArrayList<UUID> foundHeads = new ArrayList<>(List.of(UUID.randomUUID()));
        when(storageService.getHeadsPlayerForHunt(playerUuid, "hunt2")).thenReturn(foundHeads);

        try (MockedStatic<TimedRunManager> trm = mockStatic(TimedRunManager.class)) {
            // Currently in a different hunt's run
            trm.when(() -> TimedRunManager.getRun(playerUuid)).thenReturn(new TimedRunData("hunt1", System.currentTimeMillis()));
            trm.when(() -> TimedRunManager.isInRun(playerUuid, "hunt2")).thenReturn(false);

            handler.onPressurePlate(event);

            trm.verify(() -> TimedRunManager.leaveRun(playerUuid));
            trm.verify(() -> TimedRunManager.startRun(playerUuid, "hunt2"));
        }
    }

    // --- Helper ---

    private Location mockLocation(World world, int x, int y, int z) {
        Location loc = mock(Location.class);
        lenient().when(loc.getWorld()).thenReturn(world);
        lenient().when(loc.getBlockX()).thenReturn(x);
        lenient().when(loc.getBlockY()).thenReturn(y);
        lenient().when(loc.getBlockZ()).thenReturn(z);
        return loc;
    }
}
