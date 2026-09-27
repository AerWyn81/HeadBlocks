package fr.aerwyn81.headblocks.services.gui.types;

import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.SpawnParticle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpawnConfigGuiParsingTest {

    @Test
    void parseParticle_nameOnly_usesTheDefaultAmount() {
        assertThat(SpawnConfigGui.parseParticle("flame")).isEqualTo(new SpawnParticle("FLAME", 3, List.of()));
    }

    @Test
    void parseParticle_withAmountAndColors() {
        assertThat(SpawnConfigGui.parseParticle(" DUST  5 255,0,0 0,0,255 "))
                .isEqualTo(new SpawnParticle("DUST", 5, List.of("255,0,0", "0,0,255")));
    }

    @Test
    void parseParticle_invalidInput_isRejected() {
        assertThat(SpawnConfigGui.parseParticle("")).isNull();
        assertThat(SpawnConfigGui.parseParticle("DUST many")).isNull();
        assertThat(SpawnConfigGui.parseParticle("DUST 5 red")).isNull();
    }

    @Test
    void formatDecimal_dropsTheUselessFraction() {
        assertThat(SpawnConfigGui.formatDecimal(3.0)).isEqualTo("3");
        assertThat(SpawnConfigGui.formatDecimal(2.5)).isEqualTo("2.5");
    }
}
