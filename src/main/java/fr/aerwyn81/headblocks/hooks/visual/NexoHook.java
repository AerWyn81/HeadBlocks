package fr.aerwyn81.headblocks.hooks.visual;

import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.api.events.NexoItemsLoadedEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureDamageEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class NexoHook extends ItemProviderHook {

    private final NexoFurnitureRenderer furniture = new NexoFurnitureRenderer();
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
    public void listenInteractions(Plugin plugin, HeadEntityInteractions interactions) {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.LOW)
            public void onInteract(NexoFurnitureInteractEvent e) {
                if (interactions.isHead(e.getBaseEntity())) {
                    e.setCancelled(true);
                    if (e.getHand() == EquipmentSlot.HAND) {
                        interactions.use(e.getPlayer(), e.getBaseEntity());
                    }
                }
            }

            @EventHandler(priority = EventPriority.LOW)
            public void onDamage(NexoFurnitureDamageEvent e) {
                if (interactions.isHead(e.getBaseEntity())) {
                    e.setCancelled(true);
                }
            }

            @EventHandler(priority = EventPriority.LOW)
            public void onBreak(NexoFurnitureBreakEvent e) {
                if (interactions.isHead(e.getBaseEntity())) {
                    e.setCancelled(true);
                    interactions.attack(e.getPlayer(), e.getBaseEntity());
                }
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

    @Override
    public List<Entity> spawn(Location anchor, HeadContent content, RenderSettings settings) {
        return NexoFurniture.isFurniture(content.value())
                ? furniture.spawn(anchor, content, settings)
                : super.spawn(anchor, content, settings);
    }

    @Override
    public void despawn(List<Entity> entities) {
        if (furniture.handles(entities)) {
            furniture.despawn(entities);
        } else {
            super.despawn(entities);
        }
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
