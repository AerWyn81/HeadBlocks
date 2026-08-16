package fr.aerwyn81.headblocks.data.hunt.requirement;

import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * A condition a player must meet to claim a head of a hunt.
 * <p>
 * Requirements are evaluated on click, grouped in a {@link RequirementSet} that decides whether all
 * of them or any of them must pass. Adding a new kind of requirement means: implementing this
 * interface, registering it in {@link RequirementType}, and providing an editor for the GUI.
 */
public interface Requirement {

    RequirementType getType();

    /**
     * Checks the requirement against a player about to claim a head.
     * Implementations must never throw: an unresolvable state is a denial with a readable reason.
     */
    RequirementResult check(Player player, HeadLocation head, HBHunt hunt);

    /**
     * Short admin facing summary (GUI lore, {@code /hb hunt info}), never player facing.
     */
    String describe();

    /**
     * Writes this requirement into its own configuration section. The {@code type} key is written by
     * {@link RequirementFactory}, implementations only write their own fields.
     */
    void saveTo(ConfigurationSection section);

    /**
     * Whether every mandatory field has been filled. Incomplete requirements are refused by the GUI
     * and skipped at load time.
     */
    default boolean isComplete() {
        return true;
    }
}
