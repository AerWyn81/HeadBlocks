package fr.aerwyn81.headblocks.hooks;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.visual.EntityRenderer;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface VisualProviderHook extends EntityRenderer {
    String prefix();

    boolean isAvailable();

    boolean exists(String id);

    ItemStack icon(HeadContent content);

    @Override
    default double height(HeadContent content, RenderSettings settings, List<Entity> entities) {
        return 1.0;
    }
}
