package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.data.TimedRunData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TimedRunManagerTest {

    @BeforeEach
    void setUp() {
        TimedRunManager.clearAll();
    }

    @Test
    void startRun_makesPlayerInRun() {
        UUID player = UUID.randomUUID();
        TimedRunManager.startRun(player, "hunt-1");

        assertThat(TimedRunManager.isInRun(player)).isTrue();
    }

    @Test
    void startRun_getRun_returnsCorrectData() {
        UUID player = UUID.randomUUID();
        TimedRunManager.startRun(player, "hunt-1");

        TimedRunData data = TimedRunManager.getRun(player);
        assertThat(data).isNotNull();
        assertThat(data.huntId()).isEqualTo("hunt-1");
        assertThat(data.startTimeMillis()).isGreaterThan(0);
    }

    @Test
    void startRun_replacesExistingRun() {
        UUID player = UUID.randomUUID();
        TimedRunManager.startRun(player, "hunt-1");
        TimedRunManager.startRun(player, "hunt-2");

        TimedRunData data = TimedRunManager.getRun(player);
        assertThat(data.huntId()).isEqualTo("hunt-2");
    }

    @Test
    void leaveRun_removesPlayer() {
        UUID player = UUID.randomUUID();
        TimedRunManager.startRun(player, "hunt-1");
        TimedRunManager.leaveRun(player);

        assertThat(TimedRunManager.isInRun(player)).isFalse();
    }

    @Test
    void leaveRun_onAbsentPlayer_doesNotThrow() {
        UUID player = UUID.randomUUID();
        TimedRunManager.leaveRun(player); // should not throw
    }

    @Test
    void isInRun_unknownUUID_returnsFalse() {
        assertThat(TimedRunManager.isInRun(UUID.randomUUID())).isFalse();
    }

    @Test
    void isInRun_withHuntId_trueForCorrectHunt() {
        UUID player = UUID.randomUUID();
        TimedRunManager.startRun(player, "hunt-1");

        assertThat(TimedRunManager.isInRun(player, "hunt-1")).isTrue();
    }

    @Test
    void isInRun_withHuntId_falseForDifferentHunt() {
        UUID player = UUID.randomUUID();
        TimedRunManager.startRun(player, "hunt-1");

        assertThat(TimedRunManager.isInRun(player, "hunt-2")).isFalse();
    }

    @Test
    void isInRun_withHuntId_falseForAbsentPlayer() {
        assertThat(TimedRunManager.isInRun(UUID.randomUUID(), "hunt-1")).isFalse();
    }

    @Test
    void getRun_absentPlayer_returnsNull() {
        assertThat(TimedRunManager.getRun(UUID.randomUUID())).isNull();
    }

    @Test
    void getElapsedMillis_absentPlayer_returnsZero() {
        assertThat(TimedRunManager.getElapsedMillis(UUID.randomUUID())).isEqualTo(0);
    }

    @Test
    void getElapsedMillis_activeRun_returnsNonNegative() {
        UUID player = UUID.randomUUID();
        TimedRunManager.startRun(player, "hunt-1");

        assertThat(TimedRunManager.getElapsedMillis(player)).isGreaterThanOrEqualTo(0);
    }

    @Test
    void formatTime_zero() {
        assertThat(TimedRunManager.formatTime(0)).isEqualTo("00:00.000");
    }

    @Test
    void formatTime_5seconds() {
        assertThat(TimedRunManager.formatTime(5000)).isEqualTo("00:05.000");
    }

    @Test
    void formatTime_65seconds() {
        assertThat(TimedRunManager.formatTime(65000)).isEqualTo("01:05.000");
    }

    @Test
    void formatTime_withMilliseconds() {
        assertThat(TimedRunManager.formatTime(1234)).isEqualTo("00:01.234");
    }

    @Test
    void formatTime_largeValue() {
        assertThat(TimedRunManager.formatTime(3661230)).isEqualTo("61:01.230");
    }

    @Test
    void formatTime_subSecond_999ms() {
        assertThat(TimedRunManager.formatTime(999)).isEqualTo("00:00.999");
    }

    @Test
    void clearAll_emptiesTheMap() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        TimedRunManager.startRun(p1, "h1");
        TimedRunManager.startRun(p2, "h2");

        TimedRunManager.clearAll();

        assertThat(TimedRunManager.getActiveRuns()).isEmpty();
    }

    @Test
    void getActiveRuns_returnsLiveMap() {
        UUID player = UUID.randomUUID();
        TimedRunManager.startRun(player, "hunt-1");

        assertThat(TimedRunManager.getActiveRuns()).containsKey(player);
    }
}
