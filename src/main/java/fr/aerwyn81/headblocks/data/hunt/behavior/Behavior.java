package fr.aerwyn81.headblocks.data.hunt.behavior;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.OptionalInt;

@SuppressWarnings("unused")
public interface Behavior {

    String getId();

    BehaviorResult canPlayerClick(Player player, HeadLocation head, HBHunt hunt);

    void onHeadFound(Player player, HeadLocation head, HBHunt hunt);

    String getDisplayInfo(Player player, HBHunt hunt);

    default boolean isAccessGate() {
        return false;
    }

    default OptionalInt targetCount() {
        return OptionalInt.empty();
    }

    default BehaviorResult tryCommit(Player player, HeadLocation head, HBHunt hunt) {
        return BehaviorResult.allow();
    }

    default void saveTo(ConfigurationSection section) {
    }

    static Behavior fromConfig(String type, ServiceRegistry registry, ConfigurationSection section) {
        return switch (type.toLowerCase()) {
            case "ordered" -> OrderedBehavior.fromConfig(registry, section);
            case "scheduled" -> ScheduledBehavior.fromConfig(registry, section);
            case "timed" -> TimedBehavior.fromConfig(registry, section);
            case FixedPositionBehavior.ID -> FixedPositionBehavior.fromConfig(registry, section);
            default -> new FreeBehavior();
        };
    }
}
