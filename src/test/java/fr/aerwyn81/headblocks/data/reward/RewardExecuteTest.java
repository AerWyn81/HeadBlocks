package fr.aerwyn81.headblocks.data.reward;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.PlaceholdersService;
import fr.aerwyn81.headblocks.utils.bukkit.CommandDispatcher;
import fr.aerwyn81.headblocks.utils.bukkit.PluginProvider;
import fr.aerwyn81.headblocks.utils.bukkit.SchedulerAdapter;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardExecuteTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private PlaceholdersService placeholdersService;

    @Mock
    private SchedulerAdapter scheduler;

    @Mock
    private CommandDispatcher commandDispatcher;

    @Mock
    private PluginProvider pluginProvider;

    @Mock
    private Player player;

    @Mock
    private HeadLocation headLocation;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getPlaceholdersService()).thenReturn(placeholdersService);
        lenient().when(registry.getScheduler()).thenReturn(scheduler);
        lenient().when(registry.getCommandDispatcher()).thenReturn(commandDispatcher);
        lenient().when(registry.getPluginProvider()).thenReturn(pluginProvider);
        lenient().when(player.getName()).thenReturn("Steve");
        lenient().when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    }

    @Test
    void execute_messageType_sendsMessageToPlayer() {
        when(placeholdersService.parse(anyString(), any(UUID.class), any(), anyString()))
                .thenReturn("Hello Steve!");

        Reward reward = new Reward(RewardType.MESSAGE, "Hello %player%!");

        reward.execute(player, headLocation, registry);

        verify(player).sendMessage("Hello Steve!");
    }

    @Test
    void execute_commandType_dispatchesConsoleCommand() {
        when(placeholdersService.parse(anyString(), any(UUID.class), any(), anyString()))
                .thenReturn("give Steve diamond 1");

        Reward reward = new Reward(RewardType.COMMAND, "give %player% diamond 1");

        reward.execute(player, headLocation, registry);

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTaskLater(captor.capture(), eq(1L));

        captor.getValue().run();
        verify(commandDispatcher).dispatchConsoleCommand("give Steve diamond 1");
    }

    @Test
    void execute_broadcastType_broadcastsMessage() {
        when(placeholdersService.parse(anyString(), any(UUID.class), any(), anyString()))
                .thenReturn("Server announcement");

        JavaPlugin javaPlugin = mock(JavaPlugin.class);
        Server server = mock(Server.class);
        when(pluginProvider.getJavaPlugin()).thenReturn(javaPlugin);
        when(javaPlugin.getServer()).thenReturn(server);

        Reward reward = new Reward(RewardType.BROADCAST, "Server announcement");

        reward.execute(player, headLocation, registry);

        verify(server).broadcastMessage("Server announcement");
    }

    @Test
    void execute_emptyParsedValue_doesNothing() {
        when(placeholdersService.parse(anyString(), any(UUID.class), any(), anyString()))
                .thenReturn("");

        Reward reward = new Reward(RewardType.MESSAGE, "something");

        reward.execute(player, headLocation, registry);

        verify(player, never()).sendMessage(anyString());
    }

    @Test
    void execute_parseThrows_logsErrorAndContinues() {
        try (MockedStatic<LogUtil> logUtil = mockStatic(LogUtil.class)) {
            when(placeholdersService.parse(anyString(), any(UUID.class), any(), anyString()))
                    .thenThrow(new RuntimeException("parse error"));

            Reward reward = new Reward(RewardType.MESSAGE, "bad");

            reward.execute(player, headLocation, registry);

            logUtil.verify(() -> LogUtil.error(anyString(), eq("bad"), eq("parse error")));
            verify(player, never()).sendMessage(anyString());
        }
    }
}
