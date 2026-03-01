package fr.aerwyn81.headblocks.data.reward;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RewardTypeTest {

    @Test
    void of_MESSAGE_returnsMESSAGE() {
        assertThat(RewardType.of("MESSAGE")).isEqualTo(RewardType.MESSAGE);
    }

    @Test
    void of_COMMAND_returnsCOMMAND() {
        assertThat(RewardType.of("COMMAND")).isEqualTo(RewardType.COMMAND);
    }

    @Test
    void of_BROADCAST_returnsBROADCAST() {
        assertThat(RewardType.of("BROADCAST")).isEqualTo(RewardType.BROADCAST);
    }

    @Test
    void of_UNKNOWN_returnsUNKNOWN() {
        assertThat(RewardType.of("UNKNOWN")).isEqualTo(RewardType.UNKNOWN);
    }

    @Test
    void of_invalidString_returnsNull() {
        assertThat(RewardType.of("invalid")).isNull();
    }

    @Test
    void of_lowercaseMessage_returnsNull_becauseExactMatch() {
        assertThat(RewardType.of("message")).isNull();
    }
}
