package fr.aerwyn81.headblocks.storages.types;

import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.UUID;

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
}
