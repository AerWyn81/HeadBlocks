package fr.aerwyn81.core;

import fr.aerwyn81.common.IServerPlatform;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class HeadBlocks extends JavaPlugin {

    private final IServerPlatform platform;

    public HeadBlocks() {
        platform = PlatformFactory.createPlatform(this);
    }

    @Override
    public void onEnable() {
        platform.onEnabled();
    }

    @Override
    public void onDisable() {
        platform.onDisabled();
    }
}
