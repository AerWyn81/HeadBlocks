package fr.aerwyn81.headblocks.data.hunt.behavior;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.services.ConfigService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.services.TimedRunManager;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimedBehaviorTest {

    @Mock
    Player player;

    @Mock
    HeadLocation headLocation;

    @Mock
    ServiceRegistry registry;

    @Mock
    StorageService storageService;

    @Mock
    LanguageService languageService;

    @Mock
    ConfigService configService;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getStorageService()).thenReturn(storageService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
    }

    @Test
    void canPlayerClick_playerNotInRun_returnsDeny() {
        TimedBehavior behavior = new TimedBehavior(registry, null, true);
        Hunt hunt = new Hunt(configService, "hunt1", "Test", HuntState.ACTIVE, 1, "D");
        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);

        try (MockedStatic<TimedRunManager> trm = mockStatic(TimedRunManager.class)) {
            trm.when(() -> TimedRunManager.isInRun(playerUuid, "hunt1")).thenReturn(false);
            when(languageService.message("Messages.TimedNotStarted")).thenReturn("Not started");

            BehaviorResult result = behavior.canPlayerClick(player, headLocation, hunt);

            assertThat(result.allowed()).isFalse();
            assertThat(result.denyMessage()).isEqualTo("Not started");
        }
    }

    @Test
    void canPlayerClick_playerInRunForThisHunt_returnsAllow() {
        TimedBehavior behavior = new TimedBehavior(registry, null, true);
        Hunt hunt = new Hunt(configService, "hunt1", "Test", HuntState.ACTIVE, 1, "D");
        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);

        try (MockedStatic<TimedRunManager> trm = mockStatic(TimedRunManager.class)) {
            trm.when(() -> TimedRunManager.isInRun(playerUuid, "hunt1")).thenReturn(true);

            BehaviorResult result = behavior.canPlayerClick(player, headLocation, hunt);

            assertThat(result.allowed()).isTrue();
        }
    }

    @Test
    void onHeadFound_playerNotInRun_earlyReturn() {
        TimedBehavior behavior = new TimedBehavior(registry, null, true);
        Hunt hunt = new Hunt(configService, "hunt1", "Test", HuntState.ACTIVE, 1, "D");
        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);

        try (MockedStatic<TimedRunManager> trm = mockStatic(TimedRunManager.class)) {
            trm.when(() -> TimedRunManager.isInRun(playerUuid, "hunt1")).thenReturn(false);

            behavior.onHeadFound(player, headLocation, hunt);

            // StorageService should never be called
            verifyNoInteractions(storageService);
        }
    }

    @Test
    void onHeadFound_notAllHeadsFound_noCompletion() throws InternalException {
        TimedBehavior behavior = new TimedBehavior(registry, null, true);
        Hunt hunt = new Hunt(configService, "hunt1", "Test", HuntState.ACTIVE, 1, "D");
        hunt.addHead(UUID.randomUUID());
        hunt.addHead(UUID.randomUUID());
        hunt.addHead(UUID.randomUUID()); // 3 total

        UUID playerUuid = UUID.randomUUID();
        UUID headUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(headLocation.getUuid()).thenReturn(headUuid);

        try (MockedStatic<TimedRunManager> trm = mockStatic(TimedRunManager.class)) {
            trm.when(() -> TimedRunManager.isInRun(playerUuid, "hunt1")).thenReturn(true);

            // Player has found 1 head, clicking a new one makes 2, but total is 3
            ArrayList<UUID> found = new ArrayList<>();
            found.add(UUID.randomUUID());
            when(storageService.getHeadsPlayerForHunt(playerUuid, "hunt1")).thenReturn(found);

            behavior.onHeadFound(player, headLocation, hunt);

            // leaveRun should NOT be called
            trm.verify(() -> TimedRunManager.leaveRun(any()), never());
        }
    }

    @Test
    void onHeadFound_allHeadsFound_completesRun() throws InternalException {
        TimedBehavior behavior = new TimedBehavior(registry, null, false); // not repeatable
        Hunt hunt = new Hunt(configService, "hunt1", "Test Hunt", HuntState.ACTIVE, 1, "D");
        UUID h1 = UUID.randomUUID();
        hunt.addHead(h1); // 1 total head

        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        lenient().when(player.getName()).thenReturn("Steve");
        when(headLocation.getUuid()).thenReturn(h1);

        try (MockedStatic<TimedRunManager> trm = mockStatic(TimedRunManager.class)) {
            trm.when(() -> TimedRunManager.isInRun(playerUuid, "hunt1")).thenReturn(true);
            trm.when(() -> TimedRunManager.getElapsedMillis(playerUuid)).thenReturn(5000L);
            trm.when(() -> TimedRunManager.formatTime(5000L)).thenReturn("00:05.000");

            // Player found 0 so far, clicking h1 makes it 1 = total
            ArrayList<UUID> found = new ArrayList<>();
            when(storageService.getHeadsPlayerForHunt(playerUuid, "hunt1")).thenReturn(found);
            when(storageService.getTimedRunCount(playerUuid, "hunt1")).thenReturn(1);

            when(languageService.message("Messages.TimedCompleted")).thenReturn("Completed in %time% (%hunt%) x%count%");

            behavior.onHeadFound(player, headLocation, hunt);

            trm.verify(() -> TimedRunManager.leaveRun(playerUuid));
            verify(storageService).saveTimedRun(playerUuid, "hunt1", 5000L);
            verify(player).sendMessage(contains("00:05.000"));
        }
    }

    @Test
    void onHeadFound_completed_repeatable_resetsPlayerHunt() throws InternalException {
        TimedBehavior behavior = new TimedBehavior(registry, null, true); // repeatable
        Hunt hunt = new Hunt(configService, "hunt1", "Test Hunt", HuntState.ACTIVE, 1, "D");
        UUID h1 = UUID.randomUUID();
        hunt.addHead(h1);

        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        lenient().when(player.getName()).thenReturn("Steve");
        when(headLocation.getUuid()).thenReturn(h1);

        try (MockedStatic<TimedRunManager> trm = mockStatic(TimedRunManager.class)) {
            trm.when(() -> TimedRunManager.isInRun(playerUuid, "hunt1")).thenReturn(true);
            trm.when(() -> TimedRunManager.getElapsedMillis(playerUuid)).thenReturn(3000L);
            trm.when(() -> TimedRunManager.formatTime(3000L)).thenReturn("00:03.000");

            ArrayList<UUID> found = new ArrayList<>();
            when(storageService.getHeadsPlayerForHunt(playerUuid, "hunt1")).thenReturn(found);
            when(storageService.getTimedRunCount(playerUuid, "hunt1")).thenReturn(2);

            when(languageService.message("Messages.TimedCompleted")).thenReturn("Done %time% %hunt% %count%");

            behavior.onHeadFound(player, headLocation, hunt);

            verify(storageService).resetPlayerHunt(playerUuid, "hunt1");
        }
    }

    @Test
    void onHeadFound_completed_notRepeatable_doesNotReset() throws InternalException {
        TimedBehavior behavior = new TimedBehavior(registry, null, false); // not repeatable
        Hunt hunt = new Hunt(configService, "hunt1", "Test Hunt", HuntState.ACTIVE, 1, "D");
        UUID h1 = UUID.randomUUID();
        hunt.addHead(h1);

        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        lenient().when(player.getName()).thenReturn("Steve");
        when(headLocation.getUuid()).thenReturn(h1);

        try (MockedStatic<TimedRunManager> trm = mockStatic(TimedRunManager.class)) {
            trm.when(() -> TimedRunManager.isInRun(playerUuid, "hunt1")).thenReturn(true);
            trm.when(() -> TimedRunManager.getElapsedMillis(playerUuid)).thenReturn(1000L);
            trm.when(() -> TimedRunManager.formatTime(1000L)).thenReturn("00:01.000");

            ArrayList<UUID> found = new ArrayList<>();
            when(storageService.getHeadsPlayerForHunt(playerUuid, "hunt1")).thenReturn(found);
            when(storageService.getTimedRunCount(playerUuid, "hunt1")).thenReturn(1);

            when(languageService.message("Messages.TimedCompleted")).thenReturn("Done %time% %hunt% %count%");

            behavior.onHeadFound(player, headLocation, hunt);

            verify(storageService, never()).resetPlayerHunt(any(), anyString());
        }
    }

    @Test
    void onHeadFound_headAlreadyInPlayerList_doesNotDoubleCount() throws InternalException {
        TimedBehavior behavior = new TimedBehavior(registry, null, false);
        Hunt hunt = new Hunt(configService, "hunt1", "Test", HuntState.ACTIVE, 1, "D");
        UUID h1 = UUID.randomUUID();
        UUID h2 = UUID.randomUUID();
        hunt.addHead(h1);
        hunt.addHead(h2); // 2 total heads

        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(headLocation.getUuid()).thenReturn(h1);

        try (MockedStatic<TimedRunManager> trm = mockStatic(TimedRunManager.class)) {
            trm.when(() -> TimedRunManager.isInRun(playerUuid, "hunt1")).thenReturn(true);

            // Player already found h1 — clicking it again should count as 1, not 2
            ArrayList<UUID> found = new ArrayList<>();
            found.add(h1);
            when(storageService.getHeadsPlayerForHunt(playerUuid, "hunt1")).thenReturn(found);

            behavior.onHeadFound(player, headLocation, hunt);

            // foundCount = 1 (already in list, no +1), totalHeads = 2 → no completion
            trm.verify(() -> TimedRunManager.leaveRun(any()), never());
        }
    }

    @Test
    void onHeadFound_saveTimedRunThrows_runStillLeft() throws InternalException {
        TimedBehavior behavior = new TimedBehavior(registry, null, false);
        Hunt hunt = new Hunt(configService, "hunt1", "Test", HuntState.ACTIVE, 1, "D");
        UUID h1 = UUID.randomUUID();
        hunt.addHead(h1);

        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        lenient().when(player.getName()).thenReturn("Steve");
        when(headLocation.getUuid()).thenReturn(h1);

        try (MockedStatic<TimedRunManager> trm = mockStatic(TimedRunManager.class)) {
            trm.when(() -> TimedRunManager.isInRun(playerUuid, "hunt1")).thenReturn(true);
            trm.when(() -> TimedRunManager.getElapsedMillis(playerUuid)).thenReturn(2000L);
            trm.when(() -> TimedRunManager.formatTime(2000L)).thenReturn("00:02.000");

            ArrayList<UUID> found = new ArrayList<>();
            when(storageService.getHeadsPlayerForHunt(playerUuid, "hunt1")).thenReturn(found);
            doThrow(new InternalException("DB write error"))
                    .when(storageService).saveTimedRun(playerUuid, "hunt1", 2000L);
            when(storageService.getTimedRunCount(playerUuid, "hunt1")).thenReturn(0);

            when(languageService.message("Messages.TimedCompleted")).thenReturn("Done %time% %hunt% %count%");

            behavior.onHeadFound(player, headLocation, hunt);

            // leaveRun was still called (happens before saveTimedRun)
            trm.verify(() -> TimedRunManager.leaveRun(playerUuid));
        }
    }

    @Test
    void getId_returnsTimed() {
        TimedBehavior behavior = new TimedBehavior(registry, null, true);
        assertThat(behavior.getId()).isEqualTo("timed");
    }
}
