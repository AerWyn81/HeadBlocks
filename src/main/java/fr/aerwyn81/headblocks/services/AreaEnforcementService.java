package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.TimedRunData;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.behavior.Behavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.TimedBehavior;
import fr.aerwyn81.headblocks.data.hunt.requirement.area.AreaMessageMode;
import fr.aerwyn81.headblocks.data.hunt.requirement.types.AreaRequirement;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Runtime side of the area: confinement, entry and exit messages, progress reset for whoever leaves.
 */
public class AreaEnforcementService {
    public enum Decision {
        NONE,
        CONFINE
    }

    private final ServiceRegistry registry;

    public AreaEnforcementService(ServiceRegistry registry) {
        this.registry = registry;
    }

    public Decision evaluate(Player player, Location to) {
        UUID uuid = player.getUniqueId();
        String engagedId = AreaRunManager.getEngaged(uuid);

        if (engagedId != null) {
            AreaRequirement area = findArea(engagedId);
            if (!isEnforceable(area)) {
                AreaRunManager.disengage(uuid);
                return Decision.NONE;
            }

            if (area.area().contains(to)) {
                return Decision.NONE;
            }

            if (area.blockExit()) {
                return Decision.CONFINE;
            }

            AreaRunManager.disengage(uuid);
            HBHunt engagedHunt = registry.getHuntService().getHuntById(engagedId);
            boolean reset = area.resetOnLeave();
            if (reset) {
                resetProgress(uuid, engagedId);
            }
            if (engagedHunt != null) {
                sendExited(player, engagedHunt, reset);
                teleportBackTimed(player, engagedHunt);
            }
            return Decision.NONE;
        }

        HBHunt best = null;
        AreaRequirement bestArea = null;
        for (HBHunt hunt : registry.getHuntService().getAllHunts()) {
            if (!hunt.isActive()) {
                continue;
            }

            AreaRequirement area = findArea(hunt);
            if (!isEnforceable(area) || !worldMatches(area, to)) {
                continue;
            }

            if (!area.area().contains(to)) {
                continue;
            }

            boolean higherPriority = best == null || hunt.getPriority() > best.getPriority();
            boolean blockingTieBreak = best != null && hunt.getPriority() == best.getPriority()
                    && area.blockExit() && !bestArea.blockExit();
            if (higherPriority || blockingTieBreak) {
                best = hunt;
                bestArea = area;
            }
        }

        if (best == null) {
            AreaRunManager.clearReleased(uuid);
            return Decision.NONE;
        }

        if (AreaRunManager.isReleased(uuid, best.getId())) {
            return Decision.NONE;
        }

        if (isCompleted(uuid, best.getId(), best)) {
            return Decision.NONE;
        }

        AreaRunManager.engage(uuid, best.getId());
        sendEntered(player, best);
        return Decision.NONE;
    }

    public Location getRecoveryPoint(Player player, Location reference) {
        UUID uuid = player.getUniqueId();
        String engagedId = AreaRunManager.getEngaged(uuid);
        if (engagedId == null) {
            return null;
        }

        AreaRequirement area = findArea(engagedId);
        if (!isEnforceable(area)) {
            AreaRunManager.disengage(uuid);
            return null;
        }

        if (area.area().contains(reference)) {
            return null;
        }

        return area.returnPoint();
    }

    public Location getReturnPoint(Player player) {
        String engagedId = AreaRunManager.getEngaged(player.getUniqueId());
        if (engagedId == null) {
            return null;
        }
        AreaRequirement area = findArea(engagedId);
        return area == null ? null : area.returnPoint();
    }

    public boolean leave(Player player) {
        UUID uuid = player.getUniqueId();
        String engagedId = AreaRunManager.getEngaged(uuid);
        if (engagedId == null) {
            return false;
        }

        AreaRequirement area = findArea(engagedId);
        HBHunt hunt = registry.getHuntService().getHuntById(engagedId);
        AreaRunManager.disengage(uuid);
        AreaRunManager.markReleased(uuid, engagedId);

        if (area != null && area.resetOnLeave()) {
            resetProgress(uuid, engagedId);
            sendResetMessage(player, hunt != null ? hunt.getDisplayName() : engagedId);
        }

        teleportBackTimed(player, hunt);
        return true;
    }

    public void onHeadFound(Player player, HBHunt hunt, int foundCount) {
        UUID uuid = player.getUniqueId();
        if (!hunt.getId().equals(AreaRunManager.getEngaged(uuid))) {
            return;
        }

        int total = hunt.getHeadCount();
        if (total > 0 && foundCount >= total) {
            AreaRunManager.disengage(uuid);
            AreaRunManager.markReleased(uuid, hunt.getId());
        }
    }

    public boolean isLocationOutsideArea(HBHunt hunt, Location location) {
        if (location == null) {
            return false;
        }
        AreaRequirement area = findArea(hunt);
        if (area == null || area.area() == null || !area.area().isAvailable()) {
            return false;
        }
        return !area.area().contains(location);
    }

    public boolean hasArea(HBHunt hunt) {
        return findArea(hunt) != null;
    }

    public void sanitizeAreaHunts() {
        for (HBHunt hunt : registry.getHuntService().getAllHunts()) {
            AreaRequirement area = hunt.getRequirements().findOrNull(AreaRequirement.class);
            if (area == null) {
                continue;
            }

            String reason = invalidAreaReason(hunt, area);
            area.setDisabled(reason != null);

            if (reason != null) {
                LogUtil.warning("Area requirement disabled for hunt {0}: {1}", hunt.getId(), reason);
            }
        }
    }

    private String invalidAreaReason(HBHunt hunt, AreaRequirement area) {
        if (area.area() == null) {
            return "no area defined";
        }
        if (area.blockExit() && area.returnPoint() == null) {
            return "no return point defined";
        }

        if (!area.area().isAvailable()) {
            return null;
        }

        List<HeadLocation> heads = registry.getHeadService().getHeadLocationsForHunt(hunt);
        if (heads.isEmpty()) {
            return "no heads assigned";
        }

        long outside = heads.stream()
                .filter(h -> h.getLocation() != null && !area.area().contains(h.getLocation()))
                .count();
        if (outside > 0) {
            return outside + " head(s) outside the area";
        }

        return null;
    }

    private boolean isEnforceable(AreaRequirement area) {
        return area != null && area.area() != null && area.area().isAvailable()
                && (area.returnPoint() != null || !area.blockExit());
    }

    private boolean worldMatches(AreaRequirement area, Location location) {
        return location != null && location.getWorld() != null
                && location.getWorld().getName().equals(area.area().getWorldName());
    }

    private AreaRequirement findArea(String huntId) {
        HBHunt hunt = registry.getHuntService().getHuntById(huntId);
        if (hunt == null || !hunt.isActive()) {
            return null;
        }
        return findArea(hunt);
    }

    private AreaRequirement findArea(HBHunt hunt) {
        AreaRequirement area = hunt.getRequirements().findOrNull(AreaRequirement.class);
        return area == null || area.isDisabled() ? null : area;
    }

    private void teleportBackTimed(Player player, HBHunt hunt) {
        UUID uuid = player.getUniqueId();
        if (hunt == null || !TimedRunManager.isInRun(uuid, hunt.getId())) {
            return;
        }

        TimedBehavior timed = findTimedBehavior(hunt);
        if (timed == null || timed.startPlateLocation() == null
                || timed.startPlateLocation().getWorld() == null) {
            return;
        }

        TimedRunData run = TimedRunManager.getRun(uuid);
        float yaw = run != null ? run.startYaw() : 0f;
        TimedRunManager.leaveRun(uuid);

        Location target = TimedRunManager.buildReturnLocation(timed.startPlateLocation(), yaw);
        registry.getScheduler().runTaskLater(player, () -> registry.getPlatform().teleportAsync(player, target), 1L);
    }

    private TimedBehavior findTimedBehavior(HBHunt hunt) {
        for (Behavior behavior : hunt.getBehaviors()) {
            if (behavior instanceof TimedBehavior tb) {
                return tb;
            }
        }
        return null;
    }

    private boolean isCompleted(UUID uuid, String huntId, HBHunt hunt) {
        int total = hunt.getHeadCount();
        if (total <= 0) {
            return false;
        }

        try {
            return registry.getStorageService().getHeadsPlayerForHunt(uuid, huntId).size() >= total;
        } catch (InternalException e) {
            LogUtil.error("Error checking area completion for hunt {0}: {1}", huntId, e.getMessage());
            return false;
        }
    }

    private void sendEntered(Player player, HBHunt hunt) {
        String message = registry.getLanguageService().message("Messages.AreaEntered")
                .replace("%hunt%", hunt.getDisplayName());
        if (message.trim().isEmpty()) {
            return;
        }

        AreaRequirement area = findArea(hunt);
        AreaMessageMode mode = area != null ? area.messageMode() : AreaMessageMode.CHAT;

        switch (mode) {
            case ACTION_BAR ->
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
            case TITLE -> {
                String normalized = message.replace("\\n", "\n");
                int newLine = normalized.indexOf('\n');
                String title = newLine >= 0 ? normalized.substring(0, newLine) : normalized;
                String subTitle = newLine >= 0 ? normalized.substring(newLine + 1) : "";
                player.sendTitle(title, subTitle, 10, 60, 10);
            }
            default -> player.sendMessage(message);
        }
    }

    private void sendExited(Player player, HBHunt hunt, boolean reset) {
        String message = registry.getLanguageService().message("Messages.AreaExited")
                .replace("%hunt%", hunt.getDisplayName());
        if (!message.trim().isEmpty()) {
            player.sendMessage(message);
        }

        if (reset) {
            sendResetMessage(player, hunt.getDisplayName());
        }
    }

    private void sendResetMessage(Player player, String huntName) {
        String message = registry.getLanguageService().message("Messages.AreaProgressReset")
                .replace("%hunt%", huntName);
        if (!message.trim().isEmpty()) {
            player.sendMessage(message);
        }
    }

    private void resetProgress(UUID uuid, String huntId) {
        try {
            registry.getStorageService().resetPlayerHunt(uuid, huntId);
        } catch (InternalException e) {
            LogUtil.error("Error resetting area progress for hunt {0}: {1}", huntId, e.getMessage());
        }
    }
}
