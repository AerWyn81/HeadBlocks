package fr.aerwyn81.headblocks.data.hunt.behavior;

import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderedBehaviorTest {

    @Mock
    Player player;

    @Mock
    HeadLocation headLocation;

    private final OrderedBehavior behavior = new OrderedBehavior();
    private final Hunt hunt = createTestHunt();

    private static Hunt createTestHunt() {
        Hunt h = new Hunt("hunt1", "Test Hunt", HuntState.ACTIVE, 1, "DIAMOND");
        return h;
    }

    @Test
    void canPlayerClick_orderIndexZero_alwaysAllow() {
        when(headLocation.getOrderIndex()).thenReturn(0);

        BehaviorResult result = behavior.canPlayerClick(player, headLocation, hunt);

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void canPlayerClick_orderIndexNegative_alwaysAllow() {
        // -1 is the default orderIndex for heads without explicit ordering
        when(headLocation.getOrderIndex()).thenReturn(-1);

        BehaviorResult result = behavior.canPlayerClick(player, headLocation, hunt);

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void canPlayerClick_noPriorUnfound_returnsAllow() {
        UUID headUuid = UUID.randomUUID();
        UUID playerUuid = UUID.randomUUID();

        when(headLocation.getOrderIndex()).thenReturn(2);
        when(headLocation.getUuid()).thenReturn(headUuid);
        when(player.getUniqueId()).thenReturn(playerUuid);

        // Prior head with orderIndex=1 that player already found
        UUID priorHeadUuid = UUID.randomUUID();
        HeadLocation priorHead = mock(HeadLocation.class);
        when(priorHead.getUuid()).thenReturn(priorHeadUuid);
        when(priorHead.getOrderIndex()).thenReturn(1);

        hunt.addHead(headUuid);
        hunt.addHead(priorHeadUuid);

        try (MockedStatic<StorageService> ss = mockStatic(StorageService.class);
             MockedStatic<HeadService> hs = mockStatic(HeadService.class)) {

            ArrayList<UUID> playerHeads = new ArrayList<>();
            playerHeads.add(priorHeadUuid);
            ss.when(() -> StorageService.getHeadsPlayerForHunt(playerUuid, "hunt1")).thenReturn(playerHeads);
            hs.when(HeadService::getChargedHeadLocations).thenReturn(new ArrayList<>(java.util.List.of(priorHead, headLocation)));

            BehaviorResult result = behavior.canPlayerClick(player, headLocation, hunt);

            assertThat(result.allowed()).isTrue();
        }
    }

    @Test
    void canPlayerClick_priorUnfound_returnsDeny() {
        UUID headUuid = UUID.randomUUID();
        UUID playerUuid = UUID.randomUUID();

        when(headLocation.getOrderIndex()).thenReturn(2);
        when(headLocation.getUuid()).thenReturn(headUuid);
        when(headLocation.getNameOrUnnamed()).thenReturn("TestHead");
        when(player.getUniqueId()).thenReturn(playerUuid);

        // Prior head with orderIndex=1 that player has NOT found
        UUID priorHeadUuid = UUID.randomUUID();
        HeadLocation priorHead = mock(HeadLocation.class);
        when(priorHead.getUuid()).thenReturn(priorHeadUuid);
        when(priorHead.getOrderIndex()).thenReturn(1);

        hunt.addHead(headUuid);
        hunt.addHead(priorHeadUuid);

        try (MockedStatic<StorageService> ss = mockStatic(StorageService.class);
             MockedStatic<HeadService> hs = mockStatic(HeadService.class);
             MockedStatic<LanguageService> ls = mockStatic(LanguageService.class)) {

            ArrayList<UUID> playerHeads = new ArrayList<>(); // empty = nothing found
            ss.when(() -> StorageService.getHeadsPlayerForHunt(playerUuid, "hunt1")).thenReturn(playerHeads);
            hs.when(HeadService::getChargedHeadLocations).thenReturn(new ArrayList<>(java.util.List.of(priorHead, headLocation)));
            ls.when(() -> LanguageService.getMessage("Messages.OrderClickError")).thenReturn("Must find %name% first");

            BehaviorResult result = behavior.canPlayerClick(player, headLocation, hunt);

            assertThat(result.allowed()).isFalse();
            assertThat(result.denyMessage()).contains("TestHead");
        }
    }

    @Test
    void canPlayerClick_storageException_returnsAllowGracefully() throws InternalException {
        UUID headUuid = UUID.randomUUID();
        UUID playerUuid = UUID.randomUUID();

        when(headLocation.getOrderIndex()).thenReturn(2);
        when(player.getUniqueId()).thenReturn(playerUuid);

        try (MockedStatic<StorageService> ss = mockStatic(StorageService.class)) {
            ss.when(() -> StorageService.getHeadsPlayerForHunt(any(), anyString()))
                    .thenThrow(new InternalException("DB error"));

            BehaviorResult result = behavior.canPlayerClick(player, headLocation, hunt);

            assertThat(result.allowed()).isTrue();
        }
    }

    @Test
    void getId_returnsOrdered() {
        assertThat(behavior.getId()).isEqualTo("ordered");
    }

    @Test
    void onHeadFound_isNoOp() {
        assertThatNoException().isThrownBy(() -> behavior.onHeadFound(player, headLocation, hunt));
    }
}
