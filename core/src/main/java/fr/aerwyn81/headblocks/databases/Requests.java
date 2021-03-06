package fr.aerwyn81.headblocks.databases;

public class Requests {
    public static final String CREATE_TABLE_PLAYERS = "CREATE TABLE IF NOT EXISTS hb_players (`pId` INTEGER PRIMARY KEY AUTOINCREMENT, `pUUID` VARCHAR(36) UNIQUE NOT NULL, `pName` VARCHAR(16) NOT NULL)";
    public static final String CREATE_TABLE_PLAYERS_MYSQL = "CREATE TABLE IF NOT EXISTS hb_players (`pId` INTEGER PRIMARY KEY AUTO_INCREMENT, `pUUID` VARCHAR(36) UNIQUE NOT NULL, `pName` VARCHAR(16) NOT NULL)";

    public static final String CREATE_TABLE_HEADS = "CREATE TABLE IF NOT EXISTS hb_heads (`hId` INTEGER PRIMARY KEY AUTOINCREMENT, `hUUID` VARCHAR(36) UNIQUE NOT NULL,`hExist` BOOLEAN NOT NULL CHECK (hExist IN (0, 1)))";
    public static final String CREATE_TABLE_HEADS_MYSQL = "CREATE TABLE IF NOT EXISTS hb_heads (`hId` INTEGER PRIMARY KEY AUTO_INCREMENT, `hUUID` VARCHAR(36) UNIQUE NOT NULL,`hExist` BOOLEAN NOT NULL CHECK (hExist IN (0, 1)))";

    public static final String CREATE_TABLE_PLAYERHEADS = "CREATE TABLE IF NOT EXISTS hb_playerHeads (`pUUID` VARCHAR(36), `hUUID` VARCHAR(36) REFERENCES hb_heads(hUUID) ON DELETE CASCADE, PRIMARY KEY(pUUID, hUUID))";
    public static final String CREATE_TABLE_PLAYERHEADS_MYSQL = "CREATE TABLE IF NOT EXISTS hb_playerHeads (`pUUID` VARCHAR(36), `hUUID` VARCHAR(36), FOREIGN KEY (`hUUID`) REFERENCES hb_heads (`hUUID`) ON DELETE CASCADE)";

    public static final String UPDATE_PLAYER = "INSERT OR REPLACE INTO hb_players (pUUID, pName) VALUES (?, ?)";
    public static final String UPDATE_PLAYER_MYSQL = "REPLACE INTO hb_players (pUUID, pName) VALUES (?, ?)";

    public static final String CREATE_HEAD = "INSERT INTO hb_heads (hUUID, hExist) VALUES (?, true)";

    public static final String SAVE_PLAYERHEAD = "INSERT INTO hb_playerheads (pUUID, hUUID) VALUES (?, ?)";

    public static final String HAS_HEAD = "SELECT pUUID, hbh.hUUID, hbh.hExist FROM hb_playerHeads hbph INNER JOIN hb_heads hbh ON hbph.hUUID = hbh.hUUID WHERE pUUID = ? AND hbh.hUUID = ? AND hbh.hExist = True";

    public static final String CONTAINS_PLAYER = "SELECT 1 FROM hb_players WHERE pUUID = ?";

    public static final String PLAYER_HEADS = "SELECT hbh.hUUID FROM hb_playerHeads hbph INNER JOIN hb_heads hbh ON hbph.hUUID = hbh.hUUID WHERE pUUID = ? AND hbh.hExist = True";

    public static final String RESET_PLAYER = "DELETE FROM hb_playerHeads WHERE pUUID = ?";

    public static final String REMOVE_HEAD = "UPDATE hb_heads SET hExist=False WHERE hUUID = ?";

    public static final String DELETE_HEAD = "DELETE FROM hb_heads WHERE hUUID = ?";

    public static final String ALL_PLAYERS = "SELECT pUUID FROM hb_players";

    public static final String TOP_PLAYERS = "SELECT pName, COUNT(*) as hCount FROM hb_playerHeads hbph INNER JOIN hb_players hbp ON hbph.pUUID = hbp.pUUID INNER JOIN hb_heads hbh ON hbph.hUUID = hbh.hUUID WHERE hbh.hExist = True GROUP BY pName ORDER BY hCount DESC LIMIT ?";

    public static final String CHECK_PLAYER_NAME = "SELECT pName FROM hb_players WHERE pUUID = ?";

    public static final String HEAD_EXIST = "SELECT 1 FROM hb_heads WHERE hUUID = ? AND hExist = True";
}