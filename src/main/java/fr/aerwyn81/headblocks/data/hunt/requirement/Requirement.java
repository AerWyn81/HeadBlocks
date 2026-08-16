package fr.aerwyn81.headblocks.data.hunt.requirement;

import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * One condition to claim a head: implement this, register it in {@link RequirementType}, done.
 */
public interface Requirement {
    RequirementType getType();

    RequirementResult check(Player player, HeadLocation head, HBHunt hunt);

    String describe();

    void saveTo(ConfigurationSection section);

    default boolean isComplete() {
        return true;
    }
}
