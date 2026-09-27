package fr.aerwyn81.headblocks.databases.types;

import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.databases.Requests;
import fr.aerwyn81.headblocks.services.ConfigService;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseErrorPropagationTest {

    @FunctionalInterface
    interface DatabaseCall {
        void run(SQLite db) throws Exception;
    }

    @TempDir
    Path tempDir;

    private SQLite db;

    @BeforeEach
    void setUp() throws InternalException {
        ConfigService configService = mock(ConfigService.class);
        when(configService.databaseEnabled()).thenReturn(false);
        when(configService.databasePrefix()).thenReturn("");
        Requests.init(configService);

        db = new SQLite(tempDir.resolve("closed.db").toString());
        db.open();
        db.load();
        db.close();
    }

    @AfterEach
    void tearDown() throws InternalException {
        db.close();
    }

    private static final UUID PLAYER = UUID.randomUUID();
    private static final UUID HEAD = UUID.randomUUID();
    private static final PlayerProfileLight PROFILE = new PlayerProfileLight(PLAYER, "Steve", "");

    static Stream<Arguments> calls() {
        return Stream.of(
                call("load", SQLite::load),
                call("insertVersion", SQLite::insertVersion),
                call("updatePlayerInfo", d -> d.updatePlayerInfo(PROFILE)),
                call("createNewHead", d -> d.createNewHead(HEAD, "t", "s")),
                call("createSpawnHead", d -> d.createSpawnHead(HEAD, "t", 1, "s")),
                call("deleteOrphanSpawnHeads", d -> d.deleteOrphanSpawnHeads("s")),
                call("isHeadExist", d -> d.isHeadExist(HEAD)),
                call("containsPlayer", d -> d.containsPlayer(PLAYER)),
                call("getHeadsPlayer", d -> d.getHeadsPlayer(PLAYER)),
                call("addHead", d -> d.addHead(PLAYER, HEAD)),
                call("resetPlayer", d -> d.resetPlayer(PLAYER)),
                call("resetPlayerHead", d -> d.resetPlayerHead(PLAYER, HEAD)),
                call("removeHead", d -> d.removeHead(HEAD, false)),
                call("deleteHead", d -> d.removeHead(HEAD, true)),
                call("getAllPlayers", SQLite::getAllPlayers),
                call("getTopPlayers", SQLite::getTopPlayers),
                call("hasPlayerRenamed", d -> d.hasPlayerRenamed(PROFILE)),
                call("getHeads", SQLite::getHeads),
                call("getHeadsByServer", d -> d.getHeads("s")),
                call("getHeadTexture", d -> d.getHeadTexture(HEAD)),
                call("getPlayers", d -> d.getPlayers(HEAD)),
                call("getPlayerByName", d -> d.getPlayerByName("Steve")),
                call("upsertTableVersion", d -> d.upsertTableVersion(1)),
                call("getTableHeads", SQLite::getTableHeads),
                call("getTablePlayerHeads", SQLite::getTablePlayerHeads),
                call("getTablePlayers", SQLite::getTablePlayers),
                call("getDistinctServerIds", SQLite::getDistinctServerIds),
                call("migrate", SQLite::migrate),
                call("createHunt", d -> d.createHunt("h", "H", "ACTIVE")),
                call("updateHuntState", d -> d.updateHuntState("h", "ACTIVE")),
                call("updateHuntName", d -> d.updateHuntName("h", "H")),
                call("deleteHunt", d -> d.deleteHunt("h")),
                call("getHunts", SQLite::getHunts),
                call("getHuntById", d -> d.getHuntById("h")),
                call("addHeadForHunt", d -> d.addHeadForHunt(PLAYER, HEAD, "h")),
                call("getHeadsPlayerForHunt", d -> d.getHeadsPlayerForHunt(PLAYER, "h")),
                call("resetPlayerHunt", d -> d.resetPlayerHunt(PLAYER, "h")),
                call("resetPlayerHeadHunt", d -> d.resetPlayerHeadHunt(PLAYER, HEAD, "h")),
                call("getTopPlayersForHunt", d -> d.getTopPlayersForHunt("h")),
                call("transferPlayerProgress", d -> d.transferPlayerProgress("a", "b")),
                call("deletePlayerProgressForHunt", d -> d.deletePlayerProgressForHunt("h")),
                call("saveTimedRun", d -> d.saveTimedRun(PLAYER, "h", 10L)),
                call("getTimedLeaderboard", d -> d.getTimedLeaderboard("h", 10)),
                call("getBestTime", d -> d.getBestTime(PLAYER, "h")),
                call("getTimedRunCount", d -> d.getTimedRunCount(PLAYER, "h")),
                call("addColumnDisplayName", SQLite::addColumnDisplayName),
                call("addColumnServerIdentifier", SQLite::addColumnServerIdentifier),
                call("addColumnHeadTexture", SQLite::addColumnHeadTexture),
                call("addColumnHuntId", SQLite::addColumnHuntId),
                call("addColumnHeadSpawn", SQLite::addColumnHeadSpawn),
                call("migrateToV5", SQLite::migrateToV5)
        );
    }

    private static Arguments call(String name, DatabaseCall call) {
        return Arguments.of(name, call);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("calls")
    void closedDatabase_isReportedAsAnInternalException(String name, DatabaseCall call) {
        assertThatThrownBy(() -> call.run(db)).isInstanceOf(InternalException.class);
    }

    @Test
    void closedDatabase_versionIsUnknown() {
        assertThat(db.checkVersion()).isEqualTo(-1);
    }

    @Test
    void closedDatabase_hasNoTables() {
        assertThat(db.isDefaultTablesExist()).isFalse();
    }
}
