package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.services.*;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.internal.CommandsUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
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
class ProgressCommandTest {

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
    private PlaceholdersService placeholdersService;

    @Mock
    private Player player;

    private Progress command;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getHuntService()).thenReturn(huntService);
        lenient().when(registry.getStorageService()).thenReturn(storageService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(registry.getConfigService()).thenReturn(configService);
        lenient().when(registry.getPlaceholdersService()).thenReturn(placeholdersService);
        lenient().when(languageService.message(anyString())).thenReturn("mock-message");
        command = new Progress(registry);
    }

    @Nested
    class LegacyMode {

        @Test
        void playerNotFound_returnsTrue() {
            try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class);
                 MockedStatic<CommandsUtils> cu = mockStatic(CommandsUtils.class)) {
                pu.when(() -> PlayerUtils.hasPermission(any(), anyString())).thenReturn(true);
                cu.when(() -> CommandsUtils.extractAndGetPlayerUuidByName(eq(registry), any(), any(), anyBoolean()))
                        .thenReturn(null);

                boolean result = command.perform(player, new String[]{"progress"});

                assertThat(result).isTrue();
                verify(huntService, never()).isMultiHunt();
            }
        }

        @Test
        void legacyMode_sendsMessages() {
            UUID playerUuid = UUID.randomUUID();
            PlayerProfileLight profile = new PlayerProfileLight(playerUuid, "testPlayer", "");

            try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class);
                 MockedStatic<CommandsUtils> cu = mockStatic(CommandsUtils.class)) {
                pu.when(() -> PlayerUtils.hasPermission(any(), anyString())).thenReturn(true);
                cu.when(() -> CommandsUtils.extractAndGetPlayerUuidByName(eq(registry), any(), any(), anyBoolean()))
                        .thenReturn(profile);
                when(huntService.isMultiHunt()).thenReturn(false);

                ArrayList<String> messages = new ArrayList<>(java.util.List.of("line1", "line2"));
                when(languageService.messageList("Messages.ProgressCommand")).thenReturn(messages);
                when(placeholdersService.parse(eq("testPlayer"), eq(playerUuid), anyString()))
                        .thenReturn("parsed-line");

                boolean result = command.perform(player, new String[]{"progress"});

                assertThat(result).isTrue();
                verify(player, times(2)).sendMessage("parsed-line");
            }
        }
    }

    @Nested
    class MultiHuntMode {

        @Test
        void multiHunt_sendsHuntProgress() throws InternalException {
            UUID playerUuid = UUID.randomUUID();
            PlayerProfileLight profile = new PlayerProfileLight(playerUuid, "testPlayer", "");

            Hunt hunt1 = mock(Hunt.class);
            when(hunt1.getId()).thenReturn("hunt1");
            when(hunt1.getDisplayName()).thenReturn("Hunt One");
            when(hunt1.getHeadCount()).thenReturn(10);
            var huntState = mock(HuntState.class);
            when(hunt1.getState()).thenReturn(huntState);
            when(huntState.getLocalizedName(languageService)).thenReturn("Active");

            ArrayList<Hunt> hunts = new ArrayList<>(java.util.List.of(hunt1));
            ArrayList<UUID> playerHuntHeads = new ArrayList<>(java.util.List.of(UUID.randomUUID(), UUID.randomUUID()));

            try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class);
                 MockedStatic<CommandsUtils> cu = mockStatic(CommandsUtils.class);
                 MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class)) {
                pu.when(() -> PlayerUtils.hasPermission(any(), anyString())).thenReturn(true);
                cu.when(() -> CommandsUtils.extractAndGetPlayerUuidByName(eq(registry), any(), any(), anyBoolean()))
                        .thenReturn(profile);
                mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));
                mu.when(() -> MessageUtils.createProgressBar(anyInt(), anyInt(), anyInt(), anyString(), anyString(), anyString()))
                        .thenReturn("[====------]");

                when(huntService.isMultiHunt()).thenReturn(true);
                when(huntService.getAllHunts()).thenReturn(hunts);
                when(storageService.getHeadsPlayerForHunt(playerUuid, "hunt1")).thenReturn(playerHuntHeads);
                when(configService.progressBarBars()).thenReturn(10);
                when(configService.progressBarSymbol()).thenReturn("|");
                when(configService.progressBarCompletedColor()).thenReturn("&a");
                when(configService.progressBarNotCompletedColor()).thenReturn("&7");

                boolean result = command.perform(player, new String[]{"progress"});

                assertThat(result).isTrue();
                verify(languageService).message("Messages.HuntProgressHeader");
                verify(languageService).message("Messages.HuntProgressEntry");
            }
        }

        @Test
        void multiHunt_storageError_continuesGracefully() throws InternalException {
            UUID playerUuid = UUID.randomUUID();
            PlayerProfileLight profile = new PlayerProfileLight(playerUuid, "testPlayer", "");

            Hunt hunt1 = mock(Hunt.class);
            when(hunt1.getId()).thenReturn("hunt1");
            ArrayList<Hunt> hunts = new ArrayList<>(java.util.List.of(hunt1));

            try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class);
                 MockedStatic<CommandsUtils> cu = mockStatic(CommandsUtils.class)) {
                pu.when(() -> PlayerUtils.hasPermission(any(), anyString())).thenReturn(true);
                cu.when(() -> CommandsUtils.extractAndGetPlayerUuidByName(eq(registry), any(), any(), anyBoolean()))
                        .thenReturn(profile);

                when(huntService.isMultiHunt()).thenReturn(true);
                when(huntService.getAllHunts()).thenReturn(hunts);
                when(storageService.getHeadsPlayerForHunt(playerUuid, "hunt1"))
                        .thenThrow(new InternalException("db error"));

                boolean result = command.perform(player, new String[]{"progress"});

                assertThat(result).isTrue();
                // Should not crash, just log and continue
            }
        }
    }

    @Test
    void tabComplete_returnsEmpty_forThirdArg() {
        ArrayList<String> result = command.tabComplete(player, new String[]{"progress", "name", ""});

        assertThat(result).isEmpty();
    }
}
