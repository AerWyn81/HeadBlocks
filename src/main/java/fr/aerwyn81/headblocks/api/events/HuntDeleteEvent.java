package fr.aerwyn81.headblocks.api.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class HuntDeleteEvent extends Event implements Cancellable {
    public static final HandlerList handlers = new HandlerList();

    private final String huntId;
    private boolean cancelled;

    public HuntDeleteEvent(String huntId) {
        this.huntId = huntId;
    }

    public String getHuntId() {
        return huntId;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
