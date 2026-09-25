package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.behavior.Behavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.TimedBehavior;
import fr.aerwyn81.headblocks.services.HeadRemovalService;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class OnPlayerBreakBlockEvent implements Listener {

    private final ServiceRegistry registry;
    private final HeadRemovalService removalService;

    public OnPlayerBreakBlockEvent(ServiceRegistry registry) {
        this.registry = registry;
        this.removalService = new HeadRemovalService(registry);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onStartPlateBreak(BlockBreakEvent e) {
        if (!isTimedStartPlate(e.getBlock().getLocation())) {
            return;
        }

        e.setCancelled(true);
        var message = registry.getLanguageService().message("Messages.TimedPlateProtected");
        if (!message.trim().isEmpty()) {
            e.getPlayer().sendMessage(message);
        }
    }

    private boolean isTimedStartPlate(Location location) {
        for (HBHunt hunt : registry.getHuntService().getAllHunts()) {
            for (Behavior behavior : hunt.getBehaviors()) {
                if (!(behavior instanceof TimedBehavior tb)) {
                    continue;
                }

                Location plate = tb.startPlateLocation();
                if (plate == null || plate.getWorld() == null || location.getWorld() == null) {
                    continue;
                }

                if (plate.getWorld().equals(location.getWorld())
                        && plate.getBlockX() == location.getBlockX()
                        && plate.getBlockY() == location.getBlockY()
                        && plate.getBlockZ() == location.getBlockZ()) {
                    return true;
                }
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void OnBlockBreakEvent(BlockBreakEvent e) {
        var player = e.getPlayer();
        var block = e.getBlock();

        Location blockLocation = block.getLocation();

        HeadLocation headLocation = registry.getHeadService().getBlockHeadAt(blockLocation);
        if (headLocation == null) {
            return;
        }

        if (!removalService.canRemove(player)) {
            e.setCancelled(true);
            return;
        }

        e.setCancelled(false);

        if (!removalService.remove(player, headLocation, blockLocation)) {
            e.setCancelled(true);
        }
    }
}
