package fr.aerwyn81.headblocks.hooks.visual;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.visual.Hitbox;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class HandleProviderHook<H> implements VisualProviderHook {

    private final Map<UUID, H> handles = new ConcurrentHashMap<>();

    protected abstract H create(Interaction base, Location anchor, HeadContent content, RenderSettings settings);

    protected abstract void destroy(H handle);

    protected abstract Hitbox hitbox(H handle, HeadContent content, RenderSettings settings);

    protected List<Entity> entitiesOf(H handle) {
        return List.of();
    }

    protected boolean usesBase() {
        return true;
    }

    protected void setVisible(H handle, Player player, boolean visible) {
    }

    protected void rotate(H handle, Entity base, float yaw) {
        base.setRotation(yaw, 0);
    }


    @Override
    public List<Entity> spawn(Location anchor, HeadContent content, RenderSettings settings) {
        var base = usesBase() ? Objects.requireNonNull(anchor.getWorld()).spawn(anchor, Interaction.class) : null;

        H handle;
        try {
            handle = create(base, anchor, content, settings);
        } catch (RuntimeException e) {
            removeBase(base);
            throw e;
        }

        if (handle == null) {
            removeBase(base);
            throw new IllegalStateException(prefix() + " cannot render " + content.value());
        }

        List<Entity> entities = new ArrayList<>();
        if (base != null) {
            var hitbox = hitbox(handle, content, settings);
            base.setInteractionWidth((float) hitbox.width());
            base.setInteractionHeight((float) hitbox.height());
            base.setResponsive(true);
            entities.add(base);
        }
        entities.addAll(entitiesOf(handle));

        if (entities.isEmpty()) {
            destroy(handle);
            throw new IllegalStateException(prefix() + " did not spawn " + content.value());
        }

        handles.put(entities.get(0).getUniqueId(), handle);
        return entities;
    }

    @Override
    public void despawn(List<Entity> entities) {
        if (!entities.isEmpty()) {
            var handle = handles.remove(entities.get(0).getUniqueId());
            if (handle != null) {
                try {
                    destroy(handle);
                } catch (RuntimeException e) {
                    LogUtil.error("Cannot remove a {0} render: {1}", pluginName(), e.getMessage());
                }
            }
        }

        entities.forEach(Entity::remove);
    }

    @Override
    public double height(HeadContent content, RenderSettings settings, List<Entity> entities) {
        var handle = handleOf(entities);
        return handle == null ? 1.0 : hitbox(handle, content, settings).height();
    }

    @Override
    public void setVisible(Player player, List<Entity> entities, boolean visible) {
        var handle = handleOf(entities);
        if (handle != null) {
            setVisible(handle, player, visible);
        }
    }

    @Override
    public void spin(List<Entity> entities, float angle, RenderSettings settings, int periodTicks) {
        var handle = handleOf(entities);
        if (handle != null && entities.get(0).isValid()) {
            rotate(handle, entities.get(0), HeadUtils.normalizeYaw(settings.yaw() + angle));
        }
    }

    boolean handles(List<Entity> entities) {
        return handleOf(entities) != null;
    }

    private H handleOf(List<Entity> entities) {
        return entities.isEmpty() ? null : handles.get(entities.get(0).getUniqueId());
    }

    private static void removeBase(Entity base) {
        if (base != null) {
            base.remove();
        }
    }
}
