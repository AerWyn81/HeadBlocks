package fr.aerwyn81.headblocks.data.hunt.behavior;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.*;
import fr.aerwyn81.headblocks.data.hunt.requirement.area.AreaProvider;
import fr.aerwyn81.headblocks.data.hunt.requirement.types.AreaRequirement;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Predicate;

public class SpawnBehavior implements Behavior {

    public static final String ID = "spawn";

    public enum Placement {
        POINTS, AREA;

        public static Placement of(String value) {
            return "AREA".equalsIgnoreCase(value) ? AREA : POINTS;
        }
    }

    private final ServiceRegistry registry;
    private final Placement placement;
    private final List<SpawnPoint> points;
    private final AreaOptions area;
    private final int active;
    private final int goal;
    private final int maxTotalSpawns;
    private final SpawnCompletion completion;
    private final AfterGoal afterGoal;
    private final RespawnPolicy respawn;
    private final SpawnOptions options;
    private final Map<String, SpawnTemplate> templates;

    public SpawnBehavior(ServiceRegistry registry, Placement placement, List<SpawnPoint> points, AreaOptions area,
                         int active, int goal, int maxTotalSpawns, SpawnCompletion completion, AfterGoal afterGoal,
                         RespawnPolicy respawn, SpawnOptions options, Collection<SpawnTemplate> templates) {
        this.registry = registry;
        this.placement = placement;
        this.points = List.copyOf(points);
        this.area = area;
        this.active = Math.max(1, active);
        this.goal = Math.max(1, goal);
        this.maxTotalSpawns = maxTotalSpawns < 0 ? -1 : maxTotalSpawns;
        this.completion = completion;
        this.afterGoal = afterGoal;
        this.respawn = respawn;
        this.options = options;
        this.templates = new LinkedHashMap<>();
        templates.forEach(template -> this.templates.put(template.id(), template));
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayInfo(Player player, HBHunt hunt) {
        return registry.getLanguageService().message("Hunt.Behavior.Spawn");
    }

    public Placement placement() {
        return placement;
    }

    public List<SpawnPoint> points() {
        return points;
    }

    public AreaOptions area() {
        return area;
    }

    public SpawnBehavior withPoints(List<SpawnPoint> newPoints) {
        return new SpawnBehavior(registry, placement, newPoints, area, active, goal, maxTotalSpawns, completion,
                afterGoal, respawn, options, templates.values());
    }

    public Location pickPoint(Predicate<Location> isFree) {
        var candidates = new ArrayList<>(points);
        Collections.shuffle(candidates);

        for (SpawnPoint point : candidates) {
            var location = point.toLocation();
            if (location != null && isFree.test(location)) {
                return location;
            }
        }
        return null;
    }

    public float yawAt(Location location) {
        return points.stream()
                .filter(point -> point.matches(location))
                .findFirst()
                .map(SpawnPoint::yaw)
                .orElse(0f);
    }

    public static AreaProvider areaOf(HBHunt hunt) {
        var requirement = hunt.getRequirements().findOrNull(AreaRequirement.class);
        return requirement == null ? null : requirement.area();
    }

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

    public SpawnOptions options() {
        return options;
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
        if (!registry.getHeadService().isSpawned(head.getUuid())) {
            return BehaviorResult.allow();
        }

        return switch (registry.getSpawnService().claim(hunt, head, player)) {
            case FOUND -> BehaviorResult.allow();
            case TRAPPED -> BehaviorResult.deny(registry.getLanguageService().message("Messages.SpawnHeadTrapped"));
            case TAKEN -> BehaviorResult.deny(registry.getLanguageService().message("Messages.SpawnHeadTaken"));
        };
    }

    @Override
    public void onHeadFound(Player player, HeadLocation head, HBHunt hunt) {
        if (completion == SpawnCompletion.FIRST_WINS && hunt.isActive() && foundCount(player, hunt) >= goal) {
            registry.getSpawnService().win(hunt, player);
        }
    }

    @Override
    public void saveTo(ConfigurationSection section) {
        section.set("placement", placement.name());
        section.set("points", points.stream().map(SpawnPoint::serialize).toList());
        area.saveTo(section);
        section.set("active", active);
        section.set("goal", goal);
        section.set("maxTotalSpawns", maxTotalSpawns);
        section.set("completion", completion.name());
        section.set("afterGoal", afterGoal.name());
        respawn.saveTo(section.createSection("respawn"));
        options.saveTo(section);

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

    public static SpawnBehavior fromConfig(ServiceRegistry registry, ConfigurationSection section) {
        if (section == null) {
            return new SpawnBehavior(registry, Placement.POINTS, List.of(), AreaOptions.DEFAULT, 1, 1, -1,
                    SpawnCompletion.PER_PLAYER, AfterGoal.DENY, RespawnPolicy.DEFAULT, SpawnOptions.DEFAULT, List.of());
        }

        var points = new ArrayList<SpawnPoint>();
        for (Object raw : section.getList("points", List.of())) {
            var point = SpawnPoint.deserialize(raw);
            if (point != null) {
                points.add(point);
            }
        }

        return new SpawnBehavior(registry,
                Placement.of(section.getString("placement")),
                points,
                AreaOptions.fromConfig(section),
                section.getInt("active", 1),
                section.getInt("goal", 1),
                section.getInt("maxTotalSpawns", -1),
                SpawnCompletion.of(section.getString("completion")),
                AfterGoal.of(section.getString("afterGoal")),
                RespawnPolicy.fromConfig(section.getConfigurationSection("respawn")),
                SpawnOptions.fromConfig(section),
                readTemplates(section.getConfigurationSection("templates")));
    }

    private static List<SpawnTemplate> readTemplates(ConfigurationSection section) {
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
