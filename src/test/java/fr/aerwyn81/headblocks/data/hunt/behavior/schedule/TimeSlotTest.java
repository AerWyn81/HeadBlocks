package fr.aerwyn81.headblocks.data.hunt.behavior.schedule;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TimeSlotTest {

    @Test
    void matches_dayAndTimeInSlot_returnsTrue() {
        TimeSlot slot = new TimeSlot(
                List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                LocalTime.of(14, 0),
                LocalTime.of(18, 0)
        );

        // 2025-01-06 is a Monday
        LocalDateTime monday1500 = LocalDateTime.of(2025, 1, 6, 15, 0);
        assertThat(slot.matches(monday1500)).isTrue();
    }

    @Test
    void matches_dayInSlotButTimeBefore_returnsFalse() {
        TimeSlot slot = new TimeSlot(
                List.of(DayOfWeek.MONDAY),
                LocalTime.of(14, 0),
                LocalTime.of(18, 0)
        );

        LocalDateTime monday1200 = LocalDateTime.of(2025, 1, 6, 12, 0);
        assertThat(slot.matches(monday1200)).isFalse();
    }

    @Test
    void matches_dayInSlotButTimeAfter_returnsFalse() {
        TimeSlot slot = new TimeSlot(
                List.of(DayOfWeek.MONDAY),
                LocalTime.of(14, 0),
                LocalTime.of(18, 0)
        );

        LocalDateTime monday1900 = LocalDateTime.of(2025, 1, 6, 19, 0);
        assertThat(slot.matches(monday1900)).isFalse();
    }

    @Test
    void matches_dayNotInSlot_returnsFalse() {
        TimeSlot slot = new TimeSlot(
                List.of(DayOfWeek.MONDAY),
                LocalTime.of(14, 0),
                LocalTime.of(18, 0)
        );

        // 2025-01-07 is a Tuesday
        LocalDateTime tuesday1500 = LocalDateTime.of(2025, 1, 7, 15, 0);
        assertThat(slot.matches(tuesday1500)).isFalse();
    }

    @Test
    void matches_exactFromTime_returnsTrue() {
        TimeSlot slot = new TimeSlot(
                List.of(DayOfWeek.MONDAY),
                LocalTime.of(14, 0),
                LocalTime.of(18, 0)
        );

        LocalDateTime mondayExactStart = LocalDateTime.of(2025, 1, 6, 14, 0);
        assertThat(slot.matches(mondayExactStart)).isTrue();
    }

    @Test
    void matches_exactToTime_returnsFalse() {
        TimeSlot slot = new TimeSlot(
                List.of(DayOfWeek.MONDAY),
                LocalTime.of(14, 0),
                LocalTime.of(18, 0)
        );

        // "to" is exclusive
        LocalDateTime mondayExactEnd = LocalDateTime.of(2025, 1, 6, 18, 0);
        assertThat(slot.matches(mondayExactEnd)).isFalse();
    }

    @Test
    void matches_multipleDays_matchesAny() {
        TimeSlot slot = new TimeSlot(
                List.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                LocalTime.of(10, 0),
                LocalTime.of(20, 0)
        );

        // 2025-01-11 is a Saturday
        LocalDateTime saturday = LocalDateTime.of(2025, 1, 11, 15, 0);
        // 2025-01-12 is a Sunday
        LocalDateTime sunday = LocalDateTime.of(2025, 1, 12, 15, 0);

        assertThat(slot.matches(saturday)).isTrue();
        assertThat(slot.matches(sunday)).isTrue();
    }

    @Test
    void getDisplayInfo_formatsCorrectly() {
        TimeSlot slot = new TimeSlot(
                List.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                LocalTime.of(14, 0),
                LocalTime.of(18, 0)
        );

        String info = slot.getDisplayInfo();
        assertThat(info).isEqualTo("MON,FRI 14:00-18:00");
    }
}
