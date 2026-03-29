package fr.aerwyn81.headblocks.utils.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigUpdaterTest {

    @TempDir
    Path tempDir;

    private InputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    private Function<String, InputStream> resourceLoader(String content) {
        return name -> toStream(content);
    }

    private void callUpdate(Function<String, InputStream> loader, String resourceName, File toUpdate, List<String> ignoredSections) throws Exception {
        Method m = ConfigUpdater.class.getDeclaredMethod("update", Function.class, String.class, File.class, List.class);
        m.setAccessible(true);
        try {
            m.invoke(null, loader, resourceName, toUpdate, ignoredSections);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            if (e.getCause() instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e.getCause();
            }
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> callParseComments(Function<String, InputStream> loader, String resourceName,
                                                  YamlConfiguration defaultConfig) throws Exception {
        Method m = ConfigUpdater.class.getDeclaredMethod("parseComments", Function.class, String.class,
                org.bukkit.configuration.file.FileConfiguration.class);
        m.setAccessible(true);
        try {
            return (Map<String, String>) m.invoke(null, loader, resourceName, defaultConfig);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> callParseIgnoredSections(File toUpdate, YamlConfiguration currentConfig,
                                                         Map<String, String> comments, List<String> ignoredSections) throws Exception {
        Method m = ConfigUpdater.class.getDeclaredMethod("parseIgnoredSections", File.class,
                org.bukkit.configuration.file.FileConfiguration.class, Map.class, List.class);
        m.setAccessible(true);
        try {
            return (Map<String, String>) m.invoke(null, toUpdate, currentConfig, comments, ignoredSections);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }
    }

    private void callRemoveLastKey(StringBuilder keyBuilder) throws Exception {
        Method m = ConfigUpdater.class.getDeclaredMethod("removeLastKey", StringBuilder.class);
        m.setAccessible(true);
        m.invoke(null, keyBuilder);
    }

    private void callAppendNewLine(StringBuilder builder) throws Exception {
        Method m = ConfigUpdater.class.getDeclaredMethod("appendNewLine", StringBuilder.class);
        m.setAccessible(true);
        m.invoke(null, builder);
    }

    // ---- removeLastKey tests ----

    @Test
    void removeLastKey_removesLastDotSeparatedSegment() throws Exception {
        StringBuilder sb = new StringBuilder("key1.key2.key3");
        callRemoveLastKey(sb);
        assertThat(sb.toString()).isEqualTo("key1.key2");
    }

    @Test
    void removeLastKey_singleKey_becomesEmpty() throws Exception {
        StringBuilder sb = new StringBuilder("onlyKey");
        callRemoveLastKey(sb);
        assertThat(sb.toString()).isEmpty();
    }

    @Test
    void removeLastKey_emptyBuilder_staysEmpty() throws Exception {
        StringBuilder sb = new StringBuilder();
        callRemoveLastKey(sb);
        assertThat(sb.toString()).isEmpty();
    }

    @Test
    void removeLastKey_twoKeys_leavesFirst() throws Exception {
        StringBuilder sb = new StringBuilder("parent.child");
        callRemoveLastKey(sb);
        assertThat(sb.toString()).isEqualTo("parent");
    }

    // ---- appendNewLine tests ----

    @Test
    void appendNewLine_emptyBuilder_doesNotAppend() throws Exception {
        StringBuilder sb = new StringBuilder();
        callAppendNewLine(sb);
        assertThat(sb.toString()).isEmpty();
    }

    @Test
    void appendNewLine_nonEmptyBuilder_appendsNewline() throws Exception {
        StringBuilder sb = new StringBuilder("content");
        callAppendNewLine(sb);
        assertThat(sb.toString()).isEqualTo("content\n");
    }

    // ---- parseComments tests ----

    @Test
    void parseComments_extractsCommentsForKeys() throws Exception {
        String yaml = """
                # This is a comment for key1
                key1: value1
                # Comment for key2
                # Second line
                key2: value2
                key3: value3
                """;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(toStream(yaml)));
        Map<String, String> comments = callParseComments(resourceLoader(yaml), "test.yml", config);

        assertThat(comments).containsKey("key1");
        assertThat(comments.get("key1")).contains("# This is a comment for key1");

        assertThat(comments).containsKey("key2");
        assertThat(comments.get("key2")).contains("# Comment for key2");
        assertThat(comments.get("key2")).contains("# Second line");

        assertThat(comments).doesNotContainKey("key3");
    }

    @Test
    void parseComments_blankLinesAreTreatedAsComments() throws Exception {
        String yaml = """
                
                key1: value1
                """;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(toStream(yaml)));
        Map<String, String> comments = callParseComments(resourceLoader(yaml), "test.yml", config);

        assertThat(comments).containsKey("key1");
        assertThat(comments.get("key1")).startsWith("\n");
    }

    @Test
    void parseComments_trailingCommentsGoToNullKey() throws Exception {
        String yaml = """
                key1: value1
                # Trailing comment
                """;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(toStream(yaml)));
        Map<String, String> comments = callParseComments(resourceLoader(yaml), "test.yml", config);

        assertThat(comments).containsKey(null);
        assertThat(comments.get(null)).contains("# Trailing comment");
    }

    @Test
    void parseComments_nestedKeys_haveCorrectPaths() throws Exception {
        String yaml = """
                parent:
                  # Comment for child
                  child: value
                """;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(toStream(yaml)));
        Map<String, String> comments = callParseComments(resourceLoader(yaml), "test.yml", config);

        assertThat(comments).containsKey("parent.child");
        assertThat(comments.get("parent.child")).contains("# Comment for child");
    }

    @Test
    void parseComments_nullResource_returnsEmptyMap() throws Exception {
        Function<String, InputStream> loader = name -> null;
        YamlConfiguration config = new YamlConfiguration();

        Map<String, String> comments = callParseComments(loader, "missing.yml", config);

        assertThat(comments).isEmpty();
    }

    // ---- parseIgnoredSections tests ----

    @Test
    void parseIgnoredSections_noIgnoredSections_returnsEmpty() throws Exception {
        String yaml = """
                key1: value1
                key2: value2
                """;
        File file = tempDir.resolve("config.yml").toFile();
        Files.writeString(file.toPath(), yaml);

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Map<String, String> result = callParseIgnoredSections(file, config, Collections.emptyMap(), Collections.emptyList());

        assertThat(result).isEmpty();
    }

    @Test
    void parseIgnoredSections_capturesIgnoredSectionContent() throws Exception {
        String yaml = """
                key1: value1
                ignored:
                  sub1: a
                  sub2: b
                key2: value2
                """;
        File file = tempDir.resolve("config.yml").toFile();
        Files.writeString(file.toPath(), yaml);

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Map<String, String> comments = Collections.emptyMap();
        Map<String, String> result = callParseIgnoredSections(file, config, comments, List.of("ignored"));

        assertThat(result).containsKey("ignored");
        String value = result.get("ignored");
        assertThat(value).contains("ignored:");
        assertThat(value).contains("sub1: a");
        assertThat(value).contains("sub2: b");
    }

    // ---- update (full integration) tests ----

    @Test
    void update_throwsIfFileDoesNotExist() {
        File nonExistent = tempDir.resolve("nope.yml").toFile();

        assertThatThrownBy(() -> callUpdate(resourceLoader("key: val"), "res.yml", nonExistent, Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("doesn't exist");
    }

    @Test
    void update_nullResource_doesNotModifyFile() throws Exception {
        File file = tempDir.resolve("config.yml").toFile();
        String original = "existing: true\n";
        Files.writeString(file.toPath(), original);

        Function<String, InputStream> nullLoader = name -> null;
        callUpdate(nullLoader, "res.yml", file, Collections.emptyList());

        assertThat(Files.readString(file.toPath())).isEqualTo(original);
    }

    @Test
    void update_addsNewKeysFromResource() throws Exception {
        String resource = """
                key1: default1
                key2: default2
                """;

        String existing = "key1: custom1\n";
        File file = tempDir.resolve("config.yml").toFile();
        Files.writeString(file.toPath(), existing);

        callUpdate(resourceLoader(resource), "res.yml", file, Collections.emptyList());

        String result = Files.readString(file.toPath());
        assertThat(result).contains("key1: custom1");
        assertThat(result).contains("key2: default2");
    }

    @Test
    void update_preservesExistingValues() throws Exception {
        String resource = """
                key1: default1
                key2: default2
                """;

        String existing = """
                key1: myValue
                key2: otherValue
                """;
        File file = tempDir.resolve("config.yml").toFile();
        Files.writeString(file.toPath(), existing);

        callUpdate(resourceLoader(resource), "res.yml", file, Collections.emptyList());

        String result = Files.readString(file.toPath());
        assertThat(result).contains("key1: myValue");
        assertThat(result).contains("key2: otherValue");
    }

    @Test
    void update_preservesCommentsFromResource() throws Exception {
        String resource = """
                # Header comment
                key1: default1
                # Comment for key2
                key2: default2
                """;

        String existing = """
                key1: custom1
                key2: custom2
                """;
        File file = tempDir.resolve("config.yml").toFile();
        Files.writeString(file.toPath(), existing);

        callUpdate(resourceLoader(resource), "res.yml", file, Collections.emptyList());

        String result = Files.readString(file.toPath());
        assertThat(result).contains("# Header comment");
        assertThat(result).contains("# Comment for key2");
        assertThat(result).contains("key1: custom1");
        assertThat(result).contains("key2: custom2");
    }

    @Test
    void update_handlesNestedKeys() throws Exception {
        String resource = """
                parent:
                  child1: default1
                  child2: default2
                """;

        String existing = """
                parent:
                  child1: custom1
                """;
        File file = tempDir.resolve("config.yml").toFile();
        Files.writeString(file.toPath(), existing);

        callUpdate(resourceLoader(resource), "res.yml", file, Collections.emptyList());

        String result = Files.readString(file.toPath());
        assertThat(result).contains("child1: custom1");
        assertThat(result).contains("child2: default2");
    }

    @Test
    void update_doesNotWriteIfContentUnchanged() throws Exception {
        String resource = """
                key1: value1
                """;

        File file = tempDir.resolve("config.yml").toFile();
        Files.writeString(file.toPath(), "key1: value1\n");

        callUpdate(resourceLoader(resource), "res.yml", file, Collections.emptyList());

        String result = Files.readString(file.toPath());
        assertThat(result).isEqualTo("key1: value1\n");
    }

    @Test
    void update_preservesListValues() throws Exception {
        String resource = """
                myList:
                - default1
                - default2
                """;

        String existing = """
                myList:
                - custom1
                - custom2
                - custom3
                """;
        File file = tempDir.resolve("config.yml").toFile();
        Files.writeString(file.toPath(), existing);

        callUpdate(resourceLoader(resource), "res.yml", file, Collections.emptyList());

        String result = Files.readString(file.toPath());
        assertThat(result).contains("custom1");
        assertThat(result).contains("custom2");
        assertThat(result).contains("custom3");
    }

    @Test
    void update_handlesIgnoredSections() throws Exception {
        String resource = """
                key1: default1
                ignored:
                  sub: resourceDefault
                key2: default2
                """;

        String existing = """
                key1: custom1
                ignored:
                  sub: userCustom
                  extra: bonus
                key2: custom2
                """;
        File file = tempDir.resolve("config.yml").toFile();
        Files.writeString(file.toPath(), existing);

        callUpdate(resourceLoader(resource), "res.yml", file, List.of("ignored"));

        String result = Files.readString(file.toPath());
        assertThat(result).contains("key1: custom1");
        assertThat(result).contains("key2: custom2");
        assertThat(result).contains("userCustom");
    }

    @Test
    void update_preservesTrailingComments() throws Exception {
        String resource = """
                key1: value1
                # Trailing comment at end of file
                """;

        String existing = """
                key1: custom1
                """;
        File file = tempDir.resolve("config.yml").toFile();
        Files.writeString(file.toPath(), existing);

        callUpdate(resourceLoader(resource), "res.yml", file, Collections.emptyList());

        String result = Files.readString(file.toPath());
        assertThat(result).contains("# Trailing comment at end of file");
    }

    @Test
    void update_handlesEmptyConfigSection() throws Exception {
        String resource = """
                empty_section: {}
                key1: value1
                """;

        String existing = """
                empty_section: {}
                key1: custom1
                """;
        File file = tempDir.resolve("config.yml").toFile();
        Files.writeString(file.toPath(), existing);

        callUpdate(resourceLoader(resource), "res.yml", file, Collections.emptyList());

        String result = Files.readString(file.toPath());
        assertThat(result).contains("empty_section: {}");
        assertThat(result).contains("key1: custom1");
    }

    @Test
    void update_handlesMultipleBlankLinesAsComments() throws Exception {
        String resource = """
                key1: value1
                
                
                key2: value2
                """;

        String existing = """
                key1: custom1
                key2: custom2
                """;
        File file = tempDir.resolve("config.yml").toFile();
        Files.writeString(file.toPath(), existing);

        callUpdate(resourceLoader(resource), "res.yml", file, Collections.emptyList());

        String result = Files.readString(file.toPath());
        assertThat(result).contains("key1: custom1");
        assertThat(result).contains("key2: custom2");
        long blankLineCount = result.lines().filter(String::isEmpty).count();
        assertThat(blankLineCount).isGreaterThanOrEqualTo(2);
    }

    @Test
    void update_removesKeysNotInResource() throws Exception {
        String resource = """
                key1: default1
                """;

        String existing = """
                key1: custom1
                obsolete: shouldBeRemoved
                """;
        File file = tempDir.resolve("config.yml").toFile();
        Files.writeString(file.toPath(), existing);

        callUpdate(resourceLoader(resource), "res.yml", file, Collections.emptyList());

        String result = Files.readString(file.toPath());
        assertThat(result).contains("key1: custom1");
        assertThat(result).doesNotContain("obsolete");
    }
}
