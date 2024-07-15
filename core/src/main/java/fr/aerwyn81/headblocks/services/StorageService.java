package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.databases.Database;
import fr.aerwyn81.headblocks.databases.EnumTypeDatabase;
import fr.aerwyn81.headblocks.databases.Requests;
import fr.aerwyn81.headblocks.databases.types.MariaDB;
import fr.aerwyn81.headblocks.databases.types.MySQL;
import fr.aerwyn81.headblocks.databases.types.SQLite;
import fr.aerwyn81.headblocks.storages.Storage;
import fr.aerwyn81.headblocks.storages.types.Memory;
import fr.aerwyn81.headblocks.storages.types.Redis;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class StorageService {
    private static Storage storage;
    private static Database database;

    private static boolean storageError;

    private static ConcurrentHashMap<UUID, List<UUID>> _cacheHeads;
    private static LinkedHashMap<PlayerProfileLight, Integer> _cacheTop;

    public static void initialize() {
        _cacheHeads = new ConcurrentHashMap<>();
        _cacheTop = new LinkedHashMap<>();

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

        String pathToDatabase = HeadBlocks.getInstance().getDataFolder() + File.separator + "headblocks.db";
        var isFileExist = new File(pathToDatabase).exists();

        if (ConfigService.isDatabaseEnabled()) {
            if (ConfigService.getDatabaseType() == EnumTypeDatabase.MariaDB) {
                database = new MariaDB(
                        ConfigService.getDatabaseUsername(),
                        ConfigService.getDatabasePassword(),
                        ConfigService.getDatabaseHostname(),
                        ConfigService.getDatabasePort(),
                        ConfigService.getDatabaseName(),
                        ConfigService.getDatabaseSsl());
            } else {
                database = new MySQL(
                        ConfigService.getDatabaseUsername(),
                        ConfigService.getDatabasePassword(),
                        ConfigService.getDatabaseHostname(),
                        ConfigService.getDatabasePort(),
                        ConfigService.getDatabaseName(),
                        ConfigService.getDatabaseSsl());
            }
        } else {
            database = new SQLite(pathToDatabase);
        }

        try {
            storage.init();

            if (ConfigService.isRedisEnabled()) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&aRedis cache properly connected!"));
            }
        } catch (InternalException ex) {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to initialize the storage: " + ex.getMessage()));
            storageError = true;
            return;
        }

        try {
            database.open();

            if (database instanceof MySQL || isFileExist) {
                verifyDatabaseMigration();
            }

            database.load();
        } catch (InternalException ex) {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to connect to the " + ConfigService.getDatabaseType() + " database: " + ex.getMessage()));
            storageError = true;
        }

        if (!storageError) {
            if (ConfigService.isDatabaseEnabled()) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&a" + ConfigService.getDatabaseType() + " storage properly connected!"));
            } else {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&aSQLite storage properly connected!"));
            }
        }
    }

    public static boolean hasStorageError() {
        return storageError;
    }

    private static void verifyDatabaseMigration() throws InternalException {
        if (!database.isDefaultTablesExist()) {
            return;
        }

        int dbVersion = database.checkVersion();

        if (dbVersion == database.version) {
            return;
        }

        if (database instanceof SQLite) {
            var backup = backupDatabase();
            if (!backup) {
                storageError = true;
            }
        }

        if (dbVersion == -1) {
            database.migrate();
            dbVersion = database.version;
        }

        if (dbVersion == 0) {
            database.insertVersion();
            database.addColumnHeadTexture();
            database.addColumnDisplayName();
            dbVersion = database.version;
        }

        if (dbVersion == 1) {
            database.addColumnHeadTexture();
        }

        if (dbVersion == 2) {
            database.addColumnDisplayName();
        }

        if (dbVersion != database.version) {
            database.upsertTableVersion(dbVersion);
        }
    }

    private static boolean backupDatabase() {
        String pathToDatabase = HeadBlocks.getInstance().getDataFolder() + File.separator + "headblocks.db";
        var databaseFile = new File(pathToDatabase);

        if (!databaseFile.exists()) {
            return true;
        }

        File copied = new File(pathToDatabase + ".save-" + LocalDate.now());
        try (var in = new BufferedInputStream(new FileInputStream(databaseFile));
             var out = new BufferedOutputStream(new FileOutputStream(copied))) {

            var buffer = new byte[1024];
            int lengthRead;
            while ((lengthRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, lengthRead);
                out.flush();
            }
        } catch (Exception e) {
            HeadBlocks.log.sendMessage("&cError backuping database, aborting migration, storage error: " + e.getMessage());
            return false;
        }

        return true;
    }

    public static void loadPlayer(Player player) {
        UUID pUuid = player.getUniqueId();
        String playerName = player.getName();

        try {
            boolean isExist = containsPlayer(pUuid);

            String playerDisplayName = getCustomDisplay(player);

            var playerProfile = new PlayerProfileLight(pUuid, playerName, playerDisplayName);

            if (isExist) {
                boolean hasRenamed = hasPlayerRenamed(playerProfile);

                if (hasRenamed) {
                    updatePlayerName(playerProfile);
                }

                for (UUID hUuid : database.getHeadsPlayer(pUuid, playerName)) {
                    storage.addHead(pUuid, hUuid);
                }

                _cacheHeads.put(pUuid, new ArrayList<>());
            } else {
                updatePlayerName(playerProfile);
            }
        } catch (InternalException ex) {
            storageError = true;
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to load player " + playerName + " from SQL database: " + ex.getMessage()));
        }
    }

    private static String getCustomDisplay(Player player) {
        var customName = player.getName();

        if (ConfigService.isPlaceholdersLeaderboardUseNickname()) {
            customName = player.getDisplayName();
        }

        var prefix = ConfigService.getPlaceholdersLeaderboardPrefix();
        if (!prefix.isEmpty()) {
            customName = PlaceholderAPI.setPlaceholders(player, prefix) + customName;
        }

        var suffix = ConfigService.getPlaceholdersLeaderboardSuffix();
        if (!prefix.isEmpty()) {
            customName = customName + PlaceholderAPI.setPlaceholders(player, suffix);
        }

        return customName;
    }

    public static void unloadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();

        try {
            boolean isExist = containsPlayer(uuid);

            if (isExist) {
                storage.resetPlayer(uuid);
            }

            _cacheHeads.remove(uuid);
        } catch (InternalException ex) {
            storageError = true;
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to unload player " + playerName + " from SQL database: " + ex.getMessage()));
        }
    }

    public static void close() {
        try {
            if (storage != null) {
                storage.close();
            }
        } catch (InternalException ex) {
            storageError = true;
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to close the REDIS connection : " + ex.getMessage()));
        }

        try {
            if (database != null) {
                database.close();
            }
        } catch (InternalException ex) {
            storageError = true;
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to close the SQL connection : " + ex.getMessage()));
        }
    }

    public static boolean hasHead(UUID playerUuid, UUID headUuid) throws InternalException {
        if (_cacheHeads.containsKey(playerUuid) && !_cacheHeads.get(playerUuid).isEmpty())
            return _cacheHeads.get(playerUuid).contains(headUuid);

        return storage.hasHead(playerUuid, headUuid);
    }

    public static void addHead(UUID playerUuid, UUID headUuid) throws InternalException {
        invalidateCachePlayer(playerUuid);

        storage.addHead(playerUuid, headUuid);
        database.addHead(playerUuid, headUuid);
    }

    public static boolean containsPlayer(UUID playerUuid) throws InternalException {
        return storage.containsPlayer(playerUuid) || database.containsPlayer(playerUuid);
    }

    public static List<UUID> getHeadsPlayer(UUID playerUuid, String pName) throws InternalException {
        if (_cacheHeads.containsKey(playerUuid) && !_cacheHeads.get(playerUuid).isEmpty())
            return _cacheHeads.get(playerUuid);

        var headsUuid = database.getHeadsPlayer(playerUuid, pName);
        _cacheHeads.compute(playerUuid, (key, playerHeads) -> {
            if (playerHeads == null) {
                return headsUuid;
            } else {
                playerHeads.addAll(headsUuid);
                return playerHeads;
            }
        });

        return headsUuid;
    }

    public static void resetPlayer(UUID playerUuid) throws InternalException {
        invalidateCachePlayer(playerUuid);

        storage.resetPlayer(playerUuid);
        database.resetPlayer(playerUuid);
    }

    public static void removeHead(UUID headUuid, boolean withDelete) throws InternalException {
        for (var head : _cacheHeads.entrySet()) {
            head.getValue().remove(headUuid);
        }

        storage.removeHead(headUuid);
        database.removeHead(headUuid, withDelete);
    }

    public static List<UUID> getAllPlayers() throws InternalException {
        return database.getAllPlayers();
    }

    public static LinkedHashMap<PlayerProfileLight, Integer> getTopPlayers() throws InternalException {
        var copy = _cacheTop.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));

        if (!copy.isEmpty())
            return copy;

        _cacheTop.putAll(database.getTopPlayers());

        return _cacheTop;
    }

    public static void updatePlayerName(PlayerProfileLight profile) throws InternalException {
        database.updatePlayerInfo(profile);
    }

    public static boolean hasPlayerRenamed(PlayerProfileLight profile) throws InternalException {
        return database.hasPlayerRenamed(profile);
    }

    public static void createNewHead(UUID headUuid, String texture) throws InternalException {
        database.createNewHead(headUuid, texture);
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

        ArrayList<AbstractMap.SimpleEntry<String, Boolean>> heads = database.getTableHeads();
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

        ArrayList<AbstractMap.SimpleEntry<String, String>> playerHeads = database.getTablePlayerHeads();
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

        ArrayList<AbstractMap.SimpleEntry<String, String>> players = database.getTablePlayers();
        for (AbstractMap.SimpleEntry<String, String> player : players) {
            instructions.add("INSERT INTO hb_players (pUUID, pName) VALUES ('" + player.getKey() + "', '" + player.getValue() + "');");
        }

        instructions.add("");

        // Table : hb_version
        instructions.add("DROP TABLE IF EXISTS hb_version;");
        instructions.add(Requests.CREATE_TABLE_VERSION + ";");
        instructions.add(Requests.UPSERT_VERSION.replaceAll("\\?", String.valueOf(database.version)) + ";");

        return instructions;
    }

    public static String getHeadTexture(UUID headUuid) throws InternalException {
        return database.getHeadTexture(headUuid);
    }

    public static ArrayList<UUID> getPlayers(UUID headUuid) throws InternalException {
        return database.getPlayers(headUuid);
    }

    public static PlayerProfileLight getPlayerByName(String pName) throws InternalException {
        return database.getPlayerByName(pName);
    }

    private static void invalidateCachePlayer(UUID playerUuid) {
        _cacheTop.clear();

        if (!_cacheHeads.containsKey(playerUuid))
            return;

        _cacheHeads.get(playerUuid).clear();
    }
}
