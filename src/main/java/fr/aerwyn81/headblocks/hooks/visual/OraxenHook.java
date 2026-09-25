package fr.aerwyn81.headblocks.hooks.visual;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class OraxenHook extends ItemProviderHook {

    private volatile boolean loaded;

    @Override
    public String prefix() {
        return "oraxen";
    }

    @Override
    public String pluginName() {
        return "Oraxen";
    }

    @Override
    public boolean isReady() {
        return loaded || !OraxenItems.getNames().isEmpty();
    }

    @Override
    public void register(Plugin plugin, Runnable onReload) {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onItemsLoaded(OraxenItemsLoadedEvent e) {
                loaded = true;
                onReload.run();
            }
        }, plugin);
    }

    @Override
    public boolean exists(String id) {
        return OraxenItems.exists(id);
    }

    @Override
    protected ItemStack item(String id) {
        var builder = OraxenItems.exists(id) ? OraxenItems.getItemById(id) : null;
        return builder == null ? null : builder.build();
    }
}
