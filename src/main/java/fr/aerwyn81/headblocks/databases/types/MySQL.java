package fr.aerwyn81.headblocks.databases.types;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.aerwyn81.headblocks.databases.Requests;
import fr.aerwyn81.headblocks.services.ConfigService;
import fr.aerwyn81.headblocks.utils.internal.InternalException;

import java.sql.Connection;
import java.sql.SQLException;

public final class MySQL extends AbstractDatabase {
    private final String user;
    private final String password;
    private final String hostname;
    private final int port;
    private final String databaseName;
    private final boolean isSsl;
    private final ConfigService configService;

    public MySQL(String user, String password, String hostname, int port, String databaseName, boolean isSsl, ConfigService configService) {
        this.user = user;
        this.password = password;
        this.hostname = hostname;
        this.port = port;
        this.databaseName = databaseName;
        this.isSsl = isSsl;
        this.configService = configService;
    }

    // --- Dialect-specific SQL ---

    @Override
    protected String getUpdatePlayerSql() {
        return Requests.updatePlayerMySQL();
    }

    @Override
    protected String getUpdateHeadSql() {
        return Requests.updateHeadMySQL();
    }

    @Override
    protected String getHeadsSql() {
        return Requests.getHeadsMySQL();
    }

    @Override
    protected String getTransferProgressSql() {
        return Requests.transferPlayerProgressMySQL();
    }

    @Override
    protected String getTableExistSql() {
        return Requests.getIsTablePlayersExistMySQL();
    }

    // --- Lifecycle ---

    @Override
    public void open() throws InternalException {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + hostname + ":" + port + "/" + databaseName);
            config.setUsername(user);
            config.setPassword(password);
            config.setMaximumPoolSize(configService.databaseMaxConnections());
            config.setMinimumIdle(configService.databaseMinIdleConnections());
            config.setConnectionTimeout(configService.databaseConnectionTimeout());
            config.setIdleTimeout(configService.databaseIdleTimeout());
            config.setMaxLifetime(configService.databaseMaxLifetime());
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

    @Override
    public void close() throws InternalException {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    // --- ALTER TABLE (MariaDB fallback + isColumnExist) ---

    @Override
    public void addColumnDisplayName() throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.addColumnPlayerDisplayNameMariaDb())) {
            ps.executeUpdate();
        } catch (Exception ex) {
            try (var conn = dataSource.getConnection()) {
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
    public void addColumnServerIdentifier() throws InternalException {
        try (var conn = dataSource.getConnection();
             var ps = conn.prepareStatement(Requests.addColumnServerIdentifierMariaDb())) {
            ps.executeUpdate();
        } catch (Exception ex) {
            try (var conn = dataSource.getConnection()) {
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

    // --- Migration v5 (MySQL: simple ALTER) ---

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

                addColumnHuntId();

                try (var ps = conn.prepareStatement(Requests.createTableTimedRunsMySQL())) {
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

        try (var statement = conn.prepareStatement(Requests.createTableTimedRunsMySQL())) {
            statement.execute();
        }
    }

    private boolean isColumnExist(Connection conn, String tableName, String columnName) throws Exception {
        try (var ps = conn.prepareStatement(Requests.isColumnExist())) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);

            try (var rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}
