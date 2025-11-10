package fr.aerwyn81.headblocks.databases.types;

import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.databases.Database;
import fr.aerwyn81.headblocks.databases.Requests;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;

@SuppressWarnings({"DuplicatedCode", "SqlResolve"})
public class SQLite implements Database {

    private final String pathToDatabase;
    private Connection connection;

    public SQLite(String pathToDatabase) {
        this.pathToDatabase = pathToDatabase;
    }

    /**
     * Open the SQLite connection
     *
     * @throws InternalException SQL exception
     */
    @SuppressWarnings("SqlNoDataSourceInspection")
    @Override
    public void open() throws InternalException {
        if (connection != null)
            return;

        try {
            synchronized (this) {
                connection = DriverManager.getConnection("jdbc:sqlite:" + pathToDatabase);
                try (var stmt = connection.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON;");
                }
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }


    /**
     * Close the SQLite connection
     *
     * @throws InternalException SQL exception
     */
    @Override
    public void close() throws InternalException {
        if (connection == null)
            return;

        try {
            connection.close();
        } catch (SQLException ex) {
            throw new InternalException(ex);
        }
    }

    /**
     * Ensure tables are created
     *
     * @throws InternalException SQL Exception
     */
    @Override
    public void load() throws InternalException {
        try {
            // Players
            try (var statement = connection.prepareStatement(Requests.createTablePlayers())) {
                statement.execute();
            }

            // Heads
            try (var statement = connection.prepareStatement(Requests.createTableHeads())) {
                statement.execute();
            }

            // Junction table (n-n)
            try (var statement = connection.prepareStatement(Requests.createTablePlayerHeads())) {
                statement.execute();
            }

            if (checkVersion() == 0) {
                insertVersion();
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    /**
     * Check version of the database
     *
     * @return if migration needed
     */
    @Override
    public int checkVersion() {
        try (var statement = connection.prepareStatement(Requests.getContainsTableHeads())) {
            statement.executeQuery();
        } catch (Exception ex) {
            return -1;
        }

        try (var statement = connection.prepareStatement(Requests.getTableVersionData())) {
            try (var rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("current");
                } else
                    return 0;
            }
        } catch (Exception ex) {
            return 0;
        }
    }

    /**
     * Create or update a player
     *
     * @param profile@throws InternalException SQL Exception
     */
    @Override
    public void updatePlayerInfo(PlayerProfileLight profile) throws InternalException {
        try (var ps = connection.prepareStatement(Requests.updatePlayer())) {
            ps.setString(1, profile.uuid().toString());
            ps.setString(2, profile.name());
            ps.setString(3, profile.customDisplay());

            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    /**
     * Create a head
     *
     * @param hUUID    head UUID
     * @param texture  head texture
     * @param serverId server identifier
     * @throws InternalException SQL Exception
     */
    @Override
    public void createNewHead(UUID hUUID, String texture, String serverId) throws InternalException {
        try (var ps = connection.prepareStatement(Requests.updateHead())) {
            ps.setString(1, hUUID.toString());
            ps.setString(2, texture);
            ps.setString(3, serverId);

            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    /**
     * Check if player exist
     *
     * @param pUUID player UUID
     * @return true if player exist
     * @throws InternalException SQL Exception
     */
    @Override
    public boolean containsPlayer(UUID pUUID) throws InternalException {
        try (var ps = connection.prepareStatement(Requests.getContainsPlayer())) {
            ps.setString(1, pUUID.toString());

            try (var rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    /**
     * Retrieve heads for a player
     *
     * @param pUUID player UUID
     * @return list of heads UUID
     * @throws InternalException SQL Exception
     */
    @Override
    public ArrayList<UUID> getHeadsPlayer(UUID pUUID) throws InternalException {
        ArrayList<UUID> heads = new ArrayList<>();

        try (var ps = connection.prepareStatement(Requests.getPlayerHeads())) {
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

    /**
     * Save a new head for the player
     *
     * @param pUUID player UUID
     * @param hUUID head UUID
     * @throws InternalException SQL Exception
     */
    @Override
    public void addHead(UUID pUUID, UUID hUUID) throws InternalException {
        try (var ps = connection.prepareStatement(Requests.savePlayerHead())) {
            ps.setString(1, pUUID.toString());
            ps.setString(2, hUUID.toString());

            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    /**
     * Reset the players heads
     *
     * @param pUUID player UUID
     * @throws InternalException SQL Exception
     */
    @Override
    public void resetPlayer(UUID pUUID) throws InternalException {
        try (var ps = connection.prepareStatement(Requests.resetPlayer())) {
            ps.setString(1, pUUID.toString());

            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    /**
     * Reset a specific head for a player
     *
     * @param pUUID player UUID
     * @param hUUID head UUID
     * @throws InternalException SQL Exception
     */
    @Override
    public void resetPlayerHead(UUID pUUID, UUID hUUID) throws InternalException {
        try (var ps = connection.prepareStatement(Requests.resetPlayerHead())) {
            ps.setString(1, pUUID.toString());
            ps.setString(2, hUUID.toString());

            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    /**
     * Remove a head
     *
     * @param hUUID      head UUID
     * @param withDelete should delete the head from the database
     * @throws InternalException SQL Exception
     */
    @Override
    public void removeHead(UUID hUUID, boolean withDelete) throws InternalException {
        try (var ps = connection.prepareStatement(withDelete ? Requests.deleteHead() : Requests.removeHead())) {
            ps.setString(1, hUUID.toString());

            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    /**
     * Retrieve all players stored
     *
     * @return list of player UUID
     * @throws InternalException SQL Exception
     */
    @Override
    public ArrayList<UUID> getAllPlayers() throws InternalException {
        ArrayList<UUID> players = new ArrayList<>();

        try (var ps = connection.prepareStatement(Requests.getAllPlayers())) {
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

    /**
     * Retrieve top players with a limit
     *
     * @return map of player name with head count
     * @throws InternalException SQL Exception
     */
    @Override
    public LinkedHashMap<PlayerProfileLight, Integer> getTopPlayers() throws InternalException {
        LinkedHashMap<PlayerProfileLight, Integer> top = new LinkedHashMap<>();

        try (var ps = connection.prepareStatement(Requests.getTopPlayers())) {
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

    /**
     * Check player name
     *
     * @param profile player profile
     * @return boolean if playername is equals
     * @throws InternalException SQL Exception
     */
    @Override
    public boolean hasPlayerRenamed(PlayerProfileLight profile) throws InternalException {
        try (var ps = connection.prepareStatement(Requests.getCheckPlayerName())) {
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

    /**
     * Check if head exist
     *
     * @param hUUID head uuid
     * @return true if head exist otherwise false
     * @throws InternalException SQL Exception
     */
    @Override
    public boolean isHeadExist(UUID hUUID) throws InternalException {
        try (var ps = connection.prepareStatement(Requests.getHeadExist())) {
            ps.setString(1, hUUID.toString());
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    /**
     * Migrate v1 to v2 database
     *
     * @throws InternalException SQL Exception
     */
    @Override
    public void migrate() throws InternalException {
        try {
            // Create of the archive table
            try (var ps = connection.prepareStatement(Requests.migArchiveTable())) {
                ps.executeUpdate();
            }

            // Copy old data into the archive table
            try (var ps = connection.prepareStatement(Requests.migCopyOldToArchive())) {
                ps.executeUpdate();
            }

            // Delete old table
            try (var ps = connection.prepareStatement(Requests.migDeleteOld())) {
                ps.executeUpdate();
            }

            // Creation of new v2 tables
            load();

            // Import old users
            try (var psSelect = connection.prepareStatement(Requests.migImportOldUsers());
                 var rs = psSelect.executeQuery();
                 var psInsert = connection.prepareStatement(Requests.migInsertPlayer())) {

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

            // Import old heads
            try (var ps = connection.prepareStatement(Requests.migImportOldHeads())) {
                ps.executeUpdate();
            }

            // Remap
            try (var ps = connection.prepareStatement(Requests.migRemap())) {
                ps.executeUpdate();
            }

            // Delete archive table
            try (var ps = connection.prepareStatement(Requests.migDelArchive())) {
                ps.executeUpdate();
            }

        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    public void insertVersion() throws InternalException {
        try (var ps = connection.prepareStatement(Requests.createTableVersion())) {
            ps.execute();

            try (var ps1 = connection.prepareStatement(Requests.insertVersion())) {
                ps1.setInt(1, version);
                ps1.executeUpdate();
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void addColumnDisplayName() throws InternalException {
        try (var ps = connection.prepareStatement(Requests.addColumnPlayerDisplayNameSQLite())) {
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public ArrayList<UUID> getHeads() throws InternalException {
        var heads = new ArrayList<UUID>();

        try (var ps = connection.prepareStatement(Requests.getHeads())) {
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

        try (PreparedStatement ps = connection.prepareStatement(Requests.getHeadsByServerId())) {
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
    public void addColumnServerIdentifier() throws InternalException {
        try (var ps = connection.prepareStatement(Requests.addColumnServerIdentifierSQLite())) {
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    /**
     * Add table version
     *
     * @throws InternalException SQL Exception
     */
    @Override
    public void upsertTableVersion(int oldVersion) throws InternalException {
        try (var ps = connection.prepareStatement(Requests.createTableVersion())) {
            ps.execute();
            try (var ps1 = connection.prepareStatement(Requests.upsertVersion())) {
                ps1.setInt(1, version);
                ps1.setInt(2, oldVersion);
                ps1.executeUpdate();
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public ArrayList<AbstractMap.SimpleEntry<String, Boolean>> getTableHeads() throws InternalException {
        ArrayList<AbstractMap.SimpleEntry<String, Boolean>> heads = new ArrayList<>();

        try (var ps = connection.prepareStatement(Requests.getTableHeadsData())) {
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    heads.add(new AbstractMap.SimpleEntry<>(rs.getString("hUUID"), rs.getBoolean("hExist")));
                }
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }

        return heads;
    }

    @Override
    public ArrayList<AbstractMap.SimpleEntry<String, String>> getTablePlayerHeads() throws InternalException {
        ArrayList<AbstractMap.SimpleEntry<String, String>> playerHeads = new ArrayList<>();

        try (var ps = connection.prepareStatement(Requests.getTablePlayerHeadsData())) {
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    playerHeads.add(new AbstractMap.SimpleEntry<>(rs.getString("pUUID"), rs.getString("hUUID")));
                }
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }

        return playerHeads;
    }

    @Override
    public ArrayList<AbstractMap.SimpleEntry<String, String>> getTablePlayers() throws InternalException {
        ArrayList<AbstractMap.SimpleEntry<String, String>> playerHeads = new ArrayList<>();

        try (var ps = connection.prepareStatement(Requests.getTablePlayer())) {
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    playerHeads.add(new AbstractMap.SimpleEntry<>(rs.getString("pUUID"), rs.getString("pName")));
                }
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }

        return playerHeads;
    }

    @Override
    public void addColumnHeadTexture() throws InternalException {
        try (var ps = connection.prepareStatement(Requests.getTableHeadsColumnsSQLite())) {
            try (var rs = ps.executeQuery()) {
                int colCount = 0;
                if (rs.next()) {
                    colCount = rs.getInt("count");
                }

                if (colCount == 3) {
                    try (var ps1 = connection.prepareStatement(Requests.addColumnHeadTextureSQLite())) {
                        ps1.executeUpdate();
                    }
                }
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public String getHeadTexture(UUID headUuid) throws InternalException {
        try (var ps = connection.prepareStatement(Requests.getHeadTexture())) {
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

        try (var ps = connection.prepareStatement(Requests.getPlayersByHead())) {
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
        try (var ps = connection.prepareStatement(Requests.getPlayer())) {
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
        try (var ps = connection.prepareStatement(Requests.getIsTablePlayersExistSQLite())) {
            ps.executeQuery();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}