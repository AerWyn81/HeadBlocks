package fr.aerwyn81.headblocks.data.hunt.requirement;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * The requirements attached to a hunt, plus the way they combine.
 * <p>
 * An empty set never blocks anything. A set is immutable: the GUI edits a working copy and hands a
 * new instance to the hunt, which keeps the evaluation path free of concurrent mutations.
 */
public class RequirementSet {

    private final ServiceRegistry registry;
    private final RequirementMode mode;
    private final List<Requirement> requirements;

    /**
     * Entries read from the file that no requirement could be built from: an unknown type, a loader
     * error, or a requirement that is not complete yet (an unloaded world, typically).
     * <p>
     * They are kept verbatim and written back untouched, because saving rewrites the whole section:
     * dropping them from memory would delete them from disk on the next unrelated save.
     */
    private final List<Map<String, Object>> preserved;

    public RequirementSet(ServiceRegistry registry) {
        this(registry, RequirementMode.ALL, List.of());
    }

    /**
     * Set of a hunt with no requirement. It needs no registry: an empty set is answered before any
     * message has to be built.
     */
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
        return Collections.unmodifiableList(requirements);
    }

    /**
     * Raw entries the loader could not turn into a requirement. Callers rebuilding a set (the GUI,
     * the legacy migration) must carry them over so they survive the save.
     */
    public List<Map<String, Object>> getPreserved() {
        return Collections.unmodifiableList(preserved);
    }

    public boolean isEmpty() {
        return requirements.isEmpty();
    }

    public int size() {
        return requirements.size();
    }

    /**
     * First requirement of the given kind, for the features that need to read a specific one
     * (area confinement, for instance).
     */
    public <T extends Requirement> Optional<T> find(Class<T> type) {
        return Optional.ofNullable(findOrNull(type));
    }

    /**
     * Allocation free variant of {@link #find(Class)}, for the paths that run on every player move.
     */
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

    /**
     * Evaluates the set for a player clicking a head.
     * <p>
     * The returned reason is the ready to send player message: a header stating whether all or one
     * of the requirements are needed, followed by one line per blocking requirement.
     */
    public RequirementResult evaluate(Player player, HeadLocation head, HBHunt hunt) {
        if (requirements.isEmpty()) {
            return RequirementResult.ok();
        }

        List<String> blocking = new ArrayList<>();
        for (Requirement requirement : requirements) {
            RequirementResult result = requirement.check(player, head, hunt);

            // A requirement nobody can evaluate counts neither way: it must not block a click, and
            // under ANY it must not stand in for the condition the admin actually asked for.
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

        // Written back exactly as they were read, after the entries we understand.
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
            // Entries are written under numeric keys: read them back in that order rather than in
            // whatever order the YAML mapping happens to yield.
            List<String> keys = new ArrayList<>(list.getKeys(false));
            keys.sort(RequirementSet::compareIndexKeys);

            for (String key : keys) {
                ConfigurationSection entry = list.getConfigurationSection(key);
                Requirement requirement = RequirementFactory.fromSection(registry, entry);

                if (requirement != null) {
                    loaded.add(requirement);
                } else if (entry != null) {
                    // Skipped, not dropped: the factory already said why on the console, and the
                    // entry is kept so the next save does not erase it from the file.
                    preserved.add(snapshot(entry));
                }
            }
        }

        return new RequirementSet(registry, mode, loaded, preserved);
    }

    /**
     * Flat copy of a configuration section, leaves only, so the values can be written into a fresh
     * section without sharing anything with the document they were read from.
     */
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
