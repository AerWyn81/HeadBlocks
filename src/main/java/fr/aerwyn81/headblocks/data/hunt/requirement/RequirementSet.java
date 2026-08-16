package fr.aerwyn81.headblocks.data.hunt.requirement;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * The requirements of a hunt and how they combine. Immutable, so evaluation never races an edit.
 */
public class RequirementSet {
    private final ServiceRegistry registry;
    private final RequirementMode mode;
    private final List<Requirement> requirements;

    private final List<Map<String, Object>> preserved;

    public RequirementSet(ServiceRegistry registry) {
        this(registry, RequirementMode.ALL, List.of());
    }

    public static RequirementSet empty() {
        return new RequirementSet(null);
    }

    public RequirementSet(ServiceRegistry registry, RequirementMode mode, List<Requirement> requirements) {
        this(registry, mode, requirements, List.of());
    }

    public RequirementSet(ServiceRegistry registry, RequirementMode mode, List<Requirement> requirements,
                          List<Map<String, Object>> preserved) {
        this.registry = registry;
        this.mode = mode != null ? mode : RequirementMode.ALL;
        this.requirements = requirements == null ? List.of() : List.copyOf(requirements);
        this.preserved = preserved == null ? List.of() : List.copyOf(preserved);
    }

    public RequirementMode getMode() {
        return mode;
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }

    public List<Map<String, Object>> getPreserved() {
        return preserved;
    }

    public boolean isEmpty() {
        return requirements.isEmpty();
    }

    public int size() {
        return requirements.size();
    }

    public <T extends Requirement> Optional<T> find(Class<T> type) {
        return Optional.ofNullable(findOrNull(type));
    }

    public <T extends Requirement> T findOrNull(Class<T> type) {
        for (Requirement requirement : requirements) {
            if (type.isInstance(requirement)) {
                return type.cast(requirement);
            }
        }
        return null;
    }

    public RequirementSet without(Requirement requirement) {
        List<Requirement> kept = new ArrayList<>(requirements);
        kept.remove(requirement);
        return new RequirementSet(registry, mode, kept);
    }

    public RequirementResult evaluate(Player player, HeadLocation head, HBHunt hunt) {
        if (requirements.isEmpty()) {
            return RequirementResult.ok();
        }

        List<String> blocking = new ArrayList<>();
        for (Requirement requirement : requirements) {
            RequirementResult result = requirement.check(player, head, hunt);

            if (result.isUnresolvable()) {
                continue;
            }

            if (result.satisfied()) {
                if (mode == RequirementMode.ANY) {
                    return RequirementResult.ok();
                }
                continue;
            }

            blocking.add(result.reason());
        }

        if (blocking.isEmpty()) {
            return RequirementResult.ok();
        }

        return RequirementResult.unmet(buildMessage(blocking));
    }

    private String buildMessage(List<String> blocking) {
        String headerKey = mode == RequirementMode.ANY
                ? "Hunt.Requirement.DeniedAny"
                : "Hunt.Requirement.DeniedAll";

        StringBuilder builder = new StringBuilder(registry.getLanguageService().message(headerKey));
        String lineFormat = registry.getLanguageService().message("Hunt.Requirement.DeniedLine");

        for (String reason : blocking) {
            if (reason == null || reason.trim().isEmpty()) {
                continue;
            }
            builder.append('\n').append(lineFormat.replace("%reason%", reason));
        }

        return builder.toString();
    }

    public void saveTo(ConfigurationSection section) {
        section.set("mode", mode.name());

        int index = 0;
        for (Requirement requirement : requirements) {
            RequirementFactory.toSection(requirement, section.createSection("list." + index));
            index++;
        }

        for (Map<String, Object> raw : preserved) {
            ConfigurationSection entry = section.createSection("list." + index);
            raw.forEach(entry::set);
            index++;
        }
    }

    public static RequirementSet fromSection(ServiceRegistry registry, ConfigurationSection section) {
        if (section == null) {
            return new RequirementSet(registry);
        }

        RequirementMode mode = RequirementMode.fromString(section.getString("mode"));
        List<Requirement> loaded = new ArrayList<>();
        List<Map<String, Object>> preserved = new ArrayList<>();

        ConfigurationSection list = section.getConfigurationSection("list");
        if (list != null) {
            List<String> keys = new ArrayList<>(list.getKeys(false));
            keys.sort(RequirementSet::compareIndexKeys);

            for (String key : keys) {
                ConfigurationSection entry = list.getConfigurationSection(key);
                Requirement requirement = RequirementFactory.fromSection(registry, entry);

                if (requirement != null) {
                    loaded.add(requirement);
                } else if (entry != null) {
                    preserved.add(snapshot(entry));
                }
            }
        }

        return new RequirementSet(registry, mode, loaded, preserved);
    }

    public static Map<String, Object> snapshot(ConfigurationSection section) {
        Map<String, Object> values = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : section.getValues(true).entrySet()) {
            if (!(entry.getValue() instanceof ConfigurationSection)) {
                values.put(entry.getKey(), entry.getValue());
            }
        }

        return values;
    }

    private static int compareIndexKeys(String left, String right) {
        try {
            return Integer.compare(Integer.parseInt(left), Integer.parseInt(right));
        } catch (NumberFormatException e) {
            return left.compareTo(right);
        }
    }
}
