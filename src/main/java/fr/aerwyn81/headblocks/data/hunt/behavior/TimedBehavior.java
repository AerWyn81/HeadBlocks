package fr.aerwyn81.headblocks.data.hunt.behavior;

import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.services.TimedRunManager;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

public class TimedBehavior implements Behavior {

    private Location startPlateLocation;

    public TimedBehavior() {
    }

    public TimedBehavior(Location startPlateLocation) {
        this.startPlateLocation = startPlateLocation;
    }

    @Override
    public String getId() {
        return "timed";
    }

    @Override
    public BehaviorResult canPlayerClick(Player player, HeadLocation head, Hunt hunt) {
        if (!TimedRunManager.isInRun(player.getUniqueId(), hunt.getId())) {
            return BehaviorResult.deny(LanguageService.getMessage("Messages.TimedNotStarted"));
        }
        return BehaviorResult.allow();
    }

    @Override
    public void onHeadFound(Player player, HeadLocation head, Hunt hunt) {
        if (!TimedRunManager.isInRun(player.getUniqueId(), hunt.getId())) {
            return;
        }

        try {
            ArrayList<UUID> playerHuntHeads = StorageService.getHeadsPlayerForHunt(
                    player.getUniqueId(), hunt.getId());

            // +1 because the current head was just found but may not be persisted yet
            int foundCount = playerHuntHeads.size();
            if (!playerHuntHeads.contains(head.getUuid())) {
                foundCount++;
            }

            int totalHeads = hunt.getHeadCount();

            if (foundCount >= totalHeads) {
                long elapsed = TimedRunManager.getElapsedMillis(player.getUniqueId());
                TimedRunManager.leaveRun(player.getUniqueId());

                try {
                    StorageService.saveTimedRun(player.getUniqueId(), hunt.getId(), elapsed);
                } catch (InternalException e) {
                    LogUtil.error("Error saving timed run for player {0} in hunt {1}: {2}",
                            player.getName(), hunt.getId(), e.getMessage());
                }

                player.sendMessage(LanguageService.getMessage("Messages.TimedCompleted")
                        .replaceAll("%time%", TimedRunManager.formatTime(elapsed))
                        .replaceAll("%hunt%", hunt.getDisplayName()));
            }
        } catch (InternalException e) {
            LogUtil.error("Error checking timed completion for player {0} in hunt {1}: {2}",
                    player.getName(), hunt.getId(), e.getMessage());
        }
    }

    @Override
    public String getDisplayInfo(Player player, Hunt hunt) {
        return LanguageService.getMessage("Hunt.Behavior.Timed");
    }

    public Location getStartPlateLocation() {
        return startPlateLocation;
    }

    public void setStartPlateLocation(Location startPlateLocation) {
        this.startPlateLocation = startPlateLocation;
    }

    public static TimedBehavior fromConfig(ConfigurationSection section) {
        Location loc = null;

        if (section != null && section.contains("startPlate.world")) {
            String worldName = section.getString("startPlate.world");
            double x = section.getDouble("startPlate.x");
            double y = section.getDouble("startPlate.y");
            double z = section.getDouble("startPlate.z");

            var world = org.bukkit.Bukkit.getWorld(worldName);
            if (world != null) {
                loc = new Location(world, x, y, z);
            }
        }

        return new TimedBehavior(loc);
    }
}
