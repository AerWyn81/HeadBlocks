package fr.aerwyn81.headblocks.api.events;

import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class HuntCreateEvent extends AbstractCancellableEvent {
    public static final HandlerList handlers = new HandlerList();

    private final HBHunt hunt;

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
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
