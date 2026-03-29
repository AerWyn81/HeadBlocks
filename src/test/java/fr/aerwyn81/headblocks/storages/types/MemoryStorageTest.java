package fr.aerwyn81.headblocks.storages.types;

import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class MemoryStorageTest {

    private Memory storage;

    @BeforeEach
    void setUp() {
        storage = new Memory();
        storage.init();
    }

    // ---- Lifecycle ----

    @Test
    void init_initializesAllMaps() throws InternalException {
        assertThat(storage.containsPlayer(UUID.randomUUID())).isFalse();
        assertThat(storage.getCachedPlayerHeads(UUID.randomUUID())).isNull();
        assertThat(storage.getCachedTopPlayers()).isEmpty();
        assertThat(storage.getCachedHeads()).isEmpty();
    }

    @Test
    void close_cleansEverything() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();
        storage.addHead(player, head);
        storage.addCachedHead(head);

        storage.close();

        // After close, init again to verify state was reset
        storage.init();
        assertThat(storage.containsPlayer(player)).isFalse();
    }

    // ---- Player-head core ----

    @Test
    void addHead_newPlayer_createsEntryAndStoresHead() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();

        storage.addHead(player, head);

        assertThat(storage.containsPlayer(player)).isTrue();
        assertThat(storage.hasHead(player, head)).isTrue();
    }

    @Test
    void addHead_existingPlayer_addsToList() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head1 = UUID.randomUUID();
        UUID head2 = UUID.randomUUID();

        storage.addHead(player, head1);
        storage.addHead(player, head2);

        assertThat(storage.getHeadsPlayer(player)).containsExactly(head1, head2);
    }

    @Test
    void hasHead_playerWithoutHead_returnsFalse() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head1 = UUID.randomUUID();
        UUID head2 = UUID.randomUUID();

        storage.addHead(player, head1);

        assertThat(storage.hasHead(player, head2)).isFalse();
    }

    @Test
    void hasHead_unknownPlayer_returnsFalse() throws InternalException {
        assertThat(storage.hasHead(UUID.randomUUID(), UUID.randomUUID())).isFalse();
    }

    @Test
    void containsPlayer_unknownPlayer_returnsFalse() throws InternalException {
        assertThat(storage.containsPlayer(UUID.randomUUID())).isFalse();
    }

    @Test
    void resetPlayer_removesEntirePlayerEntry() throws InternalException {
        UUID player = UUID.randomUUID();
        storage.addHead(player, UUID.randomUUID());

        storage.resetPlayer(player);

        assertThat(storage.containsPlayer(player)).isFalse();
    }

    @Test
    void resetPlayerHead_removesSpecificHead() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head1 = UUID.randomUUID();
        UUID head2 = UUID.randomUUID();
        storage.addHead(player, head1);
        storage.addHead(player, head2);

        storage.resetPlayerHead(player, head1);

        assertThat(storage.hasHead(player, head1)).isFalse();
        assertThat(storage.hasHead(player, head2)).isTrue();
    }

    @Test
    void removeHead_removesFromAllPlayers() throws InternalException {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID head = UUID.randomUUID();
        storage.addHead(p1, head);
        storage.addHead(p2, head);

        storage.removeHead(head);

        assertThat(storage.hasHead(p1, head)).isFalse();
        assertThat(storage.hasHead(p2, head)).isFalse();
    }

    @Test
    void getHeadsPlayer_unknownPlayer_returnsEmptyList() throws InternalException {
        assertThat(storage.getHeadsPlayer(UUID.randomUUID())).isEmpty();
    }

    // ---- Cache: playerHeads ----

    @Test
    void cachedPlayerHeads_setAndGet() throws InternalException {
        UUID player = UUID.randomUUID();
        Set<UUID> heads = Set.of(UUID.randomUUID(), UUID.randomUUID());

        storage.setCachedPlayerHeads(player, heads);

        assertThat(storage.getCachedPlayerHeads(player)).isEqualTo(heads);
    }

    @Test
    void addCachedPlayerHead_createsSetIfAbsent() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();

        storage.addCachedPlayerHead(player, head);

        assertThat(storage.getCachedPlayerHeads(player)).contains(head);
    }

    @Test
    void removeCachedPlayerHeads_removesEntry() throws InternalException {
        UUID player = UUID.randomUUID();
        storage.setCachedPlayerHeads(player, Set.of(UUID.randomUUID()));

        storage.removeCachedPlayerHeads(player);

        assertThat(storage.getCachedPlayerHeads(player)).isNull();
    }

    // ---- Cache: topPlayers ----

    @Test
    void cachedTopPlayers_setAndGet() throws InternalException {
        LinkedHashMap<PlayerProfileLight, Integer> top = new LinkedHashMap<>();
        top.put(new PlayerProfileLight(UUID.randomUUID(), "Steve", ""), 10);

        storage.setCachedTopPlayers(top);

        assertThat(storage.getCachedTopPlayers()).hasSize(1);
    }

    @Test
    void clearCachedTopPlayers_emptiesMap() throws InternalException {
        LinkedHashMap<PlayerProfileLight, Integer> top = new LinkedHashMap<>();
        top.put(new PlayerProfileLight(UUID.randomUUID(), "A", ""), 5);
        storage.setCachedTopPlayers(top);

        storage.clearCachedTopPlayers();

        assertThat(storage.getCachedTopPlayers()).isEmpty();
    }

    // ---- Cache: heads ----

    @Test
    void cachedHeads_addAndGet() throws InternalException {
        UUID head = UUID.randomUUID();
        storage.addCachedHead(head);

        assertThat(storage.getCachedHeads()).contains(head);
    }

    @Test
    void removeCachedHead_removesAndClearsRelatedCaches() throws InternalException {
        UUID head = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        storage.addCachedHead(head);
        storage.addCachedPlayerHead(player, head);
        LinkedHashMap<PlayerProfileLight, Integer> top = new LinkedHashMap<>();
        top.put(new PlayerProfileLight(player, "X", ""), 1);
        storage.setCachedTopPlayers(top);

        storage.removeCachedHead(head);

        assertThat(storage.getCachedHeads()).doesNotContain(head);
        Set<UUID> playerHeads = storage.getCachedPlayerHeads(player);
        if (playerHeads != null) {
            assertThat(playerHeads).doesNotContain(head);
        }
        assertThat(storage.getCachedTopPlayers()).isEmpty();
    }

    // ---- Cache: hunt playerHeads ----

    @Test
    void huntPlayerHeads_setAndGet() throws InternalException {
        UUID player = UUID.randomUUID();
        Set<UUID> heads = Set.of(UUID.randomUUID());

        storage.setCachedPlayerHeadsForHunt(player, "hunt1", heads);

        assertThat(storage.getCachedPlayerHeadsForHunt(player, "hunt1")).isEqualTo(heads);
    }

    @Test
    void huntPlayerHeads_getForUnknownHunt_returnsNull() throws InternalException {
        assertThat(storage.getCachedPlayerHeadsForHunt(UUID.randomUUID(), "unknown")).isNull();
    }

    @Test
    void addCachedPlayerHeadForHunt_createsNestedMaps() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();

        storage.addCachedPlayerHeadForHunt(player, "hunt1", head);

        assertThat(storage.getCachedPlayerHeadsForHunt(player, "hunt1")).contains(head);
    }

    @Test
    void removeCachedPlayerHeadsForHunt_removesPlayerEntry() throws InternalException {
        UUID player = UUID.randomUUID();
        storage.setCachedPlayerHeadsForHunt(player, "hunt1", Set.of(UUID.randomUUID()));

        storage.removeCachedPlayerHeadsForHunt(player, "hunt1");

        assertThat(storage.getCachedPlayerHeadsForHunt(player, "hunt1")).isNull();
    }

    @Test
    void clearCachedPlayerHeadsForHunt_removesEntireHunt() throws InternalException {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        storage.setCachedPlayerHeadsForHunt(p1, "hunt1", Set.of(UUID.randomUUID()));
        storage.setCachedPlayerHeadsForHunt(p2, "hunt1", Set.of(UUID.randomUUID()));

        storage.clearCachedPlayerHeadsForHunt("hunt1");

        assertThat(storage.getCachedPlayerHeadsForHunt(p1, "hunt1")).isNull();
        assertThat(storage.getCachedPlayerHeadsForHunt(p2, "hunt1")).isNull();
    }

    @Test
    void clearAllCachedHuntDataForPlayer_removesPlayerFromAllHunts() throws InternalException {
        UUID player = UUID.randomUUID();
        storage.setCachedPlayerHeadsForHunt(player, "hunt1", Set.of(UUID.randomUUID()));
        storage.setCachedPlayerHeadsForHunt(player, "hunt2", Set.of(UUID.randomUUID()));
        storage.setCachedBestTime(player, "hunt1", 5000L);
        storage.setCachedTimedRunCount(player, "hunt1", 3);

        storage.clearAllCachedHuntDataForPlayer(player);

        assertThat(storage.getCachedPlayerHeadsForHunt(player, "hunt1")).isNull();
        assertThat(storage.getCachedPlayerHeadsForHunt(player, "hunt2")).isNull();
        assertThat(storage.getCachedBestTime(player, "hunt1")).isNull();
        assertThat(storage.getCachedTimedRunCount(player, "hunt1")).isNull();
    }

    // ---- Cache: hunt topPlayers ----

    @Test
    void huntTopPlayers_setAndGet() throws InternalException {
        LinkedHashMap<PlayerProfileLight, Integer> top = new LinkedHashMap<>();
        top.put(new PlayerProfileLight(UUID.randomUUID(), "A", ""), 10);

        storage.setCachedTopPlayersForHunt("hunt1", top);

        assertThat(storage.getCachedTopPlayersForHunt("hunt1")).hasSize(1);
    }

    @Test
    void huntTopPlayers_getForUnknown_returnsNull() throws InternalException {
        assertThat(storage.getCachedTopPlayersForHunt("unknown")).isNull();
    }

    @Test
    void clearCachedTopPlayersForHunt_removesSpecificHunt() throws InternalException {
        LinkedHashMap<PlayerProfileLight, Integer> top = new LinkedHashMap<>();
        top.put(new PlayerProfileLight(UUID.randomUUID(), "A", ""), 5);
        storage.setCachedTopPlayersForHunt("hunt1", top);
        storage.setCachedTopPlayersForHunt("hunt2", top);

        storage.clearCachedTopPlayersForHunt("hunt1");

        assertThat(storage.getCachedTopPlayersForHunt("hunt1")).isNull();
        assertThat(storage.getCachedTopPlayersForHunt("hunt2")).isNotNull();
    }

    @Test
    void clearAllCachedTopPlayersForHunt_removesAll() throws InternalException {
        LinkedHashMap<PlayerProfileLight, Integer> top = new LinkedHashMap<>();
        top.put(new PlayerProfileLight(UUID.randomUUID(), "A", ""), 1);
        storage.setCachedTopPlayersForHunt("h1", top);
        storage.setCachedTopPlayersForHunt("h2", top);

        storage.clearAllCachedTopPlayersForHunt();

        assertThat(storage.getCachedTopPlayersForHunt("h1")).isNull();
        assertThat(storage.getCachedTopPlayersForHunt("h2")).isNull();
    }

    // ---- Cache: timed leaderboard ----

    @Test
    void timedLeaderboard_setAndGet() throws InternalException {
        LinkedHashMap<PlayerProfileLight, Long> lb = new LinkedHashMap<>();
        lb.put(new PlayerProfileLight(UUID.randomUUID(), "Fast", ""), 1234L);

        storage.setCachedTimedLeaderboard("hunt1", lb);

        assertThat(storage.getCachedTimedLeaderboard("hunt1")).hasSize(1);
    }

    @Test
    void clearCachedTimedLeaderboard_removesEntry() throws InternalException {
        LinkedHashMap<PlayerProfileLight, Long> lb = new LinkedHashMap<>();
        lb.put(new PlayerProfileLight(UUID.randomUUID(), "A", ""), 100L);
        storage.setCachedTimedLeaderboard("hunt1", lb);

        storage.clearCachedTimedLeaderboard("hunt1");

        assertThat(storage.getCachedTimedLeaderboard("hunt1")).isNull();
    }

    // ---- Cache: bestTime (composite key huntId:uuid) ----

    @Test
    void bestTime_setAndGet() throws InternalException {
        UUID player = UUID.randomUUID();
        storage.setCachedBestTime(player, "hunt1", 5000L);

        assertThat(storage.getCachedBestTime(player, "hunt1")).isEqualTo(5000L);
    }

    @Test
    void bestTime_getForUnknown_returnsNull() throws InternalException {
        assertThat(storage.getCachedBestTime(UUID.randomUUID(), "hunt1")).isNull();
    }

    @Test
    void clearCachedBestTime_removesEntry() throws InternalException {
        UUID player = UUID.randomUUID();
        storage.setCachedBestTime(player, "hunt1", 1000L);

        storage.clearCachedBestTime(player, "hunt1");

        assertThat(storage.getCachedBestTime(player, "hunt1")).isNull();
    }

    // ---- Cache: runCount ----

    @Test
    void runCount_setAndGet() throws InternalException {
        UUID player = UUID.randomUUID();
        storage.setCachedTimedRunCount(player, "hunt1", 7);

        assertThat(storage.getCachedTimedRunCount(player, "hunt1")).isEqualTo(7);
    }

    @Test
    void runCount_getForUnknown_returnsNull() throws InternalException {
        assertThat(storage.getCachedTimedRunCount(UUID.randomUUID(), "hunt1")).isNull();
    }

    @Test
    void clearCachedTimedRunCount_removesEntry() throws InternalException {
        UUID player = UUID.randomUUID();
        storage.setCachedTimedRunCount(player, "hunt1", 5);

        storage.clearCachedTimedRunCount(player, "hunt1");

        assertThat(storage.getCachedTimedRunCount(player, "hunt1")).isNull();
    }

    // ---- Edge cases ----

    @Test
    void resetPlayerHead_unknownPlayer_doesNotThrow() throws InternalException {
        UUID unknownPlayer = UUID.randomUUID();
        UUID someHead = UUID.randomUUID();

        // containsPlayer is false → resetPlayerHead should be a no-op
        storage.resetPlayerHead(unknownPlayer, someHead);

        assertThat(storage.containsPlayer(unknownPlayer)).isFalse();
    }

    @Test
    void removeCachedPlayerHeadsForHunt_unknownHunt_doesNotThrow() throws InternalException {
        // Hunt never cached → should be a no-op
        storage.removeCachedPlayerHeadsForHunt(UUID.randomUUID(), "nonexistent");
    }

    // ---- Hunt version ----

    @Test
    void getHuntVersion_alwaysReturnsZero() throws InternalException {
        assertThat(storage.getHuntVersion()).isEqualTo(0);
    }

    @Test
    void incrementHuntVersion_isNoOp() throws InternalException {
        assertThatNoException().isThrownBy(() -> storage.incrementHuntVersion());
        assertThat(storage.getHuntVersion()).isEqualTo(0);
    }

    // ---- Additional hunt player heads tests ----

    @Test
    void addCachedPlayerHeadForHunt_multipleHeads_accumulatesInSet() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head1 = UUID.randomUUID();
        UUID head2 = UUID.randomUUID();

        storage.addCachedPlayerHeadForHunt(player, "hunt1", head1);
        storage.addCachedPlayerHeadForHunt(player, "hunt1", head2);

        assertThat(storage.getCachedPlayerHeadsForHunt(player, "hunt1"))
                .containsExactlyInAnyOrder(head1, head2);
    }

    @Test
    void addCachedPlayerHeadForHunt_duplicateHead_doesNotDuplicate() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();

        storage.addCachedPlayerHeadForHunt(player, "hunt1", head);
        storage.addCachedPlayerHeadForHunt(player, "hunt1", head);

        assertThat(storage.getCachedPlayerHeadsForHunt(player, "hunt1")).hasSize(1);
    }

    @Test
    void setCachedPlayerHeadsForHunt_overwritesExistingEntry() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head1 = UUID.randomUUID();
        UUID head2 = UUID.randomUUID();

        storage.setCachedPlayerHeadsForHunt(player, "hunt1", Set.of(head1));
        storage.setCachedPlayerHeadsForHunt(player, "hunt1", Set.of(head2));

        assertThat(storage.getCachedPlayerHeadsForHunt(player, "hunt1"))
                .containsExactly(head2);
    }

    @Test
    void getCachedPlayerHeadsForHunt_differentHunts_areIndependent() throws InternalException {
        UUID player = UUID.randomUUID();
        Set<UUID> heads1 = Set.of(UUID.randomUUID());
        Set<UUID> heads2 = Set.of(UUID.randomUUID());

        storage.setCachedPlayerHeadsForHunt(player, "hunt1", heads1);
        storage.setCachedPlayerHeadsForHunt(player, "hunt2", heads2);

        assertThat(storage.getCachedPlayerHeadsForHunt(player, "hunt1")).isEqualTo(heads1);
        assertThat(storage.getCachedPlayerHeadsForHunt(player, "hunt2")).isEqualTo(heads2);
    }

    @Test
    void clearCachedPlayerHeadsForHunt_doesNotAffectOtherHunts() throws InternalException {
        UUID player = UUID.randomUUID();
        storage.setCachedPlayerHeadsForHunt(player, "hunt1", Set.of(UUID.randomUUID()));
        storage.setCachedPlayerHeadsForHunt(player, "hunt2", Set.of(UUID.randomUUID()));

        storage.clearCachedPlayerHeadsForHunt("hunt1");

        assertThat(storage.getCachedPlayerHeadsForHunt(player, "hunt1")).isNull();
        assertThat(storage.getCachedPlayerHeadsForHunt(player, "hunt2")).isNotNull();
    }

    @Test
    void removeCachedPlayerHeadsForHunt_doesNotAffectOtherPlayers() throws InternalException {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        storage.setCachedPlayerHeadsForHunt(p1, "hunt1", Set.of(UUID.randomUUID()));
        storage.setCachedPlayerHeadsForHunt(p2, "hunt1", Set.of(UUID.randomUUID()));

        storage.removeCachedPlayerHeadsForHunt(p1, "hunt1");

        assertThat(storage.getCachedPlayerHeadsForHunt(p1, "hunt1")).isNull();
        assertThat(storage.getCachedPlayerHeadsForHunt(p2, "hunt1")).isNotNull();
    }

    // ---- clearAllCachedHuntDataForPlayer: isolation and composite key matching ----

    @Test
    void clearAllCachedHuntDataForPlayer_doesNotAffectOtherPlayers() throws InternalException {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        storage.setCachedPlayerHeadsForHunt(player1, "hunt1", Set.of(UUID.randomUUID()));
        storage.setCachedPlayerHeadsForHunt(player2, "hunt1", Set.of(UUID.randomUUID()));
        storage.setCachedBestTime(player1, "hunt1", 1000L);
        storage.setCachedBestTime(player2, "hunt1", 2000L);
        storage.setCachedTimedRunCount(player1, "hunt1", 3);
        storage.setCachedTimedRunCount(player2, "hunt1", 5);

        storage.clearAllCachedHuntDataForPlayer(player1);

        assertThat(storage.getCachedPlayerHeadsForHunt(player1, "hunt1")).isNull();
        assertThat(storage.getCachedBestTime(player1, "hunt1")).isNull();
        assertThat(storage.getCachedTimedRunCount(player1, "hunt1")).isNull();

        // player2 data must be intact
        assertThat(storage.getCachedPlayerHeadsForHunt(player2, "hunt1")).isNotNull();
        assertThat(storage.getCachedBestTime(player2, "hunt1")).isEqualTo(2000L);
        assertThat(storage.getCachedTimedRunCount(player2, "hunt1")).isEqualTo(5);
    }

    @Test
    void clearAllCachedHuntDataForPlayer_multipleHunts_clearsAll() throws InternalException {
        UUID player = UUID.randomUUID();
        storage.setCachedPlayerHeadsForHunt(player, "hunt1", Set.of(UUID.randomUUID()));
        storage.setCachedPlayerHeadsForHunt(player, "hunt2", Set.of(UUID.randomUUID()));
        storage.setCachedBestTime(player, "hunt1", 100L);
        storage.setCachedBestTime(player, "hunt2", 200L);
        storage.setCachedTimedRunCount(player, "hunt1", 1);
        storage.setCachedTimedRunCount(player, "hunt2", 2);

        storage.clearAllCachedHuntDataForPlayer(player);

        assertThat(storage.getCachedPlayerHeadsForHunt(player, "hunt1")).isNull();
        assertThat(storage.getCachedPlayerHeadsForHunt(player, "hunt2")).isNull();
        assertThat(storage.getCachedBestTime(player, "hunt1")).isNull();
        assertThat(storage.getCachedBestTime(player, "hunt2")).isNull();
        assertThat(storage.getCachedTimedRunCount(player, "hunt1")).isNull();
        assertThat(storage.getCachedTimedRunCount(player, "hunt2")).isNull();
    }

    @Test
    void clearAllCachedHuntDataForPlayer_withNoData_doesNotThrow() throws InternalException {
        UUID player = UUID.randomUUID();
        assertThatNoException().isThrownBy(() -> storage.clearAllCachedHuntDataForPlayer(player));
    }

    // ---- removeCachedHead cascade behavior ----

    @Test
    void removeCachedHead_cascadeClearsFromMultiplePlayers() throws InternalException {
        UUID head = UUID.randomUUID();
        UUID otherHead = UUID.randomUUID();
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        storage.addCachedHead(head);
        storage.addCachedHead(otherHead);
        storage.addCachedPlayerHead(p1, head);
        storage.addCachedPlayerHead(p1, otherHead);
        storage.addCachedPlayerHead(p2, head);

        storage.removeCachedHead(head);

        assertThat(storage.getCachedHeads()).doesNotContain(head);
        assertThat(storage.getCachedHeads()).contains(otherHead);

        // head removed from both players
        Set<UUID> p1Heads = storage.getCachedPlayerHeads(p1);
        assertThat(p1Heads).doesNotContain(head);
        assertThat(p1Heads).contains(otherHead);

        Set<UUID> p2Heads = storage.getCachedPlayerHeads(p2);
        if (p2Heads != null) {
            assertThat(p2Heads).doesNotContain(head);
        }
    }

    @Test
    void removeCachedHead_clearsGlobalTopPlayersCache() throws InternalException {
        UUID head = UUID.randomUUID();
        storage.addCachedHead(head);

        LinkedHashMap<PlayerProfileLight, Integer> top = new LinkedHashMap<>();
        top.put(new PlayerProfileLight(UUID.randomUUID(), "Alice", ""), 10);
        top.put(new PlayerProfileLight(UUID.randomUUID(), "Bob", ""), 5);
        storage.setCachedTopPlayers(top);

        storage.removeCachedHead(head);

        assertThat(storage.getCachedTopPlayers()).isEmpty();
    }

    @Test
    void removeCachedHead_nonexistentHead_doesNotThrow() throws InternalException {
        // Pre-populate some data to ensure it is not corrupted
        UUID player = UUID.randomUUID();
        UUID existingHead = UUID.randomUUID();
        storage.addCachedHead(existingHead);
        storage.addCachedPlayerHead(player, existingHead);

        assertThatNoException().isThrownBy(() -> storage.removeCachedHead(UUID.randomUUID()));

        assertThat(storage.getCachedHeads()).contains(existingHead);
        assertThat(storage.getCachedPlayerHeads(player)).contains(existingHead);
    }

    // ---- Hunt top players: additional tests ----

    @Test
    void setCachedTopPlayersForHunt_overwritesPreviousData() throws InternalException {
        LinkedHashMap<PlayerProfileLight, Integer> top1 = new LinkedHashMap<>();
        top1.put(new PlayerProfileLight(UUID.randomUUID(), "A", ""), 10);
        storage.setCachedTopPlayersForHunt("hunt1", top1);

        LinkedHashMap<PlayerProfileLight, Integer> top2 = new LinkedHashMap<>();
        top2.put(new PlayerProfileLight(UUID.randomUUID(), "B", ""), 20);
        top2.put(new PlayerProfileLight(UUID.randomUUID(), "C", ""), 15);
        storage.setCachedTopPlayersForHunt("hunt1", top2);

        assertThat(storage.getCachedTopPlayersForHunt("hunt1")).hasSize(2);
    }

    @Test
    void clearAllCachedTopPlayersForHunt_afterClear_getReturnsNull() throws InternalException {
        LinkedHashMap<PlayerProfileLight, Integer> top = new LinkedHashMap<>();
        top.put(new PlayerProfileLight(UUID.randomUUID(), "X", ""), 1);
        storage.setCachedTopPlayersForHunt("h1", top);

        storage.clearAllCachedTopPlayersForHunt();

        assertThat(storage.getCachedTopPlayersForHunt("h1")).isNull();
    }

    // ---- Timed leaderboard: additional tests ----

    @Test
    void timedLeaderboard_getForUnknown_returnsNull() throws InternalException {
        assertThat(storage.getCachedTimedLeaderboard("nonexistent")).isNull();
    }

    @Test
    void timedLeaderboard_setOverwritesPrevious() throws InternalException {
        LinkedHashMap<PlayerProfileLight, Long> lb1 = new LinkedHashMap<>();
        lb1.put(new PlayerProfileLight(UUID.randomUUID(), "A", ""), 100L);
        storage.setCachedTimedLeaderboard("hunt1", lb1);

        LinkedHashMap<PlayerProfileLight, Long> lb2 = new LinkedHashMap<>();
        lb2.put(new PlayerProfileLight(UUID.randomUUID(), "B", ""), 200L);
        lb2.put(new PlayerProfileLight(UUID.randomUUID(), "C", ""), 300L);
        storage.setCachedTimedLeaderboard("hunt1", lb2);

        assertThat(storage.getCachedTimedLeaderboard("hunt1")).hasSize(2);
    }

    @Test
    void timedLeaderboard_clearDoesNotAffectOtherHunts() throws InternalException {
        LinkedHashMap<PlayerProfileLight, Long> lb = new LinkedHashMap<>();
        lb.put(new PlayerProfileLight(UUID.randomUUID(), "A", ""), 100L);
        storage.setCachedTimedLeaderboard("hunt1", lb);
        storage.setCachedTimedLeaderboard("hunt2", lb);

        storage.clearCachedTimedLeaderboard("hunt1");

        assertThat(storage.getCachedTimedLeaderboard("hunt1")).isNull();
        assertThat(storage.getCachedTimedLeaderboard("hunt2")).isNotNull();
    }

    @Test
    void timedLeaderboard_preservesInsertionOrder() throws InternalException {
        LinkedHashMap<PlayerProfileLight, Long> lb = new LinkedHashMap<>();
        PlayerProfileLight first = new PlayerProfileLight(UUID.randomUUID(), "First", "");
        PlayerProfileLight second = new PlayerProfileLight(UUID.randomUUID(), "Second", "");
        PlayerProfileLight third = new PlayerProfileLight(UUID.randomUUID(), "Third", "");
        lb.put(first, 100L);
        lb.put(second, 200L);
        lb.put(third, 300L);

        storage.setCachedTimedLeaderboard("hunt1", lb);

        LinkedHashMap<PlayerProfileLight, Long> result = storage.getCachedTimedLeaderboard("hunt1");
        assertThat(result.keySet()).containsExactly(first, second, third);
    }

    // ---- Best time: composite key isolation ----

    @Test
    void bestTime_differentHuntsSamePlayer_areIndependent() throws InternalException {
        UUID player = UUID.randomUUID();
        storage.setCachedBestTime(player, "hunt1", 1000L);
        storage.setCachedBestTime(player, "hunt2", 2000L);

        assertThat(storage.getCachedBestTime(player, "hunt1")).isEqualTo(1000L);
        assertThat(storage.getCachedBestTime(player, "hunt2")).isEqualTo(2000L);
    }

    @Test
    void bestTime_sameHuntDifferentPlayers_areIndependent() throws InternalException {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        storage.setCachedBestTime(p1, "hunt1", 500L);
        storage.setCachedBestTime(p2, "hunt1", 750L);

        assertThat(storage.getCachedBestTime(p1, "hunt1")).isEqualTo(500L);
        assertThat(storage.getCachedBestTime(p2, "hunt1")).isEqualTo(750L);
    }

    @Test
    void bestTime_setOverwritesPrevious() throws InternalException {
        UUID player = UUID.randomUUID();
        storage.setCachedBestTime(player, "hunt1", 5000L);
        storage.setCachedBestTime(player, "hunt1", 3000L);

        assertThat(storage.getCachedBestTime(player, "hunt1")).isEqualTo(3000L);
    }

    @Test
    void clearCachedBestTime_doesNotAffectOtherEntries() throws InternalException {
        UUID player = UUID.randomUUID();
        storage.setCachedBestTime(player, "hunt1", 1000L);
        storage.setCachedBestTime(player, "hunt2", 2000L);

        storage.clearCachedBestTime(player, "hunt1");

        assertThat(storage.getCachedBestTime(player, "hunt1")).isNull();
        assertThat(storage.getCachedBestTime(player, "hunt2")).isEqualTo(2000L);
    }

    // ---- Timed run count: composite key isolation ----

    @Test
    void runCount_differentHuntsSamePlayer_areIndependent() throws InternalException {
        UUID player = UUID.randomUUID();
        storage.setCachedTimedRunCount(player, "hunt1", 3);
        storage.setCachedTimedRunCount(player, "hunt2", 7);

        assertThat(storage.getCachedTimedRunCount(player, "hunt1")).isEqualTo(3);
        assertThat(storage.getCachedTimedRunCount(player, "hunt2")).isEqualTo(7);
    }

    @Test
    void runCount_sameHuntDifferentPlayers_areIndependent() throws InternalException {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        storage.setCachedTimedRunCount(p1, "hunt1", 2);
        storage.setCachedTimedRunCount(p2, "hunt1", 9);

        assertThat(storage.getCachedTimedRunCount(p1, "hunt1")).isEqualTo(2);
        assertThat(storage.getCachedTimedRunCount(p2, "hunt1")).isEqualTo(9);
    }

    @Test
    void runCount_setOverwritesPrevious() throws InternalException {
        UUID player = UUID.randomUUID();
        storage.setCachedTimedRunCount(player, "hunt1", 5);
        storage.setCachedTimedRunCount(player, "hunt1", 10);

        assertThat(storage.getCachedTimedRunCount(player, "hunt1")).isEqualTo(10);
    }

    @Test
    void clearCachedTimedRunCount_doesNotAffectOtherEntries() throws InternalException {
        UUID player = UUID.randomUUID();
        storage.setCachedTimedRunCount(player, "hunt1", 3);
        storage.setCachedTimedRunCount(player, "hunt2", 8);

        storage.clearCachedTimedRunCount(player, "hunt1");

        assertThat(storage.getCachedTimedRunCount(player, "hunt1")).isNull();
        assertThat(storage.getCachedTimedRunCount(player, "hunt2")).isEqualTo(8);
    }

    // ---- Hunt version: repeated increments ----

    @Test
    void incrementHuntVersion_multipleIncrements_stillReturnsZero() throws InternalException {
        storage.incrementHuntVersion();
        storage.incrementHuntVersion();
        storage.incrementHuntVersion();

        assertThat(storage.getHuntVersion()).isEqualTo(0);
    }

    // ---- close: verifies hunt caches are cleared ----

    @Test
    void close_clearsHuntSpecificCaches() throws InternalException {
        UUID player = UUID.randomUUID();
        storage.setCachedPlayerHeadsForHunt(player, "hunt1", Set.of(UUID.randomUUID()));
        storage.setCachedTopPlayersForHunt("hunt1", new LinkedHashMap<>());
        LinkedHashMap<PlayerProfileLight, Long> lb = new LinkedHashMap<>();
        lb.put(new PlayerProfileLight(UUID.randomUUID(), "A", ""), 100L);
        storage.setCachedTimedLeaderboard("hunt1", lb);
        storage.setCachedBestTime(player, "hunt1", 5000L);
        storage.setCachedTimedRunCount(player, "hunt1", 3);

        storage.close();
        storage.init();

        assertThat(storage.getCachedPlayerHeadsForHunt(player, "hunt1")).isNull();
        assertThat(storage.getCachedTopPlayersForHunt("hunt1")).isNull();
        assertThat(storage.getCachedTimedLeaderboard("hunt1")).isNull();
        assertThat(storage.getCachedBestTime(player, "hunt1")).isNull();
        assertThat(storage.getCachedTimedRunCount(player, "hunt1")).isNull();
    }

    // ---- Edge cases: operations on empty / null hunt maps ----

    @Test
    void clearCachedPlayerHeadsForHunt_neverPopulatedHunt_doesNotThrow() throws InternalException {
        assertThatNoException().isThrownBy(() -> storage.clearCachedPlayerHeadsForHunt("ghost"));
    }

    @Test
    void clearCachedTopPlayersForHunt_neverPopulatedHunt_doesNotThrow() throws InternalException {
        assertThatNoException().isThrownBy(() -> storage.clearCachedTopPlayersForHunt("ghost"));
    }

    @Test
    void clearCachedTimedLeaderboard_neverPopulatedHunt_doesNotThrow() throws InternalException {
        assertThatNoException().isThrownBy(() -> storage.clearCachedTimedLeaderboard("ghost"));
    }

    @Test
    void addCachedPlayerHeadForHunt_thenGetForDifferentPlayer_returnsNull() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID otherPlayer = UUID.randomUUID();
        UUID head = UUID.randomUUID();

        storage.addCachedPlayerHeadForHunt(player, "hunt1", head);

        assertThat(storage.getCachedPlayerHeadsForHunt(otherPlayer, "hunt1")).isNull();
    }

    @Test
    void removeCachedPlayerHeadsForHunt_playerNotInHunt_doesNotThrow() throws InternalException {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        storage.setCachedPlayerHeadsForHunt(p1, "hunt1", Set.of(UUID.randomUUID()));

        // p2 was never added to hunt1
        assertThatNoException().isThrownBy(() -> storage.removeCachedPlayerHeadsForHunt(p2, "hunt1"));
        // p1 data must still be intact
        assertThat(storage.getCachedPlayerHeadsForHunt(p1, "hunt1")).isNotNull();
    }

    @Test
    void cachedPlayerHeads_addToExistingSet_accumulates() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head1 = UUID.randomUUID();
        UUID head2 = UUID.randomUUID();

        storage.setCachedPlayerHeads(player, ConcurrentHashMap.newKeySet());
        storage.addCachedPlayerHead(player, head1);
        storage.addCachedPlayerHead(player, head2);

        assertThat(storage.getCachedPlayerHeads(player)).containsExactlyInAnyOrder(head1, head2);
    }

    @Test
    void clearAllCachedHuntDataForPlayer_doesNotAffectTimedLeaderboard() throws InternalException {
        UUID player = UUID.randomUUID();
        storage.setCachedBestTime(player, "hunt1", 1000L);

        LinkedHashMap<PlayerProfileLight, Long> lb = new LinkedHashMap<>();
        lb.put(new PlayerProfileLight(player, "Player", ""), 1000L);
        storage.setCachedTimedLeaderboard("hunt1", lb);

        storage.clearAllCachedHuntDataForPlayer(player);

        // Timed leaderboard is a separate cache not cleared by clearAllCachedHuntDataForPlayer
        assertThat(storage.getCachedTimedLeaderboard("hunt1")).isNotNull().hasSize(1);
    }

    @Test
    void clearAllCachedHuntDataForPlayer_doesNotAffectHuntTopPlayers() throws InternalException {
        UUID player = UUID.randomUUID();
        storage.setCachedPlayerHeadsForHunt(player, "hunt1", Set.of(UUID.randomUUID()));

        LinkedHashMap<PlayerProfileLight, Integer> top = new LinkedHashMap<>();
        top.put(new PlayerProfileLight(player, "Player", ""), 5);
        storage.setCachedTopPlayersForHunt("hunt1", top);

        storage.clearAllCachedHuntDataForPlayer(player);

        // Hunt top players cache is separate, not cleared per-player
        assertThat(storage.getCachedTopPlayersForHunt("hunt1")).isNotNull().hasSize(1);
    }
}
