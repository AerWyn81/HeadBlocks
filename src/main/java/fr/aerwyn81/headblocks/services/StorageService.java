package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.databases.Database;
import fr.aerwyn81.headblocks.databases.EnumTypeDatabase;
import fr.aerwyn81.headblocks.databases.Requests;
import fr.aerwyn81.headblocks.databases.types.MySQL;
import fr.aerwyn81.headblocks.databases.types.SQLite;
import fr.aerwyn81.headblocks.storages.Storage;
import fr.aerwyn81.headblocks.storages.types.Memory;
import fr.aerwyn81.headblocks.storages.types.Redis;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.utils.runnables.BukkitFutureResult;
import fr.aerwyn81.headblocks.utils.runnables.CompletableBukkitFuture;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class StorageService {
    private static Storage storage;
    private static Database database;

    private static boolean storageError;

    private static String serverIdentifier = "";

    public static void initialize() {
        storageError = false;

        if (ConfigService.isRedisEnabled() && !ConfigService.isDatabaseEnabled()) {
            LogUtil.error("Error you can't use Redis without setting up an SQL database");
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
            generateServerIdentifier();

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

            if (ConfigService.isRedisEnabled()) {
                LogUtil.info("Redis cache properly connected!");
            }
        } catch (InternalException ex) {
            LogUtil.error("Error while trying to initialize the storage: {0}", ex.getMessage());
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
            LogUtil.error("Error while trying to connect to the {0} database: {1}", ConfigService.getDatabaseType(), ex.getMessage());
            storageError = true;
        }

        if (!storageError) {
            if (ConfigService.isDatabaseEnabled()) {
                LogUtil.info("{0} storage properly connected!", ConfigService.getDatabaseType());
            } else {
                LogUtil.info("SQLite storage properly connected!");
            }
        }
    }

    private static void generateServerIdentifier() {
        var file = new File(HeadBlocks.getInstance().getDataFolder() + File.separator + "server.identifier");
        if (ConfigService.isDatabaseEnabled() && file.exists()) {
            try {
                serverIdentifier = Files.readAllLines(file.toPath()).get(0);
            } catch (Exception ex) {
                storageError = true;
                LogUtil.error("Error reading server identifier file. Storage disabled. {0}", ex.getMessage());
            }

            return;
        }

        try {
            var writer = new FileWriter(file);
            serverIdentifier = UUID.randomUUID().toString().split("-")[0];
            writer.write(serverIdentifier);
            writer.close();
        } catch (Exception ex) {
            storageError = true;
            LogUtil.error("Error generating server identifier file. Storage disabled. {0}", ex.getMessage());
        }
    }

    public static boolean hasStorageError() {
        return storageError;
    }

    public static String selectedStorageType() {
        if (!ConfigService.isDatabaseEnabled())
            return "SQLite";

        return ConfigService.getDatabaseType().name();
    }

    private static void verifyDatabaseMigration() throws InternalException {
        if (!database.isDefaultTablesExist()) {
            return;
        }

        int dbVersion = database.checkVersion();

        if (dbVersion == Database.version) {
            return;
        }

        int initialVersion = dbVersion;

        if (database instanceof SQLite) {
            var backup = backupDatabase("save-") != null;
            if (!backup) {
                storageError = true;
            }
        }

        if (dbVersion == -1) {
            database.migrate();
            dbVersion = Database.version;
        }

        if (dbVersion == 0) {
            database.insertVersion();
            database.addColumnHeadTexture();
            database.addColumnDisplayName();
            database.addColumnServerIdentifier();
            dbVersion = Database.version;
        }

        if (dbVersion == 1) {
            database.addColumnHeadTexture();
            dbVersion = 2;
        }

        if (dbVersion == 2) {
            database.addColumnDisplayName();
            dbVersion = 3;
        }

        if (dbVersion == 3) {
            database.addColumnServerIdentifier();
            dbVersion = 4;
        }

        if (dbVersion == 4) {
            LogUtil.info("Migrating database from v4 to v5 (multi-hunt support)...");
            try {
                database.migrateToV5();
            } catch (InternalException ex) {
                LogUtil.error("CRITICAL: Database migration from v4 to v5 FAILED: {0}", ex.getMessage());
                LogUtil.error("The plugin storage is disabled to prevent data corruption. Please restore from backup and try again.");
                throw ex;
            }
            dbVersion = Database.version;
            LogUtil.info("Database migration to v5 completed successfully.");
        }

        if (dbVersion != initialVersion) {
            database.upsertTableVersion(initialVersion);
        }
    }

    public static String backupDatabase(String suffix) {
        var pathToDatabase = HeadBlocks.getInstance().getDataFolder() + File.separator + "headblocks.db";
        var databaseFile = new File(pathToDatabase);

        if (!databaseFile.exists()) {
            return null;
        }

        var backupFileName = "headblocks.db." + suffix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm"));
        var copied = new File(HeadBlocks.getInstance().getDataFolder() + File.separator + backupFileName);
        try (var in = new BufferedInputStream(new FileInputStream(databaseFile));
             var out = new BufferedOutputStream(new FileOutputStream(copied))) {

            var buffer = new byte[1024];
            int lengthRead;
            while ((lengthRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, lengthRead);
                out.flush();
            }
        } catch (Exception e) {
            LogUtil.error("Error backuping database: {0}", e.getMessage());
            return null;
        }

        return backupFileName;
    }

    public static void loadPlayers(Player... players) {
        CompletableBukkitFuture.runAsync(HeadBlocks.getInstance(), () -> {
            for (var player : players) {
                UUID pUuid = player.getUniqueId();
                String playerName = player.getName();

                try {
                    boolean isExist = containsPlayer(pUuid);

                    String playerDisplayName = getCustomDisplay(player);

                    var playerProfile = new PlayerProfileLight(pUuid, playerName, playerDisplayName);

                    var playerHeads = new HashSet<UUID>();

                    if (isExist) {
                        boolean hasRenamed = hasPlayerRenamed(playerProfile);

                        if (hasRenamed) {
                            updatePlayerName(playerProfile);
                        }

                        for (UUID hUuid : database.getHeadsPlayer(pUuid)) {
                            storage.addHead(pUuid, hUuid);
                            playerHeads.add(hUuid);
                        }
                    } else {
                        updatePlayerName(playerProfile);
                    }

                    storage.setCachedPlayerHeads(pUuid, playerHeads);
                } catch (InternalException ex) {
                    storageError = true;
                    LogUtil.error("Error while trying to load player {0} from SQL database: {1}", playerName, ex.getMessage());
                }
            }
        });
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
        if (!suffix.isEmpty()) {
            customName = customName + PlaceholderAPI.setPlaceholders(player, suffix);
        }

        return customName;
    }

    public static void unloadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();

        try {
            storage.removeCachedPlayerHeads(uuid);
        } catch (InternalException ex) {
            storageError = true;
            LogUtil.error("Error while trying to clear player cache: {0}", ex.getMessage());
        }

        try {
            boolean isExist = containsPlayer(uuid);

            if (isExist) {
                storage.resetPlayer(uuid);
            }
        } catch (InternalException ex) {
            storageError = true;
            LogUtil.error("Error while trying to unload player {0} from SQL database: {1}", playerName, ex.getMessage());
        }
    }

    public static void close() {
        try {
            if (storage != null) {
                storage.close();
            }
        } catch (InternalException ex) {
            storageError = true;
            LogUtil.error("Error while trying to close the REDIS connection : {0}", ex.getMessage());
        }

        try {
            if (database != null) {
                database.close();
            }
        } catch (InternalException ex) {
            storageError = true;
            LogUtil.error("Error while trying to close the SQL connection : {0}", ex.getMessage());
        }
    }

    public static boolean hasHead(UUID playerUuid, UUID headUuid) throws InternalException {
        Set<UUID> cachedHeads = storage.getCachedPlayerHeads(playerUuid);
        if (cachedHeads != null)
            return cachedHeads.contains(headUuid);

        return storage.hasHead(playerUuid, headUuid);
    }

    public static void addHead(UUID playerUuid, UUID headUuid) throws InternalException {
        storage.addHead(playerUuid, headUuid);
        database.addHead(playerUuid, headUuid);

        storage.addCachedPlayerHead(playerUuid, headUuid);

        storage.clearCachedTopPlayers();
    }

    public static Boolean containsPlayer(UUID playerUuid) throws InternalException {
        return storage.containsPlayer(playerUuid) || database.containsPlayer(playerUuid);
    }

    public static BukkitFutureResult<Set<UUID>> getHeadsPlayer(UUID playerUuid) {
        try {
            Set<UUID> cachedHeads = storage.getCachedPlayerHeads(playerUuid);
            if (cachedHeads != null)
                return BukkitFutureResult.of(HeadBlocks.getInstance(), CompletableFuture.completedFuture(cachedHeads));
        } catch (InternalException ex) {
            LogUtil.error("Error while trying to get cached heads for {0}: {1}", playerUuid, ex.getMessage());
        }

        return CompletableBukkitFuture.supplyAsync(HeadBlocks.getInstance(), () -> {
            try {
                var headsUuid = database.getHeadsPlayer(playerUuid);

                Set<UUID> playerHeads = storage.getCachedPlayerHeads(playerUuid);
                if (playerHeads == null) {
                    playerHeads = new HashSet<>();
                }
                playerHeads.addAll(headsUuid);
                storage.setCachedPlayerHeads(playerUuid, playerHeads);

                return playerHeads;
            } catch (Exception ex) {
                LogUtil.error("Error while trying to get heads for {0}: {1}", playerUuid, ex.getMessage());
                return new HashSet<>();
            }
        });
    }

    public static void resetPlayer(UUID playerUuid) throws InternalException {
        invalidateCachePlayer(playerUuid);

        storage.resetPlayer(playerUuid);
        database.resetPlayer(playerUuid);
    }

    public static void resetPlayerHead(UUID playerUuid, UUID headUuid) throws InternalException {
        try {
            Set<UUID> cachedHeads = storage.getCachedPlayerHeads(playerUuid);
            if (cachedHeads != null) {
                cachedHeads.remove(headUuid);
                storage.setCachedPlayerHeads(playerUuid, cachedHeads);
            }

            storage.clearCachedTopPlayers();
        } catch (InternalException ex) {
            LogUtil.error("Error while invalidating cache for player {0} and head {1}: {2}", playerUuid, headUuid, ex.getMessage());
        }

        storage.resetPlayerHead(playerUuid, headUuid);
        database.resetPlayerHead(playerUuid, headUuid);
    }

    public static void removeHead(UUID headUuid, boolean withDelete) throws InternalException {
        storage.removeHead(headUuid);
        database.removeHead(headUuid, withDelete);
        storage.removeCachedHead(headUuid);
    }

    public static List<UUID> getAllPlayers() throws InternalException {
        return database.getAllPlayers();
    }

    public static LinkedHashMap<PlayerProfileLight, Integer> getTopPlayers() throws InternalException {
        LinkedHashMap<PlayerProfileLight, Integer> cached = storage.getCachedTopPlayers();

        if (!cached.isEmpty()) {
            return cached.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));
        }

        LinkedHashMap<PlayerProfileLight, Integer> topPlayers = database.getTopPlayers();
        storage.setCachedTopPlayers(topPlayers);

        return topPlayers;
    }

    public static void updatePlayerName(PlayerProfileLight profile) throws InternalException {
        database.updatePlayerInfo(profile);
    }

    public static boolean hasPlayerRenamed(PlayerProfileLight profile) throws InternalException {
        return database.hasPlayerRenamed(profile);
    }

    public static void createOrUpdateHead(UUID headUuid, String texture) throws InternalException {
        database.createNewHead(headUuid, texture, serverIdentifier);

        storage.addCachedHead(headUuid);
    }

    public static boolean isHeadExist(UUID headUuid) throws InternalException {
        return database.isHeadExist(headUuid);
    }

    public static ArrayList<String> getInstructionsExport(EnumTypeDatabase type) throws InternalException {
        ArrayList<String> instructions = new ArrayList<>();

        // Table : hb_heads
        instructions.add("DROP TABLE IF EXISTS " + ConfigService.getDatabasePrefix() + "hb_heads;");

        if (type == EnumTypeDatabase.MySQL) {
            instructions.add(Requests.createTableHeadsMySQL() + ";");
        } else if (type == EnumTypeDatabase.SQLite) {
            instructions.add(Requests.createTableHeads() + ";");
        }

        ArrayList<AbstractMap.SimpleEntry<String, Boolean>> heads = database.getTableHeads();
        for (AbstractMap.SimpleEntry<String, Boolean> head : heads) {
            instructions.add("INSERT INTO " + ConfigService.getDatabasePrefix() + "hb_heads (hUUID, hExist, hTexture, serverId) VALUES ('" + head.getKey() +
                    "', " + (head.getValue() ? 1 : 0) + ", '', '" + serverIdentifier + "');");
        }

        instructions.add("");

        // Table : hb_playerHeads
        instructions.add("DROP TABLE IF EXISTS " + ConfigService.getDatabasePrefix() + "hb_playerHeads;");

        if (type == EnumTypeDatabase.MySQL) {
            instructions.add(Requests.createTablePlayerHeadsMySQL() + ";");
        } else if (type == EnumTypeDatabase.SQLite) {
            instructions.add(Requests.createTablePlayerHeads() + ";");
        }

        ArrayList<AbstractMap.SimpleEntry<String, String>> playerHeads = database.getTablePlayerHeads();
        for (AbstractMap.SimpleEntry<String, String> pHead : playerHeads) {
            instructions.add("INSERT INTO " + ConfigService.getDatabasePrefix() + "hb_playerHeads (pUUID, hUUID) VALUES ('" + pHead.getKey() +
                    "', '" + pHead.getValue() + "');");
        }

        instructions.add("");

        // Table : hb_players
        instructions.add("DROP TABLE IF EXISTS " + ConfigService.getDatabasePrefix() + "hb_players;");

        if (type == EnumTypeDatabase.MySQL) {
            instructions.add(Requests.createTablePlayersMySQL() + ";");
        } else if (type == EnumTypeDatabase.SQLite) {
            instructions.add(Requests.createTablePlayers() + ";");
        }

        ArrayList<AbstractMap.SimpleEntry<String, String>> players = database.getTablePlayers();
        for (AbstractMap.SimpleEntry<String, String> player : players) {
            instructions.add("INSERT INTO " + ConfigService.getDatabasePrefix() + "hb_players (pUUID, pName, pDisplayName) VALUES ('" + player.getKey() + "', '" + player.getValue() + "', '');");
        }

        instructions.add("");

        // Table : hb_version
        instructions.add("DROP TABLE IF EXISTS " + ConfigService.getDatabasePrefix() + "hb_version;");
        instructions.add(Requests.createTableVersion() + ";");
        instructions.add(Requests.upsertVersion().replaceAll("\\?", String.valueOf(Database.version)) + ";");

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

    public static void invalidateCachePlayer(UUID playerUuid) {
        try {
            storage.clearCachedTopPlayers();

            Set<UUID> cachedHeads = storage.getCachedPlayerHeads(playerUuid);
            if (cachedHeads != null) {
                cachedHeads.clear();
                storage.setCachedPlayerHeads(playerUuid, cachedHeads);
            }
        } catch (InternalException ex) {
            LogUtil.error("Error while invalidating cache for player {0}: {1}", playerUuid, ex.getMessage());
        }
    }

    public static ArrayList<UUID> getHeads() throws InternalException {
        Set<UUID> cachedHeads = storage.getCachedHeads();
        if (!cachedHeads.isEmpty())
            return new ArrayList<>(cachedHeads);

        var heads = database.getHeads();
        for (UUID head : heads) {
            storage.addCachedHead(head);
        }
        return heads;
    }

    public static ArrayList<UUID> getHeadsByServerId() throws InternalException {
        return database.getHeads(serverIdentifier);
    }

    public static ArrayList<String> getDistinctServerIds() throws InternalException {
        return database.getDistinctServerIds();
    }

    // --- Hunt-aware player progression ---

    public static void addHeadForHunt(UUID playerUuid, UUID headUuid, String huntId) throws InternalException {
        storage.addHead(playerUuid, headUuid);
        database.addHeadForHunt(playerUuid, headUuid, huntId);

        storage.addCachedPlayerHead(playerUuid, headUuid);
        storage.clearCachedTopPlayers();
    }

    public static ArrayList<UUID> getHeadsPlayerForHunt(UUID playerUuid, String huntId) throws InternalException {
        return database.getHeadsPlayerForHunt(playerUuid, huntId);
    }

    public static int getPlayerCountForHeadInHunt(UUID headUuid, String huntId) throws InternalException {
        return database.getPlayerCountForHeadInHunt(headUuid, huntId);
    }

    public static LinkedHashMap<PlayerProfileLight, Integer> getTopPlayersForHunt(String huntId) throws InternalException {
        return database.getTopPlayersForHunt(huntId);
    }

    public static void resetPlayerHunt(UUID playerUuid, String huntId) throws InternalException {
        database.resetPlayerHunt(playerUuid, huntId);
        invalidateCachePlayer(playerUuid);
    }

    public static void resetPlayerHeadHunt(UUID playerUuid, UUID headUuid, String huntId) throws InternalException {
        database.resetPlayerHeadHunt(playerUuid, headUuid, huntId);
        invalidateCachePlayer(playerUuid);
    }

    // --- Hunt DB access ---

    public static ArrayList<String[]> getHuntsFromDb() throws InternalException {
        return database.getHunts();
    }

    public static void createHuntInDb(String huntId, String name, String state) throws InternalException {
        database.createHunt(huntId, name, state);
    }

    public static ArrayList<UUID> getHeadsForHunt(String huntId) throws InternalException {
        return database.getHeadsForHunt(huntId);
    }

    public static void linkHeadToHunt(UUID headUUID, String huntId) throws InternalException {
        database.linkHeadToHunt(headUUID, huntId);
    }

    public static void unlinkHeadFromHunt(UUID headUUID, String huntId) throws InternalException {
        database.unlinkHeadFromHunt(headUUID, huntId);
    }

    public static void updateHuntStateInDb(String huntId, String state) throws InternalException {
        database.updateHuntState(huntId, state);
    }

    public static void updateHuntNameInDb(String huntId, String name) throws InternalException {
        database.updateHuntName(huntId, name);
    }

    public static void deleteHuntFromDb(String huntId) throws InternalException {
        database.deleteHunt(huntId);
    }

    public static void unlinkAllHeadsFromHuntInDb(String huntId) throws InternalException {
        database.unlinkAllHeadsFromHunt(huntId);
    }

    public static void resetAllPlayersForHunt(String huntId) throws InternalException {
        for (UUID playerUuid : database.getAllPlayers()) {
            database.resetPlayerHunt(playerUuid, huntId);
        }
    }

    public static String getServerIdentifier() {
        return serverIdentifier;
    }

    // --- Hunt sync version (cross-server via Redis) ---

    public static long getHuntVersion() {
        try {
            return storage.getHuntVersion();
        } catch (InternalException e) {
            return 0;
        }
    }

    public static void incrementHuntVersion() {
        try {
            storage.incrementHuntVersion();
        } catch (InternalException e) {
            LogUtil.error("Failed to increment hunt version: {0}", e.getMessage());
        }
    }
}
