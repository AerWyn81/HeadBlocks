package fr.aerwyn81.headblocks.data;

import org.bukkit.Location;

import java.util.UUID;

public class HeadMove {
    private final UUID hUuid;
    private final Location oldLoc;

    public HeadMove(UUID hUuid, Location oldLoc) {
        this.hUuid = hUuid;
        this.oldLoc = oldLoc;
    }

    public UUID gethUuid() {
        return hUuid;
    }

    public Location getOldLoc() {
        return oldLoc;
    }
}
