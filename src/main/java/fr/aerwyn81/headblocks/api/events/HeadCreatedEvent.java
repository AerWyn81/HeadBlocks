package fr.aerwyn81.headblocks.api.events;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class HeadCreatedEvent extends Event {
    public static final HandlerList handlers = new HandlerList();

    private final UUID headUuid;
    private final Location location;
    private final String huntId;

    public HeadCreatedEvent(UUID headUuid, Location location) {
        this(headUuid, location, null);
    }

    public HeadCreatedEvent(UUID headUuid, Location location, String huntId) {
        this.headUuid = headUuid;
        this.location = location;
        this.huntId = huntId;
    }

    public UUID getHeadUuid() {
        return headUuid;
    }

    public Location getLocation() {
        return location;
    }

    @Nullable
    public String getHuntId() {
        return huntId;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
