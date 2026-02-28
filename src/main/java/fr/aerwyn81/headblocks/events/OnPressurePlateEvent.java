package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.data.TimedRunData;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.data.hunt.behavior.Behavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.TimedBehavior;
import fr.aerwyn81.headblocks.services.HuntService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.services.TimedRunManager;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.UUID;

public class OnPressurePlateEvent implements Listener {

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

        for (Hunt hunt : HuntService.getAllHunts()) {
            if (!hunt.isActive()) {
                continue;
            }

            for (Behavior behavior : hunt.getBehaviors()) {
                if (!(behavior instanceof TimedBehavior tb)) {
                    continue;
                }

                Location startPlate = tb.getStartPlateLocation();
                if (startPlate == null) {
                    continue;
                }

                if (startPlate.getWorld() == null || blockLoc.getWorld() == null) {
                    continue;
                }

                if (!startPlate.getWorld().equals(blockLoc.getWorld())) {
                    continue;
                }

                if (startPlate.getBlockX() != blockLoc.getBlockX()
                        || startPlate.getBlockY() != blockLoc.getBlockY()
                        || startPlate.getBlockZ() != blockLoc.getBlockZ()) {
                    continue;
                }

                // Found matching start plate for this hunt
                handleStartPlate(player, hunt);
                return;
            }
        }
    }

    private void handleStartPlate(Player player, Hunt hunt) {
        UUID pUuid = player.getUniqueId();

        // Check if player already completed all heads for this hunt
        try {
            ArrayList<UUID> foundHeads = StorageService.getHeadsPlayerForHunt(pUuid, hunt.getId());
            if (foundHeads.size() >= hunt.getHeadCount() && hunt.getHeadCount() > 0) {
                player.sendMessage(LanguageService.getMessage("Messages.TimedAlreadyCompleted"));
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
                StorageService.resetPlayerHunt(pUuid, hunt.getId());
            } catch (InternalException e) {
                LogUtil.error("Error resetting player hunt progression for timed restart: {0}", e.getMessage());
            }
        }

        // Start/restart the run
        TimedRunManager.startRun(pUuid, hunt.getId());

        if (isRestart) {
            player.sendMessage(LanguageService.getMessage("Messages.TimedRestarted")
                    .replaceAll("%hunt%", hunt.getDisplayName()));
        } else {
            player.sendMessage(LanguageService.getMessage("Messages.TimedStarted")
                    .replaceAll("%hunt%", hunt.getDisplayName()));
        }
    }
}
