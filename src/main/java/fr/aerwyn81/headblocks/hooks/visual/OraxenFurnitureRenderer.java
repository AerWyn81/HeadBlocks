package fr.aerwyn81.headblocks.hooks.visual;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import fr.aerwyn81.headblocks.visual.renderers.DisplayRenderer;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.Location;
import org.bukkit.Rotation;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

class OraxenFurnitureRenderer extends DisplayRenderer {

    private static final double DEFAULT_SIZE = 1.0;

    private final Function<HeadContent, ItemStack> items;
    private final Set<UUID> spawned = ConcurrentHashMap.newKeySet();

    OraxenFurnitureRenderer(Function<HeadContent, ItemStack> items) {
        this.items = items;
    }

    static boolean supports(String id) {
        if (!OraxenFurniture.isFurniture(id)) {
            return false;
        }

        var mechanic = OraxenFurniture.getFurnitureMechanic(id);
        return mechanic != null
                && mechanic.getFurnitureType() == FurnitureMechanic.FurnitureType.DISPLAY_ENTITY
                && mechanic.hasDisplayEntityProperties();
    }

    @Override
    public List<Entity> spawn(Location anchor, HeadContent content, RenderSettings settings) {
        var entities = super.spawn(anchor, content, settings);
        spawned.add(entities.get(0).getUniqueId());
        return entities;
    }

    boolean handles(List<Entity> entities) {
        return !entities.isEmpty() && spawned.contains(entities.get(0).getUniqueId());
    }

    void forget(List<Entity> entities) {
        if (!entities.isEmpty()) {
            spawned.remove(entities.get(0).getUniqueId());
        }
    }

    @Override
    protected Display spawnDisplay(Location anchor, HeadContent content, float scale) {
        var mechanic = mechanicOf(content);
        var properties = mechanic.getDisplayEntityProperties();
        var fixed = properties.getDisplayTransform() == ItemDisplay.ItemDisplayTransform.FIXED;

        var location = anchor.getBlock().getLocation().add(0.5, fixed ? 0 : 0.5, 0.5);
        var display = Objects.requireNonNull(anchor.getWorld()).spawn(location, ItemDisplay.class);
        display.setItemStack(items.apply(content));
        display.setItemDisplayTransform(properties.getDisplayTransform());
        if (properties.hasSpecifiedViewRange()) {
            display.setViewRange(properties.getViewRange());
        }
        if (properties.hasTrackingRotation()) {
            display.setBillboard(properties.getTrackingRotation());
        }
        if (properties.hasShadowRadius()) {
            display.setShadowRadius(properties.getShadowRadius());
        }
        if (properties.hasShadowStrength()) {
            display.setShadowStrength(properties.getShadowStrength());
        }
        if (properties.hasBrightness()) {
            display.setBrightness(properties.getBrightness());
        }
        display.setDisplayWidth(properties.getDisplayWidth());
        display.setDisplayHeight(properties.getDisplayHeight());

        var current = display.getTransformation();
        var modelScale = properties.hasScale() ? new Vector3f(properties.getScale()) : new Vector3f(fixed ? 0.5f : 1f);
        var translation = properties.hasTranslation() ? new Vector3f(properties.getTranslation()) : current.getTranslation();
        display.setTransformation(new Transformation(translation, current.getLeftRotation(), modelScale, current.getRightRotation()));

        var floor = mechanic.hasLimitedPlacing() && mechanic.getLimitedPlacing().isFloor();
        display.setRotation(yawOf(mechanic, anchor.getYaw()), floor && fixed ? -90f : 0f);
        return display;
    }

    @Override
    protected double size(float scale) {
        return DEFAULT_SIZE;
    }

    @Override
    protected double width(HeadContent content, float scale) {
        var width = mechanicOf(content).getDisplayEntityProperties().getDisplayWidth();
        return content.optionDouble("width", width > 0 ? width : DEFAULT_SIZE);
    }

    @Override
    protected double height(HeadContent content, float scale) {
        var height = mechanicOf(content).getDisplayEntityProperties().getDisplayHeight();
        return content.optionDouble("height", height > 0 ? height : DEFAULT_SIZE);
    }

    @Override
    public void spin(List<Entity> entities, float angle, RenderSettings settings, int periodTicks) {
    }

    static float yawOf(FurnitureMechanic mechanic, float yaw) {
        var index = (int) (HeadUtils.normalizeYaw(yaw) * 8 / 360 + 0.5) % 8;
        var restricted = mechanic.getRestrictedRotation();
        if (restricted != FurnitureMechanic.RestrictedRotation.NONE && index % 2 != 0) {
            index -= restricted == FurnitureMechanic.RestrictedRotation.STRICT ? 0 : 1;
        }
        return FurnitureMechanic.rotationToYaw(Rotation.values()[index]);
    }

    private static FurnitureMechanic mechanicOf(HeadContent content) {
        var mechanic = OraxenFurniture.getFurnitureMechanic(content.value());
        if (mechanic == null) {
            throw new IllegalStateException("unknown Oraxen furniture " + content.value());
        }
        return mechanic;
    }
}
