package fr.aerwyn81.headblocks.data.hunt.behavior.schedule;

import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.configuration.ConfigurationSection;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

public record RecurringScheduleMode(RecurrenceUnit every, String startRef, Duration duration,
                                    List<TimeSlot> slots) implements ScheduleMode {

    private static final DateTimeFormatter MONTH_DAY_FORMAT = DateTimeFormatter.ofPattern("MM/dd");

    public RecurringScheduleMode(RecurrenceUnit every, String startRef, Duration duration, List<TimeSlot> slots) {
        this.every = every;
        this.startRef = startRef;
        this.duration = duration;
        this.slots = slots != null ? slots : List.of();
    }

    @Override
    public String getModeId() {
        return "recurring";
    }

    @Override
    public DenyReason getDenyReason(LocalDateTime now) {
        if (every == null || startRef == null || duration == null) {
            return DenyReason.NOT_IN_RECURRENCE;
        }

        if (!isInRecurrenceWindow(now)) {
            return DenyReason.NOT_IN_RECURRENCE;
        }

        if (!slots.isEmpty() && slots.stream().noneMatch(s -> s.matches(now))) {
            return DenyReason.OUTSIDE_SLOT;
        }

        return null;
    }

    private boolean isInRecurrenceWindow(LocalDateTime now) {
        // Check the current period and the previous one (for boundary spanning)
        LocalDateTime currentStart = computeOccurrenceStart(now);
        if (currentStart != null) {
            LocalDateTime currentEnd = currentStart.plus(duration);
            if (!now.isBefore(currentStart) && now.isBefore(currentEnd)) {
                return true;
            }
        }

        LocalDateTime previousStart = computePreviousOccurrenceStart(now);
        if (previousStart != null) {
            LocalDateTime previousEnd = previousStart.plus(duration);
            if (!now.isBefore(previousStart) && now.isBefore(previousEnd)) {
                return true;
            }
        }

        return false;
    }

    LocalDateTime computeOccurrenceStart(LocalDateTime now) {
        return switch (every) {
            case YEAR -> computeYearlyStart(now.getYear());
            case MONTH -> computeMonthlyStart(now.getYear(), now.getMonthValue());
            case WEEK -> computeWeeklyStart(now.toLocalDate());
        };
    }

    private LocalDateTime computePreviousOccurrenceStart(LocalDateTime now) {
        return switch (every) {
            case YEAR -> computeYearlyStart(now.getYear() - 1);
            case MONTH -> {
                LocalDate prev = now.toLocalDate().minusMonths(1);
                yield computeMonthlyStart(prev.getYear(), prev.getMonthValue());
            }
            case WEEK -> computeWeeklyStart(now.toLocalDate().minusWeeks(1));
        };
    }

    private LocalDateTime computeYearlyStart(int year) {
        try {
            MonthDay md = MonthDay.parse(startRef, MONTH_DAY_FORMAT);
            return md.atYear(year).atStartOfDay();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private LocalDateTime computeMonthlyStart(int year, int month) {
        try {
            int dayOfMonth = Integer.parseInt(startRef);
            LocalDate date = LocalDate.of(year, month, 1);
            int maxDay = date.lengthOfMonth();
            int day = Math.min(dayOfMonth, maxDay);
            return LocalDate.of(year, month, day).atStartOfDay();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime computeWeeklyStart(LocalDate referenceDate) {
        try {
            DayOfWeek targetDay = DayOfWeek.valueOf(startRef.toUpperCase());
            LocalDate date = referenceDate;
            // Go back to the most recent target day (including today)
            while (date.getDayOfWeek() != targetDay) {
                date = date.minusDays(1);
            }
            return date.atStartOfDay();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String getDisplayInfo() {
        StringBuilder sb = new StringBuilder("Recurring ");
        sb.append(every != null ? every.name().toLowerCase() : "?");
        sb.append(" from ").append(startRef != null ? startRef : "?");
        sb.append(" for ").append(ScheduleDateTimeParser.formatDuration(duration));

        if (!slots.isEmpty()) {
            sb.append(" [");
            sb.append(slots.stream().map(TimeSlot::getDisplayInfo).collect(Collectors.joining(", ")));
            sb.append("]");
        }

        return sb.toString();
    }

    @Override
    public String getDenyDetail(DenyReason reason) {
        if (reason == null) {
            return "";
        }

        return switch (reason) {
            case NOT_IN_RECURRENCE -> getDisplayInfo();
            case OUTSIDE_SLOT -> slots.stream()
                    .map(TimeSlot::getDisplayInfo)
                    .collect(Collectors.joining(", "));
            default -> "";
        };
    }

    @Override
    public void saveTo(ConfigurationSection section) {
        if (every != null) {
            section.set("every", every.name().toLowerCase());
        }
        if (startRef != null) {
            section.set("startRef", startRef);
        }
        if (duration != null) {
            section.set("duration", ScheduleDateTimeParser.formatDuration(duration));
        }
        TimeSlot.saveSlots(section, slots);
    }

    public static RecurringScheduleMode fromConfig(ConfigurationSection section) {
        if (section == null) {
            return new RecurringScheduleMode(null, null, null, List.of());
        }

        RecurrenceUnit every = null;
        String everyStr = section.getString("every");
        if (everyStr != null) {
            try {
                every = RecurrenceUnit.valueOf(everyStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                LogUtil.error("Invalid recurrence unit: \"{0}\"", everyStr);
            }
        }

        String startRef = section.getString("startRef");
        Duration duration = ScheduleDateTimeParser.parseDuration(section.getString("duration"));
        List<TimeSlot> slots = TimeSlot.loadSlots(section);

        return new RecurringScheduleMode(every, startRef, duration, slots);
    }
}
