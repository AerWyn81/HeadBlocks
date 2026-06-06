package fr.aerwyn81.headblocks.runnables;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.TimedRunData;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.behavior.Behavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.TimedBehavior;
import fr.aerwyn81.headblocks.services.TimedRunManager;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;

public class TimedRunTask extends BukkitRunnable {

    private final ServiceRegistry registry;

    public TimedRunTask(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void run() {
        for (Map.Entry<UUID, TimedRunData> entry : TimedRunManager.getActiveRuns().entrySet()) {
            UUID playerUuid = entry.getKey();
            TimedRunData data = entry.getValue();

            long elapsed = System.currentTimeMillis() - data.startTimeMillis();
            HBHunt hunt = registry.getHuntService().getHuntById(data.huntId());
            TimedBehavior behavior = findTimedBehavior(hunt);
            int limitSeconds = behavior != null ? behavior.limitSeconds() : 0;

            // Time limit reached: leave the run, optionally reset progression, teleport back
            if (limitSeconds > 0 && TimedRunManager.getRemainingMillis(elapsed, limitSeconds) <= 0) {
                handleExpiration(playerUuid, data, hunt, behavior);
                continue;
            }

            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null || !player.isOnline()) {
                continue;
            }

            String huntName = hunt != null ? hunt.getDisplayName() : data.huntId();
            int totalHeads = hunt != null ? hunt.getHeadCount() : 0;

            int foundHeads = 0;
            try {
                foundHeads = registry.getStorageService().getHeadsPlayerForHunt(playerUuid, data.huntId()).size();
            } catch (InternalException ignored) {
            }

            String message;
            if (limitSeconds > 0) {
                long remaining = TimedRunManager.getRemainingMillis(elapsed, limitSeconds);
                message = registry.getLanguageService().message("Gui.TimedActionBarCountdown")
                        .replace("%remaining%", TimedRunManager.formatTime(Math.max(0, remaining)));
            } else {
                message = registry.getLanguageService().message("Gui.TimedActionBar")
                        .replace("%time%", TimedRunManager.formatTime(elapsed));
            }

            message = message
                    .replace("%hunt%", huntName)
                    .replace("%found%", String.valueOf(foundHeads))
                    .replace("%total%", String.valueOf(totalHeads));

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
        }
    }

    private TimedBehavior findTimedBehavior(HBHunt hunt) {
        if (hunt == null) {
            return null;
        }

        for (Behavior behavior : hunt.getBehaviors()) {
            if (behavior instanceof TimedBehavior tb) {
                return tb;
            }
        }
        return null;
    }

    private void handleExpiration(UUID playerUuid, TimedRunData data, HBHunt hunt, TimedBehavior behavior) {
        TimedRunManager.leaveRun(playerUuid);

        if (behavior != null && behavior.resetOnExpire()) {
            try {
                registry.getStorageService().resetPlayerHunt(playerUuid, data.huntId());
            } catch (InternalException e) {
                LogUtil.error("Error resetting player hunt on timed expiration for {0} in hunt {1}: {2}",
                        playerUuid, data.huntId(), e.getMessage());
            }
        }

        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null || !player.isOnline()) {
            return;
        }

        if (behavior != null && behavior.startPlateLocation() != null
                && behavior.startPlateLocation().getWorld() != null) {
            player.teleport(TimedRunManager.buildReturnLocation(behavior.startPlateLocation(), data.startYaw()));
        }

        String huntName = hunt != null ? hunt.getDisplayName() : data.huntId();
        player.sendMessage(registry.getLanguageService().message("Messages.TimedExpired")
                .replace("%hunt%", huntName));
    }
}
