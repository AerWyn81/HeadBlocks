package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.utils.bukkit.PluginProvider;
import fr.aerwyn81.headblocks.utils.config.ConfigUpdater;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LanguageServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private PluginProvider pluginProvider;

    @Mock
    private ConfigService configService;

    private LanguageService languageService;

    private static final String MINIMAL_YAML = """
            Prefix: "&7[HB]"
            TestMessage: "&aHello %prefix% world"
            TestList:
              - "Line1"
              - "Line2"
            Other:
              NameNotSet: "NoName"
            """;

    @BeforeEach
    void setUp() throws Exception {
        when(pluginProvider.getDataFolder()).thenReturn(tempDir.toFile());

        // Create the language dir and pre-populate the en file so loadLanguage() can work
        Path langDir = tempDir.resolve("language");
        Files.createDirectories(langDir);
        Files.writeString(langDir.resolve("messages_en.yml"), MINIMAL_YAML);
        Files.writeString(langDir.resolve("messages_fr.yml"), MINIMAL_YAML);

        when(pluginProvider.getResource("language/messages_en.yml"))
                .thenReturn(new ByteArrayInputStream(MINIMAL_YAML.getBytes(StandardCharsets.UTF_8)));
        when(pluginProvider.getResource("language/messages_fr.yml"))
                .thenReturn(new ByteArrayInputStream(MINIMAL_YAML.getBytes(StandardCharsets.UTF_8)));

        when(configService.language()).thenReturn("en");

        try (MockedStatic<ConfigUpdater> updater = mockStatic(ConfigUpdater.class);
             MockedStatic<MessageUtils> msgUtils = mockStatic(MessageUtils.class)) {
            // ConfigUpdater.update should be a no-op in tests
            updater.when(() -> ConfigUpdater.update(any(PluginProvider.class), anyString(), any(File.class), anyList())).then(invocation -> null);
            // colorize returns input unchanged
            msgUtils.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));
            msgUtils.when(() -> MessageUtils.unColorize(anyString())).thenAnswer(inv -> inv.getArgument(0));

            languageService = new LanguageService(pluginProvider, configService);
        }

        // After construction, push the messages so the internal map is populated
        languageService.pushMessages();
    }

    // =========================================================================
    // checkLanguage
    // =========================================================================

    @Test
    void checkLanguage_existing_file_returns_lang() {
        assertThat(languageService.checkLanguage("en")).isEqualTo("en");
    }

    @Test
    void checkLanguage_missing_file_returns_en() {
        assertThat(languageService.checkLanguage("de")).isEqualTo("en");
    }

    @Test
    void checkLanguage_fr_file_exists_returns_fr() {
        assertThat(languageService.checkLanguage("fr")).isEqualTo("fr");
    }

    // =========================================================================
    // containsMessage
    // =========================================================================

    @Test
    void containsMessage_existing_key_returns_true() {
        assertThat(languageService.containsMessage("Prefix")).isTrue();
    }

    @Test
    void containsMessage_missing_key_returns_false() {
        assertThat(languageService.containsMessage("NonExistentKey")).isFalse();
    }

    // =========================================================================
    // message
    // =========================================================================

    @Test
    void message_replaces_prefix_placeholder() {
        try (MockedStatic<MessageUtils> msgUtils = mockStatic(MessageUtils.class)) {
            msgUtils.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));

            String result = languageService.message("TestMessage");

            assertThat(result).contains("&7[HB]");
            assertThat(result).doesNotContain("%prefix%");
        }
    }

    // =========================================================================
    // messageList
    // =========================================================================

    @Test
    void messageList_returns_list_of_strings() {
        try (MockedStatic<MessageUtils> msgUtils = mockStatic(MessageUtils.class)) {
            msgUtils.when(() -> MessageUtils.colorize(anyString())).thenAnswer(inv -> inv.getArgument(0));

            List<String> result = languageService.messageList("TestList");

            assertThat(result).containsExactly("Line1", "Line2");
        }
    }

    // =========================================================================
    // language / setLang
    // =========================================================================

    @Test
    void language_returns_current_lang() {
        assertThat(languageService.language()).isEqualTo("en");
    }

    @Test
    void setLang_changes_language() {
        languageService.setLang("fr");

        assertThat(languageService.language()).isEqualTo("fr");
    }

    @Test
    void setLang_can_set_arbitrary_language() {
        languageService.setLang("de");

        assertThat(languageService.language()).isEqualTo("de");
    }
}
