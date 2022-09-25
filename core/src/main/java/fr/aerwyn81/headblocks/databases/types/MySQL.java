package fr.aerwyn81.headblocks.databases.types;

import fr.aerwyn81.headblocks.databases.Database;
import fr.aerwyn81.headblocks.databases.Requests;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;

import java.sql.*;
import java.util.*;

@SuppressWarnings("DuplicatedCode")
public final class MySQL implements Database {
    private final String user;
    private final String password;
    private final String hostname;
    private final int port;
    private final String databaseName;
    private final boolean isSsl;

    private Connection connection;

    public MySQL(String user, String password, String hostname, int port, String databaseName, boolean isSsl) {
        this.user = user;
        this.password = password;
        this.hostname = hostname;
        this.port = port;
        this.databaseName = databaseName;
        this.isSsl = isSsl;
    }

    /**
     * Open the MySQL connection
     */
    @Override
    public void open() throws InternalException {
        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", password);
        if (!isSsl) {
            properties.setProperty("verifyServerCertificate", "false");
            properties.setProperty("useSSL", "false");
        }

        try {
            connection = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + databaseName, properties);
        } catch (SQLException ex) {
            throw new InternalException(ex);
        }
    }

    /**
     * Close the MySQL connection
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
     */
    @Override
    public void load() throws InternalException {
        PreparedStatement statement;
        try {
            statement = connection.prepareStatement(Requests.CREATE_TABLE_PLAYERS_MYSQL);
            statement.execute();

            statement = connection.prepareStatement(Requests.CREATE_TABLE_HEADS_MYSQL);
            statement.execute();

            statement = connection.prepareStatement(Requests.CREATE_TABLE_PLAYERHEADS_MYSQL);
            statement.execute();

            statement = connection.prepareStatement(Requests.CREATE_TABLE_VERSION);
            statement.execute();
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
        PreparedStatement statement;

        try {
            statement = connection.prepareStatement(Requests.CONTAINS_TABLE_HEADS);
            statement.executeQuery();
        } catch (Exception ex) {
            return -1;
        }

        try {
            statement = connection.prepareStatement(Requests.GET_TABLE_VERSION);
            ResultSet rs = statement.executeQuery();
            return rs.getInt("current");
        } catch (Exception ex) {
            return 0;
        }
    }

    /**
     * Create or update a player
     *
     * @param pUUID player UUID
     * @param pName player name
     * @throws InternalException SQL Exception
     */
    @Override
    public void updatePlayerInfo(UUID pUUID, String pName) throws InternalException {
        try (PreparedStatement ps = connection.prepareStatement(Requests.UPDATE_PLAYER_MYSQL)) {
            ps.setString(1, pUUID.toString());
            ps.setString(2, pName);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    /**
     * Create a head
     *
     * @param hUUID head UUID
     * @param texture head texture
     * @throws InternalException SQL Exception
     */
    @Override
    public void createNewHead(UUID hUUID, String texture) throws InternalException {
        try (PreparedStatement ps = connection.prepareStatement(Requests.CREATE_HEAD)) {
            ps.setString(1, hUUID.toString());
            ps.setString(2, texture);
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
        try (PreparedStatement ps = connection.prepareStatement(Requests.CONTAINS_PLAYER)) {
            ps.setString(1, pUUID.toString());
            ResultSet rs = ps.executeQuery();

            return rs.next();
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

        try (PreparedStatement ps = connection.prepareStatement(Requests.PLAYER_HEADS)) {
            ps.setString(1, pUUID.toString());

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                heads.add(UUID.fromString(rs.getString("hUUID")));
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
        try (PreparedStatement ps = connection.prepareStatement(Requests.SAVE_PLAYERHEAD)) {
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
        try (PreparedStatement ps = connection.prepareStatement(Requests.RESET_PLAYER)) {
            ps.setString(1, pUUID.toString());
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    /**
     * Remove a head
     *
     * @param hUUID head UUID
     * @param withDelete should delete the head from the database
     * @throws InternalException SQL Exception
     */
    @Override
    public void removeHead(UUID hUUID, boolean withDelete) throws InternalException {
        try (PreparedStatement ps = connection.prepareStatement(withDelete ? Requests.DELETE_HEAD : Requests.REMOVE_HEAD)) {
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

        try (PreparedStatement ps = connection.prepareStatement(Requests.ALL_PLAYERS)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                players.add(UUID.fromString(rs.getString("pUUID")));
            }

            return players;
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    /**
     * Retrieve top players with a limit
     *
     * @return list of player name with head count
     * @throws InternalException SQL Exception
     */
    @Override
    public Map<String, Integer> getTopPlayers() throws InternalException {
        Map<String, Integer> top = new LinkedHashMap<>();

        try (PreparedStatement ps = connection.prepareStatement(Requests.TOP_PLAYERS)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                top.put(rs.getString("pName"), rs.getInt("hCount"));
            }

            return top;
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    /**
     * Check player name
     * @param pUUID player UUID
     * @param pName player name
     * @return boolean if player has renamed
     * @throws InternalException SQL Exception
     */
    @Override
    public boolean hasPlayerRenamed(UUID pUUID, String pName) throws InternalException {
        try (PreparedStatement ps = connection.prepareStatement(Requests.CHECK_PLAYER_NAME)) {
            ps.setString(1, pUUID.toString());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return !pName.equals(rs.getString("pName"));
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }

        return true;
    }

    /**
     * Check if head exist
     * @param hUUID head uuid
     * @return true if head exist otherwise false
     * @throws InternalException SQL Exception
     */
    @Override
    public boolean isHeadExist(UUID hUUID) throws InternalException {
        try (PreparedStatement ps = connection.prepareStatement(Requests.HEAD_EXIST)) {
            ps.setString(1, hUUID.toString());
            ResultSet rs = ps.executeQuery();

            return rs.next();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    /**
     * Migrate v1 to v2 database
     * @throws InternalException SQL Exception
     */
    @Override
    public void migrate() throws InternalException {
        try {
            // Create of the archive table
            PreparedStatement ps = connection.prepareStatement(Requests.MIG_ARCHIVE_TABLE);
            ps.executeUpdate();

            // Copy old data into the archive table
            ps = connection.prepareStatement(Requests.MIG_COPY_OLD_TO_ARCHIVE);
            ps.executeUpdate();

            // Delete old table
            ps = connection.prepareStatement(Requests.MIG_DELETE_OLD);
            ps.executeUpdate();

            // Creation of new v2 tables
            load();

            // Import old users
            ps = connection.prepareStatement(Requests.MIG_IMPORT_OLD_USERS);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String pUUID = rs.getString("pUUID");
                String pName = PlayerUtils.getPseudoFromSession(pUUID);

                ps = connection.prepareStatement(Requests.MIG_INSERT_PLAYER);
                ps.setString(1, pUUID);
                ps.setString(2, pName);
                ps.executeUpdate();
            }

            // Import old heads
            ps = connection.prepareStatement(Requests.MIG_IMPORT_OLD_HEADS);
            ps.executeUpdate();

            // Remap
            ps = connection.prepareStatement(Requests.MIG_REMAP);
            ps.executeUpdate();

            // Delete archive table
            ps = connection.prepareStatement(Requests.MIG_DEL_ARCHIVE);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    /**
     * Add table version
     * @throws InternalException SQL Exception
     */
    @Override
    public void upsertTableVersion() throws InternalException {
        try {
            PreparedStatement ps = connection.prepareStatement(Requests.CREATE_TABLE_VERSION);
            ps.execute();

            ps = connection.prepareStatement(Requests.INSERT_VERSION);
            ps.setInt(1, version);
            ps.setInt(2, version - 1);
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public ArrayList<AbstractMap.SimpleEntry<String, Boolean>> getTableHeads() throws InternalException {
        ArrayList<AbstractMap.SimpleEntry<String, Boolean>> heads = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(Requests.GET_TABLE_HEADS)) {
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                heads.add(new AbstractMap.SimpleEntry<>(rs.getString("hUUID"), rs.getBoolean("hExist")));
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }

        return heads;
    }

    @Override
    public ArrayList<AbstractMap.SimpleEntry<String, String>> getTablePlayerHeads() throws InternalException {
        ArrayList<AbstractMap.SimpleEntry<String, String>> playerHeads = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(Requests.GET_TABLE_PLAYERHEADS)) {
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                playerHeads.add(new AbstractMap.SimpleEntry<>(rs.getString("pUUID"), rs.getString("hUUID")));
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }

        return playerHeads;
    }

    @Override
    public ArrayList<AbstractMap.SimpleEntry<String, String>> getTablePlayers() throws InternalException {
        ArrayList<AbstractMap.SimpleEntry<String, String>> playerHeads = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(Requests.GET_TABLE_PLAYER)) {
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                playerHeads.add(new AbstractMap.SimpleEntry<>(rs.getString("pUUID"), rs.getString("pName")));
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }

        return playerHeads;
    }

    @Override
    public void addColumnHeadTexture() throws InternalException {
        try (PreparedStatement ps = connection.prepareStatement(Requests.ADD_COLUMN_HEAD_TEXTURE)) {
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public String getHeadTexture(UUID headUuid) throws InternalException {
        try (PreparedStatement ps = connection.prepareStatement(Requests.GET_HEAD_TEXTURE)) {
            ps.setString(1, headUuid.toString());

            ResultSet rs  = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("hTexture");
            }

            return "";
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public ArrayList<UUID> getPlayers(UUID headUuid) throws InternalException {
        var players = new ArrayList<UUID>();

        try (PreparedStatement ps = connection.prepareStatement(Requests.GET_PLAYERS_BY_HEAD)) {
            ps.setString(1, headUuid.toString());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                players.add(UUID.fromString(rs.getString("pUUID")));
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }

        return players;
    }
}