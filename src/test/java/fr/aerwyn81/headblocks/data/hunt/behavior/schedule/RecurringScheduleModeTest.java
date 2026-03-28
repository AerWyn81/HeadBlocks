package fr.aerwyn81.headblocks.data.hunt.behavior.schedule;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecurringScheduleModeTest {

    // --- Yearly ---

    @Test
    void getDenyReason_yearlyInWindow_returnsNull() {
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.YEAR, "12/01", Duration.ofDays(31), List.of());

        // Dec 15th is within the Dec 1 - Dec 31 window
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 12, 15, 12, 0));

        assertThat(reason).isNull();
    }

    @Test
    void getDenyReason_yearlyOutsideWindow_returnsNotInRecurrence() {
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.YEAR, "12/01", Duration.ofDays(31), List.of());

        // June is way outside the December window
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 6, 15, 12, 0));

        assertThat(reason).isEqualTo(DenyReason.NOT_IN_RECURRENCE);
    }

    @Test
    void getDenyReason_yearlySpanningYearBoundary_returnsNull() {
        // Dec 15 for 30 days → spans into January
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.YEAR, "12/15", Duration.ofDays(30), List.of());

        // Jan 5 next year is within the window (Dec 15 + 30d = Jan 14)
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2026, 1, 5, 12, 0));

        assertThat(reason).isNull();
    }

    @Test
    void getDenyReason_yearlyExactStart_returnsNull() {
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.YEAR, "06/01", Duration.ofDays(15), List.of());

        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 6, 1, 0, 0));

        assertThat(reason).isNull();
    }

    @Test
    void getDenyReason_yearlyExactEnd_returnsNotInRecurrence() {
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.YEAR, "06/01", Duration.ofDays(15), List.of());

        // June 16 00:00 is exactly at the end (exclusive)
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 6, 16, 0, 0));

        assertThat(reason).isEqualTo(DenyReason.NOT_IN_RECURRENCE);
    }

    // --- Monthly ---

    @Test
    void getDenyReason_monthlyInWindow_returnsNull() {
        // 1st of each month for 3 days
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.MONTH, "1", Duration.ofDays(3), List.of());

        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 3, 2, 12, 0));

        assertThat(reason).isNull();
    }

    @Test
    void getDenyReason_monthlyOutsideWindow_returnsNotInRecurrence() {
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.MONTH, "1", Duration.ofDays(3), List.of());

        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 3, 15, 12, 0));

        assertThat(reason).isEqualTo(DenyReason.NOT_IN_RECURRENCE);
    }

    @Test
    void getDenyReason_monthlyDay31InShortMonth_clampsToDayCount() {
        // 31st of month for 1 day — in February, should clamp to 28th
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.MONTH, "31", Duration.ofDays(1), List.of());

        // Feb 28 2025 (non-leap year) should be in window since 31 clamps to 28
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 2, 28, 12, 0));

        assertThat(reason).isNull();
    }

    // --- Weekly ---

    @Test
    void getDenyReason_weeklyInWindow_returnsNull() {
        // Every Saturday for 2 days (Saturday + Sunday)
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.WEEK, "SATURDAY", Duration.ofDays(2), List.of());

        // 2025-01-11 is a Saturday
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 1, 11, 12, 0));

        assertThat(reason).isNull();
    }

    @Test
    void getDenyReason_weeklyOutsideWindow_returnsNotInRecurrence() {
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.WEEK, "SATURDAY", Duration.ofDays(1), List.of());

        // 2025-01-06 is a Monday
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 1, 6, 12, 0));

        assertThat(reason).isEqualTo(DenyReason.NOT_IN_RECURRENCE);
    }

    @Test
    void getDenyReason_weeklyNextDayInWindow_returnsNull() {
        // Every Friday for 2 days
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.WEEK, "FRIDAY", Duration.ofDays(2), List.of());

        // 2025-01-11 is a Saturday — should be within Friday+2d window
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 1, 11, 12, 0));

        assertThat(reason).isNull();
    }

    // --- With slots ---

    @Test
    void getDenyReason_inWindowButOutsideSlot_returnsOutsideSlot() {
        TimeSlot slot = new TimeSlot(List.of(DayOfWeek.SATURDAY), LocalTime.of(14, 0), LocalTime.of(18, 0));
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.YEAR, "12/01", Duration.ofDays(31), List.of(slot));

        // Dec 15 2025 is a Monday — in recurrence window but not in slot
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 12, 15, 15, 0));

        assertThat(reason).isEqualTo(DenyReason.OUTSIDE_SLOT);
    }

    @Test
    void getDenyReason_inWindowAndInSlot_returnsNull() {
        TimeSlot slot = new TimeSlot(List.of(DayOfWeek.SATURDAY), LocalTime.of(14, 0), LocalTime.of(18, 0));
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.YEAR, "12/01", Duration.ofDays(31), List.of(slot));

        // Dec 6 2025 is a Saturday at 15:00
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 12, 6, 15, 0));

        assertThat(reason).isNull();
    }

    // --- Null/missing fields ---

    @Test
    void getDenyReason_nullEvery_returnsNotInRecurrence() {
        RecurringScheduleMode mode = new RecurringScheduleMode(null, "12/01", Duration.ofDays(31), List.of());

        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 12, 15, 12, 0));

        assertThat(reason).isEqualTo(DenyReason.NOT_IN_RECURRENCE);
    }

    @Test
    void getDenyReason_nullStartRef_returnsNotInRecurrence() {
        RecurringScheduleMode mode = new RecurringScheduleMode(RecurrenceUnit.YEAR, null, Duration.ofDays(31), List.of());

        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 12, 15, 12, 0));

        assertThat(reason).isEqualTo(DenyReason.NOT_IN_RECURRENCE);
    }

    @Test
    void getDenyReason_nullDuration_returnsNotInRecurrence() {
        RecurringScheduleMode mode = new RecurringScheduleMode(RecurrenceUnit.YEAR, "12/01", null, List.of());

        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 12, 15, 12, 0));

        assertThat(reason).isEqualTo(DenyReason.NOT_IN_RECURRENCE);
    }

    // --- Meta ---

    @Test
    void getModeId_returnsRecurring() {
        RecurringScheduleMode mode = new RecurringScheduleMode(RecurrenceUnit.YEAR, "12/01", Duration.ofDays(31), List.of());

        assertThat(mode.getModeId()).isEqualTo("recurring");
    }

    @Test
    void getDisplayInfo_formatsCorrectly() {
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.YEAR, "12/01", Duration.ofDays(31), List.of());

        String info = mode.getDisplayInfo();

        assertThat(info).contains("year");
        assertThat(info).contains("12/01");
        assertThat(info).contains("31d");
    }
}
