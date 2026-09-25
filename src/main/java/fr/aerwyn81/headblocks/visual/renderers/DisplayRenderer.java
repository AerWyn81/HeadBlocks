package fr.aerwyn81.headblocks.visual.renderers;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.visual.EntityRenderer;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;

import java.util.List;
import java.util.Objects;

public abstract class DisplayRenderer implements EntityRenderer {

    protected abstract Display spawnDisplay(Location anchor, HeadContent content, float scale);

    protected abstract double size(float scale);

    protected double width(HeadContent content, float scale) {
        return size(scale);
    }

    protected double height(HeadContent content, float scale) {
        return size(scale);
    }

    @Override
    public List<Entity> spawn(Location anchor, HeadContent content, RenderSettings settings) {
        var scale = (float) settings.scale();
        var display = spawnDisplay(anchor, content, scale);
        display.setGlowing(settings.glow());

        var interaction = Objects.requireNonNull(anchor.getWorld()).spawn(anchor, Interaction.class);
        interaction.setInteractionWidth((float) width(content, scale));
        interaction.setInteractionHeight((float) height(content, scale));
        interaction.setResponsive(true);

        return List.of(display, interaction);
    }

    @Override
    public double height(HeadContent content, RenderSettings settings, List<Entity> entities) {
        return height(content, (float) settings.scale());
    }

    @Override
    public void spin(List<Entity> entities, float angle, RenderSettings settings, int periodTicks) {
        for (var entity : entities) {
            if (!(entity instanceof Display display) || !display.isValid()) {
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
