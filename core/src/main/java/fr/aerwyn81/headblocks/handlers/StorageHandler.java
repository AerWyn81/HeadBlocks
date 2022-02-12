package fr.aerwyn81.headblocks.handlers;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.databases.Database;
import fr.aerwyn81.headblocks.databases.types.MySQL;
import fr.aerwyn81.headblocks.databases.types.SQLite;
import fr.aerwyn81.headblocks.storages.Storage;
import fr.aerwyn81.headblocks.storages.types.Memory;
import fr.aerwyn81.headblocks.storages.types.Redis;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StorageHandler {

    private final HeadBlocks main;
    private final ConfigHandler configHandler;

    private Storage storage;
    private Database database;

    public StorageHandler(HeadBlocks main) {
        this.main = main;
        this.configHandler = main.getConfigHandler();

        initStorage();
    }

    public void openConnection() {
        database.open();
    }

    public Database getDatabase() {
        return database;
    }

    public Storage getStorage() {
        return storage;
    }

    public void initStorage() {
        if (configHandler.isRedisEnabled()) {
            storage = new Redis(main);
        } else {
            storage = new Memory();
        }

        if (configHandler.isDatabaseEnabled()) {
            database = new MySQL(main);
        } else {
            database = new SQLite(main);
        }
    }

    public void changeToSQLite() {
        database = new SQLite(main);
        database.open();
    }

    public void changeToMemory() {
        storage = new Memory();
        storage.init();
    }

    public boolean hasAlreadyClaimedHead(UUID playerUuid, UUID headUuid) {
        boolean isFoundInRedis = getStorage().hasAlreadyClaimedHead(playerUuid, headUuid);
        if (isFoundInRedis) {
            return true;
        }

        boolean isFoundInDatabase = getDatabase().hasHead(playerUuid, headUuid);
        if (isFoundInDatabase) {
            getStorage().savePlayer(playerUuid, headUuid);
        }

        return isFoundInDatabase;
    }

    public void savePlayer(UUID playerUuid, UUID headUuid) {
        getStorage().savePlayer(playerUuid, headUuid);
        getDatabase().savePlayer(playerUuid, headUuid);
    }

    public boolean containsPlayer(UUID playerUuid) {
        return getStorage().containsPlayer(playerUuid) || getDatabase().containsPlayer(playerUuid);
    }

    public List<UUID> getHeadsPlayer(UUID playerUuid) {
        return getDatabase().getHeadsPlayer(playerUuid);
    }

    public void resetPlayer(UUID playerUuid) {
        getStorage().resetPlayer(playerUuid);
        getDatabase().resetPlayer(playerUuid);
    }

    public void removeHead(UUID headUuid) {
        getStorage().removeHead(headUuid);
        getDatabase().removeHead(headUuid);
    }

    public List<UUID> getAllPlayers() {
        return getDatabase().getAllPlayers();
    }

    public ArrayList<Pair<UUID, Integer>> getTopPlayers(int limit) {
        return getDatabase().getTopPlayers(limit);
    }
}
