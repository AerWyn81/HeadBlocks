package fr.aerwyn81.headblocks.api.events;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class HeadDeletedEvent extends Event {
    public static final HandlerList handlers = new HandlerList();

    private final UUID headUuid;
    private final Location location;

    public HeadDeletedEvent(UUID headUuid, Location location) {
        this.headUuid = headUuid;
        this.location = location;
    }

    public UUID getHeadUuid() {
        return headUuid;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}