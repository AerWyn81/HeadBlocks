package fr.aerwyn81.headblocks.visual.renderers;

import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.visual.ContentItems;
import fr.aerwyn81.headblocks.visual.EntityRenderer;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MobRenderer implements EntityRenderer {

    public static final Map<String, EquipmentSlot> EQUIPMENT = equipmentSlots();

    @Override
    public List<Entity> spawn(Location anchor, HeadContent content, RenderSettings settings) {
        var type = typeOf(content);

        var entity = Objects.requireNonNull(anchor.getWorld()).spawnEntity(anchor, type);
        freeze(entity, settings);
        dress(entity, content);
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

    public static void freeze(Entity entity, RenderSettings settings) {
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
    }

    static void dress(Entity entity, HeadContent content) {
        if (entity instanceof LivingEntity living) {
            var equipment = living.getEquipment();
            if (equipment != null) {
                EQUIPMENT.forEach((key, slot) -> {
                    var raw = content.option(key);
                    var item = raw == null ? null : ContentItems.equipmentOf(raw);
                    if (item != null) {
                        equipment.setItem(slot, item);
                    }
                });
            }

            if (content.optionBoolean("invisible", false)) {
                living.setInvisible(true);
            }
        }

        if (entity instanceof Ageable ageable && content.optionBoolean("baby", false)) {
            ageable.setBaby();
        }

        if (entity instanceof ArmorStand stand) {
            stand.setSmall(content.optionBoolean("small", false));
            stand.setArms(content.optionBoolean("arms", false));
            stand.setBasePlate(content.optionBoolean("baseplate", true));
        }
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

    private static Map<String, EquipmentSlot> equipmentSlots() {
        Map<String, EquipmentSlot> slots = new LinkedHashMap<>();
        slots.put("head", EquipmentSlot.HEAD);
        slots.put("chest", EquipmentSlot.CHEST);
        slots.put("legs", EquipmentSlot.LEGS);
        slots.put("feet", EquipmentSlot.FEET);
        slots.put("hand", EquipmentSlot.HAND);
        slots.put("offhand", EquipmentSlot.OFF_HAND);
        return slots;
    }
}
