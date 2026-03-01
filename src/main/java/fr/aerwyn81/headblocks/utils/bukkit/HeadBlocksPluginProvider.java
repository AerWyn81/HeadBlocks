package fr.aerwyn81.headblocks.utils.bukkit;

import fr.aerwyn81.headblocks.HeadBlocks;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.util.logging.Logger;

public class HeadBlocksPluginProvider implements PluginProvider {
    private final HeadBlocks plugin;

    public HeadBlocksPluginProvider(HeadBlocks plugin) {
        this.plugin = plugin;
    }

    @Override
    public File getDataFolder() {
        return plugin.getDataFolder();
    }

    @Override
    public InputStream getResource(String filename) {
        return plugin.getResource(filename);
    }

    @Override
    public Logger getLogger() {
        return plugin.getLogger();
    }

    @Override
    public JavaPlugin getJavaPlugin() {
        return plugin;
    }

    @Override
    public boolean isPlaceholderApiActive() {
        return HeadBlocks.isPlaceholderApiActive;
    }

    @Override
    public boolean isPacketEventsActive() {
        return HeadBlocks.isPacketEventsActive;
    }

    @Override
    public boolean isHeadDatabaseActive() {
        return HeadBlocks.isHeadDatabaseActive;
    }
}
