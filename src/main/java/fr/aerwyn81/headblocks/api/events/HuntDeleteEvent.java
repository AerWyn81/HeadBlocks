package fr.aerwyn81.headblocks.api.events;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class HuntDeleteEvent extends AbstractCancellableEvent {
    public static final HandlerList handlers = new HandlerList();

    private final String huntId;

    public HuntDeleteEvent(String huntId) {
        this.huntId = huntId;
    }

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
