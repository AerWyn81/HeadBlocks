package fr.aerwyn81.headblocks.api.events;

import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class HuntStateChangeEvent extends AbstractCancellableEvent {
    public static final HandlerList handlers = new HandlerList();

    private final HBHunt hunt;
    private final HuntState oldState;
    private final HuntState newState;

    public HuntStateChangeEvent(HBHunt hunt, HuntState oldState, HuntState newState) {
        this.hunt = hunt;
        this.oldState = oldState;
        this.newState = newState;
    }

    public HBHunt getHunt() {
        return hunt;
    }

    public String getHuntId() {
        return hunt.getId();
    }

    public HuntState getOldState() {
        return oldState;
    }

    public HuntState getNewState() {
        return newState;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
