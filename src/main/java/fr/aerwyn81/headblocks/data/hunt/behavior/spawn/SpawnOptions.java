package fr.aerwyn81.headblocks.data.hunt.behavior.spawn;

import org.bukkit.configuration.ConfigurationSection;

public record SpawnOptions(boolean announce, boolean log, boolean debug, Scoring scoring, boolean resetOnActivate) {

    public static final SpawnOptions DEFAULT = new SpawnOptions(false, false, false, Scoring.HEADS, false);

    public SpawnOptions(boolean announce, boolean log, boolean debug, Scoring scoring) {
        this(announce, log, debug, scoring, false);
    }

    public enum Scoring {
        HEADS,
        POINTS;

        public static Scoring of(String raw) {
            try {
                return raw == null ? HEADS : valueOf(raw.toUpperCase());
            } catch (IllegalArgumentException e) {
                return HEADS;
            }
        }
    }

    public void saveTo(ConfigurationSection section) {
        section.set("announce", announce);
        section.set("log", log);
        section.set("debug", debug);
        section.set("scoring", scoring.name());
        section.set("resetOnActivate", resetOnActivate);
    }

    public static SpawnOptions fromConfig(ConfigurationSection section) {
        if (section == null) {
            return DEFAULT;
        }
        return new SpawnOptions(section.getBoolean("announce", false), section.getBoolean("log", false),
                section.getBoolean("debug", false), Scoring.of(section.getString("scoring")),
                section.getBoolean("resetOnActivate", false));
    }
}
