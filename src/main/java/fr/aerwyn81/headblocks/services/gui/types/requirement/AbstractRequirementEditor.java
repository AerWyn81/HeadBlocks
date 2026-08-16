package fr.aerwyn81.headblocks.services.gui.types.requirement;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.hunt.requirement.Requirement;
import fr.aerwyn81.headblocks.utils.bukkit.ItemBuilder;
import fr.aerwyn81.headblocks.utils.gui.HBMenu;
import fr.aerwyn81.headblocks.utils.gui.ItemGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Shared plumbing of the requirement editors: the callbacks of the current edition and the few
 * items every editor draws the same way.
 */
public abstract class AbstractRequirementEditor implements RequirementEditor {

    protected final ServiceRegistry registry;

    private record Callbacks(Consumer<Requirement> onDone, Consumer<Player> onCancel) {
    }

    private final ConcurrentHashMap<UUID, Callbacks> callbacks = new ConcurrentHashMap<>();

    protected AbstractRequirementEditor(ServiceRegistry registry) {
        this.registry = registry;
    }

    protected void rememberCallbacks(Player player, Consumer<Requirement> onDone, Consumer<Player> onCancel) {
        callbacks.put(player.getUniqueId(), new Callbacks(onDone, onCancel));
    }

    /**
     * Hands the finished requirement back to the caller and forgets the edition.
     */
    protected void finish(Player player, Requirement requirement) {
        Callbacks pending = callbacks.remove(player.getUniqueId());
        clearState(player.getUniqueId());

        if (pending != null && pending.onDone() != null) {
            pending.onDone().accept(requirement);
        }
    }

    /**
     * Aborts the edition and returns to the caller.
     */
    protected void cancel(Player player) {
        Callbacks pending = callbacks.remove(player.getUniqueId());
        clearState(player.getUniqueId());

        if (pending != null && pending.onCancel() != null) {
            pending.onCancel().accept(player);
        }
    }

    /**
     * Forgets everything about a player: the edition in progress and the fields being filled.
     */
    @Override
    public void clearState(UUID playerUuid) {
        callbacks.remove(playerUuid);
        clearFields(playerUuid);
    }

    /**
     * Drops the per-player values of this editor.
     */
    protected abstract void clearFields(UUID playerUuid);

    protected HBMenu newMenu(String titleKey, int rows) {
        return RequirementMenus.newMenu(registry, titleKey, rows);
    }

    protected void fillBorders(HBMenu menu, int rows) {
        RequirementMenus.fillBorders(registry, menu, rows);
    }

    /**
     * Back button, drawn in place of the pagination close button like the other config menus.
     */
    protected void attachBackButton(HBMenu menu) {
        RequirementMenus.attachBackButton(registry, menu, this::cancel);
    }

    protected ItemGUI validateItem(Runnable onValidate) {
        return new ItemGUI(new ItemBuilder(Material.DIAMOND)
                .setName(registry.getLanguageService().message("Gui.ValidateCreate"))
                .setLore(registry.getLanguageService().messageList("Gui.ValidateCreateLore"))
                .toItemStack(), true)
                .addOnClickEvent(event -> onValidate.run());
    }

    protected ItemGUI blockedValidateItem(String loreKey) {
        return new ItemGUI(new ItemBuilder(Material.BARRIER)
                .setName(registry.getLanguageService().message("Gui.ValidateBlocked"))
                .setLore(registry.getLanguageService().messageList(loreKey))
                .toItemStack());
    }

    protected ItemGUI fieldItem(Material material, String nameKey, String loreKey, String placeholder, String value) {
        List<String> lore = registry.getLanguageService().messageList(loreKey).stream()
                .map(line -> line.replace(placeholder, value))
                .collect(Collectors.toList());

        return new ItemGUI(new ItemBuilder(material)
                .setName(registry.getLanguageService().message(nameKey))
                .setLore(lore)
                .toItemStack(), true);
    }

    protected String notDefined() {
        return registry.getLanguageService().message("Gui.RequirementNotDefined");
    }
}
