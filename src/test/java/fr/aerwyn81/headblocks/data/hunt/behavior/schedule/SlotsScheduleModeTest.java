package fr.aerwyn81.headblocks.data.hunt.behavior.schedule;

import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SlotsScheduleModeTest {

    @Test
    void getDenyReason_beforeActivePeriod_returnsNotStarted() {
        TimeSlot slot = new TimeSlot(List.of(DayOfWeek.MONDAY), LocalTime.of(10, 0), LocalTime.of(18, 0));
        SlotsScheduleMode mode = new SlotsScheduleMode(List.of(slot),
                LocalDate.of(2025, 6, 1), null);

        // 2025-01-06 is a Monday but before activeFrom
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 1, 6, 12, 0));

        assertThat(reason).isEqualTo(DenyReason.NOT_STARTED);
    }

    @Test
    void getDenyReason_afterActivePeriod_returnsEnded() {
        TimeSlot slot = new TimeSlot(List.of(DayOfWeek.MONDAY), LocalTime.of(10, 0), LocalTime.of(18, 0));
        SlotsScheduleMode mode = new SlotsScheduleMode(List.of(slot),
                null, LocalDate.of(2025, 6, 1));

        // 2025-06-02 is a Monday after activeUntil
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 6, 2, 12, 0));

        assertThat(reason).isEqualTo(DenyReason.ENDED);
    }

    @Test
    void getDenyReason_inActivePeriodAndSlotMatch_returnsNull() {
        TimeSlot slot = new TimeSlot(List.of(DayOfWeek.MONDAY), LocalTime.of(10, 0), LocalTime.of(18, 0));
        SlotsScheduleMode mode = new SlotsScheduleMode(List.of(slot),
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

        // 2025-01-06 is a Monday
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 1, 6, 12, 0));

        assertThat(reason).isNull();
    }

    @Test
    void getDenyReason_inActivePeriodButNoSlotMatch_returnsOutsideSlot() {
        TimeSlot slot = new TimeSlot(List.of(DayOfWeek.MONDAY), LocalTime.of(10, 0), LocalTime.of(18, 0));
        SlotsScheduleMode mode = new SlotsScheduleMode(List.of(slot),
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

        // 2025-01-07 is a Tuesday
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 1, 7, 12, 0));

        assertThat(reason).isEqualTo(DenyReason.OUTSIDE_SLOT);
    }

    @Test
    void getDenyReason_noActivePeriodAndSlotMatch_returnsNull() {
        TimeSlot slot = new TimeSlot(List.of(DayOfWeek.FRIDAY), LocalTime.of(8, 0), LocalTime.of(20, 0));
        SlotsScheduleMode mode = new SlotsScheduleMode(List.of(slot), null, null);

        // 2025-01-10 is a Friday
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 1, 10, 12, 0));

        assertThat(reason).isNull();
    }

    @Test
    void getDenyReason_noActivePeriodAndNoSlotMatch_returnsOutsideSlot() {
        TimeSlot slot = new TimeSlot(List.of(DayOfWeek.FRIDAY), LocalTime.of(8, 0), LocalTime.of(20, 0));
        SlotsScheduleMode mode = new SlotsScheduleMode(List.of(slot), null, null);

        // 2025-01-06 is a Monday
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 1, 6, 12, 0));

        assertThat(reason).isEqualTo(DenyReason.OUTSIDE_SLOT);
    }

    @Test
    void getDenyReason_emptySlots_returnsOutsideSlot() {
        SlotsScheduleMode mode = new SlotsScheduleMode(List.of(), null, null);

        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 1, 6, 12, 0));

        assertThat(reason).isEqualTo(DenyReason.OUTSIDE_SLOT);
    }

    @Test
    void getDenyReason_multipleSlots_matchesAny() {
        TimeSlot weekday = new TimeSlot(List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                LocalTime.of(14, 0), LocalTime.of(18, 0));
        TimeSlot weekend = new TimeSlot(List.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                LocalTime.of(10, 0), LocalTime.of(20, 0));
        SlotsScheduleMode mode = new SlotsScheduleMode(List.of(weekday, weekend), null, null);

        // 2025-01-11 is a Saturday
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 1, 11, 15, 0));

        assertThat(reason).isNull();
    }

    // --- Boundary edge cases ---

    @Test
    void getDenyReason_onExactActiveFromDate_slotMatches_returnsNull() {
        // activeFrom uses isBefore, so the activeFrom date itself is NOT before → allowed
        TimeSlot slot = new TimeSlot(List.of(DayOfWeek.MONDAY), LocalTime.of(10, 0), LocalTime.of(18, 0));
        SlotsScheduleMode mode = new SlotsScheduleMode(List.of(slot),
                LocalDate.of(2025, 1, 6), null);

        // 2025-01-06 is a Monday and is the activeFrom date
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 1, 6, 12, 0));

        assertThat(reason).isNull();
    }

    @Test
    void getDenyReason_onExactActiveUntilDate_slotMatches_returnsNull() {
        // activeUntil uses isAfter, so the activeUntil date itself is NOT after → still active
        TimeSlot slot = new TimeSlot(List.of(DayOfWeek.FRIDAY), LocalTime.of(10, 0), LocalTime.of(18, 0));
        SlotsScheduleMode mode = new SlotsScheduleMode(List.of(slot),
                null, LocalDate.of(2025, 1, 10));

        // 2025-01-10 is a Friday and is the activeUntil date
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 1, 10, 12, 0));

        assertThat(reason).isNull();
    }

    @Test
    void getDenyReason_dayAfterActiveUntil_returnsEnded() {
        TimeSlot slot = new TimeSlot(List.of(DayOfWeek.SATURDAY), LocalTime.of(10, 0), LocalTime.of(18, 0));
        SlotsScheduleMode mode = new SlotsScheduleMode(List.of(slot),
                null, LocalDate.of(2025, 1, 10));

        // 2025-01-11 is a Saturday but after activeUntil
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 1, 11, 12, 0));

        assertThat(reason).isEqualTo(DenyReason.ENDED);
    }

    @Test
    void getDenyReason_dayBeforeActiveFrom_returnsNotStarted() {
        TimeSlot slot = new TimeSlot(List.of(DayOfWeek.SUNDAY), LocalTime.of(10, 0), LocalTime.of(18, 0));
        SlotsScheduleMode mode = new SlotsScheduleMode(List.of(slot),
                LocalDate.of(2025, 1, 6), null);

        // 2025-01-05 is a Sunday but before activeFrom
        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 1, 5, 12, 0));

        assertThat(reason).isEqualTo(DenyReason.NOT_STARTED);
    }

    @Test
    void getDenyReason_nullSlots_treatedAsEmpty() {
        // Constructor normalizes null → empty list
        SlotsScheduleMode mode = new SlotsScheduleMode(null, null, null);

        DenyReason reason = mode.getDenyReason(LocalDateTime.of(2025, 1, 6, 12, 0));

        assertThat(reason).isEqualTo(DenyReason.OUTSIDE_SLOT);
    }

    // --- getDenyDetail edge cases ---

    @Test
    void getDenyDetail_null_returnsEmpty() {
        SlotsScheduleMode mode = new SlotsScheduleMode(List.of(), null, null);

        assertThat(mode.getDenyDetail(null)).isEmpty();
    }

    @Test
    void getDenyDetail_notStarted_returnsFormattedDate() {
        SlotsScheduleMode mode = new SlotsScheduleMode(List.of(),
                LocalDate.of(2026, 3, 15), null);

        assertThat(mode.getDenyDetail(DenyReason.NOT_STARTED)).isEqualTo("03/15/2026");
    }

    @Test
    void getDenyDetail_ended_returnsFormattedDate() {
        SlotsScheduleMode mode = new SlotsScheduleMode(List.of(),
                null, LocalDate.of(2025, 12, 31));

        assertThat(mode.getDenyDetail(DenyReason.ENDED)).isEqualTo("12/31/2025");
    }

    @Test
    void getDenyDetail_notStarted_nullActiveFrom_returnsEmpty() {
        SlotsScheduleMode mode = new SlotsScheduleMode(List.of(), null, null);

        assertThat(mode.getDenyDetail(DenyReason.NOT_STARTED)).isEmpty();
    }

    @Test
    void getDenyDetail_outsideSlot_returnsSlotDisplayInfo() {
        TimeSlot slot = new TimeSlot(List.of(DayOfWeek.MONDAY), LocalTime.of(14, 0), LocalTime.of(18, 0));
        SlotsScheduleMode mode = new SlotsScheduleMode(List.of(slot), null, null);

        String detail = mode.getDenyDetail(DenyReason.OUTSIDE_SLOT);

        assertThat(detail).contains("MON");
        assertThat(detail).contains("14:00-18:00");
    }

    @Test
    void getDenyDetail_notInRecurrence_returnsEmpty() {
        SlotsScheduleMode mode = new SlotsScheduleMode(List.of(), null, null);

        // NOT_IN_RECURRENCE falls into default case → ""
        assertThat(mode.getDenyDetail(DenyReason.NOT_IN_RECURRENCE)).isEmpty();
    }

    // --- Display info edge cases ---

    @Test
    void getDisplayInfo_noSlots_showsNone() {
        SlotsScheduleMode mode = new SlotsScheduleMode(List.of(), null, null);

        assertThat(mode.getDisplayInfo()).contains("(none)");
    }

    @Test
    void getDisplayInfo_noActiveDates_noParentheses() {
        TimeSlot slot = new TimeSlot(List.of(DayOfWeek.MONDAY), LocalTime.of(14, 0), LocalTime.of(18, 0));
        SlotsScheduleMode mode = new SlotsScheduleMode(List.of(slot), null, null);

        String info = mode.getDisplayInfo();

        assertThat(info).doesNotContain("(");
        assertThat(info).doesNotContain("→");
    }

    @Test
    void getDisplayInfo_onlyActiveFrom_showsInfinityForUntil() {
        SlotsScheduleMode mode = new SlotsScheduleMode(List.of(),
                LocalDate.of(2026, 1, 1), null);

        String info = mode.getDisplayInfo();

        assertThat(info).contains("01/01/2026");
        assertThat(info).contains("∞");
    }

    @Test
    void getModeId_returnsSlots() {
        SlotsScheduleMode mode = new SlotsScheduleMode(List.of(), null, null);

        assertThat(mode.getModeId()).isEqualTo("slots");
    }

    @Test
    void getDisplayInfo_formatsCorrectly() {
        TimeSlot slot = new TimeSlot(List.of(DayOfWeek.MONDAY), LocalTime.of(14, 0), LocalTime.of(18, 0));
        SlotsScheduleMode mode = new SlotsScheduleMode(List.of(slot),
                LocalDate.of(2025, 3, 1), LocalDate.of(2025, 6, 30));

        String info = mode.getDisplayInfo();

        assertThat(info).contains("MON 14:00-18:00");
        assertThat(info).contains("03/01/2025");
        assertThat(info).contains("06/30/2025");
    }

    // --- saveTo / fromConfig round-trip ---

    @Test
    void saveTo_fromConfig_roundTrip_preservesAllFields() {
        TimeSlot slot = new TimeSlot(List.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                LocalTime.of(10, 0), LocalTime.of(20, 0));
        SlotsScheduleMode original = new SlotsScheduleMode(List.of(slot),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        MemoryConfiguration config = new MemoryConfiguration();
        original.saveTo(config);

        SlotsScheduleMode loaded = SlotsScheduleMode.fromConfig(config);
        assertThat(loaded.activeFrom()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(loaded.activeUntil()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(loaded.slots()).hasSize(1);
        assertThat(loaded.slots().get(0).days()).containsExactly(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    }

    @Test
    void saveTo_nullDates_doesNotWriteDateSections() {
        SlotsScheduleMode mode = new SlotsScheduleMode(List.of(), null, null);

        MemoryConfiguration config = new MemoryConfiguration();
        mode.saveTo(config);

        assertThat(config.contains("activeFrom")).isFalse();
        assertThat(config.contains("activeUntil")).isFalse();
    }

    @Test
    void fromConfig_emptySection_returnsEmptyMode() {
        MemoryConfiguration config = new MemoryConfiguration();

        SlotsScheduleMode mode = SlotsScheduleMode.fromConfig(config);

        assertThat(mode.activeFrom()).isNull();
        assertThat(mode.activeUntil()).isNull();
        assertThat(mode.slots()).isEmpty();
    }

    @Test
    void constructor_nullSlots_normalizedToEmptyList() {
        SlotsScheduleMode mode = new SlotsScheduleMode(null, null, null);

        assertThat(mode.slots()).isEmpty();
    }
}
