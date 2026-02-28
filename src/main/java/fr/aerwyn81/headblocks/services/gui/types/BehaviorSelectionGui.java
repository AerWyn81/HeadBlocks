package fr.aerwyn81.headblocks.services.gui.types;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.api.events.HuntCreateEvent;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.data.hunt.behavior.*;
import fr.aerwyn81.headblocks.services.*;
import fr.aerwyn81.headblocks.utils.bukkit.ItemBuilder;
import fr.aerwyn81.headblocks.utils.gui.HBMenu;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

public class BehaviorSelectionGui {

    private final ConcurrentHashMap<UUID, Set<String>> selectedBehaviors = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> pendingHuntNames = new ConcurrentHashMap<>();

    public void open(Player player, String huntName) {
        pendingHuntNames.put(player.getUniqueId(), huntName);
        selectedBehaviors.put(player.getUniqueId(), new HashSet<>());

        buildAndOpenGui(player);
    }

    private void buildAndOpenGui(Player player) {
        var menu = new HBMenu(HeadBlocks.getInstance(),
                LanguageService.getMessage("Gui.BehaviorSelectionTitle"), false, 2);

        // Borders
        int[] borders = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 14, 16, 17};
        IntStream.range(0, borders.length).map(i -> borders.length - i - 1).forEach(
                index -> menu.setItem(0, borders[index],
                        new ItemGUI(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName("§7").toItemStack()))
        );

        Set<String> selected = selectedBehaviors.getOrDefault(player.getUniqueId(), new HashSet<>());

        // Slot 11: Ordered
        menu.setItem(0, 11, createBehaviorItem("ordered",
                LanguageService.getMessage("Gui.BehaviorOrderedName"),
                LanguageService.getMessages("Gui.BehaviorOrderedLore"),
                selected.contains("ordered")));

        // Slot 12: Scheduled
        menu.setItem(0, 12, createBehaviorItem("scheduled",
                LanguageService.getMessage("Gui.BehaviorScheduledName"),
                LanguageService.getMessages("Gui.BehaviorScheduledLore"),
                selected.contains("scheduled")));

        // Slot 13: Timed
        menu.setItem(0, 13, createBehaviorItem("timed",
                LanguageService.getMessage("Gui.BehaviorTimedName"),
                LanguageService.getMessages("Gui.BehaviorTimedLore"),
                selected.contains("timed")));

        // Slot 15: Validate button
        menu.setItem(0, 15, new ItemGUI(new ItemBuilder(Material.DIAMOND)
                .setName(LanguageService.getMessage("Gui.ValidateCreate"))
                .setLore(LanguageService.getMessages("Gui.ValidateCreateLore"))
                .toItemStack(), true)
                .addOnClickEvent(event -> handleValidate((Player) event.getWhoClicked())));

        player.openInventory(menu.getInventory());
    }

    private ItemGUI createBehaviorItem(String behaviorId, String name, List<String> lore, boolean isSelected) {
        Material material = isSelected ? Material.LIME_DYE : Material.GRAY_DYE;
        String statusLine = isSelected
                ? LanguageService.getMessage("Gui.BehaviorEnabled")
                : LanguageService.getMessage("Gui.BehaviorDisabled");

        List<String> fullLore = new ArrayList<>(lore);
        fullLore.add("");
        fullLore.add(statusLine);

        return new ItemGUI(new ItemBuilder(material)
                .setName(name)
                .setLore(fullLore)
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    Player p = (Player) event.getWhoClicked();
                    toggleBehavior(p, behaviorId);
                    buildAndOpenGui(p);
                });
    }

    private void toggleBehavior(Player player, String behaviorId) {
        Set<String> selected = selectedBehaviors.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        if (selected.contains(behaviorId)) {
            selected.remove(behaviorId);
        } else {
            selected.add(behaviorId);
        }
    }

    private void handleValidate(Player player) {
        Set<String> selected = selectedBehaviors.get(player.getUniqueId());

        if (selected != null && selected.contains("timed")) {
            GuiService.getTimedConfigManager().open(player);
            return;
        }

        createHunt(player, null, true);
    }

    public void createHunt(Player player, Location plateLocation, boolean repeatable) {
        String huntName = pendingHuntNames.remove(player.getUniqueId());
        Set<String> selected = selectedBehaviors.remove(player.getUniqueId());

        if (huntName == null) {
            player.closeInventory();
            return;
        }

        String huntId = huntName.toLowerCase();
        Hunt hunt = new Hunt(huntId, huntName, HuntState.ACTIVE, 1, "PLAYER_HEAD");

        // Build behaviors list
        List<Behavior> behaviors = new ArrayList<>();
        behaviors.add(new FreeBehavior());

        if (selected != null) {
            for (String behaviorId : selected) {
                switch (behaviorId) {
                    case "ordered" -> behaviors.add(new OrderedBehavior());
                    case "scheduled" -> behaviors.add(new ScheduledBehavior(null, null));
                    case "timed" -> behaviors.add(new TimedBehavior(plateLocation, repeatable));
                }
            }
        }

        hunt.setBehaviors(behaviors);

        HuntCreateEvent createEvent = new HuntCreateEvent(hunt);
        Bukkit.getPluginManager().callEvent(createEvent);
        if (createEvent.isCancelled()) {
            player.closeInventory();
            return;
        }

        HuntConfigService.saveHunt(hunt);

        try {
            StorageService.createHuntInDb(hunt.getId(), hunt.getDisplayName(), hunt.getState().name());
        } catch (Exception e) {
            player.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            player.closeInventory();
            return;
        }

        HuntService.registerHunt(hunt);
        StorageService.incrementHuntVersion();

        player.closeInventory();

        player.sendMessage(LanguageService.getMessage("Messages.HuntCreated")
                .replaceAll("%hunt%", hunt.getId()));

        HuntService.setSelectedHunt(player.getUniqueId(), hunt.getId());
        player.sendMessage(LanguageService.getMessage("Messages.HuntSelected")
                .replaceAll("%hunt%", hunt.getId()));
    }

    public String getPendingHuntName(UUID playerUuid) {
        return pendingHuntNames.get(playerUuid);
    }

    public Set<String> getSelectedBehaviors(UUID playerUuid) {
        return selectedBehaviors.get(playerUuid);
    }

    public void clearState(UUID playerUuid) {
        pendingHuntNames.remove(playerUuid);
        selectedBehaviors.remove(playerUuid);
    }

    public boolean hasPendingCreation(UUID playerUuid) {
        return pendingHuntNames.containsKey(playerUuid);
    }
}
