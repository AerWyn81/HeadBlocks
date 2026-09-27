package fr.aerwyn81.headblocks.databases;

import fr.aerwyn81.headblocks.services.ConfigService;

public class Requests {
    private static final String HB_PLAYERS_OLD = "hb_players_old";
    private static final String DROP_TABLE_FORMAT = "DROP TABLE %s";
    private static final String V5_TEMP_SUFFIX = "_v5tmp";

    private Requests() {
    }

    private static String tablePrefix = "";
    private static String databaseName = "";

    public static void init(ConfigService configService) {
        if (configService.databaseEnabled()) {
            tablePrefix = configService.databasePrefix();
        } else {
            tablePrefix = "";
        }
        databaseName = configService.databaseName();
    }

    public static String getTablePlayers() {
        return addPrefix() + "hb_players";
    }

    public static String getTableHeads() {
        return addPrefix() + "hb_heads";
    }

    public static String getTablePlayerHeads() {
        return addPrefix() + "hb_playerHeads";
    }

    public static String getTableVersion() {
        return addPrefix() + "hb_version";
    }

    public static String getTableHunts() {
        return addPrefix() + "hb_hunts";
    }

    private static String addPrefix() {
        return tablePrefix;
    }

    public static String getIsTablePlayersExistSQLite() {
        return String.format("SELECT name FROM sqlite_master WHERE type='table' AND name='%s'", "hb_players");
    }

    public static String getTableHeadsColumnsSQLite() {
        return String.format("SELECT COUNT(*) AS count FROM pragma_table_info('%s');", getTableHeads());
    }

    public static String getIsTablePlayersExistMySQL() {
        return String.format("SELECT TABLE_NAME FROM information_schema.tables WHERE TABLE_SCHEMA = '%s' AND table_name = '%s' LIMIT 1", databaseName, getTablePlayers());
    }

    public static String getTableHeadsColumnsMySQL() {
        return String.format("SELECT COUNT(*) AS count FROM INFORMATION_SCHEMA.COLUMNS WHERE table_name = '%s'", getTableHeads());
    }

    public static String createTablePlayers() {
        return String.format("CREATE TABLE IF NOT EXISTS %s (`pId` INTEGER PRIMARY KEY AUTOINCREMENT, `pUUID` VARCHAR(36) UNIQUE NOT NULL, `pName` VARCHAR(16) NOT NULL, `pDisplayName` VARCHAR(255) NULL)", getTablePlayers());
    }

    public static String createTablePlayersMySQL() {
        return String.format("CREATE TABLE IF NOT EXISTS %s (`pId` INTEGER PRIMARY KEY AUTO_INCREMENT, `pUUID` VARCHAR(36) UNIQUE NOT NULL, `pName` VARCHAR(16) NOT NULL, `pDisplayName` VARCHAR(255) NULL)", getTablePlayers());
    }

    public static String getTablePlayer() {
        return String.format("SELECT pUUID, pName FROM %s", getTablePlayers());
    }

    public static String createTableHeads() {
        return String.format("CREATE TABLE IF NOT EXISTS %s (`hId` INTEGER PRIMARY KEY AUTOINCREMENT, `hUUID` VARCHAR(36) UNIQUE NOT NULL,`hExist` BOOLEAN NOT NULL CHECK (hExist IN (0, 1)), `hTexture` VARCHAR(255), `serverId` VARCHAR(8), `hSpawn` BOOLEAN NOT NULL DEFAULT 0 CHECK (hSpawn IN (0, 1)), `hPoints` DOUBLE NOT NULL DEFAULT 1)", getTableHeads());
    }

    public static String getContainsTableHeads() {
        return String.format("SELECT * FROM %s LIMIT 1", getTableHeads());
    }

    public static String createTableHeadsMySQL() {
        return String.format("CREATE TABLE IF NOT EXISTS %s (`hId` INTEGER PRIMARY KEY AUTO_INCREMENT, `hUUID` VARCHAR(36) UNIQUE NOT NULL,`hExist` BOOLEAN NOT NULL CHECK (hExist IN (0, 1)), `hTexture` VARCHAR(255), `serverId` VARCHAR(8), `hSpawn` BOOLEAN NOT NULL DEFAULT 0, `hPoints` DOUBLE NOT NULL DEFAULT 1)", getTableHeads());
    }

    public static String getTableHeadsData() {
        return String.format("SELECT hUUID, hExist, hSpawn FROM %s", getTableHeads());
    }

    public static String createTablePlayerHeads() {
        return String.format("CREATE TABLE IF NOT EXISTS %s (`pUUID` VARCHAR(36), `hUUID` VARCHAR(36) REFERENCES %s(hUUID) ON DELETE CASCADE, `huntId` VARCHAR(64) NOT NULL DEFAULT 'default', PRIMARY KEY(pUUID, hUUID, huntId))", getTablePlayerHeads(), getTableHeads());
    }

    public static String createTablePlayerHeadsMySQL() {
        return String.format("CREATE TABLE IF NOT EXISTS %s (`pUUID` VARCHAR(36), `hUUID` VARCHAR(36), `huntId` VARCHAR(64) NOT NULL DEFAULT 'default', PRIMARY KEY(pUUID, hUUID, huntId), FOREIGN KEY (`hUUID`) REFERENCES %s (`hUUID`) ON DELETE CASCADE)", getTablePlayerHeads(), getTableHeads());
    }

    public static String getTablePlayerHeadsData() {
        return String.format("SELECT pUUID, hUUID FROM %s", getTablePlayerHeads());
    }

    public static String createTableVersion() {
        return String.format("CREATE TABLE IF NOT EXISTS %s (`current` INTEGER)", getTableVersion());
    }

    public static String getTableVersionData() {
        return String.format("SELECT current FROM %s", getTableVersion());
    }

    public static String insertVersion() {
        return String.format("INSERT INTO %s VALUES (?)", getTableVersion());
    }

    public static String upsertVersion() {
        return String.format("UPDATE %s SET current = (?) WHERE current = (?)", getTableVersion());
    }

    public static String updatePlayer() {
        return String.format("INSERT OR REPLACE INTO %s (pUUID, pName, pDisplayName) VALUES (?, ?, ?)", getTablePlayers());
    }

    public static String updatePlayerMySQL() {
        return String.format("REPLACE INTO %s (pUUID, pName, pDisplayName) VALUES (?, ?, ?)", getTablePlayers());
    }

    public static String getHeads() {
        return String.format("SELECT * FROM %s WHERE hExist = True AND hSpawn = False", getTableHeads());
    }

    public static String getHeadsMySQL() {
        return String.format("SELECT * FROM %s WHERE hExist = True AND hSpawn = False AND serverId != ''", getTableHeads());
    }

    public static String getHeadsByServerId() {
        return String.format("SELECT * FROM %s WHERE hExist = True AND hSpawn = False AND serverId = ?", getTableHeads());
    }

    public static String updateHead() {
        return String.format("INSERT OR REPLACE INTO %s (hUUID, hExist, hTexture, serverId) VALUES (?, true, ?, ?)", getTableHeads());
    }

    public static String updateHeadMySQL() {
        return String.format("REPLACE INTO %s (hUUID, hExist, hTexture, serverId) VALUES (?, true, ?, ?)", getTableHeads());
    }

    public static String insertSpawnHead() {
        return String.format("INSERT INTO %s (hUUID, hExist, hTexture, serverId, hSpawn, hPoints) VALUES (?, true, ?, ?, true, ?)", getTableHeads());
    }

    public static String deleteOrphanSpawnHeads() {
        return String.format("DELETE FROM %1$s WHERE hSpawn = True AND serverId = ? AND NOT EXISTS (SELECT 1 FROM %2$s WHERE %2$s.hUUID = %1$s.hUUID)", getTableHeads(), getTablePlayerHeads());
    }

    public static String createIndexPlayerHeadsHeadSQLite() {
        return String.format("CREATE INDEX IF NOT EXISTS %1$s_hUUID ON %1$s (hUUID)", getTablePlayerHeads());
    }

    public static String hasColumnHeadSpawnSQLite() {
        return String.format("SELECT COUNT(*) AS count FROM pragma_table_info('%s') WHERE name = 'hSpawn'", getTableHeads());
    }

    public static String addColumnHeadSpawnSQLite() {
        return String.format("ALTER TABLE %s ADD COLUMN hSpawn BOOLEAN NOT NULL DEFAULT 0 CHECK (hSpawn IN (0, 1))", getTableHeads());
    }

    public static String addColumnHeadSpawnMariaDb() {
        return String.format("ALTER TABLE %s ADD COLUMN IF NOT EXISTS hSpawn BOOLEAN NOT NULL DEFAULT 0", getTableHeads());
    }

    public static String hasColumnHeadPointsSQLite() {
        return String.format("SELECT COUNT(*) AS count FROM pragma_table_info('%s') WHERE name = 'hPoints'", getTableHeads());
    }

    public static String addColumnHeadPointsSQLite() {
        return String.format("ALTER TABLE %s ADD COLUMN hPoints DOUBLE NOT NULL DEFAULT 1", getTableHeads());
    }

    public static String addColumnHeadPointsMariaDb() {
        return String.format("ALTER TABLE %s ADD COLUMN IF NOT EXISTS hPoints DOUBLE NOT NULL DEFAULT 1", getTableHeads());
    }

    public static String addColumnHeadPointsMySQL() {
        return String.format("ALTER TABLE %s ADD COLUMN hPoints DOUBLE NOT NULL DEFAULT 1", getTableHeads());
    }

    public static String getTopScoresForHunt() {
        return String.format("SELECT hbp.pUUID, pName, pDisplayName, SUM(hbh.hPoints) as score FROM %s hbph INNER JOIN %s hbp ON hbph.pUUID = hbp.pUUID INNER JOIN %s hbh ON hbph.hUUID = hbh.hUUID WHERE hbh.hExist = True AND hbph.huntId = ? GROUP BY hbp.pUUID, pName, pDisplayName ORDER BY score DESC", getTablePlayerHeads(), getTablePlayers(), getTableHeads());
    }

    public static String addColumnHeadSpawnMySQL() {
        return String.format("ALTER TABLE %s ADD COLUMN hSpawn BOOLEAN NOT NULL DEFAULT 0", getTableHeads());
    }

    public static String savePlayerHead() {
        return String.format("INSERT INTO %s (pUUID, hUUID) VALUES (?, ?)", getTablePlayerHeads());
    }

    public static String getContainsPlayer() {
        return String.format("SELECT 1 FROM %s WHERE pUUID = ?", getTablePlayers());
    }

    public static String getPlayerHeads() {
        return String.format("SELECT * FROM %s hbph INNER JOIN %s hbh ON hbph.hUUID = hbh.hUUID INNER JOIN %s hbp ON hbph.pUUID = hbp.pUUID WHERE hbp.pUUID = ? AND hbh.hExist = True", getTablePlayerHeads(), getTableHeads(), getTablePlayers());
    }

    public static String resetPlayer() {
        return String.format("DELETE FROM %s WHERE pUUID = ?", getTablePlayerHeads());
    }

    public static String resetPlayerHead() {
        return String.format("DELETE FROM %s WHERE pUUID = ? AND hUUID = ?", getTablePlayerHeads());
    }

    public static String removeHead() {
        return String.format("UPDATE %s SET hExist=False WHERE hUUID = ?", getTableHeads());
    }

    public static String deleteHead() {
        return String.format("DELETE FROM %s WHERE hUUID = ?", getTableHeads());
    }

    public static String getAllPlayers() {
        return String.format("SELECT pUUID FROM %s", getTablePlayers());
    }

    public static String getTopPlayers() {
        return String.format("SELECT hbp.pUUID, pName, pDisplayName, COUNT(*) as hCount FROM %s hbph INNER JOIN %s hbp ON hbph.pUUID = hbp.pUUID INNER JOIN %s hbh ON hbph.hUUID = hbh.hUUID WHERE hbh.hExist = True GROUP BY pName ORDER BY hCount DESC", getTablePlayerHeads(), getTablePlayers(), getTableHeads());
    }

    public static String getCheckPlayerName() {
        return String.format("SELECT pName, pDisplayName FROM %s WHERE pUUID = ?", getTablePlayers());
    }

    public static String getHeadExist() {
        return String.format("SELECT 1 FROM %s WHERE hUUID = ? AND hExist = True", getTableHeads());
    }

    // Migrations
    public static String migArchiveTable() {
        return String.format("CREATE TABLE IF NOT EXISTS %s (`pUUID` varchar(40) NOT NULL, `hUUID` varchar(40) NOT NULL, PRIMARY KEY (pUUID,`hUUID`))", HB_PLAYERS_OLD);
    }

    public static String migCopyOldToArchive() {
        return String.format("INSERT INTO %s SELECT * FROM %s", HB_PLAYERS_OLD, getTablePlayers());
    }

    public static String migDeleteOld() {
        return String.format(DROP_TABLE_FORMAT, getTablePlayers());
    }

    public static String migImportOldUsers() {
        return String.format("SELECT DISTINCT pUUID FROM %s", HB_PLAYERS_OLD);
    }

    public static String migInsertPlayer() {
        return String.format("INSERT INTO %s(`pUUID`, `pName`) VALUES (?, ?)", getTablePlayers());
    }

    public static String migImportOldHeads() {
        return String.format("INSERT INTO %s(`hUUID`, `hExist`) SELECT DISTINCT hUUID, True FROM %s", getTableHeads(), HB_PLAYERS_OLD);
    }

    public static String migRemap() {
        return String.format("INSERT INTO %s (pUUID, hUUID) SELECT pUUID, hUUID FROM %s", getTablePlayerHeads(), HB_PLAYERS_OLD);
    }

    public static String migDelArchive() {
        return String.format(DROP_TABLE_FORMAT, HB_PLAYERS_OLD);
    }

    public static String addColumnHeadTextureMariaDb() {
        return String.format("ALTER TABLE %s ADD COLUMN IF NOT EXISTS hTexture VARCHAR(255) DEFAULT ''", getTableHeads());
    }

    public static String addColumnHeadTextureMySQL() {
        return String.format("ALTER TABLE %s ADD COLUMN hTexture VARCHAR(255) DEFAULT ''", getTableHeads());
    }

    public static String addColumnHeadTextureSQLite() {
        return String.format("ALTER TABLE %s ADD COLUMN hTexture VARCHAR(255) DEFAULT ''", getTableHeads());
    }

    public static String getHeadTexture() {
        return String.format("SELECT hTexture FROM %s WHERE hUUID = (?)", getTableHeads());
    }

    public static String getPlayersByHead() {
        return String.format("SELECT pUUID FROM %s WHERE hUUID = (?)", getTablePlayerHeads());
    }

    public static String getPlayer() {
        return String.format("SELECT pUUID, pDisplayName FROM %s WHERE pName = (?)", getTablePlayers());
    }

    public static String addColumnPlayerDisplayNameMariaDb() {
        return String.format("ALTER TABLE %s ADD COLUMN IF NOT EXISTS pDisplayName VARCHAR(255) DEFAULT ''", getTablePlayers());
    }

    public static String addColumnPlayerDisplayNameSQLite() {
        return String.format("ALTER TABLE %s ADD COLUMN pDisplayName VARCHAR(255) DEFAULT ''", getTablePlayers());
    }

    public static String addColumnServerIdentifierSQLite() {
        return String.format("ALTER TABLE %s ADD COLUMN serverId VARCHAR(8) DEFAULT ''", getTableHeads());
    }

    public static String addColumnServerIdentifierMariaDb() {
        return String.format("ALTER TABLE %s ADD COLUMN IF NOT EXISTS serverId VARCHAR(8) DEFAULT ''", getTableHeads());
    }

    public static String isColumnExist() {
        return String.format("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = '%s' AND TABLE_NAME = ? AND COLUMN_NAME = ?", databaseName);
    }

    public static String addColumnServerIdentifierMySQL() {
        return String.format("ALTER TABLE %s ADD COLUMN serverId VARCHAR(8) DEFAULT ''", getTableHeads());
    }

    public static String addColumnPlayerDisplayNameMySQL() {
        return String.format("ALTER TABLE %s ADD COLUMN pDisplayName VARCHAR(255) DEFAULT ''", getTablePlayers());
    }

    public static String getDistinctServerIds() {
        return String.format("SELECT DISTINCT serverId FROM %s WHERE serverId IS NOT NULL AND serverId != ''", getTableHeads());
    }

    // --- Hunt tables (v5) ---

    public static String createTableHunts() {
        return String.format("CREATE TABLE IF NOT EXISTS %s (`hId` VARCHAR(64) PRIMARY KEY, `hName` VARCHAR(128) NOT NULL, `hState` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE')", getTableHunts());
    }

    // --- Hunt CRUD ---

    public static String insertHunt() {
        return String.format("INSERT INTO %s (hId, hName, hState) VALUES (?, ?, ?)", getTableHunts());
    }

    public static String updateHuntState() {
        return String.format("UPDATE %s SET hState = ? WHERE hId = ?", getTableHunts());
    }

    public static String updateHuntName() {
        return String.format("UPDATE %s SET hName = ? WHERE hId = ?", getTableHunts());
    }

    public static String deleteHuntById() {
        return String.format("DELETE FROM %s WHERE hId = ?", getTableHunts());
    }

    public static String getHuntsAll() {
        return String.format("SELECT hId, hName, hState FROM %s", getTableHunts());
    }

    public static String getHuntById() {
        return String.format("SELECT hId, hName, hState FROM %s WHERE hId = ?", getTableHunts());
    }

    // --- Hunt-aware player progression ---

    public static String savePlayerHeadHunt() {
        return String.format("INSERT INTO %s (pUUID, hUUID, huntId) VALUES (?, ?, ?)", getTablePlayerHeads());
    }

    public static String getPlayerHeadsForHunt() {
        return String.format("SELECT hbph.hUUID FROM %s hbph INNER JOIN %s hbh ON hbph.hUUID = hbh.hUUID WHERE hbph.pUUID = ? AND hbph.huntId = ? AND hbh.hExist = True", getTablePlayerHeads(), getTableHeads());
    }

    public static String resetPlayerHunt() {
        return String.format("DELETE FROM %s WHERE pUUID = ? AND huntId = ?", getTablePlayerHeads());
    }

    public static String resetPlayerHeadHunt() {
        return String.format("DELETE FROM %s WHERE pUUID = ? AND hUUID = ? AND huntId = ?", getTablePlayerHeads());
    }

    public static String getTopPlayersForHunt() {
        return String.format("SELECT hbp.pUUID, pName, pDisplayName, COUNT(*) as hCount FROM %s hbph INNER JOIN %s hbp ON hbph.pUUID = hbp.pUUID INNER JOIN %s hbh ON hbph.hUUID = hbh.hUUID WHERE hbh.hExist = True AND hbph.huntId = ? GROUP BY pName ORDER BY hCount DESC", getTablePlayerHeads(), getTablePlayers(), getTableHeads());
    }

    public static String transferPlayerProgressSQLite() {
        return String.format("INSERT OR IGNORE INTO %s (pUUID, hUUID, huntId) SELECT pUUID, hUUID, ? FROM %s WHERE huntId = ?", getTablePlayerHeads(), getTablePlayerHeads());
    }

    public static String transferPlayerProgressMySQL() {
        return String.format("INSERT IGNORE INTO %s (pUUID, hUUID, huntId) SELECT pUUID, hUUID, ? FROM %s WHERE huntId = ?", getTablePlayerHeads(), getTablePlayerHeads());
    }

    public static String deletePlayerProgressForHunt() {
        return String.format("DELETE FROM %s WHERE huntId = ?", getTablePlayerHeads());
    }

    // --- Migration v5 ---

    public static String addColumnHuntIdSQLite() {
        return String.format("ALTER TABLE %s ADD COLUMN huntId VARCHAR(64) NOT NULL DEFAULT 'default'", getTablePlayerHeads());
    }

    public static String addColumnHuntIdMariaDb() {
        return String.format("ALTER TABLE %s ADD COLUMN IF NOT EXISTS huntId VARCHAR(64) NOT NULL DEFAULT 'default'", getTablePlayerHeads());
    }

    public static String addColumnHuntIdMySQL() {
        return String.format("ALTER TABLE %s ADD COLUMN huntId VARCHAR(64) NOT NULL DEFAULT 'default'", getTablePlayerHeads());
    }

    public static String migV5InsertDefaultHunt() {
        return String.format("INSERT INTO %s (hId, hName, hState) VALUES ('default', 'Default', 'ACTIVE')", getTableHunts());
    }

    public static String migV5CreateTempPlayerHeadsSQLite() {
        return String.format("CREATE TABLE %s (`pUUID` VARCHAR(36), `hUUID` VARCHAR(36) REFERENCES %s(hUUID) ON DELETE CASCADE, `huntId` VARCHAR(64) NOT NULL DEFAULT 'default', PRIMARY KEY(pUUID, hUUID, huntId))", getTablePlayerHeads() + V5_TEMP_SUFFIX, getTableHeads());
    }

    public static String migV5CopyPlayerHeadsToTempSQLite() {
        return String.format("INSERT INTO %s (pUUID, hUUID, huntId) SELECT pUUID, hUUID, 'default' FROM %s", getTablePlayerHeads() + V5_TEMP_SUFFIX, getTablePlayerHeads());
    }

    public static String migV5DropOldPlayerHeadsSQLite() {
        return String.format(DROP_TABLE_FORMAT, getTablePlayerHeads());
    }

    public static String migV5RenameTempPlayerHeadsSQLite() {
        return String.format("ALTER TABLE %s RENAME TO %s", getTablePlayerHeads() + V5_TEMP_SUFFIX, getTablePlayerHeads());
    }

    // --- Timed runs (v6) ---

    public static String getTableTimedRuns() {
        return addPrefix() + "hb_timed_runs";
    }

    public static String createTableTimedRuns() {
        return String.format("CREATE TABLE IF NOT EXISTS %s (`pUUID` VARCHAR(36) NOT NULL, `huntId` VARCHAR(64) NOT NULL, `timeMs` BIGINT NOT NULL, `completedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (pUUID, huntId, completedAt))", getTableTimedRuns());
    }

    public static String createTableTimedRunsMySQL() {
        return String.format("CREATE TABLE IF NOT EXISTS %s (`pUUID` VARCHAR(36) NOT NULL, `huntId` VARCHAR(64) NOT NULL, `timeMs` BIGINT NOT NULL, `completedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (pUUID, huntId, completedAt))", getTableTimedRuns());
    }

    public static String insertTimedRun() {
        return String.format("INSERT INTO %s (pUUID, huntId, timeMs) VALUES (?, ?, ?)", getTableTimedRuns());
    }

    public static String getTimedLeaderboard() {
        return String.format("SELECT tr.pUUID, p.pName, p.pDisplayName, MIN(tr.timeMs) as bestTime FROM %s tr INNER JOIN %s p ON tr.pUUID = p.pUUID WHERE tr.huntId = ? GROUP BY tr.pUUID ORDER BY bestTime ASC LIMIT ?", getTableTimedRuns(), getTablePlayers());
    }

    public static String getBestTime() {
        return String.format("SELECT MIN(timeMs) as bestTime FROM %s WHERE pUUID = ? AND huntId = ?", getTableTimedRuns());
    }

    public static String getTimedRunCount() {
        return String.format("SELECT COUNT(*) as cnt FROM %s WHERE pUUID = ? AND huntId = ?", getTableTimedRuns());
    }
}