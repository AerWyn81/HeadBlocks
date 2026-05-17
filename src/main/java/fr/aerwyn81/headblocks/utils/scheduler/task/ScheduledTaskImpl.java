package fr.aerwyn81.headblocks.utils.scheduler.task;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public class ScheduledTaskImpl implements Task {

    private final ScheduledTask delegate;

    public ScheduledTaskImpl(ScheduledTask delegate) {
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
