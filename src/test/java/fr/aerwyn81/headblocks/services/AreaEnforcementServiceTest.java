package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.data.hunt.behavior.FreeBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.TimedBehavior;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementMode;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementSet;
import fr.aerwyn81.headblocks.data.hunt.requirement.area.AreaMessageMode;
import fr.aerwyn81.headblocks.data.hunt.requirement.area.AreaProvider;
import fr.aerwyn81.headblocks.data.hunt.requirement.types.AreaRequirement;
import fr.aerwyn81.headblocks.utils.scheduler.SchedulerAdapter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AreaEnforcementServiceTest {
    @Mock
    ServiceRegistry registry;

    @Mock
    HuntService huntService;

    @Mock
    StorageService storageService;

    @Mock
    LanguageService languageService;

    @Mock
    HeadService headService;

    @Mock
    ConfigService configService;

    @Mock
    AreaProvider area;

    @Mock
    Player player;

    @Mock
    Location to;

    @Mock
    World world;

    @Mock
    Location returnPoint;

    @Mock
    SchedulerAdapter scheduler;

    private AreaEnforcementService service;
    private UUID uuid;
    private static final String HUNT_ID = "hunt1";

    @BeforeEach
    void setUp() {
        AreaRunManager.clearAll();
        uuid = UUID.randomUUID();
        lenient().when(player.getUniqueId()).thenReturn(uuid);
        lenient().when(registry.getHuntService()).thenReturn(huntService);
        lenient().when(registry.getStorageService()).thenReturn(storageService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(registry.getHeadService()).thenReturn(headService);
        lenient().when(registry.getScheduler()).thenReturn(scheduler);

        lenient().when(world.getName()).thenReturn("world");
        lenient().when(to.getWorld()).thenReturn(world);
        lenient().when(area.getWorldName()).thenReturn("world");
        lenient().when(area.isAvailable()).thenReturn(true);

        service = new AreaEnforcementService(registry);
    }

    @AfterEach
    void tearDown() {
        AreaRunManager.clearAll();
        TimedRunManager.clearAll();
    }

    private HBHunt hunt(String id, int priority, int headCount, AreaProvider areaProvider, Location rp) {
        return hunt(id, priority, headCount, areaProvider, rp, true, true);
    }

    private HBHunt hunt(String id, int priority, int headCount, AreaProvider areaProvider, Location rp,
                        boolean blockExit, boolean resetOnLeave) {
        HBHunt hunt = new HBHunt(configService, id, "Test Hunt", HuntState.ACTIVE, priority, "D");
        for (int i = 0; i < headCount; i++) {
            hunt.addHead(UUID.randomUUID());
        }
        hunt.setBehaviors(List.of(new FreeBehavior()));
        hunt.setRequirements(new RequirementSet(registry, RequirementMode.ALL, List.of(
                new AreaRequirement(registry, areaProvider, rp, blockExit, resetOnLeave, AreaMessageMode.CHAT))));
        return hunt;
    }

    private void registerSingle(HBHunt hunt) {
        lenient().when(huntService.getHuntById(hunt.getId())).thenReturn(hunt);
        lenient().when(huntService.getAllHunts()).thenReturn(List.of(hunt));
    }

    private void foundHeads(int count) throws Exception {
        ArrayList<UUID> found = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            found.add(UUID.randomUUID());
        }
        when(storageService.getHeadsPlayerForHunt(uuid, HUNT_ID)).thenReturn(found);
    }

    // --- evaluate: engagement on entry ---

    @Test
    void evaluate_notEngaged_insideArea_notCompleted_engagesAndMessages() throws Exception {
        registerSingle(hunt(HUNT_ID, 1, 3, area, returnPoint));
        when(area.contains(to)).thenReturn(true);
        foundHeads(1);
        when(languageService.message("Messages.AreaEntered")).thenReturn("Entered %hunt%");

        AreaEnforcementService.Decision decision = service.evaluate(player, to);

        assertThat(decision).isEqualTo(AreaEnforcementService.Decision.NONE);
        assertThat(AreaRunManager.getEngaged(uuid)).isEqualTo(HUNT_ID);
        verify(player).sendMessage("Entered Test Hunt");
    }

    @Test
    void evaluate_notEngaged_returnPointNull_doesNotEngage() {
        registerSingle(hunt(HUNT_ID, 1, 3, area, null));
        lenient().when(area.contains(to)).thenReturn(true);

        AreaEnforcementService.Decision decision = service.evaluate(player, to);

        assertThat(decision).isEqualTo(AreaEnforcementService.Decision.NONE);
        assertThat(AreaRunManager.isEngaged(uuid)).isFalse();
    }

    @Test
    void evaluate_notEngaged_worldMismatch_doesNotEngage() {
        registerSingle(hunt(HUNT_ID, 1, 3, area, returnPoint));
        when(area.getWorldName()).thenReturn("other_world");

        AreaEnforcementService.Decision decision = service.evaluate(player, to);

        assertThat(decision).isEqualTo(AreaEnforcementService.Decision.NONE);
        assertThat(AreaRunManager.isEngaged(uuid)).isFalse();
        verify(area, never()).contains(any());
    }

    @Test
    void evaluate_notEngaged_insideArea_completed_doesNotEngage() throws Exception {
        registerSingle(hunt(HUNT_ID, 1, 2, area, returnPoint));
        when(area.contains(to)).thenReturn(true);
        foundHeads(2);

        AreaEnforcementService.Decision decision = service.evaluate(player, to);

        assertThat(decision).isEqualTo(AreaEnforcementService.Decision.NONE);
        assertThat(AreaRunManager.isEngaged(uuid)).isFalse();
    }

    @Test
    void evaluate_notEngaged_insideArea_released_doesNotReEngage() {
        registerSingle(hunt(HUNT_ID, 1, 3, area, returnPoint));
        when(area.contains(to)).thenReturn(true);
        AreaRunManager.markReleased(uuid, HUNT_ID);

        AreaEnforcementService.Decision decision = service.evaluate(player, to);

        assertThat(decision).isEqualTo(AreaEnforcementService.Decision.NONE);
        assertThat(AreaRunManager.isEngaged(uuid)).isFalse();
        assertThat(AreaRunManager.isReleased(uuid, HUNT_ID)).isTrue();
    }

    @Test
    void evaluate_notEngaged_outsideAllAreas_clearsRelease() {
        registerSingle(hunt(HUNT_ID, 1, 3, area, returnPoint));
        when(area.contains(to)).thenReturn(false);
        AreaRunManager.markReleased(uuid, HUNT_ID);

        service.evaluate(player, to);

        assertThat(AreaRunManager.isReleased(uuid, HUNT_ID)).isFalse();
    }

    @Test
    void evaluate_overlap_engagesHighestPriority() throws Exception {
        AreaProvider areaA = mock(AreaProvider.class);
        AreaProvider areaB = mock(AreaProvider.class);
        for (AreaProvider z : List.of(areaA, areaB)) {
            when(z.getWorldName()).thenReturn("world");
            when(z.isAvailable()).thenReturn(true);
            when(z.contains(to)).thenReturn(true);
        }

        HBHunt low = hunt("low", 1, 3, areaA, returnPoint);
        HBHunt high = hunt("high", 5, 3, areaB, returnPoint);
        when(huntService.getAllHunts()).thenReturn(List.of(low, high));
        when(storageService.getHeadsPlayerForHunt(uuid, "high")).thenReturn(new ArrayList<>());
        lenient().when(languageService.message("Messages.AreaEntered")).thenReturn("Entered %hunt%");

        service.evaluate(player, to);

        assertThat(AreaRunManager.getEngaged(uuid)).isEqualTo("high");
    }

    @Test
    void evaluate_overlapSamePriority_prefersBlockingArea() throws Exception {
        AreaProvider areaA = mock(AreaProvider.class);
        AreaProvider areaB = mock(AreaProvider.class);
        for (AreaProvider z : List.of(areaA, areaB)) {
            when(z.getWorldName()).thenReturn("world");
            when(z.isAvailable()).thenReturn(true);
            when(z.contains(to)).thenReturn(true);
        }

        HBHunt free = hunt("free", 1, 3, areaA, returnPoint, false, false);
        HBHunt blocking = hunt("blocking", 1, 3, areaB, returnPoint, true, false);
        when(huntService.getAllHunts()).thenReturn(List.of(free, blocking));
        when(storageService.getHeadsPlayerForHunt(uuid, "blocking")).thenReturn(new ArrayList<>());
        when(languageService.message("Messages.AreaEntered")).thenReturn("Entered");

        service.evaluate(player, to);

        assertThat(AreaRunManager.getEngaged(uuid)).isEqualTo("blocking");
    }

    // --- evaluate: confinement while engaged ---

    @Test
    void evaluate_engaged_insideArea_returnsNone() {
        registerSingle(hunt(HUNT_ID, 1, 3, area, returnPoint));
        when(area.contains(to)).thenReturn(true);
        AreaRunManager.engage(uuid, HUNT_ID);

        AreaEnforcementService.Decision decision = service.evaluate(player, to);

        assertThat(decision).isEqualTo(AreaEnforcementService.Decision.NONE);
        assertThat(AreaRunManager.isEngaged(uuid)).isTrue();
    }

    @Test
    void evaluate_engaged_outsideArea_returnsConfine() {
        registerSingle(hunt(HUNT_ID, 1, 3, area, returnPoint));
        when(area.contains(to)).thenReturn(false);
        AreaRunManager.engage(uuid, HUNT_ID);

        AreaEnforcementService.Decision decision = service.evaluate(player, to);

        assertThat(decision).isEqualTo(AreaEnforcementService.Decision.CONFINE);
    }

    @Test
    void evaluate_engaged_areaUnavailable_disengagesFailOpen() {
        registerSingle(hunt(HUNT_ID, 1, 3, area, returnPoint));
        when(area.isAvailable()).thenReturn(false);
        AreaRunManager.engage(uuid, HUNT_ID);

        AreaEnforcementService.Decision decision = service.evaluate(player, to);

        assertThat(decision).isEqualTo(AreaEnforcementService.Decision.NONE);
        assertThat(AreaRunManager.isEngaged(uuid)).isFalse();
    }

    @Test
    void evaluate_engaged_returnPointNull_disengages() {
        registerSingle(hunt(HUNT_ID, 1, 3, area, null));
        AreaRunManager.engage(uuid, HUNT_ID);

        AreaEnforcementService.Decision decision = service.evaluate(player, to);

        assertThat(decision).isEqualTo(AreaEnforcementService.Decision.NONE);
        assertThat(AreaRunManager.isEngaged(uuid)).isFalse();
    }

    @Test
    void evaluate_engaged_outside_blockExitFalse_resetsAndDisengages() throws Exception {
        HBHunt hunt = hunt(HUNT_ID, 1, 3, area, returnPoint, false, true);
        registerSingle(hunt);
        when(area.contains(to)).thenReturn(false);
        AreaRunManager.engage(uuid, HUNT_ID);
        when(languageService.message(anyString())).thenReturn("");

        AreaEnforcementService.Decision decision = service.evaluate(player, to);

        assertThat(decision).isEqualTo(AreaEnforcementService.Decision.NONE);
        assertThat(AreaRunManager.isEngaged(uuid)).isFalse();
        verify(storageService).resetPlayerHunt(uuid, HUNT_ID);
    }

    @Test
    void evaluate_engaged_outside_withTimedRun_endsRunAndSchedulesTeleport() {
        Location plate = mock(Location.class);
        when(plate.getWorld()).thenReturn(world);

        HBHunt hunt = new HBHunt(configService, HUNT_ID, "Test Hunt", HuntState.ACTIVE, 1, "D");
        hunt.addHead(UUID.randomUUID());
        hunt.setBehaviors(List.of(new FreeBehavior(), new TimedBehavior(registry, plate, true, 60, false)));
        hunt.setRequirements(new RequirementSet(registry, RequirementMode.ALL, List.of(
                new AreaRequirement(registry, area, returnPoint, false, true, AreaMessageMode.CHAT))));
        registerSingle(hunt);

        when(area.contains(to)).thenReturn(false);
        AreaRunManager.engage(uuid, HUNT_ID);
        TimedRunManager.startRun(uuid, HUNT_ID, 90f);
        when(languageService.message(anyString())).thenReturn("");

        service.evaluate(player, to);

        assertThat(TimedRunManager.isInRun(uuid)).isFalse();
        verify(scheduler).runTaskLater(eq(player), any(Runnable.class), eq(1L));
    }

    @Test
    void evaluate_engaged_outside_noTimedRun_doesNotScheduleTeleport() {
        HBHunt hunt = hunt(HUNT_ID, 1, 3, area, returnPoint, false, true);
        registerSingle(hunt);
        when(area.contains(to)).thenReturn(false);
        AreaRunManager.engage(uuid, HUNT_ID);
        when(languageService.message(anyString())).thenReturn("");

        service.evaluate(player, to);

        verify(scheduler, never()).runTaskLater(any(Runnable.class), anyLong());
    }

    @Test
    void evaluate_engaged_outside_blockExitFalse_noReset_doesNotResetProgress() throws Exception {
        HBHunt hunt = hunt(HUNT_ID, 1, 3, area, returnPoint, false, false);
        registerSingle(hunt);
        when(area.contains(to)).thenReturn(false);
        AreaRunManager.engage(uuid, HUNT_ID);
        when(languageService.message(anyString())).thenReturn("");

        AreaEnforcementService.Decision decision = service.evaluate(player, to);

        assertThat(decision).isEqualTo(AreaEnforcementService.Decision.NONE);
        assertThat(AreaRunManager.isEngaged(uuid)).isFalse();
        verify(storageService, never()).resetPlayerHunt(any(), anyString());
    }

    @Test
    void evaluate_notEngaged_blockExitFalse_noReturnPoint_stillEngages() throws Exception {
        HBHunt hunt = hunt(HUNT_ID, 1, 3, area, null, false, true);
        registerSingle(hunt);
        when(area.contains(to)).thenReturn(true);
        foundHeads(1);
        when(languageService.message("Messages.AreaEntered")).thenReturn("Entered");

        service.evaluate(player, to);

        assertThat(AreaRunManager.getEngaged(uuid)).isEqualTo(HUNT_ID);
    }

    // --- getRecoveryPoint ---

    @Test
    void getRecoveryPoint_notEngaged_returnsNull() {
        Location ref = mock(Location.class);
        assertThat(service.getRecoveryPoint(player, ref)).isNull();
    }

    @Test
    void getRecoveryPoint_engaged_referenceInside_returnsNull() {
        Location ref = mock(Location.class);
        registerSingle(hunt(HUNT_ID, 1, 3, area, returnPoint));
        when(area.contains(ref)).thenReturn(true);
        AreaRunManager.engage(uuid, HUNT_ID);

        assertThat(service.getRecoveryPoint(player, ref)).isNull();
    }

    @Test
    void getRecoveryPoint_engaged_referenceOutside_returnsReturnPoint() {
        Location ref = mock(Location.class);
        registerSingle(hunt(HUNT_ID, 1, 3, area, returnPoint));
        when(area.contains(ref)).thenReturn(false);
        AreaRunManager.engage(uuid, HUNT_ID);

        assertThat(service.getRecoveryPoint(player, ref)).isEqualTo(returnPoint);
    }

    // --- leave ---

    @Test
    void leave_engaged_disengagesAndMarksReleased() {
        AreaRunManager.engage(uuid, HUNT_ID);

        boolean result = service.leave(player);

        assertThat(result).isTrue();
        assertThat(AreaRunManager.isEngaged(uuid)).isFalse();
        assertThat(AreaRunManager.isReleased(uuid, HUNT_ID)).isTrue();
    }

    @Test
    void leave_notEngaged_returnsFalse() {
        assertThat(service.leave(player)).isFalse();
    }

    @Test
    void leave_engaged_resetOnLeave_resetsProgress() throws Exception {
        registerSingle(hunt(HUNT_ID, 1, 3, area, returnPoint, true, true));
        AreaRunManager.engage(uuid, HUNT_ID);
        when(languageService.message(anyString())).thenReturn("");

        service.leave(player);

        verify(storageService).resetPlayerHunt(uuid, HUNT_ID);
    }

    @Test
    void leave_engaged_noReset_keepsProgress() throws Exception {
        registerSingle(hunt(HUNT_ID, 1, 3, area, returnPoint, true, false));
        AreaRunManager.engage(uuid, HUNT_ID);

        service.leave(player);

        verify(storageService, never()).resetPlayerHunt(any(), anyString());
    }

    // --- onHeadFound (event-driven completion) ---

    @Test
    void onHeadFound_completesHunt_disengagesAndReleases() {
        HBHunt hunt = hunt(HUNT_ID, 1, 3, area, returnPoint);
        AreaRunManager.engage(uuid, HUNT_ID);

        service.onHeadFound(player, hunt, 3);

        assertThat(AreaRunManager.isEngaged(uuid)).isFalse();
        assertThat(AreaRunManager.isReleased(uuid, HUNT_ID)).isTrue();
    }

    @Test
    void onHeadFound_notComplete_staysEngaged() {
        HBHunt hunt = hunt(HUNT_ID, 1, 3, area, returnPoint);
        AreaRunManager.engage(uuid, HUNT_ID);

        service.onHeadFound(player, hunt, 2);

        assertThat(AreaRunManager.isEngaged(uuid)).isTrue();
    }

    @Test
    void onHeadFound_differentHunt_noop() {
        HBHunt hunt = hunt(HUNT_ID, 1, 3, area, returnPoint);
        AreaRunManager.engage(uuid, "otherHunt");

        service.onHeadFound(player, hunt, 3);

        assertThat(AreaRunManager.getEngaged(uuid)).isEqualTo("otherHunt");
    }

    // --- isLocationOutsideArea ---

    @Test
    void isLocationOutsideArea_inside_returnsFalse() {
        HBHunt hunt = hunt(HUNT_ID, 1, 3, area, returnPoint);
        when(area.contains(to)).thenReturn(true);

        assertThat(service.isLocationOutsideArea(hunt, to)).isFalse();
    }

    @Test
    void isLocationOutsideArea_outside_returnsTrue() {
        HBHunt hunt = hunt(HUNT_ID, 1, 3, area, returnPoint);
        when(area.contains(to)).thenReturn(false);

        assertThat(service.isLocationOutsideArea(hunt, to)).isTrue();
    }

    @Test
    void isLocationOutsideArea_nullLocation_returnsFalse() {
        HBHunt hunt = hunt(HUNT_ID, 1, 3, area, returnPoint);
        assertThat(service.isLocationOutsideArea(hunt, null)).isFalse();
    }

    @Test
    void isLocationOutsideArea_areaUnavailable_returnsFalse() {
        HBHunt hunt = hunt(HUNT_ID, 1, 3, area, returnPoint);
        when(area.isAvailable()).thenReturn(false);

        assertThat(service.isLocationOutsideArea(hunt, to)).isFalse();
    }

    @Test
    void isLocationOutsideArea_noAreaBehavior_returnsFalse() {
        HBHunt hunt = new HBHunt(configService, HUNT_ID, "H", HuntState.ACTIVE, 1, "D");
        hunt.setBehaviors(List.of(new FreeBehavior()));

        assertThat(service.isLocationOutsideArea(hunt, to)).isFalse();
    }

    // --- sanitizeAreaHunts ---

    @Test
    void sanitize_returnPointNull_disablesArea() {
        HBHunt hunt = hunt(HUNT_ID, 1, 3, area, null);
        when(huntService.getAllHunts()).thenReturn(List.of(hunt));

        service.sanitizeAreaHunts();

        assertThat(service.hasArea(hunt)).isFalse();
    }

    @Test
    void sanitize_noHeads_disablesArea() {
        HBHunt hunt = hunt(HUNT_ID, 1, 0, area, returnPoint);
        when(huntService.getAllHunts()).thenReturn(List.of(hunt));
        when(headService.getHeadLocationsForHunt(hunt)).thenReturn(new ArrayList<>());

        service.sanitizeAreaHunts();

        assertThat(service.hasArea(hunt)).isFalse();
    }

    @Test
    void sanitize_headOutside_disablesArea() {
        HBHunt hunt = hunt(HUNT_ID, 1, 1, area, returnPoint);
        HeadLocation head = mock(HeadLocation.class);
        Location headLoc = mock(Location.class);
        when(head.getLocation()).thenReturn(headLoc);
        when(area.contains(headLoc)).thenReturn(false);
        when(huntService.getAllHunts()).thenReturn(List.of(hunt));
        when(headService.getHeadLocationsForHunt(hunt)).thenReturn(new ArrayList<>(List.of(head)));

        service.sanitizeAreaHunts();

        assertThat(service.hasArea(hunt)).isFalse();
    }

    @Test
    void sanitize_allHeadsInside_keepsArea() {
        HBHunt hunt = hunt(HUNT_ID, 1, 1, area, returnPoint);
        HeadLocation head = mock(HeadLocation.class);
        Location headLoc = mock(Location.class);
        when(head.getLocation()).thenReturn(headLoc);
        when(area.contains(headLoc)).thenReturn(true);
        when(huntService.getAllHunts()).thenReturn(List.of(hunt));
        when(headService.getHeadLocationsForHunt(hunt)).thenReturn(new ArrayList<>(List.of(head)));

        service.sanitizeAreaHunts();

        assertThat(service.hasArea(hunt)).isTrue();
    }

    @Test
    void sanitize_disabling_keepsTheRequirementOnTheHunt() {
        HBHunt hunt = hunt(HUNT_ID, 1, 0, area, returnPoint);
        when(huntService.getAllHunts()).thenReturn(List.of(hunt));
        when(headService.getHeadLocationsForHunt(hunt)).thenReturn(new ArrayList<>());

        service.sanitizeAreaHunts();

        assertThat(hunt.getRequirements().find(AreaRequirement.class))
                .map(AreaRequirement::isDisabled)
                .contains(true);
    }

    @Test
    void sanitize_becomingValidAgain_reEnablesTheArea() {
        HBHunt hunt = hunt(HUNT_ID, 1, 1, area, returnPoint);
        HeadLocation head = mock(HeadLocation.class);
        Location headLoc = mock(Location.class);
        when(head.getLocation()).thenReturn(headLoc);
        when(area.contains(headLoc)).thenReturn(true);
        when(huntService.getAllHunts()).thenReturn(List.of(hunt));
        when(headService.getHeadLocationsForHunt(hunt))
                .thenReturn(new ArrayList<>())
                .thenReturn(new ArrayList<>(List.of(head)));

        service.sanitizeAreaHunts();
        assertThat(service.hasArea(hunt)).isFalse();

        service.sanitizeAreaHunts();
        assertThat(service.hasArea(hunt)).isTrue();
    }

    @Test
    void sanitize_areaUnavailable_keepsAreaSkipsHeadCheck() {
        HBHunt hunt = hunt(HUNT_ID, 1, 1, area, returnPoint);
        when(area.isAvailable()).thenReturn(false);
        when(huntService.getAllHunts()).thenReturn(List.of(hunt));

        service.sanitizeAreaHunts();

        assertThat(service.hasArea(hunt)).isTrue();
    }

    // --- Full cycles ---

    @Nested
    class FullCycle {
        private Location outside;

        @BeforeEach
        void outsideLocation() {
            outside = mock(Location.class);
            lenient().when(outside.getWorld()).thenReturn(world);
            lenient().when(area.contains(outside)).thenReturn(false);
            lenient().when(area.contains(to)).thenReturn(true);
            lenient().when(languageService.message(anyString())).thenReturn("");
        }

        @Test
        void enter_stay_thenConfinedOnTheWayOut() throws Exception {
            registerSingle(hunt(HUNT_ID, 1, 3, area, returnPoint, true, true));
            foundHeads(1);

            assertThat(service.evaluate(player, to)).isEqualTo(AreaEnforcementService.Decision.NONE);
            assertThat(AreaRunManager.getEngaged(uuid)).isEqualTo(HUNT_ID);

            assertThat(service.evaluate(player, to)).isEqualTo(AreaEnforcementService.Decision.NONE);

            assertThat(service.evaluate(player, outside)).isEqualTo(AreaEnforcementService.Decision.CONFINE);
            assertThat(AreaRunManager.getEngaged(uuid)).isEqualTo(HUNT_ID);
            verify(storageService, never()).resetPlayerHunt(any(), anyString());
        }

        @Test
        void confined_thenRecoveryPointSendsThePlayerBack() throws Exception {
            registerSingle(hunt(HUNT_ID, 1, 3, area, returnPoint, true, true));
            foundHeads(1);
            service.evaluate(player, to);

            assertThat(service.getRecoveryPoint(player, outside)).isEqualTo(returnPoint);
            assertThat(service.getRecoveryPoint(player, to)).isNull();
        }

        @Test
        void enter_leaveCommand_resetsThenReleasesUntilTheAreaIsLeft() throws Exception {
            registerSingle(hunt(HUNT_ID, 1, 3, area, returnPoint, true, true));
            foundHeads(1);
            service.evaluate(player, to);

            assertThat(service.leave(player)).isTrue();
            verify(storageService).resetPlayerHunt(uuid, HUNT_ID);
            assertThat(AreaRunManager.isEngaged(uuid)).isFalse();

            service.evaluate(player, to);
            assertThat(AreaRunManager.isEngaged(uuid)).isFalse();

            service.evaluate(player, outside);
            service.evaluate(player, to);
            assertThat(AreaRunManager.getEngaged(uuid)).isEqualTo(HUNT_ID);
        }

        @Test
        void enter_walkOutWithoutBlocking_resetsAndDisengages() throws Exception {
            registerSingle(hunt(HUNT_ID, 1, 3, area, returnPoint, false, true));
            foundHeads(1);
            service.evaluate(player, to);
            assertThat(AreaRunManager.getEngaged(uuid)).isEqualTo(HUNT_ID);

            assertThat(service.evaluate(player, outside)).isEqualTo(AreaEnforcementService.Decision.NONE);

            assertThat(AreaRunManager.isEngaged(uuid)).isFalse();
            verify(storageService).resetPlayerHunt(uuid, HUNT_ID);
        }

        @Test
        void enter_completeTheHunt_releasesTheConfinement() throws Exception {
            HBHunt hunt = hunt(HUNT_ID, 1, 2, area, returnPoint, true, true);
            registerSingle(hunt);
            foundHeads(1);
            service.evaluate(player, to);

            service.onHeadFound(player, hunt, 2);

            assertThat(AreaRunManager.isEngaged(uuid)).isFalse();
            assertThat(service.evaluate(player, outside)).isEqualTo(AreaEnforcementService.Decision.NONE);
        }

        @Test
        void enter_areaBecomesUnresolvable_freesThePlayer() throws Exception {
            registerSingle(hunt(HUNT_ID, 1, 3, area, returnPoint, true, true));
            foundHeads(1);
            service.evaluate(player, to);
            assertThat(AreaRunManager.getEngaged(uuid)).isEqualTo(HUNT_ID);

            when(area.isAvailable()).thenReturn(false);

            assertThat(service.evaluate(player, outside)).isEqualTo(AreaEnforcementService.Decision.NONE);
            assertThat(AreaRunManager.isEngaged(uuid)).isFalse();
            verify(storageService, never()).resetPlayerHunt(any(), anyString());
        }
    }
}
