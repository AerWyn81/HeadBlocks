package fr.aerwyn81.headblocks.data.hunt.behavior;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.*;
import fr.aerwyn81.headblocks.data.reward.Reward;
import fr.aerwyn81.headblocks.data.reward.RewardType;
import fr.aerwyn81.headblocks.services.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpawnPointsBehaviorTest {

    @Mock
    ServiceRegistry registry;

    @Mock
    HeadService headService;

    @Mock
    SpawnService spawnService;

    @Mock
    StorageService storageService;

    @Mock
    LanguageService languageService;

    @Mock
    ConfigService configService;

    @Mock
    Player player;

    @Mock
    HeadLocation head;

    private final UUID playerUuid = UUID.randomUUID();
    private final UUID headUuid = UUID.randomUUID();
    private HBHunt hunt;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getHeadService()).thenReturn(headService);
        lenient().when(registry.getSpawnService()).thenReturn(spawnService);
        lenient().when(registry.getStorageService()).thenReturn(storageService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(languageService.message(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(player.getUniqueId()).thenReturn(playerUuid);
        lenient().when(head.getUuid()).thenReturn(headUuid);
        hunt = new HBHunt(configService, "spawnhunt", "Spawn Hunt", HuntState.ACTIVE, 1, "D");
    }

    private SpawnPointsBehavior behavior(int goal, SpawnCompletion completion, AfterGoal afterGoal, List<SpawnPoint> points) {
        return new SpawnPointsBehavior(registry, points, 2, goal, -1, completion, afterGoal, RespawnPolicy.DEFAULT,
                List.of(new SpawnTemplate("basic", "", 1, HeadContent.head("tex"), List.of())));
    }

    private SpawnPointsBehavior behavior(int goal, AfterGoal afterGoal) {
        return behavior(goal, SpawnCompletion.PER_PLAYER, afterGoal, List.of());
    }

    private void playerFound(int count) throws Exception {
        var found = new ArrayList<UUID>();
        for (int i = 0; i < count; i++) {
            found.add(UUID.randomUUID());
        }
        when(storageService.getHeadsPlayerForHunt(playerUuid, "spawnhunt")).thenReturn(found);
    }

    @Test
    void targetCount_isTheGoal() {
        assertThat(behavior(7, AfterGoal.DENY).targetCount()).hasValue(7);
    }

    @Test
    void canPlayerClick_notASpawnedHead_allows() {
        when(headService.isSpawned(headUuid)).thenReturn(false);

        assertThat(behavior(1, AfterGoal.DENY).canPlayerClick(player, head, hunt).allowed()).isTrue();
    }

    @Test
    void canPlayerClick_goalReached_deny_denies() throws Exception {
        when(headService.isSpawned(headUuid)).thenReturn(true);
        playerFound(3);

        BehaviorResult result = behavior(3, AfterGoal.DENY).canPlayerClick(player, head, hunt);

        assertThat(result.allowed()).isFalse();
        assertThat(result.denyMessage()).isEqualTo("Messages.SpawnGoalReached");
    }

    @Test
    void canPlayerClick_goalNotReached_allows() throws Exception {
        when(headService.isSpawned(headUuid)).thenReturn(true);
        playerFound(2);

        assertThat(behavior(3, AfterGoal.DENY).canPlayerClick(player, head, hunt).allowed()).isTrue();
    }

    @Test
    void canPlayerClick_goalReached_continue_allows() {
        when(headService.isSpawned(headUuid)).thenReturn(true);

        assertThat(behavior(3, AfterGoal.CONTINUE).canPlayerClick(player, head, hunt).allowed()).isTrue();
        verifyNoInteractions(storageService);
    }

    @Test
    void tryCommit_notASpawnedHead_allowsWithoutConsuming() {
        when(headService.isSpawned(headUuid)).thenReturn(false);

        assertThat(behavior(1, AfterGoal.DENY).tryCommit(player, head, hunt).allowed()).isTrue();
        verify(spawnService, never()).consume(any(), any());
    }

    @Test
    void tryCommit_consumed_allows() {
        when(headService.isSpawned(headUuid)).thenReturn(true);
        when(spawnService.consume(hunt, head)).thenReturn(true);

        assertThat(behavior(1, AfterGoal.DENY).tryCommit(player, head, hunt).allowed()).isTrue();
    }

    @Test
    void tryCommit_alreadyTaken_denies() {
        when(headService.isSpawned(headUuid)).thenReturn(true);
        when(spawnService.consume(hunt, head)).thenReturn(false);

        BehaviorResult result = behavior(1, AfterGoal.DENY).tryCommit(player, head, hunt);

        assertThat(result.allowed()).isFalse();
        assertThat(result.denyMessage()).isEqualTo("Messages.SpawnHeadTaken");
    }

    @Test
    void onHeadFound_firstWins_goalReached_winsTheHunt() throws Exception {
        playerFound(3);

        behavior(3, SpawnCompletion.FIRST_WINS, AfterGoal.DENY, List.of()).onHeadFound(player, head, hunt);

        verify(spawnService).win(hunt, player);
    }

    @Test
    void onHeadFound_firstWins_goalNotReached_doesNothing() throws Exception {
        playerFound(2);

        behavior(3, SpawnCompletion.FIRST_WINS, AfterGoal.DENY, List.of()).onHeadFound(player, head, hunt);

        verify(spawnService, never()).win(any(), any());
    }

    @Test
    void onHeadFound_perPlayer_neverWins() {
        behavior(1, SpawnCompletion.PER_PLAYER, AfterGoal.DENY, List.of()).onHeadFound(player, head, hunt);

        verify(spawnService, never()).win(any(), any());
    }

    @Test
    void pickLocation_skipsTakenPoints() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        var points = List.of(new SpawnPoint("world", 1, 64, 1, 0f), new SpawnPoint("world", 2, 64, 2, 90f));
        var fixed = behavior(1, SpawnCompletion.PER_PLAYER, AfterGoal.DENY, points);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);
            List<Location> picked = new ArrayList<>();

            fixed.pickLocation(location -> location.getBlockX() == 2, picked::add);

            assertThat(picked).hasSize(1);
            assertThat(picked.get(0).getBlockX()).isEqualTo(2);
            assertThat(picked.get(0).getX()).isEqualTo(2.5);
            assertThat(fixed.yawAt(picked.get(0))).isEqualTo(90f);
        }
    }

    @Test
    void pickLocation_noFreePoint_returnsNull() {
        var points = List.of(new SpawnPoint("world", 1, 64, 1, 0f));
        var fixed = behavior(1, SpawnCompletion.PER_PLAYER, AfterGoal.DENY, points);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(mock(World.class));
            List<Location> picked = new ArrayList<>();

            fixed.pickLocation(location -> false, picked::add);

            assertThat(picked).containsExactly((Location) null);
        }
    }

    @Test
    void pickTemplate_ignoresZeroWeights() {
        var fixed = new SpawnPointsBehavior(registry, List.of(), 1, 1, -1, SpawnCompletion.PER_PLAYER, AfterGoal.DENY,
                RespawnPolicy.DEFAULT, List.of(
                new SpawnTemplate("never", "", 0, HeadContent.head("a"), List.of()),
                new SpawnTemplate("always", "", 5, HeadContent.head("b"), List.of())));

        for (int i = 0; i < 50; i++) {
            assertThat(fixed.pickTemplate().id()).isEqualTo("always");
        }
    }

    @Test
    void saveAndLoad_roundTripsTheWholeConfiguration() {
        var original = new SpawnPointsBehavior(registry,
                List.of(new SpawnPoint("world", 1, 64, -3, 45f), new SpawnPoint("nether", -10, 30, 7, 0f)),
                3, 12, 50, SpawnCompletion.FIRST_WINS, AfterGoal.CONTINUE,
                new RespawnPolicy(true, 5, 20, true, 600, true, false),
                List.of(new SpawnTemplate("gold", "Golden", 1, HeadContent.head("goldtex"),
                                List.of(new Reward(RewardType.MESSAGE, "gg"))),
                        new SpawnTemplate("basic", "", 10, HeadContent.head("basictex"), List.of())));

        var yaml = new YamlConfiguration();
        original.saveTo(yaml.createSection("behaviors.spawn_points"));
        var loaded = (SpawnPointsBehavior) Behavior.fromConfig("spawn_points", registry,
                yaml.getConfigurationSection("behaviors.spawn_points"));

        assertThat(loaded.points()).isEqualTo(original.points());
        assertThat(loaded.active()).isEqualTo(3);
        assertThat(loaded.goal()).isEqualTo(12);
        assertThat(loaded.maxTotalSpawns()).isEqualTo(50);
        assertThat(loaded.completion()).isEqualTo(SpawnCompletion.FIRST_WINS);
        assertThat(loaded.afterGoal()).isEqualTo(AfterGoal.CONTINUE);
        assertThat(loaded.respawn()).isEqualTo(original.respawn());
        assertThat(loaded.templates()).extracting(SpawnTemplate::id).containsExactly("gold", "basic");
        assertThat(loaded.template("gold").name()).isEqualTo("Golden");
        assertThat(loaded.template("gold").content()).isEqualTo(HeadContent.head("goldtex"));
        assertThat(loaded.template("gold").rewards()).containsExactly(new Reward(RewardType.MESSAGE, "gg"));
        assertThat(loaded.template("basic").weight()).isEqualTo(10);
    }

    @Test
    void fromConfig_templateWithoutContent_isIgnored() {
        var yaml = new YamlConfiguration();
        yaml.set("templates.broken.weight", 3);
        yaml.set("templates.ok.weight", 1);
        HeadContent.head("tex").save(yaml.getConfigurationSection("templates.ok"), "content");

        var loaded = SpawnPointsBehavior.fromConfig(registry, yaml);

        assertThat(loaded.templates()).extracting(SpawnTemplate::id).containsExactly("ok");
    }
}
