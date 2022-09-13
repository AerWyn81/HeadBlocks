package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.databases.Database;
import fr.aerwyn81.headblocks.databases.EnumTypeDatabase;
import fr.aerwyn81.headblocks.databases.Requests;
import fr.aerwyn81.headblocks.databases.types.MySQL;
import fr.aerwyn81.headblocks.databases.types.SQLite;
import fr.aerwyn81.headblocks.storages.Storage;
import fr.aerwyn81.headblocks.storages.types.Memory;
import fr.aerwyn81.headblocks.storages.types.Redis;
import fr.aerwyn81.headblocks.utils.InternalException;
import fr.aerwyn81.headblocks.utils.MessageUtils;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class StorageService {
    private static Storage storage;
    private static Database database;

    private static boolean storageError;

    public static void initialize() {
        storageError = false;

        if (ConfigService.isRedisEnabled() && !ConfigService.isDatabaseEnabled()) {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError you can't use Redis without setting up an SQL database"));
            storageError = true;
            return;
        }

        if (ConfigService.isRedisEnabled()) {
            storage = new Redis(
                    ConfigService.getRedisHostname(),
                    ConfigService.getRedisPassword(),
                    ConfigService.getRedisPort(),
                    ConfigService.getRedisDatabase());
        } else {
            storage = new Memory();
        }

        String pathToDatabase = HeadBlocks.getInstance().getDataFolder() + "\\headblocks.db";
        var isFileExist = new File(pathToDatabase).exists();

        if (ConfigService.isDatabaseEnabled()) {
            database = new MySQL(
                    ConfigService.getDatabaseUsername(),
                    ConfigService.getDatabasePassword(),
                    ConfigService.getDatabaseHostname(),
                    ConfigService.getDatabasePort(),
                    ConfigService.getDatabaseName(),
                    ConfigService.getDatabaseSsl());
        } else {
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

            if (isFileExist) {
                verifyDatabaseMigration();
            }

            database.load();
        } catch (InternalException ex) {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to connect to the SQL database: " + ex.getMessage()));
            storageError = true;
        }

        if (!storageError) {
            if (ConfigService.isDatabaseEnabled()) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&aMySQL storage properly connected!"));
            } else {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&aSQLite storage properly connected!"));
            }
        }
    }

    public static boolean hasStorageError() {
        return storageError;
    }

    private static void verifyDatabaseMigration() throws InternalException {
        int dbVersion = database.checkVersion();

        if (dbVersion == -1) {
            database.migrate();
            dbVersion = database.checkVersion();
        }

        if (dbVersion == 0) {
            database.addTableVersion();
        }
    }

    public static void loadPlayer(Player player) {
        UUID pUuid = player.getUniqueId();
        String playerName = player.getName();

        try {
            boolean isExist = database.containsPlayer(pUuid);

            if (isExist) {
                boolean hasRenamed = hasPlayerRenamed(pUuid, playerName);

                if (hasRenamed) {
                    updatePlayerName(pUuid, playerName);
                }

                for (UUID hUuid : database.getHeadsPlayer(pUuid)) {
                    storage.addHead(pUuid, hUuid);
                }
            } else {
                updatePlayerName(pUuid, playerName);
            }
        } catch (InternalException ex) {
            storageError = true;
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to load player " + playerName + " from SQL database: " + ex.getMessage()));
        }
    }

    public static void unloadPlayer(Player player) {
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

    public static void close() {
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

    public static boolean hasHead(UUID playerUuid, UUID headUuid) throws InternalException {
        return storage.hasHead(playerUuid, headUuid);
    }

    public static void addHead(UUID playerUuid, UUID headUuid) throws InternalException {
        storage.addHead(playerUuid, headUuid);
        database.addHead(playerUuid, headUuid);
    }

    public static boolean containsPlayer(UUID playerUuid) throws InternalException {
        return storage.containsPlayer(playerUuid) || database.containsPlayer(playerUuid);
    }

    public static List<UUID> getHeadsPlayer(UUID playerUuid) throws InternalException {
        return database.getHeadsPlayer(playerUuid);
    }

    public static void resetPlayer(UUID playerUuid) throws InternalException {
        storage.resetPlayer(playerUuid);
        database.resetPlayer(playerUuid);
    }

    public static void removeHead(UUID headUuid, boolean withDelete) throws InternalException {
        storage.removeHead(headUuid);
        database.removeHead(headUuid, withDelete);
    }

    public static List<UUID> getAllPlayers() throws InternalException {
        return database.getAllPlayers();
    }

    public static Map<String, Integer> getTopPlayers(int limit) throws InternalException {
        return database.getTopPlayers(limit);
    }

    public static void updatePlayerName(UUID playerUuid, String playerName) throws InternalException {
        database.updatePlayerInfo(playerUuid, playerName);
    }

    public static boolean hasPlayerRenamed(UUID playerUuid, String playerName) throws InternalException {
        return database.hasPlayerRenamed(playerUuid, playerName);
    }

    public static void createNewHead(UUID headUuid) throws InternalException {
        database.createNewHead(headUuid);
    }

    public static boolean isHeadExist(UUID headUuid) throws InternalException {
        return database.isHeadExist(headUuid);
    }

    public static ArrayList<String> getInstructionsExport(EnumTypeDatabase type) throws InternalException {
        ArrayList<String> instructions = new ArrayList<>();

        // Table : hb_heads
        instructions.add("DROP TABLE IF EXISTS hb_heads;");

        if (type == EnumTypeDatabase.MySQL) {
            instructions.add(Requests.CREATE_TABLE_HEADS_MYSQL + ";");
        } else if (type == EnumTypeDatabase.SQLite) {
            instructions.add(Requests.CREATE_TABLE_HEADS + ";");
        }

        ArrayList<AbstractMap.SimpleEntry<String, Boolean>> heads = database.getHeads();
        for (AbstractMap.SimpleEntry<String, Boolean> head : heads) {
            instructions.add("INSERT INTO hb_heads (hUUID, hExist) VALUES ('" + head.getKey() +
                    "', " + (head.getValue() ? 1 : 0) + ");");
        }

        instructions.add("");

        // Table : hb_playerHeads
        instructions.add("DROP TABLE IF EXISTS hb_playerHeads;");

        if (type == EnumTypeDatabase.MySQL) {
            instructions.add(Requests.CREATE_TABLE_PLAYERHEADS_MYSQL + ";");
        } else if (type == EnumTypeDatabase.SQLite) {
            instructions.add(Requests.CREATE_TABLE_PLAYERHEADS + ";");
        }

        ArrayList<AbstractMap.SimpleEntry<String, String>> playerHeads = database.getPlayerHeads();
        for (AbstractMap.SimpleEntry<String, String> pHead : playerHeads) {
            instructions.add("INSERT INTO hb_playerHeads (pUUID, hUUID) VALUES ('" + pHead.getKey() +
                    "', '" + pHead.getValue() + "');");
        }

        instructions.add("");

        // Table : hb_players
        instructions.add("DROP TABLE IF EXISTS hb_players;");

        if (type == EnumTypeDatabase.MySQL) {
            instructions.add(Requests.CREATE_TABLE_PLAYERS_MYSQL + ";");
        } else if (type == EnumTypeDatabase.SQLite) {
            instructions.add(Requests.CREATE_TABLE_PLAYERS + ";");
        }

        ArrayList<AbstractMap.SimpleEntry<String, String>> players = database.getPlayers();
        for (AbstractMap.SimpleEntry<String, String> player : players) {
            instructions.add("INSERT INTO hb_players (pUUID, pName) VALUES ('" + player.getKey() + "', '" + player.getValue() + "');");
        }

        instructions.add("");

        // Table : hb_version
        instructions.add("DROP TABLE IF EXISTS hb_version;");
        instructions.add(Requests.CREATE_TABLE_VERSION + ";");
        instructions.add(Requests.INSERT_VERSION.replaceAll("\\?", String.valueOf(database.version)) + ";");

        return instructions;
    }
}