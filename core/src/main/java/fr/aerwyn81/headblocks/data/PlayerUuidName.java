package fr.aerwyn81.headblocks.data;

import java.util.UUID;

public class PlayerUuidName {
    private final UUID uuid;
    private final String name;

    public PlayerUuidName(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }
}
