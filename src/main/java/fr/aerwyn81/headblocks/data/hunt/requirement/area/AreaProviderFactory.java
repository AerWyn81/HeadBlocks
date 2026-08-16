package fr.aerwyn81.headblocks.data.hunt.requirement.area;

import org.bukkit.configuration.ConfigurationSection;

public final class AreaProviderFactory {

    private AreaProviderFactory() {
    }

    public static AreaProvider fromSection(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        String type = section.getString("type", "");
        return switch (type.toLowerCase()) {
            case WorldGuardAreaProvider.TYPE -> WorldGuardAreaProvider.fromSection(section);
            case CuboidAreaProvider.TYPE -> CuboidAreaProvider.fromSection(section);
            default -> null;
        };
    }
}
