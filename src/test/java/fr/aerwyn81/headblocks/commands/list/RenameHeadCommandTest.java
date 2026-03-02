package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RenameHeadCommandTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private HeadService headService;

    @Mock
    private LanguageService languageService;

    @Mock
    private Player player;

    private RenameHead command;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getHeadService()).thenReturn(headService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(languageService.message(anyString())).thenReturn("mock-message");
        command = new RenameHead(registry);
    }

    @Test
    void targetNotHead_sendsError() {
        Block block = mock(Block.class);
        Location targetLoc = mock(Location.class);
        when(player.getTargetBlock(null, 100)).thenReturn(block);
        when(block.getLocation()).thenReturn(targetLoc);
        when(headService.getHeadAt(targetLoc)).thenReturn(null);

        boolean result = command.perform(player, new String[]{"rename", "newName"});

        assertThat(result).isTrue();
        verify(languageService).message("Messages.NoTargetHeadBlock");
    }

    @Test
    void emptyName_sendsError() {
        Block block = mock(Block.class);
        Location targetLoc = mock(Location.class);
        HeadLocation headLocation = mock(HeadLocation.class);
        when(player.getTargetBlock(null, 100)).thenReturn(block);
        when(block.getLocation()).thenReturn(targetLoc);
        when(headService.getHeadAt(targetLoc)).thenReturn(headLocation);

        // args = ["rename"] -> after copyOfRange(1, 1) -> empty, join = ""
        boolean result = command.perform(player, new String[]{"rename"});

        assertThat(result).isTrue();
        verify(languageService).message("Messages.NameCannotBeEmpty");
    }

    @Test
    void validName_renamesAndSaves() {
        Block block = mock(Block.class);
        Location targetLoc = mock(Location.class);
        HeadLocation headLocation = mock(HeadLocation.class);
        when(player.getTargetBlock(null, 100)).thenReturn(block);
        when(block.getLocation()).thenReturn(targetLoc);
        when(headService.getHeadAt(targetLoc)).thenReturn(headLocation);

        try (MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class)) {
            mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));

            boolean result = command.perform(player, new String[]{"rename", "My", "New", "Name"});

            assertThat(result).isTrue();
            verify(headLocation).setName("My New Name");
            verify(headService).saveHeadInConfig(headLocation);
            verify(languageService).message("Messages.HeadRenamed");
        }
    }

    @Test
    void tabComplete_returnsEmpty() {
        assertThat(command.tabComplete(player, new String[]{"rename", ""})).isEmpty();
    }
}
