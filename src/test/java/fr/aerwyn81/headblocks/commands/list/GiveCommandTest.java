package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.services.GuiService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.gui.types.CatalogGui;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GiveCommandTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private LanguageService languageService;

    @Mock
    private GuiService guiService;

    @Mock
    private CatalogGui catalogGui;

    @Mock
    private Player player;

    private Give command;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(registry.getGuiService()).thenReturn(guiService);
        lenient().when(guiService.getCatalogGui()).thenReturn(catalogGui);
        lenient().when(languageService.message(anyString())).thenReturn("mock-message");
        lenient().when(languageService.message(anyString(), anyString())).thenReturn("mock-message");
        command = new Give(registry);
    }

    @Test
    void noArgument_opensTheCatalogForTheSender() {
        assertThat(command.perform(player, new String[]{"give"})).isTrue();

        verify(catalogGui).open(player);
    }

    @Test
    void playerArgument_opensTheCatalogForThatPlayer() {
        Player target = mock(Player.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer("Steve")).thenReturn(target);

            command.perform(player, new String[]{"give", "Steve"});
        }

        verify(catalogGui).open(target);
        verify(catalogGui, never()).open(player);
    }

    @Test
    void extraArguments_areIgnored() {
        Player target = mock(Player.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer("Steve")).thenReturn(target);

            command.perform(player, new String[]{"give", "Steve", "2", "halloween"});
        }

        verify(catalogGui).open(target);
    }

    @Test
    void offlinePlayer_sendsAnError() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer("Ghost")).thenReturn(null);

            command.perform(player, new String[]{"give", "Ghost"});
        }

        verify(languageService).message("Messages.PlayerNotConnected", "Ghost");
        verifyNoInteractions(catalogGui);
    }

    @Test
    void console_withAPlayer_opensTheCatalogForThatPlayer() {
        Player target = mock(Player.class);
        ConsoleCommandSender console = mock(ConsoleCommandSender.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer("Steve")).thenReturn(target);

            command.perform(console, new String[]{"give", "Steve"});
        }

        verify(catalogGui).open(target);
    }

    @Test
    void console_withoutPlayer_isRefused() {
        ConsoleCommandSender console = mock(ConsoleCommandSender.class);

        command.perform(console, new String[]{"give"});

        verify(languageService).message("Messages.PlayerOnly");
        verifyNoInteractions(catalogGui);
    }

    @Test
    void tabCompletion_offersOnlinePlayersOnly() {
        Player steve = mock(Player.class);
        Player alex = mock(Player.class);
        when(steve.getName()).thenReturn("Steve");
        when(alex.getName()).thenReturn("Alex");

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenAnswer(invocation -> List.of(steve, alex));

            ArrayList<String> result = command.tabComplete(player, new String[]{"give", "st"});

            assertThat(result).containsExactly("Steve");
        }

        assertThat(command.tabComplete(player, new String[]{"give", "Steve", ""})).isEmpty();
    }
}
