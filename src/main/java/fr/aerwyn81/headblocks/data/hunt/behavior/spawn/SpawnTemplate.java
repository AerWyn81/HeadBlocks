package fr.aerwyn81.headblocks.data.hunt.behavior.spawn;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.data.reward.Reward;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public record SpawnTemplate(String id, String name, int weight, HeadContent content, List<Reward> rewards) {

    public SpawnTemplate {
        rewards = List.copyOf(rewards);
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

        return new SpawnTemplate(id, section.getString("name", ""), Math.max(0, section.getInt("weight", 1)), content, rewards);
    }
}
