package fr.aerwyn81.headblocks.storages.types;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.storages.Storage;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Redis implements Storage {
    private final String hostname;
    private final String password;
    private final int port;
    private final int redisDatabase;

    private JedisPool pool;
    private final Gson gson = new Gson();

    private static final String KEY_PLAYER_HEADS = "headblocks:playerheads:";
    private static final String KEY_CACHE_TOP_PLAYERS = "headblocks:cache:topplayers";
    private static final String KEY_CACHE_HEADS = "headblocks:cache:heads";
    private static final String KEY_HUNT_VERSION = "headblocks:hunts:version";

    // Hunt-specific cache keys
    private static final String KEY_CACHE_HUNT_PLAYER_HEADS = "headblocks:cache:hunt:playerheads:";
    private static final String KEY_CACHE_HUNT_TOP_PLAYERS = "headblocks:cache:hunt:topplayers:";
    private static final String KEY_CACHE_HUNT_TIMED_LB = "headblocks:cache:hunt:timedlb:";
    private static final String KEY_CACHE_HUNT_BEST_TIME = "headblocks:cache:hunt:besttime:";
    private static final String KEY_CACHE_HUNT_TIMED_COUNT = "headblocks:cache:hunt:timedcount:";

    public Redis(String hostname, String password, int port, int redisDatabase) {
        this.hostname = hostname;
        this.password = password;
        this.port = port;
        this.redisDatabase = redisDatabase;
    }

    @Override
    public void init() throws InternalException {
        pool = new JedisPool(new JedisPoolConfig(), hostname, port, Protocol.DEFAULT_TIMEOUT, password.isEmpty() ? null : password, redisDatabase, "HeadBlocksPlugin");

        try (Jedis redis = pool.getResource()) {
            if (redis.isConnected()) {
                redis.keys("headblocks:*");
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void close() throws InternalException {
        pool.close();
    }

    @Override
    public boolean hasHead(UUID playerUuid, UUID headUuid) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            return redis.sismember(KEY_PLAYER_HEADS + playerUuid.toString(), headUuid.toString());
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public boolean containsPlayer(UUID playerUuid) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            return redis.exists(KEY_PLAYER_HEADS + playerUuid.toString());
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void addHead(UUID playerUuid, UUID headUuid) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            redis.sadd(KEY_PLAYER_HEADS + playerUuid.toString(), headUuid.toString());
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void resetPlayer(UUID playerUuid) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            redis.del(KEY_PLAYER_HEADS + playerUuid.toString());
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void resetPlayerHead(UUID playerUuid, UUID headUuid) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            redis.srem(KEY_PLAYER_HEADS + playerUuid.toString(), headUuid.toString());
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void removeHead(UUID headUuid) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            Set<String> keys = redis.keys(KEY_PLAYER_HEADS + "*");
            keys.forEach(key -> redis.srem(key, headUuid.toString()));
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public ArrayList<UUID> getHeadsPlayer(UUID pUuid) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            Set<String> members = redis.smembers(KEY_PLAYER_HEADS + pUuid.toString());
            return members.stream().map(UUID::fromString).collect(Collectors.toCollection(ArrayList::new));
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public Set<UUID> getCachedPlayerHeads(UUID playerUuid) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            Set<String> members = redis.smembers(KEY_PLAYER_HEADS + playerUuid.toString());
            if (members == null || members.isEmpty()) {
                return null;
            }
            return members.stream().map(UUID::fromString).collect(Collectors.toCollection(ConcurrentHashMap::newKeySet));
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void setCachedPlayerHeads(UUID playerUuid, Set<UUID> heads) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            String key = KEY_PLAYER_HEADS + playerUuid.toString();
            redis.del(key);
            if (heads != null && !heads.isEmpty()) {
                String[] headArray = heads.stream().map(UUID::toString).toArray(String[]::new);
                redis.sadd(key, headArray);
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void addCachedPlayerHead(UUID playerUuid, UUID headUuid) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            redis.sadd(KEY_PLAYER_HEADS + playerUuid.toString(), headUuid.toString());
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void removeCachedPlayerHeads(UUID playerUuid) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            redis.del(KEY_PLAYER_HEADS + playerUuid.toString());
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public LinkedHashMap<PlayerProfileLight, Integer> getCachedTopPlayers() throws InternalException {
        try (Jedis redis = pool.getResource()) {
            String json = redis.get(KEY_CACHE_TOP_PLAYERS);
            if (json == null || json.isEmpty()) {
                return new LinkedHashMap<>();
            }
            Type type = new TypeToken<LinkedHashMap<PlayerProfileLight, Integer>>() {
            }.getType();
            return gson.fromJson(json, type);
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void setCachedTopPlayers(LinkedHashMap<PlayerProfileLight, Integer> topPlayers) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            String json = gson.toJson(topPlayers);
            redis.set(KEY_CACHE_TOP_PLAYERS, json);
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void clearCachedTopPlayers() throws InternalException {
        try (Jedis redis = pool.getResource()) {
            redis.del(KEY_CACHE_TOP_PLAYERS);
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public Set<UUID> getCachedHeads() throws InternalException {
        try (Jedis redis = pool.getResource()) {
            Set<String> members = redis.smembers(KEY_CACHE_HEADS);
            if (members == null || members.isEmpty()) {
                return ConcurrentHashMap.newKeySet();
            }
            return members.stream().map(UUID::fromString).collect(Collectors.toCollection(ConcurrentHashMap::newKeySet));
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void addCachedHead(UUID headUuid) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            redis.sadd(KEY_CACHE_HEADS, headUuid.toString());
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void removeCachedHead(UUID headUuid) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            redis.srem(KEY_CACHE_HEADS, headUuid.toString());

            Set<String> keys = redis.keys(KEY_PLAYER_HEADS + "*");
            keys.forEach(key -> redis.srem(key, headUuid.toString()));

            redis.del(KEY_CACHE_TOP_PLAYERS);
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    // --- Hunt-specific cache: player heads per hunt ---

    @Override
    public Set<UUID> getCachedPlayerHeadsForHunt(UUID playerUuid, String huntId) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            String key = KEY_CACHE_HUNT_PLAYER_HEADS + huntId + ":" + playerUuid.toString();
            if (!redis.exists(key)) {
                return null;
            }
            Set<String> members = redis.smembers(key);
            if (members.size() == 1 && members.contains("EMPTY")) {
                return ConcurrentHashMap.newKeySet();
            }
            return members.stream()
                    .filter(s -> !"EMPTY".equals(s))
                    .map(UUID::fromString)
                    .collect(Collectors.toCollection(ConcurrentHashMap::newKeySet));
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void setCachedPlayerHeadsForHunt(UUID playerUuid, String huntId, Set<UUID> heads) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            String key = KEY_CACHE_HUNT_PLAYER_HEADS + huntId + ":" + playerUuid.toString();
            redis.del(key);
            if (heads != null && !heads.isEmpty()) {
                String[] headArray = heads.stream().map(UUID::toString).toArray(String[]::new);
                redis.sadd(key, headArray);
            } else {
                // Store empty marker so we distinguish "cached empty" from "not cached"
                redis.sadd(key, "EMPTY");
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void addCachedPlayerHeadForHunt(UUID playerUuid, String huntId, UUID headUuid) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            String key = KEY_CACHE_HUNT_PLAYER_HEADS + huntId + ":" + playerUuid.toString();
            if (redis.exists(key)) {
                redis.srem(key, "EMPTY");
                redis.sadd(key, headUuid.toString());
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void removeCachedPlayerHeadsForHunt(UUID playerUuid, String huntId) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            redis.del(KEY_CACHE_HUNT_PLAYER_HEADS + huntId + ":" + playerUuid.toString());
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void clearCachedPlayerHeadsForHunt(String huntId) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            Set<String> keys = redis.keys(KEY_CACHE_HUNT_PLAYER_HEADS + huntId + ":*");
            if (keys != null && !keys.isEmpty()) {
                redis.del(keys.toArray(new String[0]));
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void clearAllCachedHuntDataForPlayer(UUID playerUuid) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            String suffix = ":" + playerUuid.toString();
            Set<String> keys = redis.keys(KEY_CACHE_HUNT_PLAYER_HEADS + "*" + suffix);
            Set<String> bestTimeKeys = redis.keys(KEY_CACHE_HUNT_BEST_TIME + "*" + suffix);
            Set<String> runCountKeys = redis.keys(KEY_CACHE_HUNT_TIMED_COUNT + "*" + suffix);
            if (bestTimeKeys != null) keys.addAll(bestTimeKeys);
            if (runCountKeys != null) keys.addAll(runCountKeys);
            if (!keys.isEmpty()) {
                redis.del(keys.toArray(new String[0]));
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    // --- Hunt-specific cache: top players per hunt ---

    @Override
    public LinkedHashMap<PlayerProfileLight, Integer> getCachedTopPlayersForHunt(String huntId) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            String json = redis.get(KEY_CACHE_HUNT_TOP_PLAYERS + huntId);
            if (json == null || json.isEmpty()) {
                return null;
            }
            Type type = new TypeToken<LinkedHashMap<PlayerProfileLight, Integer>>() {
            }.getType();
            return gson.fromJson(json, type);
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void setCachedTopPlayersForHunt(String huntId, LinkedHashMap<PlayerProfileLight, Integer> topPlayers) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            redis.set(KEY_CACHE_HUNT_TOP_PLAYERS + huntId, gson.toJson(topPlayers));
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void clearCachedTopPlayersForHunt(String huntId) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            redis.del(KEY_CACHE_HUNT_TOP_PLAYERS + huntId);
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void clearAllCachedTopPlayersForHunt() throws InternalException {
        try (Jedis redis = pool.getResource()) {
            Set<String> keys = redis.keys(KEY_CACHE_HUNT_TOP_PLAYERS + "*");
            if (keys != null && !keys.isEmpty()) {
                redis.del(keys.toArray(new String[0]));
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    // --- Hunt-specific cache: timed leaderboard ---

    @Override
    public LinkedHashMap<PlayerProfileLight, Long> getCachedTimedLeaderboard(String huntId) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            String json = redis.get(KEY_CACHE_HUNT_TIMED_LB + huntId);
            if (json == null || json.isEmpty()) {
                return null;
            }
            Type type = new TypeToken<LinkedHashMap<PlayerProfileLight, Long>>() {
            }.getType();
            return gson.fromJson(json, type);
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void setCachedTimedLeaderboard(String huntId, LinkedHashMap<PlayerProfileLight, Long> lb) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            redis.set(KEY_CACHE_HUNT_TIMED_LB + huntId, gson.toJson(lb));
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void clearCachedTimedLeaderboard(String huntId) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            redis.del(KEY_CACHE_HUNT_TIMED_LB + huntId);
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    // --- Hunt-specific cache: best time ---

    @Override
    public Long getCachedBestTime(UUID playerUuid, String huntId) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            String val = redis.get(KEY_CACHE_HUNT_BEST_TIME + huntId + ":" + playerUuid.toString());
            if (val == null) {
                return null;
            }
            return Long.parseLong(val);
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void setCachedBestTime(UUID playerUuid, String huntId, Long timeMs) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            redis.set(KEY_CACHE_HUNT_BEST_TIME + huntId + ":" + playerUuid.toString(), timeMs.toString());
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void clearCachedBestTime(UUID playerUuid, String huntId) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            redis.del(KEY_CACHE_HUNT_BEST_TIME + huntId + ":" + playerUuid.toString());
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    // --- Hunt-specific cache: run count ---

    @Override
    public Integer getCachedTimedRunCount(UUID playerUuid, String huntId) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            String val = redis.get(KEY_CACHE_HUNT_TIMED_COUNT + huntId + ":" + playerUuid.toString());
            if (val == null) {
                return null;
            }
            return Integer.parseInt(val);
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void setCachedTimedRunCount(UUID playerUuid, String huntId, int count) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            redis.set(KEY_CACHE_HUNT_TIMED_COUNT + huntId + ":" + playerUuid.toString(), String.valueOf(count));
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void clearCachedTimedRunCount(UUID playerUuid, String huntId) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            redis.del(KEY_CACHE_HUNT_TIMED_COUNT + huntId + ":" + playerUuid.toString());
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public long getHuntVersion() throws InternalException {
        try (Jedis redis = pool.getResource()) {
            String val = redis.get(KEY_HUNT_VERSION);
            return val != null ? Long.parseLong(val) : 0;
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void incrementHuntVersion() throws InternalException {
        try (Jedis redis = pool.getResource()) {
            redis.incr(KEY_HUNT_VERSION);
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }
}
