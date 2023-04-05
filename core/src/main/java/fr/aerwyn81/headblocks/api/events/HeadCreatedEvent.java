package fr.aerwyn81.headblocks.api.events;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class HeadCreatedEvent extends Event {
    public static final HandlerList handlers = new HandlerList();

    private final UUID headUuid;
    private final Location location;

    public HeadCreatedEvent(UUID headUuid, Location location) {
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
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
