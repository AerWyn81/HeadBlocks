package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.data.TimedRunData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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
    void getRemainingMillis_withNoLimit_returnsMaxValue() {
        assertThat(TimedRunManager.getRemainingMillis(123_456, 0)).isEqualTo(Long.MAX_VALUE);
        assertThat(TimedRunManager.getRemainingMillis(0, -5)).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void getRemainingMillis_beforeLimit_returnsPositiveRemaining() {
        assertThat(TimedRunManager.getRemainingMillis(10_000, 60)).isEqualTo(50_000);
    }

    @Test
    void getRemainingMillis_atLimit_returnsZero() {
        assertThat(TimedRunManager.getRemainingMillis(60_000, 60)).isZero();
    }

    @Test
    void getRemainingMillis_pastLimit_returnsNegative() {
        assertThat(TimedRunManager.getRemainingMillis(61_000, 60)).isEqualTo(-1_000);
    }

    @Test
    void backwardOffset_facingSouth_movesNorth() {
        // yaw 0 = facing south (+z), approach side is north (-z)
        double[] offset = TimedRunManager.backwardOffset(0f);
        assertThat(offset[0]).isCloseTo(0.0, within(1e-9));
        assertThat(offset[1]).isCloseTo(-1.0, within(1e-9));
    }

    @Test
    void backwardOffset_facingNorth_movesSouth() {
        // yaw 180 = facing north (-z), approach side is south (+z)
        double[] offset = TimedRunManager.backwardOffset(180f);
        assertThat(offset[0]).isCloseTo(0.0, within(1e-9));
        assertThat(offset[1]).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void backwardOffset_facingWest_movesEast() {
        // yaw 90 = facing west (-x), approach side is east (+x)
        double[] offset = TimedRunManager.backwardOffset(90f);
        assertThat(offset[0]).isCloseTo(1.0, within(1e-9));
        assertThat(offset[1]).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void backwardOffset_facingEast_movesWest() {
        // yaw 270 = facing east (+x), approach side is west (-x)
        double[] offset = TimedRunManager.backwardOffset(270f);
        assertThat(offset[0]).isCloseTo(-1.0, within(1e-9));
        assertThat(offset[1]).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void leaveAllForHunt_removesOnlyMatchingHunt() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID p3 = UUID.randomUUID();
        TimedRunManager.startRun(p1, "hunt-1");
        TimedRunManager.startRun(p2, "hunt-1");
        TimedRunManager.startRun(p3, "hunt-2");

        TimedRunManager.leaveAllForHunt("hunt-1");

        assertThat(TimedRunManager.isInRun(p1)).isFalse();
        assertThat(TimedRunManager.isInRun(p2)).isFalse();
        assertThat(TimedRunManager.isInRun(p3)).isTrue();
    }

    @Test
    void leaveAllForHunt_unknownHunt_noOp() {
        UUID player = UUID.randomUUID();
        TimedRunManager.startRun(player, "hunt-1");

        TimedRunManager.leaveAllForHunt("hunt-X");

        assertThat(TimedRunManager.isInRun(player)).isTrue();
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
