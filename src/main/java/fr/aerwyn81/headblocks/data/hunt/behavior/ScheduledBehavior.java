package fr.aerwyn81.headblocks.data.hunt.behavior;

import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public record ScheduledBehavior(LocalDate start, LocalDate end) implements Behavior {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public String getId() {
        return "scheduled";
    }

    @Override
    public BehaviorResult canPlayerClick(Player player, HeadLocation head, Hunt hunt) {
        LocalDate now = LocalDate.now();

        if (start != null && now.isBefore(start)) {
            return BehaviorResult.deny(LanguageService.getMessage("Hunt.Behavior.ScheduledNotStarted"));
        }

        if (end != null && now.isAfter(end)) {
            return BehaviorResult.deny(LanguageService.getMessage("Hunt.Behavior.ScheduledEnded"));
        }

        return BehaviorResult.allow();
    }

    @Override
    public void onHeadFound(Player player, HeadLocation head, Hunt hunt) {
        // No-op
    }

    @Override
    public String getDisplayInfo(Player player, Hunt hunt) {
        String startStr = start != null ? start.format(DATE_FORMAT) : "∞";
        String endStr = end != null ? end.format(DATE_FORMAT) : "∞";
        return startStr + " → " + endStr;
    }

    public static ScheduledBehavior fromConfig(ConfigurationSection section) {
        LocalDate start = null;
        LocalDate end = null;

        if (section != null) {
            String startStr = section.getString("start");
            String endStr = section.getString("end");

            if (startStr != null) {
                try {
                    start = LocalDate.parse(startStr, DATE_FORMAT);
                } catch (DateTimeParseException e) {
                    LogUtil.error("Cannot parse scheduled start date \"{0}\": {1}", startStr, e.getMessage());
                }
            }

            if (endStr != null) {
                try {
                    end = LocalDate.parse(endStr, DATE_FORMAT);
                } catch (DateTimeParseException e) {
                    LogUtil.error("Cannot parse scheduled end date \"{0}\": {1}", endStr, e.getMessage());
                }
            }
        }

        return new ScheduledBehavior(start, end);
    }
}
