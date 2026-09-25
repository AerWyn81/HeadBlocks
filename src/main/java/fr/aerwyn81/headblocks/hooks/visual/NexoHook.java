package fr.aerwyn81.headblocks.hooks.visual;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.api.events.NexoItemsLoadedEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class NexoHook extends ItemProviderHook {

    private volatile boolean loaded;

    @Override
    public String prefix() {
        return "nexo";
    }

    @Override
    public String pluginName() {
        return "Nexo";
    }

    @Override
    public boolean isReady() {
        return loaded || !NexoItems.itemNames().isEmpty();
    }

    @Override
    public void register(Plugin plugin, Runnable onReload) {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onItemsLoaded(NexoItemsLoadedEvent e) {
                loaded = true;
                onReload.run();
            }
        }, plugin);
    }

    @Override
    public boolean exists(String id) {
        return NexoItems.exists(id);
    }

    @Override
    protected ItemStack item(String id) {
        var builder = NexoItems.exists(id) ? NexoItems.itemFromId(id) : null;
        return builder == null ? null : builder.build();
    }
}
