package fr.aerwyn81.headblocks.data.hunt.behavior.spawn;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.data.hunt.behavior.SpawnPointsBehavior;
import fr.aerwyn81.headblocks.data.reward.Reward;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SpawnDraft {
    public List<SpawnPoint> points = new ArrayList<>();
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
    public final Map<String, SpawnTemplate> templates = new LinkedHashMap<>();

    public static SpawnDraft of(SpawnPointsBehavior behavior) {
        var draft = new SpawnDraft();
        draft.points = new ArrayList<>(behavior.points());
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

    public void setWeight(String id, int weight) {
        var template = templates.get(id);
        if (template != null) {
            templates.put(id, new SpawnTemplate(id, template.name(), Math.max(0, weight), template.content(), template.rewards()));
        }
    }

    public void setRewards(String id, List<Reward> rewards) {
        var template = templates.get(id);
        if (template != null) {
            templates.put(id, new SpawnTemplate(id, template.name(), template.weight(), template.content(), rewards));
        }
    }

    public boolean isValid() {
        return templates.values().stream().anyMatch(template -> template.weight() > 0);
    }

    public SpawnPointsBehavior build(ServiceRegistry registry) {
        return new SpawnPointsBehavior(registry, points, active, goal, maxTotalSpawns, completion, afterGoal,
                new RespawnPolicy(onFind, minDelay, maxDelay, interval, intervalSeconds, resetProgress, onStart),
                templates.values());
    }
}
