package fr.aerwyn81.headblocks.data.hunt.behavior.zone;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public interface ZoneProvider {

    String getType();

    String getWorldName();

    boolean contains(Location location);

    boolean isAvailable();

    void saveTo(ConfigurationSection section);
}
