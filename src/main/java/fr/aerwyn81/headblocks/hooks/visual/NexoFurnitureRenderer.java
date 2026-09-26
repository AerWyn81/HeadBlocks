package fr.aerwyn81.headblocks.hooks.visual;

import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import com.nexomc.nexo.utils.FurnitureHelpers;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.visual.Hitbox;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;

import java.util.List;

class NexoFurnitureRenderer extends HandleProviderHook<NexoFurnitureRenderer.Furniture> {

    record Furniture(FurnitureMechanic mechanic, ItemDisplay display) {
    }

    @Override
    public String prefix() {
        return "nexo";
    }

    @Override
    public String pluginName() {
        return "Nexo";
    }

    @Override
    public boolean exists(String id) {
        return NexoFurniture.isFurniture(id);
    }

    @Override
    public ItemStack icon(HeadContent content) {
        return new ItemStack(Material.ARMOR_STAND);
    }

    @Override
    protected Furniture create(Interaction base, Location anchor, HeadContent content, RenderSettings settings) {
        var mechanic = NexoFurniture.furnitureMechanic(content.value());
        if (mechanic == null) {
            throw new IllegalStateException("unknown Nexo furniture " + content.value());
        }

        var yaw = FurnitureHelpers.INSTANCE.correctedYaw(mechanic, settings.yaw());
        var display = mechanic.place(anchor.getBlock().getLocation(), yaw, BlockFace.UP, false);
        if (display == null) {
            return null;
        }

        display.setGlowing(settings.glow());
        return new Furniture(mechanic, display);
    }

    @Override
    protected List<Entity> entitiesOf(Furniture handle) {
        return List.of(handle.display());
    }

    @Override
    protected void destroy(Furniture handle) {
        handle.mechanic().removeBaseEntity(handle.display());
    }

    @Override
    protected Hitbox hitbox(Furniture handle, HeadContent content, RenderSettings settings) {
        var height = Math.max(1.0, handle.mechanic().getHitbox().hitboxHeight());
        return new Hitbox(content.optionDouble("width", 1.0), content.optionDouble("height", height));
    }

    @Override
    protected void rotate(Furniture handle, Entity base, float yaw) {
    }
}
