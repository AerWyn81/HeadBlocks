package fr.aerwyn81.headblocks.api.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class HeadClickEvent extends Event {
    public static final HandlerList handlers = new HandlerList();

    private final UUID headUuid;
    private final Player player;
    private final Location location;

    public HeadClickEvent(UUID headUuid, Player player, Location location) {
        this.headUuid = headUuid;
        this.player = player;
        this.location = location;
    }

    public UUID getHeadUuid() {
        return headUuid;
    }

    public Player getPlayer() {
        return player;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
