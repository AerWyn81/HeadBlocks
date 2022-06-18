package fr.aerwyn81.headblocks.databases.types;

import fr.aerwyn81.headblocks.databases.Database;
import fr.aerwyn81.headblocks.databases.Requests;
import fr.aerwyn81.headblocks.utils.InternalException;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
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
    @Override
    public void open() throws InternalException {
        if (connection != null)
            return;

        try {
            synchronized (this) {
                connection = DriverManager.getConnection("jdbc:sqlite:" + pathToDatabase);
                connection.prepareStatement("PRAGMA foreign_keys = ON;");
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
            PreparedStatement statement = connection.prepareStatement(Requests.CREATE_TABLE_PLAYERS);
            statement.execute();

            // Heads
            statement = connection.prepareStatement(Requests.CREATE_TABLE_HEADS);
            statement.execute();

            // Junction table (n-n)
            statement = connection.prepareStatement(Requests.CREATE_TABLE_PLAYERHEADS);
            statement.execute();
        } catch (Exception ex) {
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
        try (PreparedStatement ps = connection.prepareStatement(Requests.UPDATE_PLAYER)) {
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
     * @param limit int limit
     * @return list of player name with head count
     * @throws InternalException SQL Exception
     */
    @Override
    public Map<String, Integer> getTopPlayers(int limit) throws InternalException {
        Map<String, Integer> top = new LinkedHashMap<>();

        try (PreparedStatement ps = connection.prepareStatement(Requests.TOP_PLAYERS)) {
            ps.setInt(1, limit);

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
     * @return boolean if playername is equals
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
