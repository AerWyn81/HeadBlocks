package fr.aerwyn81.headblocks.data.hunt.behavior.spawn;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.data.hunt.behavior.RandomSpawnBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.SpawnBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.SpawnPointsBehavior;
import fr.aerwyn81.headblocks.data.reward.Reward;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public class SpawnDraft {
    public boolean random = false;
    public List<SpawnPoint> points = new ArrayList<>();
    public boolean surface = true;
    public int maxTries = 20;
    public RandomSpawnBehavior.BlockFilter filter = RandomSpawnBehavior.BlockFilter.BLACKLIST;
    public List<Material> blocks = new ArrayList<>();
    public int active = 3;
    public int goal = 10;
    public int maxTotalSpawns = -1;
    public SpawnCompletion completion = SpawnCompletion.PER_PLAYER;
    public AfterGoal afterGoal = AfterGoal.DENY;
    public boolean onFind = true;
    public int minDelay = 0;
    public int maxDelay = 0;
    public boolean interval = false;
    public int intervalSeconds = 3600;
    public boolean resetProgress = false;
    public boolean onStart = true;
    public boolean announce = false;
    public boolean log = false;
    public boolean debug = false;
    public SpawnOptions.Scoring scoring = SpawnOptions.Scoring.HEADS;
    public boolean resetOnActivate = false;
    public final Map<String, SpawnTemplate> templates = new LinkedHashMap<>();

    public static SpawnDraft random() {
        var draft = new SpawnDraft();
        draft.random = true;
        return draft;
    }

    public static SpawnDraft of(SpawnBehavior behavior) {
        var draft = new SpawnDraft();
        if (behavior instanceof SpawnPointsBehavior points) {
            draft.points = new ArrayList<>(points.points());
        } else if (behavior instanceof RandomSpawnBehavior random) {
            draft.random = true;
            draft.surface = random.surface();
            draft.maxTries = random.maxTries();
            draft.filter = random.filter();
            draft.blocks = new ArrayList<>(random.blocks());
        }
        draft.active = behavior.active();
        draft.goal = behavior.goal();
        draft.maxTotalSpawns = behavior.maxTotalSpawns();
        draft.completion = behavior.completion();
        draft.afterGoal = behavior.afterGoal();

        var respawn = behavior.respawn();
        draft.onFind = respawn.onFind();
        draft.minDelay = respawn.minDelay();
        draft.maxDelay = respawn.maxDelay();
        draft.interval = respawn.interval();
        draft.intervalSeconds = respawn.intervalSeconds();
        draft.resetProgress = respawn.resetProgress();
        draft.onStart = respawn.onStart();

        var options = behavior.options();
        draft.announce = options.announce();
        draft.log = options.log();
        draft.debug = options.debug();
        draft.scoring = options.scoring();
        draft.resetOnActivate = options.resetOnActivate();

        behavior.templates().forEach(template -> draft.templates.put(template.id(), template));
        return draft;
    }

    public void addTemplate(HeadContent content) {
        int index = templates.size() + 1;
        while (templates.containsKey("template" + index)) {
            index++;
        }

        var id = "template" + index;
        templates.put(id, new SpawnTemplate(id, "", 1, content, List.of()));
    }

    public void update(String id, UnaryOperator<SpawnTemplate> change) {
        templates.computeIfPresent(id, (key, template) -> change.apply(template));
    }

    public void setWeight(String id, int weight) {
        update(id, template -> template.withWeight(weight));
    }

    public void setRewards(String id, List<Reward> rewards) {
        update(id, template -> template.withRewards(rewards));
    }

    public boolean isValid() {
        return templates.values().stream().anyMatch(template -> template.weight() > 0);
    }

    public RespawnPolicy respawnPolicy() {
        return new RespawnPolicy(onFind, minDelay, maxDelay, interval, intervalSeconds, resetProgress, onStart);
    }

    public SpawnOptions options() {
        return new SpawnOptions(announce, log, debug, scoring, resetOnActivate);
    }

    public SpawnBehavior build(ServiceRegistry registry) {
        if (random) {
            return new RandomSpawnBehavior(registry, surface, maxTries, filter, blocks, active, goal, maxTotalSpawns,
                    completion, afterGoal, respawnPolicy(), options(), templates.values());
        }
        return new SpawnPointsBehavior(registry, points, active, goal, maxTotalSpawns, completion, afterGoal,
                respawnPolicy(), options(), templates.values());
    }
}
