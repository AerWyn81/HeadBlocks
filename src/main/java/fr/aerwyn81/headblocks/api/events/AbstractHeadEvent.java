package fr.aerwyn81.headblocks.api.events;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class AbstractHeadEvent extends Event {

    private final UUID headUuid;
    private final Location location;
    private final String huntId;

    protected AbstractHeadEvent(UUID headUuid, Location location, String huntId) {
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
}
