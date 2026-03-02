package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.reward.Reward;
import fr.aerwyn81.headblocks.data.reward.RewardType;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.HuntService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InfoCommandTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private HeadService headService;

    @Mock
    private HuntService huntService;

    @Mock
    private LanguageService languageService;

    @Mock
    private Player player;

    @Mock
    private Player.Spigot spigot;

    private Info command;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getHeadService()).thenReturn(headService);
        lenient().when(registry.getHuntService()).thenReturn(huntService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(languageService.message(anyString())).thenReturn("mock-message");
        command = new Info(registry);
    }

    @Test
    void targetNotHead_sendsError() {
        Block block = mock(Block.class);
        Location targetLoc = mock(Location.class);
        when(player.getTargetBlock(null, 100)).thenReturn(block);
        when(block.getLocation()).thenReturn(targetLoc);
        when(headService.getHeadAt(targetLoc)).thenReturn(null);

        boolean result = command.perform(player, new String[]{"info"});

        assertThat(result).isTrue();
        verify(languageService).message("Messages.NoTargetHeadBlock");
    }

    @Test
    void targetIsHead_displaysInfo() {
        UUID headUuid = UUID.randomUUID();
        Block block = mock(Block.class);
        Location targetLoc = mock(Location.class);
        HeadLocation headLocation = mock(HeadLocation.class);
        Location headLoc = mock(Location.class);
        World world = mock(World.class);

        when(player.getTargetBlock(null, 100)).thenReturn(block);
        when(block.getLocation()).thenReturn(targetLoc);
        when(headService.getHeadAt(targetLoc)).thenReturn(headLocation);
        when(headLocation.getUuid()).thenReturn(headUuid);
        when(headLocation.getName()).thenReturn("test-head");
        when(headLocation.getNameOrUnnamed(anyString())).thenReturn("test-head");
        when(headLocation.getLocation()).thenReturn(headLoc);
        when(headLoc.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(headLocation.getX()).thenReturn(10.0);
        when(headLoc.getY()).thenReturn(20.0);
        when(headLocation.getZ()).thenReturn(30.0);
        when(headLocation.isCharged()).thenReturn(true);
        when(headLocation.getDisplayedOrderIndex(anyString())).thenReturn("1");
        when(headLocation.isHintSoundEnabled()).thenReturn(true);
        when(headLocation.isHintActionBarEnabled()).thenReturn(false);
        when(headLocation.getRewards()).thenReturn(new ArrayList<>());
        when(huntService.getHuntsForHead(headUuid)).thenReturn(new ArrayList<>());
        when(player.spigot()).thenReturn(spigot);

        try (MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class);
             MockedStatic<LocationUtils> lu = mockStatic(LocationUtils.class)) {
            mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));
            lu.when(() -> LocationUtils.toFormattedString(any(Location.class))).thenReturn("world, 10, 20, 30");

            boolean result = command.perform(player, new String[]{"info"});

            assertThat(result).isTrue();
            // Verify multiple spigot messages were sent
            verify(spigot, atLeast(5)).sendMessage(any(net.md_5.bungee.api.chat.TextComponent.class));
        }
    }

    @Test
    void targetIsHead_withRewards_displaysRewards() {
        UUID headUuid = UUID.randomUUID();
        Block block = mock(Block.class);
        Location targetLoc = mock(Location.class);
        HeadLocation headLocation = mock(HeadLocation.class);
        Location headLoc = mock(Location.class);
        World world = mock(World.class);

        when(player.getTargetBlock(null, 100)).thenReturn(block);
        when(block.getLocation()).thenReturn(targetLoc);
        when(headService.getHeadAt(targetLoc)).thenReturn(headLocation);
        when(headLocation.getUuid()).thenReturn(headUuid);
        when(headLocation.getName()).thenReturn("test-head");
        when(headLocation.getNameOrUnnamed(anyString())).thenReturn("test-head");
        when(headLocation.getLocation()).thenReturn(headLoc);
        when(headLoc.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(headLocation.getX()).thenReturn(10.0);
        when(headLoc.getY()).thenReturn(20.0);
        when(headLocation.getZ()).thenReturn(30.0);
        when(headLocation.isCharged()).thenReturn(true);
        when(headLocation.getDisplayedOrderIndex(anyString())).thenReturn("1");
        when(headLocation.isHintSoundEnabled()).thenReturn(true);
        when(headLocation.isHintActionBarEnabled()).thenReturn(false);
        when(huntService.getHuntsForHead(headUuid)).thenReturn(new ArrayList<>());
        when(player.spigot()).thenReturn(spigot);

        Reward reward = new Reward(RewardType.COMMAND, "give %player% diamond 1");
        ArrayList<Reward> rewards = new ArrayList<>(java.util.List.of(reward));
        when(headLocation.getRewards()).thenReturn(rewards);

        try (MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class);
             MockedStatic<LocationUtils> lu = mockStatic(LocationUtils.class)) {
            mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));
            lu.when(() -> LocationUtils.toFormattedString(any(Location.class))).thenReturn("world, 10, 20, 30");

            boolean result = command.perform(player, new String[]{"info"});

            assertThat(result).isTrue();
            verify(languageService).message("Chat.Info.RewardsTitle");
        }
    }

    @Test
    void tabComplete_returnsEmpty() {
        assertThat(command.tabComplete(player, new String[]{"info"})).isEmpty();
    }
}
