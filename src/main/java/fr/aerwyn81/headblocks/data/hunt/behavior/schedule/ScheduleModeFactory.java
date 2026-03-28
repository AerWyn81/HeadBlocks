package fr.aerwyn81.headblocks.data.hunt.behavior.schedule;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public final class ScheduleModeFactory {

    private ScheduleModeFactory() {
    }

    public static ScheduleMode fromConfig(ConfigurationSection section) {
        if (section == null) {
            return new RangeScheduleMode(null, null, List.of());
        }

        String mode = section.getString("mode", "range");

        return switch (mode.toLowerCase()) {
            case "slots" -> SlotsScheduleMode.fromConfig(section);
            case "recurring" -> RecurringScheduleMode.fromConfig(section);
            default -> RangeScheduleMode.fromConfig(section);
        };
    }
}
