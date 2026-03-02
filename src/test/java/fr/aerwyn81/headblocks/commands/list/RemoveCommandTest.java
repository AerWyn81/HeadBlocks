package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.ConfigService;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
class RemoveCommandTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private HeadService headService;

    @Mock
    private StorageService storageService;

    @Mock
    private LanguageService languageService;

    @Mock
    private ConfigService configService;

    @Mock
    private Player playerSender;

    @Mock
    private ConsoleCommandSender consoleSender;

    private Remove command;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getHeadService()).thenReturn(headService);
        lenient().when(registry.getStorageService()).thenReturn(storageService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(registry.getConfigService()).thenReturn(configService);
        lenient().when(languageService.message(anyString())).thenReturn("mock-message");
        command = new Remove(registry);
    }

    @Test
    void tooManyArgs_sendsError() {
        boolean result = command.perform(playerSender, new String[]{"remove", "arg1", "arg2"});

        assertThat(result).isTrue();
        verify(languageService).message("Messages.ErrorCommand");
    }

    @Nested
    class NoArgTargetBlock {

        @Test
        void consoleWithNoArg_sendsPlayerOnly() {
            boolean result = command.perform(consoleSender, new String[]{"remove"});

            assertThat(result).isTrue();
            verify(languageService).message("Messages.PlayerOnly");
        }

        @Test
        void targetBlockNotHead_sendsError() {
            Block block = mock(Block.class);
            Location blockLoc = mock(Location.class);
            when(playerSender.getTargetBlock(null, 25)).thenReturn(block);
            when(block.getLocation()).thenReturn(blockLoc);
            when(headService.getHeadAt(blockLoc)).thenReturn(null);

            boolean result = command.perform(playerSender, new String[]{"remove"});

            assertThat(result).isTrue();
            verify(languageService).message("Messages.TargetBlockNotHead");
        }

        @Test
        void targetBlockIsHead_removesSuccessfully() throws InternalException {
            Block block = mock(Block.class);
            Location blockLoc = mock(Location.class);
            HeadLocation headLocation = mock(HeadLocation.class);
            when(playerSender.getTargetBlock(null, 25)).thenReturn(block);
            when(block.getLocation()).thenReturn(blockLoc);
            when(headService.getHeadAt(blockLoc)).thenReturn(headLocation);
            when(headLocation.getLocation()).thenReturn(blockLoc);

            try (MockedStatic<LocationUtils> lu = mockStatic(LocationUtils.class)) {
                lu.when(() -> LocationUtils.parseLocationPlaceholders(anyString(), any(Location.class)))
                        .thenReturn("parsed-message");

                boolean result = command.perform(playerSender, new String[]{"remove"});

                assertThat(result).isTrue();
                verify(headService).removeHeadLocation(headLocation, configService.resetPlayerData());
            }
        }
    }

    @Nested
    class WithArgIdentifier {

        @Test
        void validUuid_removesHead() throws InternalException {
            UUID headUuid = UUID.randomUUID();
            HeadLocation headLocation = mock(HeadLocation.class);
            Location loc = mock(Location.class);
            when(headService.getHeadByUUID(headUuid)).thenReturn(headLocation);
            when(headLocation.getLocation()).thenReturn(loc);

            try (MockedStatic<LocationUtils> lu = mockStatic(LocationUtils.class)) {
                lu.when(() -> LocationUtils.parseLocationPlaceholders(anyString(), any(Location.class)))
                        .thenReturn("parsed-message");

                boolean result = command.perform(playerSender, new String[]{"remove", headUuid.toString()});

                assertThat(result).isTrue();
                verify(headService).removeHeadLocation(headLocation, configService.resetPlayerData());
            }
        }

        @Test
        void validName_removesHead() throws InternalException {
            HeadLocation headLocation = mock(HeadLocation.class);
            Location loc = mock(Location.class);
            when(headService.getHeadByName("myHead")).thenReturn(headLocation);
            when(headLocation.getLocation()).thenReturn(loc);

            try (MockedStatic<LocationUtils> lu = mockStatic(LocationUtils.class)) {
                lu.when(() -> LocationUtils.parseLocationPlaceholders(anyString(), any(Location.class)))
                        .thenReturn("parsed-message");

                boolean result = command.perform(playerSender, new String[]{"remove", "myHead"});

                assertThat(result).isTrue();
                verify(headService).removeHeadLocation(headLocation, configService.resetPlayerData());
            }
        }

        @Test
        void unknownIdentifier_sendsError() {
            when(headService.getHeadByName("unknown")).thenReturn(null);

            boolean result = command.perform(playerSender, new String[]{"remove", "unknown"});

            assertThat(result).isTrue();
            verify(languageService).message("Messages.RemoveLocationError");
        }

        @Test
        void storageError_sendsError() throws InternalException {
            UUID headUuid = UUID.randomUUID();
            HeadLocation headLocation = mock(HeadLocation.class);
            Location loc = mock(Location.class);
            when(headService.getHeadByUUID(headUuid)).thenReturn(headLocation);
            when(headLocation.getLocation()).thenReturn(loc);
            when(headLocation.getNameOrUuid()).thenReturn("test-head");
            doThrow(new InternalException("db error")).when(headService).removeHeadLocation(eq(headLocation), anyBoolean());

            boolean result = command.perform(playerSender, new String[]{"remove", headUuid.toString()});

            assertThat(result).isTrue();
            verify(languageService).message("Messages.StorageError");
        }
    }

    @Nested
    class TabCompletion {

        @Test
        void secondArg_returnsFilteredHeadNames() {
            ArrayList<String> names = new ArrayList<>(java.util.List.of("head1", "head2", "other"));
            when(headService.getHeadRawNameOrUuid()).thenReturn(names);

            ArrayList<String> result = command.tabComplete(playerSender, new String[]{"remove", "he"});

            assertThat(result).containsExactly("head1", "head2");
        }

        @Test
        void thirdArg_returnsEmpty() {
            ArrayList<String> result = command.tabComplete(playerSender, new String[]{"remove", "head1", "x"});

            assertThat(result).isEmpty();
        }
    }
}
