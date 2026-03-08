package fr.aerwyn81.headblocks.api.events;

import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class HuntCreateEvent extends Event implements Cancellable {
    public static final HandlerList handlers = new HandlerList();

    private final HBHunt hunt;
    private boolean cancelled;

    public HuntCreateEvent(HBHunt hunt) {
        this.hunt = hunt;
    }

    public HBHunt getHunt() {
        return hunt;
    }

    public String getHuntId() {
        return hunt.getId();
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
