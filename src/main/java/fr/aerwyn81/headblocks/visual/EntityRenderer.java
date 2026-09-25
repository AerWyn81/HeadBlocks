package fr.aerwyn81.headblocks.visual;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;

public interface EntityRenderer {

    List<Entity> spawn(Location anchor, HeadContent content, RenderSettings settings);

    double height(HeadContent content, RenderSettings settings, List<Entity> entities);

    default void despawn(List<Entity> entities) {
        entities.forEach(Entity::remove);
    }

    default void spin(List<Entity> entities, float angle, RenderSettings settings, int periodTicks) {
    }

    default void setVisible(Player player, List<Entity> entities, boolean visible) {
    }

    default boolean anchored() {
        return false;
    }
}
