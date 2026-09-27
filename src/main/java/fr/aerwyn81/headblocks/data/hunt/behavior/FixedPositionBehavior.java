package fr.aerwyn81.headblocks.data.hunt.behavior;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.*;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class FixedPositionBehavior extends SpawnBehavior {

    public static final String ID = "fixed_position";

    private final List<SpawnPoint> points;

    public FixedPositionBehavior(ServiceRegistry registry, List<SpawnPoint> points, int active, int goal,
                                 int maxTotalSpawns, SpawnCompletion completion, AfterGoal afterGoal,
                                 RespawnPolicy respawn, Collection<SpawnTemplate> templates) {
        super(registry, active, goal, maxTotalSpawns, completion, afterGoal, respawn, templates);
        this.points = List.copyOf(points);
    }

    public List<SpawnPoint> points() {
        return points;
    }

    public FixedPositionBehavior withPoints(List<SpawnPoint> newPoints) {
        return new FixedPositionBehavior(registry, newPoints, active(), goal(), maxTotalSpawns(), completion(),
                afterGoal(), respawn(), templates());
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayInfo(Player player, HBHunt hunt) {
        return registry.getLanguageService().message("Hunt.Behavior.FixedPosition");
    }

    @Override
    public void pickLocation(Predicate<Location> isFree, Consumer<Location> onPicked) {
        var candidates = new ArrayList<>(points);
        Collections.shuffle(candidates);

        for (SpawnPoint point : candidates) {
            var location = point.toLocation();
            if (location != null && isFree.test(location)) {
                onPicked.accept(location);
                return;
            }
        }

        onPicked.accept(null);
    }

    @Override
    public float yawAt(Location location) {
        return points.stream()
                .filter(point -> point.matches(location))
                .findFirst()
                .map(SpawnPoint::yaw)
                .orElse(0f);
    }

    @Override
    protected void saveSource(ConfigurationSection section) {
        section.set("points", points.stream().map(SpawnPoint::serialize).toList());
    }

    public static FixedPositionBehavior fromConfig(ServiceRegistry registry, ConfigurationSection section) {
        if (section == null) {
            return new FixedPositionBehavior(registry, List.of(), 1, 1, -1,
                    SpawnCompletion.PER_PLAYER, AfterGoal.DENY, RespawnPolicy.DEFAULT, List.of());
        }

        var points = new ArrayList<SpawnPoint>();
        for (Object raw : section.getList("points", List.of())) {
            var point = SpawnPoint.deserialize(raw);
            if (point != null) {
                points.add(point);
            }
        }

        return new FixedPositionBehavior(registry, points,
                section.getInt("active", 1),
                section.getInt("goal", 1),
                section.getInt("maxTotalSpawns", -1),
                SpawnCompletion.of(section.getString("completion")),
                AfterGoal.of(section.getString("afterGoal")),
                RespawnPolicy.fromConfig(section.getConfigurationSection("respawn")),
                readTemplates(section.getConfigurationSection("templates")));
    }
}
