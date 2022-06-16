package fr.aerwyn81.headblocks.databases.types;

import fr.aerwyn81.headblocks.databases.Database;
import fr.aerwyn81.headblocks.databases.Requests;
import fr.aerwyn81.headblocks.utils.InternalException;
import org.javatuples.Pair;

import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.UUID;

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

            statement = connection.prepareStatement(Requests.CREATE_TABLE_PLAYERHEADS);
            statement.execute();
        } catch (SQLException ex) {
            throw new InternalException(ex);
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
     * @throws InternalException SQL Exception
     */
    @Override
    public void createNewHead(UUID hUUID) throws InternalException {
        try (PreparedStatement ps = connection.prepareStatement(Requests.CREATE_HEAD)) {
            ps.setString(1, hUUID.toString());
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    /**
     * Check that a head has been found for a player uuid
     *
     * @param pUUID player UUID
     * @param hUUID head UUID
     * @return true if head exist
     * @throws InternalException SQL Exception
     */
    @Override
    public boolean hasHead(UUID pUUID, UUID hUUID) throws InternalException {
        try (PreparedStatement ps = connection.prepareStatement(Requests.HAS_HEAD)) {
            ps.setString(1, pUUID.toString());
            ps.setString(2, hUUID.toString());
            ResultSet rs = ps.executeQuery();

            return rs.next();
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
    public void savePlayer(UUID pUUID, UUID hUUID) throws InternalException {
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
     * @param limit int limit
     * @return list of player name with head count
     * @throws InternalException SQL Exception
     */
    @Override
    public ArrayList<Pair<String, Integer>> getTopPlayers(int limit) throws InternalException {
        ArrayList<Pair<String, Integer>> top = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(Requests.TOP_PLAYERS)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                top.add(new Pair<>(rs.getString("pUUID"), rs.getInt("hCount")));
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
}