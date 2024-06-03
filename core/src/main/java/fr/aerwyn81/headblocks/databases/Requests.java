package fr.aerwyn81.headblocks.databases;

public class Requests {
    public static final String IS_TABLE_PLAYERS_EXIST_SQLITE = "SELECT name FROM sqlite_master WHERE type='table' AND name='hb_players'";
    public static final String TABLE_HEADS_COLUMNS_SQLITE = "SELECT COUNT(*) AS count FROM pragma_table_info('hb_heads');";
    public static final String IS_TABLE_PLAYERS_EXIST_MYSQL = "SELECT TABLE_NAME FROM information_schema.tables WHERE table_name = 'hb_players' LIMIT 1";
    public static final String TABLE_HEADS_COLUMNS_MYSQL = "SELECT COUNT(*) AS count FROM INFORMATION_SCHEMA.COLUMNS WHERE table_name = 'hb_heads'";

    public static final String CREATE_TABLE_PLAYERS = "CREATE TABLE IF NOT EXISTS hb_players (`pId` INTEGER PRIMARY KEY AUTOINCREMENT, `pUUID` VARCHAR(36) UNIQUE NOT NULL, `pName` VARCHAR(16) NOT NULL, `pDisplayName` VARCHAR(255) NOT NULL)";
    public static final String CREATE_TABLE_PLAYERS_MYSQL = "CREATE TABLE IF NOT EXISTS hb_players (`pId` INTEGER PRIMARY KEY AUTO_INCREMENT, `pUUID` VARCHAR(36) UNIQUE NOT NULL, `pName` VARCHAR(16) NOT NULL, `pDisplayName` VARCHAR(255) NOT NULL)";
    public static final String GET_TABLE_PLAYER = "SELECT pUUID, pName FROM hb_players";

    public static final String CREATE_TABLE_HEADS = "CREATE TABLE IF NOT EXISTS hb_heads (`hId` INTEGER PRIMARY KEY AUTOINCREMENT, `hUUID` VARCHAR(36) UNIQUE NOT NULL,`hExist` BOOLEAN NOT NULL CHECK (hExist IN (0, 1)), `hTexture` VARCHAR(255))";
    public static final String CONTAINS_TABLE_HEADS = "SELECT * FROM hb_heads LIMIT 1";
    public static final String CREATE_TABLE_HEADS_MYSQL = "CREATE TABLE IF NOT EXISTS hb_heads (`hId` INTEGER PRIMARY KEY AUTO_INCREMENT, `hUUID` VARCHAR(36) UNIQUE NOT NULL,`hExist` BOOLEAN NOT NULL CHECK (hExist IN (0, 1)), `hTexture` VARCHAR(255))";
    public static final String GET_TABLE_HEADS = "SELECT hUUID, hExist FROM hb_heads";

    public static final String CREATE_TABLE_PLAYERHEADS = "CREATE TABLE IF NOT EXISTS hb_playerHeads (`pUUID` VARCHAR(36), `hUUID` VARCHAR(36) REFERENCES hb_heads(hUUID) ON DELETE CASCADE, PRIMARY KEY(pUUID, hUUID))";
    public static final String CREATE_TABLE_PLAYERHEADS_MYSQL = "CREATE TABLE IF NOT EXISTS hb_playerHeads (`pUUID` VARCHAR(36), `hUUID` VARCHAR(36), FOREIGN KEY (`hUUID`) REFERENCES hb_heads (`hUUID`) ON DELETE CASCADE)";
    public static final String GET_TABLE_PLAYERHEADS = "SELECT pUUID, hUUID FROM hb_playerHeads";

    public static final String CREATE_TABLE_VERSION = "CREATE TABLE IF NOT EXISTS hb_version (`current` INTEGER)";
    public static final String GET_TABLE_VERSION = "SELECT current FROM hb_version";
    public static final String INSERT_VERSION = "INSERT INTO hb_version VALUES (?)";
    public static final String UPSERT_VERSION = "UPDATE hb_version SET current = (?) WHERE current = (?)";

    public static final String UPDATE_PLAYER = "INSERT OR REPLACE INTO hb_players (pUUID, pName, pDisplayName) VALUES (?, ?, ?)";
    public static final String UPDATE_PLAYER_MYSQL = "REPLACE INTO hb_players (pUUID, pName, pDisplayName) VALUES (?, ?, ?)";

    public static final String CREATE_HEAD = "INSERT INTO hb_heads (hUUID, hExist, hTexture) VALUES (?, true, ?)";

    public static final String SAVE_PLAYERHEAD = "INSERT INTO hb_playerHeads (pUUID, hUUID) VALUES (?, ?)";

    public static final String CONTAINS_PLAYER = "SELECT 1 FROM hb_players WHERE pUUID = ?";

    public static final String PLAYER_HEADS = "SELECT * FROM hb_playerHeads hbph INNER JOIN hb_heads hbh ON hbph.hUUID = hbh.hUUID INNER JOIN hb_players hbp ON hbph.pUUID = hbp.pUUID WHERE hbp.pUUID = ? AND pName = ? AND hbh.hExist = True";

    public static final String RESET_PLAYER = "DELETE FROM hb_playerHeads WHERE pUUID = ?";

    public static final String REMOVE_HEAD = "UPDATE hb_heads SET hExist=False WHERE hUUID = ?";

    public static final String DELETE_HEAD = "DELETE FROM hb_heads WHERE hUUID = ?";

    public static final String ALL_PLAYERS = "SELECT pUUID FROM hb_players";

    public static final String TOP_PLAYERS = "SELECT hbp.pUUID, pName, pDisplayName, COUNT(*) as hCount FROM hb_playerHeads hbph INNER JOIN hb_players hbp ON hbph.pUUID = hbp.pUUID INNER JOIN hb_heads hbh ON hbph.hUUID = hbh.hUUID WHERE hbh.hExist = True GROUP BY pName ORDER BY hCount DESC";

    public static final String CHECK_PLAYER_NAME = "SELECT pName, pDisplayName FROM hb_players WHERE pUUID = ?";

    public static final String HEAD_EXIST = "SELECT 1 FROM hb_heads WHERE hUUID = ? AND hExist = True";

    // Migrate
    public static final String MIG_ARCHIVE_TABLE = "CREATE TABLE IF NOT EXISTS hb_players_old (`pUUID` varchar(40) NOT NULL, `hUUID` varchar(40) NOT NULL, PRIMARY KEY (pUUID,`hUUID`))";

    public static final String MIG_COPY_OLD_TO_ARCHIVE = "INSERT INTO hb_players_old SELECT * FROM hb_players";

    public static final String MIG_DELETE_OLD = "DROP TABLE hb_players";

    public static final String MIG_IMPORT_OLD_USERS = "SELECT DISTINCT pUUID FROM hb_players_old";

    public static final String MIG_INSERT_PLAYER = "INSERT INTO hb_players(`pUUID`, `pName`) VALUES (?, ?)";

    public static final String MIG_IMPORT_OLD_HEADS = "INSERT INTO hb_heads(`hUUID`, `hExist`) SELECT DISTINCT hUUID, True FROM hb_players_old";

    public static final String MIG_REMAP = "INSERT INTO hb_playerHeads SELECT * FROM hb_players_old";

    public static final String MIG_DEL_ARCHIVE = "DROP TABLE hb_players_old";

    public static final String ADD_COLUMN_HEAD_TEXTURE_MYSQL = "ALTER TABLE hb_heads ADD COLUMN IF NOT EXISTS hTexture VARCHAR(255) DEFAULT ''";
    public static final String ADD_COLUMN_HEAD_TEXTURE_SQLITE = "ALTER TABLE hb_heads ADD COLUMN hTexture VARCHAR(255) DEFAULT ''";

    public static final String GET_HEAD_TEXTURE = "SELECT hTexture FROM hb_heads WHERE hUUID = (?)";

    public static final String GET_PLAYERS_BY_HEAD = "SELECT pUUID FROM hb_playerHeads WHERE hUUID = (?)";

    public static final String GET_PLAYER = "SELECT pUUID, pDisplayName FROM hb_players WHERE pName = (?)";

    public static final String ADD_COLUMN_PLAYER_DISPLAYNAME_MYSQL = "ALTER TABLE hb_players ADD COLUMN IF NOT EXISTS pDisplayName VARCHAR(255) DEFAULT ''";
    public static final String ADD_COLUMN_PLAYER_DISPLAYNAME_SQLITE = "ALTER TABLE hb_players ADD COLUMN pDisplayName VARCHAR(255) DEFAULT ''";
}