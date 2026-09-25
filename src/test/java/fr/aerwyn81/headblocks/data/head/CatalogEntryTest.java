package fr.aerwyn81.headblocks.data.head;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogEntryTest {

    @Test
    void string_splitsOnTheFirstColonOnly() {
        var entry = CatalogEntry.parse("item:DIAMOND:1001");

        assertThat(entry).isNotNull();
        assertThat(entry.type()).isEqualTo("item");
        assertThat(entry.value()).isEqualTo("DIAMOND:1001");
        assertThat(entry.raw()).isEqualTo("item:DIAMOND:1001");
    }

    @Test
    void string_typeIsLowercased() {
        assertThat(CatalogEntry.parse("HDB:1234").type()).isEqualTo("hdb");
    }

    @Test
    void string_legacyTextureKeepsItsValue() {
        var entry = CatalogEntry.parse("default:eyJ0ZXh0dXJlcyI6e319");

        assertThat(entry.type()).isEqualTo("default");
        assertThat(entry.value()).isEqualTo("eyJ0ZXh0dXJlcyI6e319");
    }

    @Test
    void string_withoutType_isInvalid() {
        assertThat(CatalogEntry.parse("noSeparator")).isNull();
        assertThat(CatalogEntry.parse(":value")).isNull();
    }

    @Test
    void options_areReadBetweenBrackets() {
        var entry = CatalogEntry.parse("mob:ZOMBIE[Head=DIAMOND_HELMET, hand = IRON_SWORD]");

        assertThat(entry.value()).isEqualTo("ZOMBIE");
        assertThat(entry.options()).containsExactly(Map.entry("head", "DIAMOND_HELMET"), Map.entry("hand", "IRON_SWORD"));
        assertThat(entry.raw()).isEqualTo("mob:ZOMBIE[Head=DIAMOND_HELMET, hand = IRON_SWORD]");
    }

    @Test
    void options_keepTheValueSeparators() {
        var entry = CatalogEntry.parse("item:DIAMOND:1001[scale=2]");

        assertThat(entry.value()).isEqualTo("DIAMOND:1001");
        assertThat(entry.options()).containsExactly(Map.entry("scale", "2"));
    }

    @Test
    void bracketsWithoutOptions_belongToTheValue() {
        assertThat(CatalogEntry.parse("text:[Secret]").value()).isEqualTo("[Secret]");
        assertThat(CatalogEntry.parse("text:[Secret]").options()).isEmpty();
        assertThat(CatalogEntry.parse("text:a=b [x]").value()).isEqualTo("a=b [x]");
        assertThat(CatalogEntry.parse("text:[a=1] after").value()).isEqualTo("[a=1] after");
    }

    @Test
    void withoutOptions_theMapIsEmpty() {
        assertThat(CatalogEntry.parse("block:STONE").options()).isEmpty();
        assertThat(new CatalogEntry("block", "STONE", null, "block:STONE").options()).isEmpty();
    }

    @Test
    void nonStrings_areInvalid() {
        assertThat(CatalogEntry.parse(42)).isNull();
        assertThat(CatalogEntry.parse(null)).isNull();
        assertThat(CatalogEntry.parse(Map.of("type", "mob", "value", "CAT"))).isNull();
    }
}
