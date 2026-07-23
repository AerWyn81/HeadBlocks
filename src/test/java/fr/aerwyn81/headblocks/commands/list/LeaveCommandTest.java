package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.TimedRunManager;
import fr.aerwyn81.headblocks.services.ZoneEnforcementService;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveCommandTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private LanguageService languageService;

    @Mock
    private ZoneEnforcementService zoneEnforcementService;

    @Mock
    private Player player;

    private Leave command;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(registry.getZoneEnforcementService()).thenReturn(zoneEnforcementService);
        lenient().when(languageService.message(anyString())).thenReturn("mock-message");
        command = new Leave(registry);
    }

    @Test
    void notInRunAndNotEngaged_sendsNoActiveRunMessage() {
        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(zoneEnforcementService.leave(player)).thenReturn(false);

        try (MockedStatic<TimedRunManager> trm = mockStatic(TimedRunManager.class)) {
            trm.when(() -> TimedRunManager.isInRun(playerUuid)).thenReturn(false);

            boolean result = command.perform(player, new String[]{"leave"});

            assertThat(result).isTrue();
            verify(languageService).message("Messages.TimedNoActiveRun");
            verify(player).sendMessage("mock-message");
        }
    }

    @Test
    void inRun_leavesAndSendsMessage() {
        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);

        try (MockedStatic<TimedRunManager> trm = mockStatic(TimedRunManager.class)) {
            trm.when(() -> TimedRunManager.isInRun(playerUuid)).thenReturn(true);

            boolean result = command.perform(player, new String[]{"leave"});

            assertThat(result).isTrue();
            trm.verify(() -> TimedRunManager.leaveRun(playerUuid));
            verify(languageService).message("Messages.TimedLeft");
            verify(player).sendMessage("mock-message");
            verifyNoInteractions(zoneEnforcementService);
        }
    }

    @Test
    void engagedInZone_leavesZoneAndSendsMessage() {
        UUID playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(zoneEnforcementService.leave(player)).thenReturn(true);

        try (MockedStatic<TimedRunManager> trm = mockStatic(TimedRunManager.class)) {
            trm.when(() -> TimedRunManager.isInRun(playerUuid)).thenReturn(false);

            boolean result = command.perform(player, new String[]{"leave"});

            assertThat(result).isTrue();
            verify(zoneEnforcementService).leave(player);
            verify(languageService).message("Messages.ZoneLeft");
            verify(player).sendMessage("mock-message");
        }
    }

    @Test
    void tabComplete_returnsEmpty() {
        assertThat(command.tabComplete(player, new String[]{"leave"})).isEmpty();
    }
}
