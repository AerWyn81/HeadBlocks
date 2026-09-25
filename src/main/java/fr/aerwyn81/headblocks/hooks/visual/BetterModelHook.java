package fr.aerwyn81.headblocks.hooks.visual;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.visual.Hitbox;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.ModelScaler;
import kr.toxicity.model.api.tracker.TrackerUpdateAction;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class BetterModelHook extends HandleProviderHook<EntityTracker> {

    private volatile boolean loaded;

    @Override
    public String prefix() {
        return "bettermodel";
    }

    @Override
    public String pluginName() {
        return "BetterModel";
    }

    @Override
    public boolean isReady() {
        return loaded || !BetterModel.modelKeys().isEmpty();
    }

    @Override
    public void register(Plugin plugin, Runnable onReload) {
        BetterModel.platform().addReloadEndHandler(result -> {
            loaded = true;
            onReload.run();
        });
    }

    @Override
    public boolean exists(String id) {
        return BetterModel.modelOrNull(id) != null;
    }

    @Override
    public ItemStack icon(HeadContent content) {
        return new ItemStack(Material.ARMOR_STAND);
    }

    @Override
    protected EntityTracker create(Interaction base, Location anchor, HeadContent content, RenderSettings settings) {
        var renderer = BetterModel.modelOrNull(content.value());
        if (renderer == null) {
            throw new IllegalStateException("unknown BetterModel model " + content.value());
        }

        var tracker = renderer.create(BukkitAdapter.adapt(base));
        if (settings.scale() != 1.0) {
            tracker.scaler(ModelScaler.value((float) settings.scale()));
        }
        if (settings.glow()) {
            tracker.update(TrackerUpdateAction.glow(true));
        }
        return tracker;
    }

    @Override
    protected void destroy(EntityTracker handle) {
        handle.close();
    }

    @Override
    protected Hitbox hitbox(EntityTracker handle, HeadContent content, RenderSettings settings) {
        return Hitbox.of(content, settings, 1.0, 1.0);
    }

    @Override
    protected void setVisible(EntityTracker handle, Player player, boolean visible) {
        if (visible) {
            handle.show(BukkitAdapter.adapt(player));
        } else {
            handle.hide(BukkitAdapter.adapt(player));
        }
    }
}
