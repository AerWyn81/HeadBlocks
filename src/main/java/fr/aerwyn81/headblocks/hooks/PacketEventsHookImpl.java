package fr.aerwyn81.headblocks.hooks;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Internal implementation of PacketEvents integration.
 * This class references PacketEvents classes and should only be loaded when PacketEvents is present.
 */
class PacketEventsHookImpl {

    private boolean isEnabled;
    private HeadHidingPacketListener headHidingListener;

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

        headHidingListener = new HeadHidingPacketListener();
        PacketEvents.getAPI().getEventManager().registerListener(headHidingListener, PacketListenerPriority.NORMAL);
    }

    public void unload() {
        if (!isEnabled) {
            return;
        }

        if (headHidingListener != null) {
            headHidingListener.clearCache();
        }

        PacketEvents.getAPI().terminate();
    }

    public HeadHidingPacketListener getHeadHidingListener() {
        return headHidingListener;
    }

    public boolean isEnabled() {
        return isEnabled;
    }
}
