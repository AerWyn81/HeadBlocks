package fr.aerwyn81.headblocks.api.events;

import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class HeadCreatedEvent extends AbstractHeadEvent {
    public static final HandlerList handlers = new HandlerList();

    public HeadCreatedEvent(UUID headUuid, Location location) {
        this(headUuid, location, null);
    }

    public HeadCreatedEvent(UUID headUuid, Location location, String huntId) {
        super(headUuid, location, huntId);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
