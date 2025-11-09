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
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }
}
