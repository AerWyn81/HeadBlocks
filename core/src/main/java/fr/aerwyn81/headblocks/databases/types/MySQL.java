package fr.aerwyn81.headblocks.databases.types;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.databases.Database;
import fr.aerwyn81.headblocks.handlers.ConfigHandler;
import fr.aerwyn81.headblocks.utils.FormatUtils;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MySQL implements Database {

    private final HeadBlocks main;
    private final ConfigHandler configHandler;

    private HikariDataSource hikari;

    public MySQL(HeadBlocks main) {
        this.main = main;
        this.configHandler = main.getConfigHandler();
    }

    @Override
    public void close() {
        if (hikari == null) {
            return;
        }

        hikari.close();
    }

    @Override
    public void open() {
        if (hikari != null) {
            return;
        }

        try {
            HikariConfig config = new HikariConfig();
            config.setPoolName("HeadBlocksPoolMySQL");
            config.setJdbcUrl("jdbc:mysql://" + configHandler.getDatabaseHostname() + ":" + configHandler.getDatabasePort() + "/" + configHandler.getDatabaseName());
            config.setUsername(configHandler.getDatabaseUsername());
            config.setPassword(configHandler.getDatabasePassword());
            config.setMaximumPoolSize(75);
            config.setMinimumIdle(4);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hikari = new HikariDataSource(config);

            HeadBlocks.log.sendMessage(FormatUtils.translate("&aMySQL has been enabled!"));
        } catch (HikariPool.PoolInitializationException e) {
            HeadBlocks.log.sendMessage(FormatUtils.translate("&cMySQL has an error on the connection! Now trying with SQLite..."));
            main.getStorageHandler().changeToSQLite();
        }
    }

    @Override
    public void load() {
        PreparedStatement statement = null;
        Connection connection = null;
        try {
            connection = hikari.getConnection();
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
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean hasHead(UUID playerUuid, UUID headUuid) {
        try (Connection connection = hikari.getConnection(); PreparedStatement ps = connection.prepareStatement("SELECT * FROM hb_players WHERE pUUID = '" + playerUuid.toString() + "' AND hUUID = '" + headUuid.toString() + "';"); ResultSet rs = ps.executeQuery()) {
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
        try (Connection connection = hikari.getConnection(); PreparedStatement ps = connection.prepareStatement("SELECT * FROM hb_players WHERE pUUID = '" + playerUuid.toString() + "';"); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public List<UUID> getHeadsPlayer(UUID playerUuid) {
        List<UUID> heads = new ArrayList<>();

        try (Connection connection = hikari.getConnection(); PreparedStatement ps = connection.prepareStatement("SELECT * FROM hb_players WHERE pUUID = '" + playerUuid.toString() + "';"); ResultSet rs = ps.executeQuery()) {
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
        try (Connection connection = hikari.getConnection(); PreparedStatement ps = connection.prepareStatement("REPLACE INTO hb_players (pUUID, hUUID) VALUES(?,?)")) {
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
                Connection connection = hikari.getConnection();
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
                Connection connection = hikari.getConnection();
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
    public List<UUID> getAllPlayers() {
        List<UUID> players = new ArrayList<>();

        try (Connection connection = hikari.getConnection(); PreparedStatement ps = connection.prepareStatement("SELECT DISTINCT pUUID FROM hb_players"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                players.add(UUID.fromString(rs.getString("pUUID")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return players;
    }
}
