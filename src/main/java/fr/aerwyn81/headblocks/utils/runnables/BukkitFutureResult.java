package fr.aerwyn81.headblocks.utils.runnables;

import fr.aerwyn81.headblocks.HeadBlocks;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

public class BukkitFutureResult<T> {
    private final Plugin plugin;
    private final CompletableFuture<T> future;

    private BukkitFutureResult(Plugin plugin, CompletableFuture<T> future) {
        this.plugin = plugin;
        this.future = future;
    }

    public static <T> BukkitFutureResult<T> of(Plugin plugin, CompletableFuture<T> future) {
        return new BukkitFutureResult<>(plugin, future);
    }

    public void whenComplete(@NotNull Consumer<? super T> callback) {
        whenComplete(plugin, callback);
    }

    public void whenComplete(@NotNull Consumer<? super T> callback, Consumer<Throwable> throwable) {
        whenComplete(plugin, callback, throwable);
    }

    public void whenComplete(@NotNull Plugin plugin, @NotNull Consumer<? super T> callback, Consumer<Throwable> throwableConsumer) {
        dispatch(r -> HeadBlocks.getScheduler().runTask(r), callback, throwableConsumer);
    }

    public void whenComplete(@NotNull Plugin plugin, @NotNull Consumer<? super T> callback) {
        whenComplete(plugin, callback, throwable ->
                plugin.getLogger().log(Level.SEVERE, "Exception in Future Result", throwable));
    }

    public void whenComplete(@Nullable Entity entity, @NotNull Consumer<? super T> callback) {
        whenComplete(entity, callback, throwable ->
                plugin.getLogger().log(Level.SEVERE, "Exception in Future Result", throwable));
    }

    public void whenComplete(@Nullable Entity entity, @NotNull Consumer<? super T> callback, Consumer<Throwable> throwableConsumer) {
        dispatch(r -> HeadBlocks.getScheduler().runTask(entity, r), callback, throwableConsumer);
    }

    private void dispatch(Executor executor, Consumer<? super T> callback, Consumer<Throwable> throwableConsumer) {
        this.future.thenAcceptAsync(callback, executor).exceptionally(throwable -> {
            throwableConsumer.accept(throwable);
            return null;
        });
    }

    public @Nullable T join() {
        return this.future.join();
    }

    public @NotNull CompletableFuture<T> asFuture() {
        return this.future.thenApply(Function.identity());
    }
}
