package fr.aerwyn81.headblocks.hooks.visual;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import fr.aerwyn81.headblocks.visual.renderers.ItemDisplayRenderer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public abstract class ItemProviderHook implements VisualProviderHook {

    private final ItemDisplayRenderer renderer = new ItemDisplayRenderer(this::requireItem);

    protected abstract ItemStack item(String id);

    private ItemStack requireItem(HeadContent content) {
        var item = item(content.value());
        if (item == null) {
            throw new IllegalStateException("unknown " + pluginName() + " item " + content.value());
        }
        return item;
    }

    @Override
    public boolean exists(String id) {
        return item(id) != null;
    }

    @Override
    public ItemStack icon(HeadContent content) {
        var item = item(content.value());
        return item == null ? new ItemStack(Material.BARRIER) : strip(item.clone());
    }

    protected ItemStack strip(ItemStack item) {
        var meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        var container = meta.getPersistentDataContainer();
        for (var key : container.getKeys()) {
            container.remove(key);
        }
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public List<Entity> spawn(Location anchor, HeadContent content, RenderSettings settings) {
        return renderer.spawn(anchor, content, settings);
    }

    @Override
    public double height(HeadContent content, RenderSettings settings, List<Entity> entities) {
        return renderer.height(content, settings, entities);
    }

    @Override
    public void spin(List<Entity> entities, float angle, RenderSettings settings, int periodTicks) {
        renderer.spin(entities, angle, settings, periodTicks);
    }
}
