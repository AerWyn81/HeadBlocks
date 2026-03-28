package fr.aerwyn81.headblocks.data.hunt.behavior.schedule;

import org.bukkit.configuration.ConfigurationSection;

import java.time.LocalDateTime;

public interface ScheduleMode {

    String getModeId();

    DenyReason getDenyReason(LocalDateTime now);

    default boolean isInWindow(LocalDateTime now) {
        return getDenyReason(now) == null;
    }

    String getDisplayInfo();

    String getDenyDetail(DenyReason reason);

    void saveTo(ConfigurationSection section);
}
