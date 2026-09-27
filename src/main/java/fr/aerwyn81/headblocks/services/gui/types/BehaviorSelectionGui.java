package fr.aerwyn81.headblocks.services.gui.types;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.api.events.HuntCreateEvent;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.data.hunt.behavior.*;
import fr.aerwyn81.headblocks.data.hunt.behavior.schedule.ScheduleMode;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.SpawnDraft;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementSet;
import fr.aerwyn81.headblocks.data.hunt.requirement.types.AreaRequirement;
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
    private static final String ORDERED = "ordered";
    private static final String SCHEDULED = "scheduled";
    private static final String TIMED = "timed";

    private final ServiceRegistry registry;
    private final ConcurrentHashMap<UUID, Set<String>> selectedBehaviors = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> pendingHuntNames = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, RequirementSet> pendingRequirements = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, SpawnDraft> pendingSpawn = new ConcurrentHashMap<>();

    public BehaviorSelectionGui(ServiceRegistry registry) {
        this.registry = registry;
    }

    public void open(Player player, String huntName) {
        pendingHuntNames.put(player.getUniqueId(), huntName);
        selectedBehaviors.put(player.getUniqueId(), new HashSet<>());
        pendingRequirements.remove(player.getUniqueId());
        pendingSpawn.remove(player.getUniqueId());

        buildAndOpenGui(player);
    }

    public void buildAndOpenGui(Player player) {
        var menu = new HBMenu(registry.getPluginProvider().getJavaPlugin(), registry.getGuiService(),
                registry.getLanguageService().message("Gui.BehaviorSelectionTitle"), false, 2);

        // Borders
        int[] borders = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 16, 17};
        IntStream.range(0, borders.length).map(i -> borders.length - i - 1).forEach(
                index -> menu.setItem(0, borders[index],
                        new ItemGUI(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName("§7").toItemStack()))
        );

        Set<String> selected = selectedBehaviors.getOrDefault(player.getUniqueId(), new HashSet<>());

        menu.setItem(0, 10, createRequirementsItem(player));

        // Slot 11: Ordered
        menu.setItem(0, 11, createBehaviorItem(ORDERED,
                registry.getLanguageService().message("Gui.BehaviorOrderedName"),
                registry.getLanguageService().messageList("Gui.BehaviorOrderedLore"),
                selected.contains(ORDERED)));

        // Slot 12: Scheduled
        menu.setItem(0, 12, createBehaviorItem(SCHEDULED,
                registry.getLanguageService().message("Gui.BehaviorScheduledName"),
                registry.getLanguageService().messageList("Gui.BehaviorScheduledLore"),
                selected.contains(SCHEDULED)));

        // Slot 13: Timed
        menu.setItem(0, 13, createBehaviorItem(TIMED,
                registry.getLanguageService().message("Gui.BehaviorTimedName"),
                registry.getLanguageService().messageList("Gui.BehaviorTimedLore"),
                selected.contains(TIMED)));

        menu.setItem(0, 14, createBehaviorItem(SpawnBehavior.ID,
                registry.getLanguageService().message("Gui.BehaviorSpawnName"),
                registry.getLanguageService().messageList("Gui.BehaviorSpawnLore"),
                selected.contains(SpawnBehavior.ID)));

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
                .toList();

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
        String statusLine = registry.getLanguageService().message(isSelected ? "Gui.BehaviorEnabled" : "Gui.BehaviorDisabled");

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
            return;
        }

        if (EXCLUSIVE.contains(behaviorId)) {
            selected.removeAll(EXCLUSIVE);
            pendingSpawn.remove(player.getUniqueId());
        }
        selected.add(behaviorId);
    }

    private static final Set<String> EXCLUSIVE = Set.of(ORDERED, SpawnBehavior.ID);

    private boolean hasArea(Player player) {
        var requirements = pendingRequirements.get(player.getUniqueId());
        return requirements != null && requirements.findOrNull(AreaRequirement.class) != null;
    }

    private void handleValidate(Player player) {
        Set<String> selected = selectedBehaviors.get(player.getUniqueId());

        if (selected != null && selected.contains(SpawnBehavior.ID)) {
            var draft = pendingSpawn.get(player.getUniqueId());
            if (draft != null && draft.placement == SpawnBehavior.Placement.AREA && !hasArea(player)) {
                player.sendMessage(registry.getLanguageService().message("Messages.SpawnNeedsArea"));
                openRequirements(player);
                return;
            }
        }

        if (selected != null && selected.contains(SpawnBehavior.ID) && !pendingSpawn.containsKey(player.getUniqueId())) {
            registry.getGuiService().getSpawnConfigGui().open(player, new SpawnDraft(),
                    draft -> {
                        pendingSpawn.put(player.getUniqueId(), draft);
                        handleValidate(player);
                    },
                    this::buildAndOpenGui);
            return;
        }

        if (selected != null && selected.contains(TIMED)) {
            registry.getGuiService().getTimedConfigManager().open(player);
            return;
        }

        if (selected != null && selected.contains(SCHEDULED)) {
            registry.getGuiService().getScheduledConfigManager().open(player, null, true, 0, false);
            return;
        }

        createHunt(player, null, true, 0, false, null);
    }

    private List<Behavior> buildBehaviors(Set<String> selected, Location plateLocation, boolean repeatable, int limitSeconds,
                                          boolean resetOnExpire, ScheduleMode scheduleMode, SpawnDraft spawnDraft) {
        List<Behavior> behaviors = new ArrayList<>();
        behaviors.add(new FreeBehavior());

        if (selected != null) {
            for (String behaviorId : selected) {
                if (ORDERED.equals(behaviorId)) {
                    behaviors.add(new OrderedBehavior(registry));
                } else if (SCHEDULED.equals(behaviorId)) {
                    behaviors.add(new ScheduledBehavior(registry, scheduleMode));
                } else if (TIMED.equals(behaviorId)) {
                    behaviors.add(new TimedBehavior(registry, plateLocation, repeatable, limitSeconds, resetOnExpire));
                } else if (SpawnBehavior.ID.equals(behaviorId) && spawnDraft != null) {
                    behaviors.add(spawnDraft.build(registry));
                }
            }
        }
        return behaviors;
    }

    public void createHunt(Player player, Location plateLocation, boolean repeatable,
                           int limitSeconds, boolean resetOnExpire, ScheduleMode scheduleMode) {
        String huntName = pendingHuntNames.remove(player.getUniqueId());
        Set<String> selected = selectedBehaviors.remove(player.getUniqueId());
        RequirementSet requirements = pendingRequirements.remove(player.getUniqueId());
        SpawnDraft spawnDraft = pendingSpawn.remove(player.getUniqueId());

        if (huntName == null) {
            player.closeInventory();
            return;
        }

        String huntId = huntName.toLowerCase();
        HBHunt hunt = new HBHunt(registry.getConfigService(), huntId, huntName, HuntState.ACTIVE, 1, "PLAYER_HEAD");

        // Build behaviors list
        List<Behavior> behaviors = buildBehaviors(selected, plateLocation, repeatable, limitSeconds, resetOnExpire, scheduleMode, spawnDraft);

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
        if (spawnDraft != null && spawnDraft.placement == SpawnBehavior.Placement.AREA) {
            registry.getSpawnService().refresh(hunt);
        }

        player.closeInventory();

        player.sendMessage(registry.getLanguageService().message("Messages.HuntCreated")
                .replace("%hunt%", hunt.getId()));

        registry.getHuntService().setSelectedHunt(player.getUniqueId(), hunt.getId());
        player.sendMessage(registry.getLanguageService().message("Messages.HuntSelected")
                .replace("%hunt%", hunt.getId()));

        if (selected != null && selected.contains(ORDERED)) {
            player.sendMessage(registry.getLanguageService().message("Messages.HuntOrderedHint"));
        }

        if (spawnDraft != null && spawnDraft.placement == SpawnBehavior.Placement.POINTS) {
            player.sendMessage(registry.getLanguageService().message("Messages.HuntSpawnPointsHint")
                    .replace("%hunt%", hunt.getId()));
        }
    }

    public Set<String> getSelectedBehaviors(UUID playerUuid) {
        return selectedBehaviors.get(playerUuid);
    }

    public void clearState(UUID playerUuid) {
        pendingHuntNames.remove(playerUuid);
        selectedBehaviors.remove(playerUuid);
        pendingRequirements.remove(playerUuid);
        pendingSpawn.remove(playerUuid);
    }
}
