package fr.aerwyn81.headblocks.holograms;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnumTypeHologramTest {

    @Test
    void getEnumFromText_DEFAULT_returns() {
        assertThat(EnumTypeHologram.getEnumFromText("DEFAULT")).isEqualTo(EnumTypeHologram.DEFAULT);
    }

    @Test
    void getEnumFromText_ADVANCED_returns() {
        assertThat(EnumTypeHologram.getEnumFromText("ADVANCED")).isEqualTo(EnumTypeHologram.ADVANCED);
    }

    @Test
    void getEnumFromText_invalid_returnsNull() {
        assertThat(EnumTypeHologram.getEnumFromText("UNKNOWN")).isNull();
    }

    @Test
    void getPluginName_DEFAULT_returnsSelf() {
        assertThat(EnumTypeHologram.getPluginName(EnumTypeHologram.DEFAULT)).isEqualTo(EnumTypeHologram.DEFAULT);
    }

    @Test
    void getValue_DEFAULT_returnsDEFAULT() {
        assertThat(EnumTypeHologram.DEFAULT.getValue()).isEqualTo("DEFAULT");
    }

    @Test
    void getValue_ADVANCED_returnsDEFAULT_ADVANCED() {
        assertThat(EnumTypeHologram.ADVANCED.getValue()).isEqualTo("DEFAULT_ADVANCED");
    }
}
