package fr.aerwyn81.headblocks.services.gui.types;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.services.GuiService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.utils.bukkit.ItemBuilder;
import fr.aerwyn81.headblocks.utils.gui.HBMenu;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
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

    private final ConcurrentHashMap<UUID, Location> plateLocations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> repeatableStates = new ConcurrentHashMap<>();
    private final Set<UUID> pendingPlatePlacements = ConcurrentHashMap.newKeySet();

    public void open(Player player) {
        repeatableStates.putIfAbsent(player.getUniqueId(), true);
        buildAndOpenGui(player);
    }

    private void buildAndOpenGui(Player player) {
        var menu = new HBMenu(HeadBlocks.getInstance(),
                LanguageService.getMessage("Gui.TimedConfigTitle"), false, 2);

        // Borders
        int[] borders = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 17};
        IntStream.range(0, borders.length).map(i -> borders.length - i - 1).forEach(
                index -> menu.setItem(0, borders[index],
                        new ItemGUI(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName("§7").toItemStack()))
        );

        UUID uuid = player.getUniqueId();

        // Slot 11: Set start plate
        Location plateLoc = plateLocations.get(uuid);
        String locationText;
        if (plateLoc != null) {
            locationText = MessageUtils.colorize(LanguageService.getMessage("Gui.TimedConfigPlateLocation")
                    .replaceAll("%world%", plateLoc.getWorld() != null ? plateLoc.getWorld().getName() : "?")
                    .replaceAll("%x%", String.valueOf(plateLoc.getBlockX()))
                    .replaceAll("%y%", String.valueOf(plateLoc.getBlockY()))
                    .replaceAll("%z%", String.valueOf(plateLoc.getBlockZ())));
        } else {
            locationText = LanguageService.getMessage("Gui.TimedConfigPlateNotDefined");
        }

        List<String> plateLore = LanguageService.getMessages("Gui.TimedConfigPlateLore").stream()
                .map(s -> s.replaceAll("%location%", locationText))
                .collect(Collectors.toList());

        menu.setItem(0, 11, new ItemGUI(new ItemBuilder(Material.HEAVY_WEIGHTED_PRESSURE_PLATE)
                .setName(LanguageService.getMessage("Gui.TimedConfigPlate"))
                .setLore(plateLore)
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    p.closeInventory();
                    pendingPlatePlacements.add(p.getUniqueId());
                    p.sendMessage(LanguageService.getMessage("Gui.TimedConfigPlacePlate"));
                }));

        // Slot 13: Repeatable toggle
        boolean repeatable = repeatableStates.getOrDefault(uuid, true);
        Material repeatableMat = repeatable ? Material.LIME_DYE : Material.GRAY_DYE;
        String statusText = repeatable
                ? LanguageService.getMessage("Gui.BehaviorEnabled")
                : LanguageService.getMessage("Gui.BehaviorDisabled");

        List<String> repeatableLore = LanguageService.getMessages("Gui.TimedConfigRepeatableLore").stream()
                .map(s -> s.replaceAll("%status%", statusText))
                .collect(Collectors.toList());

        menu.setItem(0, 13, new ItemGUI(new ItemBuilder(repeatableMat)
                .setName(LanguageService.getMessage("Gui.TimedConfigRepeatable"))
                .setLore(repeatableLore)
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    boolean current = repeatableStates.getOrDefault(p.getUniqueId(), true);
                    repeatableStates.put(p.getUniqueId(), !current);
                    buildAndOpenGui(p);
                }));

        // Slot 15: Validate
        menu.setItem(0, 15, new ItemGUI(new ItemBuilder(Material.DIAMOND)
                .setName(LanguageService.getMessage("Gui.ValidateCreate"))
                .setLore(LanguageService.getMessages("Gui.ValidateCreateLore"))
                .toItemStack(), true)
                .addOnClickEvent(event -> handleValidate((Player) event.getWhoClicked())));

        player.openInventory(menu.getInventory());
    }

    private void handleValidate(Player player) {
        UUID uuid = player.getUniqueId();
        Location plateLoc = plateLocations.remove(uuid);
        boolean repeatable = repeatableStates.getOrDefault(uuid, true);
        repeatableStates.remove(uuid);

        GuiService.getBehaviorSelectionManager().createHunt(player, plateLoc, repeatable);
    }

    public boolean hasPendingPlatePlacement(UUID playerUuid) {
        return pendingPlatePlacements.contains(playerUuid);
    }

    public void handlePlatePlaced(Player player, Location location) {
        pendingPlatePlacements.remove(player.getUniqueId());
        plateLocations.put(player.getUniqueId(), location);
        player.sendMessage(LanguageService.getMessage("Gui.TimedConfigPlatePlaced"));
        open(player);
    }

    public void clearState(UUID playerUuid) {
        plateLocations.remove(playerUuid);
        repeatableStates.remove(playerUuid);
        pendingPlatePlacements.remove(playerUuid);
    }
}
