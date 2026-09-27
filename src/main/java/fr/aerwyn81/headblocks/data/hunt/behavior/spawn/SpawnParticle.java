package fr.aerwyn81.headblocks.data.hunt.behavior.spawn;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public record SpawnParticle(String name, int amount, List<String> colors) {

    public SpawnParticle {
        amount = Math.max(1, amount);
        colors = List.copyOf(colors);
    }

    public ArrayList<String> colorList() {
        return new ArrayList<>(colors);
    }

    public void saveTo(ConfigurationSection section) {
        section.set("name", name);
        section.set("amount", amount);
        if (!colors.isEmpty()) {
            section.set("colors", colors);
        }
    }

    public static SpawnParticle fromConfig(ConfigurationSection section) {
        if (section == null || section.getString("name", "").isBlank()) {
            return null;
        }
        return new SpawnParticle(section.getString("name"), section.getInt("amount", 3), section.getStringList("colors"));
    }
}
