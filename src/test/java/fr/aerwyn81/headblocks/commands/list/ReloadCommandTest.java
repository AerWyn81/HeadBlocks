package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.StorageService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReloadCommandTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private LanguageService languageService;

    @Mock
    private StorageService storageService;

    @Mock
    private CommandSender sender;

    private Reload command;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(registry.getStorageService()).thenReturn(storageService);
        lenient().when(languageService.message("Messages.ReloadComplete")).thenReturn("Reload done");
        command = new Reload(registry);
    }

    @Test
    void perform_reloadsAndSendsMessage() {
        try (MockedStatic<HeadBlocks> hbStatic = mockStatic(HeadBlocks.class);
             MockedStatic<Bukkit> bukkitStatic = mockStatic(Bukkit.class)) {
            HeadBlocks plugin = mock(HeadBlocks.class);
            hbStatic.when(HeadBlocks::getInstance).thenReturn(plugin);
            lenient().when(plugin.getName()).thenReturn("HeadBlocks");

            HeadBlocks.isHeadDatabaseActive = false;

            bukkitStatic.when(Bukkit::getOnlinePlayers).thenReturn(Collections.emptyList());

            boolean result = command.perform(sender, new String[]{"reload"});

            assertThat(result).isTrue();
            verify(plugin).reloadConfig();
            verify(registry).reload();
            verify(sender).sendMessage("Reload done");
            assertThat(HeadBlocks.isReloadInProgress).isFalse();
        }
    }

    @Test
    void perform_withOnlinePlayers_loadsPlayers() {
        try (MockedStatic<HeadBlocks> hbStatic = mockStatic(HeadBlocks.class);
             MockedStatic<Bukkit> bukkitStatic = mockStatic(Bukkit.class)) {
            HeadBlocks plugin = mock(HeadBlocks.class);
            hbStatic.when(HeadBlocks::getInstance).thenReturn(plugin);
            lenient().when(plugin.getName()).thenReturn("HeadBlocks");

            HeadBlocks.isHeadDatabaseActive = false;

            Player onlinePlayer = mock(Player.class);
            bukkitStatic.when(Bukkit::getOnlinePlayers).thenReturn(Collections.singletonList(onlinePlayer));

            command.perform(sender, new String[]{"reload"});

            verify(storageService).loadPlayers(any(Player[].class));
        }
    }

    @Test
    void tabComplete_returnsEmpty() {
        assertThat(command.tabComplete(sender, new String[]{"reload"})).isEmpty();
    }
}
