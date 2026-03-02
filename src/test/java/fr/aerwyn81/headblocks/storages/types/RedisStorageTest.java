package fr.aerwyn81.headblocks.storages.types;

import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisStorageTest {

    @Mock
    private JedisPool pool;

    @Mock
    private Jedis jedis;

    private Redis storage;

    @BeforeEach
    void setUp() throws Exception {
        storage = new Redis("localhost", "", 6379, 0);

        Field poolField = Redis.class.getDeclaredField("pool");
        poolField.setAccessible(true);
        poolField.set(storage, pool);

        lenient().when(pool.getResource()).thenReturn(jedis);
    }

    // ---- Player heads: addHead ----

    @Test
    void addHead_calls_sadd_with_correct_key() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();

        storage.addHead(player, head);

        verify(jedis).sadd("headblocks:playerheads:" + player, head.toString());
    }

    // ---- Player heads: hasHead ----

    @Test
    void hasHead_calls_sismember_and_returns_true_when_member_exists() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();
        when(jedis.sismember("headblocks:playerheads:" + player, head.toString())).thenReturn(true);

        boolean result = storage.hasHead(player, head);

        assertThat(result).isTrue();
    }

    @Test
    void hasHead_returns_false_when_member_does_not_exist() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();
        when(jedis.sismember("headblocks:playerheads:" + player, head.toString())).thenReturn(false);

        boolean result = storage.hasHead(player, head);

        assertThat(result).isFalse();
    }

    // ---- Player heads: getHeadsPlayer ----

    @Test
    void getHeadsPlayer_calls_smembers_and_maps_uuids() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head1 = UUID.randomUUID();
        UUID head2 = UUID.randomUUID();
        when(jedis.smembers("headblocks:playerheads:" + player)).thenReturn(Set.of(head1.toString(), head2.toString()));

        var result = storage.getHeadsPlayer(player);

        assertThat(result).containsExactlyInAnyOrder(head1, head2);
    }

    // ---- Player heads: resetPlayer ----

    @Test
    void resetPlayer_calls_del_with_correct_key() throws InternalException {
        UUID player = UUID.randomUUID();

        storage.resetPlayer(player);

        verify(jedis).del("headblocks:playerheads:" + player);
    }

    // ---- Player heads: resetPlayerHead ----

    @Test
    void resetPlayerHead_calls_srem_with_correct_key_and_value() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();

        storage.resetPlayerHead(player, head);

        verify(jedis).srem("headblocks:playerheads:" + player, head.toString());
    }

    // ---- Player heads: containsPlayer ----

    @Test
    void containsPlayer_calls_exists_and_returns_true() throws InternalException {
        UUID player = UUID.randomUUID();
        when(jedis.exists("headblocks:playerheads:" + player)).thenReturn(true);

        boolean result = storage.containsPlayer(player);

        assertThat(result).isTrue();
    }

    @Test
    void containsPlayer_returns_false_when_key_does_not_exist() throws InternalException {
        UUID player = UUID.randomUUID();
        when(jedis.exists("headblocks:playerheads:" + player)).thenReturn(false);

        boolean result = storage.containsPlayer(player);

        assertThat(result).isFalse();
    }

    // ---- Player heads: removeHead ----

    @Test
    void removeHead_calls_keys_then_srem_for_each_matching_key() throws InternalException {
        UUID head = UUID.randomUUID();
        Set<String> keys = new HashSet<>(Set.of("headblocks:playerheads:p1", "headblocks:playerheads:p2"));
        when(jedis.keys("headblocks:playerheads:*")).thenReturn(keys);

        storage.removeHead(head);

        verify(jedis).srem("headblocks:playerheads:p1", head.toString());
        verify(jedis).srem("headblocks:playerheads:p2", head.toString());
    }

    // ---- Cached player heads: getCachedPlayerHeads ----

    @Test
    void getCachedPlayerHeads_returns_null_when_smembers_is_empty() throws InternalException {
        UUID player = UUID.randomUUID();
        when(jedis.smembers("headblocks:playerheads:" + player)).thenReturn(Set.of());

        Set<UUID> result = storage.getCachedPlayerHeads(player);

        assertThat(result).isNull();
    }

    @Test
    void getCachedPlayerHeads_maps_members_to_uuids() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head1 = UUID.randomUUID();
        UUID head2 = UUID.randomUUID();
        when(jedis.smembers("headblocks:playerheads:" + player)).thenReturn(Set.of(head1.toString(), head2.toString()));

        Set<UUID> result = storage.getCachedPlayerHeads(player);

        assertThat(result).containsExactlyInAnyOrder(head1, head2);
    }

    // ---- Cached player heads: setCachedPlayerHeads ----

    @Test
    void setCachedPlayerHeads_deletes_then_sadds() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head1 = UUID.randomUUID();
        UUID head2 = UUID.randomUUID();
        Set<UUID> heads = Set.of(head1, head2);

        storage.setCachedPlayerHeads(player, heads);

        var inOrder = inOrder(jedis);
        inOrder.verify(jedis).del("headblocks:playerheads:" + player);
        inOrder.verify(jedis).sadd(eq("headblocks:playerheads:" + player), any(String[].class));
    }

    @Test
    void setCachedPlayerHeads_with_empty_set_only_deletes() throws InternalException {
        UUID player = UUID.randomUUID();

        storage.setCachedPlayerHeads(player, Set.of());

        verify(jedis).del("headblocks:playerheads:" + player);
        verify(jedis, never()).sadd(any(), any(String[].class));
    }

    // ---- Cached player heads: addCachedPlayerHead ----

    @Test
    void addCachedPlayerHead_calls_sadd() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();

        storage.addCachedPlayerHead(player, head);

        verify(jedis).sadd("headblocks:playerheads:" + player, head.toString());
    }

    // ---- Cached player heads: removeCachedPlayerHeads ----

    @Test
    void removeCachedPlayerHeads_calls_del() throws InternalException {
        UUID player = UUID.randomUUID();

        storage.removeCachedPlayerHeads(player);

        verify(jedis).del("headblocks:playerheads:" + player);
    }

    // ---- Top players: getCachedTopPlayers ----

    @Test
    void getCachedTopPlayers_returns_empty_map_when_json_is_empty() throws InternalException {
        when(jedis.get("headblocks:cache:topplayers")).thenReturn("");

        LinkedHashMap<PlayerProfileLight, Integer> result = storage.getCachedTopPlayers();

        assertThat(result).isEmpty();
    }

    @Test
    void getCachedTopPlayers_returns_empty_map_when_json_is_null() throws InternalException {
        when(jedis.get("headblocks:cache:topplayers")).thenReturn(null);

        LinkedHashMap<PlayerProfileLight, Integer> result = storage.getCachedTopPlayers();

        assertThat(result).isEmpty();
    }

    // ---- Top players: setCachedTopPlayers ----

    @Test
    void setCachedTopPlayers_calls_set_with_json() throws InternalException {
        LinkedHashMap<PlayerProfileLight, Integer> top = new LinkedHashMap<>();

        storage.setCachedTopPlayers(top);

        verify(jedis).set(eq("headblocks:cache:topplayers"), any(String.class));
    }

    // ---- Top players: clearCachedTopPlayers ----

    @Test
    void clearCachedTopPlayers_calls_del() throws InternalException {
        storage.clearCachedTopPlayers();

        verify(jedis).del("headblocks:cache:topplayers");
    }

    // ---- Cached heads ----

    @Test
    void addCachedHead_calls_sadd() throws InternalException {
        UUID head = UUID.randomUUID();

        storage.addCachedHead(head);

        verify(jedis).sadd("headblocks:cache:heads", head.toString());
    }

    @Test
    void getCachedHeads_returns_uuids_from_smembers() throws InternalException {
        UUID head = UUID.randomUUID();
        when(jedis.smembers("headblocks:cache:heads")).thenReturn(Set.of(head.toString()));

        Set<UUID> result = storage.getCachedHeads();

        assertThat(result).containsExactly(head);
    }

    @Test
    void getCachedHeads_returns_empty_set_when_no_members() throws InternalException {
        when(jedis.smembers("headblocks:cache:heads")).thenReturn(Set.of());

        Set<UUID> result = storage.getCachedHeads();

        assertThat(result).isEmpty();
    }

    @Test
    void removeCachedHead_removes_from_heads_and_player_heads_and_top_players() throws InternalException {
        UUID head = UUID.randomUUID();
        Set<String> playerKeys = new HashSet<>(Set.of("headblocks:playerheads:p1"));
        when(jedis.keys("headblocks:playerheads:*")).thenReturn(playerKeys);

        storage.removeCachedHead(head);

        verify(jedis).srem("headblocks:cache:heads", head.toString());
        verify(jedis).srem("headblocks:playerheads:p1", head.toString());
        verify(jedis).del("headblocks:cache:topplayers");
    }

    // ---- Hunt player heads ----

    @Test
    void getCachedPlayerHeadsForHunt_returns_null_when_key_not_exists() throws InternalException {
        UUID player = UUID.randomUUID();
        String huntId = "hunt1";
        String key = "headblocks:cache:hunt:playerheads:" + huntId + ":" + player;
        when(jedis.exists(key)).thenReturn(false);

        Set<UUID> result = storage.getCachedPlayerHeadsForHunt(player, huntId);

        assertThat(result).isNull();
    }

    @Test
    void getCachedPlayerHeadsForHunt_returns_empty_set_when_only_EMPTY_marker() throws InternalException {
        UUID player = UUID.randomUUID();
        String huntId = "hunt1";
        String key = "headblocks:cache:hunt:playerheads:" + huntId + ":" + player;
        when(jedis.exists(key)).thenReturn(true);
        when(jedis.smembers(key)).thenReturn(Set.of("EMPTY"));

        Set<UUID> result = storage.getCachedPlayerHeadsForHunt(player, huntId);

        assertThat(result).isEmpty();
    }

    @Test
    void getCachedPlayerHeadsForHunt_returns_uuids_when_members_exist() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();
        String huntId = "hunt1";
        String key = "headblocks:cache:hunt:playerheads:" + huntId + ":" + player;
        when(jedis.exists(key)).thenReturn(true);
        when(jedis.smembers(key)).thenReturn(Set.of(head.toString()));

        Set<UUID> result = storage.getCachedPlayerHeadsForHunt(player, huntId);

        assertThat(result).containsExactly(head);
    }

    @Test
    void addCachedPlayerHeadForHunt_removes_EMPTY_marker_when_key_exists() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();
        String huntId = "hunt1";
        String key = "headblocks:cache:hunt:playerheads:" + huntId + ":" + player;
        when(jedis.exists(key)).thenReturn(true);

        storage.addCachedPlayerHeadForHunt(player, huntId, head);

        verify(jedis).srem(key, "EMPTY");
        verify(jedis).sadd(key, head.toString());
    }

    @Test
    void addCachedPlayerHeadForHunt_does_nothing_when_key_not_exists() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();
        String huntId = "hunt1";
        String key = "headblocks:cache:hunt:playerheads:" + huntId + ":" + player;
        when(jedis.exists(key)).thenReturn(false);

        storage.addCachedPlayerHeadForHunt(player, huntId, head);

        verify(jedis, never()).sadd(any(), any(String.class));
    }

    @Test
    void clearCachedPlayerHeadsForHunt_uses_keys_pattern_and_deletes_all() throws InternalException {
        String huntId = "hunt1";
        Set<String> keys = new HashSet<>(Set.of("headblocks:cache:hunt:playerheads:hunt1:p1", "headblocks:cache:hunt:playerheads:hunt1:p2"));
        when(jedis.keys("headblocks:cache:hunt:playerheads:" + huntId + ":*")).thenReturn(keys);

        storage.clearCachedPlayerHeadsForHunt(huntId);

        verify(jedis).del(any(String[].class));
    }

    @Test
    void clearCachedPlayerHeadsForHunt_does_nothing_when_no_keys_match() throws InternalException {
        String huntId = "hunt1";
        when(jedis.keys("headblocks:cache:hunt:playerheads:" + huntId + ":*")).thenReturn(Set.of());

        storage.clearCachedPlayerHeadsForHunt(huntId);

        verify(jedis, never()).del(any(String[].class));
    }

    @Test
    void removeCachedPlayerHeadsForHunt_calls_del_with_correct_key() throws InternalException {
        UUID player = UUID.randomUUID();
        String huntId = "hunt1";

        storage.removeCachedPlayerHeadsForHunt(player, huntId);

        verify(jedis).del("headblocks:cache:hunt:playerheads:" + huntId + ":" + player);
    }

    // ---- Hunt top players ----

    @Test
    void setCachedTopPlayersForHunt_calls_set_with_json() throws InternalException {
        LinkedHashMap<PlayerProfileLight, Integer> top = new LinkedHashMap<>();
        String huntId = "hunt1";

        storage.setCachedTopPlayersForHunt(huntId, top);

        verify(jedis).set(eq("headblocks:cache:hunt:topplayers:" + huntId), any(String.class));
    }

    @Test
    void getCachedTopPlayersForHunt_returns_null_when_json_is_null() throws InternalException {
        String huntId = "hunt1";
        when(jedis.get("headblocks:cache:hunt:topplayers:" + huntId)).thenReturn(null);

        var result = storage.getCachedTopPlayersForHunt(huntId);

        assertThat(result).isNull();
    }

    @Test
    void getCachedTopPlayersForHunt_returns_null_when_json_is_empty() throws InternalException {
        String huntId = "hunt1";
        when(jedis.get("headblocks:cache:hunt:topplayers:" + huntId)).thenReturn("");

        var result = storage.getCachedTopPlayersForHunt(huntId);

        assertThat(result).isNull();
    }

    @Test
    void clearCachedTopPlayersForHunt_calls_del_with_hunt_key() throws InternalException {
        String huntId = "hunt1";

        storage.clearCachedTopPlayersForHunt(huntId);

        verify(jedis).del("headblocks:cache:hunt:topplayers:" + huntId);
    }

    // ---- Timed leaderboard cache ----

    @Test
    void setCachedTimedLeaderboard_calls_set_with_json() throws InternalException {
        LinkedHashMap<PlayerProfileLight, Long> lb = new LinkedHashMap<>();
        String huntId = "hunt1";

        storage.setCachedTimedLeaderboard(huntId, lb);

        verify(jedis).set(eq("headblocks:cache:hunt:timedlb:" + huntId), any(String.class));
    }

    @Test
    void getCachedTimedLeaderboard_returns_null_when_json_is_null() throws InternalException {
        String huntId = "hunt1";
        when(jedis.get("headblocks:cache:hunt:timedlb:" + huntId)).thenReturn(null);

        var result = storage.getCachedTimedLeaderboard(huntId);

        assertThat(result).isNull();
    }

    @Test
    void clearCachedTimedLeaderboard_calls_del() throws InternalException {
        String huntId = "hunt1";

        storage.clearCachedTimedLeaderboard(huntId);

        verify(jedis).del("headblocks:cache:hunt:timedlb:" + huntId);
    }

    // ---- Best time cache ----

    @Test
    void getCachedBestTime_returns_null_when_value_is_null() throws InternalException {
        UUID player = UUID.randomUUID();
        String huntId = "hunt1";
        when(jedis.get("headblocks:cache:hunt:besttime:" + huntId + ":" + player)).thenReturn(null);

        Long result = storage.getCachedBestTime(player, huntId);

        assertThat(result).isNull();
    }

    @Test
    void getCachedBestTime_returns_parsed_long_when_value_exists() throws InternalException {
        UUID player = UUID.randomUUID();
        String huntId = "hunt1";
        when(jedis.get("headblocks:cache:hunt:besttime:" + huntId + ":" + player)).thenReturn("5000");

        Long result = storage.getCachedBestTime(player, huntId);

        assertThat(result).isEqualTo(5000L);
    }

    @Test
    void setCachedBestTime_calls_set_with_string_value() throws InternalException {
        UUID player = UUID.randomUUID();
        String huntId = "hunt1";

        storage.setCachedBestTime(player, huntId, 3000L);

        verify(jedis).set("headblocks:cache:hunt:besttime:" + huntId + ":" + player, "3000");
    }

    @Test
    void clearCachedBestTime_calls_del() throws InternalException {
        UUID player = UUID.randomUUID();
        String huntId = "hunt1";

        storage.clearCachedBestTime(player, huntId);

        verify(jedis).del("headblocks:cache:hunt:besttime:" + huntId + ":" + player);
    }

    // ---- Timed run count cache ----

    @Test
    void getCachedTimedRunCount_returns_null_when_value_is_null() throws InternalException {
        UUID player = UUID.randomUUID();
        String huntId = "hunt1";
        when(jedis.get("headblocks:cache:hunt:timedcount:" + huntId + ":" + player)).thenReturn(null);

        Integer result = storage.getCachedTimedRunCount(player, huntId);

        assertThat(result).isNull();
    }

    @Test
    void getCachedTimedRunCount_returns_parsed_int_when_value_exists() throws InternalException {
        UUID player = UUID.randomUUID();
        String huntId = "hunt1";
        when(jedis.get("headblocks:cache:hunt:timedcount:" + huntId + ":" + player)).thenReturn("7");

        Integer result = storage.getCachedTimedRunCount(player, huntId);

        assertThat(result).isEqualTo(7);
    }

    @Test
    void setCachedTimedRunCount_calls_set_with_string_value() throws InternalException {
        UUID player = UUID.randomUUID();
        String huntId = "hunt1";

        storage.setCachedTimedRunCount(player, huntId, 4);

        verify(jedis).set("headblocks:cache:hunt:timedcount:" + huntId + ":" + player, "4");
    }

    @Test
    void clearCachedTimedRunCount_calls_del() throws InternalException {
        UUID player = UUID.randomUUID();
        String huntId = "hunt1";

        storage.clearCachedTimedRunCount(player, huntId);

        verify(jedis).del("headblocks:cache:hunt:timedcount:" + huntId + ":" + player);
    }

    // ---- Hunt version ----

    @Test
    void getHuntVersion_returns_zero_when_value_is_null() throws InternalException {
        when(jedis.get("headblocks:hunts:version")).thenReturn(null);

        long result = storage.getHuntVersion();

        assertThat(result).isEqualTo(0L);
    }

    @Test
    void getHuntVersion_returns_parsed_long_when_value_exists() throws InternalException {
        when(jedis.get("headblocks:hunts:version")).thenReturn("42");

        long result = storage.getHuntVersion();

        assertThat(result).isEqualTo(42L);
    }

    @Test
    void incrementHuntVersion_calls_incr() throws InternalException {
        storage.incrementHuntVersion();

        verify(jedis).incr("headblocks:hunts:version");
    }
}
