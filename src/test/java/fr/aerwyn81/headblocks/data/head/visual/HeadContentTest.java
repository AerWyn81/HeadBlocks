package fr.aerwyn81.headblocks.data.head.visual;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HeadContentTest {

    @Nested
    class Factories {

        @Test
        void head_keepsTexture() {
            var content = HeadContent.head("abc");

            assertThat(content.kind()).isEqualTo(ContentKind.HEAD);
            assertThat(content.value()).isEqualTo("abc");
            assertThat(content.provider()).isNull();
            assertThat(content.options()).isEmpty();
        }

        @Test
        void head_nullTexture_becomesEmpty() {
            assertThat(HeadContent.head(null).value()).isEmpty();
        }

        @Test
        void external_keepsProvider() {
            var content = HeadContent.external("mythicmobs", "SkeletonKing", Map.of("level", 3));

            assertThat(content.kind()).isEqualTo(ContentKind.EXTERNAL);
            assertThat(content.provider()).isEqualTo("mythicmobs");
            assertThat(content.optionInt("level")).isEqualTo(3);
        }

        @Test
        void options_areImmutableCopies() {
            Map<String, Object> options = new LinkedHashMap<>();
            options.put("baby", true);
            var content = HeadContent.of(ContentKind.MOB, "CAT", options);

            options.put("name", "later");

            assertThat(content.options()).containsOnlyKeys("baby");
        }
    }

    @Nested
    class Options {

        private final HeadContent content = HeadContent.of(ContentKind.MOB, "CAT", Map.of(
                "baby", "true",
                "scale", "1.5",
                "customModelData", 1001.0,
                "broken", "notANumber"));

        @Test
        void optionBoolean_parsesStrings() {
            assertThat(content.optionBoolean("baby", false)).isTrue();
            assertThat(content.optionBoolean("missing", true)).isTrue();
        }

        @Test
        void optionDouble_parsesStringsAndFallsBack() {
            assertThat(content.optionDouble("scale", 1)).isEqualTo(1.5);
            assertThat(content.optionDouble("broken", 2)).isEqualTo(2);
            assertThat(content.optionDouble("missing", 3)).isEqualTo(3);
        }

        @Test
        void optionInt_acceptsDecimalNumbersFromJson() {
            assertThat(content.optionInt("customModelData")).isEqualTo(1001);
            assertThat(content.optionInt("broken")).isNull();
            assertThat(content.optionInt("missing")).isNull();
        }

        @Test
        void option_returnsStringValue() {
            assertThat(content.option("scale")).isEqualTo("1.5");
            assertThat(content.option("missing")).isNull();
        }
    }

    @Nested
    class Json {

        @Test
        void roundTrip_keepsEverything() {
            var content = HeadContent.external("nexo", "christmas_tree", Map.of("name", "Tree"));

            var restored = HeadContent.fromJson(content.toJson());

            assertThat(restored).isEqualTo(content);
        }

        @Test
        void roundTrip_withoutOptions() {
            var content = HeadContent.of(ContentKind.BLOCK, "LANTERN", null);

            assertThat(HeadContent.fromJson(content.toJson())).isEqualTo(content);
        }

        @Test
        void invalidJson_returnsNull() {
            assertThat(HeadContent.fromJson("{not json")).isNull();
            assertThat(HeadContent.fromJson("")).isNull();
            assertThat(HeadContent.fromJson(null)).isNull();
        }

        @Test
        void unknownKind_returnsNull() {
            assertThat(HeadContent.fromJson("{\"kind\":\"PAINTING\",\"value\":\"x\"}")).isNull();
        }

        @Test
        void missingValue_returnsNull() {
            assertThat(HeadContent.fromJson("{\"kind\":\"MOB\"}")).isNull();
        }
    }

    @Nested
    class Yaml {

        @Test
        void saveThenLoad_roundTrips() {
            var yaml = new YamlConfiguration();
            var content = HeadContent.of(ContentKind.MOB, "CAT", Map.of("baby", true, "name", "&7Kitty"));

            content.save(yaml, "content");

            assertThat(HeadContent.load(yaml.getConfigurationSection("content"))).isEqualTo(content);
        }

        @Test
        void save_replacesPreviousOptions() {
            var yaml = new YamlConfiguration();
            HeadContent.of(ContentKind.MOB, "CAT", Map.of("baby", true)).save(yaml, "content");

            HeadContent.of(ContentKind.BLOCK, "LANTERN", null).save(yaml, "content");

            assertThat(yaml.contains("content.options")).isFalse();
            assertThat(yaml.getString("content.kind")).isEqualTo("BLOCK");
        }

        @Test
        void load_nullSection_returnsNull() {
            assertThat(HeadContent.load(null)).isNull();
        }

        @Test
        void load_invalidKind_returnsNull() {
            var yaml = new YamlConfiguration();
            yaml.set("content.kind", "WHATEVER");
            yaml.set("content.value", "x");

            assertThat(HeadContent.load(yaml.getConfigurationSection("content"))).isNull();
        }
    }

    @Test
    void describe_readableForEachKind() {
        assertThat(HeadContent.head("abc").describe()).isEqualTo("head");
        assertThat(HeadContent.of(ContentKind.MOB, "CAT", null).describe()).isEqualTo("mob:CAT");
        assertThat(HeadContent.external("nexo", "tree", null).describe()).isEqualTo("nexo:tree");
    }
}
