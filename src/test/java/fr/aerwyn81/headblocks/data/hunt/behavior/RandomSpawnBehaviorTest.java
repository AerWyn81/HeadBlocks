package fr.aerwyn81.headblocks.data.hunt.behavior;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.data.hunt.behavior.RandomSpawnBehavior.BlockFilter;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.*;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementMode;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementSet;
import fr.aerwyn81.headblocks.data.hunt.requirement.area.CuboidAreaProvider;
import fr.aerwyn81.headblocks.data.hunt.requirement.types.AreaRequirement;
import fr.aerwyn81.headblocks.services.ConfigService;
import fr.aerwyn81.headblocks.services.LanguageService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RandomSpawnBehaviorTest {

    @Mock
    ServiceRegistry registry;

    @Mock
    ConfigService configService;

    @Mock
    LanguageService languageService;

    @Mock
    World world;

    private MockedStatic<Bukkit> bukkit;
    private HBHunt hunt;
    private IntFunction<Material> column = y -> y <= 63 ? (y == 63 ? Material.GRASS_BLOCK : Material.STONE) : Material.AIR;
    private final Map<String, Block> blocks = new HashMap<>();

    @BeforeEach
    void setUp() {
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);
        lenient().when(world.getName()).thenReturn("world");
        lenient().when(world.getMinHeight()).thenReturn(-64);
        lenient().when(world.getMaxHeight()).thenReturn(320);
        lenient().when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        lenient().when(world.getHighestBlockYAt(anyInt(), anyInt())).thenAnswer(invocation -> {
            for (int y = 319; y > -64; y--) {
                if (column.apply(y) != Material.AIR) {
                    return y;
                }
            }
            return -64;
        });
        lenient().when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(invocation ->
                block(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)));

        hunt = new HBHunt(configService, "random", "Random", HuntState.ACTIVE, 1, "D");
        useArea(new CuboidAreaProvider("world", 0, 50, 0, 31, 80, 31));
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    private Block block(int x, int y, int z) {
        return blocks.computeIfAbsent(x + ":" + y + ":" + z, key -> {
            var type = column.apply(y);
            var block = mock(Block.class);
            lenient().when(block.getType()).thenReturn(type);
            lenient().when(block.isEmpty()).thenReturn(type == Material.AIR);
            lenient().when(block.isLiquid()).thenReturn(type == Material.WATER);
            lenient().when(block.getLocation()).thenReturn(new Location(world, x, y, z));
            lenient().when(block.getRelative(0, -1, 0)).thenAnswer(invocation -> block(x, y - 1, z));
            return block;
        });
    }

    private void useArea(CuboidAreaProvider area) {
        hunt.setRequirements(new RequirementSet(registry, RequirementMode.ALL,
                List.of(new AreaRequirement(registry, area, null, false, false, null))));
    }

    private RandomSpawnBehavior behavior(boolean surface, BlockFilter filter, List<Material> list) {
        return new RandomSpawnBehavior(registry, surface, 20, filter, list, 3, 10, -1, SpawnCompletion.PER_PLAYER,
                AfterGoal.DENY, RespawnPolicy.DEFAULT, SpawnOptions.DEFAULT,
                List.of(new SpawnTemplate("basic", "", 1, HeadContent.head("tex"), List.of())));
    }

    private Location pick(RandomSpawnBehavior behavior) {
        var column = behavior.pickColumn(hunt);
        return column == null ? null : behavior.pickInChunk(hunt, column, location -> true);
    }

    @Test
    void pickColumn_staysInsideTheAreaBounds() {
        for (int i = 0; i < 50; i++) {
            var location = behavior(true, BlockFilter.BLACKLIST, List.of()).pickColumn(hunt);

            assertThat(location.getBlockX()).isBetween(0, 31);
            assertThat(location.getBlockZ()).isBetween(0, 31);
            assertThat(location.getWorld()).isSameAs(world);
        }
    }

    @Test
    void pickColumn_noLoadedChunk_findsNothing() {
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(false);

        assertThat(behavior(true, BlockFilter.BLACKLIST, List.of()).pickColumn(hunt)).isNull();
    }

    @Test
    void pickColumn_withoutArea_findsNothing() {
        hunt.setRequirements(new RequirementSet(registry));

        assertThat(behavior(true, BlockFilter.BLACKLIST, List.of()).pickColumn(hunt)).isNull();
        assertThat(RandomSpawnBehavior.areaOf(hunt)).isNull();
    }

    @Test
    void pickColumn_unloadedWorld_findsNothing() {
        useArea(new CuboidAreaProvider("nether", 0, 50, 0, 31, 80, 31));

        assertThat(behavior(true, BlockFilter.BLACKLIST, List.of()).pickColumn(hunt)).isNull();
    }

    @Test
    void surface_placesTheHeadRightAboveTheGround() {
        for (int i = 0; i < 20; i++) {
            var location = pick(behavior(true, BlockFilter.BLACKLIST, List.of()));

            assertThat(location.getBlockY()).isEqualTo(64);
        }
    }

    @Test
    void surface_aboveTheArea_findsNothing() {
        useArea(new CuboidAreaProvider("world", 0, 0, 0, 31, 40, 31));

        assertThat(pick(behavior(true, BlockFilter.BLACKLIST, List.of()))).isNull();
    }

    @Test
    void underground_findsTheFirstRoomAboveARandomHeight() {
        column = y -> y == 62 || y == 63 || y > 70 ? Material.AIR : Material.STONE;

        for (int i = 0; i < 30; i++) {
            var location = pick(behavior(false, BlockFilter.BLACKLIST, List.of()));

            assertThat(location.getBlockY()).isIn(62, 71);
        }
    }

    @Test
    void underground_areaOutsideTheWorldHeight_findsNothing() {
        useArea(new CuboidAreaProvider("world", 0, 400, 0, 31, 500, 31));

        assertThat(pick(behavior(false, BlockFilter.BLACKLIST, List.of()))).isNull();
    }

    @Test
    void blacklistedGround_isRefused() {
        assertThat(pick(behavior(true, BlockFilter.BLACKLIST, List.of(Material.GRASS_BLOCK)))).isNull();
    }

    @Test
    void whitelist_onlyAcceptsTheListedGround() {
        assertThat(pick(behavior(true, BlockFilter.WHITELIST, List.of(Material.GRASS_BLOCK)))).isNotNull();
        assertThat(pick(behavior(true, BlockFilter.WHITELIST, List.of(Material.SAND)))).isNull();
    }

    @Test
    void water_isNotAGround() {
        column = y -> y <= 63 ? Material.WATER : Material.AIR;

        assertThat(pick(behavior(true, BlockFilter.BLACKLIST, List.of()))).isNull();
    }

    @Test
    void occupiedSpots_areSkipped() {
        var behavior = behavior(true, BlockFilter.BLACKLIST, List.of());
        var column = behavior.pickColumn(hunt);

        assertThat(behavior.pickInChunk(hunt, column, location -> false)).isNull();
    }

    @Test
    void pickInChunk_staysInTheChunkOfTheColumn() {
        var behavior = behavior(true, BlockFilter.BLACKLIST, List.of());

        for (int i = 0; i < 20; i++) {
            var location = behavior.pickInChunk(hunt, new Location(world, 20, 50, 5), spot -> true);

            assertThat(location.getBlockX()).isBetween(16, 31);
            assertThat(location.getBlockZ()).isBetween(0, 15);
        }
    }

    @Test
    void randomYaw_isAQuarterTurn() {
        var behavior = behavior(true, BlockFilter.BLACKLIST, List.of());

        for (int i = 0; i < 20; i++) {
            assertThat(behavior.randomYaw()).isIn(0f, 90f, 180f, 270f);
        }
    }

    @Test
    void saveTo_thenFromConfig_keepsEverything() {
        var original = behavior(false, BlockFilter.WHITELIST, List.of(Material.STONE, Material.SAND));
        var yaml = new YamlConfiguration();
        original.saveTo(yaml);

        var loaded = RandomSpawnBehavior.fromConfig(registry, yaml);

        assertThat(loaded.surface()).isFalse();
        assertThat(loaded.maxTries()).isEqualTo(20);
        assertThat(loaded.filter()).isEqualTo(BlockFilter.WHITELIST);
        assertThat(loaded.blocks()).containsExactlyInAnyOrder(Material.STONE, Material.SAND);
        assertThat(loaded.active()).isEqualTo(3);
        assertThat(loaded.goal()).isEqualTo(10);
        assertThat(loaded.template("basic")).isNotNull();
    }

    @Test
    void fromConfig_nullSection_usesTheDefaults() {
        var loaded = RandomSpawnBehavior.fromConfig(registry, null);

        assertThat(loaded.surface()).isTrue();
        assertThat(loaded.maxTries()).isEqualTo(20);
        assertThat(loaded.filter()).isEqualTo(BlockFilter.BLACKLIST);
        assertThat(loaded.blocks()).isEmpty();
    }

    @Test
    void parseBlocks_ignoresUnknownNamesAndItems() {
        assertThat(RandomSpawnBehavior.parseBlocks(List.of("sand", " STONE ", "nope", "DIAMOND")))
                .containsExactly(Material.SAND, Material.STONE);
    }

    @Test
    void blockFilter_defaultsToBlacklist() {
        assertThat(BlockFilter.of("whitelist")).isEqualTo(BlockFilter.WHITELIST);
        assertThat(BlockFilter.of(null)).isEqualTo(BlockFilter.BLACKLIST);
        assertThat(BlockFilter.of("other")).isEqualTo(BlockFilter.BLACKLIST);
    }

    @Test
    void maxTries_isAtLeastOne() {
        var behavior = new RandomSpawnBehavior(registry, true, 0, BlockFilter.BLACKLIST, List.of(), 1, 1, -1,
                SpawnCompletion.PER_PLAYER, AfterGoal.DENY, RespawnPolicy.DEFAULT, SpawnOptions.DEFAULT, List.of());

        assertThat(behavior.maxTries()).isEqualTo(1);
    }

    @Test
    void idAndDisplayInfo() {
        when(registry.getLanguageService()).thenReturn(languageService);
        when(languageService.message("Hunt.Behavior.RandomSpawn")).thenReturn("Random spawn");
        var behavior = behavior(true, BlockFilter.BLACKLIST, List.of());

        assertThat(behavior.getId()).isEqualTo("random_spawn");
        assertThat(behavior.getDisplayInfo(null, hunt)).isEqualTo("Random spawn");
    }

    @Test
    void cuboidBounds_areOrdered() {
        assertThat(new CuboidAreaProvider("world", 10, 80, -5, 0, 60, 5).getBounds())
                .containsExactly(0, 60, -5, 10, 80, 5);
    }
}
