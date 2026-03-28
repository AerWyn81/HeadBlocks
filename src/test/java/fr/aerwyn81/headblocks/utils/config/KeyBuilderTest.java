package fr.aerwyn81.headblocks.utils.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KeyBuilderTest {

    // --- isSubKeyOf (static) ---

    @Nested
    class IsSubKeyOfStatic {

        @Test
        void directChild_returnsTrue() {
            assertThat(KeyBuilder.isSubKeyOf("key1", "key1.key2", '.')).isTrue();
        }

        @Test
        void deepChild_returnsTrue() {
            assertThat(KeyBuilder.isSubKeyOf("key1", "key1.key2.key3", '.')).isTrue();
        }

        @Test
        void notChild_returnsFalse() {
            assertThat(KeyBuilder.isSubKeyOf("key1", "key2.key3", '.')).isFalse();
        }

        @Test
        void emptyParent_returnsFalse() {
            assertThat(KeyBuilder.isSubKeyOf("", "key1.key2", '.')).isFalse();
        }

        @Test
        void sameKey_returnsFalse() {
            assertThat(KeyBuilder.isSubKeyOf("key1", "key1", '.')).isFalse();
        }

        @Test
        void subKeyStartsWithParentButNoDot_returnsFalse() {
            // "key1x" starts with "key1" but is not a sub-key
            assertThat(KeyBuilder.isSubKeyOf("key1", "key1x", '.')).isFalse();
        }

        @Test
        void customSeparator_returnsTrue() {
            assertThat(KeyBuilder.isSubKeyOf("a", "a/b", '/')).isTrue();
        }

        @Test
        void customSeparator_notChild_returnsFalse() {
            assertThat(KeyBuilder.isSubKeyOf("a", "a.b", '/')).isFalse();
        }

        @Test
        void emptySubKey_returnsFalse() {
            assertThat(KeyBuilder.isSubKeyOf("key1", "", '.')).isFalse();
        }
    }

    // --- getIndents (static) ---

    @Nested
    class GetIndents {

        @Test
        void oneLevel_noIndent() {
            assertThat(KeyBuilder.getIndents("key1", '.')).isEmpty();
        }

        @Test
        void twoLevels_twoSpaces() {
            assertThat(KeyBuilder.getIndents("key1.key2", '.')).isEqualTo("  ");
        }

        @Test
        void threeLevels_fourSpaces() {
            assertThat(KeyBuilder.getIndents("key1.key2.key3", '.')).isEqualTo("    ");
        }

        @Test
        void fourLevels_sixSpaces() {
            assertThat(KeyBuilder.getIndents("a.b.c.d", '.')).isEqualTo("      ");
        }

        @Test
        void customSeparator_computesCorrectly() {
            assertThat(KeyBuilder.getIndents("a/b/c", '/')).isEqualTo("    ");
        }
    }

    // --- parseLine + toString ---

    @Nested
    class ParseLine {

        @Test
        void andToString_buildsPath() {
            FileConfiguration config = mock(FileConfiguration.class);
            when(config.contains("key1")).thenReturn(true);
            when(config.contains("key1.key2")).thenReturn(true);

            KeyBuilder kb = new KeyBuilder(config, '.');

            kb.parseLine("key1: value");
            assertThat(kb.toString()).isEqualTo("key1");

            kb.parseLine("key2: value");
            assertThat(kb.toString()).isEqualTo("key1.key2");
        }

        @Test
        void stripsQuotesFromKey() {
            FileConfiguration config = mock(FileConfiguration.class);
            when(config.contains("myKey")).thenReturn(true);

            KeyBuilder kb = new KeyBuilder(config, '.');

            kb.parseLine("'myKey': value");
            assertThat(kb.toString()).isEqualTo("myKey");
        }

        @Test
        void stripsDoubleQuotesFromKey() {
            FileConfiguration config = mock(FileConfiguration.class);
            when(config.contains("myKey")).thenReturn(true);

            KeyBuilder kb = new KeyBuilder(config, '.');

            kb.parseLine("\"myKey\": value");
            assertThat(kb.toString()).isEqualTo("myKey");
        }

        @Test
        void trimsWhitespace() {
            FileConfiguration config = mock(FileConfiguration.class);
            when(config.contains("myKey")).thenReturn(true);

            KeyBuilder kb = new KeyBuilder(config, '.');

            kb.parseLine("  myKey: value  ");
            assertThat(kb.toString()).isEqualTo("myKey");
        }

        @Test
        void invalidPathBacktracks_toValidParent() {
            FileConfiguration config = mock(FileConfiguration.class);
            when(config.contains("key1")).thenReturn(true);
            when(config.contains("key1.key2")).thenReturn(true);
            // key1.key2.key3 does not exist, but key1.key3 does
            when(config.contains("key1.key2.key3")).thenReturn(false);
            when(config.contains("key1.key3")).thenReturn(true);

            KeyBuilder kb = new KeyBuilder(config, '.');

            kb.parseLine("key1: value");
            kb.parseLine("key2: value");
            assertThat(kb.toString()).isEqualTo("key1.key2");

            kb.parseLine("key3: value");
            assertThat(kb.toString()).isEqualTo("key1.key3");
        }

        @Test
        void keyOnlyLine_noValue() {
            FileConfiguration config = mock(FileConfiguration.class);
            when(config.contains("section")).thenReturn(true);

            KeyBuilder kb = new KeyBuilder(config, '.');

            kb.parseLine("section:");
            assertThat(kb.toString()).isEqualTo("section");
        }

        @Test
        void deepNesting_threeLevel() {
            FileConfiguration config = mock(FileConfiguration.class);
            when(config.contains("a")).thenReturn(true);
            when(config.contains("a.b")).thenReturn(true);
            when(config.contains("a.b.c")).thenReturn(true);

            KeyBuilder kb = new KeyBuilder(config, '.');

            kb.parseLine("a:");
            kb.parseLine("b:");
            kb.parseLine("c: value");
            assertThat(kb.toString()).isEqualTo("a.b.c");
        }
    }

    // --- removeLastKey ---

    @Nested
    class RemoveLastKey {

        @Test
        void removesLastSegment() {
            FileConfiguration config = mock(FileConfiguration.class);
            when(config.contains("key1")).thenReturn(true);
            when(config.contains("key1.key2")).thenReturn(true);

            KeyBuilder kb = new KeyBuilder(config, '.');
            kb.parseLine("key1: value");
            kb.parseLine("key2: value");
            assertThat(kb.toString()).isEqualTo("key1.key2");

            kb.removeLastKey();
            assertThat(kb.toString()).isEqualTo("key1");
        }

        @Test
        void singleKey_removesToEmpty() {
            FileConfiguration config = mock(FileConfiguration.class);
            when(config.contains("key1")).thenReturn(true);

            KeyBuilder kb = new KeyBuilder(config, '.');
            kb.parseLine("key1: value");
            assertThat(kb.toString()).isEqualTo("key1");

            kb.removeLastKey();
            assertThat(kb.toString()).isEmpty();
        }

        @Test
        void emptyBuilder_doesNothing() {
            FileConfiguration config = mock(FileConfiguration.class);

            KeyBuilder kb = new KeyBuilder(config, '.');
            kb.removeLastKey();
            assertThat(kb.toString()).isEmpty();
        }

        @Test
        void threeLevel_removesToSecondLevel() {
            FileConfiguration config = mock(FileConfiguration.class);
            when(config.contains("a")).thenReturn(true);
            when(config.contains("a.b")).thenReturn(true);
            when(config.contains("a.b.c")).thenReturn(true);

            KeyBuilder kb = new KeyBuilder(config, '.');
            kb.parseLine("a:");
            kb.parseLine("b:");
            kb.parseLine("c: value");
            assertThat(kb.toString()).isEqualTo("a.b.c");

            kb.removeLastKey();
            assertThat(kb.toString()).isEqualTo("a.b");

            kb.removeLastKey();
            assertThat(kb.toString()).isEqualTo("a");
        }
    }

    // --- isEmpty ---

    @Nested
    class IsEmpty {

        @Test
        void newBuilder_isEmpty() {
            FileConfiguration config = mock(FileConfiguration.class);
            KeyBuilder kb = new KeyBuilder(config, '.');
            assertThat(kb.isEmpty()).isTrue();
        }

        @Test
        void afterParseLine_notEmpty() {
            FileConfiguration config = mock(FileConfiguration.class);
            when(config.contains("key1")).thenReturn(true);

            KeyBuilder kb = new KeyBuilder(config, '.');
            kb.parseLine("key1: value");
            assertThat(kb.isEmpty()).isFalse();
        }

        @Test
        void afterRemoveAll_isEmpty() {
            FileConfiguration config = mock(FileConfiguration.class);
            when(config.contains("key1")).thenReturn(true);

            KeyBuilder kb = new KeyBuilder(config, '.');
            kb.parseLine("key1: value");
            kb.removeLastKey();
            assertThat(kb.isEmpty()).isTrue();
        }
    }

    // --- getLastKey ---

    @Nested
    class GetLastKey {

        @Test
        void emptyBuilder_returnsEmptyString() {
            FileConfiguration config = mock(FileConfiguration.class);
            KeyBuilder kb = new KeyBuilder(config, '.');
            assertThat(kb.getLastKey()).isEmpty();
        }

        @Test
        void singleKey_returnsKey() {
            FileConfiguration config = mock(FileConfiguration.class);
            when(config.contains("key1")).thenReturn(true);

            KeyBuilder kb = new KeyBuilder(config, '.');
            kb.parseLine("key1: value");
            assertThat(kb.getLastKey()).isEqualTo("key1");
        }

        @Test
        void multipleKeys_returnsFirstSegment() {
            FileConfiguration config = mock(FileConfiguration.class);
            when(config.contains("key1")).thenReturn(true);
            when(config.contains("key1.key2")).thenReturn(true);

            KeyBuilder kb = new KeyBuilder(config, '.');
            kb.parseLine("key1: value");
            kb.parseLine("key2: value");
            // getLastKey returns the first segment (split by separator)[0]
            assertThat(kb.getLastKey()).isEqualTo("key1");
        }
    }

    // --- isSubKey (instance) ---

    @Nested
    class IsSubKeyInstance {

        @Test
        void subKeyOfCurrentPath_returnsTrue() {
            FileConfiguration config = mock(FileConfiguration.class);
            when(config.contains("parent")).thenReturn(true);

            KeyBuilder kb = new KeyBuilder(config, '.');
            kb.parseLine("parent:");
            assertThat(kb.isSubKey("parent.child")).isTrue();
        }

        @Test
        void notSubKeyOfCurrentPath_returnsFalse() {
            FileConfiguration config = mock(FileConfiguration.class);
            when(config.contains("parent")).thenReturn(true);

            KeyBuilder kb = new KeyBuilder(config, '.');
            kb.parseLine("parent:");
            assertThat(kb.isSubKey("other.child")).isFalse();
        }
    }

    // --- isSubKeyOf (instance) ---

    @Nested
    class IsSubKeyOfInstance {

        @Test
        void currentPathIsSubKey_returnsTrue() {
            FileConfiguration config = mock(FileConfiguration.class);
            when(config.contains("parent")).thenReturn(true);
            when(config.contains("parent.child")).thenReturn(true);

            KeyBuilder kb = new KeyBuilder(config, '.');
            kb.parseLine("parent:");
            kb.parseLine("child:");
            // "parent.child" isSubKeyOf "parent"
            assertThat(kb.isSubKeyOf("parent")).isTrue();
        }

        @Test
        void currentPathIsNotSubKey_returnsFalse() {
            FileConfiguration config = mock(FileConfiguration.class);
            when(config.contains("other")).thenReturn(true);

            KeyBuilder kb = new KeyBuilder(config, '.');
            kb.parseLine("other:");
            assertThat(kb.isSubKeyOf("parent")).isFalse();
        }
    }

    // --- isConfigSection ---

    @Nested
    class IsConfigSection {

        @Test
        void pathIsConfigSection_returnsTrue() {
            FileConfiguration config = mock(FileConfiguration.class);
            when(config.contains("section")).thenReturn(true);
            when(config.isConfigurationSection("section")).thenReturn(true);

            KeyBuilder kb = new KeyBuilder(config, '.');
            kb.parseLine("section:");
            assertThat(kb.isConfigSection()).isTrue();
        }

        @Test
        void pathIsNotConfigSection_returnsFalse() {
            FileConfiguration config = mock(FileConfiguration.class);
            when(config.contains("leaf")).thenReturn(true);
            when(config.isConfigurationSection("leaf")).thenReturn(false);

            KeyBuilder kb = new KeyBuilder(config, '.');
            kb.parseLine("leaf: value");
            assertThat(kb.isConfigSection()).isFalse();
        }
    }

    // --- isConfigSectionWithKeys ---

    @Nested
    class IsConfigSectionWithKeys {

        @Test
        void sectionWithKeys_returnsTrue() {
            FileConfiguration config = mock(FileConfiguration.class);
            when(config.contains("section")).thenReturn(true);
            when(config.isConfigurationSection("section")).thenReturn(true);
            ConfigurationSection section = mock(ConfigurationSection.class);
            when(config.getConfigurationSection("section")).thenReturn(section);
            when(section.getKeys(false)).thenReturn(Set.of("child1", "child2"));

            KeyBuilder kb = new KeyBuilder(config, '.');
            kb.parseLine("section:");
            assertThat(kb.isConfigSectionWithKeys()).isTrue();
        }

        @Test
        void sectionWithNoKeys_returnsFalse() {
            FileConfiguration config = mock(FileConfiguration.class);
            when(config.contains("section")).thenReturn(true);
            when(config.isConfigurationSection("section")).thenReturn(true);
            ConfigurationSection section = mock(ConfigurationSection.class);
            when(config.getConfigurationSection("section")).thenReturn(section);
            when(section.getKeys(false)).thenReturn(Set.of());

            KeyBuilder kb = new KeyBuilder(config, '.');
            kb.parseLine("section:");
            assertThat(kb.isConfigSectionWithKeys()).isFalse();
        }

        @Test
        void notASection_returnsFalse() {
            FileConfiguration config = mock(FileConfiguration.class);
            when(config.contains("leaf")).thenReturn(true);
            when(config.isConfigurationSection("leaf")).thenReturn(false);

            KeyBuilder kb = new KeyBuilder(config, '.');
            kb.parseLine("leaf: value");
            assertThat(kb.isConfigSectionWithKeys()).isFalse();
        }
    }

}
