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

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TimedConfigGui {

    private static final int LIMIT_MAX = 3600;
    private static final int STEP_SMALL = 5;
    private static final int STEP_LARGE = 60;

    private final ServiceRegistry registry;
    private final ConcurrentHashMap<UUID, Location> plateLocations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> repeatableStates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> limitSecondsStates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> resetOnExpireStates = new ConcurrentHashMap<>();
    private final Set<UUID> pendingPlatePlacements = ConcurrentHashMap.newKeySet();

    public TimedConfigGui(ServiceRegistry registry) {
        this.registry = registry;
    }

    public void open(Player player) {
        repeatableStates.putIfAbsent(player.getUniqueId(), true);
        limitSecondsStates.putIfAbsent(player.getUniqueId(), 0);
        resetOnExpireStates.putIfAbsent(player.getUniqueId(), false);
        buildAndOpenGui(player);
    }

    private void buildAndOpenGui(Player player) {
        var menu = new HBMenu(registry.getPluginProvider().getJavaPlugin(), registry.getGuiService(),
                registry.getLanguageService().message("Gui.TimedConfigTitle"), false, 4);

        IntStream.range(0, 36).forEach(index -> menu.setItem(0, index,
                new ItemGUI(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName("§7").toItemStack())));

        UUID uuid = player.getUniqueId();

        // --- Setup (row 1) ---

        // Slot 11: Set start plate
        Location plateLoc = plateLocations.get(uuid);
        String locationText;
        if (plateLoc != null) {
            locationText = MessageUtils.colorize(registry.getLanguageService().message("Gui.TimedConfigPlateLocation")
                    .replace("%world%", plateLoc.getWorld() != null ? plateLoc.getWorld().getName() : "?")
                    .replace("%x%", String.valueOf(plateLoc.getBlockX()))
                    .replace("%y%", String.valueOf(plateLoc.getBlockY()))
                    .replace("%z%", String.valueOf(plateLoc.getBlockZ())));
        } else {
            locationText = registry.getLanguageService().message("Gui.TimedConfigPlateNotDefined");
        }

        List<String> plateLore = registry.getLanguageService().messageList("Gui.TimedConfigPlateLore").stream()
                .map(s -> s.replace("%location%", locationText))
                .collect(Collectors.toList());

        menu.setItem(0, 11, new ItemGUI(new ItemBuilder(Material.HEAVY_WEIGHTED_PRESSURE_PLATE)
                .setName(registry.getLanguageService().message("Gui.TimedConfigPlate"))
                .setLore(plateLore)
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    p.closeInventory();
                    pendingPlatePlacements.add(p.getUniqueId());
                    p.sendMessage(registry.getLanguageService().message("Gui.TimedConfigPlacePlate"));
                }));

        // Slot 15: Time limit (left/right click to adjust)
        int limitSeconds = limitSecondsStates.getOrDefault(uuid, 0);
        String limitValue = limitSeconds <= 0
                ? registry.getLanguageService().message("Gui.TimedConfigUnlimited")
                : limitSeconds + "s";

        List<String> limitLore = registry.getLanguageService().messageList("Gui.TimedConfigTimeLimitLore").stream()
                .map(s -> s.replace("%value%", limitValue))
                .collect(Collectors.toList());

        menu.setItem(0, 15, new ItemGUI(new ItemBuilder(Material.CLOCK)
                .setName(registry.getLanguageService().message("Gui.TimedConfigTimeLimit"))
                .setLore(limitLore)
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    int step = event.isShiftClick() ? STEP_LARGE : STEP_SMALL;
                    int sign = event.isRightClick() ? -1 : 1;
                    int current = limitSecondsStates.getOrDefault(p.getUniqueId(), 0);
                    int updated = Math.max(0, Math.min(LIMIT_MAX, current + sign * step));
                    limitSecondsStates.put(p.getUniqueId(), updated);
                    buildAndOpenGui(p);
                }));

        // --- Options (row 2) ---

        // Slot 21: Repeatable toggle
        boolean repeatable = repeatableStates.getOrDefault(uuid, true);
        Material repeatableMat = repeatable ? Material.LIME_DYE : Material.GRAY_DYE;
        String repeatableStatus = repeatable
                ? registry.getLanguageService().message("Gui.BehaviorEnabled")
                : registry.getLanguageService().message("Gui.BehaviorDisabled");

        List<String> repeatableLore = registry.getLanguageService().messageList("Gui.TimedConfigRepeatableLore").stream()
                .map(s -> s.replace("%status%", repeatableStatus))
                .collect(Collectors.toList());

        menu.setItem(0, 21, new ItemGUI(new ItemBuilder(repeatableMat)
                .setName(registry.getLanguageService().message("Gui.TimedConfigRepeatable"))
                .setLore(repeatableLore)
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    boolean current = repeatableStates.getOrDefault(p.getUniqueId(), true);
                    repeatableStates.put(p.getUniqueId(), !current);
                    buildAndOpenGui(p);
                }));

        // Slot 23: Reset on expire toggle
        boolean resetOnExpire = resetOnExpireStates.getOrDefault(uuid, false);
        Material resetMat = resetOnExpire ? Material.LIME_DYE : Material.GRAY_DYE;
        String resetStatus = resetOnExpire
                ? registry.getLanguageService().message("Gui.BehaviorEnabled")
                : registry.getLanguageService().message("Gui.BehaviorDisabled");

        List<String> resetLore = registry.getLanguageService().messageList("Gui.TimedConfigResetOnExpireLore").stream()
                .map(s -> s.replace("%status%", resetStatus))
                .collect(Collectors.toList());

        menu.setItem(0, 23, new ItemGUI(new ItemBuilder(resetMat)
                .setName(registry.getLanguageService().message("Gui.TimedConfigResetOnExpire"))
                .setLore(resetLore)
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    boolean current = resetOnExpireStates.getOrDefault(p.getUniqueId(), false);
                    resetOnExpireStates.put(p.getUniqueId(), !current);
                    buildAndOpenGui(p);
                }));

        // Slot 31: Validate (row 4)
        if (plateLoc != null) {
            menu.setItem(0, 31, new ItemGUI(new ItemBuilder(Material.DIAMOND)
                    .setName(registry.getLanguageService().message("Gui.ValidateCreate"))
                    .setLore(registry.getLanguageService().messageList("Gui.ValidateCreateLore"))
                    .toItemStack(), true)
                    .addOnClickEvent(event -> handleValidate((Player) event.getWhoClicked())));
        } else {
            menu.setItem(0, 31, new ItemGUI(new ItemBuilder(Material.BARRIER)
                    .setName(registry.getLanguageService().message("Gui.ValidateBlocked"))
                    .setLore(registry.getLanguageService().messageList("Gui.TimedConfigValidateBlockedLore"))
                    .toItemStack()));
        }

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
        Location plateLoc = plateLocations.remove(uuid);
        boolean repeatable = repeatableStates.getOrDefault(uuid, true);
        repeatableStates.remove(uuid);
        int limitSeconds = limitSecondsStates.getOrDefault(uuid, 0);
        limitSecondsStates.remove(uuid);
        boolean resetOnExpire = resetOnExpireStates.getOrDefault(uuid, false);
        resetOnExpireStates.remove(uuid);

        var selected = registry.getGuiService().getBehaviorSelectionManager().getSelectedBehaviors(uuid);
        if (selected != null && selected.contains("scheduled")) {
            registry.getGuiService().getScheduledConfigManager().open(player, plateLoc, repeatable, limitSeconds, resetOnExpire);
            return;
        }

        registry.getGuiService().getBehaviorSelectionManager()
                .createHunt(player, plateLoc, repeatable, limitSeconds, resetOnExpire, null);
    }

    public boolean hasPendingPlatePlacement(UUID playerUuid) {
        return pendingPlatePlacements.contains(playerUuid);
    }

    public void handlePlatePlaced(Player player, Location location) {
        pendingPlatePlacements.remove(player.getUniqueId());
        plateLocations.put(player.getUniqueId(), location);
        player.sendMessage(registry.getLanguageService().message("Gui.TimedConfigPlatePlaced"));
        open(player);
    }

    public void clearState(UUID playerUuid) {
        plateLocations.remove(playerUuid);
        repeatableStates.remove(playerUuid);
        limitSecondsStates.remove(playerUuid);
        resetOnExpireStates.remove(playerUuid);
        pendingPlatePlacements.remove(playerUuid);
    }
}
