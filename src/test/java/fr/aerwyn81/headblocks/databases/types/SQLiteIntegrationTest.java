package fr.aerwyn81.headblocks.databases.types;

import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.databases.Requests;
import fr.aerwyn81.headblocks.services.ConfigService;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SQLiteIntegrationTest {

    @TempDir
    Path tempDir;

    private SQLite db;

    @BeforeEach
    void setUp() throws InternalException {
        ConfigService configService = mock(ConfigService.class);
        when(configService.databaseEnabled()).thenReturn(false);
        when(configService.databasePrefix()).thenReturn("");
        when(configService.databaseName()).thenReturn(null);
        Requests.init(configService);

        db = new SQLite(tempDir.resolve("test.db").toString());
        db.open();
        db.load();
    }

    @AfterEach
    void tearDown() throws InternalException {
        if (db != null) {
            db.close();
        }
    }

    // ---- CRUD Players ----

    @Test
    void updatePlayerInfo_creates_player_that_containsPlayer_finds() throws InternalException {
        UUID pUUID = UUID.randomUUID();
        PlayerProfileLight profile = new PlayerProfileLight(pUUID, "Steve", "SteveDisplay");

        db.updatePlayerInfo(profile);

        assertThat(db.containsPlayer(pUUID)).isTrue();
    }

    @Test
    void containsPlayer_returns_false_for_unknown_player() throws InternalException {
        assertThat(db.containsPlayer(UUID.randomUUID())).isFalse();
    }

    @Test
    void getPlayerByName_finds_player_by_name() throws InternalException {
        UUID pUUID = UUID.randomUUID();
        db.updatePlayerInfo(new PlayerProfileLight(pUUID, "Alex", "AlexDisplay"));

        PlayerProfileLight result = db.getPlayerByName("Alex");

        assertThat(result).isNotNull();
        assertThat(result.uuid()).isEqualTo(pUUID);
        assertThat(result.customDisplay()).isEqualTo("AlexDisplay");
    }

    @Test
    void getPlayerByName_returns_null_for_unknown_name() throws InternalException {
        assertThat(db.getPlayerByName("Unknown")).isNull();
    }

    @Test
    void hasPlayerRenamed_detects_name_change() throws InternalException {
        UUID pUUID = UUID.randomUUID();
        db.updatePlayerInfo(new PlayerProfileLight(pUUID, "OldName", "OldDisplay"));

        PlayerProfileLight updated = new PlayerProfileLight(pUUID, "NewName", "OldDisplay");

        assertThat(db.hasPlayerRenamed(updated)).isTrue();
    }

    @Test
    void hasPlayerRenamed_returns_false_when_name_unchanged() throws InternalException {
        UUID pUUID = UUID.randomUUID();
        db.updatePlayerInfo(new PlayerProfileLight(pUUID, "SameName", "SameDisplay"));

        PlayerProfileLight same = new PlayerProfileLight(pUUID, "SameName", "SameDisplay");

        assertThat(db.hasPlayerRenamed(same)).isFalse();
    }

    @Test
    void getAllPlayers_returns_all_inserted_players() throws InternalException {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        db.updatePlayerInfo(new PlayerProfileLight(p1, "P1", ""));
        db.updatePlayerInfo(new PlayerProfileLight(p2, "P2", ""));

        var result = db.getAllPlayers();

        assertThat(result).containsExactlyInAnyOrder(p1, p2);
    }

    // ---- CRUD Heads ----

    @Test
    void createNewHead_creates_head_that_isHeadExist_finds() throws InternalException {
        UUID head = UUID.randomUUID();

        db.createNewHead(head, "textureABC", "srv1");

        assertThat(db.isHeadExist(head)).isTrue();
    }

    @Test
    void isHeadExist_returns_false_for_unknown_head() throws InternalException {
        assertThat(db.isHeadExist(UUID.randomUUID())).isFalse();
    }

    @Test
    void getHeadTexture_returns_stored_texture() throws InternalException {
        UUID head = UUID.randomUUID();
        db.createNewHead(head, "tex123", "srv1");

        String texture = db.getHeadTexture(head);

        assertThat(texture).isEqualTo("tex123");
    }

    @Test
    void getHeads_returns_existing_heads() throws InternalException {
        UUID h1 = UUID.randomUUID();
        UUID h2 = UUID.randomUUID();
        db.createNewHead(h1, "t1", "srv1");
        db.createNewHead(h2, "t2", "srv1");

        var result = db.getHeads();

        assertThat(result).containsExactlyInAnyOrder(h1, h2);
    }

    @Test
    void getHeads_by_serverId_filters_correctly() throws InternalException {
        UUID h1 = UUID.randomUUID();
        UUID h2 = UUID.randomUUID();
        db.createNewHead(h1, "t1", "srv1");
        db.createNewHead(h2, "t2", "srv2");

        var result = db.getHeads("srv1");

        assertThat(result).containsExactly(h1);
    }

    // ---- Player-Head linking ----

    @Test
    void addHead_links_player_to_head() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();
        db.updatePlayerInfo(new PlayerProfileLight(player, "P", ""));
        db.createNewHead(head, "t", "s");

        db.addHead(player, head);

        assertThat(db.getHeadsPlayer(player)).containsExactly(head);
    }

    @Test
    void getHeadsPlayer_returns_empty_for_player_with_no_heads() throws InternalException {
        UUID player = UUID.randomUUID();
        db.updatePlayerInfo(new PlayerProfileLight(player, "P", ""));

        assertThat(db.getHeadsPlayer(player)).isEmpty();
    }

    @Test
    void resetPlayer_removes_all_player_head_links() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID h1 = UUID.randomUUID();
        UUID h2 = UUID.randomUUID();
        db.updatePlayerInfo(new PlayerProfileLight(player, "P", ""));
        db.createNewHead(h1, "t", "s");
        db.createNewHead(h2, "t", "s");
        db.addHead(player, h1);
        db.addHead(player, h2);

        db.resetPlayer(player);

        assertThat(db.getHeadsPlayer(player)).isEmpty();
    }

    @Test
    void resetPlayerHead_removes_specific_link() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID h1 = UUID.randomUUID();
        UUID h2 = UUID.randomUUID();
        db.updatePlayerInfo(new PlayerProfileLight(player, "P", ""));
        db.createNewHead(h1, "t", "s");
        db.createNewHead(h2, "t", "s");
        db.addHead(player, h1);
        db.addHead(player, h2);

        db.resetPlayerHead(player, h1);

        assertThat(db.getHeadsPlayer(player)).containsExactly(h2);
    }

    @Test
    void getTopPlayers_returns_ranked_list() throws InternalException {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID h1 = UUID.randomUUID();
        UUID h2 = UUID.randomUUID();
        UUID h3 = UUID.randomUUID();
        db.updatePlayerInfo(new PlayerProfileLight(p1, "Top", ""));
        db.updatePlayerInfo(new PlayerProfileLight(p2, "Second", ""));
        db.createNewHead(h1, "t", "s");
        db.createNewHead(h2, "t", "s");
        db.createNewHead(h3, "t", "s");
        db.addHead(p1, h1);
        db.addHead(p1, h2);
        db.addHead(p2, h3);

        LinkedHashMap<PlayerProfileLight, Integer> top = db.getTopPlayers();

        assertThat(top).hasSize(2);
        var entries = top.entrySet().stream().toList();
        assertThat(entries.get(0).getValue()).isEqualTo(2);
        assertThat(entries.get(1).getValue()).isEqualTo(1);
    }

    // ---- Hunts CRUD ----

    @Test
    void createHunt_and_getHunts_returns_created_hunt() throws InternalException {
        db.createHunt("hunt1", "First Hunt", "ACTIVE");

        var hunts = db.getHunts();

        assertThat(hunts).hasSize(1);
        assertThat(hunts.get(0)[0]).isEqualTo("hunt1");
        assertThat(hunts.get(0)[1]).isEqualTo("First Hunt");
        assertThat(hunts.get(0)[2]).isEqualTo("ACTIVE");
    }

    @Test
    void updateHuntState_changes_state() throws InternalException {
        db.createHunt("hunt2", "H2", "ACTIVE");

        db.updateHuntState("hunt2", "PAUSED");

        var hunt = db.getHuntById("hunt2");
        assertThat(hunt).isNotNull();
        assertThat(hunt[2]).isEqualTo("PAUSED");
    }

    @Test
    void updateHuntName_changes_name() throws InternalException {
        db.createHunt("hunt3", "OldName", "ACTIVE");

        db.updateHuntName("hunt3", "NewName");

        var hunt = db.getHuntById("hunt3");
        assertThat(hunt).isNotNull();
        assertThat(hunt[1]).isEqualTo("NewName");
    }

    @Test
    void deleteHunt_removes_hunt() throws InternalException {
        db.createHunt("huntDel", "ToDelete", "ACTIVE");

        db.deleteHunt("huntDel");

        assertThat(db.getHuntById("huntDel")).isNull();
    }

    // ---- Hunt player progression ----

    @Test
    void addHeadForHunt_and_getHeadsPlayerForHunt_returns_heads() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();
        db.updatePlayerInfo(new PlayerProfileLight(player, "P", ""));
        db.createNewHead(head, "t", "s");
        db.createHunt("hunt7", "H7", "ACTIVE");

        db.addHeadForHunt(player, head, "hunt7");

        assertThat(db.getHeadsPlayerForHunt(player, "hunt7")).containsExactly(head);
    }

    @Test
    void getTopPlayersForHunt_returns_ranked_results() throws InternalException {
        UUID p1 = UUID.randomUUID();
        UUID h1 = UUID.randomUUID();
        UUID h2 = UUID.randomUUID();
        db.updatePlayerInfo(new PlayerProfileLight(p1, "P1", ""));
        db.createNewHead(h1, "t", "s");
        db.createNewHead(h2, "t", "s");
        db.createHunt("hunt8", "H8", "ACTIVE");
        db.addHeadForHunt(p1, h1, "hunt8");
        db.addHeadForHunt(p1, h2, "hunt8");

        var top = db.getTopPlayersForHunt("hunt8");

        assertThat(top).hasSize(1);
        assertThat(top.values().iterator().next()).isEqualTo(2);
    }

    @Test
    void resetPlayerHunt_removes_all_player_progress_in_hunt() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID h1 = UUID.randomUUID();
        db.updatePlayerInfo(new PlayerProfileLight(player, "P", ""));
        db.createNewHead(h1, "t", "s");
        db.createHunt("hunt9", "H9", "ACTIVE");
        db.addHeadForHunt(player, h1, "hunt9");

        db.resetPlayerHunt(player, "hunt9");

        assertThat(db.getHeadsPlayerForHunt(player, "hunt9")).isEmpty();
    }

    @Test
    void transferPlayerProgress_copies_progress_to_new_hunt() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();
        db.updatePlayerInfo(new PlayerProfileLight(player, "P", ""));
        db.createNewHead(head, "t", "s");
        db.createHunt("from", "From", "ACTIVE");
        db.createHunt("to", "To", "ACTIVE");
        db.addHeadForHunt(player, head, "from");

        db.transferPlayerProgress("from", "to");

        assertThat(db.getHeadsPlayerForHunt(player, "to")).containsExactly(head);
        assertThat(db.getHeadsPlayerForHunt(player, "from")).isEmpty();
    }

    // ---- Timed runs ----

    @Test
    void saveTimedRun_and_getBestTime_returns_minimum_time() throws InternalException, InterruptedException {
        UUID player = UUID.randomUUID();
        db.updatePlayerInfo(new PlayerProfileLight(player, "P", ""));
        db.createHunt("huntT", "HuntTimed", "ACTIVE");

        db.saveTimedRun(player, "huntT", 5000);
        Thread.sleep(1100);
        db.saveTimedRun(player, "huntT", 3000);
        Thread.sleep(1100);
        db.saveTimedRun(player, "huntT", 7000);

        Long best = db.getBestTime(player, "huntT");
        assertThat(best).isEqualTo(3000L);
    }

    @Test
    void getBestTime_returns_null_when_no_runs() throws InternalException {
        UUID player = UUID.randomUUID();
        db.updatePlayerInfo(new PlayerProfileLight(player, "P", ""));
        db.createHunt("huntNoRun", "No Run", "ACTIVE");

        Long best = db.getBestTime(player, "huntNoRun");

        assertThat(best).isNull();
    }

    @Test
    void getTimedRunCount_returns_correct_count() throws InternalException, InterruptedException {
        UUID player = UUID.randomUUID();
        db.updatePlayerInfo(new PlayerProfileLight(player, "P", ""));
        db.createHunt("huntC", "HuntCount", "ACTIVE");

        db.saveTimedRun(player, "huntC", 1000);
        Thread.sleep(1100);
        db.saveTimedRun(player, "huntC", 2000);

        int count = db.getTimedRunCount(player, "huntC");
        assertThat(count).isEqualTo(2);
    }

    @Test
    void getTimedLeaderboard_returns_ranked_by_best_time() throws InternalException, InterruptedException {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        db.updatePlayerInfo(new PlayerProfileLight(p1, "Fast", ""));
        db.updatePlayerInfo(new PlayerProfileLight(p2, "Slow", ""));
        db.createHunt("huntLB", "LB", "ACTIVE");
        db.saveTimedRun(p1, "huntLB", 2000);
        Thread.sleep(1100);
        db.saveTimedRun(p2, "huntLB", 5000);
        Thread.sleep(1100);
        db.saveTimedRun(p1, "huntLB", 1000);

        var lb = db.getTimedLeaderboard("huntLB", 10);

        assertThat(lb).hasSize(2);
        var entries = lb.entrySet().stream().toList();
        assertThat(entries.get(0).getKey().name()).isEqualTo("Fast");
        assertThat(entries.get(0).getValue()).isEqualTo(1000L);
        assertThat(entries.get(1).getKey().name()).isEqualTo("Slow");
        assertThat(entries.get(1).getValue()).isEqualTo(5000L);
    }

    // ---- Version ----

    @Test
    void checkVersion_returns_version_after_load() throws InternalException {
        int version = db.checkVersion();

        assertThat(version).isEqualTo(5);
    }

    @Test
    void isDefaultTablesExist_returns_true_after_load() {
        assertThat(db.isDefaultTablesExist()).isTrue();
    }

    @Test
    void upsertTableVersion_updates_version() throws InternalException {
        int currentVersion = db.checkVersion();

        db.upsertTableVersion(currentVersion);

        assertThat(db.checkVersion()).isEqualTo(5);
    }

    // ---- Additional AbstractDatabase coverage ----

    @Test
    void removeHead_withDelete_true_removes_head_completely() throws InternalException {
        UUID head = UUID.randomUUID();
        db.createNewHead(head, "texture", "s1");

        db.removeHead(head, true);

        assertThat(db.isHeadExist(head)).isFalse();
    }

    @Test
    void removeHead_withDelete_false_marks_head_as_not_existing() throws InternalException {
        UUID head = UUID.randomUUID();
        db.createNewHead(head, "texture", "s1");

        db.removeHead(head, false);

        // Head record still exists but flagged
        assertThat(db.getHeads()).doesNotContain(head);
    }

    @Test
    void getHuntById_returns_hunt_data() throws InternalException {
        db.createHunt("myHunt", "My Hunt", "ACTIVE");

        String[] hunt = db.getHuntById("myHunt");

        assertThat(hunt).isNotNull();
        assertThat(hunt[0]).isEqualTo("myHunt");
        assertThat(hunt[1]).isEqualTo("My Hunt");
        assertThat(hunt[2]).isEqualTo("ACTIVE");
    }

    @Test
    void getHuntById_returns_null_for_unknown() throws InternalException {
        String[] hunt = db.getHuntById("nonexistent");

        assertThat(hunt).isNull();
    }

    @Test
    void deletePlayerProgressForHunt_removes_all_progress() throws InternalException {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID h1 = UUID.randomUUID();
        UUID h2 = UUID.randomUUID();
        db.updatePlayerInfo(new PlayerProfileLight(p1, "P1", ""));
        db.updatePlayerInfo(new PlayerProfileLight(p2, "P2", ""));
        db.createNewHead(h1, "", "s");
        db.createNewHead(h2, "", "s");
        db.createHunt("huntDel", "Delete", "ACTIVE");
        db.addHeadForHunt(p1, h1, "huntDel");
        db.addHeadForHunt(p2, h2, "huntDel");

        db.deletePlayerProgressForHunt("huntDel");

        assertThat(db.getHeadsPlayerForHunt(p1, "huntDel")).isEmpty();
        assertThat(db.getHeadsPlayerForHunt(p2, "huntDel")).isEmpty();
    }

    @Test
    void resetPlayerHeadHunt_removes_specific_head_from_hunt() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID h1 = UUID.randomUUID();
        UUID h2 = UUID.randomUUID();
        db.updatePlayerInfo(new PlayerProfileLight(player, "P", ""));
        db.createNewHead(h1, "", "s");
        db.createNewHead(h2, "", "s");
        db.createHunt("huntRPH", "RPH", "ACTIVE");
        db.addHeadForHunt(player, h1, "huntRPH");
        db.addHeadForHunt(player, h2, "huntRPH");

        db.resetPlayerHeadHunt(player, h1, "huntRPH");

        assertThat(db.getHeadsPlayerForHunt(player, "huntRPH")).containsExactly(h2);
    }

    @Test
    void getDistinctServerIds_returns_unique_ids() throws InternalException {
        UUID h1 = UUID.randomUUID();
        UUID h2 = UUID.randomUUID();
        UUID h3 = UUID.randomUUID();
        db.createNewHead(h1, "", "server-a");
        db.createNewHead(h2, "", "server-b");
        db.createNewHead(h3, "", "server-a");

        var serverIds = db.getDistinctServerIds();

        assertThat(serverIds).containsExactlyInAnyOrder("server-a", "server-b");
    }

    @Test
    void getTableHeads_returns_all_head_rows() throws InternalException {
        UUID h1 = UUID.randomUUID();
        UUID h2 = UUID.randomUUID();
        db.createNewHead(h1, "tex1", "s1");
        db.createNewHead(h2, "tex2", "s1");

        var heads = db.getTableHeads();

        assertThat(heads).hasSize(2);
        assertThat(heads.stream().map(r -> r.uuid())).containsExactlyInAnyOrder(h1.toString(), h2.toString());
    }

    @Test
    void getTablePlayerHeads_returns_all_links() throws InternalException {
        UUID player = UUID.randomUUID();
        UUID head = UUID.randomUUID();
        db.updatePlayerInfo(new PlayerProfileLight(player, "P", ""));
        db.createNewHead(head, "", "s");
        db.addHead(player, head);

        var playerHeads = db.getTablePlayerHeads();

        assertThat(playerHeads).hasSize(1);
        assertThat(playerHeads.get(0).playerUuid()).isEqualTo(player.toString());
        assertThat(playerHeads.get(0).headUuid()).isEqualTo(head.toString());
    }

    @Test
    void getTablePlayers_returns_all_players() throws InternalException {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        db.updatePlayerInfo(new PlayerProfileLight(p1, "Alice", ""));
        db.updatePlayerInfo(new PlayerProfileLight(p2, "Bob", ""));

        var players = db.getTablePlayers();

        assertThat(players).hasSize(2);
        assertThat(players.stream().map(r -> r.name())).containsExactlyInAnyOrder("Alice", "Bob");
    }

    @Test
    void getPlayers_returns_players_that_found_head() throws InternalException {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID head = UUID.randomUUID();
        db.updatePlayerInfo(new PlayerProfileLight(p1, "P1", ""));
        db.updatePlayerInfo(new PlayerProfileLight(p2, "P2", ""));
        db.createNewHead(head, "", "s");
        db.addHead(p1, head);
        db.addHead(p2, head);

        var players = db.getPlayers(head);

        assertThat(players).containsExactlyInAnyOrder(p1, p2);
    }

    @Test
    void getHeadTexture_returns_empty_for_unknown_head() throws InternalException {
        UUID unknown = UUID.randomUUID();

        String texture = db.getHeadTexture(unknown);

        assertThat(texture).isEmpty();
    }

    @Test
    void getHeads_byServerId_returns_empty_for_unknown_server() throws InternalException {
        UUID head = UUID.randomUUID();
        db.createNewHead(head, "", "server-x");

        var heads = db.getHeads("server-y");

        assertThat(heads).isEmpty();
    }

    @Test
    void getTopPlayersForHunt_empty_hunt_returns_empty() throws InternalException {
        db.createHunt("emptyH", "Empty", "ACTIVE");

        var top = db.getTopPlayersForHunt("emptyH");

        assertThat(top).isEmpty();
    }

    @Test
    void hasPlayerRenamed_returns_true_for_unknown_player() throws InternalException {
        UUID unknown = UUID.randomUUID();

        boolean renamed = db.hasPlayerRenamed(new PlayerProfileLight(unknown, "X", ""));

        assertThat(renamed).isTrue();
    }

    @Test
    void getTimedRunCount_returns_zero_for_no_runs() throws InternalException {
        UUID player = UUID.randomUUID();
        db.updatePlayerInfo(new PlayerProfileLight(player, "P", ""));
        db.createHunt("huntZero", "Zero", "ACTIVE");

        int count = db.getTimedRunCount(player, "huntZero");

        assertThat(count).isEqualTo(0);
    }

    @Test
    void getTimedLeaderboard_empty_returns_empty() throws InternalException {
        db.createHunt("huntEmpty", "Empty", "ACTIVE");

        var lb = db.getTimedLeaderboard("huntEmpty", 10);

        assertThat(lb).isEmpty();
    }

    @Test
    void updatePlayerInfo_updates_display_name() throws InternalException {
        UUID player = UUID.randomUUID();
        db.updatePlayerInfo(new PlayerProfileLight(player, "Name", "Display1"));

        db.updatePlayerInfo(new PlayerProfileLight(player, "Name", "Display2"));

        boolean renamed = db.hasPlayerRenamed(new PlayerProfileLight(player, "Name", "Display2"));
        assertThat(renamed).isFalse();
    }

    @Test
    void hasPlayerRenamed_detects_display_name_change() throws InternalException {
        UUID player = UUID.randomUUID();
        db.updatePlayerInfo(new PlayerProfileLight(player, "Same", "OldDisplay"));

        boolean renamed = db.hasPlayerRenamed(new PlayerProfileLight(player, "Same", "NewDisplay"));

        assertThat(renamed).isTrue();
    }
}
