package fr.aerwyn81.headblocks.data;

import java.util.UUID;

public final class PlayerProfileLight {
    private final UUID uuid;
    private final String name;
    private final String customDisplay;

    public PlayerProfileLight(UUID uuid, String name, String customDisplay) {
        this.uuid = uuid;
        this.name = name;
        this.customDisplay = customDisplay;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public String customDisplay() {
        return customDisplay;
    }
}
