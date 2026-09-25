package fr.aerwyn81.headblocks.utils.bukkit;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import org.bukkit.entity.Player;

public final class HeadTargeting {

    private HeadTargeting() {
    }

    public static HeadLocation lookedAt(Player player, ServiceRegistry registry, int distance) {
        var entityHead = lookedAtEntity(player, registry, distance);
        if (entityHead != null) {
            return entityHead;
        }

        var targetBlock = player.getTargetBlock(null, distance);
        if (targetBlock.isEmpty()) {
            return null;
        }

        return registry.getHeadService().getHeadAt(targetBlock.getLocation());
    }

    public static HeadLocation lookedAtEntity(Player player, ServiceRegistry registry, int distance) {
        return registry.getVisualService().lookedAtHead(player, distance);
    }
}
