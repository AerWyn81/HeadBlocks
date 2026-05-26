package fr.aerwyn81.headblocks.utils.scheduler.task;

import org.bukkit.scheduler.BukkitTask;

public class BukkitTaskImpl implements Task {

    private final BukkitTask delegate;

    public BukkitTaskImpl(BukkitTask delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isCancelled() {
        return delegate.isCancelled();
    }

    @Override
    public void cancel() {
        delegate.cancel();
    }
}
