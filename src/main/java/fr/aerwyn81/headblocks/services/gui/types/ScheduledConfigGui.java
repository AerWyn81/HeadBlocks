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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ScheduledConfigGui {
    private static final String RANGE = "range";
    private static final String SLOTS = "slots";
    private static final String RECURRING = "recurring";
    private static final String GUI_BACK = "Gui.Back";
    private static final String GUI_BACK_LORE = "Gui.BackLore";
    private static final String GUI_SCHEDULED_CONFIG_TITLE = "Gui.ScheduledConfigTitle";
    private static final String GUI_SCHEDULED_CONFIG_NOT_DEFINED = "Gui.ScheduledConfigNotDefined";
    private static final String RANGE_START = "range_start";
    private static final String GUI_VALIDATE_CREATE = "Gui.ValidateCreate";
    private static final String GUI_VALIDATE_CREATE_LORE = "Gui.ValidateCreateLore";
    private static final String GUI_VALIDATE_BLOCKED = "Gui.ValidateBlocked";
    private static final String GUI_SCHEDULED_CONFIG_VALIDATE_BLOCKED_LORE = "Gui.ScheduledConfigValidateBlockedLore";
    private static final String SLOT_DAYS = "slot_days";
    private static final String RECURRING_DURATION = "recurring_duration";
    private static final String SLOT_FROM = "slot_from";
    private static final String SLOT_TO = "slot_to";
    private static final String GUI_SCHEDULED_CONFIG_INPUT_INVALID = "Gui.ScheduledConfigInputInvalid";
    private static final String GUI_SCHEDULED_CONFIG_INPUT_SET = "Gui.ScheduledConfigInputSet";

    private final ServiceRegistry registry;

    // Shared pending state
    private final ConcurrentHashMap<UUID, Location> pendingPlateLocations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> pendingRepeatables = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> pendingLimitSeconds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> pendingResetOnExpire = new ConcurrentHashMap<>();
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

    public void open(Player player, Location plateLocation, boolean repeatable, int limitSeconds, boolean resetOnExpire) {
        if (plateLocation != null) {
            pendingPlateLocations.put(player.getUniqueId(), plateLocation);
        }
        pendingRepeatables.put(player.getUniqueId(), repeatable);
        pendingLimitSeconds.put(player.getUniqueId(), limitSeconds);
        pendingResetOnExpire.put(player.getUniqueId(), resetOnExpire);
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
                    pendingModeType.put(p.getUniqueId(), RANGE);
                    buildRangeConfigGui(p);
                }));

        // Slot 12: Slots
        menu.setItem(0, 12, new ItemGUI(new ItemBuilder(Material.REPEATER)
                .setName(registry.getLanguageService().message("Gui.ScheduledModeSlots"))
                .setLore(registry.getLanguageService().messageList("Gui.ScheduledModeSlotsLore"))
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    pendingModeType.put(p.getUniqueId(), SLOTS);
                    buildSlotsConfigGui(p);
                }));

        // Slot 13: Recurring
        menu.setItem(0, 13, new ItemGUI(new ItemBuilder(Material.DAYLIGHT_DETECTOR)
                .setName(registry.getLanguageService().message("Gui.ScheduledModeRecurring"))
                .setLore(registry.getLanguageService().messageList("Gui.ScheduledModeRecurringLore"))
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    pendingModeType.put(p.getUniqueId(), RECURRING);
                    buildRecurringConfigGui(p);
                }));

        menu.setPaginationButtonBuilder((type, inv) -> {
            if (type == HBPaginationButtonType.CLOSE_BUTTON) {
                return new ItemGUI(registry.getConfigService().guiBackIcon()
                        .setName(registry.getLanguageService().message(GUI_BACK))
                        .setLore(registry.getLanguageService().messageList(GUI_BACK_LORE))
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
                registry.getLanguageService().message(GUI_SCHEDULED_CONFIG_TITLE), false, 2);

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
                : registry.getLanguageService().message(GUI_SCHEDULED_CONFIG_NOT_DEFINED);
        Material startMat = start != null ? Material.LIME_DYE : Material.GRAY_DYE;

        List<String> startLore = registry.getLanguageService().messageList("Gui.ScheduledConfigStartLore").stream()
                .map(s -> s.replace("%value%", startValue))
                .toList();

        menu.setItem(0, 11, new ItemGUI(new ItemBuilder(startMat)
                .setName(registry.getLanguageService().message("Gui.ScheduledConfigStart"))
                .setLore(startLore)
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    p.closeInventory();
                    pendingChatFields.put(p.getUniqueId(), RANGE_START);
                    p.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Gui.ScheduledConfigInputDate")));
                }));

        // Slot 13: Validate
        if (start != null || end != null) {
            menu.setItem(0, 13, new ItemGUI(new ItemBuilder(Material.DIAMOND)
                    .setName(registry.getLanguageService().message(GUI_VALIDATE_CREATE))
                    .setLore(registry.getLanguageService().messageList(GUI_VALIDATE_CREATE_LORE))
                    .toItemStack(), true)
                    .addOnClickEvent(event -> handleValidate((Player) event.getWhoClicked())));
        } else {
            menu.setItem(0, 13, new ItemGUI(new ItemBuilder(Material.BARRIER)
                    .setName(registry.getLanguageService().message(GUI_VALIDATE_BLOCKED))
                    .setLore(registry.getLanguageService().messageList(GUI_SCHEDULED_CONFIG_VALIDATE_BLOCKED_LORE))
                    .toItemStack()));
        }

        // Slot 15: End
        String endValue = end != null
                ? end.format(DATE_TIME)
                : registry.getLanguageService().message(GUI_SCHEDULED_CONFIG_NOT_DEFINED);
        Material endMat = end != null ? Material.LIME_DYE : Material.GRAY_DYE;

        List<String> endLore = registry.getLanguageService().messageList("Gui.ScheduledConfigEndLore").stream()
                .map(s -> s.replace("%value%", endValue))
                .toList();

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
                        .setName(registry.getLanguageService().message(GUI_BACK))
                        .setLore(registry.getLanguageService().messageList(GUI_BACK_LORE))
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
                registry.getLanguageService().message(GUI_SCHEDULED_CONFIG_TITLE), false, 2);

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
                            current.subList(idx, idx + 1).clear();
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
                    pendingChatFields.put(p.getUniqueId(), SLOT_DAYS);
                    p.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Gui.ScheduledSlotsInputDays")));
                }));

        // Validate button
        if (!slots.isEmpty()) {
            menu.setItem(0, 17, new ItemGUI(new ItemBuilder(Material.DIAMOND)
                    .setName(registry.getLanguageService().message(GUI_VALIDATE_CREATE))
                    .setLore(registry.getLanguageService().messageList(GUI_VALIDATE_CREATE_LORE))
                    .toItemStack(), true)
                    .addOnClickEvent(event -> handleValidate((Player) event.getWhoClicked())));
        } else {
            menu.setItem(0, 17, new ItemGUI(new ItemBuilder(Material.BARRIER)
                    .setName(registry.getLanguageService().message(GUI_VALIDATE_BLOCKED))
                    .setLore(registry.getLanguageService().messageList(GUI_SCHEDULED_CONFIG_VALIDATE_BLOCKED_LORE))
                    .toItemStack()));
        }

        menu.setPaginationButtonBuilder((type, inv) -> {
            if (type == HBPaginationButtonType.CLOSE_BUTTON) {
                return new ItemGUI(registry.getConfigService().guiBackIcon()
                        .setName(registry.getLanguageService().message(GUI_BACK))
                        .setLore(registry.getLanguageService().messageList(GUI_BACK_LORE))
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
                registry.getLanguageService().message(GUI_SCHEDULED_CONFIG_TITLE), false, 2);

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
        String everyValue = every != null ? every.name().toLowerCase() : registry.getLanguageService().message(GUI_SCHEDULED_CONFIG_NOT_DEFINED);
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
        String refValue = startRef != null ? startRef : registry.getLanguageService().message(GUI_SCHEDULED_CONFIG_NOT_DEFINED);
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
        String durValue = duration != null ? ScheduleDateTimeParser.formatDuration(duration) : registry.getLanguageService().message(GUI_SCHEDULED_CONFIG_NOT_DEFINED);
        menu.setItem(0, 13, new ItemGUI(new ItemBuilder(Material.CLOCK)
                .setName(registry.getLanguageService().message("Gui.ScheduledRecurringDuration"))
                .setLore(List.of("", "§7" + durValue, "", "§a§lCLICK§8: §7Set"))
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    p.closeInventory();
                    pendingChatFields.put(p.getUniqueId(), RECURRING_DURATION);
                    p.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Gui.ScheduledRecurringInputDuration")));
                }));

        // Slot 15: Validate
        if (every != null && startRef != null && duration != null) {
            menu.setItem(0, 15, new ItemGUI(new ItemBuilder(Material.DIAMOND)
                    .setName(registry.getLanguageService().message(GUI_VALIDATE_CREATE))
                    .setLore(registry.getLanguageService().messageList(GUI_VALIDATE_CREATE_LORE))
                    .toItemStack(), true)
                    .addOnClickEvent(event -> handleValidate((Player) event.getWhoClicked())));
        } else {
            menu.setItem(0, 15, new ItemGUI(new ItemBuilder(Material.BARRIER)
                    .setName(registry.getLanguageService().message(GUI_VALIDATE_BLOCKED))
                    .setLore(registry.getLanguageService().messageList(GUI_SCHEDULED_CONFIG_VALIDATE_BLOCKED_LORE))
                    .toItemStack()));
        }

        menu.setPaginationButtonBuilder((type, inv) -> {
            if (type == HBPaginationButtonType.CLOSE_BUTTON) {
                return new ItemGUI(registry.getConfigService().guiBackIcon()
                        .setName(registry.getLanguageService().message(GUI_BACK))
                        .setLore(registry.getLanguageService().messageList(GUI_BACK_LORE))
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
        String modeType = pendingModeType.getOrDefault(uuid, RANGE);
        Location plateLoc = pendingPlateLocations.remove(uuid);
        boolean repeatable = pendingRepeatables.getOrDefault(uuid, true);
        pendingRepeatables.remove(uuid);
        int limitSeconds = pendingLimitSeconds.getOrDefault(uuid, 0);
        pendingLimitSeconds.remove(uuid);
        boolean resetOnExpire = pendingResetOnExpire.getOrDefault(uuid, false);
        pendingResetOnExpire.remove(uuid);

        ScheduleMode scheduleMode = buildScheduleMode(uuid, modeType);
        clearModeState(uuid);

        registry.getGuiService().getBehaviorSelectionManager()
                .createHunt(player, plateLoc, repeatable, limitSeconds, resetOnExpire, scheduleMode);
    }

    private ScheduleMode buildScheduleMode(UUID uuid, String modeType) {
        return switch (modeType) {
            case SLOTS -> {
                List<TimeSlot> slots = pendingSlots.getOrDefault(uuid, List.of());
                LocalDate from = pendingActiveFrom.get(uuid);
                LocalDate until = pendingActiveUntil.get(uuid);
                yield new SlotsScheduleMode(slots, from, until);
            }
            case RECURRING -> {
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
            case RANGE_START, "range_end" -> processRangeDateInput(player, field, message);
            case SLOT_DAYS -> processSlotDaysInput(player, message);
            case SLOT_FROM -> processSlotTimeInput(player, SLOT_FROM, message);
            case SLOT_TO -> processSlotTimeInput(player, SLOT_TO, message);
            case "recurring_startref" -> processRecurringStartRefInput(player, message);
            case RECURRING_DURATION -> processRecurringDurationInput(player, message);
            default -> reopenCurrentGui(player);
        }
    }

    private void processRangeDateInput(Player player, String field, String message) {
        UUID uuid = player.getUniqueId();
        LocalDateTime parsed = parseDateTimeInput(message);
        if (parsed == null) {
            player.sendMessage(MessageUtils.colorize(registry.getLanguageService().message(GUI_SCHEDULED_CONFIG_INPUT_INVALID)));
            pendingChatFields.put(uuid, field);
            return;
        }

        if (RANGE_START.equals(field)) {
            pendingStarts.put(uuid, parsed);
        } else {
            pendingEnds.put(uuid, parsed);
        }

        player.sendMessage(MessageUtils.colorize(registry.getLanguageService().message(GUI_SCHEDULED_CONFIG_INPUT_SET)));
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
                pendingChatFields.put(uuid, SLOT_DAYS);
                return;
            }
        }

        // Store days temporarily and ask for "from" time
        pendingSlots.computeIfAbsent(uuid, k -> new ArrayList<>());
        // Store days in a simple format to carry through the multi-step flow
        String daysStr = days.stream().map(Enum::name).collect(Collectors.joining(","));
        pendingStartRef.put(uuid, daysStr); // reuse this map temporarily for multi-step
        pendingChatFields.put(uuid, SLOT_FROM);
        player.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Gui.ScheduledSlotsInputFrom")));
    }

    private void processSlotTimeInput(Player player, String field, String message) {
        UUID uuid = player.getUniqueId();
        LocalTime time;
        try {
            time = LocalTime.parse(message.trim(), ScheduleDateTimeParser.TIME_FORMAT);
        } catch (DateTimeParseException e) {
            player.sendMessage(MessageUtils.colorize(registry.getLanguageService().message(GUI_SCHEDULED_CONFIG_INPUT_INVALID)));
            pendingChatFields.put(uuid, field);
            return;
        }

        if (SLOT_FROM.equals(field)) {
            // Store "from" time appended to the days string
            String stored = pendingStartRef.getOrDefault(uuid, "");
            pendingStartRef.put(uuid, stored + "|" + time.format(ScheduleDateTimeParser.TIME_FORMAT));
            pendingChatFields.put(uuid, SLOT_TO);
            player.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Gui.ScheduledSlotsInputTo")));
        } else if (addPendingSlot(player, uuid, time)) {
            player.sendMessage(MessageUtils.colorize(registry.getLanguageService().message(GUI_SCHEDULED_CONFIG_INPUT_SET)));
            buildSlotsConfigGui(player);
        }
    }

    private boolean addPendingSlot(Player player, UUID uuid, LocalTime time) {
        String stored = pendingStartRef.remove(uuid);
        if (stored == null) {
            return true;
        }

        String[] parts = stored.split("\\|");
        if (parts.length < 2) {
            return true;
        }

        String daysStr = parts[0];
        LocalTime from;
        try {
            from = LocalTime.parse(parts[1], ScheduleDateTimeParser.TIME_FORMAT);
        } catch (DateTimeParseException e) {
            buildSlotsConfigGui(player);
            return false;
        }

        List<DayOfWeek> days = parseDays(daysStr);
        if (!days.isEmpty()) {
            TimeSlot slot = new TimeSlot(List.copyOf(days), from, time);
            pendingSlots.computeIfAbsent(uuid, k -> new ArrayList<>()).add(slot);
        }
        return true;
    }

    private static List<DayOfWeek> parseDays(String daysStr) {
        List<DayOfWeek> days = new ArrayList<>();
        for (String d : daysStr.split(",")) {
            for (DayOfWeek day : DayOfWeek.values()) {
                if (day.name().equals(d)) {
                    days.add(day);
                }
            }
        }
        return days;
    }

    private void processRecurringStartRefInput(Player player, String message) {
        UUID uuid = player.getUniqueId();
        pendingStartRef.put(uuid, message.trim());
        player.sendMessage(MessageUtils.colorize(registry.getLanguageService().message(GUI_SCHEDULED_CONFIG_INPUT_SET)));
        buildRecurringConfigGui(player);
    }

    private void processRecurringDurationInput(Player player, String message) {
        UUID uuid = player.getUniqueId();
        Duration dur = ScheduleDateTimeParser.parseDuration(message.trim());
        if (dur == null) {
            player.sendMessage(MessageUtils.colorize(registry.getLanguageService().message(GUI_SCHEDULED_CONFIG_INPUT_INVALID)));
            pendingChatFields.put(uuid, RECURRING_DURATION);
            return;
        }

        pendingDuration.put(uuid, dur);
        player.sendMessage(MessageUtils.colorize(registry.getLanguageService().message(GUI_SCHEDULED_CONFIG_INPUT_SET)));
        buildRecurringConfigGui(player);
    }

    private void reopenCurrentGui(Player player) {
        String mode = pendingModeType.getOrDefault(player.getUniqueId(), RANGE);
        switch (mode) {
            case SLOTS -> buildSlotsConfigGui(player);
            case RECURRING -> buildRecurringConfigGui(player);
            default -> buildRangeConfigGui(player);
        }
    }

    private LocalDateTime parseDateTimeInput(String input) {
        var dateTime = parseOrNull(() -> LocalDateTime.parse(input.trim(), DATE_TIME));
        if (dateTime != null) {
            return dateTime;
        }
        return parseOrNull(() -> java.time.LocalDate.parse(input.trim(), DATE_ONLY).atStartOfDay());
    }

    private static LocalDateTime parseOrNull(Supplier<LocalDateTime> parser) {
        try {
            return parser.get();
        } catch (DateTimeParseException e) {
            return null;
        }
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
        pendingLimitSeconds.remove(playerUuid);
        pendingResetOnExpire.remove(playerUuid);
    }
}
