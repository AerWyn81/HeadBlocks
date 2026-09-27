package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.data.hunt.behavior.FixedPositionBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.FreeBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.*;
import fr.aerwyn81.headblocks.services.*;
import fr.aerwyn81.headblocks.services.gui.types.SpawnConfigGui;
import fr.aerwyn81.headblocks.utils.bukkit.ParticlesUtils;
import fr.aerwyn81.headblocks.utils.scheduler.SchedulerAdapter;
import fr.aerwyn81.headblocks.utils.scheduler.Task;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpawnCommandTest {

    @Mock
    ServiceRegistry registry;

    @Mock
    HuntService huntService;

    @Mock
    HuntConfigService huntConfigService;

    @Mock
    StorageService storageService;

    @Mock
    SpawnService spawnService;

    @Mock
    AreaEnforcementService areaEnforcementService;

    @Mock
    LanguageService languageService;

    @Mock
    ConfigService configService;

    @Mock
    Player player;

    @Mock
    World world;

    private HBHunt hunt;
    private Spawn command;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getHuntService()).thenReturn(huntService);
        lenient().when(registry.getHuntConfigService()).thenReturn(huntConfigService);
        lenient().when(registry.getStorageService()).thenReturn(storageService);
        lenient().when(registry.getSpawnService()).thenReturn(spawnService);
        lenient().when(registry.getAreaEnforcementService()).thenReturn(areaEnforcementService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(languageService.message(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(world.getName()).thenReturn("world");

        hunt = new HBHunt(configService, "spawnhunt", "Spawn", HuntState.ACTIVE, 1, "D");
        lenient().when(huntService.getHuntById("spawnhunt")).thenReturn(hunt);
        command = new Spawn(registry);
    }

    private void useFixed(SpawnPoint... points) {
        hunt.setBehaviors(List.of(new FreeBehavior(), new FixedPositionBehavior(registry, List.of(points), 1, 1, -1,
                SpawnCompletion.PER_PLAYER, AfterGoal.DENY, RespawnPolicy.DEFAULT,
                List.of(new SpawnTemplate("basic", "", 1, HeadContent.head("tex"), List.of())))));
    }

    private FixedPositionBehavior fixed() {
        return (FixedPositionBehavior) hunt.getBehaviors().get(1);
    }

    private void lookAt(int x, int y, int z) {
        Block target = mock(Block.class);
        Block above = mock(Block.class);
        when(player.getTargetBlockExact(anyInt())).thenReturn(target);
        when(target.isEmpty()).thenReturn(false);
        when(target.getRelative(BlockFace.UP)).thenReturn(above);
        when(above.getLocation()).thenReturn(new Location(world, x, y + 1, z));
        lenient().when(player.getLocation()).thenReturn(new Location(world, 0, 0, 0, 90f, 0f));
    }

    @Test
    void notAFixedPositionHunt_isRefused() {
        command.perform(player, new String[]{"spawn", "spawnhunt", "reroll"});

        verify(player).sendMessage("Messages.SpawnNotFixedPosition");
        verifyNoInteractions(spawnService);
    }

    @Test
    void unknownHunt_isRefused() {
        command.perform(player, new String[]{"spawn", "nope", "reroll"});

        verify(player).sendMessage("Messages.HuntNotFound");
    }

    @Test
    void pointAdd_addsTheBlockAboveTheTarget_andRefreshesTheEngine() {
        useFixed();
        lookAt(10, 63, -4);

        command.perform(player, new String[]{"spawn", "spawnhunt", "point", "add"});

        assertThat(fixed().points()).containsExactly(new SpawnPoint("world", 10, 64, -4, 270f));
        verify(huntConfigService).saveHunt(hunt);
        verify(storageService).incrementHuntVersion();
        verify(spawnService).refresh(hunt);
        verify(player).sendMessage("Messages.SpawnPointAdded");
    }

    @Test
    void pointAdd_duplicate_isRefused() {
        useFixed(new SpawnPoint("world", 10, 64, -4, 0f));
        lookAt(10, 63, -4);

        command.perform(player, new String[]{"spawn", "spawnhunt", "point", "add"});

        assertThat(fixed().points()).hasSize(1);
        verify(player).sendMessage("Messages.SpawnPointExists");
        verify(huntConfigService, never()).saveHunt(any());
    }

    @Test
    void pointAdd_outsideTheArea_isRefused() {
        useFixed();
        lookAt(10, 63, -4);
        when(areaEnforcementService.isLocationOutsideArea(eq(hunt), any())).thenReturn(true);

        command.perform(player, new String[]{"spawn", "spawnhunt", "point", "add"});

        assertThat(fixed().points()).isEmpty();
        verify(player).sendMessage("Messages.AreaHeadOutside");
    }

    @Test
    void pointRemove_byIndex() {
        useFixed(new SpawnPoint("world", 1, 64, 1, 0f), new SpawnPoint("world", 2, 64, 2, 0f));
        CommandSender console = mock(CommandSender.class);

        command.perform(console, new String[]{"spawn", "spawnhunt", "point", "remove", "1"});

        assertThat(fixed().points()).containsExactly(new SpawnPoint("world", 2, 64, 2, 0f));
        verify(console).sendMessage("Messages.SpawnPointRemoved");
    }

    @Test
    void pointRemove_badIndex_isRefused() {
        useFixed(new SpawnPoint("world", 1, 64, 1, 0f));
        CommandSender console = mock(CommandSender.class);

        command.perform(console, new String[]{"spawn", "spawnhunt", "point", "remove", "5"});

        assertThat(fixed().points()).hasSize(1);
        verify(console).sendMessage("Messages.SpawnPointNotFound");
    }

    @Test
    void reroll_withReset_delegatesToTheEngine() {
        useFixed();

        command.perform(player, new String[]{"spawn", "spawnhunt", "reroll", "reset"});

        verify(spawnService).reroll(hunt, true);
    }

    @Test
    void clear_delegatesToTheEngine() {
        useFixed();

        command.perform(player, new String[]{"spawn", "spawnhunt", "clear"});

        verify(spawnService).clear(hunt);
    }

    @Test
    void tabComplete_listsOnlyFixedPositionHunts() {
        useFixed();
        HBHunt other = new HBHunt(configService, "other", "Other", HuntState.ACTIVE, 1, "D");
        when(huntService.getAllHunts()).thenReturn(List.of(hunt, other));

        assertThat(command.tabComplete(player, new String[]{"spawn", ""})).containsExactly("spawnhunt");
        assertThat(command.tabComplete(player, new String[]{"spawn", "spawnhunt", "po"})).containsExactly("point");
        assertThat(command.tabComplete(player, new String[]{"spawn", "spawnhunt", "point", ""}))
                .containsExactly("add", "remove", "list", "show");
    }

    @Test
    void missingArguments_showsUsage() {
        command.perform(player, new String[]{"spawn", "spawnhunt"});

        verify(player).sendMessage("Messages.SpawnUsage");
    }

    @Test
    void unknownSubcommands_showUsage() {
        useFixed();

        command.perform(player, new String[]{"spawn", "spawnhunt", "dance"});
        command.perform(player, new String[]{"spawn", "spawnhunt", "point"});
        command.perform(player, new String[]{"spawn", "spawnhunt", "point", "dance"});

        verify(player, times(3)).sendMessage("Messages.SpawnUsage");
    }

    @Test
    void reroll_withoutReset() {
        useFixed();

        command.perform(player, new String[]{"spawn", "spawnhunt", "reroll"});

        verify(spawnService).reroll(hunt, false);
        verify(player).sendMessage("Messages.SpawnRerolled");
    }

    @Test
    void pointAdd_fromConsole_isRefused() {
        useFixed();
        CommandSender console = mock(CommandSender.class);

        command.perform(console, new String[]{"spawn", "spawnhunt", "point", "add"});

        verify(console).sendMessage("Messages.PlayerOnly");
        assertThat(fixed().points()).isEmpty();
    }

    @Test
    void pointAdd_lookingAtNothing_isRefused() {
        useFixed();
        when(player.getTargetBlockExact(anyInt())).thenReturn(null);

        command.perform(player, new String[]{"spawn", "spawnhunt", "point", "add"});

        verify(player).sendMessage("Messages.SpawnNoTarget");
    }

    @Test
    void pointRemove_byLookingAtIt() {
        useFixed(new SpawnPoint("world", 10, 64, -4, 0f), new SpawnPoint("world", 1, 64, 1, 0f));
        lookAt(10, 63, -4);

        command.perform(player, new String[]{"spawn", "spawnhunt", "point", "remove"});

        assertThat(fixed().points()).containsExactly(new SpawnPoint("world", 1, 64, 1, 0f));
        verify(player).sendMessage("Messages.SpawnPointRemoved");
    }

    @Test
    void pointRemove_notANumber_isRefused() {
        useFixed(new SpawnPoint("world", 1, 64, 1, 0f));

        command.perform(player, new String[]{"spawn", "spawnhunt", "point", "remove", "abc"});

        assertThat(fixed().points()).hasSize(1);
        verify(player).sendMessage("Messages.SpawnPointNotFound");
    }

    @Test
    void pointList_listsEveryPoint() {
        useFixed(new SpawnPoint("world", 1, 64, 1, 0f), new SpawnPoint("world", 2, 65, 3, 0f));

        command.perform(player, new String[]{"spawn", "spawnhunt", "point", "list"});

        verify(player).sendMessage("Messages.SpawnPointListHeader");
        verify(player, times(2)).sendMessage("Messages.SpawnPointListLine");
    }

    @Test
    void pointShow_highlightsThePointsOfTheWorldThenStops() {
        useFixed(new SpawnPoint("world", 1, 64, 1, 0f), new SpawnPoint("other", 2, 64, 2, 0f));
        SchedulerAdapter scheduler = mock(SchedulerAdapter.class);
        when(registry.getScheduler()).thenReturn(scheduler);
        Task task = mock(Task.class);
        List<Runnable> timers = new ArrayList<>();
        when(scheduler.runTaskTimer(any(Location.class), any(Runnable.class), anyLong(), anyLong())).thenAnswer(invocation -> {
            timers.add(invocation.getArgument(1));
            return task;
        });
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        when(player.isOnline()).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<ParticlesUtils> particles = mockStatic(ParticlesUtils.class)) {
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);
            bukkit.when(() -> Bukkit.getWorld("other")).thenReturn(mock(World.class));

            command.perform(player, new String[]{"spawn", "spawnhunt", "point", "show"});
            for (int i = 0; i < 16; i++) {
                timers.get(0).run();
            }

            particles.verify(() -> ParticlesUtils.spawn(any(), any(), eq(10), isNull(), eq(player)), times(15));
            verify(task).cancel();
            verify(player).sendMessage("Messages.SpawnPointsShown");
        }
    }

    @Test
    void pointShow_fromConsole_isRefused() {
        useFixed();
        CommandSender console = mock(CommandSender.class);

        command.perform(console, new String[]{"spawn", "spawnhunt", "point", "show"});

        verify(console).sendMessage("Messages.PlayerOnly");
    }

    @Test
    void config_opensTheGuiWithTheCurrentSettings() {
        useFixed(new SpawnPoint("world", 1, 64, 1, 0f));
        GuiService guiService = mock(GuiService.class);
        SpawnConfigGui spawnConfigGui = mock(SpawnConfigGui.class);
        when(registry.getGuiService()).thenReturn(guiService);
        when(guiService.getSpawnConfigGui()).thenReturn(spawnConfigGui);

        command.perform(player, new String[]{"spawn", "spawnhunt", "config"});

        verify(spawnConfigGui).open(eq(player), argThat(draft -> draft.points.size() == 1 && draft.templates.containsKey("basic")),
                any(), any());
    }

    @Test
    void config_fromConsole_isRefused() {
        useFixed();
        CommandSender console = mock(CommandSender.class);

        command.perform(console, new String[]{"spawn", "spawnhunt", "config"});

        verify(console).sendMessage("Messages.PlayerOnly");
    }

    @Test
    void applyConfig_replacesTheBehaviorKeepingThePoints() {
        useFixed(new SpawnPoint("world", 1, 64, 1, 0f));
        var draft = SpawnDraft.of(fixed());
        draft.points = List.of();
        draft.active = 4;
        draft.goal = 9;

        command.applyConfig(player, hunt, draft);

        assertThat(fixed().active()).isEqualTo(4);
        assertThat(fixed().goal()).isEqualTo(9);
        assertThat(fixed().points()).containsExactly(new SpawnPoint("world", 1, 64, 1, 0f));
        verify(huntConfigService).saveHunt(hunt);
        verify(spawnService).reconfigure(hunt);
        verify(player).sendMessage("Messages.SpawnConfigSaved");
    }

    @Test
    void applyConfig_behaviorRemovedMeanwhile_doesNothing() {
        hunt.setBehaviors(List.of(new FreeBehavior()));

        command.applyConfig(player, hunt, new SpawnDraft());

        verifyNoInteractions(huntConfigService, spawnService);
    }

    @Test
    void tabComplete_rerollSuggestsReset() {
        assertThat(command.tabComplete(player, new String[]{"spawn", "spawnhunt", "reroll", ""})).containsExactly("reset");
        assertThat(command.tabComplete(player, new String[]{"spawn", "spawnhunt", "clear", ""})).isEmpty();
        assertThat(command.tabComplete(player, new String[]{"spawn", "spawnhunt", "point", "add", ""})).isEmpty();
    }
}
