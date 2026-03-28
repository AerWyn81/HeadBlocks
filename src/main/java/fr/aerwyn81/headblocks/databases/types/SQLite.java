package fr.aerwyn81.headblocks.databases.types;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.aerwyn81.headblocks.databases.Requests;
import fr.aerwyn81.headblocks.utils.internal.InternalException;

import java.sql.Connection;
import java.sql.SQLException;

public class SQLite extends AbstractDatabase {

    private final String pathToDatabase;

    public SQLite(String pathToDatabase) {
        this.pathToDatabase = pathToDatabase;
    }

    // --- Dialect-specific SQL ---

    @Override
    protected String getUpdatePlayerSql() {
        return Requests.updatePlayer();
    }

    @Override
    protected String getUpdateHeadSql() {
        return Requests.updateHead();
    }

    @Override
    protected String getHeadsSql() {
        return Requests.getHeads();
    }

    @Override
    protected String getTransferProgressSql() {
        return Requests.transferPlayerProgressSQLite();
    }

    @Override
    protected String getTableExistSql() {
        return Requests.getIsTablePlayersExistSQLite();
    }

    // --- Lifecycle ---

    @Override
    public void open() throws InternalException {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + pathToDatabase);
            config.setMaximumPoolSize(1);
            config.setMinimumIdle(1);
            config.setConnectionTimeout(5000);
            config.setIdleTimeout(0);
            config.setMaxLifetime(0);
            config.setPoolName("HeadBlocks-SQLite");
            config.setConnectionInitSql("PRAGMA foreign_keys = ON");
            dataSource = new HikariDataSource(config);
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void close() throws InternalException {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.getHikariPoolMXBean().softEvictConnections();
            dataSource.close();
        }
    }

    // --- ALTER TABLE (simple for SQLite) ---

    @Override
    public void addColumnDisplayName() throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.addColumnPlayerDisplayNameSQLite())) {
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void addColumnServerIdentifier() throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.addColumnServerIdentifierSQLite())) {
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void addColumnHeadTexture() throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.getTableHeadsColumnsSQLite())) {
            try (var rs = ps.executeQuery()) {
                int colCount = 0;
                if (rs.next()) {
                    colCount = rs.getInt("count");
                }

                if (colCount == 3) {
                    try (var ps1 = conn.prepareStatement(Requests.addColumnHeadTextureSQLite())) {
                        ps1.executeUpdate();
                    }
                }
            }
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    @Override
    public void addColumnHuntId() throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.addColumnHuntIdSQLite())) {
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new InternalException(ex);
        }
    }

    // --- Migration v5 (SQLite: temp table strategy) ---

    @Override
    public void migrateToV5() throws InternalException {
        try (var conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (var ps = conn.prepareStatement(Requests.createTableHunts())) {
                    ps.execute();
                }
                try (var ps = conn.prepareStatement(Requests.migV5InsertDefaultHunt())) {
                    ps.executeUpdate();
                }

                // SQLite doesn't support ALTER TABLE to change PK,
                // so we create temp → copy → drop → rename
                try (var ps = conn.prepareStatement(Requests.migV5CreateTempPlayerHeadsSQLite())) {
                    ps.execute();
                }
                try (var ps = conn.prepareStatement(Requests.migV5CopyPlayerHeadsToTempSQLite())) {
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement(Requests.migV5DropOldPlayerHeadsSQLite())) {
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement(Requests.migV5RenameTempPlayerHeadsSQLite())) {
                    ps.executeUpdate();
                }

                try (var ps = conn.prepareStatement(Requests.createTableTimedRuns())) {
                    ps.execute();
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

    // --- Internal helpers ---

    @Override
    protected void createTables(Connection conn) throws SQLException {
        try (var statement = conn.prepareStatement(Requests.createTablePlayers())) {
            statement.execute();
        }

        try (var statement = conn.prepareStatement(Requests.createTableHeads())) {
            statement.execute();
        }

        try (var statement = conn.prepareStatement(Requests.createTablePlayerHeads())) {
            statement.execute();
        }

        try (var statement = conn.prepareStatement(Requests.createTableVersion())) {
            statement.execute();
        }

        try (var statement = conn.prepareStatement(Requests.createTableHunts())) {
            statement.execute();
        }

        try (var statement = conn.prepareStatement(Requests.createTableTimedRuns())) {
            statement.execute();
        }
    }
}
