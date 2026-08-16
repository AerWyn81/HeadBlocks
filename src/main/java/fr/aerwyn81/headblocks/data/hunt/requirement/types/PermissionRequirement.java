package fr.aerwyn81.headblocks.data.hunt.requirement.types;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.requirement.Requirement;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementResult;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * Hold a permission node to claim a head.
 */
public class PermissionRequirement implements Requirement {
    private final ServiceRegistry registry;
    private final String node;

    public PermissionRequirement(ServiceRegistry registry, String node) {
        this.registry = registry;
        this.node = node;
    }

    public String node() {
        return node;
    }

    @Override
    public RequirementType getType() {
        return RequirementType.PERMISSION;
    }

    @Override
    public RequirementResult check(Player player, HeadLocation head, HBHunt hunt) {
        if (player.hasPermission(node)) {
            return RequirementResult.ok();
        }

        return RequirementResult.unmet(registry.getLanguageService().message("Hunt.Requirement.PermissionUnmet")
                .replace("%permission%", node));
    }

    @Override
    public String describe() {
        return registry.getLanguageService().message("Hunt.Requirement.PermissionDescription")
                .replace("%permission%", node);
    }

    @Override
    public boolean isComplete() {
        return node != null && !node.trim().isEmpty();
    }

    @Override
    public void saveTo(ConfigurationSection section) {
        section.set("permission", node);
    }

    public static PermissionRequirement fromConfig(ServiceRegistry registry, ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        return new PermissionRequirement(registry, section.getString("permission"));
    }
}
