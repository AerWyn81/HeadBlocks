package fr.aerwyn81.headblocks.data.hunt.behavior.schedule;

import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.configuration.ConfigurationSection;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class ScheduleDateTimeParser {

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");

    private ScheduleDateTimeParser() {
    }

    public static LocalDateTime parseDateTime(ConfigurationSection section, String key) {
        if (section == null) {
            return null;
        }

        ConfigurationSection sub = section.getConfigurationSection(key);
        if (sub == null) {
            return null;
        }

        String dateStr = sub.getString("date");
        if (dateStr == null) {
            LogUtil.error("Missing \"date\" field in scheduled behavior section \"{0}\"", key);
            return null;
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            LogUtil.error("Cannot parse scheduled {0} date \"{1}\": expected format MM/dd/yyyy", key, dateStr);
            return null;
        }

        String timeStr = sub.getString("time");
        if (timeStr == null) {
            return date.atStartOfDay();
        }

        try {
            LocalTime time = LocalTime.parse(timeStr, TIME_FORMAT);
            return date.atTime(time);
        } catch (DateTimeParseException e) {
            LogUtil.error("Cannot parse scheduled {0} time \"{1}\": expected format HH:mm", key, timeStr);
            return date.atStartOfDay();
        }
    }

    public static LocalDate parseDate(ConfigurationSection section, String key) {
        if (section == null) {
            return null;
        }

        ConfigurationSection sub = section.getConfigurationSection(key);
        if (sub == null) {
            return null;
        }

        String dateStr = sub.getString("date");
        if (dateStr == null) {
            return null;
        }

        try {
            return LocalDate.parse(dateStr, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            LogUtil.error("Cannot parse scheduled {0} date \"{1}\": expected format MM/dd/yyyy", key, dateStr);
            return null;
        }
    }

    public static Duration parseDuration(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String trimmed = input.trim().toLowerCase();
        if (trimmed.length() < 2) {
            return null;
        }

        char suffix = trimmed.charAt(trimmed.length() - 1);
        String numberPart = trimmed.substring(0, trimmed.length() - 1);

        long value;
        try {
            value = Long.parseLong(numberPart);
        } catch (NumberFormatException e) {
            return null;
        }

        if (value <= 0) {
            return null;
        }

        return switch (suffix) {
            case 'd' -> Duration.ofDays(value);
            case 'w' -> Duration.ofDays(value * 7);
            case 'h' -> Duration.ofHours(value);
            default -> null;
        };
    }

    public static String formatDuration(Duration duration) {
        if (duration == null) {
            return "?";
        }

        long hours = duration.toHours();
        if (hours % (24 * 7) == 0 && hours / (24 * 7) > 0) {
            return (hours / (24 * 7)) + "w";
        }
        if (hours % 24 == 0 && hours / 24 > 0) {
            return (hours / 24) + "d";
        }
        return hours + "h";
    }
}
