package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.ServiceRegistry;
import fr.aerwyn81.headblocks.api.events.HuntStateChangeEvent;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.head.visual.ContentKind;
import fr.aerwyn81.headblocks.data.head.visual.RenderMode;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.data.hunt.behavior.RandomSpawnBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.SpawnBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.SpawnPointsBehavior;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.ClaimOutcome;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.SpawnParticle;
import fr.aerwyn81.headblocks.data.hunt.behavior.spawn.SpawnTemplate;
import fr.aerwyn81.headblocks.utils.bukkit.PlayerUtils;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.utils.scheduler.Task;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SpawnService {
    private static final long FLUSH_PERIOD_TICKS = 20L;
    private static final int RETRY_SECONDS = 30;
    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ServiceRegistry registry;
    private final File folder;
    private final Map<String, SpawnState> states = new ConcurrentHashMap<>();
    private final Set<String> dirty = ConcurrentHashMap.newKeySet();
    private ExecutorService writer;
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
        writer = Executors.newSingleThreadExecutor();

        purgeOrphanRows();

        for (HBHunt hunt : registry.getHuntService().getAllHunts()) {
            var behavior = behaviorOf(hunt);
            if (behavior == null) {
                continue;
            }

            boolean firstStart = !fileOf(hunt.getId()).exists();
            var state = restore(hunt, behavior);
            if (hunt.isActive()) {
                resume(behavior, state, firstStart);
            }
        }

        cleanupOrphanFiles();
        flushTask = registry.getScheduler().runTaskTimer(this::flush, FLUSH_PERIOD_TICKS, FLUSH_PERIOD_TICKS);
    }

    public void stop() {
        if (flushTask != null) {
            flushTask.cancel();
            flushTask = null;
        }

        for (SpawnState state : states.values()) {
            state.stopTasks();
            save(state);
        }
        states.clear();
        dirty.clear();

        if (writer != null) {
            writer.shutdown();
            try {
                writer.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
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
        state.waiting += state.pending.get();
        state.cancelTasks();

        if (hunt.isActive()) {
            resume(behavior, state, true);
        } else {
            state.waiting = 0;
            clearHeads(state);
            state.nextIntervalAt.set(0);
            markDirty(state);
        }
    }

    public void onWorldLoaded(World world) {
        for (SpawnState state : states.values()) {
            var behavior = activeBehaviorOf(state);
            if (behavior == null) {
                continue;
            }

            for (var dormant : List.copyOf(state.dormant.values())) {
                if (!dormant.world().equals(world.getName())) {
                    continue;
                }

                state.dormant.remove(dormant.uuid());
                var template = behavior.template(dormant.templateId());
                if (template == null || !activate(state, restoredHead(state.huntId, dormant, world, template), template)) {
                    spawnOne(state, null);
                }
            }
            markDirty(state);
        }
    }

    // --- Claim flow ---

    public ClaimOutcome claim(HBHunt hunt, HeadLocation head, Player player) {
        var state = states.get(hunt.getId());
        var spawn = state == null ? null : state.active.remove(head.getUuid());
        if (spawn == null) {
            return ClaimOutcome.TAKEN;
        }

        var behavior = behaviorOf(hunt);
        var template = behavior == null ? null : behavior.template(spawn.templateId());
        if (template != null && template.rollTrap()) {
            registry.getHeadService().removeSpawnedHead(head);
            markDirty(state);
            log(state, "TRAP", head, player);
            runTrapCommands(template, hunt, head, player);
            respawnAfterFind(state, head.getLocation());
            return ClaimOutcome.TRAPPED;
        }

        try {
            registry.getStorageService().createSpawnHead(head.getUuid(), textureOf(head), template == null ? 1 : template.points());
        } catch (InternalException e) {
            state.active.put(head.getUuid(), spawn);
            LogUtil.error("Cannot store the spawned head found in hunt {0}: {1}", hunt.getId(), e.getMessage());
            return ClaimOutcome.TAKEN;
        }

        registry.getHeadService().removeSpawnedHead(head);
        markDirty(state);
        log(state, "FOUND", head, player);
        respawnAfterFind(state, head.getLocation());
        return ClaimOutcome.FOUND;
    }

    public int addHeads(HBHunt hunt, int count) {
        var state = states.get(hunt.getId());
        if (state == null || activeBehaviorOf(state) == null) {
            return 0;
        }

        int spawned = 0;
        for (int i = 0; i < count; i++) {
            if (spawnOne(state, null)) {
                spawned++;
            }
        }
        announce(state, spawned);
        return spawned;
    }

    public SpawnParticle particleOf(HeadLocation head) {
        var state = states.get(head.getHuntId());
        var spawn = state == null ? null : state.active.get(head.getUuid());
        var behavior = spawn == null ? null : activeBehaviorOf(state);
        var template = behavior == null ? null : behavior.template(spawn.templateId());
        return template == null ? null : template.particle();
    }

    public int totalSpawned(String huntId) {
        var state = states.get(huntId);
        return state == null ? 0 : state.totalSpawned.get();
    }

    public void onLost(HeadLocation head) {
        var state = removeFromItsState(head);
        var behavior = state == null ? null : activeBehaviorOf(state);
        if (behavior != null && freeSlots(state, behavior) > 0) {
            scheduleRespawn(state, RETRY_SECONDS, head.getLocation());
        }
    }

    public void onDiscarded(HeadLocation head) {
        var state = removeFromItsState(head);
        if (state != null) {
            log(state, "REMOVED", head, null);
            respawnAfterFind(state, head.getLocation());
        }
    }

    public void deleteHunt(String huntId) {
        var state = states.remove(huntId);
        if (state != null) {
            state.cancelTasks();
            clearHeads(state);
        }

        dirty.remove(huntId);
        onWriter(() -> deleteFile(fileOf(huntId)));
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
        clearHeads(state);
        markDirty(state);

        if (!resetProgress) {
            fill(state, behavior);
            return;
        }

        var online = Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).toList();
        registry.getScheduler().runTaskAsync(() -> {
            try {
                registry.getStorageService().deletePlayerProgressForHunt(hunt.getId());
                online.forEach(registry.getStorageService()::invalidateCachePlayer);
            } catch (InternalException e) {
                LogUtil.error("Cannot reset the progress of hunt {0}: {1}", hunt.getId(), e.getMessage());
            }

            registry.getScheduler().runTask(() -> {
                if (states.get(hunt.getId()) == state) {
                    fill(state, behavior);
                }
            });
        });
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
            resume(behavior, state, true);
        } else if (behavior.respawn().onStart()) {
            state.cancelRespawns();
            fill(state, behavior);
        }
    }

    public void clear(HBHunt hunt) {
        var state = states.get(hunt.getId());
        if (state == null) {
            return;
        }

        state.cancelRespawns();
        clearHeads(state);
        markDirty(state);
    }

    public Collection<HeadLocation> getActiveHeads(String huntId) {
        var state = states.get(huntId);
        return state == null ? List.of() : state.active.values().stream().map(ActiveSpawn::head).toList();
    }

    // --- Spawning ---

    private void resume(SpawnBehavior behavior, SpawnState state, boolean fillEmptySlots) {
        if (behavior.respawn().interval()) {
            long now = System.currentTimeMillis();
            long period = behavior.respawn().intervalSeconds() * 1000L;
            if (state.nextIntervalAt.get() <= now) {
                state.nextIntervalAt.set(now + period);
            }

            state.intervalTask = registry.getScheduler().runTaskTimer(() -> {
                state.nextIntervalAt.set(System.currentTimeMillis() + period);
                var current = activeBehaviorOf(state);
                if (current != null) {
                    reroll(registry.getHuntService().getHuntById(state.huntId), current.respawn().resetProgress());
                }
            }, Math.max(1L, (state.nextIntervalAt.get() - now) / 50L), period / 50L);
        }

        int waiting = Math.min(state.waiting, freeSlots(state, behavior));
        state.waiting = 0;
        int spawned = 0;
        for (int i = 0; i < waiting; i++) {
            if (spawnOne(state, null)) {
                spawned++;
            }
        }

        if (fillEmptySlots && behavior.respawn().onStart()) {
            spawned += fill(state, behavior);
        }
        announce(state, spawned);
        markDirty(state);
    }

    private int fill(SpawnState state, SpawnBehavior behavior) {
        int missing = freeSlots(state, behavior);
        int spawned = 0;
        for (int i = 0; i < missing; i++) {
            if (spawnOne(state, null)) {
                spawned++;
            }
        }
        return spawned;
    }

    private static int freeSlots(SpawnState state, SpawnBehavior behavior) {
        return behavior.active() - state.active.size() - state.dormant.size() - state.pending.get();
    }

    private void respawnAfterFind(SpawnState state, Location freed) {
        var behavior = activeBehaviorOf(state);
        if (behavior != null && behavior.respawn().onFind() && freeSlots(state, behavior) > 0) {
            scheduleRespawn(state, behavior.respawn().nextFindDelay(), freed);
        }
    }

    private void scheduleRespawn(SpawnState state, int delaySeconds, Location avoid) {
        if (delaySeconds <= 0) {
            respawn(state, avoid);
            return;
        }

        state.pending.incrementAndGet();
        state.respawnTasks.add(registry.getScheduler().runTaskLater(() -> {
            state.pending.decrementAndGet();
            respawn(state, avoid);
        }, delaySeconds * 20L));
    }

    private void respawn(SpawnState state, Location avoid) {
        if (spawnOne(state, avoid)) {
            announce(state, 1);
        }
    }

    private boolean spawnOne(SpawnState state, Location avoid) {
        var behavior = activeBehaviorOf(state);
        if (behavior == null) {
            return false;
        }

        if (behavior.maxTotalSpawns() >= 0 && state.totalSpawned.get() >= behavior.maxTotalSpawns()) {
            return false;
        }

        var template = behavior.pickTemplate();
        if (template == null) {
            LogUtil.warning("Hunt {0} has no spawn template with a positive weight, no head can appear.", state.huntId);
            return false;
        }

        if (behavior instanceof RandomSpawnBehavior random) {
            return spawnRandom(state, random, template, avoid);
        }

        var points = (SpawnPointsBehavior) behavior;
        var location = points.pickLocation(candidate -> isFree(candidate, avoid));
        if (location == null && avoid != null) {
            location = points.pickLocation(candidate -> isFree(candidate, null));
        }

        return place(state, behavior, template, location, location == null ? 0f : points.yawAt(location));
    }

    private boolean spawnRandom(SpawnState state, RandomSpawnBehavior behavior, SpawnTemplate template, Location avoid) {
        var hunt = registry.getHuntService().getHuntById(state.huntId);
        var column = behavior.pickColumn(hunt);
        if (column == null) {
            scheduleRespawn(state, RETRY_SECONDS, null);
            return false;
        }

        var placed = new AtomicBoolean();
        var deferred = new AtomicBoolean();
        state.pending.incrementAndGet();
        registry.getScheduler().runNow(column, () -> {
            state.pending.decrementAndGet();
            if (activeBehaviorOf(state) != behavior) {
                return;
            }

            var location = behavior.pickInChunk(hunt, column, candidate -> isFree(candidate, avoid));
            placed.set(place(state, behavior, template, location, behavior.randomYaw()));
            if (placed.get() && deferred.get()) {
                announce(state, 1);
            }
        });
        deferred.set(true);
        return placed.get();
    }

    private boolean place(SpawnState state, SpawnBehavior behavior, SpawnTemplate template, Location location, float yaw) {
        if (location == null) {
            scheduleRespawn(state, RETRY_SECONDS, null);
            return false;
        }

        var head = buildHead(state.huntId, UUID.randomUUID(), template, location, yaw);
        if (!activate(state, head, template)) {
            scheduleRespawn(state, RETRY_SECONDS, null);
            return false;
        }

        state.totalSpawned.incrementAndGet();
        log(state, "SPAWN", head, null);
        traceSpawn(behavior, head);
        return true;
    }

    private void announce(SpawnState state, int count) {
        var behavior = activeBehaviorOf(state);
        if (count <= 0 || behavior == null || !behavior.options().announce()) {
            return;
        }

        var hunt = registry.getHuntService().getHuntById(state.huntId);
        var message = registry.getLanguageService().message(count == 1 ? "Messages.SpawnHeadAppeared" : "Messages.SpawnHeadsAppeared")
                .replace("%count%", String.valueOf(count))
                .replace("%hunt%", hunt.getDisplayName());
        if (!message.trim().isEmpty()) {
            Bukkit.broadcastMessage(message);
        }
    }

    private void traceSpawn(SpawnBehavior behavior, HeadLocation head) {
        if (!behavior.options().debug()) {
            return;
        }

        var location = head.getLocation();
        var message = registry.getLanguageService().message("Messages.SpawnDebugAppeared")
                .replace("%hunt%", head.getHuntId())
                .replace("%world%", location.getWorld().getName())
                .replace("%x%", String.valueOf(location.getBlockX()))
                .replace("%y%", String.valueOf(location.getBlockY()))
                .replace("%z%", String.valueOf(location.getBlockZ()));
        LogUtil.info(message);

        var component = new TextComponent(message);
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, teleportCommand(location)));
        for (Player admin : Bukkit.getOnlinePlayers()) {
            if (PlayerUtils.hasPermission(admin, "headblocks.admin")) {
                admin.spigot().sendMessage(component);
            }
        }
    }

    public static String teleportCommand(Location location) {
        return "/headblocks tp " + location.getWorld().getName() + " " + location.getX() + " " + location.getY()
                + " " + location.getZ() + " 0.0 90.0";
    }

    private void runTrapCommands(SpawnTemplate template, HBHunt hunt, HeadLocation head, Player player) {
        for (String command : template.trapCommands()) {
            var parsed = registry.getPlaceholdersService().parse(player.getName(), player.getUniqueId(), head, command, hunt.getId());
            if (!parsed.isBlank()) {
                registry.getCommandDispatcher().dispatchConsoleCommand(parsed);
            }
        }
    }

    private void log(SpawnState state, String event, HeadLocation head, Player player) {
        var hunt = registry.getHuntService().getHuntById(state.huntId);
        var behavior = hunt == null ? null : behaviorOf(hunt);
        if (behavior == null || !behavior.options().log()) {
            return;
        }

        var location = head.getLocation();
        var line = LOG_TIME.format(LocalDateTime.now()) + " " + event
                + " " + (location.getWorld() == null ? "?" : location.getWorld().getName())
                + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ()
                + " " + head.getNameOrUuid()
                + (player == null ? "" : " " + player.getName())
                + System.lineSeparator();
        var file = new File(folder, state.huntId + ".log").toPath();
        onWriter(() -> {
            try {
                Files.writeString(file, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception e) {
                LogUtil.error("Cannot write the spawn log of hunt {0}: {1}", state.huntId, e.getMessage());
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

    private boolean isFree(Location location, Location avoid) {
        if (registry.getHeadService().getHeadAt(location) != null) {
            return false;
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
        template.drawRewards().forEach(head::addReward);
        return head;
    }

    private HeadLocation restoredHead(String huntId, SavedSpawn saved, World world, SpawnTemplate template) {
        var head = buildHead(huntId, saved.uuid(), template, new Location(world, saved.x(), saved.y(), saved.z()), saved.yaw());
        if (saved.render() != null) {
            head.setRenderMode(saved.render());
        }
        return head;
    }

    private void clearHeads(SpawnState state) {
        for (var spawn : List.copyOf(state.active.values())) {
            if (state.active.remove(spawn.head().getUuid()) != null) {
                registry.getHeadService().removeSpawnedHead(spawn.head());
            }
        }
        state.dormant.clear();
    }

    private SpawnState removeFromItsState(HeadLocation head) {
        for (SpawnState state : states.values()) {
            if (state.active.remove(head.getUuid()) != null) {
                markDirty(state);
                return state;
            }
        }
        return null;
    }

    private SpawnBehavior activeBehaviorOf(SpawnState state) {
        var hunt = registry.getHuntService().getHuntById(state.huntId);
        return hunt == null || !hunt.isActive() ? null : behaviorOf(hunt);
    }

    private static String textureOf(HeadLocation head) {
        var content = head.getContent();
        return content != null && content.kind() == ContentKind.HEAD ? content.value() : "";
    }

    private void purgeOrphanRows() {
        try {
            int purged = registry.getStorageService().purgeOrphanSpawnHeads();
            if (purged > 0) {
                LogUtil.info("Cleaned {0} unused spawned head(s) from the database.", purged);
            }
        } catch (InternalException e) {
            LogUtil.error("Cannot clean unused spawned heads: {0}", e.getMessage());
        }
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
        state.waiting = yaml.getInt("pending", 0);
        state.nextIntervalAt.set(yaml.getLong("nextIntervalAt", 0));

        for (var saved : readSpawns(yaml)) {
            var world = Bukkit.getWorld(saved.world());
            var template = behavior.template(saved.templateId());

            if (world == null) {
                if (hunt.isActive() && template != null) {
                    state.dormant.put(saved.uuid(), saved);
                }
            } else if (template == null || !hunt.isActive()) {
                removeLeftover(saved, world, template);
                state.waiting++;
            } else if (!activate(state, restoredHead(hunt.getId(), saved, world, template), template)) {
                state.waiting++;
            }
        }
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

            for (var saved : readSpawns(YamlConfiguration.loadConfiguration(file))) {
                var world = Bukkit.getWorld(saved.world());
                if (world != null) {
                    removeLeftover(saved, world, null);
                }
            }

            deleteFile(file);
            LogUtil.info("Removed the spawned heads of {0}, the hunt no longer uses them.", huntId);
        }
    }

    private void removeLeftover(SavedSpawn saved, World world, SpawnTemplate template) {
        var head = new HeadLocation("", saved.uuid(), new Location(world, saved.x(), saved.y(), saved.z()), "");
        if (template != null) {
            head.setContent(template.content());
        }
        head.setRenderMode(saved.render());

        if (registry.getVisualService().isBlockRendered(head)) {
            registry.getScheduler().runNow(head.getLocation(), () -> registry.getVisualService().removeBlock(head));
        }
    }

    private void markDirty(SpawnState state) {
        dirty.add(state.huntId);
    }

    private void flush() {
        for (String huntId : List.copyOf(dirty)) {
            dirty.remove(huntId);
            var state = states.get(huntId);
            if (state != null) {
                save(state);
            }
        }
    }

    private void save(SpawnState state) {
        onWriter(() -> writeFile(fileOf(state.huntId), snapshot(state)));
    }

    private void onWriter(Runnable task) {
        if (writer == null || writer.isShutdown()) {
            task.run();
        } else {
            writer.submit(task);
        }
    }

    private static String snapshot(SpawnState state) {
        var yaml = new YamlConfiguration();
        yaml.set("totalSpawned", state.totalSpawned.get());
        yaml.set("pending", state.pending.get());
        yaml.set("nextIntervalAt", state.nextIntervalAt.get());

        var section = yaml.createSection("active");
        state.active.values().forEach(spawn -> SavedSpawn.of(spawn).saveTo(section));
        state.dormant.values().forEach(dormant -> dormant.saveTo(section));
        return yaml.saveToString();
    }

    private static List<SavedSpawn> readSpawns(YamlConfiguration yaml) {
        var section = yaml.getConfigurationSection("active");
        if (section == null) {
            return List.of();
        }

        var spawns = new ArrayList<SavedSpawn>();
        for (String key : section.getKeys(false)) {
            var saved = SavedSpawn.read(key, section.getConfigurationSection(key));
            if (saved != null) {
                spawns.add(saved);
            }
        }
        return spawns;
    }

    private void writeFile(File file, String content) {
        try {
            var temp = Files.createTempFile(folder.toPath(), file.getName(), ".tmp");
            Files.writeString(temp, content);
            try {
                Files.move(temp, file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            LogUtil.error("Cannot save the spawned heads file {0}: {1}", file.getName(), e.getMessage());
        }
    }

    private static void deleteFile(File file) {
        try {
            Files.deleteIfExists(file.toPath());
        } catch (Exception e) {
            LogUtil.error("Cannot delete the spawned heads file {0}: {1}", file.getName(), e.getMessage());
        }
    }

    private File fileOf(String huntId) {
        return new File(folder, huntId + ".yml");
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

    private record SavedSpawn(UUID uuid, String templateId, String world, double x, double y, double z, float yaw,
                              RenderMode render) {

        static SavedSpawn of(ActiveSpawn spawn) {
            var head = spawn.head();
            var location = head.getLocation();
            return new SavedSpawn(head.getUuid(), spawn.templateId(),
                    location.getWorld() == null ? "" : location.getWorld().getName(),
                    location.getX(), location.getY(), location.getZ(), head.getYaw(), head.getRenderMode());
        }

        static SavedSpawn read(String key, ConfigurationSection entry) {
            if (entry == null) {
                return null;
            }

            try {
                return new SavedSpawn(UUID.fromString(key), entry.getString("template", ""), entry.getString("world", ""),
                        entry.getDouble("x"), entry.getDouble("y"), entry.getDouble("z"), (float) entry.getDouble("yaw"),
                        RenderMode.of(entry.getString("render")));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        void saveTo(ConfigurationSection section) {
            var entry = section.createSection(uuid.toString());
            entry.set("template", templateId);
            entry.set("world", world);
            entry.set("x", x);
            entry.set("y", y);
            entry.set("z", z);
            entry.set("yaw", (double) yaw);
            entry.set("render", render == null ? null : render.name());
        }
    }

    private static final class SpawnState {
        private final String huntId;
        private final Map<UUID, ActiveSpawn> active = new ConcurrentHashMap<>();
        private final Map<UUID, SavedSpawn> dormant = new ConcurrentHashMap<>();
        private final List<Task> respawnTasks = new CopyOnWriteArrayList<>();
        private final AtomicInteger pending = new AtomicInteger();
        private final AtomicInteger totalSpawned = new AtomicInteger();
        private int waiting;
        private final AtomicLong nextIntervalAt = new AtomicLong();
        private volatile Task intervalTask;

        private SpawnState(String huntId) {
            this.huntId = huntId;
        }

        private void stopTasks() {
            respawnTasks.forEach(Task::cancel);
            respawnTasks.clear();
            if (intervalTask != null) {
                intervalTask.cancel();
                intervalTask = null;
            }
        }

        private void cancelRespawns() {
            respawnTasks.forEach(Task::cancel);
            respawnTasks.clear();
            pending.set(0);
        }

        private void cancelTasks() {
            stopTasks();
            pending.set(0);
        }
    }
}
