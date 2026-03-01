package fr.aerwyn81.headblocks.data.hunt.behavior;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BehaviorResultTest {

    @Test
    void allow_returnsAllowedTrueAndNullMessage() {
        BehaviorResult result = BehaviorResult.allow();

        assertThat(result.allowed()).isTrue();
        assertThat(result.denyMessage()).isNull();
    }

    @Test
    void deny_returnsAllowedFalseWithMessage() {
        BehaviorResult result = BehaviorResult.deny("You can't do that");

        assertThat(result.allowed()).isFalse();
        assertThat(result.denyMessage()).isEqualTo("You can't do that");
    }

    @Test
    void deny_withNullMessage_returnsAllowedFalseAndNullMessage() {
        BehaviorResult result = BehaviorResult.deny(null);

        assertThat(result.allowed()).isFalse();
        assertThat(result.denyMessage()).isNull();
    }

    @Test
    void twoAllows_areEqual() {
        BehaviorResult a = BehaviorResult.allow();
        BehaviorResult b = BehaviorResult.allow();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void allow_isNotEqualToDeny() {
        BehaviorResult allow = BehaviorResult.allow();
        BehaviorResult deny = BehaviorResult.deny("x");

        assertThat(allow).isNotEqualTo(deny);
    }
}
