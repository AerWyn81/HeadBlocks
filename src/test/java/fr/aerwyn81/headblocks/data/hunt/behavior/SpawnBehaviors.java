package fr.aerwyn81.headblocks.data.hunt.behavior;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.*;
import org.bukkit.Material;

import java.util.Collection;
import java.util.List;

public final class SpawnBehaviors {

    private SpawnBehaviors() {
    }

    public static SpawnBehavior points(ServiceRegistry registry, List<SpawnPoint> points, int active, int goal, int max,
                                       SpawnCompletion completion, AfterGoal afterGoal, RespawnPolicy respawn,
                                       Collection<SpawnTemplate> templates) {
        return points(registry, points, active, goal, max, completion, afterGoal, respawn, SpawnOptions.DEFAULT, templates);
    }

    public static SpawnBehavior points(ServiceRegistry registry, List<SpawnPoint> points, int active, int goal, int max,
                                       SpawnCompletion completion, AfterGoal afterGoal, RespawnPolicy respawn,
                                       SpawnOptions options, Collection<SpawnTemplate> templates) {
        return new SpawnBehavior(registry, SpawnBehavior.Placement.POINTS, points, AreaOptions.DEFAULT, active, goal, max,
                completion, afterGoal, respawn, options, templates);
    }

    public static SpawnBehavior area(ServiceRegistry registry, boolean surface, int maxTries, AreaOptions.BlockFilter filter,
                                     Collection<Material> blocks, int active, int goal, int max, SpawnCompletion completion,
                                     AfterGoal afterGoal, RespawnPolicy respawn, SpawnOptions options,
                                     Collection<SpawnTemplate> templates) {
        return new SpawnBehavior(registry, SpawnBehavior.Placement.AREA, List.of(),
                new AreaOptions(surface, maxTries, filter, blocks), active, goal, max, completion, afterGoal, respawn,
                options, templates);
    }

    public static SpawnBehavior area(ServiceRegistry registry) {
        return area(registry, true, 20, AreaOptions.BlockFilter.BLACKLIST, List.of(), 1, 1, -1, SpawnCompletion.PER_PLAYER,
                AfterGoal.DENY, RespawnPolicy.DEFAULT, SpawnOptions.DEFAULT, List.of());
    }
}
