package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.ConfigService;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemoveAllCommandTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private HeadService headService;

    @Mock
    private LanguageService languageService;

    @Mock
    private ConfigService configService;

    @Mock
    private CommandSender sender;

    private RemoveAll command;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getHeadService()).thenReturn(headService);
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(registry.getConfigService()).thenReturn(configService);
        lenient().when(languageService.message(anyString())).thenReturn("mock-message");
        command = new RemoveAll(registry);
    }

    @Test
    void emptyHeads_sendsListEmpty() {
        when(headService.getChargedHeadLocations()).thenReturn(new ArrayList<>());

        boolean result = command.perform(sender, new String[]{"removeall"});

        assertThat(result).isTrue();
        verify(languageService).message("Messages.ListHeadEmpty");
    }

    @Test
    void noConfirmFlag_sendsConfirmMessage() {
        ArrayList<HeadLocation> heads = new ArrayList<>();
        heads.add(mock(HeadLocation.class));
        heads.add(mock(HeadLocation.class));
        when(headService.getChargedHeadLocations()).thenReturn(heads);

        boolean result = command.perform(sender, new String[]{"removeall"});

        assertThat(result).isTrue();
        verify(languageService).message("Messages.RemoveAllConfirm");
    }

    @SuppressWarnings("unchecked")
    @Test
    void withConfirm_startsRemoval() {
        ArrayList<HeadLocation> heads = new ArrayList<>();
        heads.add(mock(HeadLocation.class));
        when(headService.getChargedHeadLocations()).thenReturn(heads);

        boolean result = command.perform(sender, new String[]{"removeall", "--confirm"});

        assertThat(result).isTrue();
        verify(languageService).message("Messages.RemoveAllInProgress");
        verify(headService).removeAllHeadLocationsAsync(eq(heads), anyBoolean(), any(Consumer.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void withConfirm_callbackZeroRemoved_sendsError() {
        ArrayList<HeadLocation> heads = new ArrayList<>();
        heads.add(mock(HeadLocation.class));
        when(headService.getChargedHeadLocations()).thenReturn(heads);

        doAnswer(invocation -> {
            Consumer<Integer> callback = invocation.getArgument(2);
            callback.accept(0);
            return null;
        }).when(headService).removeAllHeadLocationsAsync(any(ArrayList.class), anyBoolean(), any(Consumer.class));

        command.perform(sender, new String[]{"removeall", "--confirm"});

        verify(languageService).message("Messages.RemoveAllError");
    }

    @SuppressWarnings("unchecked")
    @Test
    void withConfirm_callbackSuccess_sendsSuccess() {
        ArrayList<HeadLocation> heads = new ArrayList<>();
        heads.add(mock(HeadLocation.class));
        heads.add(mock(HeadLocation.class));
        when(headService.getChargedHeadLocations()).thenReturn(heads);

        doAnswer(invocation -> {
            Consumer<Integer> callback = invocation.getArgument(2);
            callback.accept(2);
            return null;
        }).when(headService).removeAllHeadLocationsAsync(any(ArrayList.class), anyBoolean(), any(Consumer.class));

        command.perform(sender, new String[]{"removeall", "--confirm"});

        verify(languageService).message("Messages.RemoveAllSuccess");
    }

    @Test
    void tabComplete_secondArg_returnsConfirm() {
        ArrayList<String> result = command.tabComplete(sender, new String[]{"removeall", ""});

        assertThat(result).containsExactly("--confirm");
    }

    @Test
    void tabComplete_thirdArg_returnsEmpty() {
        ArrayList<String> result = command.tabComplete(sender, new String[]{"removeall", "--confirm", ""});

        assertThat(result).isEmpty();
    }
}
