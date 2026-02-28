package fr.aerwyn81.headblocks.data.hunt.behavior;

import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

@SuppressWarnings("unused")
public interface Behavior {

    String getId();

    BehaviorResult canPlayerClick(Player player, HeadLocation head, Hunt hunt);

    void onHeadFound(Player player, HeadLocation head, Hunt hunt);

    String getDisplayInfo(Player player, Hunt hunt);

    static Behavior fromConfig(String type, ConfigurationSection section) {
        return switch (type.toLowerCase()) {
            case "ordered" -> OrderedBehavior.fromConfig(section);
            case "scheduled" -> ScheduledBehavior.fromConfig(section);
            default -> new FreeBehavior();
        };
    }
}
