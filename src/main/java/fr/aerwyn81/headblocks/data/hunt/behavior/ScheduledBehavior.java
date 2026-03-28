package fr.aerwyn81.headblocks.data.hunt.behavior;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public record ScheduledBehavior(ServiceRegistry registry, LocalDateTime start, LocalDateTime end) implements Behavior {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");

    @Override
    public String getId() {
        return "scheduled";
    }

    @Override
    public BehaviorResult canPlayerClick(Player player, HeadLocation head, HBHunt hunt) {
        LocalDateTime now = LocalDateTime.now();

        if (start != null && now.isBefore(start)) {
            return BehaviorResult.deny(registry.getLanguageService().message("Hunt.Behavior.ScheduledNotStarted")
                    .replace("%name%", hunt.getDisplayName())
                    .replace("%when%", start.format(DISPLAY_FORMAT)));
        }

        if (end != null && now.isAfter(end)) {
            return BehaviorResult.deny(registry.getLanguageService().message("Hunt.Behavior.ScheduledEnded")
                    .replace("%name%", hunt.getDisplayName())
                    .replace("%when%", end.format(DISPLAY_FORMAT)));
        }

        return BehaviorResult.allow();
    }

    @Override
    public void onHeadFound(Player player, HeadLocation head, HBHunt hunt) {
        // No-op
    }

    @Override
    public String getDisplayInfo(Player player, HBHunt hunt) {
        String startStr = start != null ? start.format(DISPLAY_FORMAT) : "∞";
        String endStr = end != null ? end.format(DISPLAY_FORMAT) : "∞";
        return startStr + " → " + endStr;
    }

    private static LocalDateTime parseDateTime(ConfigurationSection section, String key) {
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

    public static ScheduledBehavior fromConfig(ServiceRegistry registry, ConfigurationSection section) {
        LocalDateTime start = parseDateTime(section, "start");
        LocalDateTime end = parseDateTime(section, "end");
        return new ScheduledBehavior(registry, start, end);
    }
}
