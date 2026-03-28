package fr.aerwyn81.headblocks.api.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public abstract class AbstractCancellableEvent extends Event implements Cancellable {

    private boolean cancelled;

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
