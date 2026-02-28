package fr.aerwyn81.headblocks.api.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class HeadClickEvent extends Event {
    public static final HandlerList handlers = new HandlerList();

    private final UUID headUuid;
    private final Player player;
    private final Location location;
    private final boolean success;
    private final List<String> huntIds;

    public HeadClickEvent(UUID headUuid, Player player, Location location, boolean success) {
        this(headUuid, player, location, success, Collections.emptyList());
    }

    public HeadClickEvent(UUID headUuid, Player player, Location location, boolean success, List<String> huntIds) {
        this.headUuid = headUuid;
        this.player = player;
        this.location = location;
        this.success = success;
        this.huntIds = huntIds != null ? huntIds : Collections.emptyList();
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

    public List<String> getHuntIds() {
        return huntIds;
    }

    @Nullable
    public String getHuntId() {
        return huntIds.isEmpty() ? null : huntIds.get(0);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
