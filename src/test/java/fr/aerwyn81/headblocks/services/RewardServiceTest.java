package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.TieredReward;
import fr.aerwyn81.headblocks.data.hunt.HuntConfig;
import fr.aerwyn81.headblocks.utils.bukkit.CommandDispatcher;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.bukkit.SchedulerAdapter;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardServiceTest {

    @Mock
    private ConfigService configService;

    @Mock
    private PlaceholdersService placeholdersService;

    @Mock
    private SchedulerAdapter scheduler;

    @Mock
    private CommandDispatcher cmdDispatcher;

    @Mock
    private Player player;

    @Mock
    private HeadLocation headLocation;

    @Mock
    private Server server;

    private RewardService rewardService;

    @BeforeEach
    void setUp() {
        rewardService = new RewardService(configService, placeholdersService, scheduler, cmdDispatcher);

        lenient().when(player.getName()).thenReturn("Steve");
        lenient().when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        lenient().when(player.getServer()).thenReturn(server);
    }

    // --- Helpers ---

    /**
     * Stubs scheduler.runTaskLater to immediately execute the Runnable.
     */
    private void stubSchedulerRunTaskLater() {
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(scheduler).runTaskLater(any(Runnable.class), anyLong());
    }

    private List<UUID> playerHeadsOfSize(int size) {
        List<UUID> heads = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            heads.add(UUID.randomUUID());
        }
        return heads;
    }

    // =========================================================================
    // giveReward: click messages
    // =========================================================================

    @Test
    void giveReward_sends_click_messages_when_no_tiered_rewards() {
        when(configService.tieredRewards()).thenReturn(Collections.emptyList());
        List<String> messages = List.of("Found a head!");
        when(configService.headClickMessages()).thenReturn(messages);
        when(placeholdersService.parse(player, headLocation, messages)).thenReturn(new String[]{"Found a head!"});
        when(configService.headClickCommands()).thenReturn(Collections.emptyList());

        rewardService.giveReward(player, playerHeadsOfSize(1), headLocation);

        verify(player).sendMessage(new String[]{"Found a head!"});
    }

    @Test
    void giveReward_no_messages_no_commands_does_not_throw() {
        when(configService.tieredRewards()).thenReturn(Collections.emptyList());
        when(configService.headClickMessages()).thenReturn(Collections.emptyList());
        when(configService.headClickCommands()).thenReturn(Collections.emptyList());

        rewardService.giveReward(player, playerHeadsOfSize(1), headLocation);

        verify(player, never()).sendMessage(any(String[].class));
    }

    // =========================================================================
    // giveReward: tiered messages
    // =========================================================================

    @Test
    void giveReward_sends_tiered_messages_when_level_matches() {
        stubSchedulerRunTaskLater();

        TieredReward tier = new TieredReward(3, List.of("Tier 3!"), Collections.emptyList(), Collections.emptyList(), -1, false);
        when(configService.tieredRewards()).thenReturn(List.of(tier));
        when(placeholdersService.parse(player, headLocation, List.of("Tier 3!"))).thenReturn(new String[]{"Tier 3!"});
        when(configService.preventMessagesOnTieredRewardsLevel()).thenReturn(true);
        when(configService.preventCommandsOnTieredRewardsLevel()).thenReturn(true);

        rewardService.giveReward(player, playerHeadsOfSize(3), headLocation);

        verify(player).sendMessage(new String[]{"Tier 3!"});
    }

    @Test
    void giveReward_no_tiered_message_when_level_does_not_match() {
        when(configService.tieredRewards()).thenReturn(
                List.of(new TieredReward(5, List.of("Tier 5!"), Collections.emptyList(), Collections.emptyList(), -1, false)));
        when(configService.headClickMessages()).thenReturn(Collections.emptyList());
        when(configService.headClickCommands()).thenReturn(Collections.emptyList());

        rewardService.giveReward(player, playerHeadsOfSize(3), headLocation);

        verify(player, never()).sendMessage(any(String[].class));
    }

    // =========================================================================
    // giveReward: tiered commands dispatched
    // =========================================================================

    @Test
    void giveReward_dispatches_tiered_commands_sequentially() {
        stubSchedulerRunTaskLater();

        TieredReward tier = new TieredReward(2, Collections.emptyList(), List.of("cmd1", "cmd2"), Collections.emptyList(), -1, false);
        when(configService.tieredRewards()).thenReturn(List.of(tier));
        when(configService.preventMessagesOnTieredRewardsLevel()).thenReturn(true);
        when(configService.preventCommandsOnTieredRewardsLevel()).thenReturn(true);
        when(placeholdersService.parse(eq("Steve"), any(UUID.class), eq(headLocation), eq("cmd1"))).thenReturn("cmd1");
        when(placeholdersService.parse(eq("Steve"), any(UUID.class), eq(headLocation), eq("cmd2"))).thenReturn("cmd2");

        rewardService.giveReward(player, playerHeadsOfSize(2), headLocation);

        verify(cmdDispatcher).dispatchConsoleCommand("cmd1");
        verify(cmdDispatcher).dispatchConsoleCommand("cmd2");
    }

    @Test
    void giveReward_dispatches_tiered_random_command() {
        stubSchedulerRunTaskLater();

        TieredReward tier = new TieredReward(1, Collections.emptyList(), List.of("onlyCmd"), Collections.emptyList(), -1, true);
        when(configService.tieredRewards()).thenReturn(List.of(tier));
        when(configService.preventMessagesOnTieredRewardsLevel()).thenReturn(true);
        when(configService.preventCommandsOnTieredRewardsLevel()).thenReturn(true);
        when(placeholdersService.parse(eq("Steve"), any(UUID.class), eq(headLocation), eq("onlyCmd"))).thenReturn("onlyCmd");

        rewardService.giveReward(player, playerHeadsOfSize(1), headLocation);

        verify(cmdDispatcher).dispatchConsoleCommand("onlyCmd");
    }

    // =========================================================================
    // giveReward: broadcast messages
    // =========================================================================

    @Test
    void giveReward_sends_broadcast_on_tiered_reward() {
        stubSchedulerRunTaskLater();

        TieredReward tier = new TieredReward(1, Collections.emptyList(), Collections.emptyList(), List.of("Broadcast!"), -1, false);
        when(configService.tieredRewards()).thenReturn(List.of(tier));
        when(configService.preventMessagesOnTieredRewardsLevel()).thenReturn(true);
        when(configService.preventCommandsOnTieredRewardsLevel()).thenReturn(true);
        when(placeholdersService.parse(eq("Steve"), any(UUID.class), eq(headLocation), eq("Broadcast!"))).thenReturn("Broadcast!");

        rewardService.giveReward(player, playerHeadsOfSize(1), headLocation);

        verify(server).broadcastMessage("Broadcast!");
    }

    // =========================================================================
    // giveReward: prevention flags
    // =========================================================================

    @Test
    void giveReward_preventMessages_suppresses_click_messages_when_tiered_matches() {
        stubSchedulerRunTaskLater();

        TieredReward tier = new TieredReward(1, List.of("Tiered msg"), Collections.emptyList(), Collections.emptyList(), -1, false);
        when(configService.tieredRewards()).thenReturn(List.of(tier));
        when(configService.preventMessagesOnTieredRewardsLevel()).thenReturn(true);
        when(configService.preventCommandsOnTieredRewardsLevel()).thenReturn(true);
        when(placeholdersService.parse(player, headLocation, List.of("Tiered msg"))).thenReturn(new String[]{"Tiered msg"});

        rewardService.giveReward(player, playerHeadsOfSize(1), headLocation);

        // Only tiered messages sent, not click messages
        verify(player, times(1)).sendMessage(any(String[].class));
        verify(configService, never()).headClickMessages();
    }

    @Test
    void giveReward_preventMessages_false_sends_both_tiered_and_click_messages() {
        stubSchedulerRunTaskLater();

        TieredReward tier = new TieredReward(1, List.of("Tiered"), Collections.emptyList(), Collections.emptyList(), -1, false);
        when(configService.tieredRewards()).thenReturn(List.of(tier));
        when(configService.preventMessagesOnTieredRewardsLevel()).thenReturn(false);
        when(configService.preventCommandsOnTieredRewardsLevel()).thenReturn(true);
        when(placeholdersService.parse(player, headLocation, List.of("Tiered"))).thenReturn(new String[]{"Tiered"});

        List<String> clickMessages = List.of("Click msg");
        when(configService.headClickMessages()).thenReturn(clickMessages);
        when(placeholdersService.parse(player, headLocation, clickMessages)).thenReturn(new String[]{"Click msg"});

        rewardService.giveReward(player, playerHeadsOfSize(1), headLocation);

        verify(player, times(2)).sendMessage(any(String[].class));
    }

    @Test
    void giveReward_preventCommands_suppresses_click_commands_when_tiered_matches() {
        stubSchedulerRunTaskLater();

        TieredReward tier = new TieredReward(1, Collections.emptyList(), List.of("tieredCmd"), Collections.emptyList(), -1, false);
        when(configService.tieredRewards()).thenReturn(List.of(tier));
        when(configService.preventMessagesOnTieredRewardsLevel()).thenReturn(true);
        when(configService.preventCommandsOnTieredRewardsLevel()).thenReturn(true);
        when(placeholdersService.parse(eq("Steve"), any(UUID.class), eq(headLocation), eq("tieredCmd"))).thenReturn("tieredCmd");

        rewardService.giveReward(player, playerHeadsOfSize(1), headLocation);

        // Only tiered command dispatched, never click commands
        verify(cmdDispatcher, times(1)).dispatchConsoleCommand("tieredCmd");
        verify(configService, never()).headClickCommands();
    }

    // =========================================================================
    // giveReward: regular click commands
    // =========================================================================

    @Test
    void giveReward_dispatches_sequential_click_commands() {
        stubSchedulerRunTaskLater();

        when(configService.tieredRewards()).thenReturn(Collections.emptyList());
        when(configService.headClickMessages()).thenReturn(Collections.emptyList());
        when(configService.headClickCommandsRandomized()).thenReturn(false);
        when(configService.headClickCommands()).thenReturn(List.of("give %player% diamond", "eco give %player% 100"));
        when(placeholdersService.parse(eq("Steve"), any(UUID.class), eq(headLocation), eq("give %player% diamond")))
                .thenReturn("give Steve diamond");
        when(placeholdersService.parse(eq("Steve"), any(UUID.class), eq(headLocation), eq("eco give %player% 100")))
                .thenReturn("eco give Steve 100");

        rewardService.giveReward(player, playerHeadsOfSize(1), headLocation);

        verify(cmdDispatcher).dispatchConsoleCommand("give Steve diamond");
        verify(cmdDispatcher).dispatchConsoleCommand("eco give Steve 100");
    }

    @Test
    void giveReward_dispatches_random_click_command() {
        stubSchedulerRunTaskLater();

        when(configService.tieredRewards()).thenReturn(Collections.emptyList());
        when(configService.headClickMessages()).thenReturn(Collections.emptyList());
        when(configService.headClickCommandsRandomized()).thenReturn(true);
        when(configService.headClickCommands()).thenReturn(List.of("singleCmd"));
        when(placeholdersService.parse(eq("Steve"), any(UUID.class), eq(headLocation), eq("singleCmd")))
                .thenReturn("singleCmd");

        rewardService.giveReward(player, playerHeadsOfSize(1), headLocation);

        verify(cmdDispatcher).dispatchConsoleCommand("singleCmd");
    }

    @Test
    void giveReward_empty_click_commands_skips_dispatch() {
        when(configService.tieredRewards()).thenReturn(Collections.emptyList());
        when(configService.headClickMessages()).thenReturn(Collections.emptyList());
        when(configService.headClickCommands()).thenReturn(Collections.emptyList());

        rewardService.giveReward(player, playerHeadsOfSize(1), headLocation);

        verify(cmdDispatcher, never()).dispatchConsoleCommand(anyString());
    }

    @Test
    void giveReward_blank_parsed_command_is_not_dispatched() {
        stubSchedulerRunTaskLater();

        when(configService.tieredRewards()).thenReturn(Collections.emptyList());
        when(configService.headClickMessages()).thenReturn(Collections.emptyList());
        when(configService.headClickCommandsRandomized()).thenReturn(false);
        when(configService.headClickCommands()).thenReturn(List.of("blankCmd"));
        when(placeholdersService.parse(eq("Steve"), any(UUID.class), eq(headLocation), eq("blankCmd")))
                .thenReturn("   ");

        rewardService.giveReward(player, playerHeadsOfSize(1), headLocation);

        verify(cmdDispatcher, never()).dispatchConsoleCommand(anyString());
    }

    // =========================================================================
    // hasPlayerSlotsRequired: global slots
    // =========================================================================

    @Test
    void hasPlayerSlotsRequired_no_slots_required_returns_true() {
        when(configService.headClickCommandsSlotsRequired()).thenReturn(-1);
        when(configService.tieredRewards()).thenReturn(Collections.emptyList());

        try (MockedStatic<PlayerUtils> mocked = mockStatic(PlayerUtils.class)) {
            assertThat(rewardService.hasPlayerSlotsRequired(player, playerHeadsOfSize(1))).isTrue();
            mocked.verifyNoInteractions();
        }
    }

    @Test
    void hasPlayerSlotsRequired_insufficient_global_slots_returns_false() {
        when(configService.headClickCommandsSlotsRequired()).thenReturn(5);

        try (MockedStatic<PlayerUtils> mocked = mockStatic(PlayerUtils.class)) {
            mocked.when(() -> PlayerUtils.getEmptySlots(player)).thenReturn(3);

            assertThat(rewardService.hasPlayerSlotsRequired(player, playerHeadsOfSize(1))).isFalse();
        }
    }

    @Test
    void hasPlayerSlotsRequired_sufficient_global_slots_returns_true() {
        when(configService.headClickCommandsSlotsRequired()).thenReturn(5);
        when(configService.tieredRewards()).thenReturn(Collections.emptyList());

        try (MockedStatic<PlayerUtils> mocked = mockStatic(PlayerUtils.class)) {
            mocked.when(() -> PlayerUtils.getEmptySlots(player)).thenReturn(10);

            assertThat(rewardService.hasPlayerSlotsRequired(player, playerHeadsOfSize(1))).isTrue();
        }
    }

    // =========================================================================
    // hasPlayerSlotsRequired: tiered rewards
    // =========================================================================

    @Test
    void hasPlayerSlotsRequired_no_tiered_rewards_returns_true() {
        when(configService.headClickCommandsSlotsRequired()).thenReturn(-1);
        when(configService.tieredRewards()).thenReturn(Collections.emptyList());

        assertThat(rewardService.hasPlayerSlotsRequired(player, playerHeadsOfSize(1))).isTrue();
    }

    @Test
    void hasPlayerSlotsRequired_tiered_reward_no_slots_required_returns_true() {
        when(configService.headClickCommandsSlotsRequired()).thenReturn(-1);
        TieredReward tier = new TieredReward(2, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), -1, false);
        when(configService.tieredRewards()).thenReturn(List.of(tier));

        assertThat(rewardService.hasPlayerSlotsRequired(player, playerHeadsOfSize(2))).isTrue();
    }

    @Test
    void hasPlayerSlotsRequired_tiered_reward_insufficient_slots_returns_false() {
        when(configService.headClickCommandsSlotsRequired()).thenReturn(-1);
        TieredReward tier = new TieredReward(2, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), 4, false);
        when(configService.tieredRewards()).thenReturn(List.of(tier));

        try (MockedStatic<PlayerUtils> mocked = mockStatic(PlayerUtils.class)) {
            mocked.when(() -> PlayerUtils.getEmptySlots(player)).thenReturn(2);

            assertThat(rewardService.hasPlayerSlotsRequired(player, playerHeadsOfSize(2))).isFalse();
        }
    }

    @Test
    void hasPlayerSlotsRequired_tiered_reward_sufficient_slots_returns_true() {
        when(configService.headClickCommandsSlotsRequired()).thenReturn(-1);
        TieredReward tier = new TieredReward(2, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), 4, false);
        when(configService.tieredRewards()).thenReturn(List.of(tier));

        try (MockedStatic<PlayerUtils> mocked = mockStatic(PlayerUtils.class)) {
            mocked.when(() -> PlayerUtils.getEmptySlots(player)).thenReturn(5);

            assertThat(rewardService.hasPlayerSlotsRequired(player, playerHeadsOfSize(2))).isTrue();
        }
    }

    @Test
    void hasPlayerSlotsRequired_tiered_level_not_matching_returns_true() {
        when(configService.headClickCommandsSlotsRequired()).thenReturn(-1);
        TieredReward tier = new TieredReward(10, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), 4, false);
        when(configService.tieredRewards()).thenReturn(List.of(tier));

        assertThat(rewardService.hasPlayerSlotsRequired(player, playerHeadsOfSize(2))).isTrue();
    }

    // =========================================================================
    // giveReward with HuntConfig
    // =========================================================================

    @Test
    void giveRewardHunt_sends_click_messages_from_huntConfig() {
        HuntConfig huntConfig = new HuntConfig(configService);
        huntConfig.setHeadClickMessages(List.of("Hunt message!"));
        huntConfig.setTieredRewards(Collections.emptyList());
        huntConfig.setHeadClickCommands(Collections.emptyList());

        when(configService.preventMessagesOnTieredRewardsLevel()).thenReturn(false);
        when(placeholdersService.parse(player, headLocation, List.of("Hunt message!")))
                .thenReturn(new String[]{"Hunt message!"});

        rewardService.giveReward(player, playerHeadsOfSize(1), headLocation, huntConfig);

        verify(player).sendMessage(new String[]{"Hunt message!"});
    }

    @Test
    void giveRewardHunt_dispatches_hunt_commands_sequentially() {
        stubSchedulerRunTaskLater();

        HuntConfig huntConfig = new HuntConfig(configService);
        huntConfig.setTieredRewards(Collections.emptyList());
        huntConfig.setHeadClickMessages(Collections.emptyList());
        huntConfig.setHeadClickCommands(List.of("huntcmd1", "huntcmd2"));

        when(configService.preventMessagesOnTieredRewardsLevel()).thenReturn(false);
        when(configService.headClickCommandsRandomized()).thenReturn(false);
        when(placeholdersService.parse(eq("Steve"), any(UUID.class), eq(headLocation), eq("huntcmd1")))
                .thenReturn("huntcmd1");
        when(placeholdersService.parse(eq("Steve"), any(UUID.class), eq(headLocation), eq("huntcmd2")))
                .thenReturn("huntcmd2");

        rewardService.giveReward(player, playerHeadsOfSize(1), headLocation, huntConfig);

        verify(cmdDispatcher).dispatchConsoleCommand("huntcmd1");
        verify(cmdDispatcher).dispatchConsoleCommand("huntcmd2");
    }

    @Test
    void giveRewardHunt_dispatches_random_hunt_command() {
        stubSchedulerRunTaskLater();

        HuntConfig huntConfig = new HuntConfig(configService);
        huntConfig.setTieredRewards(Collections.emptyList());
        huntConfig.setHeadClickMessages(Collections.emptyList());
        huntConfig.setHeadClickCommands(List.of("onlyHuntCmd"));

        when(configService.preventMessagesOnTieredRewardsLevel()).thenReturn(false);
        when(configService.headClickCommandsRandomized()).thenReturn(true);
        when(placeholdersService.parse(eq("Steve"), any(UUID.class), eq(headLocation), eq("onlyHuntCmd")))
                .thenReturn("onlyHuntCmd");

        rewardService.giveReward(player, playerHeadsOfSize(1), headLocation, huntConfig);

        verify(cmdDispatcher).dispatchConsoleCommand("onlyHuntCmd");
    }

    @Test
    void giveRewardHunt_with_tiered_reward_sends_tiered_messages() {
        stubSchedulerRunTaskLater();

        HuntConfig huntConfig = new HuntConfig(configService);
        TieredReward tier = new TieredReward(2, List.of("Hunt tier msg"), Collections.emptyList(), Collections.emptyList(), -1, false);
        huntConfig.setTieredRewards(List.of(tier));

        when(placeholdersService.parse(player, headLocation, List.of("Hunt tier msg")))
                .thenReturn(new String[]{"Hunt tier msg"});
        when(configService.preventMessagesOnTieredRewardsLevel()).thenReturn(true);
        when(configService.preventCommandsOnTieredRewardsLevel()).thenReturn(true);

        rewardService.giveReward(player, playerHeadsOfSize(2), headLocation, huntConfig);

        verify(player).sendMessage(new String[]{"Hunt tier msg"});
    }

    @Test
    void giveRewardHunt_with_tiered_commands_dispatches() {
        stubSchedulerRunTaskLater();

        HuntConfig huntConfig = new HuntConfig(configService);
        TieredReward tier = new TieredReward(1, Collections.emptyList(), List.of("huntTierCmd"), Collections.emptyList(), -1, false);
        huntConfig.setTieredRewards(List.of(tier));

        when(configService.preventMessagesOnTieredRewardsLevel()).thenReturn(true);
        when(configService.preventCommandsOnTieredRewardsLevel()).thenReturn(true);
        when(placeholdersService.parse(eq("Steve"), any(UUID.class), eq(headLocation), eq("huntTierCmd")))
                .thenReturn("huntTierCmd");

        rewardService.giveReward(player, playerHeadsOfSize(1), headLocation, huntConfig);

        verify(cmdDispatcher).dispatchConsoleCommand("huntTierCmd");
    }

    @Test
    void giveRewardHunt_with_tiered_broadcast() {
        stubSchedulerRunTaskLater();

        HuntConfig huntConfig = new HuntConfig(configService);
        TieredReward tier = new TieredReward(1, Collections.emptyList(), Collections.emptyList(), List.of("Hunt broadcast!"), -1, false);
        huntConfig.setTieredRewards(List.of(tier));

        when(configService.preventMessagesOnTieredRewardsLevel()).thenReturn(true);
        when(configService.preventCommandsOnTieredRewardsLevel()).thenReturn(true);
        when(placeholdersService.parse(eq("Steve"), any(UUID.class), eq(headLocation), eq("Hunt broadcast!")))
                .thenReturn("Hunt broadcast!");

        rewardService.giveReward(player, playerHeadsOfSize(1), headLocation, huntConfig);

        verify(server).broadcastMessage("Hunt broadcast!");
    }

    @Test
    void giveRewardHunt_preventCommands_suppresses_huntConfig_commands() {
        stubSchedulerRunTaskLater();

        HuntConfig huntConfig = new HuntConfig(configService);
        TieredReward tier = new TieredReward(1, Collections.emptyList(), List.of("tierCmd"), Collections.emptyList(), -1, false);
        huntConfig.setTieredRewards(List.of(tier));
        huntConfig.setHeadClickCommands(List.of("huntCmd"));

        when(configService.preventMessagesOnTieredRewardsLevel()).thenReturn(true);
        when(configService.preventCommandsOnTieredRewardsLevel()).thenReturn(true);
        when(placeholdersService.parse(eq("Steve"), any(UUID.class), eq(headLocation), eq("tierCmd")))
                .thenReturn("tierCmd");

        rewardService.giveReward(player, playerHeadsOfSize(1), headLocation, huntConfig);

        // Only tiered command, NOT hunt click commands
        verify(cmdDispatcher, times(1)).dispatchConsoleCommand("tierCmd");
    }

    @Test
    void giveRewardHunt_empty_commands_skips() {
        HuntConfig huntConfig = new HuntConfig(configService);
        huntConfig.setTieredRewards(Collections.emptyList());
        huntConfig.setHeadClickMessages(Collections.emptyList());
        huntConfig.setHeadClickCommands(Collections.emptyList());

        when(configService.preventMessagesOnTieredRewardsLevel()).thenReturn(false);

        rewardService.giveReward(player, playerHeadsOfSize(1), headLocation, huntConfig);

        verify(cmdDispatcher, never()).dispatchConsoleCommand(anyString());
    }

    @Test
    void giveRewardHunt_tiered_random_commands() {
        stubSchedulerRunTaskLater();

        HuntConfig huntConfig = new HuntConfig(configService);
        TieredReward tier = new TieredReward(1, Collections.emptyList(), List.of("randomCmd"), Collections.emptyList(), -1, true);
        huntConfig.setTieredRewards(List.of(tier));

        when(configService.preventMessagesOnTieredRewardsLevel()).thenReturn(true);
        when(configService.preventCommandsOnTieredRewardsLevel()).thenReturn(true);
        when(placeholdersService.parse(eq("Steve"), any(UUID.class), eq(headLocation), eq("randomCmd")))
                .thenReturn("randomCmd");

        rewardService.giveReward(player, playerHeadsOfSize(1), headLocation, huntConfig);

        verify(cmdDispatcher).dispatchConsoleCommand("randomCmd");
    }

    // =========================================================================
    // hasPlayerSlotsRequired with HuntConfig
    // =========================================================================

    @Test
    void hasPlayerSlotsRequiredHunt_no_slots_required_returns_true() {
        HuntConfig huntConfig = new HuntConfig(configService);
        huntConfig.setTieredRewards(Collections.emptyList());

        when(configService.headClickCommandsSlotsRequired()).thenReturn(-1);

        assertThat(rewardService.hasPlayerSlotsRequired(player, playerHeadsOfSize(1), huntConfig)).isTrue();
    }

    @Test
    void hasPlayerSlotsRequiredHunt_insufficient_global_slots_returns_false() {
        HuntConfig huntConfig = new HuntConfig(configService);

        when(configService.headClickCommandsSlotsRequired()).thenReturn(5);

        try (MockedStatic<PlayerUtils> mocked = mockStatic(PlayerUtils.class)) {
            mocked.when(() -> PlayerUtils.getEmptySlots(player)).thenReturn(3);

            assertThat(rewardService.hasPlayerSlotsRequired(player, playerHeadsOfSize(1), huntConfig)).isFalse();
        }
    }

    @Test
    void hasPlayerSlotsRequiredHunt_tiered_sufficient_returns_true() {
        HuntConfig huntConfig = new HuntConfig(configService);
        TieredReward tier = new TieredReward(2, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), 3, false);
        huntConfig.setTieredRewards(List.of(tier));

        when(configService.headClickCommandsSlotsRequired()).thenReturn(-1);

        try (MockedStatic<PlayerUtils> mocked = mockStatic(PlayerUtils.class)) {
            mocked.when(() -> PlayerUtils.getEmptySlots(player)).thenReturn(5);

            assertThat(rewardService.hasPlayerSlotsRequired(player, playerHeadsOfSize(2), huntConfig)).isTrue();
        }
    }

    @Test
    void hasPlayerSlotsRequiredHunt_tiered_insufficient_returns_false() {
        HuntConfig huntConfig = new HuntConfig(configService);
        TieredReward tier = new TieredReward(2, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), 5, false);
        huntConfig.setTieredRewards(List.of(tier));

        when(configService.headClickCommandsSlotsRequired()).thenReturn(-1);

        try (MockedStatic<PlayerUtils> mocked = mockStatic(PlayerUtils.class)) {
            mocked.when(() -> PlayerUtils.getEmptySlots(player)).thenReturn(3);

            assertThat(rewardService.hasPlayerSlotsRequired(player, playerHeadsOfSize(2), huntConfig)).isFalse();
        }
    }

    @Test
    void hasPlayerSlotsRequiredHunt_no_tiered_rewards_returns_true() {
        HuntConfig huntConfig = new HuntConfig(configService);
        huntConfig.setTieredRewards(Collections.emptyList());

        when(configService.headClickCommandsSlotsRequired()).thenReturn(-1);

        assertThat(rewardService.hasPlayerSlotsRequired(player, playerHeadsOfSize(1), huntConfig)).isTrue();
    }

    @Test
    void hasPlayerSlotsRequiredHunt_tiered_noSlots_required_returns_true() {
        HuntConfig huntConfig = new HuntConfig(configService);
        TieredReward tier = new TieredReward(1, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), -1, false);
        huntConfig.setTieredRewards(List.of(tier));

        when(configService.headClickCommandsSlotsRequired()).thenReturn(-1);

        assertThat(rewardService.hasPlayerSlotsRequired(player, playerHeadsOfSize(1), huntConfig)).isTrue();
    }
}
