package fr.aerwyn81.headblocks.data.hunt.requirement;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.hunt.requirement.types.*;
import fr.aerwyn81.headblocks.utils.bukkit.PluginProvider;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Catalogue of the requirement kinds: add one here and it becomes loadable, saveable and selectable.
 */
public enum RequirementType {
    AREA("area", Material.STRUCTURE_VOID, AreaRequirement::fromConfig, provider -> true, true),
    PREVIOUS_HUNT("hunt", Material.CHEST_MINECART, PreviousHuntRequirement::fromConfig),
    PERMISSION("permission", Material.NAME_TAG, PermissionRequirement::fromConfig),
    PLAYTIME("playtime", Material.CLOCK, PlaytimeRequirement::fromConfig),
    PLACEHOLDER("placeholder", Material.PAPER, PlaceholderRequirement::fromConfig,
            PluginProvider::isPlaceholderApiActive);

    private final String id;
    private final String langKey;
    private final Material icon;
    private final BiFunction<ServiceRegistry, ConfigurationSection, Requirement> loader;
    private final Predicate<PluginProvider> availability;
    private final boolean unique;

    RequirementType(String id, Material icon,
                    BiFunction<ServiceRegistry, ConfigurationSection, Requirement> loader) {
        this(id, icon, loader, provider -> true, false);
    }

    RequirementType(String id, Material icon,
                    BiFunction<ServiceRegistry, ConfigurationSection, Requirement> loader,
                    Predicate<PluginProvider> availability) {
        this(id, icon, loader, availability, false);
    }

    RequirementType(String id, Material icon,
                    BiFunction<ServiceRegistry, ConfigurationSection, Requirement> loader,
                    Predicate<PluginProvider> availability, boolean unique) {
        this.id = id;
        this.langKey = Character.toUpperCase(id.charAt(0)) + id.substring(1);
        this.icon = icon;
        this.loader = loader;
        this.availability = availability;
        this.unique = unique;
    }

    public String getId() {
        return id;
    }

    public String getLangKey() {
        return langKey;
    }

    public Material getIcon() {
        return icon;
    }

    public boolean isAvailable(PluginProvider pluginProvider) {
        return pluginProvider != null && availability.test(pluginProvider);
    }

    public boolean isUnique() {
        return unique;
    }

    public Requirement load(ServiceRegistry registry, ConfigurationSection section) {
        return loader.apply(registry, section);
    }

    public static RequirementType fromId(String id) {
        if (id == null) {
            return null;
        }

        for (RequirementType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }

        return null;
    }
}
