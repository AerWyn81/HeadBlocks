package fr.aerwyn81.headblocks.data.hunt.behavior;

import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import org.bukkit.entity.Player;

public class FreeBehavior implements Behavior {

    @Override
    public String getId() {
        return "free";
    }

    @Override
    public BehaviorResult canPlayerClick(Player player, HeadLocation head, HBHunt hunt) {
        return BehaviorResult.allow();
    }

    @Override
    public void onHeadFound(Player player, HeadLocation head, HBHunt hunt) {
        // No-op for free behavior
    }

    @Override
    public String getDisplayInfo(Player player, HBHunt hunt) {
        return "";
    }
}
