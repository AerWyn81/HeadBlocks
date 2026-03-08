package fr.aerwyn81.headblocks.data.hunt.behavior;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.services.TimedRunManager;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

public class TimedBehavior implements Behavior {

    private final ServiceRegistry registry;
    private final Location startPlateLocation;
    private final boolean repeatable;

    public TimedBehavior(Location startPlateLocation, boolean repeatable) {
        this.registry = null;
        this.startPlateLocation = startPlateLocation;
        this.repeatable = repeatable;
    }

    public TimedBehavior(ServiceRegistry registry, Location startPlateLocation, boolean repeatable) {
        this.registry = registry;
        this.startPlateLocation = startPlateLocation;
        this.repeatable = repeatable;
    }

    public Location startPlateLocation() {
        return startPlateLocation;
    }

    public boolean repeatable() {
        return repeatable;
    }

    @Override
    public String getId() {
        return "timed";
    }

    @Override
    public BehaviorResult canPlayerClick(Player player, HeadLocation head, HBHunt hunt) {
        if (!TimedRunManager.isInRun(player.getUniqueId(), hunt.getId())) {
            return BehaviorResult.deny(registry.getLanguageService().message("Messages.TimedNotStarted"));
        }
        return BehaviorResult.allow();
    }

    @Override
    public void onHeadFound(Player player, HeadLocation head, HBHunt hunt) {
        if (!TimedRunManager.isInRun(player.getUniqueId(), hunt.getId())) {
            return;
        }

        try {
            ArrayList<UUID> playerHuntHeads = registry.getStorageService().getHeadsPlayerForHunt(
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
                    registry.getStorageService().saveTimedRun(player.getUniqueId(), hunt.getId(), elapsed);
                } catch (InternalException e) {
                    LogUtil.error("Error saving timed run for player {0} in hunt {1}: {2}",
                            player.getName(), hunt.getId(), e.getMessage());
                }

                int completionCount = 0;
                try {
                    completionCount = registry.getStorageService().getTimedRunCount(player.getUniqueId(), hunt.getId());
                } catch (InternalException e) {
                    LogUtil.error("Error getting timed run count for player {0} in hunt {1}: {2}",
                            player.getName(), hunt.getId(), e.getMessage());
                }

                player.sendMessage(registry.getLanguageService().message("Messages.TimedCompleted")
                        .replace("%time%", TimedRunManager.formatTime(elapsed))
                        .replace("%hunt%", hunt.getDisplayName())
                        .replace("%count%", String.valueOf(completionCount)));

                if (repeatable) {
                    try {
                        registry.getStorageService().resetPlayerHunt(player.getUniqueId(), hunt.getId());
                    } catch (InternalException e) {
                        LogUtil.error("Error resetting player hunt for repeatable timed run: {0}", e.getMessage());
                    }
                }
            }
        } catch (InternalException e) {
            LogUtil.error("Error checking timed completion for player {0} in hunt {1}: {2}",
                    player.getName(), hunt.getId(), e.getMessage());
        }
    }

    @Override
    public String getDisplayInfo(Player player, HBHunt hunt) {
        return registry.getLanguageService().message("Hunt.Behavior.Timed");
    }

    public static TimedBehavior fromConfig(ServiceRegistry registry, ConfigurationSection section) {
        Location loc = null;
        boolean repeatable = true;

        if (section != null) {
            if (section.contains("startPlate.world")) {
                String worldName = section.getString("startPlate.world", "");
                double x = section.getDouble("startPlate.x");
                double y = section.getDouble("startPlate.y");
                double z = section.getDouble("startPlate.z");

                var world = Bukkit.getWorld(worldName);
                if (world != null) {
                    loc = new Location(world, x, y, z);
                }
            }

            repeatable = section.getBoolean("repeatable", true);
        }

        return new TimedBehavior(registry, loc, repeatable);
    }
}
