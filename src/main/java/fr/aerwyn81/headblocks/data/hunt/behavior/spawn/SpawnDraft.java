package fr.aerwyn81.headblocks.data.hunt.behavior.spawn;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.data.hunt.behavior.SpawnBehavior;
import fr.aerwyn81.headblocks.data.reward.Reward;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public class SpawnDraft {
    public SpawnBehavior.Placement placement = SpawnBehavior.Placement.POINTS;
    public List<SpawnPoint> points = new ArrayList<>();
    public boolean surface = true;
    public int maxTries = 20;
    public AreaOptions.BlockFilter filter = AreaOptions.BlockFilter.BLACKLIST;
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

    public static SpawnDraft of(SpawnBehavior behavior) {
        var draft = new SpawnDraft();
        draft.placement = behavior.placement();
        draft.points = new ArrayList<>(behavior.points());
        var area = behavior.area();
        draft.surface = area.surface();
        draft.maxTries = area.maxTries();
        draft.filter = area.filter();
        draft.blocks = new ArrayList<>(area.blocks());
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
        return new SpawnBehavior(registry, placement, points, new AreaOptions(surface, maxTries, filter, blocks),
                active, goal, maxTotalSpawns, completion, afterGoal, respawnPolicy(), options(), templates.values());
    }
}
