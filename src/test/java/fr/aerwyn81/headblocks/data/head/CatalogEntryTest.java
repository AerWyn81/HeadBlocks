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
    void nonStrings_areInvalid() {
        assertThat(CatalogEntry.parse(42)).isNull();
        assertThat(CatalogEntry.parse(null)).isNull();
        assertThat(CatalogEntry.parse(Map.of("type", "mob", "value", "CAT"))).isNull();
    }
}
