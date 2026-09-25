package fr.aerwyn81.headblocks.visual.renderers;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.visual.EntityRenderer;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;

import java.util.List;
import java.util.Objects;

public abstract class DisplayRenderer implements EntityRenderer {

    protected abstract Display spawnDisplay(Location anchor, HeadContent content, float scale);

    protected abstract double size(float scale);

    @Override
    public List<Entity> spawn(Location anchor, HeadContent content, RenderSettings settings) {
        var scale = (float) settings.scale();
        var display = spawnDisplay(anchor, content, scale);
        display.setGlowing(settings.glow());

        var size = (float) size(scale);
        var interaction = Objects.requireNonNull(anchor.getWorld()).spawn(anchor, Interaction.class);
        interaction.setInteractionWidth(size);
        interaction.setInteractionHeight(size);
        interaction.setResponsive(true);

        return List.of(display, interaction);
    }

    @Override
    public double height(HeadContent content, RenderSettings settings, List<Entity> entities) {
        return size((float) settings.scale());
    }
}
