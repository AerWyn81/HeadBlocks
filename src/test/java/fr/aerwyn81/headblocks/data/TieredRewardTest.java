package fr.aerwyn81.headblocks.data;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TieredRewardTest {

    @Test
    void accessors_returnCorrectValues() {
        List<String> messages = List.of("msg1", "msg2");
        List<String> commands = List.of("give %player% diamond 1");
        List<String> broadcasts = List.of("broadcast!");
        TieredReward reward = new TieredReward(5, messages, commands, broadcasts, 3, true);

        assertThat(reward.level()).isEqualTo(5);
        assertThat(reward.messages()).containsExactly("msg1", "msg2");
        assertThat(reward.commands()).containsExactly("give %player% diamond 1");
        assertThat(reward.broadcastMessages()).containsExactly("broadcast!");
        assertThat(reward.slotsRequired()).isEqualTo(3);
        assertThat(reward.isRandom()).isTrue();
    }

    @Test
    void recordEquality_sameValues_areEqual() {
        List<String> msgs = List.of("a");
        List<String> cmds = List.of("b");
        List<String> bcast = List.of("c");
        TieredReward a = new TieredReward(1, msgs, cmds, bcast, 0, false);
        TieredReward b = new TieredReward(1, msgs, cmds, bcast, 0, false);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
