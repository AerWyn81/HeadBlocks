package fr.aerwyn81.headblocks.data.hunt.behavior.spawn;

import org.bukkit.configuration.ConfigurationSection;

import java.util.concurrent.ThreadLocalRandom;

public record RespawnPolicy(boolean onFind, int minDelay, int maxDelay,
                            boolean interval, int intervalSeconds, boolean resetProgress,
                            boolean onStart) {

    public static final RespawnPolicy DEFAULT = new RespawnPolicy(true, 0, 0, false, 3600, false, true);

    public RespawnPolicy {
        minDelay = Math.max(0, minDelay);
        maxDelay = Math.max(minDelay, maxDelay);
        intervalSeconds = Math.max(1, intervalSeconds);
    }

    public int nextFindDelay() {
        return minDelay == maxDelay ? minDelay : ThreadLocalRandom.current().nextInt(minDelay, maxDelay + 1);
    }

    public void saveTo(ConfigurationSection section) {
        section.set("onFind.enabled", onFind);
        section.set("onFind.delay.min", minDelay);
        section.set("onFind.delay.max", maxDelay);
        section.set("interval.enabled", interval);
        section.set("interval.seconds", intervalSeconds);
        section.set("interval.resetProgress", resetProgress);
        section.set("onStart", onStart);
    }

    public static RespawnPolicy fromConfig(ConfigurationSection section) {
        if (section == null) {
            return DEFAULT;
        }

        return new RespawnPolicy(
                section.getBoolean("onFind.enabled", DEFAULT.onFind()),
                section.getInt("onFind.delay.min", DEFAULT.minDelay()),
                section.getInt("onFind.delay.max", DEFAULT.maxDelay()),
                section.getBoolean("interval.enabled", DEFAULT.interval()),
                section.getInt("interval.seconds", DEFAULT.intervalSeconds()),
                section.getBoolean("interval.resetProgress", DEFAULT.resetProgress()),
                section.getBoolean("onStart", DEFAULT.onStart()));
    }
}
