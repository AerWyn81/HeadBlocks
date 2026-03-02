package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.databases.Database;
import fr.aerwyn81.headblocks.databases.EnumTypeDatabase;
import fr.aerwyn81.headblocks.databases.Requests;
import fr.aerwyn81.headblocks.storages.Storage;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    @Mock
    private Storage storage;

    @Mock
    private Database database;

    @Mock
    private ConfigService configService;

    private StorageService service;

    @BeforeEach
    void setUp() {
        service = new StorageService(configService, storage, database);
    }

    // --- hasHead: cache hit / miss ---

    @Test
    void hasHead_cacheHit_returnsTrue_whenHeadInCachedSet() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();
        when(storage.getCachedPlayerHeads(player)).thenReturn(Set.of(head));

        assertThat(service.hasHead(player, head)).isTrue();
        verify(storage, never()).hasHead(any(), any());
    }

    @Test
    void hasHead_cacheHit_returnsFalse_whenHeadNotInCachedSet() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();
        UUID otherHead = UUID.randomUUID();
        when(storage.getCachedPlayerHeads(player)).thenReturn(Set.of(otherHead));

        assertThat(service.hasHead(player, head)).isFalse();
        verify(storage, never()).hasHead(any(), any());
    }

    @Test
    void hasHead_cacheMiss_delegatesToStorage() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();
        when(storage.getCachedPlayerHeads(player)).thenReturn(null);
        when(storage.hasHead(player, head)).thenReturn(true);

        assertThat(service.hasHead(player, head)).isTrue();
        verify(storage).hasHead(player, head);
    }

    @Test
    void hasHead_cacheMiss_returnsFalse_whenStorageSaysNo() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();
        when(storage.getCachedPlayerHeads(player)).thenReturn(null);
        when(storage.hasHead(player, head)).thenReturn(false);

        assertThat(service.hasHead(player, head)).isFalse();
    }

    // --- addHead: write-through + cache invalidation ---

    @Test
    void addHead_delegatesToStorageAndDatabase() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();

        service.addHead(player, head);

        verify(storage).addHead(player, head);
        verify(database).addHead(player, head);
    }

    @Test
    void addHead_updatesCacheAndClearsTopPlayers() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();

        service.addHead(player, head);

        verify(storage).addCachedPlayerHead(player, head);
        verify(storage).clearCachedTopPlayers();
    }

    // --- containsPlayer: delegation ---

    @Test
    void containsPlayer_trueWhenStorageContains() throws InternalException {
        UUID player = UUID.randomUUID();
        when(storage.containsPlayer(player)).thenReturn(true);

        assertThat(service.containsPlayer(player)).isTrue();
    }

    @Test
    void containsPlayer_trueWhenDatabaseContains() throws InternalException {
        UUID player = UUID.randomUUID();
        when(storage.containsPlayer(player)).thenReturn(false);
        when(database.containsPlayer(player)).thenReturn(true);

        assertThat(service.containsPlayer(player)).isTrue();
    }

    @Test
    void containsPlayer_falseWhenNeitherContains() throws InternalException {
        UUID player = UUID.randomUUID();
        when(storage.containsPlayer(player)).thenReturn(false);
        when(database.containsPlayer(player)).thenReturn(false);

        assertThat(service.containsPlayer(player)).isFalse();
    }

    // --- getTopPlayers: cache hit / miss (empty map = uncached) ---

    @Test
    void getTopPlayers_cacheHit_returnsCachedData() throws InternalException {
        LinkedHashMap<PlayerProfileLight, Integer> cached = new LinkedHashMap<>();
        cached.put(new PlayerProfileLight(UUID.randomUUID(), "Steve", ""), 10);
        when(storage.getCachedTopPlayers()).thenReturn(cached);

        LinkedHashMap<PlayerProfileLight, Integer> result = service.getTopPlayers();

        assertThat(result).hasSize(1);
        verifyNoInteractions(database);
    }

    @Test
    void getTopPlayers_cacheHit_returnsDefensiveCopy() throws InternalException {
        LinkedHashMap<PlayerProfileLight, Integer> cached = new LinkedHashMap<>();
        cached.put(new PlayerProfileLight(UUID.randomUUID(), "Steve", ""), 10);
        when(storage.getCachedTopPlayers()).thenReturn(cached);

        LinkedHashMap<PlayerProfileLight, Integer> result = service.getTopPlayers();

        assertThat(result).isNotSameAs(cached);
    }

    @Test
    void getTopPlayers_cacheMiss_loadsFromDbAndCaches() throws InternalException {
        when(storage.getCachedTopPlayers()).thenReturn(new LinkedHashMap<>());
        LinkedHashMap<PlayerProfileLight, Integer> fromDb = new LinkedHashMap<>();
        fromDb.put(new PlayerProfileLight(UUID.randomUUID(), "Alex", ""), 5);
        when(database.getTopPlayers()).thenReturn(fromDb);

        LinkedHashMap<PlayerProfileLight, Integer> result = service.getTopPlayers();

        assertThat(result).hasSize(1);
        verify(storage).setCachedTopPlayers(fromDb);
    }

    // --- getTopPlayersForHunt: cache hit / miss (null = uncached) ---

    @Test
    void getTopPlayersForHunt_cacheHit_returnsCachedData() throws InternalException {
        LinkedHashMap<PlayerProfileLight, Integer> cached = new LinkedHashMap<>();
        cached.put(new PlayerProfileLight(UUID.randomUUID(), "Steve", ""), 7);
        when(storage.getCachedTopPlayersForHunt("hunt1")).thenReturn(cached);

        LinkedHashMap<PlayerProfileLight, Integer> result = service.getTopPlayersForHunt("hunt1");

        assertThat(result).hasSize(1);
        verify(database, never()).getTopPlayersForHunt(anyString());
    }

    @Test
    void getTopPlayersForHunt_cacheMiss_loadsFromDbAndCaches() throws InternalException {
        when(storage.getCachedTopPlayersForHunt("hunt1")).thenReturn(null);
        LinkedHashMap<PlayerProfileLight, Integer> fromDb = new LinkedHashMap<>();
        fromDb.put(new PlayerProfileLight(UUID.randomUUID(), "Alex", ""), 3);
        when(database.getTopPlayersForHunt("hunt1")).thenReturn(fromDb);

        LinkedHashMap<PlayerProfileLight, Integer> result = service.getTopPlayersForHunt("hunt1");

        assertThat(result).hasSize(1);
        verify(storage).setCachedTopPlayersForHunt("hunt1", fromDb);
    }

    // --- getHeads: cache hit / miss (empty set = uncached) ---

    @Test
    void getHeads_cacheHit_returnsCachedHeads() throws InternalException {
        UUID head = UUID.randomUUID();
        when(storage.getCachedHeads()).thenReturn(Set.of(head));

        ArrayList<UUID> result = service.getHeads();

        assertThat(result).containsExactly(head);
        verifyNoInteractions(database);
    }

    @Test
    void getHeads_cacheMiss_loadsFromDbAndFillsCache() throws InternalException {
        when(storage.getCachedHeads()).thenReturn(Collections.emptySet());
        UUID head1 = UUID.randomUUID();
        UUID head2 = UUID.randomUUID();
        ArrayList<UUID> fromDb = new ArrayList<>(List.of(head1, head2));
        when(database.getHeads()).thenReturn(fromDb);

        ArrayList<UUID> result = service.getHeads();

        assertThat(result).containsExactly(head1, head2);
        verify(storage).addCachedHead(head1);
        verify(storage).addCachedHead(head2);
    }

    // --- getBestTime: sentinel -1L pattern ---

    @Test
    void getBestTime_cacheHit_returnsActualTime() throws InternalException {
        UUID player = UUID.randomUUID();
        when(storage.getCachedBestTime(player, "hunt1")).thenReturn(5000L);

        assertThat(service.getBestTime(player, "hunt1")).isEqualTo(5000L);
        verifyNoInteractions(database);
    }

    @Test
    void getBestTime_cacheHit_sentinelMinus1_returnsNull() throws InternalException {
        UUID player = UUID.randomUUID();
        when(storage.getCachedBestTime(player, "hunt1")).thenReturn(-1L);

        assertThat(service.getBestTime(player, "hunt1")).isNull();
        verifyNoInteractions(database);
    }

    @Test
    void getBestTime_cacheMiss_loadsFromDb_andCachesActualValue() throws InternalException {
        UUID player = UUID.randomUUID();
        when(storage.getCachedBestTime(player, "hunt1")).thenReturn(null);
        when(database.getBestTime(player, "hunt1")).thenReturn(3000L);

        assertThat(service.getBestTime(player, "hunt1")).isEqualTo(3000L);
        verify(storage).setCachedBestTime(player, "hunt1", 3000L);
    }

    @Test
    void getBestTime_cacheMiss_dbReturnsNull_cachesSentinel() throws InternalException {
        UUID player = UUID.randomUUID();
        when(storage.getCachedBestTime(player, "hunt1")).thenReturn(null);
        when(database.getBestTime(player, "hunt1")).thenReturn(null);

        assertThat(service.getBestTime(player, "hunt1")).isNull();
        verify(storage).setCachedBestTime(player, "hunt1", -1L);
    }

    // --- getTimedLeaderboard: cache hit with limit ---

    @Test
    void getTimedLeaderboard_cacheHit_appliesLimit() throws InternalException {
        LinkedHashMap<PlayerProfileLight, Long> cached = new LinkedHashMap<>();
        cached.put(new PlayerProfileLight(UUID.randomUUID(), "A", ""), 100L);
        cached.put(new PlayerProfileLight(UUID.randomUUID(), "B", ""), 200L);
        cached.put(new PlayerProfileLight(UUID.randomUUID(), "C", ""), 300L);
        when(storage.getCachedTimedLeaderboard("hunt1")).thenReturn(cached);

        LinkedHashMap<PlayerProfileLight, Long> result = service.getTimedLeaderboard("hunt1", 2);

        assertThat(result).hasSize(2);
        verifyNoInteractions(database);
    }

    @Test
    void getTimedLeaderboard_cacheMiss_loadsFromDbAndCaches() throws InternalException {
        when(storage.getCachedTimedLeaderboard("hunt1")).thenReturn(null);
        LinkedHashMap<PlayerProfileLight, Long> fromDb = new LinkedHashMap<>();
        fromDb.put(new PlayerProfileLight(UUID.randomUUID(), "Fast", ""), 1234L);
        when(database.getTimedLeaderboard("hunt1", 5)).thenReturn(fromDb);

        LinkedHashMap<PlayerProfileLight, Long> result = service.getTimedLeaderboard("hunt1", 5);

        assertThat(result).hasSize(1);
        verify(storage).setCachedTimedLeaderboard("hunt1", fromDb);
    }

    // --- getTimedRunCount: cache hit / miss ---

    @Test
    void getTimedRunCount_cacheHit_returnsCachedValue() throws InternalException {
        UUID player = UUID.randomUUID();
        when(storage.getCachedTimedRunCount(player, "hunt1")).thenReturn(7);

        assertThat(service.getTimedRunCount(player, "hunt1")).isEqualTo(7);
        verifyNoInteractions(database);
    }

    @Test
    void getTimedRunCount_cacheMiss_loadsFromDbAndCaches() throws InternalException {
        UUID player = UUID.randomUUID();
        when(storage.getCachedTimedRunCount(player, "hunt1")).thenReturn(null);
        when(database.getTimedRunCount(player, "hunt1")).thenReturn(3);

        assertThat(service.getTimedRunCount(player, "hunt1")).isEqualTo(3);
        verify(storage).setCachedTimedRunCount(player, "hunt1", 3);
    }

    // --- selectedStorageType ---

    @Test
    void selectedStorageType_databaseDisabled_returnsSQLite() {
        when(configService.databaseEnabled()).thenReturn(false);

        assertThat(service.selectedStorageType()).isEqualTo("SQLite");
    }

    @Test
    void selectedStorageType_databaseEnabled_returnsConfigType_mysql() {
        when(configService.databaseEnabled()).thenReturn(true);
        when(configService.databaseType()).thenReturn(EnumTypeDatabase.MySQL);

        assertThat(service.selectedStorageType()).isEqualTo("MySQL");
    }

    @Test
    void selectedStorageType_databaseEnabled_returnsConfigType_sqlite() {
        when(configService.databaseEnabled()).thenReturn(true);
        when(configService.databaseType()).thenReturn(EnumTypeDatabase.SQLite);

        assertThat(service.selectedStorageType()).isEqualTo("SQLite");
    }

    // --- removeHead ---

    @Test
    void removeHead_delegatesAndInvalidatesCache() throws InternalException {
        UUID head = UUID.randomUUID();

        service.removeHead(head, true);

        verify(storage).removeHead(head);
        verify(database).removeHead(head, true);
        verify(storage).removeCachedHead(head);
    }

    @Test
    void removeHead_withDeleteFalse_passesFlag() throws InternalException {
        UUID head = UUID.randomUUID();

        service.removeHead(head, false);

        verify(database).removeHead(head, false);
    }

    // --- resetPlayer ---

    @Test
    void resetPlayer_invalidatesCacheThenDelegates() throws InternalException {
        UUID player = UUID.randomUUID();
        when(storage.getCachedPlayerHeads(player)).thenReturn(null);

        service.resetPlayer(player);

        verify(storage).clearCachedTopPlayers();
        verify(storage).resetPlayer(player);
        verify(database).resetPlayer(player);
    }

    // --- addHeadForHunt ---

    @Test
    void addHeadForHunt_delegatesToStorageAndDatabase() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();

        service.addHeadForHunt(player, head, "hunt1");

        verify(storage).addHead(player, head);
        verify(database).addHeadForHunt(player, head, "hunt1");
    }

    @Test
    void addHeadForHunt_updatesGlobalAndHuntCaches() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();

        service.addHeadForHunt(player, head, "hunt1");

        verify(storage).addCachedPlayerHead(player, head);
        verify(storage).clearCachedTopPlayers();
        verify(storage).addCachedPlayerHeadForHunt(player, "hunt1", head);
        verify(storage).clearCachedTopPlayersForHunt("hunt1");
    }

    // --- transferPlayerProgress ---

    @Test
    void transferPlayerProgress_delegatesToDbAndClearsBothHuntCaches() throws InternalException {
        service.transferPlayerProgress("huntA", "huntB");

        verify(database).transferPlayerProgress("huntA", "huntB");
        verify(storage).clearCachedPlayerHeadsForHunt("huntA");
        verify(storage).clearCachedTopPlayersForHunt("huntA");
        verify(storage).clearCachedPlayerHeadsForHunt("huntB");
        verify(storage).clearCachedTopPlayersForHunt("huntB");
    }

    // --- saveTimedRun ---

    @Test
    void saveTimedRun_delegatesToDbAndClearsTimedCaches() throws InternalException {
        UUID player = UUID.randomUUID();

        service.saveTimedRun(player, "hunt1", 12345L);

        verify(database).saveTimedRun(player, "hunt1", 12345L);
        verify(storage).clearCachedTimedLeaderboard("hunt1");
        verify(storage).clearCachedBestTime(player, "hunt1");
        verify(storage).clearCachedTimedRunCount(player, "hunt1");
    }

    // --- getHuntVersion ---

    @Test
    void getHuntVersion_delegatesToStorage() throws InternalException {
        when(storage.getHuntVersion()).thenReturn(42L);

        assertThat(service.getHuntVersion()).isEqualTo(42L);
    }

    @Test
    void getHuntVersion_onException_returnsZero() throws InternalException {
        when(storage.getHuntVersion()).thenThrow(new InternalException("fail"));

        assertThat(service.getHuntVersion()).isEqualTo(0L);
    }

    // --- incrementHuntVersion ---

    @Test
    void incrementHuntVersion_delegatesToStorage() throws InternalException {
        service.incrementHuntVersion();

        verify(storage).incrementHuntVersion();
    }

    @Test
    void incrementHuntVersion_onException_doesNotPropagate() throws InternalException {
        doThrow(new InternalException("fail")).when(storage).incrementHuntVersion();

        service.incrementHuntVersion(); // should not throw
    }

    // --- close ---

    @Test
    void close_closesStorageAndDatabase() throws InternalException {
        service.close();

        verify(storage).close();
        verify(database).close();
    }

    // --- getHeadsPlayerForHunt: cache hit / miss ---

    @Test
    void getHeadsPlayerForHunt_cacheHit_returnsCachedData() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();
        when(storage.getCachedPlayerHeadsForHunt(player, "hunt1")).thenReturn(Set.of(head));

        ArrayList<UUID> result = service.getHeadsPlayerForHunt(player, "hunt1");

        assertThat(result).containsExactly(head);
        verify(database, never()).getHeadsPlayerForHunt(any(), anyString());
    }

    @Test
    void getHeadsPlayerForHunt_cacheMiss_loadsFromDbAndCaches() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();
        when(storage.getCachedPlayerHeadsForHunt(player, "hunt1")).thenReturn(null);
        when(database.getHeadsPlayerForHunt(player, "hunt1")).thenReturn(new ArrayList<>(List.of(head)));

        ArrayList<UUID> result = service.getHeadsPlayerForHunt(player, "hunt1");

        assertThat(result).containsExactly(head);
        verify(storage).setCachedPlayerHeadsForHunt(eq(player), eq("hunt1"), anySet());
    }

    // --- resetPlayerHunt ---

    @Test
    void resetPlayerHunt_delegatesAndInvalidatesCaches() throws InternalException {
        UUID player = UUID.randomUUID();
        when(storage.getCachedPlayerHeads(player)).thenReturn(null);

        service.resetPlayerHunt(player, "hunt1");

        verify(database).resetPlayerHunt(player, "hunt1");
        verify(storage).clearCachedTopPlayers();
        verify(storage).removeCachedPlayerHeadsForHunt(player, "hunt1");
        verify(storage).clearCachedTopPlayersForHunt("hunt1");
    }

    // --- deletePlayerProgressForHunt ---

    @Test
    void deletePlayerProgressForHunt_delegatesAndClearsCaches() throws InternalException {
        service.deletePlayerProgressForHunt("hunt1");

        verify(database).deletePlayerProgressForHunt("hunt1");
        verify(storage).clearCachedPlayerHeadsForHunt("hunt1");
        verify(storage).clearCachedTopPlayersForHunt("hunt1");
    }

    // --- Simple delegation methods ---

    @Test
    void getAllPlayers_delegatesToDatabase() throws InternalException {
        ArrayList<UUID> players = new ArrayList<>(List.of(UUID.randomUUID()));
        when(database.getAllPlayers()).thenReturn(players);

        assertThat(service.getAllPlayers()).isSameAs(players);
    }

    @Test
    void updatePlayerName_delegatesToDatabase() throws InternalException {
        PlayerProfileLight profile = new PlayerProfileLight(UUID.randomUUID(), "Steve", "");

        service.updatePlayerName(profile);

        verify(database).updatePlayerInfo(profile);
    }

    @Test
    void hasPlayerRenamed_delegatesToDatabase() throws InternalException {
        PlayerProfileLight profile = new PlayerProfileLight(UUID.randomUUID(), "Steve", "");
        when(database.hasPlayerRenamed(profile)).thenReturn(true);

        assertThat(service.hasPlayerRenamed(profile)).isTrue();
    }

    @Test
    void isHeadExist_delegatesToDatabase() throws InternalException {
        UUID head = UUID.randomUUID();
        when(database.isHeadExist(head)).thenReturn(true);

        assertThat(service.isHeadExist(head)).isTrue();
    }

    @Test
    void getHeadTexture_delegatesToDatabase() throws InternalException {
        UUID head = UUID.randomUUID();
        when(database.getHeadTexture(head)).thenReturn("texture123");

        assertThat(service.getHeadTexture(head)).isEqualTo("texture123");
    }

    @Test
    void getPlayers_delegatesToDatabase() throws InternalException {
        UUID head = UUID.randomUUID();
        ArrayList<UUID> players = new ArrayList<>(List.of(UUID.randomUUID()));
        when(database.getPlayers(head)).thenReturn(players);

        assertThat(service.getPlayers(head)).isSameAs(players);
    }

    @Test
    void getPlayerByName_delegatesToDatabase() throws InternalException {
        PlayerProfileLight profile = new PlayerProfileLight(UUID.randomUUID(), "Steve", "");
        when(database.getPlayerByName("Steve")).thenReturn(profile);

        assertThat(service.getPlayerByName("Steve")).isSameAs(profile);
    }

    // --- isStorageError ---

    @Test
    void isStorageError_falseAfterTestConstruction() {
        assertThat(service.isStorageError()).isFalse();
    }

    // --- resetPlayerHead ---

    @Test
    void resetPlayerHead_delegatesAndInvalidatesCache() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();
        Set<UUID> cached = new HashSet<>(Set.of(head, UUID.randomUUID()));
        when(storage.getCachedPlayerHeads(player)).thenReturn(cached);

        service.resetPlayerHead(player, head);

        verify(storage).resetPlayerHead(player, head);
        verify(database).resetPlayerHead(player, head);
        verify(storage).clearCachedTopPlayers();
    }

    // --- createOrUpdateHead ---

    @Test
    void createOrUpdateHead_delegatesAndAddsCachedHead() throws InternalException {
        UUID head = UUID.randomUUID();

        service.createOrUpdateHead(head, "textureABC");

        verify(database).createNewHead(eq(head), eq("textureABC"), anyString());
        verify(storage).addCachedHead(head);
    }

    // ====================================================================
    // NEW TESTS: expand coverage for untested / under-tested methods
    // ====================================================================

    // --- invalidateCachePlayer (public method) ---

    @Nested
    class InvalidateCachePlayerTests {

        @Test
        void invalidateCachePlayer_withCachedHeads_clearsAndSets() throws InternalException {
            UUID player = UUID.randomUUID();
            Set<UUID> cached = new HashSet<>(Set.of(UUID.randomUUID(), UUID.randomUUID()));
            when(storage.getCachedPlayerHeads(player)).thenReturn(cached);

            service.invalidateCachePlayer(player);

            assertThat(cached).isEmpty();
            verify(storage).clearCachedTopPlayers();
            verify(storage).setCachedPlayerHeads(player, cached);
        }

        @Test
        void invalidateCachePlayer_withNullCache_onlyClearsTopPlayers() throws InternalException {
            UUID player = UUID.randomUUID();
            when(storage.getCachedPlayerHeads(player)).thenReturn(null);

            service.invalidateCachePlayer(player);

            verify(storage).clearCachedTopPlayers();
            verify(storage, never()).setCachedPlayerHeads(any(), anySet());
        }

        @Test
        void invalidateCachePlayer_storageThrows_doesNotPropagate() throws InternalException {
            UUID player = UUID.randomUUID();
            when(storage.getCachedPlayerHeads(player)).thenThrow(new InternalException("cache error"));

            service.invalidateCachePlayer(player); // should not throw
        }

        @Test
        void invalidateCachePlayer_clearTopPlayersThrows_doesNotPropagate() throws InternalException {
            UUID player = UUID.randomUUID();
            doThrow(new InternalException("clear error")).when(storage).clearCachedTopPlayers();

            service.invalidateCachePlayer(player); // should not throw
        }
    }

    // --- resetPlayer with cached heads ---

    @Nested
    class ResetPlayerTests {

        @Test
        void resetPlayer_withCachedHeads_clearsHeadsCacheThenDelegates() throws InternalException {
            UUID player = UUID.randomUUID();
            Set<UUID> cached = new HashSet<>(Set.of(UUID.randomUUID()));
            when(storage.getCachedPlayerHeads(player)).thenReturn(cached);

            service.resetPlayer(player);

            assertThat(cached).isEmpty();
            verify(storage).setCachedPlayerHeads(player, cached);
            verify(storage).clearCachedTopPlayers();
            verify(storage).resetPlayer(player);
            verify(database).resetPlayer(player);
        }
    }

    // --- resetPlayerHead: null cache path ---

    @Nested
    class ResetPlayerHeadTests {

        @Test
        void resetPlayerHead_whenCacheNull_stillDelegatesStorageAndDatabase() throws InternalException {
            UUID player = UUID.randomUUID();
            UUID head = UUID.randomUUID();
            when(storage.getCachedPlayerHeads(player)).thenReturn(null);

            service.resetPlayerHead(player, head);

            verify(storage).resetPlayerHead(player, head);
            verify(database).resetPlayerHead(player, head);
            verify(storage).clearCachedTopPlayers();
            verify(storage, never()).setCachedPlayerHeads(any(), anySet());
        }

        @Test
        void resetPlayerHead_removesHeadFromCacheAndUpdates() throws InternalException {
            UUID player = UUID.randomUUID();
            UUID head = UUID.randomUUID();
            UUID otherHead = UUID.randomUUID();
            Set<UUID> cached = new HashSet<>(Set.of(head, otherHead));
            when(storage.getCachedPlayerHeads(player)).thenReturn(cached);

            service.resetPlayerHead(player, head);

            assertThat(cached).containsExactly(otherHead);
            verify(storage).setCachedPlayerHeads(player, cached);
            verify(storage).clearCachedTopPlayers();
            verify(storage).resetPlayerHead(player, head);
            verify(database).resetPlayerHead(player, head);
        }

        @Test
        void resetPlayerHead_cacheExceptionDoesNotPreventDelegation() throws InternalException {
            UUID player = UUID.randomUUID();
            UUID head = UUID.randomUUID();
            when(storage.getCachedPlayerHeads(player)).thenThrow(new InternalException("cache fail"));

            service.resetPlayerHead(player, head);

            verify(storage).resetPlayerHead(player, head);
            verify(database).resetPlayerHead(player, head);
        }
    }

    // --- resetPlayerHunt: with cached heads ---

    @Nested
    class ResetPlayerHuntTests {

        @Test
        void resetPlayerHunt_withCachedHeads_clearsAndDelegates() throws InternalException {
            UUID player = UUID.randomUUID();
            Set<UUID> cached = new HashSet<>(Set.of(UUID.randomUUID()));
            when(storage.getCachedPlayerHeads(player)).thenReturn(cached);

            service.resetPlayerHunt(player, "hunt1");

            assertThat(cached).isEmpty();
            verify(storage).setCachedPlayerHeads(player, cached);
            verify(database).resetPlayerHunt(player, "hunt1");
            verify(storage).removeCachedPlayerHeadsForHunt(player, "hunt1");
            verify(storage).clearCachedTopPlayersForHunt("hunt1");
        }
    }

    // --- resetAllPlayersForHunt ---

    @Nested
    class ResetAllPlayersForHuntTests {

        @Test
        void resetAllPlayersForHunt_resetsEachPlayerAndClearsCaches() throws InternalException {
            UUID player1 = UUID.randomUUID();
            UUID player2 = UUID.randomUUID();
            when(database.getAllPlayers()).thenReturn(new ArrayList<>(List.of(player1, player2)));

            service.resetAllPlayersForHunt("hunt1");

            verify(database).resetPlayerHunt(player1, "hunt1");
            verify(database).resetPlayerHunt(player2, "hunt1");
            verify(storage).clearCachedPlayerHeadsForHunt("hunt1");
            verify(storage).clearCachedTopPlayersForHunt("hunt1");
        }

        @Test
        void resetAllPlayersForHunt_noPlayers_stillClearsCaches() throws InternalException {
            when(database.getAllPlayers()).thenReturn(new ArrayList<>());

            service.resetAllPlayersForHunt("hunt1");

            verify(database, never()).resetPlayerHunt(any(), anyString());
            verify(storage).clearCachedPlayerHeadsForHunt("hunt1");
            verify(storage).clearCachedTopPlayersForHunt("hunt1");
        }
    }

    // --- getTopPlayersForHunt: defensive copy ---

    @Nested
    class GetTopPlayersForHuntTests {

        @Test
        void getTopPlayersForHunt_cacheHit_returnsDefensiveCopy() throws InternalException {
            LinkedHashMap<PlayerProfileLight, Integer> cached = new LinkedHashMap<>();
            cached.put(new PlayerProfileLight(UUID.randomUUID(), "Steve", ""), 7);
            when(storage.getCachedTopPlayersForHunt("hunt1")).thenReturn(cached);

            LinkedHashMap<PlayerProfileLight, Integer> result = service.getTopPlayersForHunt("hunt1");

            assertThat(result).isNotSameAs(cached);
            assertThat(result).hasSize(1);
        }
    }

    // --- Hunt DB delegation methods ---

    @Nested
    class HuntDbDelegationTests {

        @Test
        void getHuntsFromDb_delegatesToDatabase() throws InternalException {
            ArrayList<String[]> hunts = new ArrayList<>();
            hunts.add(new String[]{"id1", "Hunt 1", "ACTIVE"});
            when(database.getHunts()).thenReturn(hunts);

            assertThat(service.getHuntsFromDb()).isSameAs(hunts);
        }

        @Test
        void createHuntInDb_delegatesToDatabase() throws InternalException {
            service.createHuntInDb("id1", "Hunt 1", "ACTIVE");

            verify(database).createHunt("id1", "Hunt 1", "ACTIVE");
        }

        @Test
        void getHeadsForHunt_delegatesToDatabase() throws InternalException {
            UUID head = UUID.randomUUID();
            ArrayList<UUID> heads = new ArrayList<>(List.of(head));
            when(database.getHeadsForHunt("hunt1")).thenReturn(heads);

            assertThat(service.getHeadsForHunt("hunt1")).isSameAs(heads);
        }

        @Test
        void linkHeadToHunt_delegatesToDatabase() throws InternalException {
            UUID head = UUID.randomUUID();

            service.linkHeadToHunt(head, "hunt1");

            verify(database).linkHeadToHunt(head, "hunt1");
        }

        @Test
        void unlinkHeadFromHunt_delegatesToDatabase() throws InternalException {
            UUID head = UUID.randomUUID();

            service.unlinkHeadFromHunt(head, "hunt1");

            verify(database).unlinkHeadFromHunt(head, "hunt1");
        }

        @Test
        void updateHuntStateInDb_delegatesToDatabase() throws InternalException {
            service.updateHuntStateInDb("hunt1", "PAUSED");

            verify(database).updateHuntState("hunt1", "PAUSED");
        }

        @Test
        void updateHuntNameInDb_delegatesToDatabase() throws InternalException {
            service.updateHuntNameInDb("hunt1", "New Name");

            verify(database).updateHuntName("hunt1", "New Name");
        }

        @Test
        void deleteHuntFromDb_delegatesToDatabase() throws InternalException {
            service.deleteHuntFromDb("hunt1");

            verify(database).deleteHunt("hunt1");
        }

        @Test
        void unlinkAllHeadsFromHuntInDb_delegatesToDatabase() throws InternalException {
            service.unlinkAllHeadsFromHuntInDb("hunt1");

            verify(database).unlinkAllHeadsFromHunt("hunt1");
        }
    }

    // --- getHeadsByServerId ---

    @Nested
    class GetHeadsByServerIdTests {

        @Test
        void getHeadsByServerId_delegatesToDatabaseWithServerIdentifier() throws InternalException {
            UUID head = UUID.randomUUID();
            ArrayList<UUID> heads = new ArrayList<>(List.of(head));
            // serverIdentifier is "" for test-constructed service
            when(database.getHeads("")).thenReturn(heads);

            assertThat(service.getHeadsByServerId()).isSameAs(heads);
        }
    }

    // --- getDistinctServerIds ---

    @Nested
    class GetDistinctServerIdsTests {

        @Test
        void getDistinctServerIds_delegatesToDatabase() throws InternalException {
            ArrayList<String> ids = new ArrayList<>(List.of("abc123", "def456"));
            when(database.getDistinctServerIds()).thenReturn(ids);

            assertThat(service.getDistinctServerIds()).isSameAs(ids);
        }
    }

    // --- getServerIdentifier ---

    @Nested
    class GetServerIdentifierTests {

        @Test
        void getServerIdentifier_returnsEmptyStringForTestConstructor() {
            assertThat(service.getServerIdentifier()).isEmpty();
        }
    }

    // --- getInstructionsExport ---

    @Nested
    class GetInstructionsExportTests {

        @Test
        void getInstructionsExport_mysql_containsDropAndCreateStatements() throws InternalException {
            try (MockedStatic<Requests> requestsMock = mockStatic(Requests.class)) {
                requestsMock.when(Requests::createTableHeadsMySQL).thenReturn("CREATE TABLE hb_heads_mysql");
                requestsMock.when(Requests::createTablePlayerHeadsMySQL).thenReturn("CREATE TABLE hb_playerHeads_mysql");
                requestsMock.when(Requests::createTablePlayersMySQL).thenReturn("CREATE TABLE hb_players_mysql");
                requestsMock.when(Requests::createTableVersion).thenReturn("CREATE TABLE hb_version");
                requestsMock.when(Requests::upsertVersion).thenReturn("UPDATE hb_version SET current = (?) WHERE current = (?)");

                lenient().when(configService.databasePrefix()).thenReturn("");
                when(database.getTableHeads()).thenReturn(new ArrayList<>());
                when(database.getTablePlayerHeads()).thenReturn(new ArrayList<>());
                when(database.getTablePlayers()).thenReturn(new ArrayList<>());

                ArrayList<String> result = service.getInstructionsExport(EnumTypeDatabase.MySQL);

                assertThat(result).isNotEmpty();
                assertThat(result.get(0)).contains("DROP TABLE IF EXISTS");
                assertThat(result.get(0)).contains("hb_heads");
                assertThat(result.get(1)).contains("CREATE TABLE hb_heads_mysql");
            }
        }

        @Test
        void getInstructionsExport_sqlite_containsDropAndCreateStatements() throws InternalException {
            try (MockedStatic<Requests> requestsMock = mockStatic(Requests.class)) {
                requestsMock.when(Requests::createTableHeads).thenReturn("CREATE TABLE hb_heads_sqlite");
                requestsMock.when(Requests::createTablePlayerHeads).thenReturn("CREATE TABLE hb_playerHeads_sqlite");
                requestsMock.when(Requests::createTablePlayers).thenReturn("CREATE TABLE hb_players_sqlite");
                requestsMock.when(Requests::createTableVersion).thenReturn("CREATE TABLE hb_version");
                requestsMock.when(Requests::upsertVersion).thenReturn("UPDATE hb_version SET current = (?) WHERE current = (?)");

                lenient().when(configService.databasePrefix()).thenReturn("");
                when(database.getTableHeads()).thenReturn(new ArrayList<>());
                when(database.getTablePlayerHeads()).thenReturn(new ArrayList<>());
                when(database.getTablePlayers()).thenReturn(new ArrayList<>());

                ArrayList<String> result = service.getInstructionsExport(EnumTypeDatabase.SQLite);

                assertThat(result).isNotEmpty();
                assertThat(result.get(1)).contains("CREATE TABLE hb_heads_sqlite");
            }
        }

        @Test
        void getInstructionsExport_withHeadsData_generatesInsertStatements() throws InternalException {
            try (MockedStatic<Requests> requestsMock = mockStatic(Requests.class)) {
                requestsMock.when(Requests::createTableHeadsMySQL).thenReturn("CREATE TABLE hb_heads");
                requestsMock.when(Requests::createTablePlayerHeadsMySQL).thenReturn("CREATE TABLE hb_playerHeads");
                requestsMock.when(Requests::createTablePlayersMySQL).thenReturn("CREATE TABLE hb_players");
                requestsMock.when(Requests::createTableVersion).thenReturn("CREATE TABLE hb_version");
                requestsMock.when(Requests::upsertVersion).thenReturn("UPDATE hb_version SET current = (?) WHERE current = (?)");

                lenient().when(configService.databasePrefix()).thenReturn("");

                ArrayList<AbstractMap.SimpleEntry<String, Boolean>> heads = new ArrayList<>();
                heads.add(new AbstractMap.SimpleEntry<>("head-uuid-1", true));
                heads.add(new AbstractMap.SimpleEntry<>("head-uuid-2", false));
                when(database.getTableHeads()).thenReturn(heads);

                ArrayList<AbstractMap.SimpleEntry<String, String>> playerHeads = new ArrayList<>();
                playerHeads.add(new AbstractMap.SimpleEntry<>("player-uuid-1", "head-uuid-1"));
                when(database.getTablePlayerHeads()).thenReturn(playerHeads);

                ArrayList<AbstractMap.SimpleEntry<String, String>> players = new ArrayList<>();
                players.add(new AbstractMap.SimpleEntry<>("player-uuid-1", "Steve"));
                when(database.getTablePlayers()).thenReturn(players);

                ArrayList<String> result = service.getInstructionsExport(EnumTypeDatabase.MySQL);

                // Check head inserts with boolean conversion
                String headInsert1 = result.stream()
                        .filter(s -> s.contains("head-uuid-1") && s.contains("INSERT INTO"))
                        .findFirst().orElse("");
                assertThat(headInsert1).contains("1"); // true -> 1

                String headInsert2 = result.stream()
                        .filter(s -> s.contains("head-uuid-2") && s.contains("INSERT INTO"))
                        .findFirst().orElse("");
                assertThat(headInsert2).contains("0"); // false -> 0

                // Check player head inserts
                String playerHeadInsert = result.stream()
                        .filter(s -> s.contains("player-uuid-1") && s.contains("head-uuid-1") && s.contains("hb_playerHeads"))
                        .findFirst().orElse("");
                assertThat(playerHeadInsert).isNotEmpty();

                // Check player inserts
                String playerInsert = result.stream()
                        .filter(s -> s.contains("player-uuid-1") && s.contains("Steve") && s.contains("hb_players"))
                        .findFirst().orElse("");
                assertThat(playerInsert).isNotEmpty();
            }
        }

        @Test
        void getInstructionsExport_withPrefix_usesPrefix() throws InternalException {
            try (MockedStatic<Requests> requestsMock = mockStatic(Requests.class)) {
                requestsMock.when(Requests::createTableHeadsMySQL).thenReturn("CREATE TABLE hb_heads");
                requestsMock.when(Requests::createTablePlayerHeadsMySQL).thenReturn("CREATE TABLE hb_playerHeads");
                requestsMock.when(Requests::createTablePlayersMySQL).thenReturn("CREATE TABLE hb_players");
                requestsMock.when(Requests::createTableVersion).thenReturn("CREATE TABLE hb_version");
                requestsMock.when(Requests::upsertVersion).thenReturn("UPDATE hb_version SET current = (?) WHERE current = (?)");

                when(configService.databasePrefix()).thenReturn("myprefix_");
                when(database.getTableHeads()).thenReturn(new ArrayList<>());
                when(database.getTablePlayerHeads()).thenReturn(new ArrayList<>());
                when(database.getTablePlayers()).thenReturn(new ArrayList<>());

                ArrayList<String> result = service.getInstructionsExport(EnumTypeDatabase.MySQL);

                assertThat(result.get(0)).contains("myprefix_hb_heads");
            }
        }

        @Test
        void getInstructionsExport_containsVersionStatements() throws InternalException {
            try (MockedStatic<Requests> requestsMock = mockStatic(Requests.class)) {
                requestsMock.when(Requests::createTableHeadsMySQL).thenReturn("CREATE TABLE hb_heads");
                requestsMock.when(Requests::createTablePlayerHeadsMySQL).thenReturn("CREATE TABLE hb_playerHeads");
                requestsMock.when(Requests::createTablePlayersMySQL).thenReturn("CREATE TABLE hb_players");
                requestsMock.when(Requests::createTableVersion).thenReturn("CREATE TABLE hb_version");
                requestsMock.when(Requests::upsertVersion).thenReturn("UPDATE hb_version SET current = (?) WHERE current = (?)");

                lenient().when(configService.databasePrefix()).thenReturn("");
                when(database.getTableHeads()).thenReturn(new ArrayList<>());
                when(database.getTablePlayerHeads()).thenReturn(new ArrayList<>());
                when(database.getTablePlayers()).thenReturn(new ArrayList<>());

                ArrayList<String> result = service.getInstructionsExport(EnumTypeDatabase.MySQL);

                // Last few instructions should contain version table
                String allInstructions = String.join("\n", result);
                assertThat(allInstructions).contains("hb_version");
                assertThat(allInstructions).contains("CREATE TABLE hb_version");
                assertThat(allInstructions).contains(String.valueOf(Database.version));
            }
        }

        @Test
        void getInstructionsExport_emptyData_producesMinimalInstructions() throws InternalException {
            try (MockedStatic<Requests> requestsMock = mockStatic(Requests.class)) {
                requestsMock.when(Requests::createTableHeadsMySQL).thenReturn("CREATE TABLE hb_heads");
                requestsMock.when(Requests::createTablePlayerHeadsMySQL).thenReturn("CREATE TABLE hb_playerHeads");
                requestsMock.when(Requests::createTablePlayersMySQL).thenReturn("CREATE TABLE hb_players");
                requestsMock.when(Requests::createTableVersion).thenReturn("CREATE TABLE hb_version");
                requestsMock.when(Requests::upsertVersion).thenReturn("UPDATE hb_version SET current = (?) WHERE current = (?)");

                lenient().when(configService.databasePrefix()).thenReturn("");
                when(database.getTableHeads()).thenReturn(new ArrayList<>());
                when(database.getTablePlayerHeads()).thenReturn(new ArrayList<>());
                when(database.getTablePlayers()).thenReturn(new ArrayList<>());

                ArrayList<String> result = service.getInstructionsExport(EnumTypeDatabase.MySQL);

                // Should have DROP + CREATE for heads, blank, DROP + CREATE for playerHeads, blank,
                // DROP + CREATE for players, blank, DROP + CREATE + INSERT for version
                // No INSERT statements for data since all tables are empty
                long insertCount = result.stream()
                        .filter(s -> s.startsWith("INSERT INTO"))
                        .count();
                // No INSERT statements when all data tables are empty (version uses UPDATE)
                assertThat(insertCount).isEqualTo(0);
            }
        }
    }

    // --- Error propagation tests ---

    @Nested
    class ErrorPropagationTests {

        @Test
        void hasHead_storageThrows_propagates() throws InternalException {
            UUID player = UUID.randomUUID();
            UUID head = UUID.randomUUID();
            when(storage.getCachedPlayerHeads(player)).thenThrow(new InternalException("storage error"));

            assertThatThrownBy(() -> service.hasHead(player, head))
                    .isInstanceOf(InternalException.class)
                    .hasMessage("storage error");
        }

        @Test
        void addHead_storageThrows_propagates() throws InternalException {
            UUID player = UUID.randomUUID();
            UUID head = UUID.randomUUID();
            doThrow(new InternalException("add error")).when(storage).addHead(player, head);

            assertThatThrownBy(() -> service.addHead(player, head))
                    .isInstanceOf(InternalException.class)
                    .hasMessage("add error");
        }

        @Test
        void addHead_databaseThrows_propagates() throws InternalException {
            UUID player = UUID.randomUUID();
            UUID head = UUID.randomUUID();
            doThrow(new InternalException("db add error")).when(database).addHead(player, head);

            assertThatThrownBy(() -> service.addHead(player, head))
                    .isInstanceOf(InternalException.class)
                    .hasMessage("db add error");
        }

        @Test
        void containsPlayer_storageThrows_propagates() throws InternalException {
            UUID player = UUID.randomUUID();
            when(storage.containsPlayer(player)).thenThrow(new InternalException("storage error"));

            assertThatThrownBy(() -> service.containsPlayer(player))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void containsPlayer_databaseThrows_propagates() throws InternalException {
            UUID player = UUID.randomUUID();
            when(storage.containsPlayer(player)).thenReturn(false);
            when(database.containsPlayer(player)).thenThrow(new InternalException("db error"));

            assertThatThrownBy(() -> service.containsPlayer(player))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void getTopPlayers_databaseThrows_propagates() throws InternalException {
            when(storage.getCachedTopPlayers()).thenReturn(new LinkedHashMap<>());
            when(database.getTopPlayers()).thenThrow(new InternalException("db error"));

            assertThatThrownBy(() -> service.getTopPlayers())
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void getTopPlayersForHunt_databaseThrows_propagates() throws InternalException {
            when(storage.getCachedTopPlayersForHunt("hunt1")).thenReturn(null);
            when(database.getTopPlayersForHunt("hunt1")).thenThrow(new InternalException("db error"));

            assertThatThrownBy(() -> service.getTopPlayersForHunt("hunt1"))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void getHeads_databaseThrows_propagates() throws InternalException {
            when(storage.getCachedHeads()).thenReturn(Collections.emptySet());
            when(database.getHeads()).thenThrow(new InternalException("db error"));

            assertThatThrownBy(() -> service.getHeads())
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void removeHead_storageThrows_propagates() throws InternalException {
            UUID head = UUID.randomUUID();
            doThrow(new InternalException("remove error")).when(storage).removeHead(head);

            assertThatThrownBy(() -> service.removeHead(head, true))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void resetPlayer_storageThrows_propagates() throws InternalException {
            UUID player = UUID.randomUUID();
            lenient().when(storage.getCachedPlayerHeads(player)).thenReturn(null);
            doThrow(new InternalException("reset error")).when(storage).resetPlayer(player);

            assertThatThrownBy(() -> service.resetPlayer(player))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void resetPlayer_databaseThrows_propagates() throws InternalException {
            UUID player = UUID.randomUUID();
            lenient().when(storage.getCachedPlayerHeads(player)).thenReturn(null);
            doThrow(new InternalException("db reset error")).when(database).resetPlayer(player);

            assertThatThrownBy(() -> service.resetPlayer(player))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void addHeadForHunt_databaseThrows_propagates() throws InternalException {
            UUID player = UUID.randomUUID();
            UUID head = UUID.randomUUID();
            doThrow(new InternalException("db error")).when(database).addHeadForHunt(player, head, "hunt1");

            assertThatThrownBy(() -> service.addHeadForHunt(player, head, "hunt1"))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void getHeadsPlayerForHunt_databaseThrows_propagates() throws InternalException {
            UUID player = UUID.randomUUID();
            when(storage.getCachedPlayerHeadsForHunt(player, "hunt1")).thenReturn(null);
            when(database.getHeadsPlayerForHunt(player, "hunt1")).thenThrow(new InternalException("db error"));

            assertThatThrownBy(() -> service.getHeadsPlayerForHunt(player, "hunt1"))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void transferPlayerProgress_databaseThrows_propagates() throws InternalException {
            doThrow(new InternalException("transfer error")).when(database).transferPlayerProgress("huntA", "huntB");

            assertThatThrownBy(() -> service.transferPlayerProgress("huntA", "huntB"))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void deletePlayerProgressForHunt_databaseThrows_propagates() throws InternalException {
            doThrow(new InternalException("delete error")).when(database).deletePlayerProgressForHunt("hunt1");

            assertThatThrownBy(() -> service.deletePlayerProgressForHunt("hunt1"))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void saveTimedRun_databaseThrows_propagates() throws InternalException {
            UUID player = UUID.randomUUID();
            doThrow(new InternalException("save error")).when(database).saveTimedRun(player, "hunt1", 12345L);

            assertThatThrownBy(() -> service.saveTimedRun(player, "hunt1", 12345L))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void getBestTime_databaseThrows_propagates() throws InternalException {
            UUID player = UUID.randomUUID();
            when(storage.getCachedBestTime(player, "hunt1")).thenReturn(null);
            when(database.getBestTime(player, "hunt1")).thenThrow(new InternalException("db error"));

            assertThatThrownBy(() -> service.getBestTime(player, "hunt1"))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void getTimedRunCount_databaseThrows_propagates() throws InternalException {
            UUID player = UUID.randomUUID();
            when(storage.getCachedTimedRunCount(player, "hunt1")).thenReturn(null);
            when(database.getTimedRunCount(player, "hunt1")).thenThrow(new InternalException("db error"));

            assertThatThrownBy(() -> service.getTimedRunCount(player, "hunt1"))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void resetPlayerHunt_databaseThrows_propagates() throws InternalException {
            UUID player = UUID.randomUUID();
            doThrow(new InternalException("reset hunt error")).when(database).resetPlayerHunt(player, "hunt1");

            assertThatThrownBy(() -> service.resetPlayerHunt(player, "hunt1"))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void resetAllPlayersForHunt_databaseThrows_propagates() throws InternalException {
            when(database.getAllPlayers()).thenThrow(new InternalException("get all error"));

            assertThatThrownBy(() -> service.resetAllPlayersForHunt("hunt1"))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void getAllPlayers_databaseThrows_propagates() throws InternalException {
            when(database.getAllPlayers()).thenThrow(new InternalException("db error"));

            assertThatThrownBy(() -> service.getAllPlayers())
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void updatePlayerName_databaseThrows_propagates() throws InternalException {
            PlayerProfileLight profile = new PlayerProfileLight(UUID.randomUUID(), "Steve", "");
            doThrow(new InternalException("update error")).when(database).updatePlayerInfo(profile);

            assertThatThrownBy(() -> service.updatePlayerName(profile))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void hasPlayerRenamed_databaseThrows_propagates() throws InternalException {
            PlayerProfileLight profile = new PlayerProfileLight(UUID.randomUUID(), "Steve", "");
            when(database.hasPlayerRenamed(profile)).thenThrow(new InternalException("rename error"));

            assertThatThrownBy(() -> service.hasPlayerRenamed(profile))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void isHeadExist_databaseThrows_propagates() throws InternalException {
            UUID head = UUID.randomUUID();
            when(database.isHeadExist(head)).thenThrow(new InternalException("head error"));

            assertThatThrownBy(() -> service.isHeadExist(head))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void getHeadTexture_databaseThrows_propagates() throws InternalException {
            UUID head = UUID.randomUUID();
            when(database.getHeadTexture(head)).thenThrow(new InternalException("texture error"));

            assertThatThrownBy(() -> service.getHeadTexture(head))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void createOrUpdateHead_databaseThrows_propagates() throws InternalException {
            UUID head = UUID.randomUUID();
            doThrow(new InternalException("create error")).when(database).createNewHead(eq(head), anyString(), anyString());

            assertThatThrownBy(() -> service.createOrUpdateHead(head, "tex"))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void getPlayers_databaseThrows_propagates() throws InternalException {
            UUID head = UUID.randomUUID();
            when(database.getPlayers(head)).thenThrow(new InternalException("players error"));

            assertThatThrownBy(() -> service.getPlayers(head))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void getPlayerByName_databaseThrows_propagates() throws InternalException {
            when(database.getPlayerByName("Steve")).thenThrow(new InternalException("player error"));

            assertThatThrownBy(() -> service.getPlayerByName("Steve"))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void getHeadsByServerId_databaseThrows_propagates() throws InternalException {
            when(database.getHeads("")).thenThrow(new InternalException("heads error"));

            assertThatThrownBy(() -> service.getHeadsByServerId())
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void getDistinctServerIds_databaseThrows_propagates() throws InternalException {
            when(database.getDistinctServerIds()).thenThrow(new InternalException("ids error"));

            assertThatThrownBy(() -> service.getDistinctServerIds())
                    .isInstanceOf(InternalException.class);
        }
    }

    // --- close: error handling ---

    @Nested
    class CloseTests {

        @Test
        void close_storageThrows_stillClosesDatabase() throws InternalException {
            doThrow(new InternalException("storage close fail")).when(storage).close();

            service.close();

            verify(storage).close();
            verify(database).close();
        }

        @Test
        void close_databaseThrows_doesNotPropagate() throws InternalException {
            doThrow(new InternalException("db close fail")).when(database).close();

            service.close(); // should not throw

            verify(storage).close();
            verify(database).close();
        }

        @Test
        void close_bothThrow_doesNotPropagate() throws InternalException {
            doThrow(new InternalException("storage fail")).when(storage).close();
            doThrow(new InternalException("db fail")).when(database).close();

            service.close(); // should not throw
        }
    }

    // --- getHeads: edge cases ---

    @Nested
    class GetHeadsEdgeCaseTests {

        @Test
        void getHeads_cacheHit_returnsArrayList() throws InternalException {
            UUID head1 = UUID.randomUUID();
            UUID head2 = UUID.randomUUID();
            when(storage.getCachedHeads()).thenReturn(new LinkedHashSet<>(List.of(head1, head2)));

            ArrayList<UUID> result = service.getHeads();

            assertThat(result).isInstanceOf(ArrayList.class);
            assertThat(result).containsExactly(head1, head2);
        }

        @Test
        void getHeads_cacheMiss_emptyDb_returnsEmptyList() throws InternalException {
            when(storage.getCachedHeads()).thenReturn(Collections.emptySet());
            when(database.getHeads()).thenReturn(new ArrayList<>());

            ArrayList<UUID> result = service.getHeads();

            assertThat(result).isEmpty();
            verify(storage, never()).addCachedHead(any());
        }
    }

    // --- getTopPlayers: edge cases ---

    @Nested
    class GetTopPlayersEdgeCaseTests {

        @Test
        void getTopPlayers_cacheMiss_emptyDb_cacheIsSet() throws InternalException {
            when(storage.getCachedTopPlayers()).thenReturn(new LinkedHashMap<>());
            LinkedHashMap<PlayerProfileLight, Integer> emptyFromDb = new LinkedHashMap<>();
            when(database.getTopPlayers()).thenReturn(emptyFromDb);

            LinkedHashMap<PlayerProfileLight, Integer> result = service.getTopPlayers();

            assertThat(result).isEmpty();
            verify(storage).setCachedTopPlayers(emptyFromDb);
        }

        @Test
        void getTopPlayers_cacheHit_preservesOrder() throws InternalException {
            LinkedHashMap<PlayerProfileLight, Integer> cached = new LinkedHashMap<>();
            PlayerProfileLight p1 = new PlayerProfileLight(UUID.randomUUID(), "First", "");
            PlayerProfileLight p2 = new PlayerProfileLight(UUID.randomUUID(), "Second", "");
            PlayerProfileLight p3 = new PlayerProfileLight(UUID.randomUUID(), "Third", "");
            cached.put(p1, 30);
            cached.put(p2, 20);
            cached.put(p3, 10);
            when(storage.getCachedTopPlayers()).thenReturn(cached);

            LinkedHashMap<PlayerProfileLight, Integer> result = service.getTopPlayers();

            List<Integer> values = new ArrayList<>(result.values());
            assertThat(values).containsExactly(30, 20, 10);
        }
    }

    // --- getTimedLeaderboard: edge cases ---

    @Nested
    class GetTimedLeaderboardEdgeCaseTests {

        @Test
        void getTimedLeaderboard_cacheHit_limitGreaterThanSize_returnsAll() throws InternalException {
            LinkedHashMap<PlayerProfileLight, Long> cached = new LinkedHashMap<>();
            cached.put(new PlayerProfileLight(UUID.randomUUID(), "A", ""), 100L);
            cached.put(new PlayerProfileLight(UUID.randomUUID(), "B", ""), 200L);
            when(storage.getCachedTimedLeaderboard("hunt1")).thenReturn(cached);

            LinkedHashMap<PlayerProfileLight, Long> result = service.getTimedLeaderboard("hunt1", 10);

            assertThat(result).hasSize(2);
        }

        @Test
        void getTimedLeaderboard_cacheHit_limitZero_returnsEmpty() throws InternalException {
            LinkedHashMap<PlayerProfileLight, Long> cached = new LinkedHashMap<>();
            cached.put(new PlayerProfileLight(UUID.randomUUID(), "A", ""), 100L);
            when(storage.getCachedTimedLeaderboard("hunt1")).thenReturn(cached);

            LinkedHashMap<PlayerProfileLight, Long> result = service.getTimedLeaderboard("hunt1", 0);

            assertThat(result).isEmpty();
        }
    }

    // --- getBestTime: edge cases ---

    @Nested
    class GetBestTimeEdgeCaseTests {

        @Test
        void getBestTime_cacheHit_zeroTime_returnsZero() throws InternalException {
            UUID player = UUID.randomUUID();
            when(storage.getCachedBestTime(player, "hunt1")).thenReturn(0L);

            assertThat(service.getBestTime(player, "hunt1")).isEqualTo(0L);
        }

        @Test
        void getBestTime_cacheMiss_dbReturnsZero_cachesZero() throws InternalException {
            UUID player = UUID.randomUUID();
            when(storage.getCachedBestTime(player, "hunt1")).thenReturn(null);
            when(database.getBestTime(player, "hunt1")).thenReturn(0L);

            assertThat(service.getBestTime(player, "hunt1")).isEqualTo(0L);
            verify(storage).setCachedBestTime(player, "hunt1", 0L);
        }
    }

    // --- getTimedRunCount: edge cases ---

    @Nested
    class GetTimedRunCountEdgeCaseTests {

        @Test
        void getTimedRunCount_cacheHit_zero_returnsZero() throws InternalException {
            UUID player = UUID.randomUUID();
            when(storage.getCachedTimedRunCount(player, "hunt1")).thenReturn(0);

            assertThat(service.getTimedRunCount(player, "hunt1")).isEqualTo(0);
        }

        @Test
        void getTimedRunCount_cacheMiss_dbReturnsZero_cachesZero() throws InternalException {
            UUID player = UUID.randomUUID();
            when(storage.getCachedTimedRunCount(player, "hunt1")).thenReturn(null);
            when(database.getTimedRunCount(player, "hunt1")).thenReturn(0);

            assertThat(service.getTimedRunCount(player, "hunt1")).isEqualTo(0);
            verify(storage).setCachedTimedRunCount(player, "hunt1", 0);
        }
    }

    // --- hasPlayerRenamed: false path ---

    @Test
    void hasPlayerRenamed_returnsFalse_whenDatabaseReturnsFalse() throws InternalException {
        PlayerProfileLight profile = new PlayerProfileLight(UUID.randomUUID(), "Steve", "");
        when(database.hasPlayerRenamed(profile)).thenReturn(false);

        assertThat(service.hasPlayerRenamed(profile)).isFalse();
    }

    // --- isHeadExist: false path ---

    @Test
    void isHeadExist_returnsFalse_whenDatabaseReturnsFalse() throws InternalException {
        UUID head = UUID.randomUUID();
        when(database.isHeadExist(head)).thenReturn(false);

        assertThat(service.isHeadExist(head)).isFalse();
    }

    // --- getHeadTexture: null result ---

    @Test
    void getHeadTexture_returnsNull_whenDatabaseReturnsNull() throws InternalException {
        UUID head = UUID.randomUUID();
        when(database.getHeadTexture(head)).thenReturn(null);

        assertThat(service.getHeadTexture(head)).isNull();
    }

    // --- getPlayerByName: null result ---

    @Test
    void getPlayerByName_returnsNull_whenDatabaseReturnsNull() throws InternalException {
        when(database.getPlayerByName("Unknown")).thenReturn(null);

        assertThat(service.getPlayerByName("Unknown")).isNull();
    }

    // --- Hunt DB delegation error paths ---

    @Nested
    class HuntDbErrorTests {

        @Test
        void getHuntsFromDb_databaseThrows_propagates() throws InternalException {
            when(database.getHunts()).thenThrow(new InternalException("hunts error"));

            assertThatThrownBy(() -> service.getHuntsFromDb())
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void createHuntInDb_databaseThrows_propagates() throws InternalException {
            doThrow(new InternalException("create error")).when(database).createHunt("id", "name", "state");

            assertThatThrownBy(() -> service.createHuntInDb("id", "name", "state"))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void getHeadsForHunt_databaseThrows_propagates() throws InternalException {
            when(database.getHeadsForHunt("hunt1")).thenThrow(new InternalException("heads error"));

            assertThatThrownBy(() -> service.getHeadsForHunt("hunt1"))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void linkHeadToHunt_databaseThrows_propagates() throws InternalException {
            UUID head = UUID.randomUUID();
            doThrow(new InternalException("link error")).when(database).linkHeadToHunt(head, "hunt1");

            assertThatThrownBy(() -> service.linkHeadToHunt(head, "hunt1"))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void unlinkHeadFromHunt_databaseThrows_propagates() throws InternalException {
            UUID head = UUID.randomUUID();
            doThrow(new InternalException("unlink error")).when(database).unlinkHeadFromHunt(head, "hunt1");

            assertThatThrownBy(() -> service.unlinkHeadFromHunt(head, "hunt1"))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void updateHuntStateInDb_databaseThrows_propagates() throws InternalException {
            doThrow(new InternalException("state error")).when(database).updateHuntState("hunt1", "PAUSED");

            assertThatThrownBy(() -> service.updateHuntStateInDb("hunt1", "PAUSED"))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void updateHuntNameInDb_databaseThrows_propagates() throws InternalException {
            doThrow(new InternalException("name error")).when(database).updateHuntName("hunt1", "New Name");

            assertThatThrownBy(() -> service.updateHuntNameInDb("hunt1", "New Name"))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void deleteHuntFromDb_databaseThrows_propagates() throws InternalException {
            doThrow(new InternalException("delete error")).when(database).deleteHunt("hunt1");

            assertThatThrownBy(() -> service.deleteHuntFromDb("hunt1"))
                    .isInstanceOf(InternalException.class);
        }

        @Test
        void unlinkAllHeadsFromHuntInDb_databaseThrows_propagates() throws InternalException {
            doThrow(new InternalException("unlink all error")).when(database).unlinkAllHeadsFromHunt("hunt1");

            assertThatThrownBy(() -> service.unlinkAllHeadsFromHuntInDb("hunt1"))
                    .isInstanceOf(InternalException.class);
        }
    }

    // --- addHeadForHunt: error path ---

    @Nested
    class AddHeadForHuntErrorTests {

        @Test
        void addHeadForHunt_storageAddHeadThrows_propagates() throws InternalException {
            UUID player = UUID.randomUUID();
            UUID head = UUID.randomUUID();
            doThrow(new InternalException("storage add error")).when(storage).addHead(player, head);

            assertThatThrownBy(() -> service.addHeadForHunt(player, head, "hunt1"))
                    .isInstanceOf(InternalException.class);
        }
    }

    // --- transferPlayerProgress: verify ordering of cache invalidation ---

    @Nested
    class TransferPlayerProgressTests {

        @Test
        void transferPlayerProgress_sameHuntId_stillDelegates() throws InternalException {
            service.transferPlayerProgress("hunt1", "hunt1");

            verify(database).transferPlayerProgress("hunt1", "hunt1");
            // Both hunts cleared even though same
            verify(storage, times(2)).clearCachedPlayerHeadsForHunt("hunt1");
            verify(storage, times(2)).clearCachedTopPlayersForHunt("hunt1");
        }
    }

    // --- saveTimedRun: verify all three caches cleared ---

    @Nested
    class SaveTimedRunTests {

        @Test
        void saveTimedRun_zeroTime_delegatesNormally() throws InternalException {
            UUID player = UUID.randomUUID();

            service.saveTimedRun(player, "hunt1", 0L);

            verify(database).saveTimedRun(player, "hunt1", 0L);
            verify(storage).clearCachedTimedLeaderboard("hunt1");
            verify(storage).clearCachedBestTime(player, "hunt1");
            verify(storage).clearCachedTimedRunCount(player, "hunt1");
        }
    }

    // --- getHeadsPlayerForHunt: edge cases ---

    @Nested
    class GetHeadsPlayerForHuntEdgeCaseTests {

        @Test
        void getHeadsPlayerForHunt_cacheHit_emptySet_returnsEmptyList() throws InternalException {
            UUID player = UUID.randomUUID();
            when(storage.getCachedPlayerHeadsForHunt(player, "hunt1")).thenReturn(Collections.emptySet());

            ArrayList<UUID> result = service.getHeadsPlayerForHunt(player, "hunt1");

            assertThat(result).isEmpty();
            verify(database, never()).getHeadsPlayerForHunt(any(), anyString());
        }

        @Test
        void getHeadsPlayerForHunt_cacheMiss_emptyDb_returnsEmptyAndCaches() throws InternalException {
            UUID player = UUID.randomUUID();
            when(storage.getCachedPlayerHeadsForHunt(player, "hunt1")).thenReturn(null);
            when(database.getHeadsPlayerForHunt(player, "hunt1")).thenReturn(new ArrayList<>());

            ArrayList<UUID> result = service.getHeadsPlayerForHunt(player, "hunt1");

            assertThat(result).isEmpty();
            verify(storage).setCachedPlayerHeadsForHunt(eq(player), eq("hunt1"), anySet());
        }

        @Test
        void getHeadsPlayerForHunt_returnsArrayList() throws InternalException {
            UUID player = UUID.randomUUID();
            UUID head = UUID.randomUUID();
            when(storage.getCachedPlayerHeadsForHunt(player, "hunt1")).thenReturn(Set.of(head));

            ArrayList<UUID> result = service.getHeadsPlayerForHunt(player, "hunt1");

            assertThat(result).isInstanceOf(ArrayList.class);
        }
    }

    // --- deletePlayerProgressForHunt: verify both caches cleared ---

    @Nested
    class DeletePlayerProgressForHuntTests {

        @Test
        void deletePlayerProgressForHunt_verifiesExactCacheClears() throws InternalException {
            service.deletePlayerProgressForHunt("myHunt");

            verify(database).deletePlayerProgressForHunt("myHunt");
            verify(storage).clearCachedPlayerHeadsForHunt("myHunt");
            verify(storage).clearCachedTopPlayersForHunt("myHunt");
            verifyNoMoreInteractions(storage);
        }
    }

    // --- resetAllPlayersForHunt: with multiple players ---

    @Nested
    class ResetAllPlayersForHuntMultipleTests {

        @Test
        void resetAllPlayersForHunt_singlePlayer_resetsAndClearsCaches() throws InternalException {
            UUID player = UUID.randomUUID();
            when(database.getAllPlayers()).thenReturn(new ArrayList<>(List.of(player)));

            service.resetAllPlayersForHunt("myHunt");

            verify(database).resetPlayerHunt(player, "myHunt");
            verify(storage).clearCachedPlayerHeadsForHunt("myHunt");
            verify(storage).clearCachedTopPlayersForHunt("myHunt");
        }
    }
}
