package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.api.events.HuntStateChangeEvent;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.head.visual.RenderMode;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.data.hunt.behavior.SpawnBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.SpawnTemplate;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.utils.scheduler.Task;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class SpawnService {
    private static final long FLUSH_PERIOD_TICKS = 20L;
    private static final int RETRY_SECONDS = 30;
    private static final long BLOCKED_POINT_MILLIS = 60_000L;

    private final ServiceRegistry registry;
    private final File folder;
    private final Map<String, SpawnState> states = new ConcurrentHashMap<>();
    private final Set<String> dirty = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> writtenVersions = new HashMap<>();
    private final AtomicBoolean writing = new AtomicBoolean();
    private final AtomicLong versions = new AtomicLong();
    private Task flushTask;

    public SpawnService(ServiceRegistry registry, File dataFolder) {
        this.registry = registry;
        this.folder = new File(dataFolder, "spawns");
    }

    // --- Lifecycle ---

    public void start() {
        if (!folder.exists() && !folder.mkdirs()) {
            LogUtil.error("Cannot create the spawns folder, spawned heads will not be persisted.");
        }

        for (HBHunt hunt : registry.getHuntService().getAllHunts()) {
            var behavior = behaviorOf(hunt);
            if (behavior == null) {
                continue;
            }

            var state = restore(hunt, behavior);

            if (hunt.isActive()) {
                resume(hunt, behavior, state);
            }
        }

        cleanupOrphanFiles();
        purgeOrphansAsync();
        flushTask = registry.getScheduler().runTaskTimer(this::flush, FLUSH_PERIOD_TICKS, FLUSH_PERIOD_TICKS);
    }

    public void stop() {
        if (flushTask != null) {
            flushTask.cancel();
            flushTask = null;
        }

        for (SpawnState state : states.values()) {
            state.closed = true;
            state.cancelTasks();
            write(state.huntId, snapshot(state), state.version.get());
        }

        states.clear();
        dirty.clear();
    }

    public void onStateChanged(HBHunt hunt) {
        reconfigure(hunt);
    }

    public void reconfigure(HBHunt hunt) {
        var behavior = behaviorOf(hunt);
        if (behavior == null) {
            return;
        }

        var state = states.computeIfAbsent(hunt.getId(), SpawnState::new);
        state.cancelTasks();

        if (hunt.isActive()) {
            resume(hunt, behavior, state);
        } else {
            clearHeads(state);
            state.pending.clear();
            state.nextIntervalAt.set(0);
            markDirty(state);
        }
    }

    public void onWorldLoaded(World world) {
        for (SpawnState state : states.values()) {
            var hunt = registry.getHuntService().getHuntById(state.huntId);
            var behavior = hunt == null ? null : behaviorOf(hunt);
            if (behavior == null || !hunt.isActive()) {
                continue;
            }

            for (var dormant : new ArrayList<>(state.dormant.values())) {
                if (!dormant.world().equals(world.getName())) {
                    continue;
                }

                state.dormant.remove(dormant.uuid());
                var template = behavior.template(dormant.templateId());
                var location = new Location(world, dormant.x(), dormant.y(), dormant.z());
                if (template == null || !activate(state, withRender(buildHead(state.huntId, dormant.uuid(), template, location, dormant.yaw()), dormant.render()), template)) {
                    deleteRowsAsync(List.of(dormant.uuid()));
                }
            }
            markDirty(state);
        }
    }

    // --- Claim flow ---

    public boolean consume(HBHunt hunt, HeadLocation head) {
        var state = states.get(hunt.getId());
        if (state == null || state.active.remove(head.getUuid()) == null) {
            return false;
        }

        state.reserved.add(head.getUuid());
        registry.getHeadService().removeSpawnedHead(head);
        markDirty(state);

        var behavior = behaviorOf(hunt);
        if (behavior != null && behavior.respawn().onFind()) {
            scheduleRespawn(state, behavior.respawn().nextFindDelay(), head.getLocation());
        }
        return true;
    }

    public void release(HBHunt hunt, HeadLocation head) {
        var state = states.get(hunt.getId());
        if (state != null) {
            state.reserved.remove(head.getUuid());
        }
    }

    public void onLost(HeadLocation head) {
        for (SpawnState state : states.values()) {
            if (state.active.remove(head.getUuid()) != null) {
                state.blockedUntil.put(blockKey(head.getLocation()), System.currentTimeMillis() + BLOCKED_POINT_MILLIS);
                deleteRowsAsync(List.of(head.getUuid()));
                markDirty(state);
                scheduleRespawn(state, RETRY_SECONDS, null);
                return;
            }
        }
    }

    public void onDiscarded(HeadLocation head) {
        for (SpawnState state : states.values()) {
            if (state.active.remove(head.getUuid()) == null) {
                continue;
            }

            deleteRowsAsync(List.of(head.getUuid()));
            markDirty(state);

            var hunt = registry.getHuntService().getHuntById(state.huntId);
            var behavior = hunt == null ? null : behaviorOf(hunt);
            if (behavior != null && behavior.respawn().onFind()) {
                scheduleRespawn(state, behavior.respawn().nextFindDelay(), head.getLocation());
            }
            return;
        }
    }

    public void deleteHunt(String huntId) {
        var state = states.remove(huntId);
        if (state != null) {
            state.closed = true;
            state.cancelTasks();
            clearHeads(state);
        }

        dirty.remove(huntId);
        synchronized (this) {
            writtenVersions.put(huntId, versions.incrementAndGet());
            try {
                Files.deleteIfExists(fileOf(huntId).toPath());
            } catch (Exception e) {
                LogUtil.error("Cannot delete the spawned heads file of hunt {0}: {1}", huntId, e.getMessage());
            }
        }
    }

    public void win(HBHunt hunt, Player player) {
        var event = new HuntStateChangeEvent(hunt, hunt.getState(), HuntState.INACTIVE);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        var message = registry.getLanguageService().message("Messages.SpawnHuntWon")
                .replace("%player%", player.getName())
                .replace("%hunt%", hunt.getDisplayName());
        if (!message.trim().isEmpty()) {
            Bukkit.broadcastMessage(message);
        }

        try {
            registry.getHuntService().changeState(hunt, HuntState.INACTIVE);
        } catch (InternalException e) {
            LogUtil.error("Cannot close hunt {0} after {1} won it: {2}", hunt.getId(), player.getName(), e.getMessage());
        }
    }

    // --- Commands ---

    public void reroll(HBHunt hunt, boolean resetProgress) {
        var behavior = behaviorOf(hunt);
        if (behavior == null) {
            return;
        }

        var state = states.computeIfAbsent(hunt.getId(), SpawnState::new);
        state.cancelRespawns();
        state.pending.clear();
        clearHeads(state);
        markDirty(state);

        if (resetProgress) {
            resetProgressAsync(hunt.getId(), () -> {
                if (!state.closed) {
                    fill(state, behavior);
                }
            });
            return;
        }

        fill(state, behavior);
    }

    public void refresh(HBHunt hunt) {
        var behavior = behaviorOf(hunt);
        if (behavior == null) {
            return;
        }

        boolean known = states.containsKey(hunt.getId());
        var state = states.computeIfAbsent(hunt.getId(), SpawnState::new);
        if (!hunt.isActive()) {
            return;
        }

        if (!known) {
            resume(hunt, behavior, state);
        } else if (behavior.respawn().onStart()) {
            state.cancelRespawns();
            state.pending.clear();
            fill(state, behavior);
        }
    }

    public void clear(HBHunt hunt) {
        var state = states.get(hunt.getId());
        if (state == null) {
            return;
        }

        state.cancelRespawns();
        state.pending.clear();
        clearHeads(state);
        markDirty(state);
    }

    public Collection<HeadLocation> getActiveHeads(String huntId) {
        var state = states.get(huntId);
        return state == null ? List.of() : state.active.values().stream().map(ActiveSpawn::head).toList();
    }

    // --- Spawning ---

    private void resume(HBHunt hunt, SpawnBehavior behavior, SpawnState state) {
        long now = System.currentTimeMillis();
        for (Long due : List.copyOf(state.pending)) {
            state.pending.remove(due);
            scheduleRespawn(state, (int) Math.max(0, (due - now + 999) / 1000), null);
        }

        if (behavior.respawn().interval()) {
            long period = behavior.respawn().intervalSeconds() * 1000L;
            long next = state.nextIntervalAt.get();
            if (next <= now) {
                next = now + period;
                state.nextIntervalAt.set(next);
            }

            state.intervalTask = registry.getScheduler().runTaskTimer(() -> {
                state.nextIntervalAt.set(System.currentTimeMillis() + period);
                var current = registry.getHuntService().getHuntById(state.huntId);
                var currentBehavior = current == null ? null : behaviorOf(current);
                if (currentBehavior != null && current.isActive()) {
                    reroll(current, currentBehavior.respawn().resetProgress());
                }
            }, Math.max(1L, (next - now) / 50L), period / 50L);
        }

        refillPoolAsync(state, behavior.active());

        if (behavior.respawn().onStart()) {
            fill(state, behavior);
        }
        markDirty(state);
    }

    private void fill(SpawnState state, SpawnBehavior behavior) {
        int missing = behavior.active() - state.active.size() - state.dormant.size()
                - state.inflight.get() - state.pending.size();
        for (int i = 0; i < missing; i++) {
            spawnOne(state, null);
        }
    }

    private void scheduleRespawn(SpawnState state, int delaySeconds, Location avoid) {
        if (delaySeconds <= 0) {
            spawnOne(state, avoid);
            return;
        }

        long due = System.currentTimeMillis() + delaySeconds * 1000L;
        state.pending.add(due);
        markDirty(state);

        state.respawnTasks.add(registry.getScheduler().runTaskLater(() -> {
            state.pending.remove(due);
            markDirty(state);
            spawnOne(state, avoid);
        }, delaySeconds * 20L));
    }

    private void spawnOne(SpawnState state, Location avoid) {
        if (state.closed) {
            return;
        }

        var hunt = registry.getHuntService().getHuntById(state.huntId);
        var behavior = hunt == null ? null : behaviorOf(hunt);
        if (behavior == null || !hunt.isActive()) {
            return;
        }

        if (behavior.maxTotalSpawns() >= 0
                && state.totalSpawned.get() + state.inflight.get() >= behavior.maxTotalSpawns()) {
            return;
        }

        var template = behavior.pickTemplate();
        if (template == null) {
            LogUtil.warning("Hunt {0} has no spawn template with a positive weight, no head can appear.", state.huntId);
            return;
        }

        state.inflight.incrementAndGet();
        Consumer<Location> onPicked = location -> {
            if (location == null) {
                state.inflight.decrementAndGet();
                scheduleRespawn(state, RETRY_SECONDS, null);
                return;
            }

            acquireUuid(state, behavior, uuid -> {
                state.inflight.decrementAndGet();
                if (uuid == null || state.closed) {
                    return;
                }

                var current = registry.getHuntService().getHuntById(state.huntId);
                if (current == null || !current.isActive()
                        || !activate(state, buildHead(state.huntId, uuid, template, location, behavior.yawAt(location)), template)) {
                    state.pool.add(uuid);
                    markDirty(state);
                    return;
                }

                state.totalSpawned.incrementAndGet();
            });
        };

        behavior.pickLocation(location -> isFree(state, location, avoid), location -> {
            if (location == null && avoid != null) {
                behavior.pickLocation(candidate -> isFree(state, candidate, null), onPicked);
            } else {
                onPicked.accept(location);
            }
        });
    }

    private boolean activate(SpawnState state, HeadLocation head, SpawnTemplate template) {
        state.active.put(head.getUuid(), new ActiveSpawn(head, template.id()));
        markDirty(state);

        if (!registry.getHeadService().addSpawnedHead(head)) {
            state.active.remove(head.getUuid());
            return false;
        }
        return true;
    }

    private boolean isFree(SpawnState state, Location location, Location avoid) {
        if (registry.getHeadService().getHeadAt(location) != null) {
            return false;
        }

        var blocked = state.blockedUntil.get(blockKey(location));
        if (blocked != null) {
            if (blocked > System.currentTimeMillis()) {
                return false;
            }
            state.blockedUntil.remove(blockKey(location));
        }

        return avoid == null || avoid.getWorld() != location.getWorld()
                || avoid.getBlockX() != location.getBlockX()
                || avoid.getBlockY() != location.getBlockY()
                || avoid.getBlockZ() != location.getBlockZ();
    }

    private HeadLocation buildHead(String huntId, UUID uuid, SpawnTemplate template, Location location, float yaw) {
        var head = new HeadLocation(template.name(), uuid, location, huntId);
        head.setContent(template.content());
        head.setYaw(yaw);
        head.setRenderMode(registry.getHuntService().configOf(huntId).getRenderMode());
        template.rewards().forEach(head::addReward);
        return head;
    }

    private static HeadLocation withRender(HeadLocation head, RenderMode render) {
        if (render != null) {
            head.setRenderMode(render);
        }
        return head;
    }

    private void clearHeads(SpawnState state) {
        var removed = new ArrayList<UUID>();
        for (var spawn : List.copyOf(state.active.values())) {
            if (state.active.remove(spawn.head().getUuid()) != null) {
                registry.getHeadService().removeSpawnedHead(spawn.head());
                removed.add(spawn.head().getUuid());
            }
        }

        removed.addAll(state.dormant.keySet());
        state.dormant.clear();
        deleteRowsAsync(removed);
    }

    // --- Database rows ---

    private void acquireUuid(SpawnState state, SpawnBehavior behavior, Consumer<UUID> consumer) {
        var pooled = state.pool.poll();
        if (pooled != null) {
            markDirty(state);
            consumer.accept(pooled);
            refillPoolAsync(state, behavior.active());
            return;
        }

        registry.getScheduler().runTaskAsync(() -> {
            var uuid = createRow(state);
            registry.getScheduler().runTask(() -> consumer.accept(uuid));
        });
    }

    private void refillPoolAsync(SpawnState state, int target) {
        if (!state.refilling.compareAndSet(false, true)) {
            return;
        }

        registry.getScheduler().runTaskAsync(() -> {
            try {
                while (state.pool.size() < target) {
                    var uuid = createRow(state);
                    if (uuid == null) {
                        return;
                    }
                    state.pool.add(uuid);
                    state.reserved.remove(uuid);
                }
            } finally {
                state.refilling.set(false);
                markDirty(state);
            }
        });
    }

    private UUID createRow(SpawnState state) {
        var uuid = UUID.randomUUID();
        state.reserved.add(uuid);
        try {
            registry.getStorageService().createSpawnHead(uuid, "");
            return uuid;
        } catch (InternalException e) {
            state.reserved.remove(uuid);
            LogUtil.error("Cannot prepare a spawned head for hunt {0}: {1}", state.huntId, e.getMessage());
            return null;
        }
    }

    private void deleteRowsAsync(Collection<UUID> uuids) {
        if (uuids.isEmpty()) {
            return;
        }

        var toDelete = List.copyOf(uuids);
        registry.getScheduler().runTaskAsync(() -> {
            try {
                registry.getStorageService().deleteSpawnHeads(toDelete);
            } catch (InternalException e) {
                LogUtil.error("Cannot delete {0} spawned head(s): {1}", toDelete.size(), e.getMessage());
            }
        });
    }

    private void purgeOrphansAsync() {
        registry.getScheduler().runTaskAsync(() -> {
            try {
                int purged = registry.getStorageService().purgeOrphanSpawnHeads(this::keptRows);
                if (purged > 0) {
                    LogUtil.info("Cleaned {0} unused spawned head(s) from the database.", purged);
                }
            } catch (InternalException e) {
                LogUtil.error("Cannot clean unused spawned heads: {0}", e.getMessage());
            }
        });
    }

    private Set<UUID> keptRows() {
        var keep = new HashSet<UUID>();
        for (SpawnState state : states.values()) {
            keep.addAll(state.active.keySet());
            keep.addAll(state.dormant.keySet());
            keep.addAll(state.pool);
            keep.addAll(state.reserved);
        }
        return keep;
    }

    private void resetProgressAsync(String huntId, Runnable then) {
        var online = Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).toList();
        registry.getScheduler().runTaskAsync(() -> {
            try {
                registry.getStorageService().deletePlayerProgressForHunt(huntId);
                online.forEach(registry.getStorageService()::invalidateCachePlayer);
            } catch (InternalException e) {
                LogUtil.error("Cannot reset the progress of hunt {0}: {1}", huntId, e.getMessage());
            }
            registry.getScheduler().runTask(then);
            purgeOrphansAsync();
        });
    }

    // --- Persistence ---

    private SpawnState restore(HBHunt hunt, SpawnBehavior behavior) {
        var state = new SpawnState(hunt.getId());
        states.put(hunt.getId(), state);
        var file = fileOf(hunt.getId());
        if (!file.exists()) {
            return state;
        }

        var yaml = YamlConfiguration.loadConfiguration(file);
        state.totalSpawned.set(yaml.getInt("totalSpawned", 0));
        state.nextIntervalAt.set(yaml.getLong("nextIntervalAt", 0));
        yaml.getStringList("pool").forEach(raw -> parseUuid(raw).ifPresent(state.pool::add));
        yaml.getLongList("pending").forEach(state.pending::add);

        var discarded = new ArrayList<UUID>();
        var activeSection = yaml.getConfigurationSection("active");
        if (activeSection != null) {
            for (String key : activeSection.getKeys(false)) {
                var uuid = parseUuid(key).orElse(null);
                var entry = activeSection.getConfigurationSection(key);
                if (uuid == null || entry == null) {
                    continue;
                }

                var dormant = new DormantSpawn(uuid, entry.getString("template", ""), entry.getString("world", ""),
                        entry.getDouble("x"), entry.getDouble("y"), entry.getDouble("z"), (float) entry.getDouble("yaw"),
                        RenderMode.of(entry.getString("render")));
                var world = Bukkit.getWorld(dormant.world());
                var template = behavior.template(dormant.templateId());

                if (world == null && hunt.isActive() && template != null) {
                    state.dormant.put(uuid, dormant);
                    continue;
                }

                var location = world == null ? null : new Location(world, dormant.x(), dormant.y(), dormant.z());
                if (location == null || template == null || !hunt.isActive()) {
                    if (location != null) {
                        removeLeftover(state.huntId, uuid, location, template, dormant.render());
                    }
                    discarded.add(uuid);
                    continue;
                }

                if (!activate(state, withRender(buildHead(hunt.getId(), uuid, template, location, dormant.yaw()), dormant.render()), template)) {
                    discarded.add(uuid);
                }
            }
        }

        deleteRowsAsync(discarded);
        return state;
    }

    private void cleanupOrphanFiles() {
        var files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            var huntId = file.getName().substring(0, file.getName().length() - ".yml".length());
            if (states.containsKey(huntId)) {
                continue;
            }

            var active = YamlConfiguration.loadConfiguration(file).getConfigurationSection("active");
            if (active != null) {
                for (String key : active.getKeys(false)) {
                    var entry = active.getConfigurationSection(key);
                    var uuid = parseUuid(key).orElse(null);
                    var world = entry == null ? null : Bukkit.getWorld(entry.getString("world", ""));
                    if (uuid != null && world != null) {
                        removeLeftover(huntId, uuid, new Location(world, entry.getDouble("x"), entry.getDouble("y"), entry.getDouble("z")),
                                null, RenderMode.of(entry.getString("render")));
                    }
                }
            }

            synchronized (this) {
                writtenVersions.put(huntId, versions.incrementAndGet());
                try {
                    Files.deleteIfExists(file.toPath());
                } catch (Exception e) {
                    LogUtil.error("Cannot delete the spawned heads file {0}: {1}", file.getName(), e.getMessage());
                }
            }
            LogUtil.info("Removed the spawned heads of {0}, the hunt no longer uses them.", huntId);
        }
    }

    private void removeLeftover(String huntId, UUID uuid, Location location, SpawnTemplate template, RenderMode render) {
        var head = new HeadLocation("", uuid, location, huntId);
        if (template != null) {
            head.setContent(template.content());
        }
        head.setRenderMode(render != null ? render : registry.getHuntService().configOf(huntId).getRenderMode());

        if (registry.getVisualService().isBlockRendered(head)) {
            registry.getScheduler().runNow(location, () -> registry.getVisualService().removeBlock(head));
        }
    }

    private void markDirty(SpawnState state) {
        state.version.set(versions.incrementAndGet());
        dirty.add(state.huntId);
    }

    private void flush() {
        if (dirty.isEmpty() || !writing.compareAndSet(false, true)) {
            return;
        }

        var snapshots = new ArrayList<Snapshot>();
        for (String huntId : List.copyOf(dirty)) {
            dirty.remove(huntId);
            var state = states.get(huntId);
            if (state != null) {
                snapshots.add(new Snapshot(huntId, snapshot(state), state.version.get()));
            }
        }

        registry.getScheduler().runTaskAsync(() -> {
            try {
                snapshots.forEach(s -> write(s.huntId(), s.content(), s.version()));
            } finally {
                writing.set(false);
            }
        });
    }

    private String snapshot(SpawnState state) {
        var yaml = new YamlConfiguration();
        yaml.set("totalSpawned", state.totalSpawned.get());
        yaml.set("nextIntervalAt", state.nextIntervalAt.get());
        yaml.set("pool", state.pool.stream().map(UUID::toString).toList());
        yaml.set("pending", List.copyOf(state.pending));

        var activeSection = yaml.createSection("active");
        for (var spawn : state.active.values()) {
            var location = spawn.head().getLocation();
            var entry = activeSection.createSection(spawn.head().getUuid().toString());
            entry.set("template", spawn.templateId());
            entry.set("world", location.getWorld() == null ? "" : location.getWorld().getName());
            entry.set("x", location.getX());
            entry.set("y", location.getY());
            entry.set("z", location.getZ());
            entry.set("yaw", (double) spawn.head().getYaw());
            entry.set("render", spawn.head().getRenderMode() == null ? null : spawn.head().getRenderMode().name());
        }

        for (var dormant : state.dormant.values()) {
            var entry = activeSection.createSection(dormant.uuid().toString());
            entry.set("template", dormant.templateId());
            entry.set("world", dormant.world());
            entry.set("x", dormant.x());
            entry.set("y", dormant.y());
            entry.set("z", dormant.z());
            entry.set("yaw", (double) dormant.yaw());
            entry.set("render", dormant.render() == null ? null : dormant.render().name());
        }

        return yaml.saveToString();
    }

    private synchronized void write(String huntId, String content, long version) {
        if (writtenVersions.getOrDefault(huntId, -1L) >= version) {
            return;
        }

        var target = fileOf(huntId).toPath();
        try {
            var temp = Files.createTempFile(folder.toPath(), huntId, ".tmp");
            Files.writeString(temp, content);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            writtenVersions.put(huntId, version);
        } catch (Exception e) {
            LogUtil.error("Cannot save the spawned heads of hunt {0}: {1}", huntId, e.getMessage());
        }
    }

    private File fileOf(String huntId) {
        return new File(folder, huntId + ".yml");
    }

    private static String blockKey(Location location) {
        return (location.getWorld() == null ? "" : location.getWorld().getName())
                + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private static Optional<UUID> parseUuid(String raw) {
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static SpawnBehavior behaviorOf(HBHunt hunt) {
        for (var behavior : hunt.getBehaviors()) {
            if (behavior instanceof SpawnBehavior spawnBehavior) {
                return spawnBehavior;
            }
        }
        return null;
    }

    private record ActiveSpawn(HeadLocation head, String templateId) {
    }

    private record DormantSpawn(UUID uuid, String templateId, String world, double x, double y, double z, float yaw,
                                RenderMode render) {
    }

    private record Snapshot(String huntId, String content, long version) {
    }

    private static final class SpawnState {
        private final String huntId;
        private final Map<UUID, ActiveSpawn> active = new ConcurrentHashMap<>();
        private final Map<UUID, DormantSpawn> dormant = new ConcurrentHashMap<>();
        private final Deque<UUID> pool = new ConcurrentLinkedDeque<>();
        private final Set<UUID> reserved = ConcurrentHashMap.newKeySet();
        private final List<Long> pending = new CopyOnWriteArrayList<>();
        private final List<Task> respawnTasks = new CopyOnWriteArrayList<>();
        private final Map<String, Long> blockedUntil = new ConcurrentHashMap<>();
        private final AtomicInteger inflight = new AtomicInteger();
        private final AtomicInteger totalSpawned = new AtomicInteger();
        private final AtomicLong nextIntervalAt = new AtomicLong();
        private final AtomicLong version = new AtomicLong();
        private final AtomicBoolean refilling = new AtomicBoolean();
        private volatile Task intervalTask;
        private volatile boolean closed;

        private SpawnState(String huntId) {
            this.huntId = huntId;
        }

        private void cancelRespawns() {
            respawnTasks.forEach(Task::cancel);
            respawnTasks.clear();
        }

        private void cancelTasks() {
            cancelRespawns();
            if (intervalTask != null) {
                intervalTask.cancel();
                intervalTask = null;
            }
        }
    }
}
