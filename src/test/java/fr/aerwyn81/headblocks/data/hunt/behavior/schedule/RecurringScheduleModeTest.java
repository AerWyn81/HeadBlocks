package fr.aerwyn81.headblocks.data.hunt.behavior.schedule;

import org.bukkit.configuration.MemoryConfiguration;
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

    // --- Monthly edge cases ---

    @Test
    void getDenyReason_monthlyDay31_leapYearFebruary_clampsTo29() {
        // Day 31 in leap year February → clamps to 29
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.MONTH, "31", Duration.ofDays(1), List.of());

        // Feb 29 2024 (leap year) should be in window since 31 clamps to 29
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2024, 2, 29, 12, 0));

        assertThat(reason).isNull();
    }

    @Test
    void getDenyReason_monthlyDay31_leapYearFebruary_day28_outsideWindow() {
        // Day 31 clamps to 29 in leap year February — so day 28 is before the window
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.MONTH, "31", Duration.ofDays(1), List.of());

        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2024, 2, 28, 12, 0));

        assertThat(reason).isEqualTo(DenyReason.NOT_IN_RECURRENCE);
    }

    @Test
    void getDenyReason_monthlyDay31_unevenSpacing_april30() {
        // April has 30 days, so day 31 clamps to 30
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.MONTH, "31", Duration.ofDays(1), List.of());

        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 4, 30, 12, 0));

        assertThat(reason).isNull();
    }

    @Test
    void getDenyReason_monthlyDay31_april31DoesNotExist() {
        // April 30 window ends at May 1 00:00, so April 30 23:59 is still in window
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.MONTH, "31", Duration.ofDays(1), List.of());

        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 4, 30, 23, 59));

        assertThat(reason).isNull();
    }

    @Test
    void getDenyReason_monthlyDay31_may31_fullDay() {
        // May has 31 days, so no clamping needed
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.MONTH, "31", Duration.ofDays(1), List.of());

        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 5, 31, 12, 0));

        assertThat(reason).isNull();
    }

    // --- Duration spanning into next month ---

    @Test
    void getDenyReason_monthlyDay28_duration5days_nextMonthDay2InWindow() {
        // Monthly on the 28th for 5 days → window ends on the 2nd/3rd of next month
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.MONTH, "28", Duration.ofDays(5), List.of());

        // March 1st should be in the Feb 28 window (Feb 28 + 5d = March 5)
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 3, 1, 12, 0));

        assertThat(reason).isNull();
    }

    @Test
    void getDenyReason_monthlyDay28_duration5days_nextMonthDay5OutOfWindow() {
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.MONTH, "28", Duration.ofDays(5), List.of());

        // March 5 at midnight is exactly the end (exclusive) → outside
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 3, 5, 0, 0));

        assertThat(reason).isEqualTo(DenyReason.NOT_IN_RECURRENCE);
    }

    // --- Weekly boundary spanning ---

    @Test
    void getDenyReason_weeklyFriday_duration3days_sundayInWindow() {
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.WEEK, "FRIDAY", Duration.ofDays(3), List.of());

        // 2025-01-12 is a Sunday — within Friday + 3d window
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 1, 12, 12, 0));

        assertThat(reason).isNull();
    }

    @Test
    void getDenyReason_weeklyFriday_duration3days_mondayOutside() {
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.WEEK, "FRIDAY", Duration.ofDays(3), List.of());

        // 2025-01-13 is a Monday — Friday + 3d = Monday 00:00 (exclusive)
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 1, 13, 0, 0));

        assertThat(reason).isEqualTo(DenyReason.NOT_IN_RECURRENCE);
    }

    // --- computeOccurrenceStart edge cases ---

    @Test
    void computeOccurrenceStart_yearlyInvalidFormat_returnsNull() {
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.YEAR, "not-a-date", Duration.ofDays(1), List.of());

        LocalDateTime start = mode.computeOccurrenceStart(LocalDateTime.of(2025, 6, 1, 0, 0));

        assertThat(start).isNull();
    }

    @Test
    void computeOccurrenceStart_weeklyInvalidDay_returnsNull() {
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.WEEK, "NOTADAY", Duration.ofDays(1), List.of());

        LocalDateTime start = mode.computeOccurrenceStart(LocalDateTime.of(2025, 6, 1, 0, 0));

        assertThat(start).isNull();
    }

    @Test
    void computeOccurrenceStart_monthlyInvalidNumber_returnsNull() {
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.MONTH, "abc", Duration.ofDays(1), List.of());

        LocalDateTime start = mode.computeOccurrenceStart(LocalDateTime.of(2025, 6, 1, 0, 0));

        assertThat(start).isNull();
    }

    // --- getDenyDetail ---

    @Test
    void getDenyDetail_null_returnsEmpty() {
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.YEAR, "12/01", Duration.ofDays(31), List.of());

        assertThat(mode.getDenyDetail(null)).isEmpty();
    }

    @Test
    void getDenyDetail_notInRecurrence_returnsDisplayInfo() {
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.YEAR, "12/01", Duration.ofDays(31), List.of());

        String detail = mode.getDenyDetail(DenyReason.NOT_IN_RECURRENCE);

        assertThat(detail).contains("year");
        assertThat(detail).contains("12/01");
    }

    @Test
    void getDenyDetail_outsideSlot_returnsSlotInfo() {
        TimeSlot slot = new TimeSlot(List.of(DayOfWeek.MONDAY), LocalTime.of(14, 0), LocalTime.of(18, 0));
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.YEAR, "12/01", Duration.ofDays(31), List.of(slot));

        String detail = mode.getDenyDetail(DenyReason.OUTSIDE_SLOT);

        assertThat(detail).contains("MON");
        assertThat(detail).contains("14:00");
    }

    @Test
    void getDenyDetail_notStarted_returnsEmpty() {
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.YEAR, "12/01", Duration.ofDays(31), List.of());

        // NOT_STARTED is not a valid reason for recurring mode, falls to default → ""
        assertThat(mode.getDenyDetail(DenyReason.NOT_STARTED)).isEmpty();
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

    @Test
    void getDisplayInfo_withSlots_appendsSlotsInBrackets() {
        TimeSlot slot = new TimeSlot(List.of(DayOfWeek.MONDAY), LocalTime.of(9, 0), LocalTime.of(17, 0));
        RecurringScheduleMode mode = new RecurringScheduleMode(
                RecurrenceUnit.WEEK, "MONDAY", Duration.ofDays(2), List.of(slot));

        String info = mode.getDisplayInfo();

        assertThat(info).contains("[");
        assertThat(info).contains("MON 09:00-17:00");
    }

    @Test
    void getDisplayInfo_nullFields_showsQuestionMarks() {
        RecurringScheduleMode mode = new RecurringScheduleMode(null, null, null, List.of());

        String info = mode.getDisplayInfo();

        assertThat(info).contains("?");
    }

    // --- saveTo / fromConfig round-trip ---

    @Test
    void saveTo_fromConfig_roundTrip_preservesAllFields() {
        TimeSlot slot = new TimeSlot(List.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
                LocalTime.of(10, 0), LocalTime.of(16, 0));
        RecurringScheduleMode original = new RecurringScheduleMode(
                RecurrenceUnit.MONTH, "15", Duration.ofDays(5), List.of(slot));

        MemoryConfiguration config = new MemoryConfiguration();
        original.saveTo(config);

        RecurringScheduleMode loaded = RecurringScheduleMode.fromConfig(config);
        assertThat(loaded.every()).isEqualTo(RecurrenceUnit.MONTH);
        assertThat(loaded.startRef()).isEqualTo("15");
        assertThat(loaded.duration()).isEqualTo(Duration.ofDays(5));
        assertThat(loaded.slots()).hasSize(1);
        assertThat(loaded.slots().get(0).days()).containsExactly(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY);
    }

    @Test
    void saveTo_nullFields_doesNotWriteNullValues() {
        RecurringScheduleMode mode = new RecurringScheduleMode(null, null, null, List.of());

        MemoryConfiguration config = new MemoryConfiguration();
        mode.saveTo(config);

        assertThat(config.contains("every")).isFalse();
        assertThat(config.contains("startRef")).isFalse();
        assertThat(config.contains("duration")).isFalse();
    }

    @Test
    void fromConfig_nullSection_returnsEmptyMode() {
        RecurringScheduleMode mode = RecurringScheduleMode.fromConfig(null);

        assertThat(mode.every()).isNull();
        assertThat(mode.startRef()).isNull();
        assertThat(mode.duration()).isNull();
        assertThat(mode.slots()).isEmpty();
    }

    @Test
    void fromConfig_invalidEveryValue_returnsNullEvery() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("every", "daily");

        RecurringScheduleMode mode = RecurringScheduleMode.fromConfig(config);

        assertThat(mode.every()).isNull();
    }

    @Test
    void fromConfig_emptySection_returnsNullFields() {
        MemoryConfiguration config = new MemoryConfiguration();

        RecurringScheduleMode mode = RecurringScheduleMode.fromConfig(config);

        assertThat(mode.every()).isNull();
        assertThat(mode.startRef()).isNull();
        assertThat(mode.duration()).isNull();
        assertThat(mode.slots()).isEmpty();
    }

    // --- Null slots normalization ---

    @Test
    void constructor_nullSlots_normalizedToEmptyList() {
        RecurringScheduleMode mode = new RecurringScheduleMode(RecurrenceUnit.YEAR, "12/01", Duration.ofDays(1), null);

        assertThat(mode.slots()).isEmpty();
    }
}
