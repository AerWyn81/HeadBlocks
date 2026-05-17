package fr.aerwyn81.headblocks.utils.paper;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.concurrent.CompletableFuture;

public final class PaperUtil {

    private static final PaperDelegates PAPER_DELEGATES = isPaper() ? new PaperDelegates() : null;

    public static boolean isPaper() {
        return classExists("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
    }

    public static boolean isFolia() {
        return classExists("io.papermc.paper.threadedregions.RegionizedServer");
    }

    public static CompletableFuture<Boolean> teleportAsync(Entity entity, Location location) {
        if (PAPER_DELEGATES != null) {
            return PAPER_DELEGATES.teleportAsync(entity, location);
        } else {
            return CompletableFuture.completedFuture(entity.teleport(location));
        }
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
