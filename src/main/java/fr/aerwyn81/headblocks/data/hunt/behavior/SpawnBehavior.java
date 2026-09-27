package fr.aerwyn81.headblocks.data.hunt.behavior;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.AfterGoal;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.RespawnPolicy;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.SpawnCompletion;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.SpawnTemplate;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class SpawnBehavior implements Behavior {

    protected final ServiceRegistry registry;
    private final int active;
    private final int goal;
    private final int maxTotalSpawns;
    private final SpawnCompletion completion;
    private final AfterGoal afterGoal;
    private final RespawnPolicy respawn;
    private final Map<String, SpawnTemplate> templates;

    protected SpawnBehavior(ServiceRegistry registry, int active, int goal, int maxTotalSpawns,
                            SpawnCompletion completion, AfterGoal afterGoal, RespawnPolicy respawn,
                            Collection<SpawnTemplate> templates) {
        this.registry = registry;
        this.active = Math.max(1, active);
        this.goal = Math.max(1, goal);
        this.maxTotalSpawns = maxTotalSpawns < 0 ? -1 : maxTotalSpawns;
        this.completion = completion;
        this.afterGoal = afterGoal;
        this.respawn = respawn;
        this.templates = new LinkedHashMap<>();
        templates.forEach(template -> this.templates.put(template.id(), template));
    }

    public abstract void pickLocation(Predicate<Location> isFree, Consumer<Location> onPicked);

    public abstract float yawAt(Location location);

    protected abstract void saveSource(ConfigurationSection section);

    public int active() {
        return active;
    }

    public int goal() {
        return goal;
    }

    public int maxTotalSpawns() {
        return maxTotalSpawns;
    }

    public SpawnCompletion completion() {
        return completion;
    }

    public AfterGoal afterGoal() {
        return afterGoal;
    }

    public RespawnPolicy respawn() {
        return respawn;
    }

    public Collection<SpawnTemplate> templates() {
        return Collections.unmodifiableCollection(templates.values());
    }

    public SpawnTemplate template(String id) {
        return templates.get(id);
    }

    public SpawnTemplate pickTemplate() {
        int total = templates.values().stream().mapToInt(SpawnTemplate::weight).sum();
        if (total <= 0) {
            return null;
        }

        int roll = new Random().nextInt(total);
        for (SpawnTemplate template : templates.values()) {
            roll -= template.weight();
            if (roll < 0) {
                return template;
            }
        }
        return null;
    }

    @Override
    public OptionalInt targetCount() {
        return OptionalInt.of(goal);
    }

    @Override
    public BehaviorResult canPlayerClick(Player player, HeadLocation head, HBHunt hunt) {
        if (!registry.getHeadService().isSpawned(head.getUuid()) || afterGoal == AfterGoal.CONTINUE) {
            return BehaviorResult.allow();
        }

        if (foundCount(player, hunt) >= goal) {
            return BehaviorResult.deny(registry.getLanguageService().message("Messages.SpawnGoalReached")
                    .replace("%hunt%", hunt.getDisplayName()));
        }

        return BehaviorResult.allow();
    }

    @Override
    public BehaviorResult tryCommit(Player player, HeadLocation head, HBHunt hunt) {
        if (!registry.getHeadService().isSpawned(head.getUuid()) || registry.getSpawnService().consume(hunt, head)) {
            return BehaviorResult.allow();
        }

        return BehaviorResult.deny(registry.getLanguageService().message("Messages.SpawnHeadTaken"));
    }

    @Override
    public void onHeadFound(Player player, HeadLocation head, HBHunt hunt) {
        registry.getSpawnService().release(hunt, head);

        if (completion == SpawnCompletion.FIRST_WINS && hunt.isActive() && foundCount(player, hunt) >= goal) {
            registry.getSpawnService().win(hunt, player);
        }
    }

    @Override
    public void saveTo(ConfigurationSection section) {
        saveSource(section);
        section.set("active", active);
        section.set("goal", goal);
        section.set("maxTotalSpawns", maxTotalSpawns);
        section.set("completion", completion.name());
        section.set("afterGoal", afterGoal.name());
        respawn.saveTo(section.createSection("respawn"));

        var templatesSection = section.createSection("templates");
        templates.values().forEach(template -> template.saveTo(templatesSection.createSection(template.id())));
    }

    private int foundCount(Player player, HBHunt hunt) {
        try {
            return registry.getStorageService().getHeadsPlayerForHunt(player.getUniqueId(), hunt.getId()).size();
        } catch (InternalException e) {
            LogUtil.error("Error reading the progress of {0} in hunt {1}: {2}", player.getName(), hunt.getId(), e.getMessage());
            return 0;
        }
    }

    protected static List<SpawnTemplate> readTemplates(ConfigurationSection section) {
        var templates = new ArrayList<SpawnTemplate>();
        if (section == null) {
            return templates;
        }

        for (String id : section.getKeys(false)) {
            var template = SpawnTemplate.fromConfig(id, section.getConfigurationSection(id));
            if (template == null) {
                LogUtil.warning("Spawn template {0} has no valid content, it was ignored.", id);
                continue;
            }
            templates.add(template);
        }
        return templates;
    }
}
