package fr.aerwyn81.paper;

import fr.aerwyn81.common.IServerPlatform;
import org.bukkit.plugin.java.JavaPlugin;

public final class PaperPlatform implements IServerPlatform {

    private final JavaPlugin plugin;

    public PaperPlatform(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnabled() {
        plugin.getLogger().info("Activating Paper-specific features...");
    }

    @Override
    public void onDisabled() {

    }
}
