package fr.aerwyn81.headblocks.services.gui.types.requirement;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.hunt.requirement.Requirement;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementMode;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementSet;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementType;
import fr.aerwyn81.headblocks.services.gui.types.requirement.editors.*;
import fr.aerwyn81.headblocks.utils.bukkit.ItemBuilder;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The menu listing the requirements of a hunt. Callback driven, so any caller can plug into it.
 */
public class RequirementsGui {
    private static final int MAX_REQUIREMENTS = 18;
    private static final int FIRST_ENTRY_SLOT = 9;
    private static final int MODE_SLOT = 4;
    private static final int ADD_SLOT = 30;
    private static final int VALIDATE_SLOT = 32;
    private static final int ROWS = 4;
    private static final int PICKER_ROWS = 3;

    private static final class Session {
        private RequirementMode mode;
        private final List<Requirement> requirements;
        private final List<Map<String, Object>> preserved;
        private final Consumer<RequirementSet> onValidate;
        private final Consumer<Player> onCancel;

        private Session(RequirementSet initial, Consumer<RequirementSet> onValidate, Consumer<Player> onCancel) {
            this.mode = initial != null ? initial.getMode() : RequirementMode.ALL;
            this.requirements = initial != null ? new ArrayList<>(initial.getRequirements()) : new ArrayList<>();
            this.preserved = initial != null ? initial.getPreserved() : List.of();
            this.onValidate = onValidate;
            this.onCancel = onCancel;
        }

        private boolean has(RequirementType type) {
            return requirements.stream().anyMatch(requirement -> requirement.getType() == type);
        }
    }

    private final ServiceRegistry registry;
    private final Map<RequirementType, RequirementEditor> editors = new EnumMap<>(RequirementType.class);
    private final AreaRequirementEditor areaEditor;
    private final ConcurrentHashMap<UUID, Session> sessions = new ConcurrentHashMap<>();

    public RequirementsGui(ServiceRegistry registry) {
        this.registry = registry;
        this.areaEditor = new AreaRequirementEditor(registry);

        register(areaEditor);
        register(new PreviousHuntRequirementEditor(registry));
        register(new PermissionRequirementEditor(registry));
        register(new PlaytimeRequirementEditor(registry));
        register(new PlaceholderRequirementEditor(registry));
    }

    private void register(RequirementEditor editor) {
        editors.put(editor.getType(), editor);
    }

    public AreaRequirementEditor getAreaEditor() {
        return areaEditor;
    }

    public void open(Player player, RequirementSet initial,
                     Consumer<RequirementSet> onValidate, Consumer<Player> onCancel) {
        sessions.put(player.getUniqueId(), new Session(initial, onValidate, onCancel));
        buildAndOpenGui(player);
    }

    public void buildAndOpenGui(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) {
            player.closeInventory();
            return;
        }

        var menu = RequirementMenus.newMenu(registry, "Gui.RequirementsTitle", ROWS);
        RequirementMenus.fillBorders(registry, menu, ROWS);

        menu.setItem(0, MODE_SLOT, modeItem(session));

        for (int i = 0; i < session.requirements.size() && i < MAX_REQUIREMENTS; i++) {
            menu.setItem(0, FIRST_ENTRY_SLOT + i, requirementItem(session.requirements.get(i)));
        }

        menu.setItem(0, ADD_SLOT, addItem(session));
        menu.setItem(0, VALIDATE_SLOT, validateItem());

        RequirementMenus.attachBackButton(registry, menu, this::handleBack);

        player.openInventory(menu.getInventory());
    }

    // --- Items ---

    private ItemGUI modeItem(Session session) {
        boolean all = session.mode == RequirementMode.ALL;
        String modeLabel = session.mode.getLocalizedName(registry.getLanguageService());

        List<String> lore = registry.getLanguageService().messageList("Gui.RequirementsModeLore").stream()
                .map(line -> line.replace("%mode%", modeLabel))
                .collect(Collectors.toList());

        return new ItemGUI(new ItemBuilder(all ? Material.COMPARATOR : Material.REPEATER)
                .setName(registry.getLanguageService().message("Gui.RequirementsMode"))
                .setLore(lore)
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    Player player = (Player) event.getWhoClicked();
                    session.mode = session.mode.next();
                    buildAndOpenGui(player);
                });
    }

    private ItemGUI requirementItem(Requirement requirement) {
        RequirementType type = requirement.getType();

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(requirement.describe());
        lore.addAll(registry.getLanguageService().messageList("Gui.RequirementEntryLore"));

        return new ItemGUI(new ItemBuilder(type.getIcon())
                .setName(registry.getLanguageService().message("Gui.Requirement" + type.getLangKey() + "Name"))
                .setLore(lore)
                .toItemStack(), true)
                .addOnClickEvent(event -> {
                    Player player = (Player) event.getWhoClicked();
                    if (event.isRightClick()) {
                        removeRequirement(player, requirement);
                    } else {
                        editRequirement(player, requirement);
                    }
                });
    }

    private ItemGUI addItem(Session session) {
        if (session.requirements.size() >= MAX_REQUIREMENTS) {
            return new ItemGUI(new ItemBuilder(Material.BARRIER)
                    .setName(registry.getLanguageService().message("Gui.RequirementsAdd"))
                    .setLore(registry.getLanguageService().messageList("Gui.RequirementsAddFullLore"))
                    .toItemStack());
        }

        return new ItemGUI(new ItemBuilder(Material.LIME_DYE)
                .setName(registry.getLanguageService().message("Gui.RequirementsAdd"))
                .setLore(registry.getLanguageService().messageList("Gui.RequirementsAddLore"))
                .toItemStack(), true)
                .addOnClickEvent(event -> openTypePicker((Player) event.getWhoClicked()));
    }

    private ItemGUI validateItem() {
        return new ItemGUI(new ItemBuilder(Material.DIAMOND)
                .setName(registry.getLanguageService().message("Gui.ValidateCreate"))
                .setLore(registry.getLanguageService().messageList("Gui.RequirementsValidateLore"))
                .toItemStack(), true)
                .addOnClickEvent(event -> handleValidate((Player) event.getWhoClicked()));
    }

    // --- Type picker ---

    private void openTypePicker(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) {
            player.closeInventory();
            return;
        }

        var menu = RequirementMenus.newMenu(registry, "Gui.RequirementPickerTitle", PICKER_ROWS);
        RequirementMenus.fillBorders(registry, menu, PICKER_ROWS);

        int slot = 10;
        for (RequirementType type : RequirementType.values()) {
            menu.setItem(0, slot, typeItem(type, session));
            slot++;
        }

        RequirementMenus.attachBackButton(registry, menu, this::buildAndOpenGui);

        player.openInventory(menu.getInventory());
    }

    private ItemGUI typeItem(RequirementType type, Session session) {
        String name = registry.getLanguageService().message("Gui.Requirement" + type.getLangKey() + "Name");
        List<String> lore = new ArrayList<>(
                registry.getLanguageService().messageList("Gui.Requirement" + type.getLangKey() + "Lore"));

        if (!type.isAvailable(registry.getPluginProvider())) {
            lore.addAll(registry.getLanguageService().messageList("Gui.RequirementUnavailableLore"));
            return new ItemGUI(new ItemBuilder(Material.BARRIER).setName(name).setLore(lore).toItemStack());
        }

        if (type.isUnique() && session.has(type)) {
            lore.addAll(registry.getLanguageService().messageList("Gui.RequirementAlreadyAddedLore"));
            return new ItemGUI(new ItemBuilder(Material.BARRIER).setName(name).setLore(lore).toItemStack());
        }

        return new ItemGUI(new ItemBuilder(type.getIcon()).setName(name).setLore(lore).toItemStack(), true)
                .addOnClickEvent(event -> addRequirement((Player) event.getWhoClicked(), type));
    }

    // --- Actions ---

    private void addRequirement(Player player, RequirementType type) {
        Session opening = sessions.get(player.getUniqueId());
        RequirementEditor editor = editors.get(type);

        if (editor == null || opening == null || (type.isUnique() && opening.has(type))) {
            buildAndOpenGui(player);
            return;
        }

        editor.open(player, null, requirement -> {
            Session session = sessions.get(player.getUniqueId());
            if (session != null && requirement != null && !(type.isUnique() && session.has(type))) {
                session.requirements.add(requirement);
            }
            buildAndOpenGui(player);
        }, this::buildAndOpenGui);
    }

    private void editRequirement(Player player, Requirement requirement) {
        RequirementEditor editor = editors.get(requirement.getType());
        if (editor == null) {
            buildAndOpenGui(player);
            return;
        }

        editor.open(player, requirement, edited -> {
            Session session = sessions.get(player.getUniqueId());
            if (session != null && edited != null) {
                int index = session.requirements.indexOf(requirement);
                if (index >= 0) {
                    session.requirements.set(index, edited);
                } else {
                    session.requirements.add(edited);
                }
            }
            buildAndOpenGui(player);
        }, this::buildAndOpenGui);
    }

    private void removeRequirement(Player player, Requirement requirement) {
        Session session = sessions.get(player.getUniqueId());
        if (session != null) {
            session.requirements.remove(requirement);
        }
        buildAndOpenGui(player);
    }

    private void handleValidate(Player player) {
        Session session = sessions.remove(player.getUniqueId());
        if (session == null) {
            player.closeInventory();
            return;
        }

        RequirementSet result = new RequirementSet(registry, session.mode, session.requirements, session.preserved);
        if (session.onValidate != null) {
            session.onValidate.accept(result);
        }
    }

    private void handleBack(Player player) {
        Session session = sessions.remove(player.getUniqueId());
        if (session == null) {
            player.closeInventory();
            return;
        }

        if (session.onCancel != null) {
            session.onCancel.accept(player);
        }
    }

    public void clearState(UUID playerUuid) {
        sessions.remove(playerUuid);
        editors.values().forEach(editor -> editor.clearState(playerUuid));
    }
}
