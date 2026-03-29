package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.databases.Database;
import fr.aerwyn81.headblocks.storages.types.Memory;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Integration tests using REAL Memory storage + mocked Database.
 * These tests exercise the full cache-first workflow: cache miss → DB → cache set → cache hit,
 * plus cascade invalidation across multiple operations chained together.
 */
@ExtendWith(MockitoExtension.class)
class StorageServiceIntegrationTest {

    @Mock
    private Database database;

    @Mock
    private ConfigService configService;

    private Memory memory;
    private StorageService service;

    @BeforeEach
    void setUp() {
        memory = new Memory();
        memory.init();
        service = new StorageService(configService, memory, database);
    }

    // =========================================================================
    // Full player head lifecycle: add → check → leaderboard → reset → verify
    // =========================================================================

    @Nested
    class PlayerHeadLifecycle {

        @Test
        void fullLifecycle_addHeads_checkCache_leaderboard_thenReset() throws InternalException {
            UUID alice = UUID.randomUUID();
            UUID bob = UUID.randomUUID();
            UUID head1 = UUID.randomUUID();
            UUID head2 = UUID.randomUUID();
            UUID head3 = UUID.randomUUID();

            // Step 1: Alice finds head1 and head2
            service.addHead(alice, head1);
            service.addHead(alice, head2);

            // Step 2: Bob finds head1
            service.addHead(bob, head1);

            // Step 3: hasHead should be resolved from cache (no DB call for cache lookup)
            assertThat(service.hasHead(alice, head1)).isTrue();
            assertThat(service.hasHead(alice, head2)).isTrue();
            assertThat(service.hasHead(alice, head3)).isFalse();
            assertThat(service.hasHead(bob, head1)).isTrue();
            assertThat(service.hasHead(bob, head2)).isFalse();

            // Step 4: Top players — cache was cleared by addHead, so DB is called
            var topPlayers = new LinkedHashMap<PlayerProfileLight, Integer>();
            topPlayers.put(new PlayerProfileLight(alice, "Alice", ""), 2);
            topPlayers.put(new PlayerProfileLight(bob, "Bob", ""), 1);
            when(database.getTopPlayers()).thenReturn(topPlayers);

            var result = service.getTopPlayers();
            assertThat(result).hasSize(2);
            verify(database, times(1)).getTopPlayers();

            // Step 5: Second call to getTopPlayers → cache hit, no additional DB call
            var result2 = service.getTopPlayers();
            assertThat(result2).hasSize(2);
            verify(database, times(1)).getTopPlayers(); // still 1

            // Step 6: Reset Alice's progress
            when(database.getHeadsPlayer(alice)).thenReturn(new ArrayList<>());
            service.resetPlayer(alice);

            // Step 7: After reset, Alice has no heads (cache rebuilt from DB)
            assertThat(service.hasHead(alice, head1)).isFalse();
            assertThat(service.hasHead(alice, head2)).isFalse();

            // Step 8: Bob's heads unaffected by Alice's reset
            assertThat(service.hasHead(bob, head1)).isTrue();

            // Step 9: Top players cache was cleared by reset, so DB called again
            when(database.getTopPlayers()).thenReturn(new LinkedHashMap<>() {{
                put(new PlayerProfileLight(bob, "Bob", ""), 1);
            }});
            var result3 = service.getTopPlayers();
            assertThat(result3).hasSize(1);
            verify(database, times(2)).getTopPlayers();
        }

        @Test
        void addHead_thenResetSingleHead_onlyThatHeadRemoved() throws InternalException {
            UUID player = UUID.randomUUID();
            UUID head1 = UUID.randomUUID();
            UUID head2 = UUID.randomUUID();

            service.addHead(player, head1);
            service.addHead(player, head2);

            // Both heads present
            assertThat(service.hasHead(player, head1)).isTrue();
            assertThat(service.hasHead(player, head2)).isTrue();

            // Reset only head1
            service.resetPlayerHead(player, head1);

            // head1 gone, head2 remains
            assertThat(service.hasHead(player, head1)).isFalse();
            assertThat(service.hasHead(player, head2)).isTrue();
        }

        @Test
        void removeHead_cascadesAcrossAllPlayers_andInvalidatesLeaderboard() throws InternalException {
            UUID alice = UUID.randomUUID();
            UUID bob = UUID.randomUUID();
            UUID charlie = UUID.randomUUID();
            UUID sharedHead = UUID.randomUUID();
            UUID aliceOnlyHead = UUID.randomUUID();

            // All three players found sharedHead; Alice also found aliceOnlyHead
            service.addHead(alice, sharedHead);
            service.addHead(alice, aliceOnlyHead);
            service.addHead(bob, sharedHead);
            service.addHead(charlie, sharedHead);

            // Populate top players cache
            when(database.getTopPlayers()).thenReturn(new LinkedHashMap<>());
            service.getTopPlayers();
            verify(database, times(1)).getTopPlayers();

            // Remove sharedHead entirely
            service.removeHead(sharedHead, true);

            // Cache cascaded: sharedHead removed from all player caches
            assertThat(service.hasHead(alice, sharedHead)).isFalse();
            assertThat(service.hasHead(bob, sharedHead)).isFalse();
            assertThat(service.hasHead(charlie, sharedHead)).isFalse();

            // Alice still has her other head
            assertThat(service.hasHead(alice, aliceOnlyHead)).isTrue();

            // Top players cache was invalidated by removeCachedHead
            when(database.getTopPlayers()).thenReturn(new LinkedHashMap<>());
            service.getTopPlayers();
            verify(database, times(2)).getTopPlayers();
        }
    }

    // =========================================================================
    // Hunt-specific progression: add → query → leaderboard → reset
    // =========================================================================

    @Nested
    class HuntProgressionLifecycle {

        @Test
        void fullHuntLifecycle_addHeads_queryProgress_leaderboard_reset() throws InternalException {
            UUID player = UUID.randomUUID();
            UUID head1 = UUID.randomUUID();
            UUID head2 = UUID.randomUUID();
            String huntId = "halloween-2025";

            // Step 1: Player finds heads in a hunt
            service.addHeadForHunt(player, head1, huntId);
            service.addHeadForHunt(player, head2, huntId);

            // Step 2: Hunt-specific progress is cached
            Set<UUID> cached = memory.getCachedPlayerHeadsForHunt(player, huntId);
            assertThat(cached).containsExactlyInAnyOrder(head1, head2);

            // Step 3: Query goes through cache (no DB call)
            var progress = service.getHeadsPlayerForHunt(player, huntId);
            assertThat(progress).containsExactlyInAnyOrder(head1, head2);
            verify(database, never()).getHeadsPlayerForHunt(any(), any());

            // Step 4: Global cache also updated
            assertThat(service.hasHead(player, head1)).isTrue();
            assertThat(service.hasHead(player, head2)).isTrue();

            // Step 5: Hunt leaderboard — cache miss triggers DB
            var huntLeaderboard = new LinkedHashMap<PlayerProfileLight, Integer>();
            huntLeaderboard.put(new PlayerProfileLight(player, "Player1", ""), 2);
            when(database.getTopPlayersForHunt(huntId)).thenReturn(huntLeaderboard);

            var lb = service.getTopPlayersForHunt(huntId);
            assertThat(lb).hasSize(1);
            verify(database, times(1)).getTopPlayersForHunt(huntId);

            // Step 6: Second call → cache hit
            service.getTopPlayersForHunt(huntId);
            verify(database, times(1)).getTopPlayersForHunt(huntId);

            // Step 7: Reset player's hunt progress
            when(database.getHeadsPlayer(player)).thenReturn(new ArrayList<>());
            service.resetPlayerHunt(player, huntId);

            // Step 8: Hunt cache cleared — next query goes to DB
            when(database.getHeadsPlayerForHunt(player, huntId)).thenReturn(new ArrayList<>());
            var afterReset = service.getHeadsPlayerForHunt(player, huntId);
            assertThat(afterReset).isEmpty();
            verify(database, times(1)).getHeadsPlayerForHunt(player, huntId);

            // Step 9: Global cache also rebuilt — player has no heads globally
            assertThat(service.hasHead(player, head1)).isFalse();

            // Step 10: Hunt leaderboard cache invalidated
            when(database.getTopPlayersForHunt(huntId)).thenReturn(new LinkedHashMap<>());
            service.getTopPlayersForHunt(huntId);
            verify(database, times(2)).getTopPlayersForHunt(huntId);
        }

        @Test
        void multipleHunts_progressIsIsolated() throws InternalException {
            UUID player = UUID.randomUUID();
            UUID headA = UUID.randomUUID();
            UUID headB = UUID.randomUUID();
            String hunt1 = "hunt-alpha";
            String hunt2 = "hunt-beta";

            // Player finds different heads in different hunts
            service.addHeadForHunt(player, headA, hunt1);
            service.addHeadForHunt(player, headB, hunt2);

            // Each hunt has its own cache
            var hunt1Heads = service.getHeadsPlayerForHunt(player, hunt1);
            assertThat(hunt1Heads).containsExactly(headA);

            var hunt2Heads = service.getHeadsPlayerForHunt(player, hunt2);
            assertThat(hunt2Heads).containsExactly(headB);

            // Reset hunt1 doesn't affect hunt2
            when(database.getHeadsPlayer(player)).thenReturn(new ArrayList<>(List.of(headB)));
            service.resetPlayerHunt(player, hunt1);

            // hunt2 cache still intact
            var hunt2HeadsAfter = service.getHeadsPlayerForHunt(player, hunt2);
            assertThat(hunt2HeadsAfter).containsExactly(headB);
        }

        @Test
        void transferProgress_invalidatesBothHuntCaches() throws InternalException {
            UUID player = UUID.randomUUID();
            UUID head = UUID.randomUUID();
            String fromHunt = "old-hunt";
            String toHunt = "new-hunt";

            // Populate caches for both hunts
            service.addHeadForHunt(player, head, fromHunt);
            memory.setCachedPlayerHeadsForHunt(player, toHunt, new HashSet<>());

            // Transfer
            service.transferPlayerProgress(fromHunt, toHunt);

            // Both hunt caches cleared
            assertThat(memory.getCachedPlayerHeadsForHunt(player, fromHunt)).isNull();
            assertThat(memory.getCachedPlayerHeadsForHunt(player, toHunt)).isNull();
            assertThat(memory.getCachedTopPlayersForHunt(fromHunt)).isNull();
            assertThat(memory.getCachedTopPlayersForHunt(toHunt)).isNull();
        }
    }

    // =========================================================================
    // Timed run lifecycle: save run → leaderboard → best time → count
    // =========================================================================

    @Nested
    class TimedRunLifecycle {

        @Test
        void fullTimedRunLifecycle_saveRuns_queryLeaderboard_bestTime_count() throws InternalException {
            UUID alice = UUID.randomUUID();
            UUID bob = UUID.randomUUID();
            String huntId = "speedrun-hunt";

            // Step 1: Alice completes a run in 5000ms
            service.saveTimedRun(alice, huntId, 5000L);

            // Step 2: Bob completes a run in 3000ms
            service.saveTimedRun(bob, huntId, 3000L);

            // Step 3: Query leaderboard — cache miss, DB called
            var leaderboard = new LinkedHashMap<PlayerProfileLight, Long>();
            leaderboard.put(new PlayerProfileLight(bob, "Bob", ""), 3000L);
            leaderboard.put(new PlayerProfileLight(alice, "Alice", ""), 5000L);
            when(database.getTimedLeaderboard(huntId, 10)).thenReturn(leaderboard);

            var lb = service.getTimedLeaderboard(huntId, 10);
            assertThat(lb).hasSize(2);
            assertThat(lb.values().iterator().next()).isEqualTo(3000L); // Bob first
            verify(database, times(1)).getTimedLeaderboard(huntId, 10);

            // Step 4: Second call → cache hit
            service.getTimedLeaderboard(huntId, 10);
            verify(database, times(1)).getTimedLeaderboard(huntId, 10);

            // Step 5: Best time — cache miss, DB called
            when(database.getBestTime(alice, huntId)).thenReturn(5000L);
            Long aliceBest = service.getBestTime(alice, huntId);
            assertThat(aliceBest).isEqualTo(5000L);
            verify(database, times(1)).getBestTime(alice, huntId);

            // Step 6: Best time cached — no more DB calls
            Long aliceBest2 = service.getBestTime(alice, huntId);
            assertThat(aliceBest2).isEqualTo(5000L);
            verify(database, times(1)).getBestTime(alice, huntId);

            // Step 7: Run count — cache miss, DB called
            when(database.getTimedRunCount(alice, huntId)).thenReturn(1);
            int count = service.getTimedRunCount(alice, huntId);
            assertThat(count).isEqualTo(1);
            verify(database, times(1)).getTimedRunCount(alice, huntId);

            // Step 8: Alice does a faster run — all caches invalidated
            service.saveTimedRun(alice, huntId, 4000L);

            // Step 9: Best time cache cleared, next call goes to DB
            when(database.getBestTime(alice, huntId)).thenReturn(4000L);
            Long aliceBest3 = service.getBestTime(alice, huntId);
            assertThat(aliceBest3).isEqualTo(4000L);
            verify(database, times(2)).getBestTime(alice, huntId);

            // Step 10: Run count cache cleared too
            when(database.getTimedRunCount(alice, huntId)).thenReturn(2);
            int count2 = service.getTimedRunCount(alice, huntId);
            assertThat(count2).isEqualTo(2);
            verify(database, times(2)).getTimedRunCount(alice, huntId);

            // Step 11: Leaderboard cache also cleared
            when(database.getTimedLeaderboard(huntId, 10)).thenReturn(leaderboard);
            service.getTimedLeaderboard(huntId, 10);
            verify(database, times(2)).getTimedLeaderboard(huntId, 10);
        }

        @Test
        void bestTime_nullFromDb_cachedAsSentinel_returnsNull() throws InternalException {
            UUID player = UUID.randomUUID();
            String huntId = "hunt1";

            // DB returns null (no runs)
            when(database.getBestTime(player, huntId)).thenReturn(null);
            Long best = service.getBestTime(player, huntId);
            assertThat(best).isNull();

            // Second call: cache has sentinel -1L, returns null without DB call
            Long best2 = service.getBestTime(player, huntId);
            assertThat(best2).isNull();
            verify(database, times(1)).getBestTime(player, huntId);
        }

        @Test
        void timedRun_differentHunts_cachesAreIsolated() throws InternalException {
            UUID player = UUID.randomUUID();
            String hunt1 = "hunt-a";
            String hunt2 = "hunt-b";

            // Save run for hunt1
            service.saveTimedRun(player, hunt1, 1000L);

            // Cache best time for hunt2
            when(database.getBestTime(player, hunt2)).thenReturn(2000L);
            service.getBestTime(player, hunt2);

            // Save another run for hunt1 — only hunt1 caches cleared
            service.saveTimedRun(player, hunt1, 900L);

            // hunt2 best time still cached
            Long hunt2Best = service.getBestTime(player, hunt2);
            assertThat(hunt2Best).isEqualTo(2000L);
            verify(database, times(1)).getBestTime(player, hunt2); // only 1 call total
        }
    }

    // =========================================================================
    // Cache invalidation chains: verifying that write operations properly
    // invalidate dependent caches across the whole service
    // =========================================================================

    @Nested
    class CacheInvalidationChains {

        @Test
        void addHead_invalidatesTopPlayers_butNotOtherPlayerCaches() throws InternalException {
            UUID alice = UUID.randomUUID();
            UUID bob = UUID.randomUUID();
            UUID head1 = UUID.randomUUID();
            UUID head2 = UUID.randomUUID();

            // Populate Bob's cache
            service.addHead(bob, head1);

            // Populate top players cache
            when(database.getTopPlayers()).thenReturn(new LinkedHashMap<>());
            service.getTopPlayers();

            // Alice adds a head — top players invalidated but Bob's cache untouched
            service.addHead(alice, head2);

            // Bob's cache still intact
            assertThat(service.hasHead(bob, head1)).isTrue();

            // Top players requires fresh DB call
            when(database.getTopPlayers()).thenReturn(new LinkedHashMap<>());
            service.getTopPlayers();
            verify(database, times(2)).getTopPlayers();
        }

        @Test
        void removeHead_cascadesClearsThroughEntireCache() throws InternalException {
            UUID player1 = UUID.randomUUID();
            UUID player2 = UUID.randomUUID();
            UUID headToRemove = UUID.randomUUID();
            UUID headToKeep = UUID.randomUUID();

            // Both players find both heads
            service.addHead(player1, headToRemove);
            service.addHead(player1, headToKeep);
            service.addHead(player2, headToRemove);
            service.addHead(player2, headToKeep);

            // Also add to heads cache
            service.createOrUpdateHead(headToRemove, "texture1");
            service.createOrUpdateHead(headToKeep, "texture2");

            // Remove one head
            service.removeHead(headToRemove, true);

            // Head removed from heads set
            assertThat(memory.getCachedHeads()).containsExactly(headToKeep);

            // Head removed from both player caches
            assertThat(service.hasHead(player1, headToRemove)).isFalse();
            assertThat(service.hasHead(player2, headToRemove)).isFalse();

            // Other head still present for both players
            assertThat(service.hasHead(player1, headToKeep)).isTrue();
            assertThat(service.hasHead(player2, headToKeep)).isTrue();
        }

        @Test
        void invalidateCachePlayer_rebuildsFromDb_subsequentHasHeadUsesNewCache() throws InternalException {
            UUID player = UUID.randomUUID();
            UUID head1 = UUID.randomUUID();
            UUID head2 = UUID.randomUUID();

            // Player initially has both heads in cache
            service.addHead(player, head1);
            service.addHead(player, head2);
            assertThat(service.hasHead(player, head1)).isTrue();
            assertThat(service.hasHead(player, head2)).isTrue();

            // DB says player now only has head2 (head1 was removed externally)
            when(database.getHeadsPlayer(player)).thenReturn(new ArrayList<>(List.of(head2)));

            // Invalidate cache — rebuilds from DB
            service.invalidateCachePlayer(player);

            // Cache now reflects DB state
            assertThat(service.hasHead(player, head1)).isFalse();
            assertThat(service.hasHead(player, head2)).isTrue();
        }

        @Test
        void addHeadForHunt_invalidatesBothGlobalAndHuntCaches() throws InternalException {
            UUID player = UUID.randomUUID();
            UUID head = UUID.randomUUID();
            String huntId = "my-hunt";

            // Populate global top players cache
            when(database.getTopPlayers()).thenReturn(new LinkedHashMap<>());
            service.getTopPlayers();

            // Populate hunt top players cache
            when(database.getTopPlayersForHunt(huntId)).thenReturn(new LinkedHashMap<>());
            service.getTopPlayersForHunt(huntId);

            // Add head for hunt — both caches should be invalidated
            service.addHeadForHunt(player, head, huntId);

            // Global top players cache cleared → DB called again
            when(database.getTopPlayers()).thenReturn(new LinkedHashMap<>());
            service.getTopPlayers();
            verify(database, times(2)).getTopPlayers();

            // Hunt top players cache cleared → DB called again
            when(database.getTopPlayersForHunt(huntId)).thenReturn(new LinkedHashMap<>());
            service.getTopPlayersForHunt(huntId);
            verify(database, times(2)).getTopPlayersForHunt(huntId);
        }
    }

    // =========================================================================
    // Head management: create → get → remove lifecycle
    // =========================================================================

    @Nested
    class HeadManagementLifecycle {

        @Test
        void createHeads_getHeads_removeHead_verifyCache() throws InternalException {
            UUID head1 = UUID.randomUUID();
            UUID head2 = UUID.randomUUID();
            UUID head3 = UUID.randomUUID();

            // Step 1: Create heads — each goes into cache
            service.createOrUpdateHead(head1, "tex1");
            service.createOrUpdateHead(head2, "tex2");
            service.createOrUpdateHead(head3, "tex3");

            // Step 2: getHeads from cache (non-empty → no DB call)
            var heads = service.getHeads();
            assertThat(heads).containsExactlyInAnyOrder(head1, head2, head3);
            verify(database, never()).getHeads();

            // Step 3: Remove one head
            service.removeHead(head2, true);

            // Step 4: Cache updated without DB call
            var headsAfter = service.getHeads();
            assertThat(headsAfter).containsExactlyInAnyOrder(head1, head3);
            verify(database, never()).getHeads();
        }

        @Test
        void getHeads_emptyCacheMiss_loadsFromDb_thenCacheHit() throws InternalException {
            UUID head1 = UUID.randomUUID();
            UUID head2 = UUID.randomUUID();

            // First call: cache empty → DB
            when(database.getHeads()).thenReturn(new ArrayList<>(List.of(head1, head2)));
            var heads = service.getHeads();
            assertThat(heads).containsExactlyInAnyOrder(head1, head2);
            verify(database, times(1)).getHeads();

            // Second call: cache populated → no DB
            var heads2 = service.getHeads();
            assertThat(heads2).containsExactlyInAnyOrder(head1, head2);
            verify(database, times(1)).getHeads();
        }
    }

    // =========================================================================
    // Multi-player concurrent scenario: interleaved operations
    // =========================================================================

    @Nested
    class MultiPlayerScenarios {

        @Test
        void interleavedOperations_cachesRemainConsistent() throws InternalException {
            UUID alice = UUID.randomUUID();
            UUID bob = UUID.randomUUID();
            UUID charlie = UUID.randomUUID();
            UUID head1 = UUID.randomUUID();
            UUID head2 = UUID.randomUUID();
            UUID head3 = UUID.randomUUID();
            String huntId = "event";

            // Alice finds head1 and head2 in the hunt
            service.addHeadForHunt(alice, head1, huntId);
            service.addHeadForHunt(alice, head2, huntId);

            // Bob finds head1 in the hunt
            service.addHeadForHunt(bob, head1, huntId);

            // Charlie finds head3 in the hunt
            service.addHeadForHunt(charlie, head3, huntId);

            // Verify isolated progress
            assertThat(service.getHeadsPlayerForHunt(alice, huntId))
                    .containsExactlyInAnyOrder(head1, head2);
            assertThat(service.getHeadsPlayerForHunt(bob, huntId))
                    .containsExactly(head1);
            assertThat(service.getHeadsPlayerForHunt(charlie, huntId))
                    .containsExactly(head3);

            // Global state: all players have heads
            assertThat(service.hasHead(alice, head1)).isTrue();
            assertThat(service.hasHead(bob, head1)).isTrue();
            assertThat(service.hasHead(charlie, head3)).isTrue();

            // Reset Bob's hunt — doesn't affect Alice or Charlie
            when(database.getHeadsPlayer(bob)).thenReturn(new ArrayList<>());
            service.resetPlayerHunt(bob, huntId);

            // Bob's hunt progress gone
            when(database.getHeadsPlayerForHunt(bob, huntId)).thenReturn(new ArrayList<>());
            assertThat(service.getHeadsPlayerForHunt(bob, huntId)).isEmpty();

            // Alice and Charlie unaffected
            assertThat(service.getHeadsPlayerForHunt(alice, huntId))
                    .containsExactlyInAnyOrder(head1, head2);
            assertThat(service.getHeadsPlayerForHunt(charlie, huntId))
                    .containsExactly(head3);
        }

        @Test
        void deletePlayerProgressForHunt_clearsAllPlayerCachesForHunt() throws InternalException {
            UUID player1 = UUID.randomUUID();
            UUID player2 = UUID.randomUUID();
            UUID head = UUID.randomUUID();
            String huntId = "deletable-hunt";

            service.addHeadForHunt(player1, head, huntId);
            service.addHeadForHunt(player2, head, huntId);

            // Both players have cached hunt progress
            assertThat(memory.getCachedPlayerHeadsForHunt(player1, huntId)).isNotNull();
            assertThat(memory.getCachedPlayerHeadsForHunt(player2, huntId)).isNotNull();

            // Delete all progress for the hunt
            service.deletePlayerProgressForHunt(huntId);

            // All hunt caches cleared
            assertThat(memory.getCachedPlayerHeadsForHunt(player1, huntId)).isNull();
            assertThat(memory.getCachedPlayerHeadsForHunt(player2, huntId)).isNull();
            assertThat(memory.getCachedTopPlayersForHunt(huntId)).isNull();
        }
    }

    // =========================================================================
    // Top players leaderboard ordering: verify the LinkedHashMap preserves
    // insertion order through cache round-trips
    // =========================================================================

    @Nested
    class LeaderboardOrdering {

        @Test
        void topPlayers_preservesDbOrderingThroughCache() throws InternalException {
            var ordered = new LinkedHashMap<PlayerProfileLight, Integer>();
            ordered.put(new PlayerProfileLight(UUID.randomUUID(), "First", ""), 10);
            ordered.put(new PlayerProfileLight(UUID.randomUUID(), "Second", ""), 7);
            ordered.put(new PlayerProfileLight(UUID.randomUUID(), "Third", ""), 3);
            when(database.getTopPlayers()).thenReturn(ordered);

            // First call — DB
            var result1 = service.getTopPlayers();
            assertThat(new ArrayList<>(result1.values())).containsExactly(10, 7, 3);

            // Second call — cache, same order
            var result2 = service.getTopPlayers();
            assertThat(new ArrayList<>(result2.values())).containsExactly(10, 7, 3);
        }

        @Test
        void huntTopPlayers_preservesOrdering() throws InternalException {
            String huntId = "ranked-hunt";
            var ordered = new LinkedHashMap<PlayerProfileLight, Integer>();
            ordered.put(new PlayerProfileLight(UUID.randomUUID(), "Pro", ""), 50);
            ordered.put(new PlayerProfileLight(UUID.randomUUID(), "Mid", ""), 25);
            ordered.put(new PlayerProfileLight(UUID.randomUUID(), "Noob", ""), 1);
            when(database.getTopPlayersForHunt(huntId)).thenReturn(ordered);

            var result = service.getTopPlayersForHunt(huntId);
            assertThat(new ArrayList<>(result.values())).containsExactly(50, 25, 1);

            // Cached — same order
            var result2 = service.getTopPlayersForHunt(huntId);
            assertThat(new ArrayList<>(result2.values())).containsExactly(50, 25, 1);
        }

        @Test
        void timedLeaderboard_respectsLimitFromCache() throws InternalException {
            String huntId = "timed-hunt";
            var all = new LinkedHashMap<PlayerProfileLight, Long>();
            all.put(new PlayerProfileLight(UUID.randomUUID(), "Fast", ""), 1000L);
            all.put(new PlayerProfileLight(UUID.randomUUID(), "Medium", ""), 5000L);
            all.put(new PlayerProfileLight(UUID.randomUUID(), "Slow", ""), 10000L);

            // DB returns all 3 with limit 10
            when(database.getTimedLeaderboard(huntId, 10)).thenReturn(all);
            service.getTimedLeaderboard(huntId, 10);

            // Query with limit 2 from cache — should only return top 2
            var limited = service.getTimedLeaderboard(huntId, 2);
            assertThat(limited).hasSize(2);
            assertThat(new ArrayList<>(limited.values())).containsExactly(1000L, 5000L);
        }
    }

    // =========================================================================
    // getTopPlayers returns defensive copy — mutations don't leak into cache
    // =========================================================================

    @Nested
    class DefensiveCopies {

        @Test
        void getTopPlayers_returnsCopy_mutationDoesNotAffectCache() throws InternalException {
            var original = new LinkedHashMap<PlayerProfileLight, Integer>();
            original.put(new PlayerProfileLight(UUID.randomUUID(), "P1", ""), 5);
            when(database.getTopPlayers()).thenReturn(original);

            var result = service.getTopPlayers();
            result.clear(); // mutate the returned map

            // Cache still intact
            var result2 = service.getTopPlayers();
            assertThat(result2).hasSize(1);
            verify(database, times(1)).getTopPlayers(); // still only 1 DB call
        }

        @Test
        void getTopPlayersForHunt_cachedPath_returnsCopy_mutationDoesNotAffectCache() throws InternalException {
            // When served from cache (2nd+ call), getTopPlayersForHunt returns a stream copy.
            // The 1st call returns the DB result directly (same ref stored in cache).
            String huntId = "copy-test";
            var original = new LinkedHashMap<PlayerProfileLight, Integer>();
            original.put(new PlayerProfileLight(UUID.randomUUID(), "P1", ""), 3);
            when(database.getTopPlayersForHunt(huntId)).thenReturn(original);

            // 1st call: loads from DB and caches
            service.getTopPlayersForHunt(huntId);

            // 2nd call: from cache, returns a stream copy
            var cachedCopy = service.getTopPlayersForHunt(huntId);
            cachedCopy.clear(); // mutate the copy

            // 3rd call: still from cache, still has data
            var result = service.getTopPlayersForHunt(huntId);
            assertThat(result).hasSize(1);
            verify(database, times(1)).getTopPlayersForHunt(huntId);
        }
    }
}
