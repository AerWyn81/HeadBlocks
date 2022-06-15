package fr.aerwyn81.headblocks.data.reward;

import org.bukkit.configuration.ConfigurationSection;

public class Reward {
    private final RewardType type;
    private final String value;

    public Reward(RewardType type, String value) {
        this.type = type;
        this.value = value;
    }

    public void serialize(ConfigurationSection section) {
        section.set(type + ".value", value);
    }

    public static Reward deserialize(ConfigurationSection section) {
        RewardType type;

        try {
            type = RewardType.valueOf(section.getString("type"));
        } catch (Exception ex) {
            type = RewardType.UNKNOWN;
        }

        String value = section.getString("value");

        return new Reward(type, value);
    }
}
