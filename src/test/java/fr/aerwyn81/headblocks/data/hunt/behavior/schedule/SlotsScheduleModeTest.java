package fr.aerwyn81.headblocks.data.hunt.behavior.schedule;

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
}
