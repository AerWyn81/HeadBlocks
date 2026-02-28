package fr.aerwyn81.headblocks.data.hunt.behavior;

import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import org.bukkit.entity.Player;

public class FreeBehavior implements Behavior {

    @Override
    public String getId() {
        return "free";
    }

    @Override
    public BehaviorResult canPlayerClick(Player player, HeadLocation head, Hunt hunt) {
        return BehaviorResult.allow();
    }

    @Override
    public void onHeadFound(Player player, HeadLocation head, Hunt hunt) {
        // No-op for free behavior
    }

    @Override
    public String getDisplayInfo(Player player, Hunt hunt) {
        return "";
    }
}
