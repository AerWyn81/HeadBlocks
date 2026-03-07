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

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class StorageService {
    private final ConfigService configService;
    private final File dataFolder;

    private Storage storage;
    private Database database;
    private volatile boolean storageError;
    private String serverIdentifier = "";

    // --- Constructor ---

    public StorageService(ConfigService configService, File dataFolder) {
        this.configService = configService;
        this.dataFolder = dataFolder;

        initialize();
    }

    StorageService(ConfigService configService, Storage storage, Database database) {
        this.configService = configService;
        this.dataFolder = null;
        this.storage = storage;
        this.database = database;
        this.storageError = false;
    }

    // --- Instance methods ---

    public void initialize() {
        storageError = false;

        if (configService.redisEnabled() && !configService.databaseEnabled()) {
            LogUtil.error("Error you can't use Redis without setting up an SQL database");
            storageError = true;
            return;
        }

        if (configService.redisEnabled()) {
            storage = new Redis(
                    configService.redisHostname(),
                    configService.redisPassword(),
                    configService.redisPort(),
                    configService.redisDatabase());
        } else {
            storage = new Memory();
        }

        var databasePath = dataFolder.toPath().resolve("headblocks.db");
        var isFileExist = Files.exists(databasePath);

        if (configService.databaseEnabled()) {
            generateServerIdentifier();

            database = new MySQL(
                    configService.databaseUsername(),
                    configService.databasePassword(),
                    configService.databaseHostname(),
                    configService.databasePort(),
                    configService.databaseName(),
                    configService.databaseSsl(),
                    configService);
        } else {
            database = new SQLite(databasePath.toString());
        }

        try {
            storage.init();

            if (configService.redisEnabled()) {
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
            LogUtil.error("Error while trying to connect to the {0} database: {1}", configService.databaseType(), ex.getMessage());
            storageError = true;
        }

        if (!storageError) {
            if (configService.databaseEnabled()) {
                LogUtil.info("{0} storage properly connected!", configService.databaseType());
            } else {
                LogUtil.info("SQLite storage properly connected!");
            }
        }
    }

    private void generateServerIdentifier() {
        var file = dataFolder.toPath().resolve("server.identifier").toFile();
        if (configService.databaseEnabled() && file.exists()) {
            try {
                serverIdentifier = Files.readAllLines(file.toPath()).get(0);
            } catch (Exception ex) {
                storageError = true;
                LogUtil.error("Error reading server identifier file. Storage disabled. {0}", ex.getMessage());
            }

            return;
        }

        try (var writer = new FileWriter(file)) {
            serverIdentifier = UUID.randomUUID().toString().split("-")[0];
            writer.write(serverIdentifier);
        } catch (Exception ex) {
            storageError = true;
            LogUtil.error("Error generating server identifier file. Storage disabled. {0}", ex.getMessage());
        }
    }

    public boolean isStorageError() {
        return storageError;
    }

    public String selectedStorageType() {
        if (!configService.databaseEnabled()) {
            return "SQLite";
        }

        return configService.databaseType().name();
    }

    private void verifyDatabaseMigration() throws InternalException {
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
            dbVersion = 5;
            LogUtil.info("Database migration to v5 completed successfully.");
        }

        if (dbVersion != initialVersion) {
            database.upsertTableVersion(initialVersion);
        }
    }

    public String backupDatabase(String suffix) {
        var databasePath = dataFolder.toPath().resolve("headblocks.db");

        if (!Files.exists(databasePath)) {
            return null;
        }

        var backupFileName = "headblocks.db." + suffix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm"));
        var backupPath = dataFolder.toPath().resolve(backupFileName);
        try {
            Files.copy(databasePath, backupPath);
        } catch (Exception e) {
            LogUtil.error("Error backing up database: {0}", e.getMessage());
            return null;
        }

        return backupFileName;
    }

    public void loadPlayers(Player... players) {
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

    private String getCustomDisplay(Player player) {
        var customName = player.getName();

        if (configService.placeholdersLeaderboardUseNickname()) {
            customName = player.getDisplayName();
        }

        if (HeadBlocks.isPlaceholderApiActive) {
            var prefix = configService.placeholdersLeaderboardPrefix();
            if (!prefix.isEmpty()) {
                customName = PlaceholderAPI.setPlaceholders(player, prefix) + customName;
            }

            var suffix = configService.placeholdersLeaderboardSuffix();
            if (!suffix.isEmpty()) {
                customName = customName + PlaceholderAPI.setPlaceholders(player, suffix);
            }
        }

        return customName;
    }

    public void unloadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();

        try {
            storage.removeCachedPlayerHeads(uuid);
        } catch (InternalException ex) {
            storageError = true;
            LogUtil.error("Error while trying to clear player cache: {0}", ex.getMessage());
        }

        try {
            storage.clearAllCachedHuntDataForPlayer(uuid);
        } catch (InternalException ex) {
            LogUtil.error("Error while trying to clear hunt cache for player {0}: {1}", playerName, ex.getMessage());
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

    public void close() {
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

    public boolean hasHead(UUID playerUuid, UUID headUuid) throws InternalException {
        Set<UUID> cachedHeads = storage.getCachedPlayerHeads(playerUuid);
        if (cachedHeads != null) {
            return cachedHeads.contains(headUuid);
        }

        return storage.hasHead(playerUuid, headUuid);
    }

    public void addHead(UUID playerUuid, UUID headUuid) throws InternalException {
        storage.addHead(playerUuid, headUuid);
        database.addHead(playerUuid, headUuid);

        storage.addCachedPlayerHead(playerUuid, headUuid);

        storage.clearCachedTopPlayers();
    }

    public boolean containsPlayer(UUID playerUuid) throws InternalException {
        return storage.containsPlayer(playerUuid) || database.containsPlayer(playerUuid);
    }

    public BukkitFutureResult<Set<UUID>> getHeadsPlayer(UUID playerUuid) {
        try {
            Set<UUID> cachedHeads = storage.getCachedPlayerHeads(playerUuid);
            if (cachedHeads != null) {
                return BukkitFutureResult.of(HeadBlocks.getInstance(), CompletableFuture.completedFuture(cachedHeads));
            }
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

    public void resetPlayer(UUID playerUuid) throws InternalException {
        storage.resetPlayer(playerUuid);
        database.resetPlayer(playerUuid);

        invalidateCachePlayer(playerUuid);
    }

    public void resetPlayerHead(UUID playerUuid, UUID headUuid) throws InternalException {
        storage.resetPlayerHead(playerUuid, headUuid);
        database.resetPlayerHead(playerUuid, headUuid);

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
    }

    public void removeHead(UUID headUuid, boolean withDelete) throws InternalException {
        storage.removeHead(headUuid);
        database.removeHead(headUuid, withDelete);
        storage.removeCachedHead(headUuid);
    }

    public List<UUID> getAllPlayers() throws InternalException {
        return database.getAllPlayers();
    }

    public LinkedHashMap<PlayerProfileLight, Integer> getTopPlayers() throws InternalException {
        LinkedHashMap<PlayerProfileLight, Integer> cached = storage.getCachedTopPlayers();

        if (!cached.isEmpty()) {
            return cached.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));
        }

        LinkedHashMap<PlayerProfileLight, Integer> topPlayers = database.getTopPlayers();
        storage.setCachedTopPlayers(topPlayers);

        return topPlayers;
    }

    public void updatePlayerName(PlayerProfileLight profile) throws InternalException {
        database.updatePlayerInfo(profile);
    }

    public boolean hasPlayerRenamed(PlayerProfileLight profile) throws InternalException {
        return database.hasPlayerRenamed(profile);
    }

    public void createOrUpdateHead(UUID headUuid, String texture) throws InternalException {
        database.createNewHead(headUuid, texture, serverIdentifier);

        storage.addCachedHead(headUuid);
    }

    public boolean isHeadExist(UUID headUuid) throws InternalException {
        return database.isHeadExist(headUuid);
    }

    public ArrayList<String> getInstructionsExport(EnumTypeDatabase type) throws InternalException {
        ArrayList<String> instructions = new ArrayList<>();

        instructions.add("DROP TABLE IF EXISTS " + configService.databasePrefix() + "hb_heads;");

        if (type == EnumTypeDatabase.MySQL) {
            instructions.add(Requests.createTableHeadsMySQL() + ";");
        } else if (type == EnumTypeDatabase.SQLite) {
            instructions.add(Requests.createTableHeads() + ";");
        }

        ArrayList<Database.HeadExportRow> heads = database.getTableHeads();
        for (Database.HeadExportRow head : heads) {
            instructions.add("INSERT INTO " + configService.databasePrefix() + "hb_heads (hUUID, hExist, hTexture, serverId) VALUES ('" + escapeSql(head.uuid()) +
                    "', " + (head.exists() ? 1 : 0) + ", '', '" + escapeSql(serverIdentifier) + "');");
        }

        instructions.add("");

        instructions.add("DROP TABLE IF EXISTS " + configService.databasePrefix() + "hb_playerHeads;");

        if (type == EnumTypeDatabase.MySQL) {
            instructions.add(Requests.createTablePlayerHeadsMySQL() + ";");
        } else if (type == EnumTypeDatabase.SQLite) {
            instructions.add(Requests.createTablePlayerHeads() + ";");
        }

        ArrayList<Database.PlayerHeadExportRow> playerHeads = database.getTablePlayerHeads();
        for (Database.PlayerHeadExportRow pHead : playerHeads) {
            instructions.add("INSERT INTO " + configService.databasePrefix() + "hb_playerHeads (pUUID, hUUID) VALUES ('" + escapeSql(pHead.playerUuid()) +
                    "', '" + escapeSql(pHead.headUuid()) + "');");
        }

        instructions.add("");

        instructions.add("DROP TABLE IF EXISTS " + configService.databasePrefix() + "hb_players;");

        if (type == EnumTypeDatabase.MySQL) {
            instructions.add(Requests.createTablePlayersMySQL() + ";");
        } else if (type == EnumTypeDatabase.SQLite) {
            instructions.add(Requests.createTablePlayers() + ";");
        }

        ArrayList<Database.PlayerExportRow> players = database.getTablePlayers();
        for (Database.PlayerExportRow player : players) {
            instructions.add("INSERT INTO " + configService.databasePrefix() + "hb_players (pUUID, pName, pDisplayName) VALUES ('" + escapeSql(player.uuid()) + "', '" + escapeSql(player.name()) + "', '');");
        }

        instructions.add("");

        instructions.add("DROP TABLE IF EXISTS " + configService.databasePrefix() + "hb_version;");
        instructions.add(Requests.createTableVersion() + ";");
        instructions.add(Requests.upsertVersion().replaceAll("\\?", String.valueOf(Database.version)) + ";");

        return instructions;
    }

    private static String escapeSql(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "''");
    }

    public String getHeadTexture(UUID headUuid) throws InternalException {
        return database.getHeadTexture(headUuid);
    }

    public ArrayList<UUID> getPlayers(UUID headUuid) throws InternalException {
        return database.getPlayers(headUuid);
    }

    public PlayerProfileLight getPlayerByName(String pName) throws InternalException {
        return database.getPlayerByName(pName);
    }

    public void invalidateCachePlayer(UUID playerUuid) {
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

    public ArrayList<UUID> getHeads() throws InternalException {
        Set<UUID> cachedHeads = storage.getCachedHeads();
        if (!cachedHeads.isEmpty()) {
            return new ArrayList<>(cachedHeads);
        }

        var heads = database.getHeads();
        for (UUID head : heads) {
            storage.addCachedHead(head);
        }
        return heads;
    }

    public ArrayList<UUID> getHeadsByServerId() throws InternalException {
        return database.getHeads(serverIdentifier);
    }

    public ArrayList<String> getDistinctServerIds() throws InternalException {
        return database.getDistinctServerIds();
    }

    // --- Hunt-aware player progression ---

    public void addHeadForHunt(UUID playerUuid, UUID headUuid, String huntId) throws InternalException {
        storage.addHead(playerUuid, headUuid);
        database.addHeadForHunt(playerUuid, headUuid, huntId);

        storage.addCachedPlayerHead(playerUuid, headUuid);
        storage.clearCachedTopPlayers();

        storage.addCachedPlayerHeadForHunt(playerUuid, huntId, headUuid);
        storage.clearCachedTopPlayersForHunt(huntId);
    }

    public ArrayList<UUID> getHeadsPlayerForHunt(UUID playerUuid, String huntId) throws InternalException {
        Set<UUID> cached = storage.getCachedPlayerHeadsForHunt(playerUuid, huntId);
        if (cached != null) {
            return new ArrayList<>(cached);
        }

        ArrayList<UUID> fromDb = database.getHeadsPlayerForHunt(playerUuid, huntId);
        storage.setCachedPlayerHeadsForHunt(playerUuid, huntId, new HashSet<>(fromDb));
        return fromDb;
    }

    public LinkedHashMap<PlayerProfileLight, Integer> getTopPlayersForHunt(String huntId) throws InternalException {
        LinkedHashMap<PlayerProfileLight, Integer> cached = storage.getCachedTopPlayersForHunt(huntId);
        if (cached != null) {
            return cached.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));
        }

        LinkedHashMap<PlayerProfileLight, Integer> topPlayers = database.getTopPlayersForHunt(huntId);
        storage.setCachedTopPlayersForHunt(huntId, topPlayers);
        return topPlayers;
    }

    public void resetPlayerHunt(UUID playerUuid, String huntId) throws InternalException {
        database.resetPlayerHunt(playerUuid, huntId);
        invalidateCachePlayer(playerUuid);

        storage.removeCachedPlayerHeadsForHunt(playerUuid, huntId);
        storage.clearCachedTopPlayersForHunt(huntId);
    }

    // --- Hunt DB access ---

    public ArrayList<String[]> getHuntsFromDb() throws InternalException {
        return database.getHunts();
    }

    public void createHuntInDb(String huntId, String name, String state) throws InternalException {
        database.createHunt(huntId, name, state);
    }

    public ArrayList<UUID> getHeadsForHunt(String huntId) throws InternalException {
        return database.getHeadsForHunt(huntId);
    }

    public void linkHeadToHunt(UUID headUUID, String huntId) throws InternalException {
        database.linkHeadToHunt(headUUID, huntId);
    }

    public void unlinkHeadFromHunt(UUID headUUID, String huntId) throws InternalException {
        database.unlinkHeadFromHunt(headUUID, huntId);
    }

    public void updateHuntStateInDb(String huntId, String state) throws InternalException {
        database.updateHuntState(huntId, state);
    }

    public void updateHuntNameInDb(String huntId, String name) throws InternalException {
        database.updateHuntName(huntId, name);
    }

    public void deleteHuntFromDb(String huntId) throws InternalException {
        database.deleteHunt(huntId);
    }

    public void unlinkAllHeadsFromHuntInDb(String huntId) throws InternalException {
        database.unlinkAllHeadsFromHunt(huntId);
    }

    public void resetAllPlayersForHunt(String huntId) throws InternalException {
        for (UUID playerUuid : database.getAllPlayers()) {
            database.resetPlayerHunt(playerUuid, huntId);
        }

        storage.clearCachedPlayerHeadsForHunt(huntId);
        storage.clearCachedTopPlayersForHunt(huntId);
    }

    public void transferPlayerProgress(String fromHuntId, String toHuntId) throws InternalException {
        database.transferPlayerProgress(fromHuntId, toHuntId);

        storage.clearCachedPlayerHeadsForHunt(fromHuntId);
        storage.clearCachedTopPlayersForHunt(fromHuntId);
        storage.clearCachedPlayerHeadsForHunt(toHuntId);
        storage.clearCachedTopPlayersForHunt(toHuntId);
    }

    public void deletePlayerProgressForHunt(String huntId) throws InternalException {
        database.deletePlayerProgressForHunt(huntId);

        storage.clearCachedPlayerHeadsForHunt(huntId);
        storage.clearCachedTopPlayersForHunt(huntId);
    }

    // --- Timed runs ---

    public void saveTimedRun(UUID playerUuid, String huntId, long timeMs) throws InternalException {
        database.saveTimedRun(playerUuid, huntId, timeMs);

        storage.clearCachedTimedLeaderboard(huntId);
        storage.clearCachedBestTime(playerUuid, huntId);
        storage.clearCachedTimedRunCount(playerUuid, huntId);
    }

    public LinkedHashMap<PlayerProfileLight, Long> getTimedLeaderboard(String huntId, int limit) throws InternalException {
        LinkedHashMap<PlayerProfileLight, Long> cached = storage.getCachedTimedLeaderboard(huntId);
        if (cached != null) {
            return cached.entrySet().stream()
                    .limit(limit)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));
        }

        LinkedHashMap<PlayerProfileLight, Long> fromDb = database.getTimedLeaderboard(huntId, limit);
        storage.setCachedTimedLeaderboard(huntId, fromDb);
        return fromDb;
    }

    public Long getBestTime(UUID playerUuid, String huntId) throws InternalException {
        Long cached = storage.getCachedBestTime(playerUuid, huntId);
        if (cached != null) {
            return cached == -1L ? null : cached;
        }

        Long fromDb = database.getBestTime(playerUuid, huntId);
        storage.setCachedBestTime(playerUuid, huntId, fromDb != null ? fromDb : -1L);
        return fromDb;
    }

    public int getTimedRunCount(UUID playerUuid, String huntId) throws InternalException {
        Integer cached = storage.getCachedTimedRunCount(playerUuid, huntId);
        if (cached != null) {
            return cached;
        }

        int fromDb = database.getTimedRunCount(playerUuid, huntId);
        storage.setCachedTimedRunCount(playerUuid, huntId, fromDb);
        return fromDb;
    }

    public String getServerIdentifier() {
        return serverIdentifier;
    }

    // --- Hunt sync version ---

    public long getHuntVersion() {
        try {
            return storage.getHuntVersion();
        } catch (InternalException e) {
            return 0;
        }
    }

    public void incrementHuntVersion() {
        try {
            storage.incrementHuntVersion();
        } catch (InternalException e) {
            LogUtil.error("Failed to increment hunt version: {0}", e.getMessage());
        }
    }

}
