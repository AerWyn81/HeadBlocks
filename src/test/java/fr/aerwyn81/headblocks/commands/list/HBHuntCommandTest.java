package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.data.hunt.behavior.FreeBehavior;
import fr.aerwyn81.headblocks.services.*;
import fr.aerwyn81.headblocks.services.gui.types.BehaviorSelectionGui;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HBHuntCommandTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private HuntService huntService;

    @Mock
    private StorageService storageService;

    @Mock
    private LanguageService languageService;

    @Mock
    private ConfigService configService;

    @Mock
    private HeadService headService;

    @Mock
    private HuntConfigService huntConfigService;

    @Mock
    private GuiService guiService;

    @Mock
    private CommandSender consoleSender;

    @Mock
    private Player playerSender;

    private Hunt huntCommand;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getHuntService()).thenReturn(huntService);
        lenient().when(registry.getStorageService()).thenReturn(storageService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(registry.getConfigService()).thenReturn(configService);
        lenient().when(registry.getHeadService()).thenReturn(headService);
        lenient().when(registry.getHuntConfigService()).thenReturn(huntConfigService);
        lenient().when(registry.getGuiService()).thenReturn(guiService);

        lenient().when(languageService.message(anyString())).thenReturn("mock-message");
        lenient().when(languageService.message(anyString(), anyString())).thenReturn("mock-message");

        huntCommand = new Hunt(registry);
    }

    // --- No subcommand ---

    @Test
    void noSubcommand_sendsUsage() {
        huntCommand.perform(consoleSender, new String[]{"hunt"});

        verify(languageService).message("Messages.HuntUsage");
    }

    // --- Unknown subcommand ---

    @Test
    void unknownSubcommand_sendsUsage() {
        huntCommand.perform(consoleSender, new String[]{"hunt", "unknown"});

        verify(languageService).message("Messages.HuntUsage");
    }

    // ==================== CREATE ====================

    @Nested
    class Create {
        @Test
        void noName_sendsUsage() {
            huntCommand.perform(consoleSender, new String[]{"hunt", "create"});

            verify(languageService).message("Messages.HuntUsage");
        }

        @Test
        void invalidName_sendsError() {
            huntCommand.perform(consoleSender, new String[]{"hunt", "create", "bad name!"});

            verify(languageService).message("Messages.HuntInvalidName");
        }

        @Test
        void duplicateName_sendsError() {
            when(huntService.getHuntNames()).thenReturn(java.util.List.of("existing"));

            huntCommand.perform(consoleSender, new String[]{"hunt", "create", "existing"});

            verify(languageService).message("Messages.HuntAlreadyExists");
        }

        @Test
        void playerSender_opensGui() {
            when(huntService.getHuntNames()).thenReturn(java.util.List.of());
            var behaviorGui = mock(BehaviorSelectionGui.class);
            when(guiService.getBehaviorSelectionManager()).thenReturn(behaviorGui);

            huntCommand.perform(playerSender, new String[]{"hunt", "create", "newHunt"});

            verify(behaviorGui).open(playerSender, "newHunt");
        }

        @Test
        void consoleSender_createsDirectly() throws InternalException {
            when(huntService.getHuntNames()).thenReturn(java.util.List.of());

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                PluginManager pm = mock(PluginManager.class);
                bukkit.when(Bukkit::getPluginManager).thenReturn(pm);

                huntCommand.perform(consoleSender, new String[]{"hunt", "create", "testHunt"});

                verify(huntConfigService).saveHunt(any());
                verify(storageService).createHuntInDb(eq("testhunt"), eq("testHunt"), eq("ACTIVE"));
                verify(huntService).registerHunt(any());
                verify(storageService).incrementHuntVersion();
            }
        }
    }

    // ==================== DELETE ====================

    @Nested
    class Delete {
        @Test
        void noArgs_sendsUsage() {
            huntCommand.perform(consoleSender, new String[]{"hunt", "delete"});

            verify(languageService).message("Messages.HuntUsage");
        }

        @Test
        void deleteDefault_refused() {
            huntCommand.perform(consoleSender, new String[]{"hunt", "delete", "default"});

            verify(languageService).message("Messages.HuntCannotDeleteDefault");
        }

        @Test
        void huntNotFound_sendsError() {
            when(huntService.getHuntById("nonexistent")).thenReturn(null);

            huntCommand.perform(consoleSender, new String[]{"hunt", "delete", "nonexistent"});

            verify(languageService).message("Messages.HuntNotFound");
        }

        @Test
        void noConfirmFlag_sendsConfirmMessage() {
            HBHunt hunt = mock(HBHunt.class);
            when(hunt.getHeadCount()).thenReturn(5);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            huntCommand.perform(consoleSender, new String[]{"hunt", "delete", "myhunt"});

            verify(languageService).message("Messages.HuntDeleteConfirm");
        }

        @Test
        void fallbackWithoutKeepHeads_sendsError() {
            HBHunt hunt = mock(HBHunt.class);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            huntCommand.perform(consoleSender, new String[]{"hunt", "delete", "myhunt", "--fallback", "other"});

            verify(languageService).message("Messages.HuntDeleteFallbackRequiresKeepHeads");
        }

        @Test
        void keepHeadsConfirm_withFallbackNotFound_sendsError() {
            HBHunt hunt = mock(HBHunt.class);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);
            when(huntService.getHuntById("nonexistent")).thenReturn(null);

            huntCommand.perform(consoleSender, new String[]{"hunt", "delete", "myhunt", "--keepheads", "--fallback", "nonexistent"});

            verify(languageService).message("Messages.HuntDeleteFallbackNotFound");
        }
    }

    // ==================== ENABLE / DISABLE ====================

    @Nested
    class EnableDisable {
        @Test
        void enableNoArgs_sendsUsage() {
            huntCommand.perform(consoleSender, new String[]{"hunt", "enable"});

            verify(languageService).message("Messages.HuntUsage");
        }

        @Test
        void enableHuntNotFound_sendsError() {
            when(huntService.getHuntById("nope")).thenReturn(null);

            huntCommand.perform(consoleSender, new String[]{"hunt", "enable", "nope"});

            verify(languageService).message("Messages.HuntNotFound");
        }

        @Test
        void enableAlreadyActive_sendsError() {
            HBHunt hunt = mock(HBHunt.class);
            when(hunt.isActive()).thenReturn(true);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            huntCommand.perform(consoleSender, new String[]{"hunt", "enable", "myhunt"});

            verify(languageService).message("Messages.HuntAlreadyActive");
        }

        @Test
        void enableSuccess() throws InternalException {
            HBHunt hunt = mock(HBHunt.class);
            when(hunt.isActive()).thenReturn(false);
            when(hunt.getState()).thenReturn(HuntState.INACTIVE);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                PluginManager pm = mock(PluginManager.class);
                bukkit.when(Bukkit::getPluginManager).thenReturn(pm);

                huntCommand.perform(consoleSender, new String[]{"hunt", "enable", "myhunt"});

                verify(hunt).setState(HuntState.ACTIVE);
                verify(huntConfigService).saveHunt(hunt);
                verify(storageService).updateHuntStateInDb("myhunt", "ACTIVE");
                verify(storageService).incrementHuntVersion();
            }
        }

        @Test
        void disableNoArgs_sendsUsage() {
            huntCommand.perform(consoleSender, new String[]{"hunt", "disable"});

            verify(languageService).message("Messages.HuntUsage");
        }

        @Test
        void disableAlreadyInactive_sendsError() {
            HBHunt hunt = mock(HBHunt.class);
            when(hunt.isActive()).thenReturn(false);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            huntCommand.perform(consoleSender, new String[]{"hunt", "disable", "myhunt"});

            verify(languageService).message("Messages.HuntAlreadyInactive");
        }

        @Test
        void disableSuccess() throws InternalException {
            HBHunt hunt = mock(HBHunt.class);
            when(hunt.isActive()).thenReturn(true);
            when(hunt.getState()).thenReturn(HuntState.ACTIVE);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                PluginManager pm = mock(PluginManager.class);
                bukkit.when(Bukkit::getPluginManager).thenReturn(pm);

                huntCommand.perform(consoleSender, new String[]{"hunt", "disable", "myhunt"});

                verify(hunt).setState(HuntState.INACTIVE);
                verify(huntConfigService).saveHunt(hunt);
                verify(storageService).updateHuntStateInDb("myhunt", "INACTIVE");
            }
        }
    }

    // ==================== LIST ====================

    @Nested
    class ListCmd {
        @Test
        void emptyList_sendsMessage() {
            when(huntService.getAllHunts()).thenReturn(java.util.List.of());

            huntCommand.perform(consoleSender, new String[]{"hunt", "list"});

            verify(languageService).message("Messages.HuntListEmpty");
        }

        @Test
        void nonEmptyList_sendsEntries() {
            HBHunt hunt = mock(HBHunt.class);
            when(hunt.getId()).thenReturn("myhunt");
            when(hunt.getDisplayName()).thenReturn("My Hunt");
            when(hunt.getState()).thenReturn(HuntState.ACTIVE);
            when(hunt.getHeadCount()).thenReturn(5);

            when(huntService.getAllHunts()).thenReturn(java.util.List.of(hunt));

            huntCommand.perform(consoleSender, new String[]{"hunt", "list"});

            verify(consoleSender, times(2)).sendMessage(anyString());
        }
    }

    // ==================== INFO ====================

    @Nested
    class Info {
        @Test
        void noArgs_sendsUsage() {
            huntCommand.perform(consoleSender, new String[]{"hunt", "info"});

            verify(languageService).message("Messages.HuntUsage");
        }

        @Test
        void huntNotFound_sendsError() {
            when(huntService.getHuntById("nope")).thenReturn(null);

            huntCommand.perform(consoleSender, new String[]{"hunt", "info", "nope"});

            verify(languageService).message("Messages.HuntNotFound");
        }

        @Test
        void success_sendsInfo() throws InternalException {
            HBHunt hunt = mock(HBHunt.class);
            when(hunt.getId()).thenReturn("myhunt");
            when(hunt.getDisplayName()).thenReturn("My Hunt");
            when(hunt.getState()).thenReturn(HuntState.ACTIVE);
            when(hunt.getPriority()).thenReturn(1);
            when(hunt.getHeadCount()).thenReturn(3);
            when(hunt.getBehaviors()).thenReturn(java.util.List.of(new FreeBehavior()));
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);
            when(storageService.getTopPlayersForHunt("myhunt")).thenReturn(new LinkedHashMap<>());

            huntCommand.perform(consoleSender, new String[]{"hunt", "info", "myhunt"});

            // Header + name + state + priority + heads + behaviors + players = 7 messages
            verify(consoleSender, atLeast(7)).sendMessage(anyString());
        }
    }

    // ==================== SELECT ====================

    @Nested
    class Select {
        @Test
        void fromConsole_refused() {
            huntCommand.perform(consoleSender, new String[]{"hunt", "select", "myhunt"});

            verify(languageService).message("Messages.PlayerOnly");
        }

        @Test
        void noHuntArg_clearsSelection() {
            UUID playerUuid = UUID.randomUUID();
            when(playerSender.getUniqueId()).thenReturn(playerUuid);

            huntCommand.perform(playerSender, new String[]{"hunt", "select"});

            verify(huntService).clearSelectedHunt(playerUuid);
        }

        @Test
        void huntNotFound_sendsError() {
            when(huntService.getHuntById("nope")).thenReturn(null);

            huntCommand.perform(playerSender, new String[]{"hunt", "select", "nope"});

            verify(languageService).message("Messages.HuntNotFound");
        }

        @Test
        void success_setsSelection() {
            UUID playerUuid = UUID.randomUUID();
            when(playerSender.getUniqueId()).thenReturn(playerUuid);
            when(huntService.getHuntById("myhunt")).thenReturn(mock(HBHunt.class));

            huntCommand.perform(playerSender, new String[]{"hunt", "select", "myhunt"});

            verify(huntService).setSelectedHunt(playerUuid, "myhunt");
        }
    }

    // ==================== ACTIVE ====================

    @Nested
    class Active {
        @Test
        void fromConsole_refused() {
            huntCommand.perform(consoleSender, new String[]{"hunt", "active"});

            verify(languageService).message("Messages.PlayerOnly");
        }

        @Test
        void showsActiveSelection() {
            UUID playerUuid = UUID.randomUUID();
            when(playerSender.getUniqueId()).thenReturn(playerUuid);
            when(huntService.getSelectedHunt(playerUuid)).thenReturn("myhunt");

            huntCommand.perform(playerSender, new String[]{"hunt", "active"});

            verify(languageService).message("Messages.HuntActiveSelection");
        }
    }

    // ==================== TRANSFER ====================

    @Nested
    class Transfer {
        @Test
        void noArgs_sendsUsage() {
            huntCommand.perform(consoleSender, new String[]{"hunt", "transfer"});

            verify(languageService).message("Messages.HuntUsage");
        }

        @Test
        void invalidUuid_sendsError() {
            huntCommand.perform(consoleSender, new String[]{"hunt", "transfer", "not-a-uuid", "myhunt"});

            verify(languageService).message("Messages.HuntHeadNotFound");
        }

        @Test
        void headNotFound_sendsError() {
            UUID headUuid = UUID.randomUUID();
            when(headService.getHeadByUUID(headUuid)).thenReturn(null);

            huntCommand.perform(consoleSender, new String[]{"hunt", "transfer", headUuid.toString(), "myhunt"});

            verify(languageService).message("Messages.HuntHeadNotFound");
        }

        @Test
        void huntNotFound_sendsError() {
            UUID headUuid = UUID.randomUUID();
            HeadLocation hl = mock(HeadLocation.class);
            when(headService.getHeadByUUID(headUuid)).thenReturn(hl);
            when(huntService.huntExists("nope")).thenReturn(false);

            huntCommand.perform(consoleSender, new String[]{"hunt", "transfer", headUuid.toString(), "nope"});

            verify(languageService).message("Messages.HuntNotFound");
        }

        @Test
        void success_transfersHead() throws Exception {
            UUID headUuid = UUID.randomUUID();
            HeadLocation hl = mock(HeadLocation.class);
            when(hl.getNameOrUuid()).thenReturn("head1");
            when(headService.getHeadByUUID(headUuid)).thenReturn(hl);
            when(huntService.huntExists("myhunt")).thenReturn(true);

            huntCommand.perform(consoleSender, new String[]{"hunt", "transfer", headUuid.toString(), "myhunt"});

            verify(huntService).transferHead(headUuid, "myhunt");
        }
    }

    // ==================== PROGRESS ====================

    @Nested
    class ProgressCmd {
        @Test
        void noArgs_sendsUsage() {
            huntCommand.perform(consoleSender, new String[]{"hunt", "progress"});

            verify(languageService).message("Messages.HuntUsage");
        }

        @Test
        void huntNotFound_sendsError() {
            when(huntService.getHuntById("nope")).thenReturn(null);

            huntCommand.perform(consoleSender, new String[]{"hunt", "progress", "nope"});

            verify(languageService).message("Messages.HuntNotFound");
        }

        @Test
        void consoleWithoutPlayerName_refused() {
            HBHunt hunt = mock(HBHunt.class);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            huntCommand.perform(consoleSender, new String[]{"hunt", "progress", "myhunt"});

            verify(languageService).message("Messages.PlayerOnly");
        }

        @Test
        void success_withPlayerName() throws InternalException {
            HBHunt hunt = mock(HBHunt.class);
            when(hunt.getId()).thenReturn("myhunt");
            when(hunt.getDisplayName()).thenReturn("My Hunt");
            when(hunt.getHeadCount()).thenReturn(5);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            UUID playerUuid = UUID.randomUUID();
            when(storageService.getPlayerByName("TestPlayer"))
                    .thenReturn(new PlayerProfileLight(playerUuid, "TestPlayer", ""));
            when(storageService.getHeadsPlayerForHunt(playerUuid, "myhunt"))
                    .thenReturn(new ArrayList<>(java.util.List.of(UUID.randomUUID(), UUID.randomUUID())));
            when(configService.progressBarBars()).thenReturn(10);
            when(configService.progressBarSymbol()).thenReturn("|");
            when(configService.progressBarCompletedColor()).thenReturn("&a");
            when(configService.progressBarNotCompletedColor()).thenReturn("&c");

            try (MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class)) {
                mu.when(() -> MessageUtils.createProgressBar(anyInt(), anyInt(), anyInt(), anyString(), anyString(), anyString()))
                        .thenReturn("[||||||----]");

                huntCommand.perform(consoleSender, new String[]{"hunt", "progress", "myhunt", "TestPlayer"});

                verify(consoleSender).sendMessage(anyString());
            }
        }
    }

    // ==================== TOP ====================

    @Nested
    class TopCmd {
        @Test
        void noArgs_sendsUsage() {
            huntCommand.perform(consoleSender, new String[]{"hunt", "top"});

            verify(languageService).message("Messages.HuntUsage");
        }

        @Test
        void huntNotFound_sendsError() {
            when(huntService.getHuntById("nope")).thenReturn(null);

            huntCommand.perform(consoleSender, new String[]{"hunt", "top", "nope"});

            verify(languageService).message("Messages.HuntNotFound");
        }

        @Test
        void emptyTop_sendsMessage() throws InternalException {
            HBHunt hunt = mock(HBHunt.class);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);
            when(storageService.getTopPlayersForHunt("myhunt")).thenReturn(new LinkedHashMap<>());

            huntCommand.perform(consoleSender, new String[]{"hunt", "top", "myhunt"});

            verify(languageService).message("Messages.TopEmpty");
        }

        @Test
        void success_showsEntries() throws InternalException {
            HBHunt hunt = mock(HBHunt.class);
            when(hunt.getId()).thenReturn("myhunt");
            when(hunt.getDisplayName()).thenReturn("My Hunt");
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            LinkedHashMap<PlayerProfileLight, Integer> top = new LinkedHashMap<>();
            top.put(new PlayerProfileLight(UUID.randomUUID(), "Player1", ""), 10);
            top.put(new PlayerProfileLight(UUID.randomUUID(), "Player2", ""), 5);
            when(storageService.getTopPlayersForHunt("myhunt")).thenReturn(top);

            try (MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class)) {
                mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(i -> i.getArgument(0));

                huntCommand.perform(consoleSender, new String[]{"hunt", "top", "myhunt"});

                // header + 2 entries = at least 3 messages
                verify(consoleSender, atLeast(3)).sendMessage(anyString());
            }
        }
    }

    // ==================== RESET ====================

    @Nested
    class ResetCmd {
        @Test
        void noArgs_sendsUsage() {
            huntCommand.perform(consoleSender, new String[]{"hunt", "reset"});

            verify(languageService).message("Messages.HuntUsage");
        }

        @Test
        void huntNotFound_sendsError() {
            when(huntService.getHuntById("nope")).thenReturn(null);

            huntCommand.perform(consoleSender, new String[]{"hunt", "reset", "nope", "TestPlayer"});

            verify(languageService).message("Messages.HuntNotFound");
        }

        @Test
        void playerNotFound_sendsError() throws InternalException {
            HBHunt hunt = mock(HBHunt.class);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);
            when(storageService.getPlayerByName("Nobody")).thenReturn(null);

            huntCommand.perform(consoleSender, new String[]{"hunt", "reset", "myhunt", "Nobody"});

            verify(languageService).message("Messages.PlayerNotFound", "Nobody");
        }

        @Test
        void success_resetsPlayer() throws InternalException {
            HBHunt hunt = mock(HBHunt.class);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            UUID playerUuid = UUID.randomUUID();
            when(storageService.getPlayerByName("TestPlayer"))
                    .thenReturn(new PlayerProfileLight(playerUuid, "TestPlayer", ""));

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer(playerUuid)).thenReturn(null);

                huntCommand.perform(consoleSender, new String[]{"hunt", "reset", "myhunt", "TestPlayer"});

                verify(storageService).resetPlayerHunt(playerUuid, "myhunt");
            }
        }
    }

    // ==================== TAB COMPLETION ====================

    @Nested
    class TabCompletion {
        @Test
        void firstArg_returnsSubcommands() {
            ArrayList<String> result = huntCommand.tabComplete(consoleSender, new String[]{"hunt", ""});

            assertThat(result).contains("create", "delete", "enable", "disable", "list", "info",
                    "select", "active", "set", "assign", "transfer", "progress", "top", "reset");
        }

        @Test
        void firstArg_filters() {
            ArrayList<String> result = huntCommand.tabComplete(consoleSender, new String[]{"hunt", "cr"});

            assertThat(result).containsExactly("create");
        }

        @Test
        void deleteSecondArg_returnsHuntNamesExceptDefault() {
            when(huntService.getHuntNames()).thenReturn(java.util.List.of("default", "myhunt", "other"));

            ArrayList<String> result = huntCommand.tabComplete(consoleSender, new String[]{"hunt", "delete", ""});

            assertThat(result).contains("myhunt", "other");
            assertThat(result).doesNotContain("default");
        }

        @Test
        void enableSecondArg_returnsAllHuntNames() {
            when(huntService.getHuntNames()).thenReturn(java.util.List.of("default", "myhunt"));

            ArrayList<String> result = huntCommand.tabComplete(consoleSender, new String[]{"hunt", "enable", ""});

            assertThat(result).contains("default", "myhunt");
        }

        @Test
        void transferSecondArg_returnsHeadNames() {
            when(headService.getHeadRawNameOrUuid()).thenReturn(new ArrayList<>(java.util.List.of("head1", "head2")));

            ArrayList<String> result = huntCommand.tabComplete(consoleSender, new String[]{"hunt", "transfer", ""});

            assertThat(result).contains("head1", "head2");
        }

        @Test
        void assignThirdArg_returnsModes() {
            ArrayList<String> result = huntCommand.tabComplete(consoleSender, new String[]{"hunt", "assign", "myhunt", ""});

            assertThat(result).contains("all", "radius");
        }

        @Test
        void transferThirdArg_returnsHuntNames() {
            when(huntService.getHuntNames()).thenReturn(java.util.List.of("default", "myhunt"));

            ArrayList<String> result = huntCommand.tabComplete(consoleSender, new String[]{"hunt", "transfer", "headuuid", ""});

            assertThat(result).contains("default", "myhunt");
        }

        @Test
        void deleteFourthArg_returnsFlags() {
            ArrayList<String> result = huntCommand.tabComplete(consoleSender, new String[]{"hunt", "delete", "myhunt", ""});

            assertThat(result).contains("--confirm", "--keepHeads");
        }

        @Test
        void deleteFallbackAfterKeepHeads_showsFallbackFlag() {
            ArrayList<String> result = huntCommand.tabComplete(consoleSender, new String[]{"hunt", "delete", "myhunt", "--keepheads", ""});

            assertThat(result).contains("--fallback");
        }

        @Test
        void deleteAfterFallback_returnsHuntNames() {
            when(huntService.getHuntNames()).thenReturn(java.util.List.of("default", "myhunt", "other"));

            ArrayList<String> result = huntCommand.tabComplete(consoleSender, new String[]{"hunt", "delete", "myhunt", "--keepheads", "--fallback", ""});

            assertThat(result).contains("default", "other");
            assertThat(result).doesNotContain("myhunt");
        }

        @Test
        void unknownLength_returnsEmpty() {
            ArrayList<String> result = huntCommand.tabComplete(consoleSender, new String[]{"hunt", "list", "extra", "args", "here"});

            assertThat(result).isEmpty();
        }
    }
}
