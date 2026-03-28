package fr.aerwyn81.headblocks.data.hunt.behavior.schedule;

import org.bukkit.configuration.ConfigurationSection;

import java.time.LocalDateTime;
import java.util.List;

public record RangeScheduleMode(LocalDateTime start, LocalDateTime end, List<TimeSlot> slots) implements ScheduleMode {

    public RangeScheduleMode(LocalDateTime start, LocalDateTime end, List<TimeSlot> slots) {
        this.start = start;
        this.end = end;
        this.slots = slots != null ? slots : List.of();
    }

    @Override
    public String getModeId() {
        return "range";
    }

    @Override
    public DenyReason getDenyReason(LocalDateTime now) {
        if (start != null && now.isBefore(start)) {
            return DenyReason.NOT_STARTED;
        }

        if (end != null && now.isAfter(end)) {
            return DenyReason.ENDED;
        }

        if (!slots.isEmpty() && slots.stream().noneMatch(s -> s.matches(now))) {
            return DenyReason.OUTSIDE_SLOT;
        }

        return null;
    }

    @Override
    public String getDisplayInfo() {
        String startStr = start != null ? start.format(ScheduleDateTimeParser.DISPLAY_FORMAT) : "∞";
        String endStr = end != null ? end.format(ScheduleDateTimeParser.DISPLAY_FORMAT) : "∞";
        StringBuilder sb = new StringBuilder(startStr + " → " + endStr);

        if (!slots.isEmpty()) {
            sb.append(" [");
            for (int i = 0; i < slots.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(slots.get(i).getDisplayInfo());
            }
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
            case NOT_STARTED -> start != null ? start.format(ScheduleDateTimeParser.DISPLAY_FORMAT) : "";
            case ENDED -> end != null ? end.format(ScheduleDateTimeParser.DISPLAY_FORMAT) : "";
            case OUTSIDE_SLOT -> slots.isEmpty() ? "" : slots.stream()
                    .map(TimeSlot::getDisplayInfo)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            default -> "";
        };
    }

    @Override
    public void saveTo(ConfigurationSection section) {
        if (start != null) {
            section.set("start.date", start.format(ScheduleDateTimeParser.DATE_FORMAT));
            section.set("start.time", start.format(ScheduleDateTimeParser.TIME_FORMAT));
        }
        if (end != null) {
            section.set("end.date", end.format(ScheduleDateTimeParser.DATE_FORMAT));
            section.set("end.time", end.format(ScheduleDateTimeParser.TIME_FORMAT));
        }
        TimeSlot.saveSlots(section, slots);
    }

    public static RangeScheduleMode fromConfig(ConfigurationSection section) {
        LocalDateTime start = ScheduleDateTimeParser.parseDateTime(section, "start");
        LocalDateTime end = ScheduleDateTimeParser.parseDateTime(section, "end");
        List<TimeSlot> slots = TimeSlot.loadSlots(section);
        return new RangeScheduleMode(start, end, slots);
    }
}
