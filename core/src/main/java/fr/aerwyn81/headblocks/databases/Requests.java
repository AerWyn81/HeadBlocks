package fr.aerwyn81.headblocks.databases;

import fr.aerwyn81.headblocks.services.ConfigService;

public class Requests {
    private static String getTablePlayers() {
        return ConfigService.getDatabasePrefix() + "hb_players";
    }

    private static String getTableHeads() {
        return ConfigService.getDatabasePrefix() + "hb_heads";
    }

    private static String getTablePlayerHeads() {
        return ConfigService.getDatabasePrefix() + "hb_playerHeads";
    }

    private static String getTableVersion() {
        return ConfigService.getDatabasePrefix() + "hb_version";
    }

    public static String getIsTablePlayersExistSQLite() {
        return String.format("SELECT name FROM sqlite_master WHERE type='table' AND name='%s'", getTablePlayers());
    }

    public static String getTableHeadsColumnsSQLite() {
        return String.format("SELECT COUNT(*) AS count FROM pragma_table_info('%s');", getTableHeads());
    }

    public static String getIsTablePlayersExistMySQL() {
        return String.format("SELECT TABLE_NAME FROM information_schema.tables WHERE table_name = '%s' LIMIT 1", getTablePlayers());
    }

    public static String getTableHeadsColumnsMySQL() {
        return String.format("SELECT COUNT(*) AS count FROM INFORMATION_SCHEMA.COLUMNS WHERE table_name = '%s'", getTableHeads());
    }

    public static String getCreateTablePlayers() {
        return String.format("CREATE TABLE IF NOT EXISTS %s (`pId` INTEGER PRIMARY KEY AUTOINCREMENT, `pUUID` VARCHAR(36) UNIQUE NOT NULL, `pName` VARCHAR(16) NOT NULL, `pDisplayName` VARCHAR(255) NOT NULL)", getTablePlayers());
    }

    public static String getCreateTablePlayersMySQL() {
        return String.format("CREATE TABLE IF NOT EXISTS %s (`pId` INTEGER PRIMARY KEY AUTO_INCREMENT, `pUUID` VARCHAR(36) UNIQUE NOT NULL, `pName` VARCHAR(16) NOT NULL, `pDisplayName` VARCHAR(255) NOT NULL)", getTablePlayers());
    }

    public static String getTablePlayer() {
        return String.format("SELECT pUUID, pName FROM %s", getTablePlayers());
    }

    public static String getCreateTableHeads() {
        return String.format("CREATE TABLE IF NOT EXISTS %s (`hId` INTEGER PRIMARY KEY AUTOINCREMENT, `hUUID` VARCHAR(36) UNIQUE NOT NULL,`hExist` BOOLEAN NOT NULL CHECK (hExist IN (0, 1)), `hTexture` VARCHAR(255))", getTableHeads());
    }

    public static String getContainsTableHeads() {
        return String.format("SELECT * FROM %s LIMIT 1", getTableHeads());
    }

    public static String getCreateTableHeadsMySQL() {
        return String.format("CREATE TABLE IF NOT EXISTS %s (`hId` INTEGER PRIMARY KEY AUTO_INCREMENT, `hUUID` VARCHAR(36) UNIQUE NOT NULL,`hExist` BOOLEAN NOT NULL CHECK (hExist IN (0, 1)), `hTexture` VARCHAR(255))", getTableHeads());
    }

    public static String getTableHeadsData() {
        return String.format("SELECT hUUID, hExist FROM %s", getTableHeads());
    }

    public static String getCreateTablePlayerHeads() {
        return String.format("CREATE TABLE IF NOT EXISTS %s (`pUUID` VARCHAR(36), `hUUID` VARCHAR(36) REFERENCES %s(hUUID) ON DELETE CASCADE, PRIMARY KEY(pUUID, hUUID))", getTablePlayerHeads(), getTableHeads());
    }

    public static String getCreateTablePlayerHeadsMySQL() {
        return String.format("CREATE TABLE IF NOT EXISTS %s (`pUUID` VARCHAR(36), `hUUID` VARCHAR(36), FOREIGN KEY (`hUUID`) REFERENCES %s (`hUUID`) ON DELETE CASCADE)", getTablePlayerHeads(), getTableHeads());
    }

    public static String getTablePlayerHeadsData() {
        return String.format("SELECT pUUID, hUUID FROM %s", getTablePlayerHeads());
    }

    public static String getCreateTableVersion() {
        return String.format("CREATE TABLE IF NOT EXISTS %s (`current` INTEGER)", getTableVersion());
    }

    public static String getTableVersionData() {
        return String.format("SELECT current FROM %s", getTableVersion());
    }

    public static String getInsertVersion() {
        return String.format("INSERT INTO %s VALUES (?)", getTableVersion());
    }

    public static String getUpsertVersion() {
        return String.format("UPDATE %s SET current = (?) WHERE current = (?)", getTableVersion());
    }

    public static String getUpdatePlayer() {
        return String.format("INSERT OR REPLACE INTO %s (pUUID, pName, pDisplayName) VALUES (?, ?, ?)", getTablePlayers());
    }

    public static String getUpdatePlayerMySQL() {
        return String.format("REPLACE INTO %s (pUUID, pName, pDisplayName) VALUES (?, ?, ?)", getTablePlayers());
    }

    public static String getHeads() {
        return String.format("SELECT * FROM %s WHERE hExist = True", getTableHeads());
    }

    public static String getUpdateHead() {
        return String.format("INSERT OR REPLACE INTO %s (hUUID, hExist, hTexture) VALUES (?, true, ?)", getTableHeads());
    }

    public static String getUpdateHeadMySQL() {
        return String.format("REPLACE INTO %s (hUUID, hExist, hTexture) VALUES (?, true, ?)", getTableHeads());
    }

    public static String getSavePlayerHead() {
        return String.format("INSERT INTO %s (pUUID, hUUID) VALUES (?, ?)", getTablePlayerHeads());
    }

    public static String getContainsPlayer() {
        return String.format("SELECT 1 FROM %s WHERE pUUID = ?", getTablePlayers());
    }

    public static String getPlayerHeads() {
        return String.format("SELECT * FROM %s hbph INNER JOIN %s hbh ON hbph.hUUID = hbh.hUUID INNER JOIN %s hbp ON hbph.pUUID = hbp.pUUID WHERE hbp.pUUID = ? AND hbh.hExist = True", getTablePlayerHeads(), getTableHeads(), getTablePlayers());
    }

    public static String getResetPlayer() {
        return String.format("DELETE FROM %s WHERE pUUID = ?", getTablePlayerHeads());
    }

    public static String getRemoveHead() {
        return String.format("UPDATE %s SET hExist=False WHERE hUUID = ?", getTableHeads());
    }

    public static String getDeleteHead() {
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
    public static String getMigArchiveTable() {
        return String.format("CREATE TABLE IF NOT EXISTS %s (`pUUID` varchar(40) NOT NULL, `hUUID` varchar(40) NOT NULL, PRIMARY KEY (pUUID,`hUUID`))", "hb_players_old");
    }

    public static String getMigCopyOldToArchive() {
        return String.format("INSERT INTO %s SELECT * FROM %s", "hb_players_old", getTablePlayers());
    }

    public static String getMigDeleteOld() {
        return String.format("DROP TABLE %s", getTablePlayers());
    }

    public static String getMigImportOldUsers() {
        return String.format("SELECT DISTINCT pUUID FROM %s", "hb_players_old");
    }

    public static String getMigInsertPlayer() {
        return String.format("INSERT INTO %s(`pUUID`, `pName`) VALUES (?, ?)", getTablePlayers());
    }

    public static String getMigImportOldHeads() {
        return String.format("INSERT INTO %s(`hUUID`, `hExist`) SELECT DISTINCT hUUID, True FROM %s", getTableHeads(), "hb_players_old");
    }

    public static String getMigRemap() {
        return String.format("INSERT INTO %s SELECT * FROM %s", getTablePlayerHeads(), "hb_players_old");
    }

    public static String getMigDelArchive() {
        return String.format("DROP TABLE %s", "hb_players_old");
    }

    public static String getAddColumnHeadTextureMySQL() {
        return String.format("ALTER TABLE %s ADD COLUMN IF NOT EXISTS hTexture VARCHAR(255) DEFAULT ''", getTableHeads());
    }

    public static String getAddColumnHeadTextureSQLite() {
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

    public static String getAddColumnPlayerDisplayNameMySQL() {
        return String.format("ALTER TABLE %s ADD COLUMN IF NOT EXISTS pDisplayName VARCHAR(255) DEFAULT ''", getTablePlayers());
    }

    public static String getAddColumnPlayerDisplayNameSQLite() {
        return String.format("ALTER TABLE %s ADD COLUMN pDisplayName VARCHAR(255) DEFAULT ''", getTablePlayers());
    }
}