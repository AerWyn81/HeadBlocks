package fr.aerwyn81.headblocks.databases.types;

import fr.aerwyn81.headblocks.databases.Database;
import fr.aerwyn81.headblocks.databases.Requests;
import fr.aerwyn81.headblocks.services.ConfigService;
import fr.aerwyn81.headblocks.services.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SQLiteMigrationTest {

    @TempDir
    Path dataFolder;

    private ConfigService configService;
    private final UUID player = UUID.randomUUID();
    private final UUID head = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        configService = mock(ConfigService.class);
        when(configService.databaseEnabled()).thenReturn(false);
        when(configService.redisEnabled()).thenReturn(false);
        when(configService.databasePrefix()).thenReturn("");
        when(configService.databaseName()).thenReturn(null);
        Requests.init(configService);
    }

    private void createLegacyDatabase(int version) throws Exception {
        boolean texture = version >= 2;
        boolean displayName = version >= 3;
        boolean serverId = version >= 4;

        var url = "jdbc:sqlite:" + dataFolder.resolve("headblocks.db");
        try (var conn = DriverManager.getConnection(url); var st = conn.createStatement()) {
            st.execute("CREATE TABLE hb_players (`pId` INTEGER PRIMARY KEY AUTOINCREMENT, `pUUID` VARCHAR(36) UNIQUE NOT NULL, `pName` VARCHAR(16) NOT NULL"
                    + (displayName ? ", `pDisplayName` VARCHAR(255) NULL" : "") + ")");
            st.execute("CREATE TABLE hb_heads (`hId` INTEGER PRIMARY KEY AUTOINCREMENT, `hUUID` VARCHAR(36) UNIQUE NOT NULL, `hExist` BOOLEAN NOT NULL CHECK (hExist IN (0, 1))"
                    + (texture ? ", `hTexture` VARCHAR(255)" : "") + (serverId ? ", `serverId` VARCHAR(8)" : "") + ")");
            st.execute("CREATE TABLE hb_playerHeads (`pUUID` VARCHAR(36), `hUUID` VARCHAR(36) REFERENCES hb_heads(hUUID) ON DELETE CASCADE, PRIMARY KEY(pUUID, hUUID))");
            if (version > 0) {
                st.execute("CREATE TABLE hb_version (`current` INTEGER)");
                st.execute("INSERT INTO hb_version VALUES (" + version + ")");
            }

            st.execute("INSERT INTO hb_players (pUUID, pName) VALUES ('" + player + "', 'Steve')");
            st.execute("INSERT INTO hb_heads (hUUID, hExist) VALUES ('" + head + "', 1)");
            st.execute("INSERT INTO hb_playerHeads (pUUID, hUUID) VALUES ('" + player + "', '" + head + "')");
        }
    }

    private SQLite openMigrated() throws Exception {
        var db = new SQLite(dataFolder.resolve("headblocks.db").toString());
        db.open();
        return db;
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4})
    void legacyDatabase_isMigratedToTheLatestVersion_keepingTheProgress(int version) throws Exception {
        createLegacyDatabase(version);

        var storage = new StorageService(configService, dataFolder.toFile());

        assertThat(storage.isStorageError()).isFalse();
        assertThat(storage.getHeadsPlayerForHunt(player, "default")).containsExactly(head);
        storage.close();

        var db = openMigrated();
        try {
            assertThat(db.checkVersion()).isEqualTo(Database.VERSION);
            assertThat(db.getHeads()).containsExactly(head);
        } finally {
            db.close();
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4})
    void legacyDatabase_acceptsSpawnedHeadsAfterTheMigration(int version) throws Exception {
        createLegacyDatabase(version);
        var storage = new StorageService(configService, dataFolder.toFile());
        var spawned = UUID.randomUUID();

        storage.createSpawnHead(spawned, "tex", 1);
        storage.addHeadForHunt(player, spawned, "default");

        assertThat(storage.getHeadsPlayerForHunt(player, "default")).containsExactlyInAnyOrder(head, spawned);
        assertThat(storage.getHeads()).containsExactly(head);
        storage.close();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4})
    void legacyDatabase_isBackedUpBeforeTheMigration(int version) throws Exception {
        createLegacyDatabase(version);

        new StorageService(configService, dataFolder.toFile()).close();

        try (var files = Files.list(dataFolder)) {
            assertThat(files.map(p -> p.getFileName().toString()))
                    .anyMatch(name -> name.startsWith("headblocks.db.save-"));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 6})
    void recentDatabase_migratingTwice_isHarmless(int restarts) throws Exception {
        createLegacyDatabase(4);

        for (int i = 0; i < restarts; i++) {
            var storage = new StorageService(configService, dataFolder.toFile());
            assertThat(storage.isStorageError()).isFalse();
            storage.close();
        }

        var storage = new StorageService(configService, dataFolder.toFile());
        assertThat(storage.getHeadsPlayerForHunt(player, "default")).containsExactly(head);
        storage.close();
    }

    @org.junit.jupiter.api.Test
    void freshDatabase_isCreatedAtTheLatestVersion() throws Exception {
        var storage = new StorageService(configService, dataFolder.toFile());
        assertThat(storage.isStorageError()).isFalse();
        storage.close();

        var db = openMigrated();
        try {
            assertThat(db.checkVersion()).isEqualTo(Database.VERSION);
        } finally {
            db.close();
        }
    }

    @org.junit.jupiter.api.Test
    void purgeOrphanSpawnHeads_keepsFoundHeads() throws Exception {
        var storage = new StorageService(configService, dataFolder.toFile());
        var found = UUID.randomUUID();
        var orphan = UUID.randomUUID();
        storage.updatePlayerName(new fr.aerwyn81.headblocks.data.PlayerProfileLight(player, "Steve", ""));
        storage.createSpawnHead(found, "", 1);
        storage.createSpawnHead(orphan, "", 1);
        storage.addHeadForHunt(player, found, "default");

        assertThat(storage.purgeOrphanSpawnHeads()).isEqualTo(1);
        assertThat(storage.isHeadExist(found)).isTrue();
        assertThat(storage.isHeadExist(orphan)).isFalse();
        storage.close();
    }
}
