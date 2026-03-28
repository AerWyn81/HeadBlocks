package fr.aerwyn81.headblocks.commands.list.schedule;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.behavior.Behavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.ScheduledBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.schedule.*;
import org.bukkit.command.CommandSender;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ScheduleCommandHandler {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final ServiceRegistry registry;

    public ScheduleCommandHandler(ServiceRegistry registry) {
        this.registry = registry;
    }

    public void handle(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntUsage"));
            return;
        }

        String huntId = args[2].toLowerCase();
        HBHunt hunt = registry.getHuntService().getHuntById(huntId);
        if (hunt == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntNotFound")
                    .replace("%hunt%", huntId));
            return;
        }

        String action = args[3].toLowerCase();

        switch (action) {
            case "clear" -> handleClear(sender, hunt, args);
            case "start", "end" -> handleStartEnd(sender, hunt, args, action);
            case "mode" -> handleMode(sender, hunt, args);
            case "addslot" -> handleAddSlot(sender, hunt, args);
            case "removeslot" -> handleRemoveSlot(sender, hunt, args);
            case "every" -> handleEvery(sender, hunt, args);
            case "startref" -> handleStartRef(sender, hunt, args);
            case "duration" -> handleDuration(sender, hunt, args);
            case "activefrom" -> handleActiveDate(sender, hunt, args, "activeFrom");
            case "activeuntil" -> handleActiveDate(sender, hunt, args, "activeUntil");
            case "info" -> handleInfo(sender, hunt);
            default -> sender.sendMessage(registry.getLanguageService().message("Messages.HuntUsage"));
        }
    }

    private ScheduledBehavior findScheduledBehavior(HBHunt hunt) {
        return hunt.getBehaviors().stream()
                .filter(b -> b instanceof ScheduledBehavior)
                .map(b -> (ScheduledBehavior) b)
                .findFirst().orElse(null);
    }

    private ScheduledBehavior getOrCreateScheduledBehavior(HBHunt hunt) {
        ScheduledBehavior existing = findScheduledBehavior(hunt);
        if (existing != null) {
            return existing;
        }
        return new ScheduledBehavior(registry, new RangeScheduleMode(null, null, List.of()));
    }

    private void replaceScheduledBehavior(HBHunt hunt, ScheduleMode newMode) {
        ArrayList<Behavior> behaviors = new ArrayList<>(hunt.getBehaviors());
        behaviors.removeIf(b -> b instanceof ScheduledBehavior);
        behaviors.add(new ScheduledBehavior(registry, newMode));
        hunt.setBehaviors(behaviors);
        registry.getHuntConfigService().saveHunt(hunt);
    }

    // --- clear ---

    private void handleClear(CommandSender sender, HBHunt hunt, String[] args) {
        ScheduledBehavior existing = findScheduledBehavior(hunt);
        if (existing == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntScheduleNoScheduled")
                    .replace("%hunt%", hunt.getId()));
            return;
        }

        String which = args.length >= 5 ? args[4].toLowerCase() : null;
        ArrayList<Behavior> behaviors = new ArrayList<>(hunt.getBehaviors());
        behaviors.remove(existing);

        ScheduleMode mode = existing.getScheduleMode();
        if (mode instanceof RangeScheduleMode rsm) {
            if ("start".equals(which) && rsm.end() != null) {
                behaviors.add(new ScheduledBehavior(registry, new RangeScheduleMode(null, rsm.end(), rsm.slots())));
            } else if ("end".equals(which) && rsm.start() != null) {
                behaviors.add(new ScheduledBehavior(registry, new RangeScheduleMode(rsm.start(), null, rsm.slots())));
            }
        }

        hunt.setBehaviors(behaviors);
        registry.getHuntConfigService().saveHunt(hunt);
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntScheduleCleared")
                .replace("%hunt%", hunt.getId()));
    }

    // --- start / end (range mode) ---

    private void handleStartEnd(CommandSender sender, HBHunt hunt, String[] args, String action) {
        if (args.length < 5) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntUsage"));
            return;
        }

        LocalDate date;
        try {
            date = LocalDate.parse(args[4], DATE_FMT);
        } catch (DateTimeParseException e) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntScheduleInvalidDate"));
            return;
        }

        LocalDateTime datetime;
        if (args.length >= 6) {
            try {
                datetime = date.atTime(LocalTime.parse(args[5], TIME_FMT));
            } catch (DateTimeParseException e) {
                sender.sendMessage(registry.getLanguageService().message("Messages.HuntScheduleInvalidDate"));
                return;
            }
        } else {
            datetime = date.atStartOfDay();
        }

        ScheduledBehavior existing = findScheduledBehavior(hunt);
        RangeScheduleMode currentRange = null;
        if (existing != null && existing.getScheduleMode() instanceof RangeScheduleMode rsm) {
            currentRange = rsm;
        }

        LocalDateTime newStart;
        LocalDateTime newEnd;
        List<TimeSlot> existingSlots;
        if (currentRange != null) {
            newStart = "start".equals(action) ? datetime : currentRange.start();
            newEnd = "end".equals(action) ? datetime : currentRange.end();
            existingSlots = currentRange.slots();
        } else {
            newStart = "start".equals(action) ? datetime : null;
            newEnd = "end".equals(action) ? datetime : null;
            existingSlots = List.of();
        }

        replaceScheduledBehavior(hunt, new RangeScheduleMode(newStart, newEnd, existingSlots));
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntScheduleUpdated")
                .replace("%hunt%", hunt.getId()));
    }

    // --- mode ---

    private void handleMode(CommandSender sender, HBHunt hunt, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntScheduleInvalidMode"));
            return;
        }

        String modeName = args[4].toLowerCase();
        ScheduleMode newMode = switch (modeName) {
            case "range" -> new RangeScheduleMode(null, null, List.of());
            case "slots" -> new SlotsScheduleMode(List.of(), null, null);
            case "recurring" -> new RecurringScheduleMode(null, null, null, List.of());
            default -> null;
        };

        if (newMode == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntScheduleInvalidMode"));
            return;
        }

        replaceScheduledBehavior(hunt, newMode);
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntScheduleModeChanged")
                .replace("%hunt%", hunt.getId())
                .replace("%mode%", modeName));
    }

    // --- addslot ---

    private void handleAddSlot(CommandSender sender, HBHunt hunt, String[] args) {
        // /hb hunt schedule <name> addslot MON,WED,FRI 14:00 18:00
        if (args.length < 7) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntScheduleInvalidSlot"));
            return;
        }

        List<DayOfWeek> days = new ArrayList<>();
        for (String dayStr : args[4].split(",")) {
            try {
                days.add(parseDayOfWeek(dayStr.trim()));
            } catch (IllegalArgumentException e) {
                sender.sendMessage(registry.getLanguageService().message("Messages.HuntScheduleInvalidDay")
                        .replace("%day%", dayStr.trim()));
                return;
            }
        }

        LocalTime from;
        LocalTime to;
        try {
            from = LocalTime.parse(args[5], TIME_FMT);
            to = LocalTime.parse(args[6], TIME_FMT);
        } catch (DateTimeParseException e) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntScheduleInvalidSlot"));
            return;
        }

        TimeSlot newSlot = new TimeSlot(List.copyOf(days), from, to);
        ScheduledBehavior existing = getOrCreateScheduledBehavior(hunt);
        ScheduleMode mode = existing.getScheduleMode();

        ScheduleMode updated = addSlotToMode(mode, newSlot);
        replaceScheduledBehavior(hunt, updated);
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntScheduleSlotAdded")
                .replace("%hunt%", hunt.getId()));
    }

    private ScheduleMode addSlotToMode(ScheduleMode mode, TimeSlot slot) {
        if (mode instanceof RangeScheduleMode rsm) {
            List<TimeSlot> slots = new ArrayList<>(rsm.slots());
            slots.add(slot);
            return new RangeScheduleMode(rsm.start(), rsm.end(), slots);
        }
        if (mode instanceof SlotsScheduleMode ssm) {
            List<TimeSlot> slots = new ArrayList<>(ssm.slots());
            slots.add(slot);
            return new SlotsScheduleMode(slots, ssm.activeFrom(), ssm.activeUntil());
        }
        if (mode instanceof RecurringScheduleMode rsm) {
            List<TimeSlot> slots = new ArrayList<>(rsm.slots());
            slots.add(slot);
            return new RecurringScheduleMode(rsm.every(), rsm.startRef(), rsm.duration(), slots);
        }
        return mode;
    }

    // --- removeslot ---

    private void handleRemoveSlot(CommandSender sender, HBHunt hunt, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntUsage"));
            return;
        }

        int index;
        try {
            index = Integer.parseInt(args[4]) - 1;
        } catch (NumberFormatException e) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntUsage"));
            return;
        }

        ScheduledBehavior existing = findScheduledBehavior(hunt);
        if (existing == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntScheduleNoScheduled")
                    .replace("%hunt%", hunt.getId()));
            return;
        }

        ScheduleMode mode = existing.getScheduleMode();
        ScheduleMode updated = removeSlotFromMode(mode, index);
        if (updated == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntScheduleNoSlots")
                    .replace("%hunt%", hunt.getId()));
            return;
        }

        replaceScheduledBehavior(hunt, updated);
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntScheduleSlotRemoved")
                .replace("%hunt%", hunt.getId()));
    }

    private ScheduleMode removeSlotFromMode(ScheduleMode mode, int index) {
        if (mode instanceof RangeScheduleMode rsm) {
            if (index < 0 || index >= rsm.slots().size()) {
                return null;
            }
            List<TimeSlot> slots = new ArrayList<>(rsm.slots());
            slots.remove(index);
            return new RangeScheduleMode(rsm.start(), rsm.end(), slots);
        }
        if (mode instanceof SlotsScheduleMode ssm) {
            if (index < 0 || index >= ssm.slots().size()) {
                return null;
            }
            List<TimeSlot> slots = new ArrayList<>(ssm.slots());
            slots.remove(index);
            return new SlotsScheduleMode(slots, ssm.activeFrom(), ssm.activeUntil());
        }
        if (mode instanceof RecurringScheduleMode rsm) {
            if (index < 0 || index >= rsm.slots().size()) {
                return null;
            }
            List<TimeSlot> slots = new ArrayList<>(rsm.slots());
            slots.remove(index);
            return new RecurringScheduleMode(rsm.every(), rsm.startRef(), rsm.duration(), slots);
        }
        return null;
    }

    // --- every (recurring) ---

    private void handleEvery(CommandSender sender, HBHunt hunt, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntUsage"));
            return;
        }

        RecurrenceUnit unit;
        try {
            unit = RecurrenceUnit.valueOf(args[4].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntScheduleInvalidMode"));
            return;
        }

        ScheduledBehavior existing = getOrCreateScheduledBehavior(hunt);
        ScheduleMode mode = existing.getScheduleMode();

        RecurringScheduleMode rsm;
        if (mode instanceof RecurringScheduleMode r) {
            rsm = new RecurringScheduleMode(unit, r.startRef(), r.duration(), r.slots());
        } else {
            rsm = new RecurringScheduleMode(unit, null, null, List.of());
        }

        replaceScheduledBehavior(hunt, rsm);
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntScheduleRecurrenceSet")
                .replace("%hunt%", hunt.getId()));
    }

    // --- startref (recurring) ---

    private void handleStartRef(CommandSender sender, HBHunt hunt, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntUsage"));
            return;
        }

        String ref = args[4];
        ScheduledBehavior existing = getOrCreateScheduledBehavior(hunt);
        ScheduleMode mode = existing.getScheduleMode();

        RecurringScheduleMode rsm;
        if (mode instanceof RecurringScheduleMode r) {
            rsm = new RecurringScheduleMode(r.every(), ref, r.duration(), r.slots());
        } else {
            rsm = new RecurringScheduleMode(null, ref, null, List.of());
        }

        replaceScheduledBehavior(hunt, rsm);
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntScheduleRecurrenceSet")
                .replace("%hunt%", hunt.getId()));
    }

    // --- duration (recurring) ---

    private void handleDuration(CommandSender sender, HBHunt hunt, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntUsage"));
            return;
        }

        Duration dur = ScheduleDateTimeParser.parseDuration(args[4]);
        if (dur == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntScheduleInvalidDuration"));
            return;
        }

        ScheduledBehavior existing = getOrCreateScheduledBehavior(hunt);
        ScheduleMode mode = existing.getScheduleMode();

        RecurringScheduleMode rsm;
        if (mode instanceof RecurringScheduleMode r) {
            rsm = new RecurringScheduleMode(r.every(), r.startRef(), dur, r.slots());
        } else {
            rsm = new RecurringScheduleMode(null, null, dur, List.of());
        }

        replaceScheduledBehavior(hunt, rsm);
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntScheduleRecurrenceSet")
                .replace("%hunt%", hunt.getId()));
    }

    // --- activefrom / activeuntil (slots mode) ---

    private void handleActiveDate(CommandSender sender, HBHunt hunt, String[] args, String field) {
        if (args.length < 5) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntUsage"));
            return;
        }

        LocalDate date;
        try {
            date = LocalDate.parse(args[4], DATE_FMT);
        } catch (DateTimeParseException e) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntScheduleInvalidDate"));
            return;
        }

        ScheduledBehavior existing = getOrCreateScheduledBehavior(hunt);
        ScheduleMode mode = existing.getScheduleMode();

        SlotsScheduleMode ssm;
        if (mode instanceof SlotsScheduleMode s) {
            LocalDate from = "activeFrom".equals(field) ? date : s.activeFrom();
            LocalDate until = "activeUntil".equals(field) ? date : s.activeUntil();
            ssm = new SlotsScheduleMode(s.slots(), from, until);
        } else {
            LocalDate from = "activeFrom".equals(field) ? date : null;
            LocalDate until = "activeUntil".equals(field) ? date : null;
            ssm = new SlotsScheduleMode(List.of(), from, until);
        }

        replaceScheduledBehavior(hunt, ssm);
        sender.sendMessage(registry.getLanguageService().message("Messages.HuntScheduleUpdated")
                .replace("%hunt%", hunt.getId()));
    }

    // --- info ---

    private void handleInfo(CommandSender sender, HBHunt hunt) {
        ScheduledBehavior existing = findScheduledBehavior(hunt);
        if (existing == null) {
            sender.sendMessage(registry.getLanguageService().message("Messages.HuntScheduleNoScheduled")
                    .replace("%hunt%", hunt.getId()));
            return;
        }

        ScheduleMode mode = existing.getScheduleMode();
        StringBuilder info = new StringBuilder();
        info.append("&7Mode: &e").append(mode.getModeId());
        info.append("\n&7Display: &e").append(mode.getDisplayInfo());

        List<TimeSlot> slots = getSlotsFromMode(mode);
        if (!slots.isEmpty()) {
            info.append("\n&7Slots:");
            for (int i = 0; i < slots.size(); i++) {
                info.append("\n  &e").append(i + 1).append(". &7").append(slots.get(i).getDisplayInfo());
            }
        }

        sender.sendMessage(registry.getLanguageService().message("Messages.HuntScheduleInfo")
                .replace("%hunt%", hunt.getId())
                .replace("%info%", info.toString()));
    }

    private List<TimeSlot> getSlotsFromMode(ScheduleMode mode) {
        if (mode instanceof RangeScheduleMode rsm) {
            return rsm.slots();
        }
        if (mode instanceof SlotsScheduleMode ssm) {
            return ssm.slots();
        }
        if (mode instanceof RecurringScheduleMode rsm) {
            return rsm.slots();
        }
        return List.of();
    }

    // --- Tab completion ---

    public ArrayList<String> tabComplete(String[] args) {
        if (args.length == 4) {
            return Stream.of("start", "end", "clear", "mode", "addslot", "removeslot",
                            "every", "startref", "duration", "activefrom", "activeuntil", "info")
                    .filter(s -> s.startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        if (args.length == 5) {
            String action = args[3].toLowerCase();
            return switch (action) {
                case "mode" -> Stream.of("range", "slots", "recurring")
                        .filter(s -> s.startsWith(args[4].toLowerCase()))
                        .collect(Collectors.toCollection(ArrayList::new));
                case "every" -> Stream.of("year", "month", "week")
                        .filter(s -> s.startsWith(args[4].toLowerCase()))
                        .collect(Collectors.toCollection(ArrayList::new));
                case "clear" -> Stream.of("start", "end")
                        .filter(s -> s.startsWith(args[4].toLowerCase()))
                        .collect(Collectors.toCollection(ArrayList::new));
                default -> new ArrayList<>();
            };
        }

        return new ArrayList<>();
    }

    private DayOfWeek parseDayOfWeek(String input) {
        return switch (input.toUpperCase()) {
            case "MON", "MONDAY" -> DayOfWeek.MONDAY;
            case "TUE", "TUESDAY" -> DayOfWeek.TUESDAY;
            case "WED", "WEDNESDAY" -> DayOfWeek.WEDNESDAY;
            case "THU", "THURSDAY" -> DayOfWeek.THURSDAY;
            case "FRI", "FRIDAY" -> DayOfWeek.FRIDAY;
            case "SAT", "SATURDAY" -> DayOfWeek.SATURDAY;
            case "SUN", "SUNDAY" -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException("Invalid day: " + input);
        };
    }
}
