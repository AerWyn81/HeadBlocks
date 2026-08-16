package fr.aerwyn81.headblocks.data.hunt.requirement.area;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

/**
 * A shape that can say whether a location is inside it, whatever actually draws it.
 */
public interface AreaProvider {
    String getType();

    String getWorldName();

    boolean contains(Location location);

    boolean isAvailable();

    String getDescription();

    void saveTo(ConfigurationSection section);
}
