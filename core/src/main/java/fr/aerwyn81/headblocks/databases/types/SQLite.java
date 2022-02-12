package fr.aerwyn81.headblocks.databases.types;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.databases.Database;
import fr.aerwyn81.headblocks.utils.FormatUtils;
import org.bukkit.Bukkit;
import org.javatuples.Pair;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

public class SQLite implements Database {

    private final HeadBlocks main;

    private Connection connection;

    public SQLite(HeadBlocks main) {
        this.main = main;
    }

    @Override
    public void close() {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException t) {
            t.printStackTrace();
        }
    }

    @Override
    public void open() {
        if (connection != null) {
            return;
        }

        File file = new File(main.getDataFolder(), "headblocks.db");
        String URL = "jdbc:sqlite:" + file;

        synchronized (this) {
            try {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection(URL);
            } catch (SQLException | ClassNotFoundException e) {
                HeadBlocks.log.sendMessage(FormatUtils.translate("&cCannot load SQLite database, plugin require it : " + e.getMessage()));
                Bukkit.getPluginManager().disablePlugin(main);
            }
        }
    }

    @Override
    public void load() {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS hb_players (`pUUID` varchar(40) NOT NULL, `hUUID` varchar(40) NOT NULL, PRIMARY KEY (pUUID,`hUUID`));");
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean hasHead(UUID playerUuid, UUID headUuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM hb_players WHERE pUUID = '" + playerUuid.toString() + "' AND hUUID = '" + headUuid.toString() + "';"); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean containsPlayer(UUID playerUuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM hb_players WHERE pUUID = '" + playerUuid.toString() + "';"); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public ArrayList<UUID> getHeadsPlayer(UUID playerUuid) {
        ArrayList<UUID> heads = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM hb_players WHERE pUUID = '" + playerUuid.toString() + "';"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                heads.add(UUID.fromString(rs.getString("hUUID")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return heads;
    }

    @Override
    public void savePlayer(UUID playerUuid, UUID headUuid) {
        try (PreparedStatement ps = connection.prepareStatement("REPLACE INTO hb_players (pUUID, hUUID) VALUES(?,?)")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, headUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void resetPlayer(UUID playerUuid) {
        Bukkit.getScheduler().runTaskAsynchronously(main, () -> {
            PreparedStatement ps = null;
            try {
                ps = connection.prepareStatement("DELETE FROM hb_players WHERE pUUID = '" + playerUuid.toString() + "'");
                ps.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void removeHead(UUID headUuid) {
        Bukkit.getScheduler().runTaskAsynchronously(main, () -> {
            PreparedStatement ps = null;
            try {
                ps = connection.prepareStatement("DELETE FROM hb_players WHERE hUUID = '" + headUuid.toString() + "'");
                ps.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public ArrayList<UUID> getAllPlayers() {
        ArrayList<UUID> players = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement("SELECT DISTINCT pUUID FROM hb_players"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                players.add(UUID.fromString(rs.getString("pUUID")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return players;
    }

    @Override
    public ArrayList<Pair<UUID, Integer>> getTopPlayers(int limit) {
        ArrayList<Pair<UUID, Integer>> top = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement("SELECT `pUUID`, COUNT(*) as hCount FROM hb_players GROUP BY `pUUID` ORDER BY hCount DESC LIMIT '" + limit + "'"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                top.add(new Pair<>(UUID.fromString(rs.getString("pUUID")), rs.getInt("hCount")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return top;
    }
}
