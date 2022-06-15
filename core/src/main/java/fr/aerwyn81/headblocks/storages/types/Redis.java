package fr.aerwyn81.headblocks.storages.types;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.handlers.ConfigHandler;
import fr.aerwyn81.headblocks.storages.Storage;
import fr.aerwyn81.headblocks.utils.InternalException;
import fr.aerwyn81.headblocks.utils.MessageUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Redis implements Storage {
    private final HeadBlocks main;
    private final ConfigHandler configHandler;

    private JedisPool pool;

    public Redis(HeadBlocks main) {
        this.main = main;
        this.configHandler = main.getConfigHandler();
    }

    public void init() {
        String password = configHandler.getRedisPassword().trim();

        pool = new JedisPool(new JedisPoolConfig(),
                configHandler.getRedisHostname(),
                configHandler.getRedisPort(),
                Protocol.DEFAULT_TIMEOUT,
                password.isEmpty() ? null : password,
                configHandler.getRedisDatabase(),
                "HeadBlocksPlugin");

        try (Jedis redis = pool.getResource()) {
            redis.keys("headblocks:*");
            HeadBlocks.log.sendMessage(MessageUtils.translate("&aRedis connected!"));
        } catch (Exception ex) {
            HeadBlocks.log.sendMessage(MessageUtils.translate("&cError cannot connect to Redis database : " + ex.getMessage()));
        }
    }

    public void close() {
        pool.close();
    }

    public boolean hasAlreadyClaimedHead(UUID pUuid, UUID hUuid) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            return redis.lrange("headblocks:" + pUuid.toString(), 0, -1).stream().anyMatch(e -> e.equals(hUuid.toString()));
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    public void savePlayer(UUID pUuid, UUID hUuid) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            redis.rpush("headblocks:" + pUuid.toString(), hUuid.toString());
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    public boolean containsPlayer(UUID pUuid) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            return redis.exists("headblocks:" + pUuid.toString());
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    public List<UUID> getHeadsPlayer(UUID pUuid) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            return redis.lrange("headblocks:" + pUuid.toString(), 0, -1).stream().map(UUID::fromString).collect(Collectors.toList());
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    public void resetPlayer(UUID pUuid) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            redis.del("headblocks:" + pUuid.toString());
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    public void removeHead(UUID hUuid) throws InternalException {
        try (Jedis redis = pool.getResource()) {
            Set<String> keys = redis.keys("headblocks:*");
            keys.forEach(key -> redis.lrem(key, -1, hUuid.toString()));
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }
}
