package fr.aerwyn81.headblocks.data.hunt.behavior;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.behavior.schedule.DenyReason;
import fr.aerwyn81.headblocks.data.hunt.behavior.schedule.RangeScheduleMode;
import fr.aerwyn81.headblocks.data.hunt.behavior.schedule.ScheduleMode;
import fr.aerwyn81.headblocks.data.hunt.behavior.schedule.ScheduleModeFactory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.List;

public class ScheduledBehavior implements Behavior {

    private final ServiceRegistry registry;
    private final ScheduleMode scheduleMode;

    public ScheduledBehavior(ServiceRegistry registry, ScheduleMode scheduleMode) {
        this.registry = registry;
        this.scheduleMode = scheduleMode;
    }

    public ScheduledBehavior(ServiceRegistry registry, LocalDateTime start, LocalDateTime end) {
        this(registry, new RangeScheduleMode(start, end, List.of()));
    }

    public ScheduleMode getScheduleMode() {
        return scheduleMode;
    }

    @Override
    public String getId() {
        return "scheduled";
    }

    @Override
    public boolean isAccessGate() {
        return true;
    }

    @Override
    public BehaviorResult canPlayerClick(Player player, HeadLocation head, HBHunt hunt) {
        LocalDateTime now = LocalDateTime.now();
        DenyReason reason = scheduleMode.getDenyReason(now);

        if (reason == null) {
            return BehaviorResult.allow();
        }

        String msgKey = switch (reason) {
            case NOT_STARTED -> "Hunt.Behavior.ScheduledNotStarted";
            case ENDED -> "Hunt.Behavior.ScheduledEnded";
            case OUTSIDE_SLOT -> "Hunt.Behavior.ScheduledOutsideSlot";
            case NOT_IN_RECURRENCE -> "Hunt.Behavior.ScheduledNotInRecurrence";
        };

        String detail = scheduleMode.getDenyDetail(reason);

        return BehaviorResult.deny(registry.getLanguageService().message(msgKey)
                .replace("%name%", hunt.getDisplayName())
                .replace("%when%", detail));
    }

    @Override
    public void onHeadFound(Player player, HeadLocation head, HBHunt hunt) {
        // No-op
    }

    @Override
    public String getDisplayInfo(Player player, HBHunt hunt) {
        return scheduleMode.getDisplayInfo();
    }

    public static ScheduledBehavior fromConfig(ServiceRegistry registry, ConfigurationSection section) {
        return new ScheduledBehavior(registry, ScheduleModeFactory.fromConfig(section));
    }
}
