package fr.aerwyn81.headblocks.platform;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.concurrent.CompletableFuture;

public class PaperPlatform implements Platform {

    @Override
    public String name() {
        return "Paper";
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Entity entity, Location location) {
        return entity.teleportAsync(location);
    }
}
