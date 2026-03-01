package fr.aerwyn81.headblocks.data;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimedRunDataTest {

    @Test
    void accessors_returnCorrectValues() {
        TimedRunData data = new TimedRunData("hunt-1", 123456789L);

        assertThat(data.huntId()).isEqualTo("hunt-1");
        assertThat(data.startTimeMillis()).isEqualTo(123456789L);
    }

    @Test
    void recordEquality_sameValues_areEqual() {
        TimedRunData a = new TimedRunData("hunt-1", 100L);
        TimedRunData b = new TimedRunData("hunt-1", 100L);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
