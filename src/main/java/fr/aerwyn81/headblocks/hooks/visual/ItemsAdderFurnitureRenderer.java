package fr.aerwyn81.headblocks.hooks.visual;

import dev.lone.itemsadder.api.CustomFurniture;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.visual.Hitbox;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.inventory.ItemStack;

import java.util.List;

class ItemsAdderFurnitureRenderer extends HandleProviderHook<CustomFurniture> {

    static boolean supports(String id) {
        return CustomFurniture.getNamespacedIdsInRegistry().contains(id);
    }

    @Override
    public String prefix() {
        return "itemsadder";
    }

    @Override
    public String pluginName() {
        return "ItemsAdder";
    }

    @Override
    public boolean exists(String id) {
        return supports(id);
    }

    @Override
    public ItemStack icon(HeadContent content) {
        return new ItemStack(Material.ARMOR_STAND);
    }

    @Override
    protected CustomFurniture create(Interaction base, Location anchor, HeadContent content, RenderSettings settings) {
        var furniture = CustomFurniture.spawn(content.value(), anchor.getBlock());
        if (furniture == null || furniture.getEntity() == null) {
            return null;
        }

        var entity = furniture.getEntity();
        entity.setRotation(settings.yaw(), 0);
        entity.setGlowing(settings.glow());
        return furniture;
    }

    @Override
    protected List<Entity> entitiesOf(CustomFurniture handle) {
        return handle.getEntity() == null ? List.of() : List.of(handle.getEntity());
    }

    @Override
    protected void destroy(CustomFurniture handle) {
        handle.remove(false);
    }

    @Override
    protected Hitbox hitbox(CustomFurniture handle, HeadContent content, RenderSettings settings) {
        return new Hitbox(content.optionDouble("width", 1.0), content.optionDouble("height", 1.0));
    }

    @Override
    protected void rotate(CustomFurniture handle, Entity base, float yaw) {
    }
}
