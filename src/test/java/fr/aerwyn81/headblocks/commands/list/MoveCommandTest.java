package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.HeadMove;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.bukkit.LocationUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MoveCommandTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private HeadService headService;

    @Mock
    private LanguageService languageService;

    @Mock
    private Player player;

    private Move command;
    private HashMap<UUID, HeadMove> headMoves;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getHeadService()).thenReturn(headService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(languageService.message(anyString())).thenReturn("mock-message");

        headMoves = new HashMap<>();
        lenient().when(headService.getHeadMoves()).thenReturn(headMoves);

        command = new Move(registry);
    }

    @Nested
    class CancelMove {

        @Test
        void cancelWithPendingMove_removesAndSendsMessage() {
            UUID playerUuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);
            headMoves.put(playerUuid, new HeadMove(UUID.randomUUID(), mock(Location.class)));

            boolean result = command.perform(player, new String[]{"move", "--cancel"});

            assertThat(result).isTrue();
            assertThat(headMoves).doesNotContainKey(playerUuid);
            verify(languageService).message("Messages.HeadMoveCancel");
        }

        @Test
        void cancelWithNoPendingMove_noMessage() {
            UUID playerUuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);

            boolean result = command.perform(player, new String[]{"move", "--cancel"});

            assertThat(result).isTrue();
            verify(languageService, never()).message("Messages.HeadMoveCancel");
        }
    }

    @Nested
    class InitiateMove {

        @Test
        void alreadyHasPendingMove_sendsAlreadyMessage() {
            UUID playerUuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);
            headMoves.put(playerUuid, new HeadMove(UUID.randomUUID(), mock(Location.class)));

            boolean result = command.perform(player, new String[]{"move"});

            assertThat(result).isTrue();
            verify(languageService).message("Messages.HeadMoveAlready");
        }

        @Test
        void targetNotHead_sendsError() {
            UUID playerUuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);

            Block block = mock(Block.class);
            Location targetLoc = mock(Location.class);
            when(player.getTargetBlock(null, 100)).thenReturn(block);
            when(block.getLocation()).thenReturn(targetLoc);
            when(headService.getHeadAt(targetLoc)).thenReturn(null);

            boolean result = command.perform(player, new String[]{"move"});

            assertThat(result).isTrue();
            verify(languageService).message("Messages.NoTargetHeadBlock");
        }

        @Test
        void targetIsHead_storesHeadMoveAndSendsInfo() {
            UUID playerUuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);

            Block block = mock(Block.class);
            Location targetLoc = mock(Location.class);
            HeadLocation headLocation = mock(HeadLocation.class);
            when(player.getTargetBlock(null, 100)).thenReturn(block);
            when(block.getLocation()).thenReturn(targetLoc);
            when(headService.getHeadAt(targetLoc)).thenReturn(headLocation);
            when(headLocation.getNameOrUuid()).thenReturn("test-head");

            try (MockedStatic<LocationUtils> lu = mockStatic(LocationUtils.class)) {
                lu.when(() -> LocationUtils.parseLocationPlaceholders(anyString(), any(Location.class)))
                        .thenReturn("parsed-message");

                boolean result = command.perform(player, new String[]{"move"});

                assertThat(result).isTrue();
                assertThat(headMoves).containsKey(playerUuid);
            }
        }
    }

    @Nested
    class ConfirmMove {

        @Test
        void noPendingMove_sendsNoPlayerMessage() {
            UUID playerUuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);

            Block block = mock(Block.class);
            Location targetLoc = mock(Location.class);
            when(player.getTargetBlock(null, 100)).thenReturn(block);
            when(block.getLocation()).thenReturn(targetLoc);

            boolean result = command.perform(player, new String[]{"move", "--confirm"});

            assertThat(result).isTrue();
            verify(languageService).message("Messages.HeadMoveNoPlayer");
        }

        @Test
        void sameLocation_sendsOtherLocMessage() {
            UUID playerUuid = UUID.randomUUID();
            Location oldLoc = mock(Location.class);
            when(player.getUniqueId()).thenReturn(playerUuid);
            headMoves.put(playerUuid, new HeadMove(UUID.randomUUID(), oldLoc));

            Block block = mock(Block.class);
            Location targetLoc = mock(Location.class);
            Location newLoc = mock(Location.class);
            when(player.getTargetBlock(null, 100)).thenReturn(block);
            when(block.getLocation()).thenReturn(targetLoc);
            when(targetLoc.clone()).thenReturn(newLoc);
            when(newLoc.add(0, 1, 0)).thenReturn(newLoc);

            try (MockedStatic<LocationUtils> lu = mockStatic(LocationUtils.class)) {
                lu.when(() -> LocationUtils.areEquals(newLoc, oldLoc)).thenReturn(true);

                boolean result = command.perform(player, new String[]{"move", "--confirm"});

                assertThat(result).isTrue();
                verify(languageService).message("Messages.HeadMoveOtherLoc");
            }
        }

        @Test
        void emptyTargetBlock_sendsNoTargetMessage() {
            Block targetBlock = mock(Block.class);
            when(player.getTargetBlock(null, 100)).thenReturn(targetBlock);
            when(targetBlock.isEmpty()).thenReturn(true);

            boolean result = command.perform(player, new String[]{"move", "--confirm"});

            assertThat(result).isTrue();
            verify(languageService).message("Messages.NoTargetHeadBlock");
        }

        @Test
        void validMove_changesHeadLocation() {
            UUID playerUuid = UUID.randomUUID();
            UUID headUuid = UUID.randomUUID();
            Location oldLoc = mock(Location.class);
            Block oldBlock = mock(Block.class);
            when(oldLoc.getBlock()).thenReturn(oldBlock);
            when(player.getUniqueId()).thenReturn(playerUuid);
            headMoves.put(playerUuid, new HeadMove(headUuid, oldLoc));

            Block targetBlock = mock(Block.class);
            Location targetLoc = mock(Location.class);
            Location newLoc = mock(Location.class);
            Block newBlock = mock(Block.class);
            when(player.getTargetBlock(null, 100)).thenReturn(targetBlock);
            when(targetBlock.getLocation()).thenReturn(targetLoc);
            when(targetLoc.clone()).thenReturn(newLoc);
            when(newLoc.add(0, 1, 0)).thenReturn(newLoc);
            when(targetLoc.getBlock()).thenReturn(targetBlock);
            when(newLoc.getBlock()).thenReturn(newBlock);
            when(targetBlock.isEmpty()).thenReturn(false);
            when(newBlock.isEmpty()).thenReturn(true);

            try (MockedStatic<LocationUtils> lu = mockStatic(LocationUtils.class);
                 MockedStatic<HeadUtils> hu = mockStatic(HeadUtils.class)) {
                lu.when(() -> LocationUtils.areEquals(newLoc, oldLoc)).thenReturn(false);
                lu.when(() -> LocationUtils.parseLocationPlaceholders(anyString(), any(Location.class)))
                        .thenReturn("parsed-message");
                hu.when(() -> HeadUtils.isPlayerHead(targetBlock)).thenReturn(false);

                boolean result = command.perform(player, new String[]{"move", "--confirm"});

                assertThat(result).isTrue();
                verify(headService).changeHeadLocation(headUuid, oldBlock, newBlock);
                assertThat(headMoves).doesNotContainKey(playerUuid);
            }
        }
    }

    @Nested
    class TabCompletion {

        @Test
        void hasPendingMove_returnsConfirmCancel() {
            UUID playerUuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);
            headMoves.put(playerUuid, new HeadMove(UUID.randomUUID(), mock(Location.class)));

            ArrayList<String> result = command.tabComplete(player, new String[]{"move", ""});

            assertThat(result).containsExactly("--confirm", "--cancel");
        }

        @Test
        void noPendingMove_returnsEmpty() {
            UUID playerUuid = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerUuid);

            ArrayList<String> result = command.tabComplete(player, new String[]{"move", ""});

            assertThat(result).isEmpty();
        }
    }
}
