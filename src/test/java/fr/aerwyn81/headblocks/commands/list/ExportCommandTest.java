package fr.aerwyn81.headblocks.commands.list;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitScheduler;
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
class ExportCommandTest {

    @Mock
    private ServiceRegistry registry;

    @Mock
    private LanguageService languageService;

    @Mock
    private CommandSender sender;

    private Export command;

    @BeforeEach
    void setUp() {
        lenient().when(registry.getLanguageService()).thenReturn(languageService);
        lenient().when(languageService.message(anyString())).thenReturn("mock-message");
        lenient().when(languageService.prefix()).thenReturn("[HB]");
        command = new Export(registry);
    }

    @Test
    void wrongArgCount_sendsError() {
        boolean result = command.perform(sender, new String[]{"export", "database"});

        assertThat(result).isTrue();
        verify(languageService).message("Messages.ErrorCommand");
    }

    @Test
    void tooManyArgs_sendsError() {
        boolean result = command.perform(sender, new String[]{"export", "database", "sqlite", "extra"});

        assertThat(result).isTrue();
        verify(languageService).message("Messages.ErrorCommand");
    }

    @Test
    void invalidDatabaseType_sendsError() {
        try (MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class)) {
            mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));

            boolean result = command.perform(sender, new String[]{"export", "database", "invalid"});

            assertThat(result).isTrue();
            verify(sender).sendMessage(contains("invalid"));
        }
    }

    @Test
    void validDatabaseType_startsAsyncExport() {
        try (MockedStatic<MessageUtils> mu = mockStatic(MessageUtils.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<HeadBlocks> hb = mockStatic(HeadBlocks.class)) {
            mu.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));

            HeadBlocks plugin = mock(HeadBlocks.class);
            hb.when(HeadBlocks::getInstance).thenReturn(plugin);
            lenient().when(plugin.getName()).thenReturn("HeadBlocks");

            BukkitScheduler scheduler = mock(BukkitScheduler.class);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);

            boolean result = command.perform(sender, new String[]{"export", "database", "SQLite"});

            assertThat(result).isTrue();
            verify(scheduler).runTaskAsynchronously(eq(plugin), any(Runnable.class));
            verify(languageService).message("Messages.ExportInProgress");
        }
    }

    @Nested
    class TabCompletion {

        @Test
        void secondArg_returnsDatabase() {
            ArrayList<String> result = command.tabComplete(sender, new String[]{"export", ""});

            assertThat(result).containsExactly("database");
        }

        @Test
        void thirdArg_returnsDatabaseTypes() {
            ArrayList<String> result = command.tabComplete(sender, new String[]{"export", "database", ""});

            assertThat(result).contains("SQLite", "MySQL");
        }

        @Test
        void fourthArg_returnsEmpty() {
            ArrayList<String> result = command.tabComplete(sender, new String[]{"export", "database", "sqlite", ""});

            assertThat(result).isEmpty();
        }
    }
}
