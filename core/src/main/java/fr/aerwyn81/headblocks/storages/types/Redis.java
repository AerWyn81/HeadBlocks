package fr.aerwyn81.headblocks.storages.types;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.handlers.ConfigHandler;
import fr.aerwyn81.headblocks.storages.Storage;
import fr.aerwyn81.headblocks.utils.FormatUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Redis implements Storage {
    private final HeadBlocks main;
    private final ConfigHandler configHandler;

    private JedisPool pool;

    private boolean enable;

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
            enable = true;

            HeadBlocks.log.sendMessage(FormatUtils.translate("&aRedis connected!"));
        } catch (Exception ex) {
            HeadBlocks.log.sendMessage(FormatUtils.translate("&cError cannot connect to Redis database : " + ex.getMessage()));
            enable = false;
            main.getStorageHandler().changeToMemory();
        }
    }

    public void close() {
        if (!enable) {
            HeadBlocks.log.sendMessage(FormatUtils.translate("&cError with redis connection, data not persisted!"));
            return;
        }

        pool.close();
    }

    public boolean hasAlreadyClaimedHead(UUID pUuid, UUID hUuid) {
        if (!enable) {
            HeadBlocks.log.sendMessage(FormatUtils.translate("&cError with redis connection, data not persisted!"));
            return false;
        }

        try (Jedis redis = pool.getResource()) {
            return redis.lrange("headblocks:" + pUuid.toString(), 0, -1).stream().anyMatch(e -> e.equals(hUuid.toString()));
        }
    }

    public void savePlayer(UUID pUuid, UUID hUuid) {
        if (!enable) {
            HeadBlocks.log.sendMessage(FormatUtils.translate("&cError with redis connection, data not persisted!"));
            return;
        }

        try (Jedis redis = pool.getResource()) {
            redis.rpush("headblocks:" + pUuid.toString(), hUuid.toString());
        }
    }

    public boolean containsPlayer(UUID pUuid) {
        if (!enable) {
            HeadBlocks.log.sendMessage(FormatUtils.translate("&cError with redis connection, data not persisted!"));
            return false;
        }

        try (Jedis redis = pool.getResource()) {
            return redis.exists("headblocks:" + pUuid.toString());
        }
    }

    public List<UUID> getHeadsPlayer(UUID pUuid) {
        if (!enable) {
            HeadBlocks.log.sendMessage(FormatUtils.translate("&cError with redis connection, data not persisted!"));
            return new ArrayList<>();
        }

        try (Jedis redis = pool.getResource()) {
            return redis.lrange("headblocks:" + pUuid.toString(), 0, -1).stream().map(UUID::fromString).collect(Collectors.toList());
        }
    }

    public void resetPlayer(UUID pUuid) {
        if (!enable) {
            HeadBlocks.log.sendMessage(FormatUtils.translate("&cError with redis connection, data not persisted!"));
            return;
        }

        try (Jedis redis = pool.getResource()) {
            redis.del("headblocks:" + pUuid.toString());
        }
    }

    public void removeHead(UUID hUuid) {
        if (!enable) {
            HeadBlocks.log.sendMessage(FormatUtils.translate("&cError with redis connection, data not persisted!"));
            return;
        }

        try (Jedis redis = pool.getResource()) {
            Set<String> keys = redis.keys("headblocks:*");
            keys.forEach(key -> redis.lrem(key, -1, hUuid.toString()));
        }
    }
}
