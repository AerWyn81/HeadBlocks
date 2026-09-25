package fr.aerwyn81.headblocks.visual.renderers;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.visual.ContentItems;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Objects;

public class ItemDisplayRenderer extends DisplayRenderer {

    private static final double ITEM_SIZE = 0.5;

    @Override
    protected Display spawnDisplay(Location anchor, HeadContent content, float scale) {
        var display = Objects.requireNonNull(anchor.getWorld()).spawn(anchor, ItemDisplay.class);
        display.setItemStack(ContentItems.itemOf(content));
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        display.setTransformation(new Transformation(
                new Vector3f(0, (float) (size(scale) / 2), 0),
                new AxisAngle4f(),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f()));
        return display;
    }

    @Override
    protected double size(float scale) {
        return ITEM_SIZE * scale;
    }

    @Override
    public void spin(List<Entity> entities, float angle, RenderSettings settings, int periodTicks) {
        for (var entity : entities) {
            if (!(entity instanceof ItemDisplay display) || !display.isValid()) {
                continue;
            }

            var current = display.getTransformation();
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(Math.max(1, periodTicks));
            display.setTransformation(new Transformation(current.getTranslation(),
                    new Quaternionf().rotationY((float) Math.toRadians(-angle)),
                    current.getScale(), current.getRightRotation()));
        }
    }
}
