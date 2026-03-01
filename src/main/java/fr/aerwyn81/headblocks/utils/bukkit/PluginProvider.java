package fr.aerwyn81.headblocks.utils.bukkit;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.util.logging.Logger;

public interface PluginProvider {
    File getDataFolder();

    InputStream getResource(String filename);

    Logger getLogger();

    JavaPlugin getJavaPlugin();

    boolean isPlaceholderApiActive();

    boolean isPacketEventsActive();

    boolean isHeadDatabaseActive();
}
