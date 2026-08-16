package fr.aerwyn81.headblocks.data.hunt.behavior;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

@SuppressWarnings("unused")
public interface Behavior {

    String getId();

    BehaviorResult canPlayerClick(Player player, HeadLocation head, HBHunt hunt);

    void onHeadFound(Player player, HeadLocation head, HBHunt hunt);

    String getDisplayInfo(Player player, HBHunt hunt);

    default boolean isAccessGate() {
        return false;
    }

    static Behavior fromConfig(String type, ServiceRegistry registry, ConfigurationSection section) {
        return switch (type.toLowerCase()) {
            case "ordered" -> OrderedBehavior.fromConfig(registry, section);
            case "scheduled" -> ScheduledBehavior.fromConfig(registry, section);
            case "timed" -> TimedBehavior.fromConfig(registry, section);
            default -> new FreeBehavior();
        };
    }
}
