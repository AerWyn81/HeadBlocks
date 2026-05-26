package fr.aerwyn81.headblocks.utils.scheduler.task;

import fr.aerwyn81.headblocks.HeadBlocks;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public abstract class TaskTimer implements Runnable {

    @MonotonicNonNull
    private Task handle;

    public void cancel() {
        if (handle != null) {
            handle.cancel();
        }
    }

    public boolean isCancelled() {
        if (handle == null) {
            return true;
        }
        return handle.isCancelled();
    }

    public void runNowGlobal() {
        HeadBlocks.getScheduler().runTaskGlobal(this);
    }

    public void delayedGlobal(long delay) {
        HeadBlocks.getScheduler().runTaskGlobalLater(this, delay);
    }

    public void repeatingGlobal(long initialDelay, long period) {
        handle = HeadBlocks.getScheduler().runTaskGlobalTimer(this, initialDelay, period);
    }
}
