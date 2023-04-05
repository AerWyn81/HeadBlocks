package fr.aerwyn81.headblocks.data;

import org.bukkit.Location;

import java.util.UUID;

public class HeadMove {
    private final UUID hUuid;
    private final Location oldLoc;
    private final String trackId;

    public HeadMove(UUID hUuid, Location oldLoc, String trackId) {
        this.hUuid = hUuid;
        this.oldLoc = oldLoc;
        this.trackId = trackId;
    }

    public UUID gethUuid() {
        return hUuid;
    }

    public Location getOldLoc() {
        return oldLoc;
    }

    public String getTrackId() {
        return trackId;
    }
}
