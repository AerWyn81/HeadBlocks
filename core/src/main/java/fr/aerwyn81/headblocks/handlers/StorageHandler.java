package fr.aerwyn81.headblocks.handlers;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.databases.Database;
import fr.aerwyn81.headblocks.databases.types.MySQL;
import fr.aerwyn81.headblocks.databases.types.SQLite;
import fr.aerwyn81.headblocks.storages.Storage;
import fr.aerwyn81.headblocks.storages.types.Memory;
import fr.aerwyn81.headblocks.storages.types.Redis;
import fr.aerwyn81.headblocks.utils.InternalException;
import fr.aerwyn81.headblocks.utils.MessageUtils;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StorageHandler {

    private final HeadBlocks main;
    private final ConfigHandler configHandler;

    private Storage storage;
    private Database database;

    private boolean storageError;

    public StorageHandler(HeadBlocks main) {
        this.main = main;
        this.configHandler = main.getConfigHandler();
        
        this.storageError = false;
    }

    public boolean hasStorageError() {
        return storageError;
    }

    public void init() {
        if (configHandler.isRedisEnabled() && !configHandler.isDatabaseEnabled()) {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError you can't use Redis without setting up an SQL database"));
            storageError = true;
            return;
        }

        if (configHandler.isRedisEnabled()) {
            storage = new Redis(
                    configHandler.getRedisHostname(),
                    configHandler.getRedisPassword(),
                    configHandler.getRedisPort(),
                    configHandler.getRedisDatabase());
        } else {
            storage = new Memory();
        }

        if (configHandler.isDatabaseEnabled()) {
            database = new MySQL(
                    configHandler.getDatabaseUsername(),
                    configHandler.getDatabasePassword(),
                    configHandler.getDatabaseHostname(),
                    configHandler.getDatabasePort(),
                    configHandler.getDatabaseName(),
                    configHandler.getDatabaseSsl());
        } else {
            String pathToDatabase = main.getDataFolder() + "\\headblocks.db";
            database = new SQLite(pathToDatabase);
        }

        try {
            storage.init();
        } catch (InternalException ex) {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to initialize the storage: " + ex.getMessage()));
            storageError = true;
            return;
        }

        try {
            database.open();
            database.load();
        } catch (InternalException ex) {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to connect to the SQL database: " + ex.getMessage()));
            storageError = true;
        }
    }

    public void loadPlayer(Player player) {
        UUID pUuid = player.getUniqueId();
        String playerName = player.getName();

        try {
            boolean isExist = database.containsPlayer(pUuid);

            if (isExist) {
                boolean hasRenamed = main.getStorageHandler().hasPlayerRenamed(pUuid, playerName);

                if (hasRenamed) {
                    main.getStorageHandler().updatePlayerName(pUuid, playerName);
                }

                for (UUID hUuid : database.getHeadsPlayer(pUuid)) {
                    storage.addHead(pUuid, hUuid);
                }
            } else {
                main.getStorageHandler().updatePlayerName(pUuid, playerName);
            }
        } catch (InternalException ex) {
            storageError = true;
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to load player " + playerName + " from SQL database: " + ex.getMessage()));
        }
    }

    public void unloadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();

        try {
            boolean isExist = storage.containsPlayer(uuid);

            if (isExist) {
                storage.resetPlayer(uuid);
            }
        } catch (InternalException ex) {
            storageError = true;
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to unload player " + playerName + " from SQL database: " + ex.getMessage()));
        }
    }

    public void close() {
        try {
            storage.close();
        } catch (InternalException ex) {
            storageError = true;
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to close the REDIS connection : " + ex.getMessage()));
        }

        try {
            database.close();
        } catch (InternalException ex) {
            storageError = true;
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to close the SQL connection : " + ex.getMessage()));
        }
    }

    public boolean hasHead(UUID playerUuid, UUID headUuid) throws InternalException {
        return storage.hasHead(playerUuid, headUuid);
    }

    public void addHead(UUID playerUuid, UUID headUuid) throws InternalException {
        storage.addHead(playerUuid, headUuid);
        database.addHead(playerUuid, headUuid);
    }

    public boolean containsPlayer(UUID playerUuid) throws InternalException {
        return storage.containsPlayer(playerUuid) || database.containsPlayer(playerUuid);
    }

    public List<UUID> getHeadsPlayer(UUID playerUuid) throws InternalException {
        return database.getHeadsPlayer(playerUuid);
    }

    public void resetPlayer(UUID playerUuid) throws InternalException {
        storage.resetPlayer(playerUuid);
        database.resetPlayer(playerUuid);
    }

    public void removeHead(UUID headUuid, boolean withDelete) throws InternalException {
        storage.removeHead(headUuid);
        database.removeHead(headUuid, withDelete);
    }

    public List<UUID> getAllPlayers() throws InternalException {
        return database.getAllPlayers();
    }

    public Map<String, Integer> getTopPlayers(int limit) throws InternalException {
        return database.getTopPlayers(limit);
    }

    public void updatePlayerName(UUID playerUuid, String playerName) throws InternalException {
        database.updatePlayerInfo(playerUuid, playerName);
    }

    public boolean hasPlayerRenamed(UUID playerUuid, String playerName) throws InternalException {
        return database.hasPlayerRenamed(playerUuid, playerName);
    }

    public void createNewHead(UUID headUuid) throws InternalException {
        database.createNewHead(headUuid);
    }

    public boolean isHeadExist(UUID headUuid) throws InternalException {
        return database.isHeadExist(headUuid);
    }
}
