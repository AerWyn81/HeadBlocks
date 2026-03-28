package fr.aerwyn81.headblocks.data.hunt.behavior.schedule;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RangeScheduleModeTest {

    @Test
    void getDenyReason_beforeStart_returnsNotStarted() {
        LocalDateTime start = LocalDateTime.of(2025, 6, 1, 0, 0);
        RangeScheduleMode mode = new RangeScheduleMode(start, null, List.of());

        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 5, 1, 0, 0));

        assertThat(reason).isEqualTo(DenyReason.NOT_STARTED);
    }

    @Test
    void getDenyReason_afterEnd_returnsEnded() {
        LocalDateTime end = LocalDateTime.of(2025, 6, 1, 0, 0);
        RangeScheduleMode mode = new RangeScheduleMode(null, end, List.of());

        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 7, 1, 0, 0));

        assertThat(reason).isEqualTo(DenyReason.ENDED);
    }

    @Test
    void getDenyReason_withinRange_returnsNull() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 12, 31, 23, 59);
        RangeScheduleMode mode = new RangeScheduleMode(start, end, List.of());

        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 6, 15, 12, 0));

        assertThat(reason).isNull();
    }

    @Test
    void getDenyReason_nullBounds_returnsNull() {
        RangeScheduleMode mode = new RangeScheduleMode(null, null, List.of());

        DenyReason reason = mode.getDenyReason(LocalDateTime.now());

        assertThat(reason).isNull();
    }

    @Test
    void getDenyReason_withinRangeButOutsideSlot_returnsOutsideSlot() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 12, 31, 23, 59);
        TimeSlot slot = new TimeSlot(List.of(DayOfWeek.MONDAY), LocalTime.of(14, 0), LocalTime.of(18, 0));
        RangeScheduleMode mode = new RangeScheduleMode(start, end, List.of(slot));

        // 2025-01-07 is a Tuesday
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 1, 7, 15, 0));

        assertThat(reason).isEqualTo(DenyReason.OUTSIDE_SLOT);
    }

    @Test
    void getDenyReason_withinRangeAndInSlot_returnsNull() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 12, 31, 23, 59);
        TimeSlot slot = new TimeSlot(List.of(DayOfWeek.MONDAY), LocalTime.of(14, 0), LocalTime.of(18, 0));
        RangeScheduleMode mode = new RangeScheduleMode(start, end, List.of(slot));

        // 2025-01-06 is a Monday
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 1, 6, 15, 0));

        assertThat(reason).isNull();
    }

    @Test
    void getDenyReason_noSlotsConfigured_ignoresSlotCheck() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
        RangeScheduleMode mode = new RangeScheduleMode(start, null, List.of());

        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 6, 15, 3, 0));

        assertThat(reason).isNull();
    }

    @Test
    void getModeId_returnsRange() {
        RangeScheduleMode mode = new RangeScheduleMode(null, null, List.of());

        assertThat(mode.getModeId()).isEqualTo("range");
    }

    @Test
    void getDisplayInfo_formatsStartAndEnd() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 15, 10, 30);
        LocalDateTime end = LocalDateTime.of(2025, 12, 31, 23, 59);
        RangeScheduleMode mode = new RangeScheduleMode(start, end, List.of());

        assertThat(mode.getDisplayInfo()).isEqualTo("01/15/2025 10:30 → 12/31/2025 23:59");
    }

    @Test
    void getDisplayInfo_nullDatesShowInfinity() {
        RangeScheduleMode mode = new RangeScheduleMode(null, null, List.of());

        assertThat(mode.getDisplayInfo()).isEqualTo("∞ → ∞");
    }

    @Test
    void getDisplayInfo_withSlots_appendsSlotInfo() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
        TimeSlot slot = new TimeSlot(List.of(DayOfWeek.MONDAY), LocalTime.of(14, 0), LocalTime.of(18, 0));
        RangeScheduleMode mode = new RangeScheduleMode(start, null, List.of(slot));

        String info = mode.getDisplayInfo();

        assertThat(info).contains("MON 14:00-18:00");
    }

    @Test
    void getDenyDetail_notStarted_returnsFormattedStart() {
        LocalDateTime start = LocalDateTime.of(2025, 6, 1, 9, 0);
        RangeScheduleMode mode = new RangeScheduleMode(start, null, List.of());

        String detail = mode.getDenyDetail(DenyReason.NOT_STARTED);

        assertThat(detail).isEqualTo("06/01/2025 09:00");
    }

    @Test
    void getDenyDetail_ended_returnsFormattedEnd() {
        LocalDateTime end = LocalDateTime.of(2025, 12, 31, 23, 59);
        RangeScheduleMode mode = new RangeScheduleMode(null, end, List.of());

        String detail = mode.getDenyDetail(DenyReason.ENDED);

        assertThat(detail).isEqualTo("12/31/2025 23:59");
    }
}
