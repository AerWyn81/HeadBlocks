package fr.aerwyn81.headblocks.platform;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.concurrent.CompletableFuture;

public class SpigotPlatform implements Platform {

    @Override
    public String name() {
        return "Spigot";
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Entity entity, Location location) {
        return CompletableFuture.completedFuture(entity.teleport(location));
    }
}
