package fr.aerwyn81.headblocks.services.gui.types;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.utils.bukkit.ItemBuilder;
import fr.aerwyn81.headblocks.utils.gui.HBMenu;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
import fr.aerwyn81.headblocks.utils.gui.pagination.HBPaginationButtonType;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ScheduledConfigGui {

    private final ServiceRegistry registry;
    private final ConcurrentHashMap<UUID, LocalDateTime> pendingStarts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, LocalDateTime> pendingEnds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> pendingChatFields = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Location> pendingPlateLocations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> pendingRepeatables = new ConcurrentHashMap<>();

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
        buildAndOpenGui(player);
    }

    private void buildAndOpenGui(Player player) {
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
                    pendingChatFields.put(p.getUniqueId(), "start");
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
                    pendingChatFields.put(p.getUniqueId(), "end");
                    p.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Gui.ScheduledConfigInputDate")));
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

    private void handleValidate(Player player) {
        UUID uuid = player.getUniqueId();
        LocalDateTime start = pendingStarts.remove(uuid);
        LocalDateTime end = pendingEnds.remove(uuid);
        Location plateLoc = pendingPlateLocations.remove(uuid);
        boolean repeatable = pendingRepeatables.getOrDefault(uuid, true);
        pendingRepeatables.remove(uuid);

        registry.getGuiService().getBehaviorSelectionManager().createHunt(player, plateLoc, repeatable, start, end);
    }

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
            buildAndOpenGui(player);
            return;
        }

        LocalDateTime parsed = parseInput(message);
        if (parsed == null) {
            player.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Gui.ScheduledConfigInputInvalid")));
            pendingChatFields.put(uuid, field);
            return;
        }

        if ("start".equals(field)) {
            pendingStarts.put(uuid, parsed);
        } else {
            pendingEnds.put(uuid, parsed);
        }

        player.sendMessage(MessageUtils.colorize(registry.getLanguageService().message("Gui.ScheduledConfigInputSet")));
        buildAndOpenGui(player);
    }

    private LocalDateTime parseInput(String input) {
        try {
            return LocalDateTime.parse(input.trim(), DATE_TIME);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(input.trim(), DATE_ONLY).atStartOfDay();
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }

    public void clearState(UUID playerUuid) {
        pendingStarts.remove(playerUuid);
        pendingEnds.remove(playerUuid);
        pendingChatFields.remove(playerUuid);
        pendingPlateLocations.remove(playerUuid);
        pendingRepeatables.remove(playerUuid);
    }
}
