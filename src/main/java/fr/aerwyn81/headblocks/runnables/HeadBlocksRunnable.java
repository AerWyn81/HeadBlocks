package fr.aerwyn81.headblocks.runnables;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import fr.aerwyn81.headblocks.HeadBlocks;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.concurrent.CompletableFuture;

public abstract class HeadBlocksRunnable {

    @MonotonicNonNull
    private WrappedTask task = null;


    protected abstract void run();


    public WrappedTask runAsync() {
        this.task = new CompletablableWrappedTask<>(HeadBlocks.getScheduler().runAsync((task) -> this.run()));
        return this.task;
    }

    public WrappedTask runSync(Location location) {
        this.task = new CompletablableWrappedTask<>(HeadBlocks.getScheduler().runAtLocation(location, (task) -> this.run()));
        return this.task;
    }

    public WrappedTask runSync(Entity entity) {
        this.task = new CompletablableWrappedTask<>(HeadBlocks.getScheduler().runAtEntity(entity, (task) -> this.run()));
        return this.task;
    }

    public WrappedTask runAsyncDelayed(long delay) {
        this.task = HeadBlocks.getScheduler().runLaterAsync(this::run, delay);
        return this.task;
    }

    public WrappedTask runSyncDelayed(Location location, long delay) {
        this.task = HeadBlocks.getScheduler().runAtLocationLater(location, this::run, delay);
        return this.task;
    }

    public WrappedTask runSyncDelayed(Entity entity, long delay) {
        this.task = HeadBlocks.getScheduler().runAtEntityLater(entity, this::run, delay);
        return this.task;
    }


    public WrappedTask runAsyncRepeating(long delay, long period) {
        this.task = HeadBlocks.getScheduler().runTimerAsync(this::run, delay, period);
        return this.task;
    }

    public WrappedTask runSyncRepeating(Location location, long delay, long period) {
        this.task = HeadBlocks.getScheduler().runAtLocationTimer(location, this::run, delay, period);
        return this.task;
    }

    public WrappedTask runSyncRepeating(Entity entity, long delay, long period) {
        this.task = HeadBlocks.getScheduler().runAtEntityTimer(entity, this::run, delay, period);
        return this.task;
    }

    public WrappedTask runGlobal() {
        this.task = new CompletablableWrappedTask<>(HeadBlocks.getScheduler().runNextTick((task) -> this.run()));
        return this.task;
    }

    public WrappedTask runGlobalDelayed(long delay) {
        this.task = HeadBlocks.getScheduler().runLater(this::run, delay);
        return this.task;
    }


    public WrappedTask runGlobalRepeating(long delay, long period) {
        this.task = HeadBlocks.getScheduler().runTimer(this::run, delay, period);
        return this.task;
    }


    public CompletableFuture<Boolean> cancel() {
        return CompletableFuture.supplyAsync(() -> {
            long start = System.currentTimeMillis();
            while (task == null) {
                if (System.currentTimeMillis() - start > 500) return true; // 0.5s timeout
                Thread.onSpinWait();
            }
            task.cancel();
            return true;
        });
    }

    public CompletableFuture<Boolean> isCancelled() {
        return CompletableFuture.supplyAsync(() -> {
            long start = System.currentTimeMillis();
            while (task == null) {
                if (System.currentTimeMillis() - start > 500) return true; // 0.5s timeout, consider cancelled
                Thread.onSpinWait();
            }
            return task.isCancelled();
        });
    }
    
    
    public static class CompletablableWrappedTask<T> implements WrappedTask {
        
        private final CompletableFuture<T> future;
        private final boolean async;

        public CompletablableWrappedTask(CompletableFuture<T> future) {
            this(future, true);
        }
        
        public CompletablableWrappedTask(CompletableFuture<T> future, boolean async) {
            this.future = future;
            this.async = async;
        }

        @Override
        public void cancel() {
            future.cancel(true);
        }

        @Override
        public boolean isCancelled() {
            return future.isCancelled();
        }

        @Override
        public Plugin getOwningPlugin() {
            return HeadBlocks.getInstance();
        }

        @Override
        public boolean isAsync() {
            return async;
        }
    }
}
