package fr.aerwyn81.headblocks.data;

import java.util.UUID;

public final class PlayerProfileLight {
    private final UUID uuid;
    private final String name;
    private final String displayName;

    public PlayerProfileLight(UUID uuid, String name, String displayName) {
        this.uuid = uuid;
        this.name = name;
        this.displayName = displayName;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public String displayName() {
        return displayName;
    }
}
