package fr.aerwyn81.headblocks.hooks;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.plugin.java.JavaPlugin;

public class PacketEventsHook {

    private PacketEventsHookImpl packetEventsImpl;
    private boolean isEnabled;

    public boolean load(JavaPlugin plugin) {
        try {
            Class.forName("com.github.retrooper.packetevents.PacketEvents");

            packetEventsImpl = new PacketEventsHookImpl();
            isEnabled = packetEventsImpl.load(plugin);

            if (isEnabled) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("HeadBlocks &ePacketEvents &asuccessfully hooked!"));
            }

            return isEnabled;
        } catch (ClassNotFoundException e) {
            isEnabled = false;
            return false;
        }
    }

    public void init() {
        if (packetEventsImpl != null && isEnabled) {
            packetEventsImpl.init();
        }
    }

    public void unload() {
        if (packetEventsImpl != null && isEnabled) {
            packetEventsImpl.unload();
        }
    }

    public HeadHidingPacketListener getHeadHidingListener() {
        return packetEventsImpl != null
                ? packetEventsImpl.getHeadHidingListener()
                : null;
    }

    public boolean isEnabled() {
        return isEnabled && packetEventsImpl != null && packetEventsImpl.isEnabled();
    }
}
