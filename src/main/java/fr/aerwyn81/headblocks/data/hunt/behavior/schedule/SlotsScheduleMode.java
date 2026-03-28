package fr.aerwyn81.headblocks.data.hunt.behavior.schedule;

import org.bukkit.configuration.ConfigurationSection;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record SlotsScheduleMode(List<TimeSlot> slots, LocalDate activeFrom,
                                LocalDate activeUntil) implements ScheduleMode {

    public SlotsScheduleMode(List<TimeSlot> slots, LocalDate activeFrom, LocalDate activeUntil) {
        this.slots = slots != null ? slots : List.of();
        this.activeFrom = activeFrom;
        this.activeUntil = activeUntil;
    }

    @Override
    public String getModeId() {
        return "slots";
    }

    @Override
    public DenyReason getDenyReason(LocalDateTime now) {
        LocalDate today = now.toLocalDate();

        if (activeFrom != null && today.isBefore(activeFrom)) {
            return DenyReason.NOT_STARTED;
        }

        if (activeUntil != null && today.isAfter(activeUntil)) {
            return DenyReason.ENDED;
        }

        if (slots.isEmpty() || slots.stream().noneMatch(s -> s.matches(now))) {
            return DenyReason.OUTSIDE_SLOT;
        }

        return null;
    }

    @Override
    public String getDisplayInfo() {
        StringBuilder sb = new StringBuilder("Slots: ");

        if (!slots.isEmpty()) {
            sb.append(slots.stream().map(TimeSlot::getDisplayInfo).collect(Collectors.joining(", ")));
        } else {
            sb.append("(none)");
        }

        if (activeFrom != null || activeUntil != null) {
            String fromStr = activeFrom != null ? activeFrom.format(ScheduleDateTimeParser.DATE_FORMAT) : "∞";
            String untilStr = activeUntil != null ? activeUntil.format(ScheduleDateTimeParser.DATE_FORMAT) : "∞";
            sb.append(" (").append(fromStr).append(" → ").append(untilStr).append(")");
        }

        return sb.toString();
    }

    @Override
    public String getDenyDetail(DenyReason reason) {
        if (reason == null) {
            return "";
        }

        return switch (reason) {
            case NOT_STARTED -> activeFrom != null ? activeFrom.format(ScheduleDateTimeParser.DATE_FORMAT) : "";
            case ENDED -> activeUntil != null ? activeUntil.format(ScheduleDateTimeParser.DATE_FORMAT) : "";
            case OUTSIDE_SLOT -> slots.stream()
                    .map(TimeSlot::getDisplayInfo)
                    .collect(Collectors.joining(", "));
            default -> "";
        };
    }

    @Override
    public void saveTo(ConfigurationSection section) {
        TimeSlot.saveSlots(section, slots);

        if (activeFrom != null) {
            section.set("activeFrom.date", activeFrom.format(ScheduleDateTimeParser.DATE_FORMAT));
        }
        if (activeUntil != null) {
            section.set("activeUntil.date", activeUntil.format(ScheduleDateTimeParser.DATE_FORMAT));
        }
    }

    public static SlotsScheduleMode fromConfig(ConfigurationSection section) {
        List<TimeSlot> slots = TimeSlot.loadSlots(section);
        LocalDate activeFrom = ScheduleDateTimeParser.parseDate(section, "activeFrom");
        LocalDate activeUntil = ScheduleDateTimeParser.parseDate(section, "activeUntil");
        return new SlotsScheduleMode(slots, activeFrom, activeUntil);
    }
}
