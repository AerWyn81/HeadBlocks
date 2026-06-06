package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.behavior.Behavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.ZoneBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.zone.ZoneMessageMode;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ZoneEnforcementService {

    public enum Decision {
        NONE,
        CONFINE
    }

    private final ServiceRegistry registry;

    public ZoneEnforcementService(ServiceRegistry registry) {
        this.registry = registry;
    }

    public Decision evaluate(Player player, Location to) {
        UUID uuid = player.getUniqueId();
        String engagedId = ZoneRunManager.getEngaged(uuid);

        if (engagedId != null) {
            ZoneBehavior zb = findZoneBehavior(engagedId);
            if (!isEnforceable(zb) || !zb.zone().isAvailable()) {
                ZoneRunManager.disengage(uuid);
                return Decision.NONE;
            }

            if (zb.zone().contains(to)) {
                return Decision.NONE;
            }

            if (zb.blockExit()) {
                return Decision.CONFINE;
            }

            ZoneRunManager.disengage(uuid);
            HBHunt engagedHunt = registry.getHuntService().getHuntById(engagedId);
            boolean reset = zb.resetOnLeave();
            if (reset) {
                resetProgress(uuid, engagedId);
            }
            if (engagedHunt != null) {
                sendExited(player, engagedHunt, reset);
            }
            return Decision.NONE;
        }

        HBHunt best = null;
        ZoneBehavior bestZone = null;
        for (HBHunt hunt : registry.getHuntService().getAllHunts()) {
            if (!hunt.isActive()) {
                continue;
            }

            ZoneBehavior zb = findZoneBehavior(hunt);
            if (!isEnforceable(zb) || !worldMatches(zb, to) || !zb.zone().isAvailable()) {
                continue;
            }

            if (!zb.zone().contains(to)) {
                continue;
            }

            boolean higherPriority = best == null || hunt.getPriority() > best.getPriority();
            boolean blockingTieBreak = best != null && hunt.getPriority() == best.getPriority()
                    && zb.blockExit() && !bestZone.blockExit();
            if (higherPriority || blockingTieBreak) {
                best = hunt;
                bestZone = zb;
            }
        }

        if (best == null) {
            ZoneRunManager.clearReleased(uuid);
            return Decision.NONE;
        }

        if (ZoneRunManager.isReleased(uuid, best.getId())) {
            return Decision.NONE;
        }

        if (isCompleted(uuid, best.getId(), best)) {
            return Decision.NONE;
        }

        ZoneRunManager.engage(uuid, best.getId());
        sendEntered(player, best);
        return Decision.NONE;
    }

    public Location getRecoveryPoint(Player player, Location reference) {
        UUID uuid = player.getUniqueId();
        String engagedId = ZoneRunManager.getEngaged(uuid);
        if (engagedId == null) {
            return null;
        }

        ZoneBehavior zb = findZoneBehavior(engagedId);
        if (!isEnforceable(zb) || !zb.zone().isAvailable()) {
            ZoneRunManager.disengage(uuid);
            return null;
        }

        if (zb.zone().contains(reference)) {
            return null;
        }

        return zb.returnPoint();
    }

    public Location getReturnPoint(Player player) {
        String engagedId = ZoneRunManager.getEngaged(player.getUniqueId());
        if (engagedId == null) {
            return null;
        }
        ZoneBehavior zb = findZoneBehavior(engagedId);
        return zb == null ? null : zb.returnPoint();
    }

    public boolean leave(Player player) {
        UUID uuid = player.getUniqueId();
        String engagedId = ZoneRunManager.getEngaged(uuid);
        if (engagedId == null) {
            return false;
        }

        ZoneBehavior zb = findZoneBehavior(engagedId);
        ZoneRunManager.disengage(uuid);
        ZoneRunManager.markReleased(uuid, engagedId);

        if (zb != null && zb.resetOnLeave()) {
            resetProgress(uuid, engagedId);
            String reset = registry.getLanguageService().message("Messages.ZoneProgressReset")
                    .replace("%hunt%", huntName(engagedId));
            if (!reset.trim().isEmpty()) {
                player.sendMessage(reset);
            }
        }
        return true;
    }

    public void onHeadFound(Player player, HBHunt hunt, int foundCount) {
        UUID uuid = player.getUniqueId();
        if (!hunt.getId().equals(ZoneRunManager.getEngaged(uuid))) {
            return;
        }

        int total = hunt.getHeadCount();
        if (total > 0 && foundCount >= total) {
            ZoneRunManager.disengage(uuid);
            ZoneRunManager.markReleased(uuid, hunt.getId());
        }
    }

    public boolean isLocationOutsideZone(HBHunt hunt, Location location) {
        if (location == null) {
            return false;
        }
        ZoneBehavior zb = findZoneBehavior(hunt);
        if (zb == null || zb.zone() == null || !zb.zone().isAvailable()) {
            return false;
        }
        return !zb.zone().contains(location);
    }

    public boolean hasZoneBehavior(HBHunt hunt) {
        return findZoneBehavior(hunt) != null;
    }

    public void sanitizeZoneHunts() {
        for (HBHunt hunt : registry.getHuntService().getAllHunts()) {
            ZoneBehavior zb = findZoneBehavior(hunt);
            if (zb == null) {
                continue;
            }

            String reason = invalidZoneReason(hunt, zb);
            if (reason == null) {
                continue;
            }

            LogUtil.warning("Zone behavior disabled for hunt {0}: {1}", hunt.getId(), reason);
            List<Behavior> kept = hunt.getBehaviors().stream()
                    .filter(b -> !(b instanceof ZoneBehavior))
                    .collect(Collectors.toList());
            hunt.setBehaviors(kept);
        }
    }

    private String invalidZoneReason(HBHunt hunt, ZoneBehavior zb) {
        if (zb.zone() == null) {
            return "no zone defined";
        }
        if (zb.blockExit() && zb.returnPoint() == null) {
            return "no return point defined";
        }

        if (!zb.zone().isAvailable()) {
            return null;
        }

        List<HeadLocation> heads = registry.getHeadService().getHeadLocationsForHunt(hunt);
        if (heads.isEmpty()) {
            return "no heads assigned";
        }

        long outside = heads.stream()
                .filter(h -> h.getLocation() != null && !zb.zone().contains(h.getLocation()))
                .count();
        if (outside > 0) {
            return outside + " head(s) outside the zone";
        }

        return null;
    }

    private boolean isEnforceable(ZoneBehavior zb) {
        return zb != null && zb.zone() != null && (zb.returnPoint() != null || !zb.blockExit());
    }

    private boolean worldMatches(ZoneBehavior zb, Location location) {
        return location != null && location.getWorld() != null
                && location.getWorld().getName().equals(zb.zone().getWorldName());
    }

    private ZoneBehavior findZoneBehavior(String huntId) {
        HBHunt hunt = registry.getHuntService().getHuntById(huntId);
        if (hunt == null || !hunt.isActive()) {
            return null;
        }
        return findZoneBehavior(hunt);
    }

    private ZoneBehavior findZoneBehavior(HBHunt hunt) {
        for (Behavior behavior : hunt.getBehaviors()) {
            if (behavior instanceof ZoneBehavior zb) {
                return zb;
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
            LogUtil.error("Error checking zone completion for hunt {0}: {1}", huntId, e.getMessage());
            return false;
        }
    }

    private void sendEntered(Player player, HBHunt hunt) {
        String message = registry.getLanguageService().message("Messages.ZoneEntered")
                .replace("%hunt%", hunt.getDisplayName());
        if (message.trim().isEmpty()) {
            return;
        }

        ZoneBehavior zb = findZoneBehavior(hunt);
        ZoneMessageMode mode = zb != null ? zb.messageMode() : ZoneMessageMode.CHAT;

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
        String message = registry.getLanguageService().message("Messages.ZoneExited")
                .replace("%hunt%", hunt.getDisplayName());
        if (!message.trim().isEmpty()) {
            player.sendMessage(message);
        }

        if (reset) {
            String resetMessage = registry.getLanguageService().message("Messages.ZoneProgressReset")
                    .replace("%hunt%", hunt.getDisplayName());
            if (!resetMessage.trim().isEmpty()) {
                player.sendMessage(resetMessage);
            }
        }
    }

    private void resetProgress(UUID uuid, String huntId) {
        try {
            registry.getStorageService().resetPlayerHunt(uuid, huntId);
        } catch (InternalException e) {
            LogUtil.error("Error resetting zone progress for hunt {0}: {1}", huntId, e.getMessage());
        }
    }

    private String huntName(String huntId) {
        HBHunt hunt = registry.getHuntService().getHuntById(huntId);
        return hunt != null ? hunt.getDisplayName() : huntId;
    }
}
