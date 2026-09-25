package fr.aerwyn81.headblocks.visual.renderers;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.visual.ContentItems;
import fr.aerwyn81.headblocks.visual.EntityRenderer;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import org.bukkit.Location;
import org.bukkit.Rotation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;

import java.util.List;
import java.util.Objects;

public class ItemFrameRenderer implements EntityRenderer {

    private static final Rotation[] ROTATIONS = Rotation.values();

    @Override
    public List<Entity> spawn(Location anchor, HeadContent content, RenderSettings settings) {
        var facing = HeadUtils.cardinalOf(settings.yaw());
        var item = ContentItems.itemOf(content);
        Class<? extends ItemFrame> type = content.optionBoolean("lit", false) ? GlowItemFrame.class : ItemFrame.class;

        ItemFrame frame = Objects.requireNonNull(anchor.getWorld()).spawn(anchor.getBlock().getLocation(), type);
        frame.setFacingDirection(facing, true);
        frame.setItem(item, false);
        frame.setItemDropChance(0);
        frame.setVisible(content.optionBoolean("visible", false));
        frame.setFixed(true);
        frame.setSilent(true);
        frame.setGlowing(settings.glow());

        return List.of(frame);
    }

    @Override
    public double height(HeadContent content, RenderSettings settings, List<Entity> entities) {
        return 1.0;
    }

    @Override
    public void spin(List<Entity> entities, float angle, RenderSettings settings, int periodTicks) {
        for (var entity : entities) {
            if (entity instanceof ItemFrame frame && frame.isValid()) {
                frame.setRotation(rotationOf(angle));
            }
        }
    }

    static Rotation rotationOf(float angle) {
        return ROTATIONS[Math.round(HeadUtils.normalizeYaw(angle) / 45f) % ROTATIONS.length];
    }
}
