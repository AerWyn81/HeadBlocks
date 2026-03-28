package fr.aerwyn81.headblocks.data.hunt.behavior.schedule;

import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.configuration.ConfigurationSection;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record TimeSlot(List<DayOfWeek> days, LocalTime from, LocalTime to) {

    public boolean matches(LocalDateTime now) {
        if (!days.contains(now.getDayOfWeek())) {
            return false;
        }

        LocalTime time = now.toLocalTime();
        return !time.isBefore(from) && time.isBefore(to);
    }

    public String getDisplayInfo() {
        String daysStr = days.stream()
                .map(d -> d.name().substring(0, 3))
                .collect(Collectors.joining(","));
        return daysStr + " " + from.format(ScheduleDateTimeParser.TIME_FORMAT)
                + "-" + to.format(ScheduleDateTimeParser.TIME_FORMAT);
    }

    public void saveTo(ConfigurationSection section) {
        List<String> dayNames = days.stream().map(Enum::name).toList();
        section.set("days", dayNames);
        section.set("from", from.format(ScheduleDateTimeParser.TIME_FORMAT));
        section.set("to", to.format(ScheduleDateTimeParser.TIME_FORMAT));
    }

    public static TimeSlot fromConfig(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        List<String> dayNames = section.getStringList("days");
        if (dayNames.isEmpty()) {
            return null;
        }

        List<DayOfWeek> days = new ArrayList<>();
        for (String name : dayNames) {
            try {
                days.add(DayOfWeek.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException e) {
                LogUtil.error("Invalid day of week in time slot: \"{0}\"", name);
            }
        }

        if (days.isEmpty()) {
            return null;
        }

        String fromStr = section.getString("from");
        String toStr = section.getString("to");
        if (fromStr == null || toStr == null) {
            LogUtil.error("Missing \"from\" or \"to\" in time slot configuration");
            return null;
        }

        try {
            LocalTime from = LocalTime.parse(fromStr, ScheduleDateTimeParser.TIME_FORMAT);
            LocalTime to = LocalTime.parse(toStr, ScheduleDateTimeParser.TIME_FORMAT);
            return new TimeSlot(List.copyOf(days), from, to);
        } catch (DateTimeParseException e) {
            LogUtil.error("Cannot parse time slot from/to: \"{0}\" / \"{1}\"", fromStr, toStr);
            return null;
        }
    }

    public static List<TimeSlot> loadSlots(ConfigurationSection parent) {
        List<TimeSlot> slots = new ArrayList<>();
        if (parent == null || !parent.isList("slots")) {
            return slots;
        }

        var slotsList = parent.getMapList("slots");
        for (int i = 0; i < slotsList.size(); i++) {
            var map = slotsList.get(i);
            // Create a temporary section from the map
            ConfigurationSection tempSection = parent.createSection("_temp_slot_" + i, map);
            TimeSlot slot = fromConfig(tempSection);
            if (slot != null) {
                slots.add(slot);
            }
            parent.set("_temp_slot_" + i, null);
        }

        return slots;
    }

    public static void saveSlots(ConfigurationSection parent, List<TimeSlot> slots) {
        if (slots == null || slots.isEmpty()) {
            return;
        }

        List<java.util.Map<String, Object>> slotMaps = new ArrayList<>();
        for (TimeSlot slot : slots) {
            java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("days", slot.days().stream().map(Enum::name).toList());
            map.put("from", slot.from().format(ScheduleDateTimeParser.TIME_FORMAT));
            map.put("to", slot.to().format(ScheduleDateTimeParser.TIME_FORMAT));
            slotMaps.add(map);
        }
        parent.set("slots", slotMaps);
    }
}
