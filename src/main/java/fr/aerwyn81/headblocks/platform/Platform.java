package fr.aerwyn81.headblocks.platform;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.concurrent.CompletableFuture;

public interface Platform {

    String name();

    CompletableFuture<Boolean> teleportAsync(Entity entity, Location location);
}
