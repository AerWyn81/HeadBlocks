package fr.aerwyn81.headblocks.data.hunt.behavior.schedule;

import org.bukkit.configuration.MemoryConfiguration;
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
    void matches_fromEqualsTo_zeroWidthSlot_alwaysFalse() {
        // A slot where from == to creates a zero-width window → never matches
        TimeSlot slot = new TimeSlot(
                List.of(DayOfWeek.MONDAY),
                LocalTime.of(14, 0),
                LocalTime.of(14, 0)
        );

        // Exact time of from/to — still false because to is exclusive
        LocalDateTime mondayExact = LocalDateTime.of(2025, 1, 6, 14, 0);
        assertThat(slot.matches(mondayExact)).isFalse();
    }

    @Test
    void matches_oneMinuteBeforeTo_returnsTrue() {
        TimeSlot slot = new TimeSlot(
                List.of(DayOfWeek.MONDAY),
                LocalTime.of(14, 0),
                LocalTime.of(18, 0)
        );

        LocalDateTime monday1759 = LocalDateTime.of(2025, 1, 6, 17, 59);
        assertThat(slot.matches(monday1759)).isTrue();
    }

    @Test
    void matches_oneSecondAfterFrom_returnsTrue() {
        TimeSlot slot = new TimeSlot(
                List.of(DayOfWeek.MONDAY),
                LocalTime.of(14, 0),
                LocalTime.of(18, 0)
        );

        LocalDateTime monday1400plus1s = LocalDateTime.of(2025, 1, 6, 14, 0, 1);
        assertThat(slot.matches(monday1400plus1s)).isTrue();
    }

    @Test
    void matches_midnight_slotFromMidnight() {
        // Slot from 00:00 to 06:00
        TimeSlot slot = new TimeSlot(
                List.of(DayOfWeek.SATURDAY),
                LocalTime.of(0, 0),
                LocalTime.of(6, 0)
        );

        // 2025-01-11 is a Saturday at midnight
        LocalDateTime satMidnight = LocalDateTime.of(2025, 1, 11, 0, 0);
        assertThat(slot.matches(satMidnight)).isTrue();
    }

    @Test
    void matches_endOfDay_slotUntilMidnight() {
        // Slot from 20:00 to 23:59
        TimeSlot slot = new TimeSlot(
                List.of(DayOfWeek.SATURDAY),
                LocalTime.of(20, 0),
                LocalTime.of(23, 59)
        );

        // 23:59 is exclusive, so 23:58:59 should match but 23:59:00 should not
        LocalDateTime sat2358 = LocalDateTime.of(2025, 1, 11, 23, 58);
        LocalDateTime sat2359 = LocalDateTime.of(2025, 1, 11, 23, 59);
        assertThat(slot.matches(sat2358)).isTrue();
        assertThat(slot.matches(sat2359)).isFalse();
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

    // --- saveTo / fromConfig round-trip ---

    @Test
    void saveTo_fromConfig_roundTrip() {
        TimeSlot original = new TimeSlot(
                List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                LocalTime.of(9, 0),
                LocalTime.of(17, 30)
        );

        MemoryConfiguration section = new MemoryConfiguration();
        original.saveTo(section);

        TimeSlot loaded = TimeSlot.fromConfig(section);
        assertThat(loaded).isNotNull();
        assertThat(loaded.days()).containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
        assertThat(loaded.from()).isEqualTo(LocalTime.of(9, 0));
        assertThat(loaded.to()).isEqualTo(LocalTime.of(17, 30));
    }

    @Test
    void fromConfig_nullSection_returnsNull() {
        assertThat(TimeSlot.fromConfig(null)).isNull();
    }

    @Test
    void fromConfig_emptyDays_returnsNull() {
        MemoryConfiguration section = new MemoryConfiguration();
        section.set("from", "09:00");
        section.set("to", "17:00");
        // no days → empty list → null

        assertThat(TimeSlot.fromConfig(section)).isNull();
    }

    @Test
    void fromConfig_invalidDayName_skipsInvalidAndLoadsValid() {
        MemoryConfiguration section = new MemoryConfiguration();
        section.set("days", List.of("MONDAY", "INVALID_DAY", "FRIDAY"));
        section.set("from", "10:00");
        section.set("to", "18:00");

        TimeSlot loaded = TimeSlot.fromConfig(section);
        assertThat(loaded).isNotNull();
        assertThat(loaded.days()).containsExactly(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
    }

    @Test
    void fromConfig_allInvalidDays_returnsNull() {
        MemoryConfiguration section = new MemoryConfiguration();
        section.set("days", List.of("NOTADAY", "ALSONOTADAY"));
        section.set("from", "10:00");
        section.set("to", "18:00");

        assertThat(TimeSlot.fromConfig(section)).isNull();
    }

    @Test
    void fromConfig_missingFrom_returnsNull() {
        MemoryConfiguration section = new MemoryConfiguration();
        section.set("days", List.of("MONDAY"));
        section.set("to", "18:00");

        assertThat(TimeSlot.fromConfig(section)).isNull();
    }

    @Test
    void fromConfig_missingTo_returnsNull() {
        MemoryConfiguration section = new MemoryConfiguration();
        section.set("days", List.of("MONDAY"));
        section.set("from", "09:00");

        assertThat(TimeSlot.fromConfig(section)).isNull();
    }

    @Test
    void fromConfig_invalidTimeFormat_returnsNull() {
        MemoryConfiguration section = new MemoryConfiguration();
        section.set("days", List.of("MONDAY"));
        section.set("from", "invalid");
        section.set("to", "18:00");

        assertThat(TimeSlot.fromConfig(section)).isNull();
    }

    // --- saveSlots / loadSlots ---

    @Test
    void saveSlots_loadSlots_roundTrip() {
        TimeSlot slot1 = new TimeSlot(List.of(DayOfWeek.MONDAY), LocalTime.of(8, 0), LocalTime.of(12, 0));
        TimeSlot slot2 = new TimeSlot(List.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), LocalTime.of(10, 0), LocalTime.of(20, 0));

        MemoryConfiguration parent = new MemoryConfiguration();
        TimeSlot.saveSlots(parent, List.of(slot1, slot2));

        List<TimeSlot> loaded = TimeSlot.loadSlots(parent);
        assertThat(loaded).hasSize(2);
        assertThat(loaded.get(0).days()).containsExactly(DayOfWeek.MONDAY);
        assertThat(loaded.get(0).from()).isEqualTo(LocalTime.of(8, 0));
        assertThat(loaded.get(1).days()).containsExactly(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    }

    @Test
    void saveSlots_nullSlots_doesNotWrite() {
        MemoryConfiguration parent = new MemoryConfiguration();
        TimeSlot.saveSlots(parent, null);

        assertThat(parent.contains("slots")).isFalse();
    }

    @Test
    void saveSlots_emptySlots_doesNotWrite() {
        MemoryConfiguration parent = new MemoryConfiguration();
        TimeSlot.saveSlots(parent, List.of());

        assertThat(parent.contains("slots")).isFalse();
    }

    @Test
    void loadSlots_nullParent_returnsEmptyList() {
        assertThat(TimeSlot.loadSlots(null)).isEmpty();
    }

    @Test
    void loadSlots_noSlotsKey_returnsEmptyList() {
        MemoryConfiguration parent = new MemoryConfiguration();
        assertThat(TimeSlot.loadSlots(parent)).isEmpty();
    }
}
