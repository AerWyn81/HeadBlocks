package fr.aerwyn81.headblocks.data.hunt.behavior.schedule;

import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleDateTimeParserTest {

    // --- parseDuration ---

    @Test
    void parseDuration_days_parsesCorrectly() {
        Duration dur = ScheduleDateTimeParser.parseDuration("31d");

        assertThat(dur).isEqualTo(Duration.ofDays(31));
    }

    @Test
    void parseDuration_weeks_parsesCorrectly() {
        Duration dur = ScheduleDateTimeParser.parseDuration("2w");

        assertThat(dur).isEqualTo(Duration.ofDays(14));
    }

    @Test
    void parseDuration_hours_parsesCorrectly() {
        Duration dur = ScheduleDateTimeParser.parseDuration("48h");

        assertThat(dur).isEqualTo(Duration.ofHours(48));
    }

    @Test
    void parseDuration_null_returnsNull() {
        assertThat(ScheduleDateTimeParser.parseDuration(null)).isNull();
    }

    @Test
    void parseDuration_blank_returnsNull() {
        assertThat(ScheduleDateTimeParser.parseDuration("  ")).isNull();
    }

    @Test
    void parseDuration_invalidSuffix_returnsNull() {
        assertThat(ScheduleDateTimeParser.parseDuration("5x")).isNull();
    }

    @Test
    void parseDuration_invalidNumber_returnsNull() {
        assertThat(ScheduleDateTimeParser.parseDuration("abcd")).isNull();
    }

    @Test
    void parseDuration_zero_returnsNull() {
        assertThat(ScheduleDateTimeParser.parseDuration("0d")).isNull();
    }

    @Test
    void parseDuration_negative_returnsNull() {
        assertThat(ScheduleDateTimeParser.parseDuration("-5d")).isNull();
    }

    // --- formatDuration ---

    @Test
    void formatDuration_days_formatsCorrectly() {
        assertThat(ScheduleDateTimeParser.formatDuration(Duration.ofDays(31))).isEqualTo("31d");
    }

    @Test
    void formatDuration_weeks_formatsCorrectly() {
        assertThat(ScheduleDateTimeParser.formatDuration(Duration.ofDays(14))).isEqualTo("2w");
    }

    @Test
    void formatDuration_hours_formatsCorrectly() {
        assertThat(ScheduleDateTimeParser.formatDuration(Duration.ofHours(48))).isEqualTo("2d");
    }

    @Test
    void formatDuration_oddHours_formatsAsHours() {
        assertThat(ScheduleDateTimeParser.formatDuration(Duration.ofHours(5))).isEqualTo("5h");
    }

    @Test
    void formatDuration_null_returnsQuestionMark() {
        assertThat(ScheduleDateTimeParser.formatDuration(null)).isEqualTo("?");
    }

    // --- parseDuration edge cases ---

    @Test
    void parseDuration_minutesSuffix_returnsNull() {
        // "m" (minutes) is not a supported suffix — only d, w, h
        assertThat(ScheduleDateTimeParser.parseDuration("30m")).isNull();
    }

    @Test
    void parseDuration_secondsSuffix_returnsNull() {
        assertThat(ScheduleDateTimeParser.parseDuration("60s")).isNull();
    }

    @Test
    void parseDuration_singleChar_returnsNull() {
        // length < 2 → null
        assertThat(ScheduleDateTimeParser.parseDuration("d")).isNull();
    }

    @Test
    void parseDuration_empty_returnsNull() {
        assertThat(ScheduleDateTimeParser.parseDuration("")).isNull();
    }

    @Test
    void parseDuration_uppercase_parsesCorrectly() {
        // Input is lowercased internally
        assertThat(ScheduleDateTimeParser.parseDuration("5D")).isEqualTo(Duration.ofDays(5));
    }

    @Test
    void parseDuration_leadingTrailingSpaces_parsesCorrectly() {
        assertThat(ScheduleDateTimeParser.parseDuration("  3d  ")).isEqualTo(Duration.ofDays(3));
    }

    @Test
    void parseDuration_largeValue_parsesCorrectly() {
        assertThat(ScheduleDateTimeParser.parseDuration("365d")).isEqualTo(Duration.ofDays(365));
    }

    // --- formatDuration edge cases ---

    @Test
    void formatDuration_zeroDuration_returns0h() {
        // Duration.ZERO has 0 hours — formatDuration doesn't check for zero
        assertThat(ScheduleDateTimeParser.formatDuration(Duration.ZERO)).isEqualTo("0h");
    }

    @Test
    void formatDuration_exactlyOneWeek_returns1w() {
        assertThat(ScheduleDateTimeParser.formatDuration(Duration.ofDays(7))).isEqualTo("1w");
    }

    @Test
    void formatDuration_8days_returns8d() {
        // 8 days is not divisible by 7, so it's formatted as days
        assertThat(ScheduleDateTimeParser.formatDuration(Duration.ofDays(8))).isEqualTo("8d");
    }

    @Test
    void formatDuration_25hours_returns25h() {
        // 25 hours is not divisible by 24, so it's formatted as hours
        assertThat(ScheduleDateTimeParser.formatDuration(Duration.ofHours(25))).isEqualTo("25h");
    }

    // --- parseDateTime ---

    @Test
    void parseDateTime_nullSection_returnsNull() {
        assertThat(ScheduleDateTimeParser.parseDateTime(null, "start")).isNull();
    }

    @Test
    void parseDateTime_missingSubSection_returnsNull() {
        MemoryConfiguration config = new MemoryConfiguration();

        assertThat(ScheduleDateTimeParser.parseDateTime(config, "start")).isNull();
    }

    @Test
    void parseDateTime_missingDate_returnsNull() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.createSection("start");

        assertThat(ScheduleDateTimeParser.parseDateTime(config, "start")).isNull();
    }

    @Test
    void parseDateTime_dateOnly_returnsStartOfDay() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.createSection("start").set("date", "06/15/2026");

        LocalDateTime result = ScheduleDateTimeParser.parseDateTime(config, "start");

        assertThat(result).isEqualTo(LocalDateTime.of(2026, 6, 15, 0, 0));
    }

    @Test
    void parseDateTime_dateAndTime_parsesBoth() {
        MemoryConfiguration config = new MemoryConfiguration();
        var section = config.createSection("start");
        section.set("date", "06/15/2026");
        section.set("time", "14:30");

        LocalDateTime result = ScheduleDateTimeParser.parseDateTime(config, "start");

        assertThat(result).isEqualTo(LocalDateTime.of(2026, 6, 15, 14, 30));
    }

    @Test
    void parseDateTime_invalidDate_returnsNull() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.createSection("start").set("date", "not-a-date");

        assertThat(ScheduleDateTimeParser.parseDateTime(config, "start")).isNull();
    }

    @Test
    void parseDateTime_invalidTime_returnsStartOfDay() {
        MemoryConfiguration config = new MemoryConfiguration();
        var section = config.createSection("start");
        section.set("date", "06/15/2026");
        section.set("time", "bad-time");

        LocalDateTime result = ScheduleDateTimeParser.parseDateTime(config, "start");

        // Invalid time falls back to start of day
        assertThat(result).isEqualTo(LocalDateTime.of(2026, 6, 15, 0, 0));
    }

    // --- parseDate ---

    @Test
    void parseDate_nullSection_returnsNull() {
        assertThat(ScheduleDateTimeParser.parseDate(null, "activeFrom")).isNull();
    }

    @Test
    void parseDate_missingSubSection_returnsNull() {
        MemoryConfiguration config = new MemoryConfiguration();

        assertThat(ScheduleDateTimeParser.parseDate(config, "activeFrom")).isNull();
    }

    @Test
    void parseDate_missingDate_returnsNull() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.createSection("activeFrom");

        assertThat(ScheduleDateTimeParser.parseDate(config, "activeFrom")).isNull();
    }

    @Test
    void parseDate_validDate_parsesCorrectly() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.createSection("activeFrom").set("date", "03/15/2026");

        LocalDate result = ScheduleDateTimeParser.parseDate(config, "activeFrom");

        assertThat(result).isEqualTo(LocalDate.of(2026, 3, 15));
    }

    @Test
    void parseDate_invalidDate_returnsNull() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.createSection("activeFrom").set("date", "invalid");

        assertThat(ScheduleDateTimeParser.parseDate(config, "activeFrom")).isNull();
    }
}
