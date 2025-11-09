package fr.aerwyn81.headblocks.databases;

import fr.aerwyn81.headblocks.services.ConfigService;

public class Requests {
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

    private static String addPrefix() {
        var prefix = "";

        if (ConfigService.isDatabaseEnabled()) {
            prefix = ConfigService.getDatabasePrefix();
        }

        return prefix;
    }

    public static String getIsTablePlayersExistSQLite() {
        return String.format("SELECT name FROM sqlite_master WHERE type='table' AND name='%s'", "hb_players");
    }

    public static String getTableHeadsColumnsSQLite() {
        return String.format("SELECT COUNT(*) AS count FROM pragma_table_info('%s');", getTableHeads());
    }

    public static String getIsTablePlayersExistMySQL() {
        return String.format("SELECT TABLE_NAME FROM information_schema.tables WHERE TABLE_SCHEMA = '%s' AND table_name = '%s' LIMIT 1", ConfigService.getDatabaseName(), getTablePlayers());
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
        return String.format("CREATE TABLE IF NOT EXISTS %s (`hId` INTEGER PRIMARY KEY AUTOINCREMENT, `hUUID` VARCHAR(36) UNIQUE NOT NULL,`hExist` BOOLEAN NOT NULL CHECK (hExist IN (0, 1)), `hTexture` VARCHAR(255), `serverId` VARCHAR(8))", getTableHeads());
    }

    public static String getContainsTableHeads() {
        return String.format("SELECT * FROM %s LIMIT 1", getTableHeads());
    }

    public static String createTableHeadsMySQL() {
        return String.format("CREATE TABLE IF NOT EXISTS %s (`hId` INTEGER PRIMARY KEY AUTO_INCREMENT, `hUUID` VARCHAR(36) UNIQUE NOT NULL,`hExist` BOOLEAN NOT NULL CHECK (hExist IN (0, 1)), `hTexture` VARCHAR(255), `serverId` VARCHAR(8))", getTableHeads());
    }

    public static String getTableHeadsData() {
        return String.format("SELECT hUUID, hExist FROM %s", getTableHeads());
    }

    public static String createTablePlayerHeads() {
        return String.format("CREATE TABLE IF NOT EXISTS %s (`pUUID` VARCHAR(36), `hUUID` VARCHAR(36) REFERENCES %s(hUUID) ON DELETE CASCADE, PRIMARY KEY(pUUID, hUUID))", getTablePlayerHeads(), getTableHeads());
    }

    public static String createTablePlayerHeadsMySQL() {
        return String.format("CREATE TABLE IF NOT EXISTS %s (`pUUID` VARCHAR(36), `hUUID` VARCHAR(36), FOREIGN KEY (`hUUID`) REFERENCES %s (`hUUID`) ON DELETE CASCADE)", getTablePlayerHeads(), getTableHeads());
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
        return String.format("SELECT * FROM %s WHERE hExist = True", getTableHeads());
    }

    public static String getHeadsMySQL() {
        return String.format("SELECT * FROM %s WHERE hExist = True AND serverId != ''", getTableHeads());
    }

    public static String getHeadsByServerId() {
        return String.format("SELECT * FROM %s WHERE hExist = True AND serverId = ?", getTableHeads());
    }

    public static String updateHead() {
        return String.format("INSERT OR REPLACE INTO %s (hUUID, hExist, hTexture, serverId) VALUES (?, true, ?, ?)", getTableHeads());
    }

    public static String updateHeadMySQL() {
        return String.format("REPLACE INTO %s (hUUID, hExist, hTexture, serverId) VALUES (?, true, ?, ?)", getTableHeads());
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
        return String.format("CREATE TABLE IF NOT EXISTS %s (`pUUID` varchar(40) NOT NULL, `hUUID` varchar(40) NOT NULL, PRIMARY KEY (pUUID,`hUUID`))", "hb_players_old");
    }

    public static String migCopyOldToArchive() {
        return String.format("INSERT INTO %s SELECT * FROM %s", "hb_players_old", getTablePlayers());
    }

    public static String migDeleteOld() {
        return String.format("DROP TABLE %s", getTablePlayers());
    }

    public static String migImportOldUsers() {
        return String.format("SELECT DISTINCT pUUID FROM %s", "hb_players_old");
    }

    public static String migInsertPlayer() {
        return String.format("INSERT INTO %s(`pUUID`, `pName`) VALUES (?, ?)", getTablePlayers());
    }

    public static String migImportOldHeads() {
        return String.format("INSERT INTO %s(`hUUID`, `hExist`) SELECT DISTINCT hUUID, True FROM %s", getTableHeads(), "hb_players_old");
    }

    public static String migRemap() {
        return String.format("INSERT INTO %s SELECT * FROM %s", getTablePlayerHeads(), "hb_players_old");
    }

    public static String migDelArchive() {
        return String.format("DROP TABLE %s", "hb_players_old");
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
        return String.format("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = '%s' AND TABLE_NAME = ? AND COLUMN_NAME = ?", ConfigService.getDatabaseName());
    }

    public static String addColumnServerIdentifierMySQL() {
        return String.format("ALTER TABLE %s ADD COLUMN serverId VARCHAR(8) DEFAULT ''", getTableHeads());
    }

    public static String addColumnPlayerDisplayNameMySQL() {
        return String.format("ALTER TABLE %s ADD COLUMN pDisplayName VARCHAR(255) DEFAULT ''", getTablePlayers());
    }
}