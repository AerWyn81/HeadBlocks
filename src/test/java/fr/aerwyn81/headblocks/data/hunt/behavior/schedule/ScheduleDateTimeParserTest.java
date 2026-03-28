package fr.aerwyn81.headblocks.data.hunt.behavior.schedule;

import org.junit.jupiter.api.Test;

import java.time.Duration;

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
}
