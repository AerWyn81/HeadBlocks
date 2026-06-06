package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.services.ZoneEnforcementService;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class OnPlayerMoveEvent implements Listener {

    private final ServiceRegistry registry;

    public OnPlayerMoveEvent(ServiceRegistry registry) {
        this.registry = registry;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null || !crossedBlock(from, to)) {
            return;
        }

        Player player = e.getPlayer();
        if (isExempt(player)) {
            return;
        }

        ZoneEnforcementService service = registry.getZoneEnforcementService();
        if (service.evaluate(player, to) != ZoneEnforcementService.Decision.CONFINE) {
            return;
        }

        Location recovery = service.getRecoveryPoint(player, from);
        if (recovery == null && !hasGroundBelow(from)) {
            recovery = service.getReturnPoint(player);
        }
        if (recovery != null) {
            e.setTo(recovery);
            return;
        }

        Location blocked = from.clone();
        blocked.setYaw(to.getYaw());
        blocked.setPitch(to.getPitch());
        e.setTo(blocked);
    }

    private boolean hasGroundBelow(Location loc) {
        return loc.clone().subtract(0, 1, 0).getBlock().getType().isSolid();
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        Location to = e.getTo();
        if (to == null) {
            return;
        }

        Player player = e.getPlayer();
        if (isExempt(player)) {
            return;
        }

        ZoneEnforcementService service = registry.getZoneEnforcementService();
        if (service.evaluate(player, to) != ZoneEnforcementService.Decision.CONFINE) {
            return;
        }

        Location recovery = service.getRecoveryPoint(player, e.getFrom());
        if (recovery != null) {
            e.setTo(recovery);
        } else {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        if (isExempt(player)) {
            return;
        }

        Location recovery = registry.getZoneEnforcementService().getRecoveryPoint(player, e.getRespawnLocation());
        if (recovery != null) {
            e.setRespawnLocation(recovery);
        }
    }

    private boolean isExempt(Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return true;
        }
        return PlayerUtils.hasPermission(player, "headblocks.zone.bypass");
    }

    private boolean crossedBlock(Location from, Location to) {
        if (from.getWorld() != to.getWorld()) {
            return true;
        }
        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ();
    }
}
