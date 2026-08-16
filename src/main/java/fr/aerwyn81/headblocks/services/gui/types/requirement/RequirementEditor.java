package fr.aerwyn81.headblocks.services.gui.types.requirement;

import fr.aerwyn81.headblocks.data.hunt.requirement.Requirement;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementType;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Configuration menu of one requirement kind.
 * <p>
 * Editors know nothing about where the requirement ends up: they take the current value (or
 * {@code null} for a new one) and hand back the configured requirement. That is what lets the same
 * editor serve the creation flow and, later, an edition flow on an existing hunt.
 */
public interface RequirementEditor {

    RequirementType getType();

    /**
     * Opens the menu.
     *
     * @param existing the requirement being edited, {@code null} when adding a new one
     * @param onDone   receives the configured requirement
     * @param onCancel called when the admin backs out without validating
     */
    void open(Player player, Requirement existing, Consumer<Requirement> onDone, Consumer<Player> onCancel);

    /**
     * Drops any pending state of a player (disconnection, menu closed for good).
     */
    void clearState(UUID playerUuid);
}
