package fr.aerwyn81.headblocks.data.hunt.requirement;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Reads and writes a single requirement in its configuration section, dispatching on the
 * {@code type} key. The envelope (the {@code type} key itself) lives here so both directions of the
 * conversion stay in one place; implementations only deal with their own fields.
 */
public final class RequirementFactory {

    private RequirementFactory() {
    }

    public static void toSection(Requirement requirement, ConfigurationSection section) {
        section.set("type", requirement.getType().getId());
        requirement.saveTo(section);
    }

    public static Requirement fromSection(ServiceRegistry registry, ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        String typeId = section.getString("type");
        RequirementType type = RequirementType.fromId(typeId);
        if (type == null) {
            LogUtil.warning("Unknown requirement type \"{0}\", skipping it.", String.valueOf(typeId));
            return null;
        }

        Requirement requirement;
        try {
            requirement = type.load(registry, section);
        } catch (Exception e) {
            LogUtil.error("Cannot read the \"{0}\" requirement: {1}", typeId, e.getMessage());
            return null;
        }

        if (requirement == null || !requirement.isComplete()) {
            LogUtil.warning("Incomplete \"{0}\" requirement, skipping it.", typeId);
            return null;
        }

        return requirement;
    }
}
