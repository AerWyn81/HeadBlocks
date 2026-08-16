package fr.aerwyn81.headblocks.data.hunt.requirement.area;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public interface AreaProvider {

    String getType();

    String getWorldName();

    boolean contains(Location location);

    boolean isAvailable();

    /**
     * Short admin facing summary of the area, shown in the GUI and in {@code /hb hunt info}.
     */
    String getDescription();

    void saveTo(ConfigurationSection section);
}
