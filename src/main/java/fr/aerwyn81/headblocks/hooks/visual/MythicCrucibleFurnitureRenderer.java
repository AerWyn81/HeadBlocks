package fr.aerwyn81.headblocks.hooks.visual;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.visual.Hitbox;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import io.lumine.mythiccrucible.api.MythicCrucibleProvider;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

class MythicCrucibleFurnitureRenderer extends HandleProviderHook<Entity> {

    private final Set<UUID> removing = ConcurrentHashMap.newKeySet();

    @Override
    public String prefix() {
        return "mythiccrucible";
    }

    @Override
    public String pluginName() {
        return "MythicCrucible";
    }

    @Override
    public boolean exists(String id) {
        return MythicCrucibleProvider.get().isFurnitureItem(id);
    }

    @Override
    public ItemStack icon(HeadContent content) {
        return new ItemStack(Material.ARMOR_STAND);
    }

    @Override
    protected Entity create(Interaction base, Location anchor, HeadContent content, RenderSettings settings) {
        var api = MythicCrucibleProvider.get();
        var block = anchor.getBlock();
        var result = api.placeFurnitureResult(content.value(), block, settings.yaw());
        if (!result.isSuccess()) {
            throw new IllegalStateException("MythicCrucible cannot place " + content.value() + ": " + result);
        }

        var furniture = api.getFurniture(block)
                .orElseThrow(() -> new IllegalStateException("MythicCrucible did not expose the placed furniture"));
        var frame = furniture.getItemFrame();
        frame.setGlowing(settings.glow());
        return frame;
    }

    @Override
    protected List<Entity> entitiesOf(Entity frame) {
        return List.of(frame);
    }

    @Override
    protected void destroy(Entity frame) {
        // Re-resolve after reloads: Crucible replaces its furniture handles.
        removing.add(frame.getUniqueId());
        try {
            MythicCrucibleProvider.get().getFurniture(frame).ifPresent(furniture ->
                    furniture.getFurnitureData().remove(furniture, null, false, false));
        } finally {
            removing.remove(frame.getUniqueId());
        }
    }

    boolean isRemoving(Entity frame) {
        return removing.contains(frame.getUniqueId());
    }

    @Override
    protected Hitbox hitbox(Entity frame, HeadContent content, RenderSettings settings) {
        return new Hitbox(content.optionDouble("width", 1.0), content.optionDouble("height", 1.0));
    }

    @Override
    protected void rotate(Entity frame, Entity base, float yaw) {
    }
}
