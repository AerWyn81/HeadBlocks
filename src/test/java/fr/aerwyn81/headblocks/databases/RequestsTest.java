package fr.aerwyn81.headblocks.databases;

import fr.aerwyn81.headblocks.services.ConfigService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestsTest {

    private void initWithPrefix(String prefix) {
        ConfigService configService = mock(ConfigService.class);
        if (prefix.isEmpty()) {
            when(configService.databaseEnabled()).thenReturn(false);
        } else {
            when(configService.databaseEnabled()).thenReturn(true);
            when(configService.databasePrefix()).thenReturn(prefix);
        }
        when(configService.databaseName()).thenReturn(null);
        Requests.init(configService);
    }

    private void initWithPrefixAndDbName(String prefix, String dbName) {
        ConfigService configService = mock(ConfigService.class);
        if (prefix.isEmpty()) {
            when(configService.databaseEnabled()).thenReturn(false);
        } else {
            when(configService.databaseEnabled()).thenReturn(true);
            when(configService.databasePrefix()).thenReturn(prefix);
        }
        when(configService.databaseName()).thenReturn(dbName);
        Requests.init(configService);
    }

    // =========================================================================
    // 1. Table names without prefix
    // =========================================================================

    @Nested
    class TableNamesWithoutPrefix {

        @Test
        void table_names_without_prefix_match_expected() {
            initWithPrefix("");

            assertThat(Requests.getTablePlayers()).isEqualTo("hb_players");
            assertThat(Requests.getTableHeads()).isEqualTo("hb_heads");
            assertThat(Requests.getTablePlayerHeads()).isEqualTo("hb_playerHeads");
            assertThat(Requests.getTableVersion()).isEqualTo("hb_version");
            assertThat(Requests.getTableHunts()).isEqualTo("hb_hunts");
            assertThat(Requests.getTableHeadHunts()).isEqualTo("hb_head_hunts");
            assertThat(Requests.getTableTimedRuns()).isEqualTo("hb_timed_runs");
        }
    }

    // =========================================================================
    // 2. Table names with prefix
    // =========================================================================

    @Nested
    class TableNamesWithPrefix {

        @Test
        void table_names_with_prefix_prepend_prefix() {
            initWithPrefix("test_");

            assertThat(Requests.getTablePlayers()).isEqualTo("test_hb_players");
            assertThat(Requests.getTableHeads()).isEqualTo("test_hb_heads");
            assertThat(Requests.getTablePlayerHeads()).isEqualTo("test_hb_playerHeads");
            assertThat(Requests.getTableVersion()).isEqualTo("test_hb_version");
            assertThat(Requests.getTableHunts()).isEqualTo("test_hb_hunts");
            assertThat(Requests.getTableHeadHunts()).isEqualTo("test_hb_head_hunts");
            assertThat(Requests.getTableTimedRuns()).isEqualTo("test_hb_timed_runs");
        }
    }

    // =========================================================================
    // 3. SQL creation: players
    // =========================================================================

    @Nested
    class CreateTablePlayers {

        @Test
        void createTablePlayers_contains_primary_key() {
            initWithPrefix("");

            String sql = Requests.createTablePlayers();

            assertThat(sql).contains("PRIMARY KEY");
            assertThat(sql).contains("hb_players");
            assertThat(sql).contains("pUUID");
            assertThat(sql).contains("pName");
        }

        @Test
        void createTablePlayers_uses_autoincrement() {
            initWithPrefix("");

            assertThat(Requests.createTablePlayers()).contains("AUTOINCREMENT");
        }

        @Test
        void createTablePlayersMySQL_uses_auto_increment() {
            initWithPrefix("");

            String sql = Requests.createTablePlayersMySQL();

            assertThat(sql).contains("AUTO_INCREMENT");
            assertThat(sql).contains("hb_players");
            assertThat(sql).contains("pUUID");
            assertThat(sql).contains("pName");
            assertThat(sql).contains("pDisplayName");
        }

        @Test
        void createTablePlayersMySQL_with_prefix() {
            initWithPrefix("srv_");

            assertThat(Requests.createTablePlayersMySQL()).contains("srv_hb_players");
        }

        @Test
        void createTablePlayers_contains_pDisplayName_column() {
            initWithPrefix("");

            assertThat(Requests.createTablePlayers()).contains("pDisplayName");
        }
    }

    // =========================================================================
    // 4. SQL creation: heads
    // =========================================================================

    @Nested
    class CreateTableHeads {

        @Test
        void createTableHeads_contains_unique_constraint() {
            initWithPrefix("");

            String sql = Requests.createTableHeads();

            assertThat(sql).contains("UNIQUE");
            assertThat(sql).contains("hb_heads");
            assertThat(sql).contains("hUUID");
        }

        @Test
        void createTableHeads_contains_hExist_check() {
            initWithPrefix("");

            assertThat(Requests.createTableHeads()).contains("CHECK (hExist IN (0, 1))");
        }

        @Test
        void createTableHeads_contains_hTexture_column() {
            initWithPrefix("");

            assertThat(Requests.createTableHeads()).contains("hTexture");
        }

        @Test
        void createTableHeads_contains_serverId_column() {
            initWithPrefix("");

            assertThat(Requests.createTableHeads()).contains("serverId");
        }

        @Test
        void createTableHeadsMySQL_uses_auto_increment() {
            initWithPrefix("");

            String sql = Requests.createTableHeadsMySQL();

            assertThat(sql).contains("AUTO_INCREMENT");
            assertThat(sql).contains("hb_heads");
        }

        @Test
        void createTableHeadsMySQL_with_prefix() {
            initWithPrefix("s1_");

            assertThat(Requests.createTableHeadsMySQL()).contains("s1_hb_heads");
        }
    }

    // =========================================================================
    // 5. SQL creation: playerHeads
    // =========================================================================

    @Nested
    class CreateTablePlayerHeads {

        @Test
        void createTablePlayerHeads_contains_composite_primary_key() {
            initWithPrefix("");

            String sql = Requests.createTablePlayerHeads();

            assertThat(sql).contains("PRIMARY KEY(pUUID, hUUID, huntId)");
            assertThat(sql).contains("hb_playerHeads");
        }

        @Test
        void createTablePlayerHeads_contains_foreign_key_reference() {
            initWithPrefix("");

            assertThat(Requests.createTablePlayerHeads()).contains("REFERENCES");
            assertThat(Requests.createTablePlayerHeads()).contains("ON DELETE CASCADE");
        }

        @Test
        void createTablePlayerHeads_contains_huntId_with_default() {
            initWithPrefix("");

            assertThat(Requests.createTablePlayerHeads()).contains("huntId");
            assertThat(Requests.createTablePlayerHeads()).contains("DEFAULT 'default'");
        }

        @Test
        void createTablePlayerHeadsMySQL_contains_foreign_key() {
            initWithPrefix("");

            String sql = Requests.createTablePlayerHeadsMySQL();

            assertThat(sql).contains("FOREIGN KEY");
            assertThat(sql).contains("ON DELETE CASCADE");
            assertThat(sql).contains("hb_playerHeads");
        }

        @Test
        void createTablePlayerHeadsMySQL_with_prefix() {
            initWithPrefix("p_");

            assertThat(Requests.createTablePlayerHeadsMySQL()).contains("p_hb_playerHeads");
            assertThat(Requests.createTablePlayerHeadsMySQL()).contains("p_hb_heads");
        }
    }

    // =========================================================================
    // 6. SQL creation: hunts
    // =========================================================================

    @Nested
    class CreateTableHunts {

        @Test
        void createTableHunts_contains_primary_key() {
            initWithPrefix("");

            String sql = Requests.createTableHunts();

            assertThat(sql).contains("PRIMARY KEY");
            assertThat(sql).contains("hb_hunts");
            assertThat(sql).contains("hId");
        }

        @Test
        void createTableHunts_contains_hName_and_hState() {
            initWithPrefix("");

            String sql = Requests.createTableHunts();

            assertThat(sql).contains("hName");
            assertThat(sql).contains("hState");
            assertThat(sql).contains("DEFAULT 'ACTIVE'");
        }
    }

    // =========================================================================
    // 7. SQL creation: head hunts
    // =========================================================================

    @Nested
    class CreateTableHeadHunts {

        @Test
        void createTableHeadHunts_contains_composite_primary_key() {
            initWithPrefix("");

            String sql = Requests.createTableHeadHunts();

            assertThat(sql).contains("PRIMARY KEY");
            assertThat(sql).contains("headUUID");
            assertThat(sql).contains("huntId");
            assertThat(sql).contains("hb_head_hunts");
        }
    }

    // =========================================================================
    // 8. SQL creation: version
    // =========================================================================

    @Nested
    class CreateTableVersion {

        @Test
        void createTableVersion_contains_current_column() {
            initWithPrefix("");

            String sql = Requests.createTableVersion();

            assertThat(sql).contains("hb_version");
            assertThat(sql).contains("current");
            assertThat(sql).contains("INTEGER");
        }

        @Test
        void createTableVersion_with_prefix() {
            initWithPrefix("v_");

            assertThat(Requests.createTableVersion()).contains("v_hb_version");
        }
    }

    // =========================================================================
    // 9. SQL creation: timed runs
    // =========================================================================

    @Nested
    class CreateTableTimedRuns {

        @Test
        void createTableTimedRuns_contains_bigint() {
            initWithPrefix("");

            String sql = Requests.createTableTimedRuns();

            assertThat(sql).contains("BIGINT");
            assertThat(sql).contains("hb_timed_runs");
            assertThat(sql).contains("timeMs");
        }

        @Test
        void createTableTimedRuns_contains_completedAt() {
            initWithPrefix("");

            assertThat(Requests.createTableTimedRuns()).contains("completedAt");
            assertThat(Requests.createTableTimedRuns()).contains("CURRENT_TIMESTAMP");
        }

        @Test
        void createTableTimedRunsMySQL_matches_structure() {
            initWithPrefix("");

            String sql = Requests.createTableTimedRunsMySQL();

            assertThat(sql).contains("BIGINT");
            assertThat(sql).contains("hb_timed_runs");
            assertThat(sql).contains("PRIMARY KEY");
        }

        @Test
        void createTableTimedRuns_with_prefix() {
            initWithPrefix("t_");

            assertThat(Requests.createTableTimedRuns()).contains("t_hb_timed_runs");
        }
    }

    // =========================================================================
    // 10. Prefix propagation in SQL statements
    // =========================================================================

    @Nested
    class PrefixPropagation {

        @Test
        void sql_statements_use_prefixed_table_names() {
            initWithPrefix("test_");

            assertThat(Requests.createTablePlayers()).contains("test_hb_players");
            assertThat(Requests.createTableHeads()).contains("test_hb_heads");
            assertThat(Requests.createTablePlayerHeads()).contains("test_hb_playerHeads");
            assertThat(Requests.createTableHunts()).contains("test_hb_hunts");
            assertThat(Requests.createTableHeadHunts()).contains("test_hb_head_hunts");
            assertThat(Requests.createTableTimedRuns()).contains("test_hb_timed_runs");
        }

        @Test
        void crud_statements_use_prefixed_table_names() {
            initWithPrefix("p_");

            assertThat(Requests.updatePlayer()).contains("p_hb_players");
            assertThat(Requests.updateHead()).contains("p_hb_heads");
            assertThat(Requests.savePlayerHead()).contains("p_hb_playerHeads");
            assertThat(Requests.insertHunt()).contains("p_hb_hunts");
            assertThat(Requests.insertTimedRun()).contains("p_hb_timed_runs");
        }
    }

    // =========================================================================
    // 11. Player CRUD queries
    // =========================================================================

    @Nested
    class PlayerCrudQueries {

        @Test
        void getTablePlayer_selects_pUUID_and_pName() {
            initWithPrefix("");

            String sql = Requests.getTablePlayer();

            assertThat(sql).startsWith("SELECT pUUID, pName FROM hb_players");
        }

        @Test
        void updatePlayer_uses_insert_or_replace() {
            initWithPrefix("");

            String sql = Requests.updatePlayer();

            assertThat(sql).contains("INSERT OR REPLACE INTO");
            assertThat(sql).contains("hb_players");
            assertThat(sql).contains("(?, ?, ?)");
        }

        @Test
        void updatePlayerMySQL_uses_replace_into() {
            initWithPrefix("");

            String sql = Requests.updatePlayerMySQL();

            assertThat(sql).startsWith("REPLACE INTO");
            assertThat(sql).contains("hb_players");
            assertThat(sql).contains("(?, ?, ?)");
        }

        @Test
        void getContainsPlayer_uses_select_1() {
            initWithPrefix("");

            String sql = Requests.getContainsPlayer();

            assertThat(sql).contains("SELECT 1 FROM hb_players WHERE pUUID = ?");
        }

        @Test
        void getAllPlayers_selects_pUUID() {
            initWithPrefix("");

            assertThat(Requests.getAllPlayers()).isEqualTo("SELECT pUUID FROM hb_players");
        }

        @Test
        void getCheckPlayerName_selects_pName_and_pDisplayName() {
            initWithPrefix("");

            String sql = Requests.getCheckPlayerName();

            assertThat(sql).contains("SELECT pName, pDisplayName FROM hb_players WHERE pUUID = ?");
        }

        @Test
        void getPlayer_selects_by_pName() {
            initWithPrefix("");

            String sql = Requests.getPlayer();

            assertThat(sql).contains("SELECT pUUID, pDisplayName FROM hb_players WHERE pName = (?)");
        }
    }

    // =========================================================================
    // 12. Head CRUD queries
    // =========================================================================

    @Nested
    class HeadCrudQueries {

        @Test
        void getHeads_filters_by_hExist() {
            initWithPrefix("");

            String sql = Requests.getHeads();

            assertThat(sql).contains("SELECT * FROM hb_heads WHERE hExist = True");
        }

        @Test
        void getHeadsMySQL_filters_by_hExist_and_serverId() {
            initWithPrefix("");

            String sql = Requests.getHeadsMySQL();

            assertThat(sql).contains("hExist = True");
            assertThat(sql).contains("serverId != ''");
        }

        @Test
        void getHeadsByServerId_uses_placeholder() {
            initWithPrefix("");

            String sql = Requests.getHeadsByServerId();

            assertThat(sql).contains("hExist = True AND serverId = ?");
        }

        @Test
        void updateHead_uses_insert_or_replace() {
            initWithPrefix("");

            String sql = Requests.updateHead();

            assertThat(sql).contains("INSERT OR REPLACE INTO");
            assertThat(sql).contains("hb_heads");
            assertThat(sql).contains("(?, true, ?, ?)");
        }

        @Test
        void updateHeadMySQL_uses_replace_into() {
            initWithPrefix("");

            String sql = Requests.updateHeadMySQL();

            assertThat(sql).startsWith("REPLACE INTO");
            assertThat(sql).contains("(?, true, ?, ?)");
        }

        @Test
        void removeHead_sets_hExist_to_false() {
            initWithPrefix("");

            String sql = Requests.removeHead();

            assertThat(sql).contains("UPDATE hb_heads SET hExist=False WHERE hUUID = ?");
        }

        @Test
        void deleteHead_deletes_by_hUUID() {
            initWithPrefix("");

            String sql = Requests.deleteHead();

            assertThat(sql).contains("DELETE FROM hb_heads WHERE hUUID = ?");
        }

        @Test
        void getHeadExist_uses_select_1() {
            initWithPrefix("");

            String sql = Requests.getHeadExist();

            assertThat(sql).contains("SELECT 1 FROM hb_heads WHERE hUUID = ? AND hExist = True");
        }

        @Test
        void getHeadTexture_selects_hTexture() {
            initWithPrefix("");

            String sql = Requests.getHeadTexture();

            assertThat(sql).contains("SELECT hTexture FROM hb_heads WHERE hUUID = (?)");
        }

        @Test
        void getTableHeadsData_selects_hUUID_and_hExist() {
            initWithPrefix("");

            String sql = Requests.getTableHeadsData();

            assertThat(sql).isEqualTo("SELECT hUUID, hExist FROM hb_heads");
        }

        @Test
        void getContainsTableHeads_limits_to_1() {
            initWithPrefix("");

            String sql = Requests.getContainsTableHeads();

            assertThat(sql).isEqualTo("SELECT * FROM hb_heads LIMIT 1");
        }

        @Test
        void getPlayersByHead_selects_pUUID_by_hUUID() {
            initWithPrefix("");

            String sql = Requests.getPlayersByHead();

            assertThat(sql).contains("SELECT pUUID FROM hb_playerHeads WHERE hUUID = (?)");
        }

        @Test
        void getDistinctServerIds_filters_null_and_empty() {
            initWithPrefix("");

            String sql = Requests.getDistinctServerIds();

            assertThat(sql).contains("SELECT DISTINCT serverId FROM hb_heads");
            assertThat(sql).contains("serverId IS NOT NULL");
            assertThat(sql).contains("serverId != ''");
        }
    }

    // =========================================================================
    // 13. PlayerHead CRUD queries
    // =========================================================================

    @Nested
    class PlayerHeadCrudQueries {

        @Test
        void savePlayerHead_inserts_two_placeholders() {
            initWithPrefix("");

            String sql = Requests.savePlayerHead();

            assertThat(sql).contains("INSERT INTO hb_playerHeads (pUUID, hUUID) VALUES (?, ?)");
        }

        @Test
        void getPlayerHeads_performs_inner_join() {
            initWithPrefix("");

            String sql = Requests.getPlayerHeads();

            assertThat(sql).contains("INNER JOIN hb_heads");
            assertThat(sql).contains("INNER JOIN hb_players");
            assertThat(sql).contains("hbp.pUUID = ?");
            assertThat(sql).contains("hbh.hExist = True");
        }

        @Test
        void resetPlayer_deletes_by_pUUID() {
            initWithPrefix("");

            assertThat(Requests.resetPlayer()).isEqualTo("DELETE FROM hb_playerHeads WHERE pUUID = ?");
        }

        @Test
        void resetPlayerHead_deletes_by_pUUID_and_hUUID() {
            initWithPrefix("");

            assertThat(Requests.resetPlayerHead()).isEqualTo("DELETE FROM hb_playerHeads WHERE pUUID = ? AND hUUID = ?");
        }

        @Test
        void getTablePlayerHeadsData_selects_pUUID_and_hUUID() {
            initWithPrefix("");

            assertThat(Requests.getTablePlayerHeadsData()).isEqualTo("SELECT pUUID, hUUID FROM hb_playerHeads");
        }

        @Test
        void getTopPlayers_orders_by_count_desc() {
            initWithPrefix("");

            String sql = Requests.getTopPlayers();

            assertThat(sql).contains("COUNT(*) as hCount");
            assertThat(sql).contains("ORDER BY hCount DESC");
            assertThat(sql).contains("INNER JOIN hb_players");
            assertThat(sql).contains("INNER JOIN hb_heads");
            assertThat(sql).contains("GROUP BY pName");
        }
    }

    // =========================================================================
    // 14. Version queries
    // =========================================================================

    @Nested
    class VersionQueries {

        @Test
        void getTableVersionData_selects_current() {
            initWithPrefix("");

            assertThat(Requests.getTableVersionData()).isEqualTo("SELECT current FROM hb_version");
        }

        @Test
        void insertVersion_uses_single_placeholder() {
            initWithPrefix("");

            assertThat(Requests.insertVersion()).isEqualTo("INSERT INTO hb_version VALUES (?)");
        }

        @Test
        void upsertVersion_uses_two_placeholders() {
            initWithPrefix("");

            String sql = Requests.upsertVersion();

            assertThat(sql).contains("UPDATE hb_version SET current = (?) WHERE current = (?)");
        }
    }

    // =========================================================================
    // 15. SQLite-specific existence checks
    // =========================================================================

    @Nested
    class SQLiteExistenceChecks {

        @Test
        void getIsTablePlayersExistSQLite_checks_sqlite_master() {
            initWithPrefix("");

            String sql = Requests.getIsTablePlayersExistSQLite();

            assertThat(sql).contains("sqlite_master");
            assertThat(sql).contains("hb_players");
        }

        @Test
        void getTableHeadsColumnsSQLite_uses_pragma() {
            initWithPrefix("");

            String sql = Requests.getTableHeadsColumnsSQLite();

            assertThat(sql).contains("pragma_table_info");
            assertThat(sql).contains("hb_heads");
        }
    }

    // =========================================================================
    // 16. MySQL-specific existence checks
    // =========================================================================

    @Nested
    class MySQLExistenceChecks {

        @Test
        void getIsTablePlayersExistMySQL_uses_information_schema() {
            initWithPrefixAndDbName("", "mydb");

            String sql = Requests.getIsTablePlayersExistMySQL();

            assertThat(sql).contains("information_schema.tables");
            assertThat(sql).contains("mydb");
            assertThat(sql).contains("hb_players");
        }

        @Test
        void getTableHeadsColumnsMySQL_uses_information_schema() {
            initWithPrefix("");

            String sql = Requests.getTableHeadsColumnsMySQL();

            assertThat(sql).contains("INFORMATION_SCHEMA.COLUMNS");
            assertThat(sql).contains("hb_heads");
        }

        @Test
        void isColumnExist_uses_database_name() {
            initWithPrefixAndDbName("", "testdb");

            String sql = Requests.isColumnExist();

            assertThat(sql).contains("information_schema.COLUMNS");
            assertThat(sql).contains("testdb");
            assertThat(sql).contains("TABLE_NAME = ?");
            assertThat(sql).contains("COLUMN_NAME = ?");
        }
    }

    // =========================================================================
    // 17. Hunt CRUD queries
    // =========================================================================

    @Nested
    class HBHuntCrudQueries {

        @Test
        void insertHunt_uses_three_placeholders() {
            initWithPrefix("");

            String sql = Requests.insertHunt();

            assertThat(sql).contains("INSERT INTO hb_hunts (hId, hName, hState) VALUES (?, ?, ?)");
        }

        @Test
        void updateHuntState_updates_hState_by_hId() {
            initWithPrefix("");

            String sql = Requests.updateHuntState();

            assertThat(sql).contains("UPDATE hb_hunts SET hState = ? WHERE hId = ?");
        }

        @Test
        void updateHuntName_updates_hName_by_hId() {
            initWithPrefix("");

            String sql = Requests.updateHuntName();

            assertThat(sql).contains("UPDATE hb_hunts SET hName = ? WHERE hId = ?");
        }

        @Test
        void deleteHuntById_deletes_by_hId() {
            initWithPrefix("");

            assertThat(Requests.deleteHuntById()).isEqualTo("DELETE FROM hb_hunts WHERE hId = ?");
        }

        @Test
        void getHuntsAll_selects_all_columns() {
            initWithPrefix("");

            assertThat(Requests.getHuntsAll()).isEqualTo("SELECT hId, hName, hState FROM hb_hunts");
        }

        @Test
        void getHuntById_selects_by_hId() {
            initWithPrefix("");

            String sql = Requests.getHuntById();

            assertThat(sql).contains("SELECT hId, hName, hState FROM hb_hunts WHERE hId = ?");
        }
    }

    // =========================================================================
    // 18. Head-Hunt linking queries
    // =========================================================================

    @Nested
    class HeadHBHuntLinkQueries {

        @Test
        void getHuntsForHead_selects_huntId_by_headUUID() {
            initWithPrefix("");

            assertThat(Requests.getHuntsForHead()).isEqualTo("SELECT huntId FROM hb_head_hunts WHERE headUUID = ?");
        }

    }

    // =========================================================================
    // 19. Hunt-aware player progression queries
    // =========================================================================

    @Nested
    class HBHuntAwareProgressionQueries {

        @Test
        void savePlayerHeadHunt_contains_three_placeholders() {
            initWithPrefix("");

            String sql = Requests.savePlayerHeadHunt();

            assertThat(sql).contains("(?, ?, ?)");
            assertThat(sql).contains("hb_playerHeads");
        }

        @Test
        void getPlayerHeadsForHunt_joins_with_heads_and_filters_hExist() {
            initWithPrefix("");

            String sql = Requests.getPlayerHeadsForHunt();

            assertThat(sql).contains("INNER JOIN hb_heads");
            assertThat(sql).contains("hbph.pUUID = ?");
            assertThat(sql).contains("hbph.huntId = ?");
            assertThat(sql).contains("hbh.hExist = True");
        }

        @Test
        void resetPlayerHunt_deletes_by_pUUID_and_huntId() {
            initWithPrefix("");

            assertThat(Requests.resetPlayerHunt()).isEqualTo("DELETE FROM hb_playerHeads WHERE pUUID = ? AND huntId = ?");
        }

        @Test
        void resetPlayerHeadHunt_deletes_by_pUUID_hUUID_and_huntId() {
            initWithPrefix("");

            assertThat(Requests.resetPlayerHeadHunt()).isEqualTo("DELETE FROM hb_playerHeads WHERE pUUID = ? AND hUUID = ? AND huntId = ?");
        }

        @Test
        void getTopPlayersForHunt_orders_by_count_desc_and_filters_by_huntId() {
            initWithPrefix("");

            String sql = Requests.getTopPlayersForHunt();

            assertThat(sql).contains("COUNT(*) as hCount");
            assertThat(sql).contains("ORDER BY hCount DESC");
            assertThat(sql).contains("hbph.huntId = ?");
            assertThat(sql).contains("hbh.hExist = True");
        }

        @Test
        void transferPlayerProgressSQLite_uses_insert_or_ignore() {
            initWithPrefix("");

            String sql = Requests.transferPlayerProgressSQLite();

            assertThat(sql).contains("INSERT OR IGNORE INTO hb_playerHeads");
            assertThat(sql).contains("SELECT pUUID, hUUID, ?");
            assertThat(sql).contains("WHERE huntId = ?");
        }

        @Test
        void transferPlayerProgressMySQL_uses_insert_ignore() {
            initWithPrefix("");

            String sql = Requests.transferPlayerProgressMySQL();

            assertThat(sql).contains("INSERT IGNORE INTO hb_playerHeads");
            assertThat(sql).contains("SELECT pUUID, hUUID, ?");
            assertThat(sql).contains("WHERE huntId = ?");
        }

        @Test
        void deletePlayerProgressForHunt_deletes_by_huntId() {
            initWithPrefix("");

            assertThat(Requests.deletePlayerProgressForHunt()).isEqualTo("DELETE FROM hb_playerHeads WHERE huntId = ?");
        }
    }

    // =========================================================================
    // 20. Timed run queries
    // =========================================================================

    @Nested
    class TimedRunQueries {

        @Test
        void insertTimedRun_uses_three_placeholders() {
            initWithPrefix("");

            String sql = Requests.insertTimedRun();

            assertThat(sql).contains("INSERT INTO hb_timed_runs (pUUID, huntId, timeMs) VALUES (?, ?, ?)");
        }

        @Test
        void getTimedLeaderboard_orders_by_bestTime_asc() {
            initWithPrefix("");

            String sql = Requests.getTimedLeaderboard();

            assertThat(sql).contains("MIN(tr.timeMs) as bestTime");
            assertThat(sql).contains("ORDER BY bestTime ASC");
            assertThat(sql).contains("LIMIT ?");
            assertThat(sql).contains("INNER JOIN hb_players");
            assertThat(sql).contains("tr.huntId = ?");
        }

        @Test
        void getBestTime_uses_min_aggregate() {
            initWithPrefix("");

            String sql = Requests.getBestTime();

            assertThat(sql).contains("MIN(timeMs) as bestTime");
            assertThat(sql).contains("pUUID = ?");
            assertThat(sql).contains("huntId = ?");
        }

        @Test
        void getTimedRunCount_uses_count_aggregate() {
            initWithPrefix("");

            String sql = Requests.getTimedRunCount();

            assertThat(sql).contains("COUNT(*) as cnt");
            assertThat(sql).contains("pUUID = ?");
            assertThat(sql).contains("huntId = ?");
        }

        @Test
        void insertTimedRun_with_prefix() {
            initWithPrefix("x_");

            assertThat(Requests.insertTimedRun()).contains("x_hb_timed_runs");
        }
    }

    // =========================================================================
    // 21. Migration queries
    // =========================================================================

    @Nested
    class MigrationQueries {

        @Test
        void migArchiveTable_creates_hb_players_old() {
            initWithPrefix("");

            String sql = Requests.migArchiveTable();

            assertThat(sql).contains("hb_players_old");
            assertThat(sql).contains("PRIMARY KEY");
        }

        @Test
        void migCopyOldToArchive_inserts_from_players_to_old() {
            initWithPrefix("");

            String sql = Requests.migCopyOldToArchive();

            assertThat(sql).contains("INSERT INTO hb_players_old SELECT * FROM hb_players");
        }

        @Test
        void migDeleteOld_drops_players_table() {
            initWithPrefix("");

            assertThat(Requests.migDeleteOld()).isEqualTo("DROP TABLE hb_players");
        }

        @Test
        void migImportOldUsers_selects_distinct_from_old() {
            initWithPrefix("");

            assertThat(Requests.migImportOldUsers()).isEqualTo("SELECT DISTINCT pUUID FROM hb_players_old");
        }

        @Test
        void migInsertPlayer_inserts_pUUID_and_pName() {
            initWithPrefix("");

            String sql = Requests.migInsertPlayer();

            assertThat(sql).contains("INSERT INTO hb_players");
            assertThat(sql).contains("(?, ?)");
        }

        @Test
        void migImportOldHeads_selects_from_old_table() {
            initWithPrefix("");

            String sql = Requests.migImportOldHeads();

            assertThat(sql).contains("INSERT INTO hb_heads");
            assertThat(sql).contains("hb_players_old");
        }

        @Test
        void migRemap_copies_from_old_to_playerHeads() {
            initWithPrefix("");

            String sql = Requests.migRemap();

            assertThat(sql).contains("INSERT INTO hb_playerHeads");
            assertThat(sql).contains("hb_players_old");
        }

        @Test
        void migDelArchive_drops_old_table() {
            initWithPrefix("");

            assertThat(Requests.migDelArchive()).isEqualTo("DROP TABLE hb_players_old");
        }
    }

    // =========================================================================
    // 22. Column alteration queries
    // =========================================================================

    @Nested
    class ColumnAlterations {

        @Test
        void addColumnHeadTextureMariaDb_uses_if_not_exists() {
            initWithPrefix("");

            String sql = Requests.addColumnHeadTextureMariaDb();

            assertThat(sql).contains("ALTER TABLE hb_heads ADD COLUMN IF NOT EXISTS hTexture");
        }

        @Test
        void addColumnHeadTextureMySQL_adds_column() {
            initWithPrefix("");

            String sql = Requests.addColumnHeadTextureMySQL();

            assertThat(sql).contains("ALTER TABLE hb_heads ADD COLUMN hTexture");
            assertThat(sql).doesNotContain("IF NOT EXISTS");
        }

        @Test
        void addColumnHeadTextureSQLite_adds_column() {
            initWithPrefix("");

            String sql = Requests.addColumnHeadTextureSQLite();

            assertThat(sql).contains("ALTER TABLE hb_heads ADD COLUMN hTexture");
        }

        @Test
        void addColumnPlayerDisplayNameMariaDb_uses_if_not_exists() {
            initWithPrefix("");

            String sql = Requests.addColumnPlayerDisplayNameMariaDb();

            assertThat(sql).contains("ALTER TABLE hb_players ADD COLUMN IF NOT EXISTS pDisplayName");
        }

        @Test
        void addColumnPlayerDisplayNameSQLite_adds_column() {
            initWithPrefix("");

            String sql = Requests.addColumnPlayerDisplayNameSQLite();

            assertThat(sql).contains("ALTER TABLE hb_players ADD COLUMN pDisplayName");
        }

        @Test
        void addColumnPlayerDisplayNameMySQL_adds_column() {
            initWithPrefix("");

            String sql = Requests.addColumnPlayerDisplayNameMySQL();

            assertThat(sql).contains("ALTER TABLE hb_players ADD COLUMN pDisplayName");
        }

        @Test
        void addColumnServerIdentifierSQLite_adds_column() {
            initWithPrefix("");

            String sql = Requests.addColumnServerIdentifierSQLite();

            assertThat(sql).contains("ALTER TABLE hb_heads ADD COLUMN serverId");
        }

        @Test
        void addColumnServerIdentifierMariaDb_uses_if_not_exists() {
            initWithPrefix("");

            String sql = Requests.addColumnServerIdentifierMariaDb();

            assertThat(sql).contains("ALTER TABLE hb_heads ADD COLUMN IF NOT EXISTS serverId");
        }

        @Test
        void addColumnServerIdentifierMySQL_adds_column() {
            initWithPrefix("");

            String sql = Requests.addColumnServerIdentifierMySQL();

            assertThat(sql).contains("ALTER TABLE hb_heads ADD COLUMN serverId");
        }

        @Test
        void column_alterations_use_prefix() {
            initWithPrefix("pre_");

            assertThat(Requests.addColumnHeadTextureMariaDb()).contains("pre_hb_heads");
            assertThat(Requests.addColumnPlayerDisplayNameMariaDb()).contains("pre_hb_players");
            assertThat(Requests.addColumnServerIdentifierMariaDb()).contains("pre_hb_heads");
        }
    }

    // =========================================================================
    // 23. Migration v5 queries
    // =========================================================================

    @Nested
    class MigrationV5Queries {

        @Test
        void addColumnHuntIdSQLite_adds_huntId_with_default() {
            initWithPrefix("");

            String sql = Requests.addColumnHuntIdSQLite();

            assertThat(sql).contains("ALTER TABLE hb_playerHeads ADD COLUMN huntId");
            assertThat(sql).contains("DEFAULT 'default'");
        }

        @Test
        void addColumnHuntIdMariaDb_uses_if_not_exists() {
            initWithPrefix("");

            String sql = Requests.addColumnHuntIdMariaDb();

            assertThat(sql).contains("IF NOT EXISTS");
            assertThat(sql).contains("huntId");
        }

        @Test
        void addColumnHuntIdMySQL_adds_column() {
            initWithPrefix("");

            String sql = Requests.addColumnHuntIdMySQL();

            assertThat(sql).contains("ALTER TABLE hb_playerHeads ADD COLUMN huntId");
        }

        @Test
        void migV5InsertDefaultHunt_inserts_default_hunt() {
            initWithPrefix("");

            String sql = Requests.migV5InsertDefaultHunt();

            assertThat(sql).contains("INSERT INTO hb_hunts");
            assertThat(sql).contains("'default'");
            assertThat(sql).contains("'Default'");
            assertThat(sql).contains("'ACTIVE'");
        }

        @Test
        void migV5LinkAllHeadsToDefault_links_existing_heads() {
            initWithPrefix("");

            String sql = Requests.migV5LinkAllHeadsToDefault();

            assertThat(sql).contains("INSERT INTO hb_head_hunts");
            assertThat(sql).contains("SELECT hUUID, 'default' FROM hb_heads");
            assertThat(sql).contains("hExist = True");
        }

        @Test
        void migV5CreateTempPlayerHeadsSQLite_creates_temp_table() {
            initWithPrefix("");

            String sql = Requests.migV5CreateTempPlayerHeadsSQLite();

            assertThat(sql).contains("hb_playerHeads_v5tmp");
            assertThat(sql).contains("PRIMARY KEY(pUUID, hUUID, huntId)");
        }

        @Test
        void migV5CopyPlayerHeadsToTempSQLite_copies_with_default_huntId() {
            initWithPrefix("");

            String sql = Requests.migV5CopyPlayerHeadsToTempSQLite();

            assertThat(sql).contains("INSERT INTO hb_playerHeads_v5tmp");
            assertThat(sql).contains("'default'");
            assertThat(sql).contains("FROM hb_playerHeads");
        }

        @Test
        void migV5DropOldPlayerHeadsSQLite_drops_table() {
            initWithPrefix("");

            assertThat(Requests.migV5DropOldPlayerHeadsSQLite()).isEqualTo("DROP TABLE hb_playerHeads");
        }

        @Test
        void migV5RenameTempPlayerHeadsSQLite_renames_table() {
            initWithPrefix("");

            String sql = Requests.migV5RenameTempPlayerHeadsSQLite();

            assertThat(sql).contains("ALTER TABLE hb_playerHeads_v5tmp RENAME TO hb_playerHeads");
        }
    }

    // =========================================================================
    // 24. Init behavior
    // =========================================================================

    @Nested
    class InitBehavior {

        @Test
        void init_with_database_disabled_uses_empty_prefix() {
            ConfigService configService = mock(ConfigService.class);
            when(configService.databaseEnabled()).thenReturn(false);
            when(configService.databaseName()).thenReturn("testdb");

            Requests.init(configService);

            assertThat(Requests.getTablePlayers()).isEqualTo("hb_players");
        }

        @Test
        void init_with_database_enabled_uses_configured_prefix() {
            ConfigService configService = mock(ConfigService.class);
            when(configService.databaseEnabled()).thenReturn(true);
            when(configService.databasePrefix()).thenReturn("srv_");
            when(configService.databaseName()).thenReturn("testdb");

            Requests.init(configService);

            assertThat(Requests.getTablePlayers()).isEqualTo("srv_hb_players");
        }

        @Test
        void init_stores_database_name_for_mysql_queries() {
            initWithPrefixAndDbName("", "myappdb");

            String sql = Requests.getIsTablePlayersExistMySQL();

            assertThat(sql).contains("myappdb");
        }

        @Test
        void init_reinitializes_prefix_when_called_again() {
            initWithPrefix("first_");
            assertThat(Requests.getTablePlayers()).isEqualTo("first_hb_players");

            initWithPrefix("second_");
            assertThat(Requests.getTablePlayers()).isEqualTo("second_hb_players");
        }
    }
}
