package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.data.head.visual.RenderMode;
import fr.aerwyn81.headblocks.data.head.visual.VisualForm;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntConfig;
import fr.aerwyn81.headblocks.hooks.VisualProviderHook;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.visual.RenderSettings;
import fr.aerwyn81.headblocks.visual.VisualRenderers;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class HeadVisualService {
    public static final String ENTITY_KEY = "hb_head";

    private static final float SPIN_STEP = 22.5f;
    private static final long RETRY_DELAY_MS = 30_000L;
    private static final int CONVERSIONS_PER_TICK = 20;

    private final ServiceRegistry registry;
    private final VisualRenderers renderers;
    private final Map<UUID, List<Entity>> spawned = new ConcurrentHashMap<>();
    private final Map<UUID, Float> spinAngles = new ConcurrentHashMap<>();
    private final Map<UUID, Long> retryAfter = new ConcurrentHashMap<>();
    private final Set<String> reportedFailures = ConcurrentHashMap.newKeySet();
    private NamespacedKey entityKey;

    public HeadVisualService(ServiceRegistry registry, Map<String, VisualProviderHook> providers) {
        this.registry = registry;
        this.renderers = new VisualRenderers(providers);
    }

    public Map<String, VisualProviderHook> getProviders() {
        return renderers.providers();
    }

    public VisualProviderHook getProvider(String prefix) {
        return renderers.provider(prefix);
    }

    public HuntConfig configOf(HeadLocation head) {
        return registry.getHuntService().configOf(head.getHuntId());
    }

    public RenderMode renderModeOf(HeadLocation head) {
        return head.getRenderMode() != null ? head.getRenderMode() : RenderMode.BLOCK;
    }

    public VisualForm formOf(HeadLocation head) {
        return VisualForm.resolve(head.getContent(), renderModeOf(head));
    }

    public VisualForm formOf(HeadContent content, String huntId) {
        return VisualForm.resolve(content, registry.getHuntService().configOf(huntId).getRenderMode());
    }

    public boolean isEntityRendered(HeadLocation head) {
        return formOf(head).isEntityBased();
    }

    public boolean isBlockRendered(HeadLocation head) {
        return formOf(head).isBlockBased();
    }

    public HeadLocation headOf(Entity entity) {
        if (entity == null) {
            return null;
        }

        var raw = entity.getPersistentDataContainer().get(entityKey(), PersistentDataType.STRING);
        if (raw == null) {
            return null;
        }

        try {
            return registry.getHeadService().getHeadByUUID(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isHeadEntity(Entity entity) {
        return entity != null && entity.getPersistentDataContainer().has(entityKey(), PersistentDataType.STRING);
    }

    public List<Entity> entitiesOf(HeadLocation head) {
        return spawned.getOrDefault(head.getUuid(), List.of());
    }

    public void forEachSpawned(BiConsumer<HeadLocation, List<Entity>> action) {
        for (var entry : spawned.entrySet()) {
            var head = registry.getHeadService().getHeadByUUID(entry.getKey());
            if (head != null) {
                action.accept(head, entry.getValue());
            }
        }
    }

    public HeadLocation lookedAtHead(Player player, int distance) {
        var eye = player.getEyeLocation();
        var result = player.getWorld().rayTrace(eye, eye.getDirection(), distance, FluidCollisionMode.NEVER, true, 0.1,
                entity -> entity != player && isHeadEntity(entity));

        return result == null ? null : headOf(result.getHitEntity());
    }

    public void ensureSpawned(HeadLocation head) {
        if (!isEntityRendered(head)) {
            return;
        }

        var location = head.getLocation();
        if (location == null || location.getWorld() == null
                || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            spawned.remove(head.getUuid());
            return;
        }

        var renderer = renderers.entity(formOf(head), head.getContent());
        var current = spawned.get(head.getUuid());
        if (current != null && current.stream().allMatch(Entity::isValid)) {
            if (renderer != null && renderer.anchored()) {
                keepInPlace(head, current);
            }
            return;
        }

        var retryAt = retryAfter.get(head.getUuid());
        if (retryAt != null && System.currentTimeMillis() < retryAt) {
            return;
        }

        if (current != null) {
            current.forEach(Entity::remove);
        }

        var content = head.getContent() != null ? head.getContent() : ensureContent(head);
        if (renderer == null || content == null) {
            reportFailure(head, renderer == null ? "its plugin is not available" : "its content is unknown");
            return;
        }

        List<Entity> created;
        try {
            created = new ArrayList<>(renderer.spawn(anchorOf(head), content, settingsOf(head)));
        } catch (Exception ex) {
            reportFailure(head, ex.getMessage());
            return;
        }

        created.removeIf(entity -> entity == null || !entity.isValid());
        if (created.isEmpty()) {
            reportFailure(head, "the entity could not be spawned, is spawning blocked by another plugin?");
            return;
        }

        created.forEach(entity -> tag(entity, head));
        retryAfter.remove(head.getUuid());
        spawned.put(head.getUuid(), List.copyOf(created));

        registry.getVisibilityService().onSpawned(head, created);

        var form = formOf(head);
        if (form == VisualForm.MOB || form == VisualForm.EXTERNAL) {
            refreshHolograms(head);
        }
    }

    public void despawn(HeadLocation head) {
        var entities = spawned.remove(head.getUuid());
        spinAngles.remove(head.getUuid());
        retryAfter.remove(head.getUuid());
        if (entities == null) {
            return;
        }

        var renderer = renderers.entity(formOf(head), head.getContent());
        registry.getScheduler().runNow(entities.get(0), () -> {
            if (renderer != null) {
                renderer.despawn(entities);
            } else {
                entities.forEach(Entity::remove);
            }
        });
    }

    public void clear(HeadLocation head) {
        despawn(head);

        if (isBlockRendered(head)) {
            var location = head.getLocation();
            if (location != null && location.getWorld() != null) {
                location.getBlock().setType(Material.AIR);
            }
        }
    }

    public void respawn(HeadLocation head) {
        despawn(head);
        var location = head.getLocation();
        if (location != null) {
            registry.getScheduler().runNow(location, () -> ensureSpawned(head));
        }
    }

    public void spawnLoaded() {
        for (var head : registry.getHeadService().getHeadLocations()) {
            var location = head.getLocation();
            if (location != null && isEntityRendered(head)) {
                registry.getScheduler().runNow(location, () -> ensureSpawned(head));
            }
        }
    }

    public void onChunkLoad(Chunk chunk) {
        for (var head : registry.getHeadService().getHeadsInChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ())) {
            var location = head.getLocation();
            if (location != null && isEntityRendered(head)) {
                registry.getScheduler().runTask(location, () -> ensureSpawned(head));
            }
        }
    }

    public void tick() {
        if (HeadBlocks.isReloadInProgress) {
            return;
        }

        var toCheck = new HashSet<>(spawned.keySet());
        var now = System.currentTimeMillis();
        retryAfter.forEach((uuid, retryAt) -> {
            if (now >= retryAt) {
                toCheck.add(uuid);
            }
        });

        for (var uuid : toCheck) {
            var head = registry.getHeadService().getHeadByUUID(uuid);
            if (head == null || head.getLocation() == null) {
                discard(uuid);
                continue;
            }

            registry.getScheduler().runNow(head.getLocation(), () -> ensureSpawned(head));
        }
    }

    public void removeOrphan(Entity entity) {
        if (!isHeadEntity(entity)) {
            return;
        }

        var head = headOf(entity);
        if (head == null || !entitiesOf(head).contains(entity)) {
            entity.remove();
        }
    }

    public void despawnAll() {
        for (var entities : spawned.values()) {
            registry.getScheduler().runNow(entities.get(0), () -> entities.forEach(Entity::remove));
        }

        spawned.clear();
        spinAngles.clear();
        retryAfter.clear();
        reportedFailures.clear();
    }

    public void shutdown() {
        for (var entities : spawned.values()) {
            for (var entity : entities) {
                try {
                    entity.remove();
                } catch (Exception ignored) {
                }
            }
        }

        spawned.clear();
        spinAngles.clear();
        retryAfter.clear();
    }

    public void spin(HeadLocation head, int periodTicks) {
        var entities = spawned.get(head.getUuid());
        var renderer = renderers.entity(formOf(head), head.getContent());
        if (entities == null || renderer == null) {
            return;
        }

        var angle = HeadUtils.normalizeYaw(spinAngles.getOrDefault(head.getUuid(), 0f) + SPIN_STEP);
        spinAngles.put(head.getUuid(), angle);

        renderer.spin(entities, angle, settingsOf(head), periodTicks);
    }

    public double visualHeight(HeadLocation head) {
        var form = formOf(head);
        if (form.isBlockBased()) {
            return renderers.block(form).height();
        }

        var renderer = renderers.entity(form, head.getContent());
        return renderer == null ? 1.0 : renderer.height(head.getContent(), settingsOf(head), entitiesOf(head));
    }

    public ItemStack iconOf(HeadContent content) {
        return renderers.icon(content);
    }

    public HeadContent ensureContent(HeadLocation head) {
        if (head.getContent() != null) {
            return head.getContent();
        }

        String texture = "";
        var location = head.getLocation();
        if (location != null && location.getWorld() != null) {
            var block = location.getBlock();
            if (HeadUtils.isPlayerHead(block)) {
                texture = HeadUtils.getHeadTexture(block);
                head.setYaw(HeadUtils.yawOf(block));
            }
        }

        if (texture == null || texture.isEmpty()) {
            try {
                texture = registry.getStorageService().getHeadTexture(head.getUuid());
            } catch (InternalException ex) {
                texture = "";
            }
        }

        if (texture == null || texture.isEmpty()) {
            return null;
        }

        var content = HeadContent.head(texture);
        if (location != null && location.getWorld() != null && location.getBlock().getType() == Material.PLAYER_WALL_HEAD) {
            content = HeadContent.withWall(content);
        }

        head.setContent(content);
        registry.getHeadService().saveHeadInConfig(head);
        return head.getContent();
    }

    public boolean placeBlock(HeadLocation head) {
        var location = head.getLocation();
        var content = ensureContent(head);
        var renderer = renderers.block(VisualForm.resolve(content, RenderMode.BLOCK));
        if (location == null || location.getWorld() == null || content == null || renderer == null) {
            return false;
        }

        var block = location.getBlock();
        if (!block.isEmpty() && !renderer.matches(block, content)) {
            return false;
        }

        return renderer.place(block, content, head.getYaw());
    }

    public void removeBlock(HeadLocation head) {
        var location = head.getLocation();
        if (location == null || location.getWorld() == null) {
            return;
        }

        var block = location.getBlock();
        var content = head.getContent();
        var renderer = renderers.block(VisualForm.resolve(content, RenderMode.BLOCK));
        if (HeadUtils.isPlayerHead(block) || (renderer != null && content != null && renderer.matches(block, content))) {
            block.setType(Material.AIR);
        }
    }

    public void convertHunt(HBHunt hunt, RenderMode newMode, Consumer<ConversionReport> onComplete) {
        var heads = registry.getHeadService().getHeadLocationsForHunt(hunt);
        var report = new ConversionReport();

        hunt.getConfig().setRenderMode(newMode);
        registry.getHuntConfigService().saveHunt(hunt);

        if (heads.isEmpty()) {
            onComplete.accept(report);
            return;
        }

        var remaining = new AtomicInteger(heads.size());
        Runnable finishOne = () -> {
            if (remaining.decrementAndGet() == 0) {
                registry.getScheduler().runTask(() -> onComplete.accept(report));
            }
        };

        for (int i = 0; i < heads.size(); i++) {
            var head = heads.get(i);
            var location = head.getLocation();
            if (location == null || location.getWorld() == null) {
                report.skipped.incrementAndGet();
                finishOne.run();
                continue;
            }

            registry.getScheduler().runTaskLater(location, () -> {
                try {
                    convertHead(head, newMode, report);
                } finally {
                    finishOne.run();
                }
            }, i / CONVERSIONS_PER_TICK);
        }
    }

    public void rerender(HeadLocation head) {
        var targetMode = configOf(head).getRenderMode();
        if (formOf(head) == VisualForm.resolve(head.getContent(), targetMode)) {
            return;
        }

        var location = head.getLocation();
        if (location == null || location.getWorld() == null) {
            return;
        }

        registry.getScheduler().runTask(location, () -> convertHead(head, targetMode, new ConversionReport()));
    }

    private void convertHead(HeadLocation head, RenderMode newMode, ConversionReport report) {
        var previousForm = formOf(head);
        var targetForm = VisualForm.resolve(head.getContent(), newMode);

        if (previousForm == targetForm) {
            if (head.getRenderMode() != newMode) {
                head.setRenderMode(newMode);
                registry.getHeadService().saveHeadInConfig(head);
            }
            report.unchanged.incrementAndGet();
            return;
        }

        if (ensureContent(head) == null) {
            report.failed.incrementAndGet();
            return;
        }

        if (targetForm.isBlockBased()) {
            if (!placeBlock(head)) {
                report.failed.incrementAndGet();
                return;
            }

            despawn(head);
            head.setRenderMode(newMode);
        } else {
            removeBlock(head);
            despawn(head);
            head.setRenderMode(newMode);
            ensureSpawned(head);
        }

        registry.getHeadService().saveHeadInConfig(head);
        refreshHolograms(head);
        report.converted.incrementAndGet();
    }

    private void refreshHolograms(HeadLocation head) {
        var hologramService = registry.getHologramService();
        if (hologramService == null || !hologramService.isEnabled()) {
            return;
        }

        hologramService.removeHolograms(head.getLocation());
        hologramService.createHolograms(head.getLocation(), configOf(head));
    }

    private RenderSettings settingsOf(HeadLocation head) {
        var config = configOf(head);
        return new RenderSettings(config.getRenderScale(), config.isRenderGlow(), head.getYaw());
    }

    private Location anchorOf(HeadLocation head) {
        var anchor = head.getLocation().clone();
        anchor.setYaw(head.getYaw());
        anchor.setPitch(0);
        return anchor;
    }

    private void keepInPlace(HeadLocation head, List<Entity> entities) {
        var anchor = head.getLocation();
        for (var entity : entities) {
            if (entity.getWorld() != anchor.getWorld() || entity.getLocation().distanceSquared(anchor) > 0.25) {
                var target = anchor.clone();
                target.setYaw(entity.getLocation().getYaw());
                registry.getPlatform().teleportAsync(entity, target);
            }
        }
    }

    private void discard(UUID headUuid) {
        retryAfter.remove(headUuid);
        spinAngles.remove(headUuid);
        var entities = spawned.remove(headUuid);
        if (entities != null) {
            registry.getScheduler().runNow(entities.get(0), () -> entities.forEach(Entity::remove));
        }
    }

    private void tag(Entity entity, HeadLocation head) {
        entity.setPersistent(false);
        entity.getPersistentDataContainer().set(entityKey(), PersistentDataType.STRING, head.getUuid().toString());
    }

    private NamespacedKey entityKey() {
        if (entityKey == null) {
            entityKey = new NamespacedKey(HeadBlocks.getInstance(), ENTITY_KEY);
        }
        return entityKey;
    }

    private void reportFailure(HeadLocation head, String reason) {
        spawned.remove(head.getUuid());
        retryAfter.put(head.getUuid(), System.currentTimeMillis() + RETRY_DELAY_MS);

        if (reportedFailures.add(head.getUuid() + reason)) {
            LogUtil.error("Cannot render head {0}: {1}", head.getNameOrUuid(), reason);
        }
    }

    public static final class ConversionReport {
        private final AtomicInteger converted = new AtomicInteger();
        private final AtomicInteger unchanged = new AtomicInteger();
        private final AtomicInteger failed = new AtomicInteger();
        private final AtomicInteger skipped = new AtomicInteger();

        public int converted() {
            return converted.get();
        }

        public int unchanged() {
            return unchanged.get();
        }

        public int failed() {
            return failed.get();
        }

        public int skipped() {
            return skipped.get();
        }
    }
}
