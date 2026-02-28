package fr.aerwyn81.headblocks.data.hunt.behavior;

import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

public class OrderedBehavior implements Behavior {

    @Override
    public String getId() {
        return "ordered";
    }

    @Override
    public BehaviorResult canPlayerClick(Player player, HeadLocation head, Hunt hunt) {
        int headOrder = head.getOrderIndex();
        if (headOrder <= 0) {
            return BehaviorResult.allow();
        }

        try {
            ArrayList<UUID> playerHuntHeads = StorageService.getHeadsPlayerForHunt(
                    player.getUniqueId(), hunt.getId());

            // Check if there are heads in this hunt with a lower orderIndex that the player hasn't found
            boolean hasUnfoundPrior = HeadService.getChargedHeadLocations().stream()
                    .filter(h -> hunt.containsHead(h.getUuid()))
                    .filter(h -> !h.getUuid().equals(head.getUuid()))
                    .filter(h -> h.getOrderIndex() != -1 && h.getOrderIndex() < headOrder)
                    .anyMatch(h -> !playerHuntHeads.contains(h.getUuid()));

            if (hasUnfoundPrior) {
                return BehaviorResult.deny(LanguageService.getMessage("Messages.OrderClickError")
                        .replaceAll("%name%", head.getNameOrUnnamed()));
            }
        } catch (InternalException e) {
            LogUtil.error("Error checking ordered behavior for player {0} in hunt {1}: {2}",
                    player.getName(), hunt.getId(), e.getMessage());
        }

        return BehaviorResult.allow();
    }

    @Override
    public void onHeadFound(Player player, HeadLocation head, Hunt hunt) {
        // No-op — progression is handled by the click flow
    }

    @Override
    public String getDisplayInfo(Player player, Hunt hunt) {
        return LanguageService.getMessage("Hunt.Behavior.Ordered");
    }

    public static OrderedBehavior fromConfig(ConfigurationSection section) {
        return new OrderedBehavior();
    }
}
