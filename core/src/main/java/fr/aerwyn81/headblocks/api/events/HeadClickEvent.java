package fr.aerwyn81.headblocks.api.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class HeadClickEvent extends Event {
    public static final HandlerList handlers = new HandlerList();

    private final UUID headUuid;
    private final Player player;
    private final Location location;
    private final boolean success;

    public HeadClickEvent(UUID headUuid, Player player, Location location, boolean success) {
        this.headUuid = headUuid;
        this.player = player;
        this.location = location;
        this.success = success;
    }

    public UUID getHeadUuid() {
        return headUuid;
    }

    public Player getPlayer() {
        return player;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
