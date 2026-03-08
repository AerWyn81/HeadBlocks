package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.utils.bukkit.PluginProvider;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import fr.aerwyn81.headblocks.utils.runnables.BukkitFutureResult;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaceholdersServiceTest {

    @Mock
    private StorageService storageService;

    @Mock
    private ConfigService configService;

    @Mock
    private LanguageService languageService;

    @Mock
    private PluginProvider pluginProvider;

    @Mock
    private HuntService huntService;

    @Mock
    private Plugin plugin;

    private PlaceholdersService placeholdersService;

    @BeforeEach
    void setUp() {
        lenient().when(pluginProvider.isPlaceholderApiActive()).thenReturn(false);
        placeholdersService = new PlaceholdersService(storageService, configService, languageService, pluginProvider, huntService);
    }

    // --- Helpers ---

    private void stubMessageUtilsPassthrough(MockedStatic<MessageUtils> mocked) {
        mocked.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));
        mocked.when(() -> MessageUtils.centerMessage(anyString())).thenAnswer(inv -> inv.getArgument(0));
        mocked.when(() -> MessageUtils.createProgressBar(anyInt(), anyInt(), anyInt(), anyString(), anyString(), anyString()))
                .thenReturn("[====]");
    }

    private BukkitFutureResult<Set<UUID>> fakeFutureResult(Set<UUID> heads) {
        return BukkitFutureResult.of(plugin, CompletableFuture.completedFuture(heads));
    }

    // =========================================================================
    // parse: %player% replacement
    // =========================================================================

    @Test
    void parse_replaces_player_placeholder() {
        when(languageService.prefix()).thenReturn("[HB]");

        try (MockedStatic<MessageUtils> mocked = mockStatic(MessageUtils.class)) {
            stubMessageUtilsPassthrough(mocked);

            String result = placeholdersService.parse("Steve", UUID.randomUUID(), null, "Hello %player%!");

            assertThat(result).isEqualTo("Hello Steve!");
        }
    }

    // =========================================================================
    // parse: %prefix% replacement
    // =========================================================================

    @Test
    void parse_replaces_prefix_placeholder() {
        when(languageService.prefix()).thenReturn("[HB]");

        try (MockedStatic<MessageUtils> mocked = mockStatic(MessageUtils.class)) {
            stubMessageUtilsPassthrough(mocked);

            String result = placeholdersService.parse("Steve", UUID.randomUUID(), null, "%prefix% Welcome");

            assertThat(result).isEqualTo("[HB] Welcome");
        }
    }

    @Test
    void parse_replaces_both_player_and_prefix() {
        when(languageService.prefix()).thenReturn("[HB]");

        try (MockedStatic<MessageUtils> mocked = mockStatic(MessageUtils.class)) {
            stubMessageUtilsPassthrough(mocked);

            String result = placeholdersService.parse("Alex", UUID.randomUUID(), null, "%prefix% Hello %player%");

            assertThat(result).isEqualTo("[HB] Hello Alex");
        }
    }

    // =========================================================================
    // parse: %current%, %max%, %left%
    // =========================================================================

    @Test
    void parse_replaces_current_max_left_placeholders() throws InternalException {
        when(languageService.prefix()).thenReturn("");
        UUID pUuid = UUID.randomUUID();

        Set<UUID> playerHeads = Set.of(UUID.randomUUID(), UUID.randomUUID());
        when(storageService.getHeadsPlayer(pUuid)).thenReturn(fakeFutureResult(playerHeads));

        ArrayList<UUID> allHeads = new ArrayList<>(List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
        when(storageService.getHeads()).thenReturn(allHeads);

        try (MockedStatic<MessageUtils> mocked = mockStatic(MessageUtils.class)) {
            stubMessageUtilsPassthrough(mocked);

            String result = placeholdersService.parse("Steve", pUuid, null, "Found %current%/%max% Left: %left%");

            assertThat(result).isEqualTo("Found 2/5 Left: 3");
        }
    }

    // =========================================================================
    // parse: %headName% with various headLocation states
    // =========================================================================

    @Test
    void parse_headName_with_null_headLocation_shows_notSet_message() throws InternalException {
        when(languageService.prefix()).thenReturn("");
        UUID pUuid = UUID.randomUUID();

        when(storageService.getHeadsPlayer(pUuid)).thenReturn(fakeFutureResult(new HashSet<>()));
        when(storageService.getHeads()).thenReturn(new ArrayList<>());
        when(languageService.message("Other.NameNotSet")).thenReturn("NoName");

        try (MockedStatic<MessageUtils> mocked = mockStatic(MessageUtils.class)) {
            stubMessageUtilsPassthrough(mocked);

            String result = placeholdersService.parse("Steve", pUuid, null, "Head: %headName%");

            assertThat(result).isEqualTo("Head: NoName");
        }
    }

    @Test
    void parse_headName_with_headLocation_having_name_shows_name() throws InternalException {
        when(languageService.prefix()).thenReturn("");
        UUID pUuid = UUID.randomUUID();

        HeadLocation headLocation = mock(HeadLocation.class);
        when(headLocation.getName()).thenReturn("GoldenHead");

        when(storageService.getHeadsPlayer(pUuid)).thenReturn(fakeFutureResult(new HashSet<>()));
        when(storageService.getHeads()).thenReturn(new ArrayList<>());

        try (MockedStatic<MessageUtils> mocked = mockStatic(MessageUtils.class)) {
            stubMessageUtilsPassthrough(mocked);

            String result = placeholdersService.parse("Steve", pUuid, headLocation, "Head: %headName%");

            assertThat(result).isEqualTo("Head: GoldenHead");
        }
    }

    @Test
    void parse_headName_with_headLocation_empty_name_shows_uuid() throws InternalException {
        when(languageService.prefix()).thenReturn("");
        UUID pUuid = UUID.randomUUID();
        UUID headUuid = UUID.randomUUID();

        HeadLocation headLocation = mock(HeadLocation.class);
        when(headLocation.getName()).thenReturn("");
        when(headLocation.getUuid()).thenReturn(headUuid);

        when(storageService.getHeadsPlayer(pUuid)).thenReturn(fakeFutureResult(new HashSet<>()));
        when(storageService.getHeads()).thenReturn(new ArrayList<>());

        try (MockedStatic<MessageUtils> mocked = mockStatic(MessageUtils.class)) {
            stubMessageUtilsPassthrough(mocked);

            String result = placeholdersService.parse("Steve", pUuid, headLocation, "Head: %headName%");

            assertThat(result).isEqualTo("Head: " + headUuid);
        }
    }

    // =========================================================================
    // parse: %progress%
    // =========================================================================

    @Test
    void parse_progress_creates_progress_bar() throws InternalException {
        when(languageService.prefix()).thenReturn("");
        UUID pUuid = UUID.randomUUID();

        Set<UUID> playerHeads = Set.of(UUID.randomUUID());
        when(storageService.getHeadsPlayer(pUuid)).thenReturn(fakeFutureResult(playerHeads));

        ArrayList<UUID> allHeads = new ArrayList<>(List.of(UUID.randomUUID(), UUID.randomUUID()));
        when(storageService.getHeads()).thenReturn(allHeads);

        when(configService.progressBarBars()).thenReturn(10);
        when(configService.progressBarSymbol()).thenReturn("|");
        when(configService.progressBarCompletedColor()).thenReturn("&a");
        when(configService.progressBarNotCompletedColor()).thenReturn("&7");

        try (MockedStatic<MessageUtils> mocked = mockStatic(MessageUtils.class)) {
            stubMessageUtilsPassthrough(mocked);

            String result = placeholdersService.parse("Steve", pUuid, null, "Progress: %progress%");

            assertThat(result).isEqualTo("Progress: [====]");
            mocked.verify(() -> MessageUtils.createProgressBar(1, 2, 10, "|", "&a", "&7"));
        }
    }

    // =========================================================================
    // parse: no special placeholders - just colorize
    // =========================================================================

    @Test
    void parse_simple_message_without_special_placeholders_colorized() {
        when(languageService.prefix()).thenReturn("");

        try (MockedStatic<MessageUtils> mocked = mockStatic(MessageUtils.class)) {
            stubMessageUtilsPassthrough(mocked);

            String result = placeholdersService.parse("Steve", UUID.randomUUID(), null, "Just a plain message");

            assertThat(result).isEqualTo("Just a plain message");
            mocked.verify(() -> MessageUtils.colorize("Just a plain message"));
        }
    }

    // =========================================================================
    // parse batch: multiple messages
    // =========================================================================

    @Test
    void parse_batch_parses_all_messages() {
        when(languageService.prefix()).thenReturn("[P]");
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Steve");
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        try (MockedStatic<MessageUtils> mocked = mockStatic(MessageUtils.class)) {
            stubMessageUtilsPassthrough(mocked);

            String[] result = placeholdersService.parse(player, null, List.of("Hello %player%", "%prefix% World"));

            assertThat(result).hasSize(2);
            assertThat(result[0]).isEqualTo("Hello Steve");
            assertThat(result[1]).isEqualTo("[P] World");
        }
    }

    @Test
    void parse_batch_empty_list_returns_empty_array() {
        Player player = mock(Player.class);
        lenient().when(player.getName()).thenReturn("Steve");
        lenient().when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        String[] result = placeholdersService.parse(player, null, List.of());

        assertThat(result).isEmpty();
    }
}
