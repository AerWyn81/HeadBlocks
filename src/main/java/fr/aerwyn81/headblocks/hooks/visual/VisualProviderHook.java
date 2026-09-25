package fr.aerwyn81.headblocks.hooks.visual;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.visual.EntityRenderer;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;

public interface VisualProviderHook extends EntityRenderer {
    String prefix();

    String pluginName();

    default boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled(pluginName());
    }

    default boolean isReady() {
        return true;
    }

    default void register(Plugin plugin, Runnable onReload) {
    }

    boolean exists(String id);

    ItemStack icon(HeadContent content);

    @Override
    default double height(HeadContent content, RenderSettings settings, List<Entity> entities) {
        return 1.0;
    }
}
