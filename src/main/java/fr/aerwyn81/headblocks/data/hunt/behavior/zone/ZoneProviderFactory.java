package fr.aerwyn81.headblocks.data.hunt.behavior.zone;

import org.bukkit.configuration.ConfigurationSection;

public final class ZoneProviderFactory {

    private ZoneProviderFactory() {
    }

    public static ZoneProvider fromSection(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        String type = section.getString("type", "");
        return switch (type.toLowerCase()) {
            case WorldGuardZoneProvider.TYPE -> WorldGuardZoneProvider.fromSection(section);
            case CuboidZoneProvider.TYPE -> CuboidZoneProvider.fromSection(section);
            default -> null;
        };
    }
}
