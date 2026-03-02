package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.HuntService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResetAllCommandTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private StorageService storageService;

    @Mock
    private LanguageService languageService;

    @Mock
    private HeadService headService;

    @Mock
    private HuntService huntService;

    @Mock
    private Player playerSender;

    private ResetAll command;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getStorageService()).thenReturn(storageService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(registry.getHeadService()).thenReturn(headService);
        lenient().when(registry.getHuntService()).thenReturn(huntService);
        lenient().when(languageService.message(anyString())).thenReturn("mock-message");
        command = new ResetAll(registry);
    }

    @Nested
    class ResetAllPlayers {

        @Test
        void noData_sendsNoDataMessage() throws InternalException {
            when(storageService.getAllPlayers()).thenReturn(new ArrayList<>());

            boolean result = command.perform(playerSender, new String[]{"resetall", "--confirm"});

            assertThat(result).isTrue();
            verify(languageService).message("Messages.ResetAllNoData");
        }

        @Test
        void noConfirm_sendsConfirmMessage() throws InternalException {
            java.util.List<UUID> players = java.util.List.of(UUID.randomUUID(), UUID.randomUUID());
            when(storageService.getAllPlayers()).thenReturn(players);

            boolean result = command.perform(playerSender, new String[]{"resetall"});

            assertThat(result).isTrue();
            verify(languageService).message("Messages.ResetAllConfirm");
        }

        @Test
        void withConfirm_resetsAllPlayers() throws InternalException {
            UUID p1 = UUID.randomUUID();
            UUID p2 = UUID.randomUUID();
            java.util.List<UUID> players = java.util.List.of(p1, p2);
            when(storageService.getAllPlayers()).thenReturn(players);

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
                 MockedStatic<HeadBlocks> hb = mockStatic(HeadBlocks.class)) {
                bukkit.when(() -> Bukkit.getPlayer(any(UUID.class))).thenReturn(null);

                boolean result = command.perform(playerSender, new String[]{"resetall", "--confirm"});

                assertThat(result).isTrue();
                verify(storageService).resetPlayer(p1);
                verify(storageService).resetPlayer(p2);
                verify(languageService).message("Messages.ResetAllSuccess");
            }
        }

        @Test
        void storageErrorOnGetPlayers_sendsError() throws InternalException {
            when(storageService.getAllPlayers()).thenThrow(new InternalException("db error"));

            boolean result = command.perform(playerSender, new String[]{"resetall", "--confirm"});

            assertThat(result).isTrue();
            verify(languageService).message("Messages.StorageError");
        }

        @Test
        void storageErrorDuringReset_sendsError() throws InternalException {
            UUID p1 = UUID.randomUUID();
            java.util.List<UUID> players = java.util.List.of(p1);
            when(storageService.getAllPlayers()).thenReturn(players);
            doThrow(new InternalException("db error")).when(storageService).resetPlayer(p1);

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {

                boolean result = command.perform(playerSender, new String[]{"resetall", "--confirm"});

                assertThat(result).isTrue();
                verify(languageService).message("Messages.StorageError");
            }
        }
    }

    @Nested
    class ResetSpecificHead {

        @Test
        void noPlayersWithHead_sendsNoDataMessage() throws InternalException {
            UUID headUuid = UUID.randomUUID();
            HeadLocation headLocation = mock(HeadLocation.class);
            when(headLocation.getUuid()).thenReturn(headUuid);
            when(headService.resolveHeadIdentifier(headUuid.toString())).thenReturn(headLocation);
            when(storageService.getPlayers(headUuid)).thenReturn(new ArrayList<UUID>());

            boolean result = command.perform(playerSender, new String[]{"resetall", "--head", headUuid.toString(), "--confirm"});

            assertThat(result).isTrue();
            verify(languageService).message("Messages.ResetAllNoData");
        }

        @Test
        void noConfirm_sendsConfirmMessage() throws InternalException {
            UUID headUuid = UUID.randomUUID();
            HeadLocation headLocation = mock(HeadLocation.class);
            when(headLocation.getUuid()).thenReturn(headUuid);
            when(headService.resolveHeadIdentifier(headUuid.toString())).thenReturn(headLocation);
            when(headService.getHeadByUUID(headUuid)).thenReturn(headLocation);
            when(headLocation.getNameOrUnnamed(anyString())).thenReturn("testHead");
            ArrayList<UUID> players = new ArrayList<>(java.util.List.of(UUID.randomUUID()));
            when(storageService.getPlayers(headUuid)).thenReturn(players);

            boolean result = command.perform(playerSender, new String[]{"resetall", "--head", headUuid.toString()});

            assertThat(result).isTrue();
            verify(languageService).message("Messages.ResetAllHeadConfirm");
        }

        @Test
        void withConfirm_resetsAllPlayersForHead() throws InternalException {
            UUID headUuid = UUID.randomUUID();
            UUID p1 = UUID.randomUUID();
            HeadLocation headLocation = mock(HeadLocation.class);
            when(headLocation.getUuid()).thenReturn(headUuid);
            when(headService.resolveHeadIdentifier(headUuid.toString())).thenReturn(headLocation);
            when(headService.getHeadByUUID(headUuid)).thenReturn(headLocation);
            when(headLocation.getNameOrUnnamed(anyString())).thenReturn("testHead");
            ArrayList<UUID> playersWithHead = new ArrayList<>(java.util.List.of(p1));
            when(storageService.getPlayers(headUuid)).thenReturn(playersWithHead);

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
                 MockedStatic<HeadBlocks> hb = mockStatic(HeadBlocks.class)) {
                bukkit.when(() -> Bukkit.getPlayer(p1)).thenReturn(null);

                boolean result = command.perform(playerSender, new String[]{"resetall", "--head", headUuid.toString(), "--confirm"});

                assertThat(result).isTrue();
                verify(storageService).resetPlayerHead(p1, headUuid);
                verify(languageService).message("Messages.ResetAllHeadSuccess");
            }
        }

        @Test
        void headNotFound_sendsError() {
            when(headService.resolveHeadIdentifier("unknown")).thenReturn(null);

            boolean result = command.perform(playerSender, new String[]{"resetall", "--head", "unknown"});

            assertThat(result).isTrue();
            verify(languageService).message("Messages.HeadNameNotFound");
        }
    }

    @Nested
    class TabCompletion {

        @Test
        void secondArg_returnsConfirmAndHead() {
            var result = command.tabComplete(playerSender, new String[]{"resetall", ""});

            assertThat(result).contains("--confirm", "--head");
        }

        @Test
        void thirdArgAfterHead_returnsConfirmAndHeadNames() {
            var names = new ArrayList<>(java.util.List.of("head1", "head2"));
            when(headService.getHeadRawNameOrUuid()).thenReturn(names);

            var result = command.tabComplete(playerSender, new String[]{"resetall", "--head", ""});

            assertThat(result).contains("--confirm", "head1", "head2");
        }

        @Test
        void thirdArgAfterConfirm_returnsHead() {
            var result = command.tabComplete(playerSender, new String[]{"resetall", "--confirm", ""});

            assertThat(result).contains("--head");
        }

        @Test
        void fifthArgAfterHeadAndValue_returnsConfirm() {
            var result = command.tabComplete(playerSender, new String[]{"resetall", "--head", "head1", "x", ""});

            assertThat(result).isEmpty();
        }
    }
}
