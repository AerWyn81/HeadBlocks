package fr.aerwyn81.headblocks.databases.types;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.databases.Database;
import fr.aerwyn81.headblocks.databases.Requests;
import fr.aerwyn81.headblocks.services.ConfigService;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;

@SuppressWarnings("DuplicatedCode")
public final class MySQL implements Database {
    private final String user;
    private final String password;
    private final String hostname;
    private final int port;
    private final String databaseName;
    private final boolean isSsl;

    private HikariDataSource dataSource;

    public MySQL(String user, String password, String hostname, int port, String databaseName, boolean isSsl) {
        this.user = user;
        this.password = password;
        this.hostname = hostname;
        this.port = port;
        this.databaseName = databaseName;
        this.isSsl = isSsl;
    }

    /**
     * Open the MySQL connection pool
     */
    @Override
    public void open() throws InternalException {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + hostname + ":" + port + "/" + databaseName);
            config.setUsername(user);
            config.setPassword(password);
            config.setMaximumPoolSize(ConfigService.getDatabaseMaxConnections());
            config.setMinimumIdle(ConfigService.getDatabaseMinIdleConnections());
            config.setConnectionTimeout(ConfigService.getDatabaseConnectionTimeout());
            config.setIdleTimeout(ConfigService.getDatabaseIdleTimeout());
            config.setMaxLifetime(ConfigService.getDatabaseMaxLifetime());
            config.setPoolName("HeadBlocks-MySQL");
            if (!isSsl) {
                config.addDataSourceProperty("useSSL", "false");
                config.addDataSourceProperty("verifyServerCertificate", "false");
            }
            dataSource = new HikariDataSource(config);
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    /**
     * Close the MySQL connection pool
     */
    @Override
    public void close() throws InternalException {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /**
     * Ensure tables are created
     */
    @Override
    public void load() throws InternalException {
        try (var conn = dataSource.getConnection()) {
            createTables(conn);

            if (checkVersion(conn) == 0) {
                insertVersion(conn);
            }
        } catch (SQLException ex) {
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
        try (var conn = dataSource.getConnection()) {
            return checkVersion(conn);
        } catch (Exception ex) {
            return -1;
        }
    }

    /**
     * Create or update a player
     *
     * @param profile player profile
     * @throws InternalException SQL Exception
     */
    @Override
    public void updatePlayerInfo(PlayerProfileLight profile) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.updatePlayerMySQL())) {
            ps.setString(1, profile.uuid().toString());
            ps.setString(2, profile.name());
            ps.setString(3, profile.customDisplay());
            ps.executeUpdate();
        } catch (SQLException ex) {
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
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.updateHeadMySQL())) {
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

    /**
     * Save a new head for the player
     *
     * @param pUUID player UUID
     * @param hUUID head UUID
     * @throws InternalException SQL Exception
     */
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

    /**
     * Reset the players heads
     *
     * @param pUUID player UUID
     * @throws InternalException SQL Exception
     */
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

    /**
     * Reset a specific head for a player
     *
     * @param pUUID player UUID
     * @param hUUID head UUID
     * @throws InternalException SQL Exception
     */
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

    /**
     * Remove a head
     *
     * @param hUUID      head UUID
     * @param withDelete should delete the head from the database
     * @throws InternalException SQL Exception
     */
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

    /**
     * Retrieve all players stored
     *
     * @return list of player UUID
     * @throws InternalException SQL Exception
     */
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

    /**
     * Retrieve top players with a limit
     *
     * @return map of player name with head count
     * @throws InternalException SQL Exception
     */
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

    /**
     * Check player name
     *
     * @param profile player profile
     * @return boolean if player has renamed
     * @throws InternalException SQL Exception
     */
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

    /**
     * Check if head exist
     *
     * @param hUUID head uuid
     * @return true if head exist otherwise false
     * @throws InternalException SQL Exception
     */
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

    /**
     * Migrate v1 to v2 database
     *
     * @throws InternalException SQL Exception
     */
    @Override
    public void migrate() throws InternalException {
        try (var conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Create of the archive table
                try (var ps = conn.prepareStatement(Requests.migArchiveTable())) {
                    ps.executeUpdate();
                }

                // Copy old data into the archive table
                try (var ps = conn.prepareStatement(Requests.migCopyOldToArchive())) {
                    ps.executeUpdate();
                }

                // Delete old table
                try (var ps = conn.prepareStatement(Requests.migDeleteOld())) {
                    ps.executeUpdate();
                }

                // Creation of new v2 tables
                createTables(conn);

                if (checkVersion(conn) == 0) {
                    insertVersion(conn);
                }

                // Import old users
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
                }

                // Import old heads
                try (var ps = conn.prepareStatement(Requests.migImportOldHeads())) {
                    ps.executeUpdate();
                }

                // Remap
                try (var ps = conn.prepareStatement(Requests.migRemap())) {
                    ps.executeUpdate();
                }

                // Delete archive table
                try (var ps = conn.prepareStatement(Requests.migDelArchive())) {
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

    public void insertVersion() throws InternalException {
        try (var conn = dataSource.getConnection()) {
            insertVersion(conn);
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void addColumnDisplayName() throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.addColumnPlayerDisplayNameMariaDb())) {
            ps.executeUpdate();
        } catch (Exception ex) {
            try (var conn = dataSource.getConnection()) {
                // MySQL doesn't support add column if not exists
                if (isColumnExist(conn, Requests.getTablePlayers(), "pDisplayName")) {
                    return;
                }

                try (var alterStmt = conn.createStatement()) {
                    alterStmt.executeUpdate(Requests.addColumnPlayerDisplayNameMySQL());
                }
            } catch (Exception exe) {
                throw new InternalException(ex);
            }
        }
    }

    @Override
    public ArrayList<UUID> getHeads() throws InternalException {
        var heads = new ArrayList<UUID>();

        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getHeadsMySQL())) {
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
    public void addColumnServerIdentifier() throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.addColumnServerIdentifierMariaDb())) {
            ps.executeUpdate();
        } catch (Exception ex) {
            try (var conn = dataSource.getConnection()) {
                // MySQL doesn't support add column if not exists
                if (isColumnExist(conn, Requests.getTableHeads(), "serverId")) {
                    return;
                }

                try (var alterStmt = conn.createStatement()) {
                    alterStmt.executeUpdate(Requests.addColumnServerIdentifierMySQL());
                }
            } catch (Exception exe) {
                throw new InternalException(ex);
            }
        }
    }

    /**
     * Add table version
     *
     * @throws InternalException SQL Exception
     */
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
    public ArrayList<AbstractMap.SimpleEntry<String, Boolean>> getTableHeads() throws InternalException {
        ArrayList<AbstractMap.SimpleEntry<String, Boolean>> heads = new ArrayList<>();

        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getTableHeadsData())) {
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

        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getTablePlayerHeadsData())) {
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

        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getTablePlayer())) {
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
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getTableHeadsColumnsMySQL())) {
            try (var rs = ps.executeQuery()) {
                int colCount = 0;
                if (rs.next()) {
                    colCount = rs.getInt("count");
                }

                try {
                    if (colCount == 3) {
                        try (var ps1 = conn.prepareStatement(Requests.addColumnHeadTextureMariaDb())) {
                            ps1.executeUpdate();
                        }
                    }
                } catch (Exception ex) {
                    // MySQL doesn't support add column if not exists
                    if (isColumnExist(conn, Requests.getTableHeads(), "hTexture")) {
                        return;
                    }

                    try (var alterStmt = conn.createStatement()) {
                        alterStmt.executeUpdate(Requests.addColumnHeadTextureMySQL());
                    }
                }
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
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
            }

            return "";
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
            }

            return null;
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public boolean isDefaultTablesExist() {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getIsTablePlayersExistMySQL())) {
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception ex) {
            return false;
        }
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

    // --- Head-Hunt linking (v5) ---

    @Override
    public void linkHeadToHunt(UUID headUUID, String huntId) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.linkHeadToHuntMySQL())) {
            ps.setString(1, headUUID.toString());
            ps.setString(2, huntId);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void unlinkHeadFromHunt(UUID headUUID, String huntId) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.unlinkHeadFromHunt())) {
            ps.setString(1, headUUID.toString());
            ps.setString(2, huntId);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void unlinkAllHeadsFromHunt(String huntId) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.unlinkAllHeadsFromHunt())) {
            ps.setString(1, huntId);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public ArrayList<String> getHuntsForHead(UUID headUUID) throws InternalException {
        var huntIds = new ArrayList<String>();

        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getHuntsForHead())) {
            ps.setString(1, headUUID.toString());
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    huntIds.add(rs.getString("huntId"));
                }
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }

        return huntIds;
    }

    @Override
    public ArrayList<UUID> getHeadsForHunt(String huntId) throws InternalException {
        var heads = new ArrayList<UUID>();

        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getHeadsForHunt())) {
            ps.setString(1, huntId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    heads.add(UUID.fromString(rs.getString("headUUID")));
                }
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }

        return heads;
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
    public int getPlayerCountForHeadInHunt(UUID hUUID, String huntId) throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getPlayerCountForHeadInHunt())) {
            ps.setString(1, hUUID.toString());
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

    // --- Migration v5 ---

    @Override
    public void addColumnHuntId() throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.addColumnHuntIdMariaDb())) {
            ps.executeUpdate();
        } catch (Exception ex) {
            try (var conn = dataSource.getConnection()) {
                if (isColumnExist(conn, Requests.getTablePlayerHeads(), "huntId")) {
                    return;
                }

                try (var alterStmt = conn.createStatement()) {
                    alterStmt.executeUpdate(Requests.addColumnHuntIdMySQL());
                }
            } catch (Exception exe) {
                throw new InternalException(ex);
            }
        }
    }

    @Override
    public void migrateToV5() throws InternalException {
        try (var conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Create hunt tables
                try (var ps = conn.prepareStatement(Requests.createTableHunts())) {
                    ps.execute();
                }
                try (var ps = conn.prepareStatement(Requests.createTableHeadHunts())) {
                    ps.execute();
                }

                // 2. Insert "default" hunt
                try (var ps = conn.prepareStatement(Requests.migV5InsertDefaultHunt())) {
                    ps.executeUpdate();
                }

                // 3. Link all existing heads to "default"
                try (var ps = conn.prepareStatement(Requests.migV5LinkAllHeadsToDefault())) {
                    ps.executeUpdate();
                }

                // 4. Add huntId column to hb_playerHeads
                addColumnHuntId();

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    // --- Internal helpers ---

    private void createTables(Connection conn) throws SQLException {
        try (var statement = conn.prepareStatement(Requests.createTablePlayersMySQL())) {
            statement.execute();
        }

        try (var statement = conn.prepareStatement(Requests.createTableHeadsMySQL())) {
            statement.execute();
        }

        try (var statement = conn.prepareStatement(Requests.createTablePlayerHeadsMySQL())) {
            statement.execute();
        }

        try (var statement = conn.prepareStatement(Requests.createTableVersion())) {
            statement.execute();
        }

        try (var statement = conn.prepareStatement(Requests.createTableHunts())) {
            statement.execute();
        }

        try (var statement = conn.prepareStatement(Requests.createTableHeadHunts())) {
            statement.execute();
        }
    }

    private int checkVersion(Connection conn) {
        try (var statement = conn.prepareStatement(Requests.getTableVersionData())) {
            try (var rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("current");
                } else
                    return 0;
            }
        } catch (Exception ex) {
            return -1;
        }
    }

    private void insertVersion(Connection conn) throws SQLException {
        try (var ps = conn.prepareStatement(Requests.createTableVersion())) {
            ps.execute();
        }

        try (var ps = conn.prepareStatement(Requests.insertVersion())) {
            ps.setInt(1, version);
            ps.executeUpdate();
        }
    }

    private boolean isColumnExist(Connection conn, String tableName, String columnName) throws Exception {
        try (var ps = conn.prepareStatement(Requests.isColumnExist())) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);

            try (var rs = ps.executeQuery()) {
                return !rs.next() || rs.getInt(1) != 0;
            }
        }
    }
}
