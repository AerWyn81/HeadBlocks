package fr.aerwyn81.headblocks.hooks.visual;

import de.tr7zw.changeme.nbtapi.NBT;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import dev.lone.itemsadder.api.Events.FurnitureInteractEvent;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import dev.lone.itemsadder.api.ItemsAdder;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class ItemsAdderHook extends ItemProviderHook {

    private static final String DATA_KEY = "itemsadder";

    private final ItemsAdderFurnitureRenderer furniture = new ItemsAdderFurnitureRenderer();

    @Override
    public String prefix() {
        return "itemsadder";
    }

    @Override
    public String pluginName() {
        return "ItemsAdder";
    }

    @Override
    public boolean isReady() {
        return ItemsAdder.areItemsLoaded();
    }

    @Override
    public void register(Plugin plugin, Runnable onReload) {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onDataLoaded(ItemsAdderLoadDataEvent e) {
                onReload.run();
            }
        }, plugin);
    }

    @Override
    public void listenInteractions(Plugin plugin, HeadEntityInteractions interactions) {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.LOW)
            public void onInteract(FurnitureInteractEvent e) {
                if (interactions.isHead(e.getBukkitEntity())) {
                    e.setCancelled(true);
                    interactions.use(e.getPlayer(), e.getBukkitEntity());
                }
            }

            @EventHandler(priority = EventPriority.LOW)
            public void onBreak(FurnitureBreakEvent e) {
                if (interactions.isHead(e.getBukkitEntity())) {
                    e.setCancelled(true);
                    interactions.attack(e.getPlayer(), e.getBukkitEntity());
                }
            }
        }, plugin);
    }

    @Override
    public boolean exists(String id) {
        return CustomStack.isInRegistry(id);
    }

    @Override
    protected ItemStack item(String id) {
        var stack = CustomStack.isInRegistry(id) ? CustomStack.getInstance(id) : null;
        return stack == null ? null : stack.getItemStack();
    }

    @Override
    protected ItemStack strip(ItemStack item) {
        var stripped = super.strip(item);
        NBT.modify(stripped, nbt -> {
            nbt.removeKey(DATA_KEY);
        });
        return stripped;
    }

    @Override
    public List<Entity> spawn(Location anchor, HeadContent content, RenderSettings settings) {
        return ItemsAdderFurnitureRenderer.supports(content.value())
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
