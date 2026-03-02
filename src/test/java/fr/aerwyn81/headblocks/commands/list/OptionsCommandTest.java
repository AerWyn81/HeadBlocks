package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.GuiService;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.gui.types.HintGui;
import fr.aerwyn81.headblocks.services.gui.types.OrderGui;
import fr.aerwyn81.headblocks.services.gui.types.RewardsGui;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OptionsCommandTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private HeadService headService;

    @Mock
    private LanguageService languageService;

    @Mock
    private GuiService guiService;

    @Mock
    private OrderGui orderManager;

    @Mock
    private HintGui hintManager;

    @Mock
    private RewardsGui rewardsManager;

    @Mock
    private Player player;

    private Options command;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getHeadService()).thenReturn(headService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(registry.getGuiService()).thenReturn(guiService);
        lenient().when(guiService.getOrderManager()).thenReturn(orderManager);
        lenient().when(guiService.getHintManager()).thenReturn(hintManager);
        lenient().when(guiService.getRewardsManager()).thenReturn(rewardsManager);
        lenient().when(languageService.message(anyString())).thenReturn("mock-message");
        command = new Options(registry);
    }

    @Test
    void noSubcommand_opensOptionsGui() {
        boolean result = command.perform(player, new String[]{"options"});

        assertThat(result).isTrue();
        verify(guiService).openOptionsGui(player);
    }

    @Test
    void orderSubcommand_opensOrderGui() {
        boolean result = command.perform(player, new String[]{"options", "order"});

        assertThat(result).isTrue();
        verify(orderManager).openOrderGui(player);
    }

    @Test
    void hintSubcommand_opensHintGui() {
        boolean result = command.perform(player, new String[]{"options", "hint"});

        assertThat(result).isTrue();
        verify(hintManager).openHintGui(player);
    }

    @Nested
    class RewardsSubcommand {

        @Test
        void noHeadArg_opensRewardsWithNull() {
            boolean result = command.perform(player, new String[]{"options", "rewards"});

            assertThat(result).isTrue();
            verify(rewardsManager).openRewardsSelectionGui(player, null);
        }

        @Test
        void validHeadArg_opensRewardsWithHead() {
            HeadLocation head = mock(HeadLocation.class);
            when(headService.resolveHeadIdentifier("myHead")).thenReturn(head);

            boolean result = command.perform(player, new String[]{"options", "rewards", "myHead"});

            assertThat(result).isTrue();
            verify(rewardsManager).openRewardsSelectionGui(player, head);
        }

        @Test
        void unknownHeadArg_sendsError() {
            when(headService.resolveHeadIdentifier("unknown")).thenReturn(null);

            boolean result = command.perform(player, new String[]{"options", "rewards", "unknown"});

            assertThat(result).isTrue();
            verify(languageService).message("Messages.HeadNameNotFound");
        }
    }

    @Nested
    class TabCompletion {

        @Test
        void secondArg_returnsSubcommands() {
            ArrayList<String> result = command.tabComplete(player, new String[]{"options", ""});

            assertThat(result).containsExactly("hint", "order", "rewards");
        }

        @Test
        void secondArg_filtersSubcommands() {
            ArrayList<String> result = command.tabComplete(player, new String[]{"options", "h"});

            assertThat(result).containsExactly("hint");
        }

        @Test
        void thirdArg_returnsHeadNames() {
            ArrayList<String> names = new ArrayList<>(java.util.List.of("head1", "head2"));
            when(headService.getHeadRawNameOrUuid()).thenReturn(names);

            ArrayList<String> result = command.tabComplete(player, new String[]{"options", "rewards", ""});

            assertThat(result).containsExactly("head1", "head2");
        }

        @Test
        void fourthArg_returnsEmpty() {
            ArrayList<String> result = command.tabComplete(player, new String[]{"options", "rewards", "head", ""});

            assertThat(result).isEmpty();
        }
    }
}
