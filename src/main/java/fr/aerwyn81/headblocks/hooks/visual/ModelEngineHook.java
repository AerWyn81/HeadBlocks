package fr.aerwyn81.headblocks.hooks.visual;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.events.ModelRegistrationEvent;
import com.ticxo.modelengine.api.generator.ModelGenerator;
import com.ticxo.modelengine.api.generator.blueprint.ModelBlueprint;
import com.ticxo.modelengine.api.model.ModeledEntity;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.visual.Hitbox;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Interaction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class ModelEngineHook extends HandleProviderHook<ModelEngineHook.Model> {

    public record Model(ModeledEntity modeled, ModelBlueprint blueprint) {
    }

    private volatile boolean loaded;

    @Override
    public String prefix() {
        return "modelengine";
    }

    @Override
    public String pluginName() {
        return "ModelEngine";
    }

    @Override
    public boolean isReady() {
        return loaded || !ModelEngineAPI.getAPI().getModelRegistry().getOrderedId().isEmpty();
    }

    @Override
    public void register(Plugin plugin, Runnable onReload) {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onRegistration(ModelRegistrationEvent e) {
                if (e.getPhase() == ModelGenerator.Phase.FINISHED) {
                    loaded = true;
                    onReload.run();
                }
            }
        }, plugin);
    }

    @Override
    public boolean exists(String id) {
        return ModelEngineAPI.getBlueprint(id) != null;
    }

    @Override
    public ItemStack icon(HeadContent content) {
        return new ItemStack(Material.ARMOR_STAND);
    }

    @Override
    protected Model create(Interaction base, Location anchor, HeadContent content, RenderSettings settings) {
        var blueprint = ModelEngineAPI.getBlueprint(content.value());
        if (blueprint == null) {
            throw new IllegalStateException("unknown ModelEngine model " + content.value());
        }

        var modeled = ModelEngineAPI.createModeledEntity(base);
        var active = ModelEngineAPI.createActiveModel(blueprint);
        active.setScale(settings.scale());
        if (settings.glow()) {
            active.setGlowing(true);
        }

        modeled.addModel(active, true);
        modeled.setBaseEntityVisible(false);
        return new Model(modeled, blueprint);
    }

    @Override
    protected void destroy(Model handle) {
        handle.modeled().destroy();
    }

    @Override
    protected Hitbox hitbox(Model handle, HeadContent content, RenderSettings settings) {
        var main = handle.blueprint().getMainHitbox();
        return main == null
                ? Hitbox.of(content, settings, 1.0, 1.0)
                : Hitbox.of(content, settings, main.getWidth(), main.getHeight());
    }
}
