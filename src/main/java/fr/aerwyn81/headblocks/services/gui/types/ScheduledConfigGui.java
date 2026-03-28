package fr.aerwyn81.headblocks.services.gui.types;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.hunt.behavior.schedule.*;
import fr.aerwyn81.headblocks.utils.bukkit.ItemBuilder;
import fr.aerwyn81.headblocks.utils.gui.HBMenu;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
import fr.aerwyn81.headblocks.utils.gui.pagination.HBPaginationButtonType;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ScheduledConfigGui {

    private final ServiceRegistry registry;

    // Shared pending state
    private final ConcurrentHashMap<UUID, Location> pendingPlateLocations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> pendingRepeatables = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> pendingChatFields = new ConcurrentHashMap<>();

    // Mode selection
    private final ConcurrentHashMap<UUID, String> pendingModeType = new ConcurrentHashMap<>();

    // Range mode state
    private final ConcurrentHashMap<UUID, LocalDateTime> pendingStarts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, LocalDateTime> pendingEnds = new ConcurrentHashMap<>();

    // Slots mode state
    private final ConcurrentHashMap<UUID, List<TimeSlot>> pendingSlots = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, LocalDate> pendingActiveFrom = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, LocalDate> pendingActiveUntil = new ConcurrentHashMap<>();

    // Recurring mode state
    private final ConcurrentHashMap<UUID, RecurrenceUnit> pendingEvery = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> pendingStartRef = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Duration> pendingDuration = new ConcurrentHashMap<>();

    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");

    public ScheduledConfigGui(ServiceRegistry registry) {
        this.registry = registry;
    }

    public void open(Player player, Location plateLocation, boolean repeatable) {
        if (plateLocation != null) {
            pendingPlateLocations.put(player.getUniqueId(), plateLocation);
        }
        pendingRepeatables.put(player.getUniqueId(), repeatable);
        buildModeSelectionGui(player);
    }

    // --- Mode Selection Page ---

    private void buildModeSelectionGui(Player player) {
        var menu = new HBMenu(registry.getPluginProvider().getJavaPlugin(), registry.getGuiService(),
                registry.getLanguageService().message("Gui.ScheduledModeSelectionTitle"), false, 2);

        int[] borders = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 14, 16, 17};
        IntStream.range(0, borders.length).map(i -> borders.length - i - 1).forEach(
                index -> menu.setItem(0, borders[index],
                        new ItemGUI(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName("§7").toItemStack()))
        );

        // Slot 11: Range
        menu.setItem(0, 11, new ItemGUI(new ItemBuilder(Material.CLOCK)
                .setName(registry.getLanguageService().message("Gui.ScheduledModeRange"))
                .setLore(registry.getLanguageService().messageList("Gui.ScheduledModeRangeLore"))
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    pendingModeType.put(p.getUniqueId(), "range");
                    buildRangeConfigGui(p);
                }));

        // Slot 12: Slots
        menu.setItem(0, 12, new ItemGUI(new ItemBuilder(Material.REPEATER)
                .setName(registry.getLanguageService().message("Gui.ScheduledModeSlots"))
                .setLore(registry.getLanguageService().messageList("Gui.ScheduledModeSlotsLore"))
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    pendingModeType.put(p.getUniqueId(), "slots");
                    buildSlotsConfigGui(p);
                }));

        // Slot 13: Recurring
        menu.setItem(0, 13, new ItemGUI(new ItemBuilder(Material.DAYLIGHT_DETECTOR)
                .setName(registry.getLanguageService().message("Gui.ScheduledModeRecurring"))
                .setLore(registry.getLanguageService().messageList("Gui.ScheduledModeRecurringLore"))
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    pendingModeType.put(p.getUniqueId(), "recurring");
                    buildRecurringConfigGui(p);
                }));

        menu.setPaginationButtonBuilder((type, inv) -> {
            if (type == HBPaginationButtonType.CLOSE_BUTTON) {
                return new ItemGUI(registry.getConfigService().guiBackIcon()
                        .setName(registry.getLanguageService().message("Gui.Back"))
                        .setLore(registry.getLanguageService().messageList("Gui.BackLore"))
                        .toItemStack())
                        .addOnClickEvent(event -> registry.getGuiService().getBehaviorSelectionManager()
                                .buildAndOpenGui((Player) event.getWhoClicked()));
            }
            return null;
        });

        player.openInventory(menu.getInventory());
    }

    // --- Range Config Page ---

    private void buildRangeConfigGui(Player player) {
        var menu = new HBMenu(registry.getPluginProvider().getJavaPlugin(), registry.getGuiService(),
                registry.getLanguageService().message("Gui.ScheduledConfigTitle"), false, 2);

        int[] borders = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 17};
        IntStream.range(0, borders.length).map(i -> borders.length - i - 1).forEach(
                index -> menu.setItem(0, borders[index],
                        new ItemGUI(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName("§7").toItemStack()))
        );

        UUID uuid = player.getUniqueId();
        LocalDateTime start = pendingStarts.get(uuid);
        LocalDateTime end = pendingEnds.get(uuid);

        // Slot 11: Start
        String startValue = start != null
                ? start.format(DATE_TIME)
                : registry.getLanguageService().message("Gui.ScheduledConfigNotDefined");
        Material startMat = start != null ? Material.LIME_DYE : Material.GRAY_DYE;

        List<String> startLore = registry.getLanguageService().messageList("Gui.ScheduledConfigStartLore").stream()
                .map(s -> s.replace("%value%", startValue))
                .collect(Collectors.toList());

        menu.setItem(0, 11, new ItemGUI(new ItemBuilder(startMat)
                .setName(registry.getLanguageService().message("Gui.ScheduledConfigStart"))
                .setLore(startLore)
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    p.closeInventory();
                    pendingChatFields.put(p.getUniqueId(), "range_start");
                    p.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Gui.ScheduledConfigInputDate")));
                }));

        // Slot 13: Validate
        if (start != null || end != null) {
            menu.setItem(0, 13, new ItemGUI(new ItemBuilder(Material.DIAMOND)
                    .setName(registry.getLanguageService().message("Gui.ValidateCreate"))
                    .setLore(registry.getLanguageService().messageList("Gui.ValidateCreateLore"))
                    .toItemStack(), true)
                    .addOnClickEvent(event -> handleValidate((Player) event.getWhoClicked())));
        } else {
            menu.setItem(0, 13, new ItemGUI(new ItemBuilder(Material.BARRIER)
                    .setName(registry.getLanguageService().message("Gui.ValidateBlocked"))
                    .setLore(registry.getLanguageService().messageList("Gui.ScheduledConfigValidateBlockedLore"))
                    .toItemStack()));
        }

        // Slot 15: End
        String endValue = end != null
                ? end.format(DATE_TIME)
                : registry.getLanguageService().message("Gui.ScheduledConfigNotDefined");
        Material endMat = end != null ? Material.LIME_DYE : Material.GRAY_DYE;

        List<String> endLore = registry.getLanguageService().messageList("Gui.ScheduledConfigEndLore").stream()
                .map(s -> s.replace("%value%", endValue))
                .collect(Collectors.toList());

        menu.setItem(0, 15, new ItemGUI(new ItemBuilder(endMat)
                .setName(registry.getLanguageService().message("Gui.ScheduledConfigEnd"))
                .setLore(endLore)
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    p.closeInventory();
                    pendingChatFields.put(p.getUniqueId(), "range_end");
                    p.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Gui.ScheduledConfigInputDate")));
                }));

        menu.setPaginationButtonBuilder((type, inv) -> {
            if (type == HBPaginationButtonType.CLOSE_BUTTON) {
                return new ItemGUI(registry.getConfigService().guiBackIcon()
                        .setName(registry.getLanguageService().message("Gui.Back"))
                        .setLore(registry.getLanguageService().messageList("Gui.BackLore"))
                        .toItemStack())
                        .addOnClickEvent(event -> buildModeSelectionGui((Player) event.getWhoClicked()));
            }
            return null;
        });

        player.openInventory(menu.getInventory());
    }

    // --- Slots Config Page ---

    private void buildSlotsConfigGui(Player player) {
        var menu = new HBMenu(registry.getPluginProvider().getJavaPlugin(), registry.getGuiService(),
                registry.getLanguageService().message("Gui.ScheduledConfigTitle"), false, 2);

        int[] borders = {0, 1, 2, 3, 4, 5, 6, 7, 8};
        IntStream.range(0, borders.length).map(i -> borders.length - i - 1).forEach(
                index -> menu.setItem(0, borders[index],
                        new ItemGUI(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName("§7").toItemStack()))
        );

        UUID uuid = player.getUniqueId();
        List<TimeSlot> slots = pendingSlots.getOrDefault(uuid, new ArrayList<>());

        // Display existing slots in the second row
        for (int i = 0; i < Math.min(slots.size(), 7); i++) {
            TimeSlot slot = slots.get(i);
            int idx = i;
            menu.setItem(0, 9 + i, new ItemGUI(new ItemBuilder(Material.PAPER)
                    .setName("§e" + slot.getDisplayInfo())
                    .setLore(List.of("", registry.getLanguageService().message("Gui.ScheduledSlotsRemoveLore")))
                    .toItemStack(), true)
                    .addOnClickEvent(event -> {
                        Player p = (Player) event.getWhoClicked();
                        List<TimeSlot> current = pendingSlots.getOrDefault(p.getUniqueId(), new ArrayList<>());
                        if (idx < current.size()) {
                            current.remove(idx);
                        }
                        buildSlotsConfigGui(p);
                    }));
        }

        // Add slot button
        int addSlotIdx = Math.min(slots.size(), 7) + 9;
        menu.setItem(0, addSlotIdx, new ItemGUI(new ItemBuilder(Material.LIME_DYE)
                .setName(registry.getLanguageService().message("Gui.ScheduledSlotsAddSlot"))
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    p.closeInventory();
                    pendingChatFields.put(p.getUniqueId(), "slot_days");
                    p.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Gui.ScheduledSlotsInputDays")));
                }));

        // Validate button
        if (!slots.isEmpty()) {
            menu.setItem(0, 17, new ItemGUI(new ItemBuilder(Material.DIAMOND)
                    .setName(registry.getLanguageService().message("Gui.ValidateCreate"))
                    .setLore(registry.getLanguageService().messageList("Gui.ValidateCreateLore"))
                    .toItemStack(), true)
                    .addOnClickEvent(event -> handleValidate((Player) event.getWhoClicked())));
        } else {
            menu.setItem(0, 17, new ItemGUI(new ItemBuilder(Material.BARRIER)
                    .setName(registry.getLanguageService().message("Gui.ValidateBlocked"))
                    .setLore(registry.getLanguageService().messageList("Gui.ScheduledConfigValidateBlockedLore"))
                    .toItemStack()));
        }

        menu.setPaginationButtonBuilder((type, inv) -> {
            if (type == HBPaginationButtonType.CLOSE_BUTTON) {
                return new ItemGUI(registry.getConfigService().guiBackIcon()
                        .setName(registry.getLanguageService().message("Gui.Back"))
                        .setLore(registry.getLanguageService().messageList("Gui.BackLore"))
                        .toItemStack())
                        .addOnClickEvent(event -> buildModeSelectionGui((Player) event.getWhoClicked()));
            }
            return null;
        });

        player.openInventory(menu.getInventory());
    }

    // --- Recurring Config Page ---

    private void buildRecurringConfigGui(Player player) {
        var menu = new HBMenu(registry.getPluginProvider().getJavaPlugin(), registry.getGuiService(),
                registry.getLanguageService().message("Gui.ScheduledConfigTitle"), false, 2);

        int[] borders = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 14, 16, 17};
        IntStream.range(0, borders.length).map(i -> borders.length - i - 1).forEach(
                index -> menu.setItem(0, borders[index],
                        new ItemGUI(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName("§7").toItemStack()))
        );

        UUID uuid = player.getUniqueId();
        RecurrenceUnit every = pendingEvery.get(uuid);
        String startRef = pendingStartRef.get(uuid);
        Duration duration = pendingDuration.get(uuid);

        // Slot 11: Every (click to cycle)
        String everyValue = every != null ? every.name().toLowerCase() : registry.getLanguageService().message("Gui.ScheduledConfigNotDefined");
        menu.setItem(0, 11, new ItemGUI(new ItemBuilder(Material.COMPASS)
                .setName(registry.getLanguageService().message("Gui.ScheduledRecurringEvery"))
                .setLore(List.of("", "§7" + everyValue, "", "§a§lCLICK§8: §7Cycle"))
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    RecurrenceUnit current = pendingEvery.get(p.getUniqueId());
                    RecurrenceUnit next;
                    if (current == null) {
                        next = RecurrenceUnit.YEAR;
                    } else {
                        next = switch (current) {
                            case YEAR -> RecurrenceUnit.MONTH;
                            case MONTH -> RecurrenceUnit.WEEK;
                            case WEEK -> RecurrenceUnit.YEAR;
                        };
                    }
                    pendingEvery.put(p.getUniqueId(), next);
                    buildRecurringConfigGui(p);
                }));

        // Slot 12: Start ref (chat input)
        String refValue = startRef != null ? startRef : registry.getLanguageService().message("Gui.ScheduledConfigNotDefined");
        menu.setItem(0, 12, new ItemGUI(new ItemBuilder(Material.NAME_TAG)
                .setName(registry.getLanguageService().message("Gui.ScheduledRecurringStartRef"))
                .setLore(List.of("", "§7" + refValue, "", "§a§lCLICK§8: §7Set"))
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    p.closeInventory();
                    pendingChatFields.put(p.getUniqueId(), "recurring_startref");
                    p.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Gui.ScheduledRecurringInputStartRef")));
                }));

        // Slot 13: Duration (chat input)
        String durValue = duration != null ? ScheduleDateTimeParser.formatDuration(duration) : registry.getLanguageService().message("Gui.ScheduledConfigNotDefined");
        menu.setItem(0, 13, new ItemGUI(new ItemBuilder(Material.CLOCK)
                .setName(registry.getLanguageService().message("Gui.ScheduledRecurringDuration"))
                .setLore(List.of("", "§7" + durValue, "", "§a§lCLICK§8: §7Set"))
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    p.closeInventory();
                    pendingChatFields.put(p.getUniqueId(), "recurring_duration");
                    p.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Gui.ScheduledRecurringInputDuration")));
                }));

        // Slot 15: Validate
        if (every != null && startRef != null && duration != null) {
            menu.setItem(0, 15, new ItemGUI(new ItemBuilder(Material.DIAMOND)
                    .setName(registry.getLanguageService().message("Gui.ValidateCreate"))
                    .setLore(registry.getLanguageService().messageList("Gui.ValidateCreateLore"))
                    .toItemStack(), true)
                    .addOnClickEvent(event -> handleValidate((Player) event.getWhoClicked())));
        } else {
            menu.setItem(0, 15, new ItemGUI(new ItemBuilder(Material.BARRIER)
                    .setName(registry.getLanguageService().message("Gui.ValidateBlocked"))
                    .setLore(registry.getLanguageService().messageList("Gui.ScheduledConfigValidateBlockedLore"))
                    .toItemStack()));
        }

        menu.setPaginationButtonBuilder((type, inv) -> {
            if (type == HBPaginationButtonType.CLOSE_BUTTON) {
                return new ItemGUI(registry.getConfigService().guiBackIcon()
                        .setName(registry.getLanguageService().message("Gui.Back"))
                        .setLore(registry.getLanguageService().messageList("Gui.BackLore"))
                        .toItemStack())
                        .addOnClickEvent(event -> buildModeSelectionGui((Player) event.getWhoClicked()));
            }
            return null;
        });

        player.openInventory(menu.getInventory());
    }

    // --- Validate ---

    private void handleValidate(Player player) {
        UUID uuid = player.getUniqueId();
        String modeType = pendingModeType.getOrDefault(uuid, "range");
        Location plateLoc = pendingPlateLocations.remove(uuid);
        boolean repeatable = pendingRepeatables.getOrDefault(uuid, true);
        pendingRepeatables.remove(uuid);

        ScheduleMode scheduleMode = buildScheduleMode(uuid, modeType);
        clearModeState(uuid);

        registry.getGuiService().getBehaviorSelectionManager().createHunt(player, plateLoc, repeatable, scheduleMode);
    }

    private ScheduleMode buildScheduleMode(UUID uuid, String modeType) {
        return switch (modeType) {
            case "slots" -> {
                List<TimeSlot> slots = pendingSlots.getOrDefault(uuid, List.of());
                LocalDate from = pendingActiveFrom.get(uuid);
                LocalDate until = pendingActiveUntil.get(uuid);
                yield new SlotsScheduleMode(slots, from, until);
            }
            case "recurring" -> {
                RecurrenceUnit every = pendingEvery.get(uuid);
                String ref = pendingStartRef.get(uuid);
                Duration dur = pendingDuration.get(uuid);
                yield new RecurringScheduleMode(every, ref, dur, List.of());
            }
            default -> {
                LocalDateTime start = pendingStarts.remove(uuid);
                LocalDateTime end = pendingEnds.remove(uuid);
                yield new RangeScheduleMode(start, end, List.of());
            }
        };
    }

    private void clearModeState(UUID uuid) {
        pendingModeType.remove(uuid);
        pendingStarts.remove(uuid);
        pendingEnds.remove(uuid);
        pendingSlots.remove(uuid);
        pendingActiveFrom.remove(uuid);
        pendingActiveUntil.remove(uuid);
        pendingEvery.remove(uuid);
        pendingStartRef.remove(uuid);
        pendingDuration.remove(uuid);
    }

    // --- Chat input handling ---

    public boolean hasPendingChatInput(Player player) {
        return pendingChatFields.containsKey(player.getUniqueId());
    }

    public void processPendingChatInput(Player player, String message) {
        UUID uuid = player.getUniqueId();
        String field = pendingChatFields.remove(uuid);
        if (field == null) {
            return;
        }

        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Gui.ScheduledConfigInputCancelled")));
            reopenCurrentGui(player);
            return;
        }

        switch (field) {
            case "range_start", "range_end" -> processRangeDateInput(player, field, message);
            case "slot_days" -> processSlotDaysInput(player, message);
            case "slot_from" -> processSlotTimeInput(player, "slot_from", message);
            case "slot_to" -> processSlotTimeInput(player, "slot_to", message);
            case "recurring_startref" -> processRecurringStartRefInput(player, message);
            case "recurring_duration" -> processRecurringDurationInput(player, message);
            default -> reopenCurrentGui(player);
        }
    }

    private void processRangeDateInput(Player player, String field, String message) {
        UUID uuid = player.getUniqueId();
        LocalDateTime parsed = parseDateTimeInput(message);
        if (parsed == null) {
            player.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Gui.ScheduledConfigInputInvalid")));
            pendingChatFields.put(uuid, field);
            return;
        }

        if ("range_start".equals(field)) {
            pendingStarts.put(uuid, parsed);
        } else {
            pendingEnds.put(uuid, parsed);
        }

        player.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Gui.ScheduledConfigInputSet")));
        buildRangeConfigGui(player);
    }

    private void processSlotDaysInput(Player player, String message) {
        UUID uuid = player.getUniqueId();
        List<DayOfWeek> days = new ArrayList<>();
        for (String part : message.split(",")) {
            try {
                days.add(parseDayOfWeek(part.trim()));
            } catch (IllegalArgumentException e) {
                player.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Gui.ScheduledSlotsInvalidDay")
                        .replace("%day%", part.trim())));
                pendingChatFields.put(uuid, "slot_days");
                return;
            }
        }

        // Store days temporarily and ask for "from" time
        pendingSlots.computeIfAbsent(uuid, k -> new ArrayList<>());
        // Store days in a simple format to carry through the multi-step flow
        String daysStr = days.stream().map(Enum::name).collect(Collectors.joining(","));
        pendingStartRef.put(uuid, daysStr); // reuse this map temporarily for multi-step
        pendingChatFields.put(uuid, "slot_from");
        player.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Gui.ScheduledSlotsInputFrom")));
    }

    private void processSlotTimeInput(Player player, String field, String message) {
        UUID uuid = player.getUniqueId();
        LocalTime time;
        try {
            time = LocalTime.parse(message.trim(), ScheduleDateTimeParser.TIME_FORMAT);
        } catch (DateTimeParseException e) {
            player.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Gui.ScheduledConfigInputInvalid")));
            pendingChatFields.put(uuid, field);
            return;
        }

        if ("slot_from".equals(field)) {
            // Store "from" time appended to the days string
            String stored = pendingStartRef.getOrDefault(uuid, "");
            pendingStartRef.put(uuid, stored + "|" + time.format(ScheduleDateTimeParser.TIME_FORMAT));
            pendingChatFields.put(uuid, "slot_to");
            player.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Gui.ScheduledSlotsInputTo")));
        } else {
            // "slot_to" - finalize the slot
            String stored = pendingStartRef.remove(uuid);
            if (stored != null) {
                String[] parts = stored.split("\\|");
                if (parts.length >= 2) {
                    String daysStr = parts[0];
                    LocalTime from;
                    try {
                        from = LocalTime.parse(parts[1], ScheduleDateTimeParser.TIME_FORMAT);
                    } catch (DateTimeParseException e) {
                        buildSlotsConfigGui(player);
                        return;
                    }

                    List<DayOfWeek> days = new ArrayList<>();
                    for (String d : daysStr.split(",")) {
                        try {
                            days.add(DayOfWeek.valueOf(d));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }

                    if (!days.isEmpty()) {
                        TimeSlot slot = new TimeSlot(List.copyOf(days), from, time);
                        pendingSlots.computeIfAbsent(uuid, k -> new ArrayList<>()).add(slot);
                    }
                }
            }

            player.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Gui.ScheduledConfigInputSet")));
            buildSlotsConfigGui(player);
        }
    }

    private void processRecurringStartRefInput(Player player, String message) {
        UUID uuid = player.getUniqueId();
        pendingStartRef.put(uuid, message.trim());
        player.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Gui.ScheduledConfigInputSet")));
        buildRecurringConfigGui(player);
    }

    private void processRecurringDurationInput(Player player, String message) {
        UUID uuid = player.getUniqueId();
        Duration dur = ScheduleDateTimeParser.parseDuration(message.trim());
        if (dur == null) {
            player.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Gui.ScheduledConfigInputInvalid")));
            pendingChatFields.put(uuid, "recurring_duration");
            return;
        }

        pendingDuration.put(uuid, dur);
        player.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Gui.ScheduledConfigInputSet")));
        buildRecurringConfigGui(player);
    }

    private void reopenCurrentGui(Player player) {
        String mode = pendingModeType.getOrDefault(player.getUniqueId(), "range");
        switch (mode) {
            case "slots" -> buildSlotsConfigGui(player);
            case "recurring" -> buildRecurringConfigGui(player);
            default -> buildRangeConfigGui(player);
        }
    }

    private LocalDateTime parseDateTimeInput(String input) {
        try {
            return LocalDateTime.parse(input.trim(), DATE_TIME);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return java.time.LocalDate.parse(input.trim(), DATE_ONLY).atStartOfDay();
        } catch (DateTimeParseException ignored) {
        }
        return null;
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

    public void clearState(UUID playerUuid) {
        clearModeState(playerUuid);
        pendingChatFields.remove(playerUuid);
        pendingPlateLocations.remove(playerUuid);
        pendingRepeatables.remove(playerUuid);
    }
}
