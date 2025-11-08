package fr.aerwyn81.headblocks.hooks;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.java.JavaPlugin;

public class PacketEventsHook {

    private boolean isEnabled;

    public boolean load(JavaPlugin plugin) {
        try {
            PacketEvents.setAPI(SpigotPacketEventsBuilder.build(plugin));
            PacketEvents.getAPI().load();

            isEnabled = PacketEvents.getAPI().isLoaded();
            return isEnabled;
        } catch (Exception | NoClassDefFoundError ignored) {
            return false;
        }
    }

    public void init() {
        if (!isEnabled) {
            return;
        }

        PacketEvents.getAPI().getSettings().checkForUpdates(false);
        PacketEvents.getAPI().init();
    }

    public void unload() {
        if (!isEnabled) {
            return;
        }

        PacketEvents.getAPI().terminate();
    }
}
