package fr.aerwyn81.headblocks.hooks;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.services.*;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import fr.aerwyn81.headblocks.utils.runnables.BukkitFutureResult;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
class PlaceholderHookTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private StorageService storageService;

    @Mock
    private HuntService huntService;

    @Mock
    private HeadService headService;

    @Mock
    private ConfigService configService;

    @Mock
    private LanguageService languageService;

    @Mock
    private OfflinePlayer player;

    private PlaceholderHook hook;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getStorageService()).thenReturn(storageService);
        lenient().when(registry.getHuntService()).thenReturn(huntService);
        lenient().when(registry.getHeadService()).thenReturn(headService);
        lenient().when(registry.getConfigService()).thenReturn(configService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);

        hook = new PlaceholderHook(registry);
    }

    // --- Metadata ---

    @Test
    void getIdentifier_returnsHeadblocks() {
        assertThat(hook.getIdentifier()).isEqualTo("headblocks");
    }

    @Test
    void getAuthor_returnsAuthor() {
        assertThat(hook.getAuthor()).isEqualTo("AerWyn81");
    }

    @Test
    void persist_returnsTrue() {
        assertThat(hook.persist()).isTrue();
    }

    // --- Null player ---

    @Test
    void nullPlayer_returnsEmpty() {
        assertThat(hook.onRequest(null, "current")).isEqualTo("");
    }

    // --- current/left ---

    @Nested
    class CurrentLeft {
        @Test
        void current_multiHunt_returnsHint() {
            when(huntService.isMultiHunt()).thenReturn(true);

            String result = hook.onRequest(player, "current");

            assertThat(result).contains("hunt");
        }

        @SuppressWarnings("unchecked")
        @Test
        void current_singleHunt_returnsCount() {
            UUID playerUuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);
            when(huntService.isMultiHunt()).thenReturn(false);

            BukkitFutureResult<Set<UUID>> futureResult = mock(BukkitFutureResult.class);
            CompletableFuture<Set<UUID>> cf = CompletableFuture.completedFuture(Set.of(UUID.randomUUID(), UUID.randomUUID()));
            when(futureResult.asFuture()).thenReturn(cf);
            when(storageService.getHeadsPlayer(playerUuid)).thenReturn(futureResult);

            String result = hook.onRequest(player, "current");

            assertThat(result).isEqualTo("2");
        }

        @SuppressWarnings("unchecked")
        @Test
        void left_singleHunt_returnsRemaining() throws InternalException {
            UUID playerUuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);
            when(huntService.isMultiHunt()).thenReturn(false);

            BukkitFutureResult<Set<UUID>> futureResult = mock(BukkitFutureResult.class);
            CompletableFuture<Set<UUID>> cf = CompletableFuture.completedFuture(Set.of(UUID.randomUUID()));
            when(futureResult.asFuture()).thenReturn(cf);
            when(storageService.getHeadsPlayer(playerUuid)).thenReturn(futureResult);
            when(storageService.getHeads()).thenReturn(new ArrayList<>(java.util.List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())));

            String result = hook.onRequest(player, "left");

            assertThat(result).isEqualTo("2");
        }
    }

    // --- max ---

    @Test
    void max_returnsHeadCount() throws InternalException {
        when(storageService.getHeads()).thenReturn(new ArrayList<>(java.util.List.of(UUID.randomUUID(), UUID.randomUUID())));

        String result = hook.onRequest(player, "max");

        assertThat(result).isEqualTo("2");
    }

    // --- hasHead ---

    @Nested
    class HasHead {
        @Test
        void hasHead_byUuid_returnsBoolean() throws InternalException {
            UUID playerUuid = UUID.randomUUID();
            UUID headUuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);
            when(storageService.hasHead(playerUuid, headUuid)).thenReturn(true);

            String result = hook.onRequest(player, "hasHead_" + headUuid);

            assertThat(result).isEqualTo("true");
        }

        @Test
        void hasHead_byName_returnsBoolean() throws InternalException {
            UUID playerUuid = UUID.randomUUID();
            UUID headUuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);

            HeadLocation hl = mock(HeadLocation.class);
            when(hl.getUuid()).thenReturn(headUuid);
            when(headService.getHeadByName("dragon")).thenReturn(hl);
            when(storageService.hasHead(playerUuid, headUuid)).thenReturn(false);

            String result = hook.onRequest(player, "hasHead_dragon");

            assertThat(result).isEqualTo("false");
        }

        @Test
        void hasHead_byName_unknownHead_returnsError() {
            when(headService.getHeadByName("unknown")).thenReturn(null);

            String result = hook.onRequest(player, "hasHead_unknown");

            assertThat(result).contains("Unknown head");
        }
    }

    // --- hunt_<id>_<type> ---

    @Nested
    class HBHuntPlaceholders {
        @Test
        void huntNotFound_returnsEmpty() {
            when(huntService.getHuntById("nope")).thenReturn(null);

            String result = hook.onRequest(player, "hunt_nope_found");

            assertThat(result).isEqualTo("");
        }

        @Test
        void huntFound_returnsCount() throws InternalException {
            UUID playerUuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);

            HBHunt hunt = mock(HBHunt.class);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);
            when(storageService.getHeadsPlayerForHunt(playerUuid, "myhunt"))
                    .thenReturn(new ArrayList<>(java.util.List.of(UUID.randomUUID(), UUID.randomUUID())));

            String result = hook.onRequest(player, "hunt_myhunt_found");

            assertThat(result).isEqualTo("2");
        }

        @Test
        void huntTotal_returnsHeadCount() {
            HBHunt hunt = mock(HBHunt.class);
            when(hunt.getHeadCount()).thenReturn(7);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            String result = hook.onRequest(player, "hunt_myhunt_total");

            assertThat(result).isEqualTo("7");
        }

        @Test
        void huntLeft_returnsRemaining() throws InternalException {
            UUID playerUuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);

            HBHunt hunt = mock(HBHunt.class);
            when(hunt.getHeadCount()).thenReturn(5);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);
            when(storageService.getHeadsPlayerForHunt(playerUuid, "myhunt"))
                    .thenReturn(new ArrayList<>(java.util.List.of(UUID.randomUUID(), UUID.randomUUID())));

            String result = hook.onRequest(player, "hunt_myhunt_left");

            assertThat(result).isEqualTo("3");
        }

        @Test
        void huntName_returnsDisplayName() {
            HBHunt hunt = mock(HBHunt.class);
            when(hunt.getDisplayName()).thenReturn("My Cool Hunt");
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            String result = hook.onRequest(player, "hunt_myhunt_name");

            assertThat(result).isEqualTo("My Cool Hunt");
        }

        @Test
        void huntState_returnsLocalizedState() {
            HBHunt hunt = mock(HBHunt.class);
            HuntState state = mock(HuntState.class);
            when(hunt.getState()).thenReturn(state);
            when(state.getLocalizedName(languageService)).thenReturn("Active");
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            String result = hook.onRequest(player, "hunt_myhunt_state");

            assertThat(result).isEqualTo("Active");
        }

        @Test
        void huntBestTime_noBestTime_returnsDash() throws InternalException {
            UUID playerUuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);

            HBHunt hunt = mock(HBHunt.class);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);
            when(storageService.getBestTime(playerUuid, "myhunt")).thenReturn(null);

            String result = hook.onRequest(player, "hunt_myhunt_besttime");

            assertThat(result).isEqualTo("-");
        }

        @Test
        void huntBestTime_withTime_returnsFormatted() throws InternalException {
            UUID playerUuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);

            HBHunt hunt = mock(HBHunt.class);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);
            when(storageService.getBestTime(playerUuid, "myhunt")).thenReturn(65000L);

            String result = hook.onRequest(player, "hunt_myhunt_besttime");

            assertThat(result).isNotEqualTo("-");
        }

        @Test
        void huntTimedCount_returnsCount() throws InternalException {
            UUID playerUuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);

            HBHunt hunt = mock(HBHunt.class);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);
            when(storageService.getTimedRunCount(playerUuid, "myhunt")).thenReturn(3);

            String result = hook.onRequest(player, "hunt_myhunt_timedcount");

            assertThat(result).isEqualTo("3");
        }

        @Test
        void huntProgress_returnsProgressBar() throws InternalException {
            UUID playerUuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);

            HBHunt hunt = mock(HBHunt.class);
            when(hunt.getHeadCount()).thenReturn(5);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);
            when(storageService.getHeadsPlayerForHunt(playerUuid, "myhunt"))
                    .thenReturn(new ArrayList<>(java.util.List.of(UUID.randomUUID(), UUID.randomUUID())));
            when(configService.progressBarBars()).thenReturn(10);
            when(configService.progressBarSymbol()).thenReturn("|");
            when(configService.progressBarCompletedColor()).thenReturn("&a");
            when(configService.progressBarNotCompletedColor()).thenReturn("&c");

            try (MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class)) {
                mu.when(() -> MessageUtils.createProgressBar(2, 5, 10, "|", "&a", "&c"))
                        .thenReturn("[||||------]");

                String result = hook.onRequest(player, "hunt_myhunt_progress");

                assertThat(result).isEqualTo("[||||------]");
            }
        }

        @Test
        void huntTimePosition_found_returnsPosition() throws InternalException {
            UUID playerUuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);

            HBHunt hunt = mock(HBHunt.class);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            LinkedHashMap<PlayerProfileLight, Long> lb = new LinkedHashMap<>();
            lb.put(new PlayerProfileLight(UUID.randomUUID(), "First", ""), 1000L);
            lb.put(new PlayerProfileLight(playerUuid, "Me", ""), 2000L);
            when(storageService.getTimedLeaderboard("myhunt", 50)).thenReturn(lb);

            String result = hook.onRequest(player, "hunt_myhunt_timeposition");

            assertThat(result).isEqualTo("2");
        }

        @Test
        void huntTimePosition_notFound_returnsDash() throws InternalException {
            HBHunt hunt = mock(HBHunt.class);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);
            when(storageService.getTimedLeaderboard("myhunt", 50)).thenReturn(new LinkedHashMap<>());

            String result = hook.onRequest(player, "hunt_myhunt_timeposition");

            assertThat(result).isEqualTo("-");
        }

        @Test
        void huntTooFewParts_returnsEmpty() {
            String result = hook.onRequest(player, "hunt_myhunt");

            assertThat(result).isEqualTo("");
        }

        @Test
        void unknownSubType_returnsEmpty() {
            HBHunt hunt = mock(HBHunt.class);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            String result = hook.onRequest(player, "hunt_myhunt_unknownfield");

            assertThat(result).isEqualTo("");
        }

        @Test
        void huntTimetop_name_returnsPlayerName() throws InternalException {
            HBHunt hunt = mock(HBHunt.class);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            LinkedHashMap<PlayerProfileLight, Long> lb = new LinkedHashMap<>();
            lb.put(new PlayerProfileLight(UUID.randomUUID(), "FastPlayer", ""), 5000L);
            when(storageService.getTimedLeaderboard("myhunt", 1)).thenReturn(lb);

            String result = hook.onRequest(player, "hunt_myhunt_timetop_1_name");

            assertThat(result).isEqualTo("FastPlayer");
        }

        @Test
        void huntTimetop_time_returnsFormattedTime() throws InternalException {
            HBHunt hunt = mock(HBHunt.class);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            LinkedHashMap<PlayerProfileLight, Long> lb = new LinkedHashMap<>();
            lb.put(new PlayerProfileLight(UUID.randomUUID(), "FastPlayer", ""), 65000L);
            when(storageService.getTimedLeaderboard("myhunt", 1)).thenReturn(lb);

            String result = hook.onRequest(player, "hunt_myhunt_timetop_1_time");

            assertThat(result).isNotEqualTo("-");
        }

        @Test
        void huntTimetop_positionTooHigh_returnsDash() throws InternalException {
            HBHunt hunt = mock(HBHunt.class);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            LinkedHashMap<PlayerProfileLight, Long> lb = new LinkedHashMap<>();
            lb.put(new PlayerProfileLight(UUID.randomUUID(), "Player1", ""), 1000L);
            when(storageService.getTimedLeaderboard("myhunt", 5)).thenReturn(lb);

            String result = hook.onRequest(player, "hunt_myhunt_timetop_5_name");

            assertThat(result).isEqualTo("-");
        }

        @Test
        void huntTimetop_unknownField_returnsDash() throws InternalException {
            HBHunt hunt = mock(HBHunt.class);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            LinkedHashMap<PlayerProfileLight, Long> lb = new LinkedHashMap<>();
            lb.put(new PlayerProfileLight(UUID.randomUUID(), "Player1", ""), 1000L);
            when(storageService.getTimedLeaderboard("myhunt", 1)).thenReturn(lb);

            String result = hook.onRequest(player, "hunt_myhunt_timetop_1_unknown");

            assertThat(result).isEqualTo("-");
        }

        @Test
        void huntTimetop_tooFewParts_returnsEmpty() {
            HBHunt hunt = mock(HBHunt.class);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);

            String result = hook.onRequest(player, "hunt_myhunt_timetop_1");

            assertThat(result).isEqualTo("");
        }

        @Test
        void huntFound_storageError_returnsZero() throws InternalException {
            UUID playerUuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);

            HBHunt hunt = mock(HBHunt.class);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);
            when(storageService.getHeadsPlayerForHunt(playerUuid, "myhunt"))
                    .thenThrow(new InternalException("db error"));

            String result = hook.onRequest(player, "hunt_myhunt_found");

            assertThat(result).isEqualTo("0");
        }

        @Test
        void huntLeft_storageError_returnsTotalCount() throws InternalException {
            UUID playerUuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);

            HBHunt hunt = mock(HBHunt.class);
            when(hunt.getHeadCount()).thenReturn(5);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);
            when(storageService.getHeadsPlayerForHunt(playerUuid, "myhunt"))
                    .thenThrow(new InternalException("db error"));

            String result = hook.onRequest(player, "hunt_myhunt_left");

            assertThat(result).isEqualTo("5");
        }

        @Test
        void huntBestTime_storageError_returnsDash() throws InternalException {
            UUID playerUuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);

            HBHunt hunt = mock(HBHunt.class);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);
            when(storageService.getBestTime(playerUuid, "myhunt"))
                    .thenThrow(new InternalException("db error"));

            String result = hook.onRequest(player, "hunt_myhunt_besttime");

            assertThat(result).isEqualTo("-");
        }

        @Test
        void huntTimedCount_storageError_returnsZero() throws InternalException {
            UUID playerUuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);

            HBHunt hunt = mock(HBHunt.class);
            when(huntService.getHuntById("myhunt")).thenReturn(hunt);
            when(storageService.getTimedRunCount(playerUuid, "myhunt"))
                    .thenThrow(new InternalException("db error"));

            String result = hook.onRequest(player, "hunt_myhunt_timedcount");

            assertThat(result).isEqualTo("0");
        }
    }

    // --- leaderboard ---

    @Nested
    class Leaderboard {
        @Test
        void leaderboard_position_returnsRank() throws InternalException {
            UUID playerUuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);

            LinkedHashMap<PlayerProfileLight, Integer> top = new LinkedHashMap<>();
            top.put(new PlayerProfileLight(UUID.randomUUID(), "Top1", ""), 10);
            top.put(new PlayerProfileLight(playerUuid, "Me", ""), 5);
            when(storageService.getTopPlayers()).thenReturn(top);

            String result = hook.onRequest(player, "leaderboard_position");

            assertThat(result).isEqualTo("2");
        }

        @Test
        void leaderboard_position_notInTop_returnsDash() throws InternalException {
            UUID playerUuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);

            LinkedHashMap<PlayerProfileLight, Integer> top = new LinkedHashMap<>();
            top.put(new PlayerProfileLight(UUID.randomUUID(), "Top1", ""), 10);
            when(storageService.getTopPlayers()).thenReturn(top);

            String result = hook.onRequest(player, "leaderboard_position");

            assertThat(result).isEqualTo("-");
        }

        @Test
        void leaderboard_name_returnsPlayerName() throws InternalException {
            LinkedHashMap<PlayerProfileLight, Integer> top = new LinkedHashMap<>();
            top.put(new PlayerProfileLight(UUID.randomUUID(), "TopPlayer", ""), 10);
            when(storageService.getTopPlayers()).thenReturn(top);

            String result = hook.onRequest(player, "leaderboard_1_name");

            assertThat(result).isEqualTo("TopPlayer");
        }

        @Test
        void leaderboard_value_returnsScore() throws InternalException {
            LinkedHashMap<PlayerProfileLight, Integer> top = new LinkedHashMap<>();
            top.put(new PlayerProfileLight(UUID.randomUUID(), "TopPlayer", ""), 42);
            when(storageService.getTopPlayers()).thenReturn(top);

            String result = hook.onRequest(player, "leaderboard_1_value");

            assertThat(result).isEqualTo("42");
        }

        @Test
        void leaderboard_custom_withDisplay_returnsCustom() throws InternalException {
            LinkedHashMap<PlayerProfileLight, Integer> top = new LinkedHashMap<>();
            top.put(new PlayerProfileLight(UUID.randomUUID(), "TopPlayer", "CustomName"), 10);
            when(storageService.getTopPlayers()).thenReturn(top);

            String result = hook.onRequest(player, "leaderboard_1_custom");

            assertThat(result).isEqualTo("CustomName");
        }

        @Test
        void leaderboard_custom_noDisplay_returnsName() throws InternalException {
            LinkedHashMap<PlayerProfileLight, Integer> top = new LinkedHashMap<>();
            top.put(new PlayerProfileLight(UUID.randomUUID(), "TopPlayer", ""), 10);
            when(storageService.getTopPlayers()).thenReturn(top);

            String result = hook.onRequest(player, "leaderboard_1_custom");

            assertThat(result).isEqualTo("TopPlayer");
        }

        @Test
        void leaderboard_positionTooHigh_returnsDash() throws InternalException {
            LinkedHashMap<PlayerProfileLight, Integer> top = new LinkedHashMap<>();
            top.put(new PlayerProfileLight(UUID.randomUUID(), "TopPlayer", ""), 10);
            when(storageService.getTopPlayers()).thenReturn(top);

            String result = hook.onRequest(player, "leaderboard_5_name");

            assertThat(result).isEqualTo("-");
        }
    }

    // --- order ---

    @Nested
    class Order {
        @Test
        void order_malformed_returnsNotFound() {
            String result = hook.onRequest(player, "order_a_b");

            assertThat(result).isEqualTo("Placeholder not found!");
        }

        @SuppressWarnings("unchecked")
        @Test
        void order_current_returnsLastFoundHead() {
            UUID playerUuid = UUID.randomUUID();
            UUID h1Uuid = UUID.randomUUID();
            UUID h2Uuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);

            HeadLocation h1 = mock(HeadLocation.class);
            when(h1.getOrderIndex()).thenReturn(1);
            when(h1.getUuid()).thenReturn(h1Uuid);

            HeadLocation h2 = mock(HeadLocation.class);
            when(h2.getOrderIndex()).thenReturn(2);
            when(h2.getUuid()).thenReturn(h2Uuid);
            when(h2.getNameOrUuid()).thenReturn("head2");

            when(headService.getChargedHeadLocations()).thenReturn(new ArrayList<>(java.util.List.of(h1, h2)));

            BukkitFutureResult<Set<UUID>> futureResult = mock(BukkitFutureResult.class);
            CompletableFuture<Set<UUID>> cf = CompletableFuture.completedFuture(Set.of(h1Uuid, h2Uuid));
            when(futureResult.asFuture()).thenReturn(cf);
            when(storageService.getHeadsPlayer(playerUuid)).thenReturn(futureResult);

            String result = hook.onRequest(player, "order_current");

            assertThat(result).isEqualTo("head2");
        }

        @SuppressWarnings("unchecked")
        @Test
        void order_previous_returnsPreviousFoundHead() {
            UUID playerUuid = UUID.randomUUID();
            UUID h1Uuid = UUID.randomUUID();
            UUID h2Uuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);

            HeadLocation h1 = mock(HeadLocation.class);
            when(h1.getOrderIndex()).thenReturn(1);
            when(h1.getUuid()).thenReturn(h1Uuid);
            when(h1.getNameOrUuid()).thenReturn("head1");

            HeadLocation h2 = mock(HeadLocation.class);
            when(h2.getOrderIndex()).thenReturn(2);
            when(h2.getUuid()).thenReturn(h2Uuid);

            when(headService.getChargedHeadLocations()).thenReturn(new ArrayList<>(java.util.List.of(h1, h2)));

            BukkitFutureResult<Set<UUID>> futureResult = mock(BukkitFutureResult.class);
            CompletableFuture<Set<UUID>> cf = CompletableFuture.completedFuture(Set.of(h1Uuid, h2Uuid));
            when(futureResult.asFuture()).thenReturn(cf);
            when(storageService.getHeadsPlayer(playerUuid)).thenReturn(futureResult);

            String result = hook.onRequest(player, "order_previous");

            assertThat(result).isEqualTo("head1");
        }

        @SuppressWarnings("unchecked")
        @Test
        void order_next_returnsNextHead() {
            UUID playerUuid = UUID.randomUUID();
            UUID h1Uuid = UUID.randomUUID();
            UUID h2Uuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);

            HeadLocation h1 = mock(HeadLocation.class);
            when(h1.getOrderIndex()).thenReturn(1);
            when(h1.getUuid()).thenReturn(h1Uuid);

            HeadLocation h2 = mock(HeadLocation.class);
            when(h2.getOrderIndex()).thenReturn(2);
            lenient().when(h2.getUuid()).thenReturn(h2Uuid);
            when(h2.getNameOrUuid()).thenReturn("head2");

            when(headService.getChargedHeadLocations()).thenReturn(new ArrayList<>(java.util.List.of(h1, h2)));

            BukkitFutureResult<Set<UUID>> futureResult = mock(BukkitFutureResult.class);
            CompletableFuture<Set<UUID>> cf = CompletableFuture.completedFuture(Set.of(h1Uuid));
            when(futureResult.asFuture()).thenReturn(cf);
            when(storageService.getHeadsPlayer(playerUuid)).thenReturn(futureResult);

            String result = hook.onRequest(player, "order_next");

            assertThat(result).isEqualTo("head2");
        }

        @SuppressWarnings("unchecked")
        @Test
        void order_next_allFound_returnsDash() {
            UUID playerUuid = UUID.randomUUID();
            UUID h1Uuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);

            HeadLocation h1 = mock(HeadLocation.class);
            lenient().when(h1.getOrderIndex()).thenReturn(1);
            when(h1.getUuid()).thenReturn(h1Uuid);

            when(headService.getChargedHeadLocations()).thenReturn(new ArrayList<>(java.util.List.of(h1)));

            BukkitFutureResult<Set<UUID>> futureResult = mock(BukkitFutureResult.class);
            CompletableFuture<Set<UUID>> cf = CompletableFuture.completedFuture(Set.of(h1Uuid));
            when(futureResult.asFuture()).thenReturn(cf);
            when(storageService.getHeadsPlayer(playerUuid)).thenReturn(futureResult);

            String result = hook.onRequest(player, "order_next");

            assertThat(result).isEqualTo("-");
        }

        @SuppressWarnings("unchecked")
        @Test
        void order_current_noHeadsFound_returnsDash() {
            UUID playerUuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);

            HeadLocation h1 = mock(HeadLocation.class);
            lenient().when(h1.getOrderIndex()).thenReturn(1);
            lenient().when(h1.getUuid()).thenReturn(UUID.randomUUID());

            when(headService.getChargedHeadLocations()).thenReturn(new ArrayList<>(java.util.List.of(h1)));

            BukkitFutureResult<Set<UUID>> futureResult = mock(BukkitFutureResult.class);
            CompletableFuture<Set<UUID>> cf = CompletableFuture.completedFuture(Collections.emptySet());
            when(futureResult.asFuture()).thenReturn(cf);
            when(storageService.getHeadsPlayer(playerUuid)).thenReturn(futureResult);

            String result = hook.onRequest(player, "order_current");

            assertThat(result).isEqualTo("-");
        }

        @Test
        void order_noChargedHeads_returnsNoLoadedHeads() {
            when(headService.getChargedHeadLocations()).thenReturn(new ArrayList<>());

            String result = hook.onRequest(player, "order_current");

            assertThat(result).isEqualTo("No loaded heads");
        }

        @SuppressWarnings("unchecked")
        @Test
        void order_previous_onlyOneFound_returnsDash() {
            UUID playerUuid = UUID.randomUUID();
            UUID h1Uuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);

            HeadLocation h1 = mock(HeadLocation.class);
            lenient().when(h1.getOrderIndex()).thenReturn(1);
            when(h1.getUuid()).thenReturn(h1Uuid);

            when(headService.getChargedHeadLocations()).thenReturn(new ArrayList<>(java.util.List.of(h1)));

            BukkitFutureResult<Set<UUID>> futureResult = mock(BukkitFutureResult.class);
            CompletableFuture<Set<UUID>> cf = CompletableFuture.completedFuture(Set.of(h1Uuid));
            when(futureResult.asFuture()).thenReturn(cf);
            when(storageService.getHeadsPlayer(playerUuid)).thenReturn(futureResult);

            String result = hook.onRequest(player, "order_previous");

            assertThat(result).isEqualTo("-");
        }
    }

    // --- Unknown identifier ---

    @Test
    void unknownIdentifier_returnsNull() {
        String result = hook.onRequest(player, "nonexistent_placeholder");

        assertThat(result).isNull();
    }
}
