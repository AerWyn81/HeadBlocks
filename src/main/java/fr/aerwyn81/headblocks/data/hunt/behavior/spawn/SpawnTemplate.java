package fr.aerwyn81.headblocks.data.hunt.behavior.spawn;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.data.reward.Reward;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public record SpawnTemplate(String id, String name, int weight, HeadContent content, List<Reward> rewards,
                            boolean randomReward, double rewardChance, double points,
                            double trapChance, List<String> trapCommands, SpawnParticle particle) {

    public SpawnTemplate {
        weight = Math.max(0, weight);
        rewards = List.copyOf(rewards);
        rewardChance = clampPercent(rewardChance);
        trapChance = clampPercent(trapChance);
        trapCommands = List.copyOf(trapCommands);
    }

    public SpawnTemplate(String id, String name, int weight, HeadContent content, List<Reward> rewards) {
        this(id, name, weight, content, rewards, false, 100, 1, 0, List.of(), null);
    }

    public SpawnTemplate withWeight(int newWeight) {
        return new SpawnTemplate(id, name, newWeight, content, rewards, randomReward, rewardChance, points, trapChance, trapCommands, particle);
    }

    public SpawnTemplate withRewards(List<Reward> newRewards) {
        return new SpawnTemplate(id, name, weight, content, newRewards, randomReward, rewardChance, points, trapChance, trapCommands, particle);
    }

    public SpawnTemplate withRewardDraw(boolean newRandomReward, double newRewardChance) {
        return new SpawnTemplate(id, name, weight, content, rewards, newRandomReward, newRewardChance, points, trapChance, trapCommands, particle);
    }

    public SpawnTemplate withPoints(double newPoints) {
        return new SpawnTemplate(id, name, weight, content, rewards, randomReward, rewardChance, newPoints, trapChance, trapCommands, particle);
    }

    public SpawnTemplate withTrap(double newTrapChance, List<String> newTrapCommands) {
        return new SpawnTemplate(id, name, weight, content, rewards, randomReward, rewardChance, points, newTrapChance, newTrapCommands, particle);
    }

    public SpawnTemplate withParticle(SpawnParticle newParticle) {
        return new SpawnTemplate(id, name, weight, content, rewards, randomReward, rewardChance, points, trapChance, trapCommands, newParticle);
    }

    public List<Reward> drawRewards() {
        if (rewards.isEmpty() || !roll(rewardChance)) {
            return List.of();
        }
        if (randomReward) {
            return List.of(rewards.get(ThreadLocalRandom.current().nextInt(rewards.size())));
        }
        return rewards;
    }

    public boolean rollTrap() {
        return roll(trapChance);
    }

    public void saveTo(ConfigurationSection section) {
        if (!name.isEmpty()) {
            section.set("name", name);
        }
        section.set("weight", weight);
        content.save(section, "content");

        if (!rewards.isEmpty()) {
            section.set("rewards", rewards.stream().map(Reward::serialize).toList());
        }
        section.set("randomReward", randomReward);
        section.set("rewardChance", rewardChance);
        section.set("points", points);
        section.set("trap.chance", trapChance);
        section.set("trap.commands", trapCommands);
        if (particle != null) {
            particle.saveTo(section.createSection("particle"));
        }
    }

    public static SpawnTemplate fromConfig(String id, ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        var content = HeadContent.load(section.getConfigurationSection("content"));
        if (content == null) {
            return null;
        }

        var rewards = new ArrayList<Reward>();
        for (Object raw : section.getList("rewards", List.of())) {
            rewards.add(Reward.deserialize(raw));
        }

        return new SpawnTemplate(id, section.getString("name", ""), section.getInt("weight", 1), content, rewards,
                section.getBoolean("randomReward", false),
                section.getDouble("rewardChance", 100),
                section.getDouble("points", 1),
                section.getDouble("trap.chance", 0),
                section.getStringList("trap.commands"),
                SpawnParticle.fromConfig(section.getConfigurationSection("particle")));
    }

    private static boolean roll(double percent) {
        return percent >= 100 || (percent > 0 && ThreadLocalRandom.current().nextDouble(100) < percent);
    }

    private static double clampPercent(double value) {
        return Math.max(0, Math.min(100, value));
    }
}
