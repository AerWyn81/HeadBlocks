package fr.aerwyn81.headblocks.databases.types;

import com.zaxxer.hikari.HikariDataSource;
import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.databases.Database;
import fr.aerwyn81.headblocks.databases.Requests;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;

@SuppressWarnings({"SqlSourceToSinkFlow"})
public abstract class AbstractDatabase implements Database {

    protected HikariDataSource dataSource;

    protected abstract String getUpdatePlayerSql();

    protected abstract String getUpdateHeadSql();

    protected abstract String getHeadsSql();

    protected abstract String getTransferProgressSql();

    protected abstract String getTableExistSql();

    protected abstract void createTables(Connection conn) throws SQLException;

    @Override
    public void load() throws InternalException {
        try (var conn = dataSource.getConnection()) {
            createTables(conn);

            if (checkVersion(conn) == 0) {
                insertVersion(conn);
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public int checkVersion() {
        try (var conn = dataSource.getConnection()) {
            return checkVersion(conn);
        } catch (Exception ex) {
            return -1;
        }
    }

    @Override
    public void insertVersion() throws InternalException {
        try (var conn = dataSource.getConnection()) {
            insertVersion(conn);
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    // --- Player operations ---

    @Override
    public void updatePlayerInfo(PlayerProfileLight profile) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(getUpdatePlayerSql())) {
            ps.setString(1, profile.uuid().toString());
            ps.setString(2, profile.name());
            ps.setString(3, profile.customDisplay());
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void createNewHead(UUID hUUID, String texture, String serverId) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(getUpdateHeadSql())) {
            ps.setString(1, hUUID.toString());
            ps.setString(2, texture);
            ps.setString(3, serverId);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public boolean containsPlayer(UUID pUUID) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getContainsPlayer())) {
            ps.setString(1, pUUID.toString());
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public ArrayList<UUID> getHeadsPlayer(UUID pUUID) throws InternalException {
        ArrayList<UUID> heads = new ArrayList<>();

        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getPlayerHeads())) {
            ps.setString(1, pUUID.toString());

            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    heads.add(UUID.fromString(rs.getString("hUUID")));
                }
            }

            return heads;
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void addHead(UUID pUUID, UUID hUUID) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.savePlayerHead())) {
            ps.setString(1, pUUID.toString());
            ps.setString(2, hUUID.toString());
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void resetPlayer(UUID pUUID) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.resetPlayer())) {
            ps.setString(1, pUUID.toString());
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void resetPlayerHead(UUID pUUID, UUID hUUID) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.resetPlayerHead())) {
            ps.setString(1, pUUID.toString());
            ps.setString(2, hUUID.toString());
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void removeHead(UUID hUUID, boolean withDelete) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(withDelete ? Requests.deleteHead() : Requests.removeHead())) {
            ps.setString(1, hUUID.toString());
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public ArrayList<UUID> getAllPlayers() throws InternalException {
        ArrayList<UUID> players = new ArrayList<>();

        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getAllPlayers())) {
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    players.add(UUID.fromString(rs.getString("pUUID")));
                }
            }

            return players;
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public LinkedHashMap<PlayerProfileLight, Integer> getTopPlayers() throws InternalException {
        LinkedHashMap<PlayerProfileLight, Integer> top = new LinkedHashMap<>();

        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getTopPlayers())) {
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    top.put(new PlayerProfileLight(UUID.fromString(rs.getString("pUUID")), rs.getString("pName"), rs.getString("pDisplayName")), rs.getInt("hCount"));
                }
            }

            return top;
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public boolean hasPlayerRenamed(PlayerProfileLight profile) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getCheckPlayerName())) {
            ps.setString(1, profile.uuid().toString());
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return !profile.name().equals(rs.getString("pName")) || !profile.customDisplay().equals(rs.getString("pDisplayName"));
                }
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }

        return true;
    }

    @Override
    public boolean isHeadExist(UUID hUUID) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getHeadExist())) {
            ps.setString(1, hUUID.toString());
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    // --- Head queries ---

    @Override
    public ArrayList<UUID> getHeads() throws InternalException {
        var heads = new ArrayList<UUID>();

        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(getHeadsSql())) {
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    heads.add(UUID.fromString(rs.getString("hUUID")));
                }
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }

        return heads;
    }

    @Override
    public ArrayList<UUID> getHeads(String serverId) throws InternalException {
        var heads = new ArrayList<UUID>();

        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getHeadsByServerId())) {
            ps.setString(1, serverId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    heads.add(UUID.fromString(rs.getString("hUUID")));
                }
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }

        return heads;
    }

    @Override
    public String getHeadTexture(UUID headUuid) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getHeadTexture())) {
            ps.setString(1, headUuid.toString());
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("hTexture");
                }

                return "";
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public ArrayList<UUID> getPlayers(UUID headUuid) throws InternalException {
        var players = new ArrayList<UUID>();

        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getPlayersByHead())) {
            ps.setString(1, headUuid.toString());
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    players.add(UUID.fromString(rs.getString("pUUID")));
                }
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }

        return players;
    }

    @Override
    public PlayerProfileLight getPlayerByName(String pName) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getPlayer())) {
            ps.setString(1, pName);

            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PlayerProfileLight(UUID.fromString(rs.getString("pUUID")), pName, rs.getString("pDisplayName"));
                }

                return null;
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public boolean isDefaultTablesExist() {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(getTableExistSql())) {
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception ex) {
            return false;
        }
    }

    // --- Export ---

    @Override
    public void upsertTableVersion(int oldVersion) throws InternalException {
        try (var conn = dataSource.getConnection()) {
            try (var ps = conn.prepareStatement(Requests.createTableVersion())) {
                ps.execute();
            }
            try (var ps = conn.prepareStatement(Requests.upsertVersion())) {
                ps.setInt(1, version);
                ps.setInt(2, oldVersion);
                ps.executeUpdate();
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public ArrayList<Database.HeadExportRow> getTableHeads() throws InternalException {
        ArrayList<Database.HeadExportRow> heads = new ArrayList<>();

        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getTableHeadsData())) {
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    heads.add(new Database.HeadExportRow(rs.getString("hUUID"), rs.getBoolean("hExist")));
                }
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }

        return heads;
    }

    @Override
    public ArrayList<Database.PlayerHeadExportRow> getTablePlayerHeads() throws InternalException {
        ArrayList<Database.PlayerHeadExportRow> playerHeads = new ArrayList<>();

        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getTablePlayerHeadsData())) {
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    playerHeads.add(new Database.PlayerHeadExportRow(rs.getString("pUUID"), rs.getString("hUUID")));
                }
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }

        return playerHeads;
    }

    @Override
    public ArrayList<Database.PlayerExportRow> getTablePlayers() throws InternalException {
        ArrayList<Database.PlayerExportRow> playerHeads = new ArrayList<>();

        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getTablePlayer())) {
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    playerHeads.add(new Database.PlayerExportRow(rs.getString("pUUID"), rs.getString("pName")));
                }
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }

        return playerHeads;
    }

    @Override
    public ArrayList<String> getDistinctServerIds() throws InternalException {
        var serverIds = new ArrayList<String>();

        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getDistinctServerIds())) {
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    serverIds.add(rs.getString("serverId"));
                }
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }

        return serverIds;
    }

    // --- Migration v1→v2 ---

    @Override
    public void migrate() throws InternalException {
        try (var conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try {
                try (var ps = conn.prepareStatement(Requests.migArchiveTable())) {
                    ps.executeUpdate();
                }

                try (var ps = conn.prepareStatement(Requests.migCopyOldToArchive())) {
                    ps.executeUpdate();
                }

                try (var ps = conn.prepareStatement(Requests.migDeleteOld())) {
                    ps.executeUpdate();
                }

                createTables(conn);

                if (checkVersion(conn) == 0) {
                    insertVersion(conn);
                }

                try (var psSelect = conn.prepareStatement(Requests.migImportOldUsers());
                     var rs = psSelect.executeQuery();
                     var psInsert = conn.prepareStatement(Requests.migInsertPlayer())) {

                    int batchSize = 0;

                    while (rs.next()) {
                        String pUUID = rs.getString("pUUID");
                        String pName = PlayerUtils.getPseudoFromSession(pUUID);

                        psInsert.setString(1, pUUID);
                        psInsert.setString(2, pName);
                        psInsert.addBatch();

                        if (++batchSize % 500 == 0) {
                            psInsert.executeBatch();
                            batchSize = 0;
                        }
                    }

                    if (batchSize > 0) {
                        psInsert.executeBatch();
                    }
                } catch (SQLException e) {
                    throw new InternalException(e);
                }

                try (var ps = conn.prepareStatement(Requests.migImportOldHeads())) {
                    ps.executeUpdate();
                }

                try (var ps = conn.prepareStatement(Requests.migRemap())) {
                    ps.executeUpdate();
                }

                try (var ps = conn.prepareStatement(Requests.migDelArchive())) {
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    // --- Hunt CRUD (v5) ---

    @Override
    public void createHunt(String huntId, String name, String state) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.insertHunt())) {
            ps.setString(1, huntId);
            ps.setString(2, name);
            ps.setString(3, state);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void updateHuntState(String huntId, String state) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.updateHuntState())) {
            ps.setString(1, state);
            ps.setString(2, huntId);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void updateHuntName(String huntId, String name) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.updateHuntName())) {
            ps.setString(1, name);
            ps.setString(2, huntId);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void deleteHunt(String huntId) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.deleteHuntById())) {
            ps.setString(1, huntId);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public ArrayList<String[]> getHunts() throws InternalException {
        var hunts = new ArrayList<String[]>();

        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getHuntsAll())) {
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    hunts.add(new String[]{rs.getString("hId"), rs.getString("hName"), rs.getString("hState")});
                }
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }

        return hunts;
    }

    @Override
    public String[] getHuntById(String huntId) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getHuntById())) {
            ps.setString(1, huntId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[]{rs.getString("hId"), rs.getString("hName"), rs.getString("hState")};
                }
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }

        return null;
    }

    // --- Hunt-aware player progression (v5) ---

    @Override
    public void addHeadForHunt(UUID pUUID, UUID hUUID, String huntId) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.savePlayerHeadHunt())) {
            ps.setString(1, pUUID.toString());
            ps.setString(2, hUUID.toString());
            ps.setString(3, huntId);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public ArrayList<UUID> getHeadsPlayerForHunt(UUID pUUID, String huntId) throws InternalException {
        var heads = new ArrayList<UUID>();

        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getPlayerHeadsForHunt())) {
            ps.setString(1, pUUID.toString());
            ps.setString(2, huntId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    heads.add(UUID.fromString(rs.getString("hUUID")));
                }
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }

        return heads;
    }

    @Override
    public void resetPlayerHunt(UUID pUUID, String huntId) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.resetPlayerHunt())) {
            ps.setString(1, pUUID.toString());
            ps.setString(2, huntId);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void resetPlayerHeadHunt(UUID pUUID, UUID hUUID, String huntId) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.resetPlayerHeadHunt())) {
            ps.setString(1, pUUID.toString());
            ps.setString(2, hUUID.toString());
            ps.setString(3, huntId);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public LinkedHashMap<PlayerProfileLight, Integer> getTopPlayersForHunt(String huntId) throws InternalException {
        var top = new LinkedHashMap<PlayerProfileLight, Integer>();

        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getTopPlayersForHunt())) {
            ps.setString(1, huntId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    top.put(new PlayerProfileLight(UUID.fromString(rs.getString("pUUID")), rs.getString("pName"), rs.getString("pDisplayName")), rs.getInt("hCount"));
                }
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }

        return top;
    }

    @Override
    public void transferPlayerProgress(String fromHuntId, String toHuntId) throws InternalException {
        try (var conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (var ps = conn.prepareStatement(getTransferProgressSql())) {
                    ps.setString(1, toHuntId);
                    ps.setString(2, fromHuntId);
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement(Requests.deletePlayerProgressForHunt())) {
                    ps.setString(1, fromHuntId);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void deletePlayerProgressForHunt(String huntId) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.deletePlayerProgressForHunt())) {
            ps.setString(1, huntId);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    // --- Timed runs (v5) ---

    @Override
    public void saveTimedRun(UUID pUUID, String huntId, long timeMs) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.insertTimedRun())) {
            ps.setString(1, pUUID.toString());
            ps.setString(2, huntId);
            ps.setLong(3, timeMs);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public LinkedHashMap<PlayerProfileLight, Long> getTimedLeaderboard(String huntId, int limit) throws InternalException {
        var top = new LinkedHashMap<PlayerProfileLight, Long>();

        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getTimedLeaderboard())) {
            ps.setString(1, huntId);
            ps.setInt(2, limit);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    top.put(new PlayerProfileLight(
                            UUID.fromString(rs.getString("pUUID")),
                            rs.getString("pName"),
                            rs.getString("pDisplayName")
                    ), rs.getLong("bestTime"));
                }
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }

        return top;
    }

    @Override
    public Long getBestTime(UUID pUUID, String huntId) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getBestTime())) {
            ps.setString(1, pUUID.toString());
            ps.setString(2, huntId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    long val = rs.getLong("bestTime");
                    if (rs.wasNull()) {
                        return null;
                    }
                    return val;
                }
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
        return null;
    }

    @Override
    public int getTimedRunCount(UUID pUUID, String huntId) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getTimedRunCount())) {
            ps.setString(1, pUUID.toString());
            ps.setString(2, huntId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt");
                }
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
        return 0;
    }

    // --- Internal helpers ---

    protected int checkVersion(Connection conn) {
        try (var statement = conn.prepareStatement(Requests.getTableVersionData());
             var rs = statement.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("current");
            } else {
                return 0;
            }
        } catch (Exception ex) {
            return 0;
        }
    }

    protected void insertVersion(Connection conn) throws SQLException {
        try (var ps = conn.prepareStatement(Requests.createTableVersion())) {
            ps.execute();
        }

        try (var ps = conn.prepareStatement(Requests.insertVersion())) {
            ps.setInt(1, version);
            ps.executeUpdate();
        }
    }
}