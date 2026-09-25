package fr.aerwyn81.headblocks.visual.renderers;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.visual.ContentItems;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.function.Function;

public class ItemDisplayRenderer extends DisplayRenderer {

    private static final double ITEM_SIZE = 0.5;

    private final Function<HeadContent, ItemStack> items;

    public ItemDisplayRenderer() {
        this(ContentItems::itemOf);
    }

    public ItemDisplayRenderer(Function<HeadContent, ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Display spawnDisplay(Location anchor, HeadContent content, float scale) {
        var item = items.apply(content);
        var display = Objects.requireNonNull(anchor.getWorld()).spawn(anchor, ItemDisplay.class);
        display.setItemStack(item);
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
}
