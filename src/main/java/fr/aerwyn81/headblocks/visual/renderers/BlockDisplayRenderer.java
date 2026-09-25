package fr.aerwyn81.headblocks.visual.renderers;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.visual.ContentItems;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Objects;

public class BlockDisplayRenderer extends DisplayRenderer {

    @Override
    protected Display spawnDisplay(Location anchor, HeadContent content, float scale) {
        var data = ContentItems.blockDataOf(content);
        if (data == null) {
            throw new IllegalStateException("invalid block " + content.value());
        }

        var display = Objects.requireNonNull(anchor.getWorld()).spawn(anchor, BlockDisplay.class);
        display.setBlock(data);
        display.setTransformation(new Transformation(
                new Vector3f(-scale / 2, 0, -scale / 2),
                new AxisAngle4f(),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f()));
        return display;
    }

    @Override
    protected double size(float scale) {
        return scale;
    }

    @Override
    public void spin(List<Entity> entities, float angle, RenderSettings settings, int periodTicks) {
        for (var entity : entities) {
            if (entity instanceof BlockDisplay && entity.isValid()) {
                entity.setRotation(HeadUtils.normalizeYaw(settings.yaw() + angle), 0);
            }
        }
    }
}
