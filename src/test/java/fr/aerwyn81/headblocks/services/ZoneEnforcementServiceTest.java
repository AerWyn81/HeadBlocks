package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.data.hunt.behavior.FreeBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.ZoneBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.zone.ZoneMessageMode;
import fr.aerwyn81.headblocks.data.hunt.behavior.zone.ZoneProvider;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
class ZoneEnforcementServiceTest {

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
    ZoneProvider zone;

    @Mock
    Player player;

    @Mock
    Location to;

    @Mock
    World world;

    @Mock
    Location returnPoint;

    private ZoneEnforcementService service;
    private UUID uuid;
    private static final String HUNT_ID = "hunt1";

    @BeforeEach
    void setUp() {
        ZoneRunManager.clearAll();
        uuid = UUID.randomUUID();
        lenient().when(player.getUniqueId()).thenReturn(uuid);
        lenient().when(registry.getHuntService()).thenReturn(huntService);
        lenient().when(registry.getStorageService()).thenReturn(storageService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(registry.getHeadService()).thenReturn(headService);

        lenient().when(world.getName()).thenReturn("world");
        lenient().when(to.getWorld()).thenReturn(world);
        lenient().when(zone.getWorldName()).thenReturn("world");
        lenient().when(zone.isAvailable()).thenReturn(true);

        service = new ZoneEnforcementService(registry);
    }

    @AfterEach
    void tearDown() {
        ZoneRunManager.clearAll();
    }

    private HBHunt hunt(String id, int priority, int headCount, ZoneProvider zoneProvider, Location rp) {
        return hunt(id, priority, headCount, zoneProvider, rp, true, true);
    }

    private HBHunt hunt(String id, int priority, int headCount, ZoneProvider zoneProvider, Location rp,
                        boolean blockExit, boolean resetOnLeave) {
        HBHunt hunt = new HBHunt(configService, id, "Test Hunt", HuntState.ACTIVE, priority, "D");
        for (int i = 0; i < headCount; i++) {
            hunt.addHead(UUID.randomUUID());
        }
        hunt.setBehaviors(List.of(new FreeBehavior(),
                new ZoneBehavior(registry, zoneProvider, rp, blockExit, resetOnLeave, ZoneMessageMode.CHAT)));
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
    void evaluate_notEngaged_insideZone_notCompleted_engagesAndMessages() throws Exception {
        registerSingle(hunt(HUNT_ID, 1, 3, zone, returnPoint));
        when(zone.contains(to)).thenReturn(true);
        foundHeads(1);
        when(languageService.message("Messages.ZoneEntered")).thenReturn("Entered %hunt%");

        ZoneEnforcementService.Decision decision = service.evaluate(player, to);

        assertThat(decision).isEqualTo(ZoneEnforcementService.Decision.NONE);
        assertThat(ZoneRunManager.getEngaged(uuid)).isEqualTo(HUNT_ID);
        verify(player).sendMessage("Entered Test Hunt");
    }

    @Test
    void evaluate_notEngaged_returnPointNull_doesNotEngage() {
        registerSingle(hunt(HUNT_ID, 1, 3, zone, null));
        lenient().when(zone.contains(to)).thenReturn(true);

        ZoneEnforcementService.Decision decision = service.evaluate(player, to);

        assertThat(decision).isEqualTo(ZoneEnforcementService.Decision.NONE);
        assertThat(ZoneRunManager.isEngaged(uuid)).isFalse();
    }

    @Test
    void evaluate_notEngaged_worldMismatch_doesNotEngage() {
        registerSingle(hunt(HUNT_ID, 1, 3, zone, returnPoint));
        when(zone.getWorldName()).thenReturn("other_world");

        ZoneEnforcementService.Decision decision = service.evaluate(player, to);

        assertThat(decision).isEqualTo(ZoneEnforcementService.Decision.NONE);
        assertThat(ZoneRunManager.isEngaged(uuid)).isFalse();
        verify(zone, never()).contains(any());
    }

    @Test
    void evaluate_notEngaged_insideZone_completed_doesNotEngage() throws Exception {
        registerSingle(hunt(HUNT_ID, 1, 2, zone, returnPoint));
        when(zone.contains(to)).thenReturn(true);
        foundHeads(2);

        ZoneEnforcementService.Decision decision = service.evaluate(player, to);

        assertThat(decision).isEqualTo(ZoneEnforcementService.Decision.NONE);
        assertThat(ZoneRunManager.isEngaged(uuid)).isFalse();
    }

    @Test
    void evaluate_notEngaged_insideZone_released_doesNotReEngage() {
        registerSingle(hunt(HUNT_ID, 1, 3, zone, returnPoint));
        when(zone.contains(to)).thenReturn(true);
        ZoneRunManager.markReleased(uuid, HUNT_ID);

        ZoneEnforcementService.Decision decision = service.evaluate(player, to);

        assertThat(decision).isEqualTo(ZoneEnforcementService.Decision.NONE);
        assertThat(ZoneRunManager.isEngaged(uuid)).isFalse();
        assertThat(ZoneRunManager.isReleased(uuid, HUNT_ID)).isTrue();
    }

    @Test
    void evaluate_notEngaged_outsideAllZones_clearsRelease() {
        registerSingle(hunt(HUNT_ID, 1, 3, zone, returnPoint));
        when(zone.contains(to)).thenReturn(false);
        ZoneRunManager.markReleased(uuid, HUNT_ID);

        service.evaluate(player, to);

        assertThat(ZoneRunManager.isReleased(uuid, HUNT_ID)).isFalse();
    }

    @Test
    void evaluate_overlap_engagesHighestPriority() throws Exception {
        ZoneProvider zoneA = mock(ZoneProvider.class);
        ZoneProvider zoneB = mock(ZoneProvider.class);
        for (ZoneProvider z : List.of(zoneA, zoneB)) {
            when(z.getWorldName()).thenReturn("world");
            when(z.isAvailable()).thenReturn(true);
            when(z.contains(to)).thenReturn(true);
        }

        HBHunt low = hunt("low", 1, 3, zoneA, returnPoint);
        HBHunt high = hunt("high", 5, 3, zoneB, returnPoint);
        when(huntService.getAllHunts()).thenReturn(List.of(low, high));
        when(storageService.getHeadsPlayerForHunt(uuid, "high")).thenReturn(new ArrayList<>());
        lenient().when(languageService.message("Messages.ZoneEntered")).thenReturn("Entered %hunt%");

        service.evaluate(player, to);

        assertThat(ZoneRunManager.getEngaged(uuid)).isEqualTo("high");
    }

    @Test
    void evaluate_overlapSamePriority_prefersBlockingZone() throws Exception {
        ZoneProvider zoneA = mock(ZoneProvider.class);
        ZoneProvider zoneB = mock(ZoneProvider.class);
        for (ZoneProvider z : List.of(zoneA, zoneB)) {
            when(z.getWorldName()).thenReturn("world");
            when(z.isAvailable()).thenReturn(true);
            when(z.contains(to)).thenReturn(true);
        }

        HBHunt free = hunt("free", 1, 3, zoneA, returnPoint, false, false);
        HBHunt blocking = hunt("blocking", 1, 3, zoneB, returnPoint, true, false);
        when(huntService.getAllHunts()).thenReturn(List.of(free, blocking));
        when(storageService.getHeadsPlayerForHunt(uuid, "blocking")).thenReturn(new ArrayList<>());
        when(languageService.message("Messages.ZoneEntered")).thenReturn("Entered");

        service.evaluate(player, to);

        assertThat(ZoneRunManager.getEngaged(uuid)).isEqualTo("blocking");
    }

    // --- evaluate: confinement while engaged ---

    @Test
    void evaluate_engaged_insideZone_returnsNone() {
        registerSingle(hunt(HUNT_ID, 1, 3, zone, returnPoint));
        when(zone.contains(to)).thenReturn(true);
        ZoneRunManager.engage(uuid, HUNT_ID);

        ZoneEnforcementService.Decision decision = service.evaluate(player, to);

        assertThat(decision).isEqualTo(ZoneEnforcementService.Decision.NONE);
        assertThat(ZoneRunManager.isEngaged(uuid)).isTrue();
    }

    @Test
    void evaluate_engaged_outsideZone_returnsConfine() {
        registerSingle(hunt(HUNT_ID, 1, 3, zone, returnPoint));
        when(zone.contains(to)).thenReturn(false);
        ZoneRunManager.engage(uuid, HUNT_ID);

        ZoneEnforcementService.Decision decision = service.evaluate(player, to);

        assertThat(decision).isEqualTo(ZoneEnforcementService.Decision.CONFINE);
    }

    @Test
    void evaluate_engaged_zoneUnavailable_disengagesFailOpen() {
        registerSingle(hunt(HUNT_ID, 1, 3, zone, returnPoint));
        when(zone.isAvailable()).thenReturn(false);
        ZoneRunManager.engage(uuid, HUNT_ID);

        ZoneEnforcementService.Decision decision = service.evaluate(player, to);

        assertThat(decision).isEqualTo(ZoneEnforcementService.Decision.NONE);
        assertThat(ZoneRunManager.isEngaged(uuid)).isFalse();
    }

    @Test
    void evaluate_engaged_returnPointNull_disengages() {
        registerSingle(hunt(HUNT_ID, 1, 3, zone, null));
        ZoneRunManager.engage(uuid, HUNT_ID);

        ZoneEnforcementService.Decision decision = service.evaluate(player, to);

        assertThat(decision).isEqualTo(ZoneEnforcementService.Decision.NONE);
        assertThat(ZoneRunManager.isEngaged(uuid)).isFalse();
    }

    @Test
    void evaluate_engaged_outside_blockExitFalse_resetsAndDisengages() throws Exception {
        HBHunt hunt = hunt(HUNT_ID, 1, 3, zone, returnPoint, false, true);
        registerSingle(hunt);
        when(zone.contains(to)).thenReturn(false);
        ZoneRunManager.engage(uuid, HUNT_ID);
        when(languageService.message(anyString())).thenReturn("");

        ZoneEnforcementService.Decision decision = service.evaluate(player, to);

        assertThat(decision).isEqualTo(ZoneEnforcementService.Decision.NONE);
        assertThat(ZoneRunManager.isEngaged(uuid)).isFalse();
        verify(storageService).resetPlayerHunt(uuid, HUNT_ID);
    }

    @Test
    void evaluate_engaged_outside_blockExitFalse_noReset_doesNotResetProgress() throws Exception {
        HBHunt hunt = hunt(HUNT_ID, 1, 3, zone, returnPoint, false, false);
        registerSingle(hunt);
        when(zone.contains(to)).thenReturn(false);
        ZoneRunManager.engage(uuid, HUNT_ID);
        when(languageService.message(anyString())).thenReturn("");

        ZoneEnforcementService.Decision decision = service.evaluate(player, to);

        assertThat(decision).isEqualTo(ZoneEnforcementService.Decision.NONE);
        assertThat(ZoneRunManager.isEngaged(uuid)).isFalse();
        verify(storageService, never()).resetPlayerHunt(any(), anyString());
    }

    @Test
    void evaluate_notEngaged_blockExitFalse_noReturnPoint_stillEngages() throws Exception {
        HBHunt hunt = hunt(HUNT_ID, 1, 3, zone, null, false, true);
        registerSingle(hunt);
        when(zone.contains(to)).thenReturn(true);
        foundHeads(1);
        when(languageService.message("Messages.ZoneEntered")).thenReturn("Entered");

        service.evaluate(player, to);

        assertThat(ZoneRunManager.getEngaged(uuid)).isEqualTo(HUNT_ID);
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
        registerSingle(hunt(HUNT_ID, 1, 3, zone, returnPoint));
        when(zone.contains(ref)).thenReturn(true);
        ZoneRunManager.engage(uuid, HUNT_ID);

        assertThat(service.getRecoveryPoint(player, ref)).isNull();
    }

    @Test
    void getRecoveryPoint_engaged_referenceOutside_returnsReturnPoint() {
        Location ref = mock(Location.class);
        registerSingle(hunt(HUNT_ID, 1, 3, zone, returnPoint));
        when(zone.contains(ref)).thenReturn(false);
        ZoneRunManager.engage(uuid, HUNT_ID);

        assertThat(service.getRecoveryPoint(player, ref)).isEqualTo(returnPoint);
    }

    // --- leave ---

    @Test
    void leave_engaged_disengagesAndMarksReleased() {
        ZoneRunManager.engage(uuid, HUNT_ID);

        boolean result = service.leave(player);

        assertThat(result).isTrue();
        assertThat(ZoneRunManager.isEngaged(uuid)).isFalse();
        assertThat(ZoneRunManager.isReleased(uuid, HUNT_ID)).isTrue();
    }

    @Test
    void leave_notEngaged_returnsFalse() {
        assertThat(service.leave(player)).isFalse();
    }

    @Test
    void leave_engaged_resetOnLeave_resetsProgress() throws Exception {
        registerSingle(hunt(HUNT_ID, 1, 3, zone, returnPoint, true, true));
        ZoneRunManager.engage(uuid, HUNT_ID);
        when(languageService.message(anyString())).thenReturn("");

        service.leave(player);

        verify(storageService).resetPlayerHunt(uuid, HUNT_ID);
    }

    @Test
    void leave_engaged_noReset_keepsProgress() throws Exception {
        registerSingle(hunt(HUNT_ID, 1, 3, zone, returnPoint, true, false));
        ZoneRunManager.engage(uuid, HUNT_ID);

        service.leave(player);

        verify(storageService, never()).resetPlayerHunt(any(), anyString());
    }

    // --- onHeadFound (event-driven completion) ---

    @Test
    void onHeadFound_completesHunt_disengagesAndReleases() {
        HBHunt hunt = hunt(HUNT_ID, 1, 3, zone, returnPoint);
        ZoneRunManager.engage(uuid, HUNT_ID);

        service.onHeadFound(player, hunt, 3);

        assertThat(ZoneRunManager.isEngaged(uuid)).isFalse();
        assertThat(ZoneRunManager.isReleased(uuid, HUNT_ID)).isTrue();
    }

    @Test
    void onHeadFound_notComplete_staysEngaged() {
        HBHunt hunt = hunt(HUNT_ID, 1, 3, zone, returnPoint);
        ZoneRunManager.engage(uuid, HUNT_ID);

        service.onHeadFound(player, hunt, 2);

        assertThat(ZoneRunManager.isEngaged(uuid)).isTrue();
    }

    @Test
    void onHeadFound_differentHunt_noop() {
        HBHunt hunt = hunt(HUNT_ID, 1, 3, zone, returnPoint);
        ZoneRunManager.engage(uuid, "otherHunt");

        service.onHeadFound(player, hunt, 3);

        assertThat(ZoneRunManager.getEngaged(uuid)).isEqualTo("otherHunt");
    }

    // --- isLocationOutsideZone ---

    @Test
    void isLocationOutsideZone_inside_returnsFalse() {
        HBHunt hunt = hunt(HUNT_ID, 1, 3, zone, returnPoint);
        when(zone.contains(to)).thenReturn(true);

        assertThat(service.isLocationOutsideZone(hunt, to)).isFalse();
    }

    @Test
    void isLocationOutsideZone_outside_returnsTrue() {
        HBHunt hunt = hunt(HUNT_ID, 1, 3, zone, returnPoint);
        when(zone.contains(to)).thenReturn(false);

        assertThat(service.isLocationOutsideZone(hunt, to)).isTrue();
    }

    @Test
    void isLocationOutsideZone_nullLocation_returnsFalse() {
        HBHunt hunt = hunt(HUNT_ID, 1, 3, zone, returnPoint);
        assertThat(service.isLocationOutsideZone(hunt, null)).isFalse();
    }

    @Test
    void isLocationOutsideZone_zoneUnavailable_returnsFalse() {
        HBHunt hunt = hunt(HUNT_ID, 1, 3, zone, returnPoint);
        when(zone.isAvailable()).thenReturn(false);

        assertThat(service.isLocationOutsideZone(hunt, to)).isFalse();
    }

    @Test
    void isLocationOutsideZone_noZoneBehavior_returnsFalse() {
        HBHunt hunt = new HBHunt(configService, HUNT_ID, "H", HuntState.ACTIVE, 1, "D");
        hunt.setBehaviors(List.of(new FreeBehavior()));

        assertThat(service.isLocationOutsideZone(hunt, to)).isFalse();
    }

    // --- sanitizeZoneHunts ---

    @Test
    void sanitize_returnPointNull_disablesZone() {
        HBHunt hunt = hunt(HUNT_ID, 1, 3, zone, null);
        when(huntService.getAllHunts()).thenReturn(List.of(hunt));

        service.sanitizeZoneHunts();

        assertThat(service.hasZoneBehavior(hunt)).isFalse();
    }

    @Test
    void sanitize_noHeads_disablesZone() {
        HBHunt hunt = hunt(HUNT_ID, 1, 0, zone, returnPoint);
        when(huntService.getAllHunts()).thenReturn(List.of(hunt));
        when(headService.getHeadLocationsForHunt(hunt)).thenReturn(new ArrayList<>());

        service.sanitizeZoneHunts();

        assertThat(service.hasZoneBehavior(hunt)).isFalse();
    }

    @Test
    void sanitize_headOutside_disablesZone() {
        HBHunt hunt = hunt(HUNT_ID, 1, 1, zone, returnPoint);
        HeadLocation head = mock(HeadLocation.class);
        Location headLoc = mock(Location.class);
        when(head.getLocation()).thenReturn(headLoc);
        when(zone.contains(headLoc)).thenReturn(false);
        when(huntService.getAllHunts()).thenReturn(List.of(hunt));
        when(headService.getHeadLocationsForHunt(hunt)).thenReturn(new ArrayList<>(List.of(head)));

        service.sanitizeZoneHunts();

        assertThat(service.hasZoneBehavior(hunt)).isFalse();
    }

    @Test
    void sanitize_allHeadsInside_keepsZone() {
        HBHunt hunt = hunt(HUNT_ID, 1, 1, zone, returnPoint);
        HeadLocation head = mock(HeadLocation.class);
        Location headLoc = mock(Location.class);
        when(head.getLocation()).thenReturn(headLoc);
        when(zone.contains(headLoc)).thenReturn(true);
        when(huntService.getAllHunts()).thenReturn(List.of(hunt));
        when(headService.getHeadLocationsForHunt(hunt)).thenReturn(new ArrayList<>(List.of(head)));

        service.sanitizeZoneHunts();

        assertThat(service.hasZoneBehavior(hunt)).isTrue();
    }

    @Test
    void sanitize_zoneUnavailable_keepsZoneSkipsHeadCheck() {
        HBHunt hunt = hunt(HUNT_ID, 1, 1, zone, returnPoint);
        when(zone.isAvailable()).thenReturn(false);
        when(huntService.getAllHunts()).thenReturn(List.of(hunt));

        service.sanitizeZoneHunts();

        assertThat(service.hasZoneBehavior(hunt)).isTrue();
    }
}
