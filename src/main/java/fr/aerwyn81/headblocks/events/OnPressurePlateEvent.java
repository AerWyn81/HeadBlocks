package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.TimedRunData;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.behavior.Behavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.TimedBehavior;
import fr.aerwyn81.headblocks.services.TimedRunManager;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class OnPressurePlateEvent implements Listener {
    private static final String HUNT_PLACEHOLDER = "%hunt%";

    private final ServiceRegistry registry;

    public OnPressurePlateEvent(ServiceRegistry registry) {
        this.registry = registry;
    }

    @EventHandler
    public void onPressurePlate(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) {
            return;
        }

        if (event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        Location blockLoc = event.getClickedBlock().getLocation();

        for (HBHunt hunt : registry.getHuntService().getAllHunts()) {
            if (hunt.isActive() && hasStartPlateAt(hunt, blockLoc)) {
                handleStartPlate(player, hunt);
                return;
            }
        }
    }

    private static boolean hasStartPlateAt(HBHunt hunt, Location blockLoc) {
        for (Behavior behavior : hunt.getBehaviors()) {
            if (behavior instanceof TimedBehavior tb && isSameBlock(tb.startPlateLocation(), blockLoc)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSameBlock(Location startPlate, Location blockLoc) {
        return startPlate != null && startPlate.getWorld() != null && blockLoc.getWorld() != null
                && startPlate.getWorld().equals(blockLoc.getWorld())
                && startPlate.getBlockX() == blockLoc.getBlockX()
                && startPlate.getBlockY() == blockLoc.getBlockY()
                && startPlate.getBlockZ() == blockLoc.getBlockZ();
    }

    private void handleStartPlate(Player player, HBHunt hunt) {
        UUID pUuid = player.getUniqueId();

        if (!hunt.isValid()) {
            player.sendMessage(registry.getLanguageService().message("Messages.TimedNoHeads")
                    .replace(HUNT_PLACEHOLDER, hunt.getDisplayName()));
            return;
        }

        // Check if player already completed all heads for this hunt
        try {
            var foundHeads = registry.getStorageService().getHeadsPlayerForHunt(pUuid, hunt.getId());
            if (foundHeads.size() >= hunt.getTargetCount() && hunt.getTargetCount() > 0) {
                player.sendMessage(registry.getLanguageService().message("Messages.TimedAlreadyCompleted"));
                return;
            }
        } catch (InternalException e) {
            LogUtil.error("Error checking hunt progress for timed start: {0}", e.getMessage());
        }

        // If player is already in a run for a different hunt, leave it
        TimedRunData existingRun = TimedRunManager.getRun(pUuid);
        if (existingRun != null && !existingRun.huntId().equals(hunt.getId())) {
            TimedRunManager.leaveRun(pUuid);
        }

        boolean isRestart = TimedRunManager.isInRun(pUuid, hunt.getId());

        // Reset player progression for this hunt if restarting
        if (isRestart) {
            try {
                registry.getStorageService().resetPlayerHunt(pUuid, hunt.getId());
            } catch (InternalException e) {
                LogUtil.error("Error resetting player hunt progression for timed restart: {0}", e.getMessage());
            }
        }

        // Start/restart the run, capturing the player's facing (yaw) so we can face them back
        // toward the course if the time limit expires
        TimedRunManager.startRun(pUuid, hunt.getId(), player.getLocation().getYaw());

        if (isRestart) {
            player.sendMessage(registry.getLanguageService().message("Messages.TimedRestarted")
                    .replace(HUNT_PLACEHOLDER, hunt.getDisplayName()));
        } else {
            player.sendMessage(registry.getLanguageService().message("Messages.TimedStarted")
                    .replace(HUNT_PLACEHOLDER, hunt.getDisplayName()));
        }
    }
}
