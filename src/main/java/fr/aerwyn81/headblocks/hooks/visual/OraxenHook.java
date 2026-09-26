package fr.aerwyn81.headblocks.hooks.visual;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class OraxenHook extends ItemProviderHook {

    private final OraxenFurnitureRenderer furniture = new OraxenFurnitureRenderer(content -> item(content.value()));

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

    @Override
    public List<Entity> spawn(Location anchor, HeadContent content, RenderSettings settings) {
        return OraxenFurnitureRenderer.supports(content.value())
                ? furniture.spawn(anchor, content, settings)
                : super.spawn(anchor, content, settings);
    }

    @Override
    public void despawn(List<Entity> entities) {
        furniture.forget(entities);
        super.despawn(entities);
    }

    @Override
    public double height(HeadContent content, RenderSettings settings, List<Entity> entities) {
        return furniture.handles(entities)
                ? furniture.height(content, settings, entities)
                : super.height(content, settings, entities);
    }

    @Override
    public void spin(List<Entity> entities, float angle, RenderSettings settings, int periodTicks) {
        if (!furniture.handles(entities)) {
            super.spin(entities, angle, settings, periodTicks);
        }
    }
}
