package fr.aerwyn81.headblocks.storages.types;

import fr.aerwyn81.headblocks.storages.Storage;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Redis implements Storage {
    private final String hostname;
    private final String password;
    private final int port;
    private final int redisDatabase;

    private JedisPool pool;

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
            return redis.lrange("headblocks:" + playerUuid.toString(), 0, -1).stream().anyMatch(e -> e.equals(headUuid.toString()));
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public boolean containsPlayer(UUID playerUuid) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            return redis.exists("headblocks:" + playerUuid.toString());
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void addHead(UUID playerUuid, UUID headUuid) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            redis.rpush("headblocks:" + playerUuid.toString(), headUuid.toString());
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void resetPlayer(UUID playerUuid) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            redis.del("headblocks:" + playerUuid.toString());
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void removeHead(UUID headUuid) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            Set<String> keys = redis.keys("headblocks:*");
            keys.forEach(key -> redis.lrem(key, -1, headUuid.toString()));
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public ArrayList<UUID> getHeadsPlayer(UUID pUuid) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            return redis.lrange("headblocks:" + pUuid.toString(), 0, -1).stream().map(UUID::fromString).collect(Collectors.toCollection(ArrayList::new));
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }
}
