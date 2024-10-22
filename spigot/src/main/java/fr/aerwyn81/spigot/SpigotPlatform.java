package fr.aerwyn81.spigot;

import fr.aerwyn81.common.IServerPlatform;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpigotPlatform implements IServerPlatform {

    private final JavaPlugin plugin;

    public SpigotPlatform(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnabled() {
        plugin.getLogger().info("Activating Spigot-specific features...");
    }

    @Override
    public void onDisabled() {

    }
}