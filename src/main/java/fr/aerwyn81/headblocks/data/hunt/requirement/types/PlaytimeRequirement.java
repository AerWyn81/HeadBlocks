package fr.aerwyn81.headblocks.data.hunt.requirement.types;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.requirement.Requirement;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementResult;
import fr.aerwyn81.headblocks.data.hunt.requirement.RequirementType;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * Have played long enough on the server to claim a head.
 */
public class PlaytimeRequirement implements Requirement {
    private static final int TICKS_PER_MINUTE = 20 * 60;

    private static final Statistic PLAY_TIME = resolvePlayTimeStatistic();

    private final ServiceRegistry registry;
    private final int minutes;

    public PlaytimeRequirement(ServiceRegistry registry, int minutes) {
        this.registry = registry;
        this.minutes = Math.max(0, minutes);
    }

    public int minutes() {
        return minutes;
    }

    @Override
    public RequirementType getType() {
        return RequirementType.PLAYTIME;
    }

    @Override
    public RequirementResult check(Player player, HeadLocation head, HBHunt hunt) {
        if (PLAY_TIME == null) {
            return RequirementResult.ok();
        }

        int playedMinutes = player.getStatistic(PLAY_TIME) / TICKS_PER_MINUTE;
        if (playedMinutes >= minutes) {
            return RequirementResult.ok();
        }

        return RequirementResult.unmet(registry.getLanguageService().message("Hunt.Requirement.PlaytimeUnmet")
                .replace("%required%", format(minutes))
                .replace("%played%", format(playedMinutes))
                .replace("%missing%", format(minutes - playedMinutes)));
    }

    @Override
    public String describe() {
        return registry.getLanguageService().message("Hunt.Requirement.PlaytimeDescription")
                .replace("%required%", format(minutes));
    }

    @Override
    public boolean isComplete() {
        return minutes > 0;
    }

    @Override
    public void saveTo(ConfigurationSection section) {
        section.set("minutes", minutes);
    }

    public static String format(int totalMinutes) {
        int safe = Math.max(0, totalMinutes);
        int hours = safe / 60;
        int remaining = safe % 60;

        if (hours == 0) {
            return remaining + "min";
        }
        if (remaining == 0) {
            return hours + "h";
        }
        return hours + "h" + String.format("%02d", remaining);
    }

    public static PlaytimeRequirement fromConfig(ServiceRegistry registry, ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        return new PlaytimeRequirement(registry, section.getInt("minutes"));
    }

    private static Statistic resolvePlayTimeStatistic() {
        for (String name : new String[]{"PLAY_ONE_MINUTE", "PLAY_TIME", "PLAY_ONE_TICK"}) {
            try {
                return Statistic.valueOf(name);
            } catch (Exception ignored) {
            }
        }

        LogUtil.warning("No play time statistic found on this server: playtime requirements are ignored.");
        return null;
    }
}
