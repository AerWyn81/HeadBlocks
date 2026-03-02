package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.data.head.types.HBHeadHDB;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GiveCommandTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private HeadService headService;

    @Mock
    private LanguageService languageService;

    @Mock
    private Player player;

    @Mock
    private PlayerInventory playerInventory;

    private Give command;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getHeadService()).thenReturn(headService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(languageService.message(anyString())).thenReturn("mock-message");
        lenient().when(languageService.message(anyString(), anyString())).thenReturn("mock-message");
        command = new Give(registry);
    }

    @Test
    void emptyHeads_sendsListEmpty() {
        when(headService.getHeads()).thenReturn(new ArrayList<>());

        boolean result = command.perform(player, new String[]{"give"});

        assertThat(result).isTrue();
        verify(languageService).message("Messages.ListHeadEmpty");
    }

    @Test
    void targetPlayerNotConnected_sendsError() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer("unknownPlayer")).thenReturn(null);

            boolean result = command.perform(player, new String[]{"give", "unknownPlayer"});

            assertThat(result).isTrue();
            verify(languageService).message("Messages.PlayerNotConnected", "unknownPlayer");
        }
    }

    @Nested
    class GiveAllHeads {

        @Test
        void noSlots_sendsInventoryFull() {
            HBHead head1 = mock(HBHead.class);
            HBHead head2 = mock(HBHead.class);
            ArrayList<HBHead> heads = new ArrayList<>(java.util.List.of(head1, head2));
            when(headService.getHeads()).thenReturn(heads);

            try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class)) {
                pu.when(() -> PlayerUtils.getEmptySlots(player)).thenReturn(1);

                boolean result = command.perform(player, new String[]{"give"});

                assertThat(result).isTrue();
                verify(languageService).message("Messages.InventoryFull");
            }
        }

        @Test
        void enoughSlots_givesAllHeads() {
            HBHead head1 = mock(HBHead.class);
            HBHead head2 = mock(HBHead.class);
            ItemStack item1 = mock(ItemStack.class);
            ItemStack item2 = mock(ItemStack.class);
            when(head1.getItemStack()).thenReturn(item1);
            when(head2.getItemStack()).thenReturn(item2);
            ArrayList<HBHead> heads = new ArrayList<>(java.util.List.of(head1, head2));
            when(headService.getHeads()).thenReturn(heads);
            when(player.getInventory()).thenReturn(playerInventory);

            try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class)) {
                pu.when(() -> PlayerUtils.getEmptySlots(player)).thenReturn(5);

                boolean result = command.perform(player, new String[]{"give"});

                assertThat(result).isTrue();
                verify(playerInventory).addItem(item1);
                verify(playerInventory).addItem(item2);
                verify(languageService).message("Messages.HeadGiven");
            }
        }

        @Test
        void withStar_givesAllHeads() {
            HBHead head1 = mock(HBHead.class);
            ItemStack item1 = mock(ItemStack.class);
            when(head1.getItemStack()).thenReturn(item1);
            ArrayList<HBHead> heads = new ArrayList<>(java.util.List.of(head1));
            when(headService.getHeads()).thenReturn(heads);
            when(player.getInventory()).thenReturn(playerInventory);

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
                 MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class)) {
                bukkit.when(() -> Bukkit.getPlayer("pName")).thenReturn(player);
                pu.when(() -> PlayerUtils.getEmptySlots(player)).thenReturn(5);

                boolean result = command.perform(player, new String[]{"give", "pName", "*"});

                assertThat(result).isTrue();
                verify(playerInventory).addItem(item1);
            }
        }
    }

    @Nested
    class GiveSpecificHead {

        @Test
        void validId_givesOneHead() {
            HBHead head1 = mock(HBHead.class);
            HBHead head2 = mock(HBHead.class);
            ItemStack item2 = mock(ItemStack.class);
            when(head2.getItemStack()).thenReturn(item2);
            ArrayList<HBHead> heads = new ArrayList<>(java.util.List.of(head1, head2));
            when(headService.getHeads()).thenReturn(heads);
            when(player.getInventory()).thenReturn(playerInventory);

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
                 MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class)) {
                bukkit.when(() -> Bukkit.getPlayer("pName")).thenReturn(player);
                pu.when(() -> PlayerUtils.getEmptySlots(player)).thenReturn(5);

                boolean result = command.perform(player, new String[]{"give", "pName", "2"});

                assertThat(result).isTrue();
                verify(playerInventory).addItem(item2);
                verify(playerInventory, times(1)).addItem(any(ItemStack.class));
            }
        }

        @Test
        void idTooHigh_sendsError() {
            HBHead head1 = mock(HBHead.class);
            ArrayList<HBHead> heads = new ArrayList<>(java.util.List.of(head1));
            when(headService.getHeads()).thenReturn(heads);

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getPlayer("pName")).thenReturn(player);

                boolean result = command.perform(player, new String[]{"give", "pName", "5"});

                assertThat(result).isTrue();
                verify(languageService).message("Messages.ErrorCommand");
            }
        }

        @Test
        void invalidNumber_defaultsToFirst() {
            HBHead head1 = mock(HBHead.class);
            ItemStack item1 = mock(ItemStack.class);
            when(head1.getItemStack()).thenReturn(item1);
            ArrayList<HBHead> heads = new ArrayList<>(java.util.List.of(head1));
            when(headService.getHeads()).thenReturn(heads);
            when(player.getInventory()).thenReturn(playerInventory);

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
                 MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class)) {
                bukkit.when(() -> Bukkit.getPlayer("pName")).thenReturn(player);
                pu.when(() -> PlayerUtils.getEmptySlots(player)).thenReturn(5);

                boolean result = command.perform(player, new String[]{"give", "pName", "abc"});

                assertThat(result).isTrue();
                verify(playerInventory).addItem(item1);
            }
        }

        @Test
        void negativeId_defaultsToFirst() {
            HBHead head1 = mock(HBHead.class);
            ItemStack item1 = mock(ItemStack.class);
            when(head1.getItemStack()).thenReturn(item1);
            ArrayList<HBHead> heads = new ArrayList<>(java.util.List.of(head1));
            when(headService.getHeads()).thenReturn(heads);
            when(player.getInventory()).thenReturn(playerInventory);

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
                 MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class)) {
                bukkit.when(() -> Bukkit.getPlayer("pName")).thenReturn(player);
                pu.when(() -> PlayerUtils.getEmptySlots(player)).thenReturn(5);

                boolean result = command.perform(player, new String[]{"give", "pName", "-1"});

                assertThat(result).isTrue();
                verify(playerInventory).addItem(item1);
            }
        }
    }

    @Nested
    class HDBHead {

        @Test
        void hdbHeadNotLoaded_sendsNotLoadedMessage() {
            HBHeadHDB hdbHead = mock(HBHeadHDB.class);
            when(hdbHead.isLoaded()).thenReturn(false);
            when(hdbHead.getId()).thenReturn("hdb-123");
            ArrayList<HBHead> heads = new ArrayList<>(java.util.List.of(hdbHead));
            when(headService.getHeads()).thenReturn(heads);

            try (MockedStatic<PlayerUtils> pu = mockStatic(PlayerUtils.class)) {
                pu.when(() -> PlayerUtils.getEmptySlots(player)).thenReturn(5);

                boolean result = command.perform(player, new String[]{"give"});

                assertThat(result).isTrue();
                // headGiven is 0, so HeadGiven message should not be sent
                verify(languageService, never()).message("Messages.HeadGiven");
            }
        }
    }

    @Nested
    class TabCompletion {

        @Test
        void thirdArg_withMultipleHeads_returnsStarAndNumbers() {
            HBHead h1 = mock(HBHead.class);
            HBHead h2 = mock(HBHead.class);
            HBHead h3 = mock(HBHead.class);
            when(headService.getHeads()).thenReturn(new ArrayList<>(java.util.List.of(h1, h2, h3)));

            ArrayList<String> result = command.tabComplete(player, new String[]{"give", "player", ""});

            assertThat(result).containsExactly("*", "1", "2", "3");
        }

        @Test
        void thirdArg_withSingleHead_returnsEmpty() {
            HBHead h1 = mock(HBHead.class);
            when(headService.getHeads()).thenReturn(new ArrayList<>(java.util.List.of(h1)));

            ArrayList<String> result = command.tabComplete(player, new String[]{"give", "player", ""});

            assertThat(result).isEmpty();
        }
    }
}
