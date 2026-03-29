package fr.aerwyn81.headblocks.data.hunt.behavior.schedule;

import org.bukkit.configuration.MemoryConfiguration;
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

    // --- Boundary edge cases ---

    @Test
    void getDenyReason_exactStart_returnsNull() {
        LocalDateTime start = LocalDateTime.of(2025, 6, 1, 10, 0);
        RangeScheduleMode mode = new RangeScheduleMode(start, null, List.of());

        // Exact start time should be allowed (start is inclusive)
        DenyReason reason = mode.getDenyReason(start);

        assertThat(reason).isNull();
    }

    @Test
    void getDenyReason_exactEnd_returnsNull() {
        LocalDateTime end = LocalDateTime.of(2025, 6, 1, 10, 0);
        RangeScheduleMode mode = new RangeScheduleMode(null, end, List.of());

        // Exact end time — isAfter is strict, so exact end is still allowed (inclusive end)
        DenyReason reason = mode.getDenyReason(end);

        assertThat(reason).isNull();
    }

    @Test
    void getDenyReason_oneSecondAfterEnd_returnsEnded() {
        LocalDateTime end = LocalDateTime.of(2025, 6, 1, 10, 0);
        RangeScheduleMode mode = new RangeScheduleMode(null, end, List.of());

        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 6, 1, 10, 0, 1));

        assertThat(reason).isEqualTo(DenyReason.ENDED);
    }

    @Test
    void getDenyReason_oneSecondBeforeEnd_returnsNull() {
        LocalDateTime end = LocalDateTime.of(2025, 6, 1, 10, 0);
        RangeScheduleMode mode = new RangeScheduleMode(null, end, List.of());

        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 6, 1, 9, 59, 59));

        assertThat(reason).isNull();
    }

    @Test
    void getDenyReason_startAfterEnd_alwaysDenied() {
        // If start > end, the range is inverted — checks are independent, so always denied
        LocalDateTime start = LocalDateTime.of(2025, 12, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 6, 1, 0, 0);
        RangeScheduleMode mode = new RangeScheduleMode(start, end, List.of());

        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 9, 1, 0, 0));

        // Sept 1 is before start (Dec 1) → NOT_STARTED (checked first)
        assertThat(reason).isEqualTo(DenyReason.NOT_STARTED);
    }

    @Test
    void getDenyReason_onlyStartSet_neverEnds() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
        RangeScheduleMode mode = new RangeScheduleMode(start, null, List.of());

        // Far future should still be allowed (no end)
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2099, 12, 31, 23, 59));

        assertThat(reason).isNull();
    }

    @Test
    void getDenyReason_onlyEndSet_immediatelyActive() {
        LocalDateTime end = LocalDateTime.of(2025, 12, 31, 0, 0);
        RangeScheduleMode mode = new RangeScheduleMode(null, end, List.of());

        // Far past should be allowed (no start)
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2000, 1, 1, 0, 0));

        assertThat(reason).isNull();
    }

    // --- getDenyDetail edge cases ---

    @Test
    void getDenyDetail_null_returnsEmpty() {
        RangeScheduleMode mode = new RangeScheduleMode(null, null, List.of());

        assertThat(mode.getDenyDetail(null)).isEmpty();
    }

    @Test
    void getDenyDetail_notStarted_nullStart_returnsEmpty() {
        RangeScheduleMode mode = new RangeScheduleMode(null, null, List.of());

        assertThat(mode.getDenyDetail(DenyReason.NOT_STARTED)).isEmpty();
    }

    @Test
    void getDenyDetail_ended_nullEnd_returnsEmpty() {
        RangeScheduleMode mode = new RangeScheduleMode(null, null, List.of());

        assertThat(mode.getDenyDetail(DenyReason.ENDED)).isEmpty();
    }

    @Test
    void getDenyDetail_outsideSlot_returnsSlotInfo() {
        TimeSlot slot = new TimeSlot(List.of(DayOfWeek.WEDNESDAY), LocalTime.of(9, 0), LocalTime.of(12, 0));
        RangeScheduleMode mode = new RangeScheduleMode(null, null, List.of(slot));

        String detail = mode.getDenyDetail(DenyReason.OUTSIDE_SLOT);

        assertThat(detail).contains("WED");
        assertThat(detail).contains("09:00-12:00");
    }

    @Test
    void getDenyDetail_notInRecurrence_returnsEmpty() {
        RangeScheduleMode mode = new RangeScheduleMode(null, null, List.of());

        assertThat(mode.getDenyDetail(DenyReason.NOT_IN_RECURRENCE)).isEmpty();
    }

    // --- saveTo / fromConfig round-trip ---

    @Test
    void saveTo_fromConfig_roundTrip_preservesAllFields() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 15, 14, 30);
        LocalDateTime end = LocalDateTime.of(2026, 12, 31, 23, 59);
        TimeSlot slot = new TimeSlot(List.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                LocalTime.of(9, 0), LocalTime.of(17, 0));
        RangeScheduleMode original = new RangeScheduleMode(start, end, List.of(slot));

        MemoryConfiguration config = new MemoryConfiguration();
        original.saveTo(config);

        RangeScheduleMode loaded = RangeScheduleMode.fromConfig(config);
        assertThat(loaded.start()).isEqualTo(start);
        assertThat(loaded.end()).isEqualTo(end);
        assertThat(loaded.slots()).hasSize(1);
        assertThat(loaded.slots().get(0).days()).containsExactly(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
    }

    @Test
    void fromConfig_nullSection_returnsNullBounds() {
        RangeScheduleMode mode = RangeScheduleMode.fromConfig(new MemoryConfiguration());

        assertThat(mode.start()).isNull();
        assertThat(mode.end()).isNull();
        assertThat(mode.slots()).isEmpty();
    }

    @Test
    void nullSlots_normalizedToEmptyList() {
        RangeScheduleMode mode = new RangeScheduleMode(null, null, null);

        assertThat(mode.slots()).isEmpty();
    }
}
