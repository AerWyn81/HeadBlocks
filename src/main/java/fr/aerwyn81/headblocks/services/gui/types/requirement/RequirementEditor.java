package fr.aerwyn81.headblocks.services.gui.types.requirement;

import fr.aerwyn81.headblocks.data.hunt.requirement.Requirement;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementType;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * The per-type editor the requirements menu opens to fill one requirement in.
 */
public interface RequirementEditor {
    RequirementType getType();

    void open(Player player, Requirement existing, Consumer<Requirement> onDone, Consumer<Player> onCancel);

    void clearState(UUID playerUuid);
}
