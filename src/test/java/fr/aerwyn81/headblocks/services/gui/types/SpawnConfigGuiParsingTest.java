package fr.aerwyn81.headblocks.services.gui.types;

import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.SpawnParticle;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
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
    void formatScore_dropsTheUselessFraction() {
        assertThat(MessageUtils.formatScore(3.0)).isEqualTo("3");
        assertThat(MessageUtils.formatScore(2.5)).isEqualTo("2.5");
        assertThat(MessageUtils.formatScore(1.005)).isEqualTo("1.01");
        assertThat(MessageUtils.formatScore(0.1 + 0.2)).isEqualTo("0.3");
    }
}
