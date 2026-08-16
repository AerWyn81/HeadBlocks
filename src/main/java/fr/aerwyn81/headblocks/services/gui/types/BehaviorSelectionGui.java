package fr.aerwyn81.headblocks.services.gui.types;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.api.events.HuntCreateEvent;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.data.hunt.behavior.*;
import fr.aerwyn81.headblocks.data.hunt.behavior.schedule.ScheduleMode;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementSet;
import fr.aerwyn81.headblocks.utils.bukkit.ItemBuilder;
import fr.aerwyn81.headblocks.utils.gui.HBMenu;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BehaviorSelectionGui {

    private final ServiceRegistry registry;
    private final ConcurrentHashMap<UUID, Set<String>> selectedBehaviors = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> pendingHuntNames = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, RequirementSet> pendingRequirements = new ConcurrentHashMap<>();

    public BehaviorSelectionGui(ServiceRegistry registry) {
        this.registry = registry;
    }

    public void open(Player player, String huntName) {
        pendingHuntNames.put(player.getUniqueId(), huntName);
        selectedBehaviors.put(player.getUniqueId(), new HashSet<>());
        pendingRequirements.remove(player.getUniqueId());

        buildAndOpenGui(player);
    }

    public void buildAndOpenGui(Player player) {
        var menu = new HBMenu(registry.getPluginProvider().getJavaPlugin(), registry.getGuiService(),
                registry.getLanguageService().message("Gui.BehaviorSelectionTitle"), false, 2);

        // Borders
        int[] borders = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 14, 16, 17};
        IntStream.range(0, borders.length).map(i -> borders.length - i - 1).forEach(
                index -> menu.setItem(0, borders[index],
                        new ItemGUI(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName("§7").toItemStack()))
        );

        Set<String> selected = selectedBehaviors.getOrDefault(player.getUniqueId(), new HashSet<>());

        // Slot 10: Requirements (not a behavior: it opens its own menu)
        menu.setItem(0, 10, createRequirementsItem(player));

        // Slot 11: Ordered
        menu.setItem(0, 11, createBehaviorItem("ordered",
                registry.getLanguageService().message("Gui.BehaviorOrderedName"),
                registry.getLanguageService().messageList("Gui.BehaviorOrderedLore"),
                selected.contains("ordered")));

        // Slot 12: Scheduled
        menu.setItem(0, 12, createBehaviorItem("scheduled",
                registry.getLanguageService().message("Gui.BehaviorScheduledName"),
                registry.getLanguageService().messageList("Gui.BehaviorScheduledLore"),
                selected.contains("scheduled")));

        // Slot 13: Timed
        menu.setItem(0, 13, createBehaviorItem("timed",
                registry.getLanguageService().message("Gui.BehaviorTimedName"),
                registry.getLanguageService().messageList("Gui.BehaviorTimedLore"),
                selected.contains("timed")));

        // Slot 15: Validate button
        menu.setItem(0, 15, new ItemGUI(new ItemBuilder(Material.DIAMOND)
                .setName(registry.getLanguageService().message("Gui.ValidateCreate"))
                .setLore(registry.getLanguageService().messageList("Gui.ValidateCreateLore"))
                .toItemStack(), true)
                .addOnClickEvent(event -> handleValidate((Player) event.getWhoClicked())));

        player.openInventory(menu.getInventory());
    }

    private ItemGUI createRequirementsItem(Player player) {
        RequirementSet requirements = pendingRequirements.get(player.getUniqueId());
        int count = requirements != null ? requirements.size() : 0;

        List<String> lore = registry.getLanguageService().messageList("Gui.BehaviorRequirementsLore").stream()
                .map(line -> line.replace("%count%", String.valueOf(count)))
                .collect(Collectors.toList());

        return new ItemGUI(new ItemBuilder(count > 0 ? Material.WRITTEN_BOOK : Material.BOOK)
                .setName(registry.getLanguageService().message("Gui.BehaviorRequirementsName"))
                .setLore(lore)
                .toItemStack(), true)
                .addOnClickEvent(event -> openRequirements((Player) event.getWhoClicked()));
    }

    private void openRequirements(Player player) {
        UUID uuid = player.getUniqueId();

        registry.getGuiService().getRequirementsGui().open(player, pendingRequirements.get(uuid),
                set -> {
                    if (set == null || set.isEmpty()) {
                        pendingRequirements.remove(uuid);
                    } else {
                        pendingRequirements.put(uuid, set);
                    }
                    buildAndOpenGui(player);
                },
                this::buildAndOpenGui);
    }

    private ItemGUI createBehaviorItem(String behaviorId, String name, List<String> lore, boolean isSelected) {
        Material material = isSelected ? Material.LIME_DYE : Material.GRAY_DYE;
        String statusLine = isSelected
                ? registry.getLanguageService().message("Gui.BehaviorEnabled")
                : registry.getLanguageService().message("Gui.BehaviorDisabled");

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
            registry.getGuiService().getTimedConfigManager().open(player);
            return;
        }

        if (selected != null && selected.contains("scheduled")) {
            registry.getGuiService().getScheduledConfigManager().open(player, null, true, 0, false);
            return;
        }

        createHunt(player, null, true, 0, false, null);
    }

    public void createHunt(Player player, Location plateLocation, boolean repeatable,
                           int limitSeconds, boolean resetOnExpire, ScheduleMode scheduleMode) {
        String huntName = pendingHuntNames.remove(player.getUniqueId());
        Set<String> selected = selectedBehaviors.remove(player.getUniqueId());
        RequirementSet requirements = pendingRequirements.remove(player.getUniqueId());

        if (huntName == null) {
            player.closeInventory();
            return;
        }

        String huntId = huntName.toLowerCase();
        HBHunt hunt = new HBHunt(registry.getConfigService(), huntId, huntName, HuntState.ACTIVE, 1, "PLAYER_HEAD");

        // Build behaviors list
        List<Behavior> behaviors = new ArrayList<>();
        behaviors.add(new FreeBehavior());

        if (selected != null) {
            for (String behaviorId : selected) {
                switch (behaviorId) {
                    case "ordered" -> behaviors.add(new OrderedBehavior(registry));
                    case "scheduled" -> behaviors.add(new ScheduledBehavior(registry, scheduleMode));
                    case "timed" ->
                            behaviors.add(new TimedBehavior(registry, plateLocation, repeatable, limitSeconds, resetOnExpire));
                    default -> {
                        // Unknown toggle, nothing to build.
                    }
                }
            }
        }

        hunt.setBehaviors(behaviors);
        hunt.setRequirements(requirements);

        HuntCreateEvent createEvent = new HuntCreateEvent(hunt);
        Bukkit.getPluginManager().callEvent(createEvent);
        if (createEvent.isCancelled()) {
            player.closeInventory();
            return;
        }

        registry.getHuntConfigService().saveHunt(hunt);

        try {
            registry.getStorageService().createHuntInDb(hunt.getId(), hunt.getDisplayName(), hunt.getState().name());
        } catch (Exception e) {
            player.sendMessage(registry.getLanguageService().message("Messages.StorageError"));
            player.closeInventory();
            return;
        }

        registry.getHuntService().registerHunt(hunt);
        registry.getStorageService().incrementHuntVersion();

        player.closeInventory();

        player.sendMessage(registry.getLanguageService().message("Messages.HuntCreated")
                .replace("%hunt%", hunt.getId()));

        registry.getHuntService().setSelectedHunt(player.getUniqueId(), hunt.getId());
        player.sendMessage(registry.getLanguageService().message("Messages.HuntSelected")
                .replace("%hunt%", hunt.getId()));

        if (selected != null && selected.contains("ordered")) {
            player.sendMessage(registry.getLanguageService().message("Messages.HuntOrderedHint"));
        }
    }

    public Set<String> getSelectedBehaviors(UUID playerUuid) {
        return selectedBehaviors.get(playerUuid);
    }

    public void clearState(UUID playerUuid) {
        pendingHuntNames.remove(playerUuid);
        selectedBehaviors.remove(playerUuid);
        pendingRequirements.remove(playerUuid);
    }
}
