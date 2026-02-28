package fr.aerwyn81.headblocks.api.events;

import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class HuntStateChangeEvent extends Event implements Cancellable {
    public static final HandlerList handlers = new HandlerList();

    private final Hunt hunt;
    private final HuntState oldState;
    private final HuntState newState;
    private boolean cancelled;

    public HuntStateChangeEvent(Hunt hunt, HuntState oldState, HuntState newState) {
        this.hunt = hunt;
        this.oldState = oldState;
        this.newState = newState;
    }

    public Hunt getHunt() {
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
