package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.hooks.HeadHidingPacketListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HeadVisibilityService {
    private final ServiceRegistry registry;
    private final Map<UUID, Set<UUID>> foundHeads = new ConcurrentHashMap<>();

    public HeadVisibilityService(ServiceRegistry registry) {
        this.registry = registry;
    }

    public void onSpawned(HeadLocation head, List<Entity> entities) {
        if (!isEnabled()) {
            return;
        }

        for (var player : Bukkit.getOnlinePlayers()) {
            if (hasFound(player.getUniqueId(), head.getUuid())) {
                registry.getScheduler().runNow(player, () -> setVisible(player, entities, false));
            }
        }
    }

    public void onHeadFound(Player player, HeadLocation head) {
        var found = foundHeads.get(player.getUniqueId());
        if (found != null) {
            found.add(head.getUuid());
        }

        if (registry.getVisualService().isEntityRendered(head)) {
            if (isEnabled()) {
                setVisible(player, registry.getVisualService().entitiesOf(head), false);
            }
            return;
        }

        var packetHiding = packetHiding();
        if (packetHiding != null) {
            packetHiding.addFoundHead(player, head.getUuid());
        }
    }

    public void onHeadReset(Player player, UUID headUuid) {
        var found = foundHeads.get(player.getUniqueId());
        if (found != null) {
            found.remove(headUuid);
        }

        var packetHiding = packetHiding();
        if (packetHiding != null) {
            packetHiding.removeFoundHead(player, headUuid);
        }

        var head = registry.getHeadService().getHeadByUUID(headUuid);
        if (head != null) {
            setVisible(player, registry.getVisualService().entitiesOf(head), true);
        }
    }

    public void onHuntReset(Player player, HBHunt hunt) {
        for (var head : registry.getHeadService().getHeadLocationsForHunt(hunt)) {
            onHeadReset(player, head.getUuid());
        }
    }

    public void onProgressReset(Player player) {
        var found = foundHeads.get(player.getUniqueId());
        if (found != null) {
            found.clear();
        }

        var packetHiding = packetHiding();
        if (packetHiding != null) {
            packetHiding.showAllPreviousHeads(player);
        }

        refreshEntities(player);
    }

    public void onJoin(Player player) {
        var packetHiding = packetHiding();
        if (packetHiding != null) {
            packetHiding.onPlayerJoin(player);
        }

        loadFoundHeads(player);
    }

    public void loadOnlinePlayers() {
        Bukkit.getOnlinePlayers().forEach(this::loadFoundHeads);
    }

    public void onQuit(UUID playerUuid) {
        foundHeads.remove(playerUuid);

        var packetHiding = packetHiding();
        if (packetHiding != null) {
            packetHiding.invalidatePlayerCache(playerUuid);
        }
    }

    public void refreshEntities(Player player) {
        var enabled = isEnabled();
        registry.getVisualService().forEachSpawned((head, entities) ->
                setVisible(player, entities, !(enabled && hasFound(player.getUniqueId(), head.getUuid()))));
    }

    private void loadFoundHeads(Player player) {
        if (!isEnabled()) {
            return;
        }

        registry.getStorageService().getHeadsPlayer(player.getUniqueId()).whenComplete(player, heads -> {
            if (heads == null || !player.isOnline()) {
                return;
            }

            Set<UUID> found = ConcurrentHashMap.newKeySet();
            found.addAll(heads);
            foundHeads.put(player.getUniqueId(), found);
            refreshEntities(player);
        });
    }

    private boolean hasFound(UUID playerUuid, UUID headUuid) {
        var found = foundHeads.get(playerUuid);
        return found != null && found.contains(headUuid);
    }

    private void setVisible(Player player, List<Entity> entities, boolean visible) {
        for (var entity : entities) {
            if (visible) {
                player.showEntity(HeadBlocks.getInstance(), entity);
            } else {
                player.hideEntity(HeadBlocks.getInstance(), entity);
            }
        }
    }

    private boolean isEnabled() {
        return registry.getConfigService().isHideFoundHeads();
    }

    private HeadHidingPacketListener packetHiding() {
        var hook = HeadBlocks.getInstance().getPacketEventsHook();
        return hook != null && hook.isEnabled() ? hook.getHeadHidingListener() : null;
    }
}
