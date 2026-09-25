package fr.aerwyn81.headblocks.visual.renderers;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.visual.EntityRenderer;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.Objects;

public class MobRenderer implements EntityRenderer {

    @Override
    public List<Entity> spawn(Location anchor, HeadContent content, RenderSettings settings) {
        var type = typeOf(content);

        var entity = Objects.requireNonNull(anchor.getWorld()).spawnEntity(anchor, type);
        entity.setSilent(true);
        entity.setGravity(false);
        entity.setGlowing(settings.glow());

        if (entity instanceof LivingEntity living) {
            living.setAI(false);
            living.setRemoveWhenFarAway(false);
            living.setCanPickupItems(false);
            living.setCollidable(false);
        }

        entity.setRotation(settings.yaw(), 0);
        return List.of(entity);
    }

    @Override
    public double height(HeadContent content, RenderSettings settings, List<Entity> entities) {
        return entities.isEmpty() || !entities.get(0).isValid() ? 1.0 : entities.get(0).getHeight();
    }

    @Override
    public void spin(List<Entity> entities, float angle, RenderSettings settings, int periodTicks) {
        for (var entity : entities) {
            if (entity.isValid()) {
                entity.setRotation(HeadUtils.normalizeYaw(settings.yaw() + angle), 0);
            }
        }
    }

    @Override
    public boolean anchored() {
        return true;
    }

    public static EntityType typeOf(HeadContent content) {
        EntityType type;
        try {
            type = EntityType.valueOf(content.value().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("unknown entity type " + content.value());
        }

        if (!type.isSpawnable() || !type.isAlive()) {
            throw new IllegalStateException("entity type " + content.value() + " cannot be used");
        }

        return type;
    }
}
