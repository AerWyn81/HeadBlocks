package fr.aerwyn81.headblocks.services;

import de.tr7zw.changeme.nbtapi.NBT;
import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.HeadMove;
import fr.aerwyn81.headblocks.data.head.CatalogEntry;
import fr.aerwyn81.headblocks.data.head.HBHead;
import fr.aerwyn81.headblocks.data.head.HeadType;
import fr.aerwyn81.headblocks.data.head.types.HBHeadContent;
import fr.aerwyn81.headblocks.data.head.types.HBHeadDefault;
import fr.aerwyn81.headblocks.data.head.types.HBHeadPlayer;
import fr.aerwyn81.headblocks.data.head.visual.ContentKind;
import fr.aerwyn81.headblocks.data.head.visual.HeadContent;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.hooks.HeadProviderHook;
import fr.aerwyn81.headblocks.hooks.visual.VisualProviders;
import fr.aerwyn81.headblocks.utils.bukkit.HeadUtils;
import fr.aerwyn81.headblocks.utils.bukkit.PluginProvider;
import fr.aerwyn81.headblocks.utils.internal.InternalException;
import fr.aerwyn81.headblocks.utils.internal.InternalUtils;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import fr.aerwyn81.headblocks.utils.scheduler.SchedulerAdapter;
import fr.aerwyn81.headblocks.utils.scheduler.Task;
import fr.aerwyn81.headblocks.visual.ContentItems;
import fr.aerwyn81.headblocks.visual.renderers.MobRenderer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.Bed;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class HeadService {
    private final ConfigService configService;
    private final StorageService storageService;
    private final LanguageService languageService;
    private final SchedulerAdapter scheduler;
    private final Map<String, HeadProviderHook> headProviders;
    private HologramService hologramService; // setter-injected (circular dep)
    private HuntService huntService; // setter-injected
    private HuntConfigService huntConfigService; // setter-injected
    private HeadVisualService visualService; // setter-injected

    private ArrayList<HBHead> heads;
    private Map<UUID, HeadMove> headMoves;
    private List<HeadLocation> headLocations;
    private Map<UUID, Task> tasksHeadSpin;
    private final Map<String, Map<Long, HeadLocation>> headsByBlock = new ConcurrentHashMap<>();
    private final Map<UUID, HeadLocation> headsByUuid = new ConcurrentHashMap<>();
    private final Map<String, Set<HeadLocation>> headsByChunk = new ConcurrentHashMap<>();

    public static String HB_KEY = "HB_HEAD";
    public static String HB_HUNT_KEY = "HB_HUNT";
    public static String HB_CONTENT_KEY = "HB_CONTENT";

    // --- Constructor + instance lifecycle ---

    public HeadService(ConfigService configService, StorageService storageService,
                       LanguageService languageService, SchedulerAdapter scheduler,
                       PluginProvider pluginProvider) {
        this(configService, storageService, languageService, scheduler, pluginProvider, Collections.emptyMap());
    }

    public HeadService(ConfigService configService, StorageService storageService,
                       LanguageService languageService, SchedulerAdapter scheduler,
                       PluginProvider pluginProvider, Map<String, HeadProviderHook> headProviders) {
        this.configService = configService;
        this.storageService = storageService;
        this.languageService = languageService;
        this.scheduler = scheduler;
        this.headProviders = headProviders == null ? Collections.emptyMap() : headProviders;
    }

    public Map<String, HeadProviderHook> getHeadProviders() {
        return headProviders;
    }

    public void setHologramService(HologramService hologramService) {
        this.hologramService = hologramService;
    }

    public void setHuntService(HuntService huntService) {
        this.huntService = huntService;
    }

    public void setHuntConfigService(HuntConfigService huntConfigService) {
        this.huntConfigService = huntConfigService;
    }

    public void setVisualService(HeadVisualService visualService) {
        this.visualService = visualService;
    }

    // --- Instance lifecycle ---

    public void initialize() {
        heads = new ArrayList<>();
        headLocations = new CopyOnWriteArrayList<>();
        headMoves = new ConcurrentHashMap<>();
        tasksHeadSpin = new ConcurrentHashMap<>();

        load();
    }

    public void load() {
        heads.clear();
        headLocations.clear();
        headMoves.clear();
        cancelAllSpinTasks();

        loadHeads();
        loadLocations();
    }

    public void reloadCatalog() {
        heads.clear();
        loadHeads();
        headProviders.values().stream().filter(HeadProviderHook::isAvailable).forEach(HeadProviderHook::loadTextures);
    }

    public void cancelAllSpinTasks() {
        if (tasksHeadSpin != null) {
            tasksHeadSpin.values().forEach(Task::cancel);
            tasksHeadSpin.clear();
        }
    }

    public void loadLocations() {
        headLocations.clear();
        headsByBlock.clear();
        headsByUuid.clear();
        headsByChunk.clear();

        if (storageService.isStorageError()) {
            LogUtil.error("Cannot load locations from storage, theres an issue with the database.");
            return;
        }

        var i = 0;
        for (var hunt : huntService.getAllHunts()) {
            List<HeadLocation> huntLocations = huntConfigService.loadLocationsFromHunt(hunt.getId());

            for (var headLoc : huntLocations) {
                i++;

                try {
                    boolean isExist = storageService.isHeadExist(headLoc.getUuid());
                    if (!isExist) {
                        storageService.createOrUpdateHead(headLoc.getUuid(), textureOf(headLoc));
                    }
                } catch (Exception ex) {
                    LogUtil.error("Error while trying to create a head ({0}) in the storage: {1}", headLoc.getUuid(), ex.getMessage());
                    continue;
                }

                hunt.addHead(headLoc.getUuid());
                addHeadToSpin(headLoc, i);
                register(headLoc);
            }
        }

        // Purge for remote database
        if (configService.databaseEnabled()) {
            try {
                var dbHeads = storageService.getHeadsByServerId();
                if (dbHeads.isEmpty()) {
                    for (var headLoc : getHeadLocations()) {
                        storageService.createOrUpdateHead(headLoc.getUuid(), textureOf(headLoc));
                    }
                } else {
                    dbHeads.removeAll(getHeadLocations().stream().map(HeadLocation::getUuid).toList());

                    if (!dbHeads.isEmpty()) {
                        LogUtil.error("Found {0} heads ({1}) out of sync with the server, deleting...",
                                dbHeads.size(),
                                String.join(", ", dbHeads.stream().map(UUID::toString).toList()));

                        for (var head : dbHeads) {
                            storageService.removeHead(head, true);
                        }

                        LogUtil.success("Headblocks heads table cleaned!");
                    }
                }
            } catch (Exception e) {
                LogUtil.error("Error when purging heads out of sync in the database: {0}", e.getMessage());
            }
        }

        LogUtil.success("Loaded {0} locations!", headLocations.size());
    }

    private void addHeadToSpin(HeadLocation headLoc, int offset) {
        var huntConfig = huntService.configOf(headLoc.getHuntId());
        if (!huntConfig.isSpinEnabled() || huntConfig.isSpinLinked()) {
            return;
        }

        if (headLoc.getLocation() == null) {
            return;
        }

        var task = scheduler.runTaskTimer(headLoc.getLocation(),
                () -> rotateHead(headLoc), 5L * offset, huntConfig.getSpinSpeed());

        var previous = tasksHeadSpin.put(headLoc.getUuid(), task);
        if (previous != null) {
            previous.cancel();
        }
    }

    public UUID saveHeadLocation(Location location, String texture, String huntId) throws InternalException {
        return saveHeadLocation(location, texture == null ? null : HeadContent.head(texture), 0f, huntId);
    }

    public UUID saveHeadLocation(Location location, HeadContent content, float yaw, String huntId) throws InternalException {
        UUID uniqueUuid = InternalUtils.generateNewUUID(headLocations.stream().map(HeadLocation::getUuid).collect(Collectors.toList()));

        var texture = content != null && content.kind() == ContentKind.HEAD ? content.value() : "";
        storageService.createOrUpdateHead(uniqueUuid, texture);

        var headLocation = new HeadLocation("", uniqueUuid, location, huntId);
        headLocation.setContent(content);
        headLocation.setYaw(yaw);
        headLocation.setRenderMode(huntService.configOf(huntId).getRenderMode());
        saveHeadInConfig(headLocation);

        var hunt = huntService.getHuntById(huntId);
        if (hunt != null) {
            hunt.addHead(uniqueUuid);
        }

        register(headLocation);

        visualService.ensureSpawned(headLocation);

        if (configService.hologramsEnabled() && hologramService != null) {
            hologramService.createHolograms(location, huntService.configOf(huntId));
        }

        addHeadToSpin(headLocation, 1);

        return uniqueUuid;
    }

    private void register(HeadLocation headLocation) {
        headLocations.add(headLocation);
        headsByUuid.putIfAbsent(headLocation.getUuid(), headLocation);
        indexPosition(headLocation);
    }

    private void unregister(HeadLocation headLocation) {
        headLocations.remove(headLocation);
        headsByUuid.remove(headLocation.getUuid(), headLocation);
        unindexPosition(headLocation);
    }

    private void indexPosition(HeadLocation headLocation) {
        headsByBlock.computeIfAbsent(worldOf(headLocation), k -> new ConcurrentHashMap<>())
                .putIfAbsent(blockKey(headLocation.getX(), headLocation.getY(), headLocation.getZ()), headLocation);
        headsByChunk.computeIfAbsent(chunkKey(headLocation), k -> ConcurrentHashMap.newKeySet()).add(headLocation);
    }

    private void unindexPosition(HeadLocation headLocation) {
        headsByBlock.computeIfPresent(worldOf(headLocation), (k, heads) -> {
            heads.remove(blockKey(headLocation.getX(), headLocation.getY(), headLocation.getZ()), headLocation);
            return heads.isEmpty() ? null : heads;
        });
        headsByChunk.computeIfPresent(chunkKey(headLocation), (k, heads) -> {
            heads.remove(headLocation);
            return heads.isEmpty() ? null : heads;
        });
    }

    private void relocate(HeadLocation headLocation, Location newLocation) {
        unindexPosition(headLocation);
        headLocation.setLocation(newLocation);
        indexPosition(headLocation);
    }

    private static String chunkKey(HeadLocation headLocation) {
        return chunkKey(worldOf(headLocation),
                (int) Math.floor(headLocation.getX()) >> 4, (int) Math.floor(headLocation.getZ()) >> 4);
    }

    private static String chunkKey(String world, int chunkX, int chunkZ) {
        return world + ":" + chunkX + ":" + chunkZ;
    }

    public Collection<HeadLocation> getHeadsInChunk(String world, int chunkX, int chunkZ) {
        var heads = headsByChunk.get(chunkKey(world, chunkX, chunkZ));
        return heads == null ? List.of() : List.copyOf(heads);
    }

    private static String worldOf(HeadLocation headLocation) {
        return Objects.requireNonNullElse(headLocation.getConfigWorldName(), "");
    }

    private static long blockKey(double x, double y, double z) {
        return blockKey((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    private static long blockKey(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | (y & 0xFFFL);
    }

    private String textureOf(HeadLocation headLocation) {
        var content = headLocation.getContent();
        if (content != null) {
            return content.kind() == ContentKind.HEAD ? content.value() : "";
        }

        var location = headLocation.getLocation();
        return location != null ? HeadUtils.getHeadTexture(location.getBlock()) : "";
    }

    public void saveHeadInConfig(HeadLocation headLocation) {
        huntConfigService.saveLocationInHunt(headLocation.getHuntId(), headLocation);
    }

    public void saveAllHeadsInConfig() {
        for (var headLocation : headLocations) {
            huntConfigService.saveLocationInHunt(headLocation.getHuntId(), headLocation);
        }
    }

    public void removeHeadLocation(HeadLocation headLocation, boolean withDelete) throws InternalException {
        if (headLocation != null) {
            storageService.removeHead(headLocation.getUuid(), withDelete);

            var location = headLocation.getLocation();
            if (location != null) {
                scheduler.runNow(location, () -> {
                    visualService.clear(headLocation);

                    if (configService.hologramsEnabled() && hologramService != null) {
                        hologramService.removeHolograms(location);
                    }
                });
            }

            var hunt = huntService.getHuntById(headLocation.getHuntId());
            if (hunt != null) {
                hunt.removeHead(headLocation.getUuid());
            }
            huntConfigService.removeLocationFromHunt(headLocation.getHuntId(), headLocation.getUuid());

            unregister(headLocation);

            headMoves.entrySet().removeIf(hM -> headLocation.getUuid().equals(hM.getKey()));
            var spinTaskId = tasksHeadSpin.remove(headLocation.getUuid());
            if (spinTaskId != null) {
                spinTaskId.cancel();
            }
        }
    }

    public void removeAllHeadLocationsAsync(ArrayList<HeadLocation> headsToRemove, boolean withDelete,
                                            java.util.function.Consumer<Integer> onComplete) {
        scheduler.runTaskAsync(() -> {
            int removed = 0;

            for (HeadLocation headLocation : headsToRemove) {
                if (headLocation == null) {
                    continue;
                }

                try {
                    storageService.removeHead(headLocation.getUuid(), withDelete);
                } catch (InternalException ex) {
                    LogUtil.error("Error removing head {0} from storage: {1}", headLocation.getNameOrUuid(), ex.getMessage());
                    continue;
                }

                var location = headLocation.getLocation();
                if (location != null) {
                    scheduler.runTask(location, () -> {
                        visualService.clear(headLocation);

                        if (configService.hologramsEnabled() && hologramService != null) {
                            hologramService.removeHolograms(location);
                        }
                    });
                }

                unregister(headLocation);

                huntConfigService.removeLocationFromHunt(headLocation.getHuntId(), headLocation.getUuid());

                headMoves.entrySet().removeIf(hM -> headLocation.getUuid().equals(hM.getKey()));
                var spinTaskId = tasksHeadSpin.remove(headLocation.getUuid());
                if (spinTaskId != null) {
                    spinTaskId.cancel();
                }

                removed++;
            }

            final int finalRemoved = removed;
            scheduler.runTask(() -> {
                for (HeadLocation hl : headsToRemove) {
                    if (hl != null) {
                        var hunt = huntService.getHuntById(hl.getHuntId());
                        if (hunt != null) {
                            hunt.removeHead(hl.getUuid());
                        }
                    }
                }

                onComplete.accept(finalRemoved);
            });
        });
    }

    public HeadLocation getHeadByUUID(UUID headUuid) {
        if (headUuid == null) {
            return null;
        }
        return headsByUuid.get(headUuid);
    }

    public HeadLocation getHeadByName(String name) {
        return headLocations.stream().filter(h -> h.getRawNameOrUuid().equals(name))
                .findFirst()
                .orElse(null);
    }

    public HeadLocation getHeadAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }

        var heads = headsByBlock.get(location.getWorld().getName());
        return heads == null ? null : heads.get(blockKey(location.getX(), location.getY(), location.getZ()));
    }

    public HeadLocation getBlockHeadAt(Block block) {
        var heads = headsByBlock.get(block.getWorld().getName());
        if (heads == null) {
            return null;
        }

        var headLocation = heads.get(blockKey(block.getX(), block.getY(), block.getZ()));
        return headLocation != null && visualService.isBlockRendered(headLocation) ? headLocation : null;
    }

    public HeadLocation getBlockHeadAt(Location location) {
        var headLocation = getHeadAt(location);
        if (headLocation == null || !visualService.isBlockRendered(headLocation)) {
            return null;
        }
        return headLocation;
    }

    private void loadHeads() {
        List<?> headsConfig;

        if (configService.headsThemeEnabled()) {
            var selectedTheme = configService.headsThemeSelected().trim();
            var themeHeads = configService.headsThemeEntries().get(selectedTheme);

            if (selectedTheme.isEmpty() || themeHeads == null) {
                LogUtil.error("Error when trying to use heads theme, selected theme is empty or don't match any theme.");
                return;
            }

            headsConfig = themeHeads;
        } else {
            headsConfig = configService.headEntries();
        }

        for (int i = 0; i < headsConfig.size(); i++) {
            var entry = CatalogEntry.parse(headsConfig.get(i));
            int line = i + 1;

            if (entry == null) {
                LogUtil.error("Invalid format for {0} in HBHeads configuration section (l.{1})", headsConfig.get(i), line);
                continue;
            }

            if (entry.value().trim().isEmpty()) {
                LogUtil.error("Value cannot be empty for {0} in HBHeads configuration section (l.{1})", entry.raw(), line);
                continue;
            }

            var head = createCatalogHead(entry, line);
            if (head != null) {
                heads.add(head);
            }
        }

        long providerHeadCount = heads.size() - heads.stream()
                .filter(h -> h instanceof HBHeadDefault || h instanceof HBHeadPlayer || h instanceof HBHeadContent)
                .count();
        long localHeadCount = heads.size() - providerHeadCount;

        if (providerHeadCount == 0) {
            LogUtil.success("Loaded {0} configuration heads!", localHeadCount);
        } else {
            LogUtil.success("Loaded {0} (+{1} provider heads) configuration heads!", localHeadCount, providerHeadCount);
        }
    }

    private HBHead createCatalogHead(CatalogEntry entry, int line) {
        return switch (entry.type()) {
            case "default" -> HeadUtils.createHead(new HBHeadDefault(baseHeadItem()), entry.value());
            case "player" -> playerHead(entry);
            case "block", "item", "mob", "entity", "text", "frame" -> contentHead(contentOf(entry, line));
            default -> VisualProviders.isKnown(entry.type())
                    ? externalHead(entry, line)
                    : addProviderHead(baseHeadItem(), entry.type(), entry.value(), entry.raw(), line);
        };
    }

    private ItemStack baseHeadItem() {
        var head = new ItemStack(Material.PLAYER_HEAD, 1);
        return withCatalogMeta(head, languageService.messageList("Head.Lore"));
    }

    private ItemStack withCatalogMeta(ItemStack item, List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(languageService.message("Head.Name"));
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(new NamespacedKey(HeadBlocks.getInstance(), HB_KEY), PersistentDataType.STRING, "");
        item.setItemMeta(meta);
        return item;
    }

    private HBHead playerHead(CatalogEntry entry) {
        OfflinePlayer player;
        try {
            player = Bukkit.getOfflinePlayer(UUID.fromString(entry.value()));
        } catch (Exception ex) {
            LogUtil.error("Cannot parse the player UUID {0}. Please provide a correct UUID.", entry.raw());
            return null;
        }

        var head = baseHeadItem();
        if (head.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(player);
            head.setItemMeta(meta);
        }
        return new HBHeadPlayer(head);
    }

    private HBHead externalHead(CatalogEntry entry, int line) {
        var provider = visualService.getProvider(entry.type());
        if (provider == null) {
            LogUtil.error("Cannot load head {0}: the {1} plugin is not installed or enabled.", entry.raw(), VisualProviders.pluginOf(entry.type()));
            return null;
        }

        if (!provider.isReady()) {
            return null;
        }

        if (!provider.exists(entry.value())) {
            LogUtil.error("Invalid head {0} (l.{1}): {2} does not know {3}.", entry.raw(), line, provider.pluginName(), entry.value());
            return null;
        }

        return contentHead(HeadContent.external(entry.type(), entry.value(), entry.options()));
    }

    private HBHead contentHead(HeadContent content) {
        if (content == null) {
            return null;
        }

        var lore = new ArrayList<>(languageService.messageList("Head.Lore"));
        lore.add(languageService.message("Head.ContentLore").replace("%content%", content.describe()));
        var item = withCatalogMeta(visualService.iconOf(content), lore);

        return new HBHeadContent(HeadUtils.withContent(item, content), content);
    }

    private HeadContent contentOf(CatalogEntry entry, int line) {
        Map<String, Object> options = new LinkedHashMap<>(entry.options());

        switch (entry.type()) {
            case "block" -> {
                var material = Material.matchMaterial(entry.value());
                if (material == null || !material.isBlock() || !material.isItem()) {
                    LogUtil.error("Invalid head {0} (l.{1}): {2} is not a placeable block.", entry.raw(), line, entry.value());
                    return null;
                }
                if (isUnstableBlock(material)) {
                    LogUtil.error("Invalid head {0} (l.{1}): {2} falls or spans two blocks, it cannot stay in place.", entry.raw(), line, entry.value());
                    return null;
                }
                return HeadContent.of(ContentKind.BLOCK, material.name(), options);
            }
            case "item", "frame" -> {
                var parts = entry.value().split(":");
                var material = Material.matchMaterial(parts[0]);
                if (material == null || !material.isItem()) {
                    LogUtil.error("Invalid head {0} (l.{1}): {2} is not an item.", entry.raw(), line, parts[0]);
                    return null;
                }
                if (parts.length > 1) {
                    try {
                        options.put("customModelData", Integer.parseInt(parts[1]));
                    } catch (NumberFormatException e) {
                        LogUtil.error("Invalid head {0} (l.{1}): custom model data {2} is not a number.", entry.raw(), line, parts[1]);
                        return null;
                    }
                }
                return HeadContent.of(entry.type().equals("frame") ? ContentKind.FRAME : ContentKind.ITEM, material.name(), options);
            }
            case "text" -> {
                return HeadContent.of(ContentKind.TEXT, entry.value(), options);
            }
            default -> {
                var content = HeadContent.of(ContentKind.MOB, entry.value().toUpperCase(), options);
                try {
                    MobRenderer.typeOf(content);
                } catch (IllegalStateException e) {
                    LogUtil.error("Invalid head {0} (l.{1}): {2}.", entry.raw(), line, e.getMessage());
                    return null;
                }

                var invalid = MobRenderer.EQUIPMENT.keySet().stream()
                        .filter(slot -> content.option(slot) != null && ContentItems.equipmentOf(content.option(slot)) == null)
                        .findFirst();
                if (invalid.isPresent()) {
                    LogUtil.error("Invalid head {0} (l.{1}): {2} is not an item.", entry.raw(), line, content.option(invalid.get()));
                    return null;
                }
                return content;
            }
        }
    }

    private static boolean isUnstableBlock(Material material) {
        if (material.hasGravity()) {
            return true;
        }

        var data = material.createBlockData();
        return data instanceof Bisected || data instanceof Bed;
    }

    HBHead addProviderHead(ItemStack head, String type, String rawId, String configHead, int line) {
        HeadProviderHook provider = headProviders.get(type);
        if (provider != null && provider.isAvailable()) {
            try {
                return provider.createHead(head, rawId);
            } catch (IllegalArgumentException ex) {
                LogUtil.error("Invalid head {0} (l.{1}): {2}", configHead, line, ex.getMessage());
                return null;
            }
        }

        HeadType headType = HeadType.fromPrefix(type);
        if (headType != null && headType.requiresPlugin()) {
            LogUtil.error("Cannot load head {0}: the {1} plugin is not installed or enabled.", configHead, headType.getPluginName());
        } else {
            LogUtil.error("The {0} type is not yet supported!", type);
        }
        return null;
    }

    public ArrayList<HBHead> getHeads() {
        return heads;
    }

    public ArrayList<HeadLocation> getChargedHeadLocations() {
        return headLocations.stream().filter(HeadLocation::isCharged).collect(Collectors.toCollection(ArrayList::new));
    }

    public List<HeadLocation> getHeadLocations() {
        return headLocations;
    }

    public ArrayList<HeadLocation> getHeadLocationsForHunt(HBHunt hunt) {
        return headLocations.stream()
                .filter(h -> hunt.getId().equals(h.getHuntId()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<String> getHeadRawNameOrUuid() {
        return headLocations.stream().map(HeadLocation::getRawNameOrUuid).collect(Collectors.toCollection(ArrayList::new));
    }

    public Map<UUID, HeadMove> getHeadMoves() {
        return headMoves;
    }

    public void clearHeadMoves() {
        if (headMoves == null) {
            return;
        }

        headMoves.clear();
    }

    public HeadLocation resolveHeadIdentifier(String headIdentifier) {
        try {
            var headUuid = UUID.fromString(headIdentifier);
            return getHeadByUUID(headUuid);
        } catch (IllegalArgumentException e) {
            return getHeadByName(headIdentifier);
        }
    }

    public void changeHeadLocation(UUID hUuid, @NotNull Block oldBlock, Block newBlock) {
        if (oldBlock.getState() instanceof Skull oldSkull) {
            var rotation = oldSkull.getBlockData() instanceof Rotatable skullRotation
                    ? skullRotation.getRotation()
                    : HeadUtils.rotationOf(HeadUtils.yawOf(oldBlock));

            newBlock.setType(Material.PLAYER_HEAD);

            Skull newSkull = (Skull) newBlock.getState();

            Rotatable rotatable = (Rotatable) newSkull.getBlockData();
            rotatable.setRotation(rotation);
            newSkull.setBlockData(rotatable);

            NBT.modify(newSkull, nbt -> {
                NBT.get(oldSkull, oldNbt -> {
                    nbt.mergeCompound(oldNbt);
                    return null;
                });
            });

            newSkull.setOwnerProfile(oldSkull.getOwnerProfile());

            newSkull.update(true);
        } else {
            newBlock.setBlockData(oldBlock.getBlockData());
        }

        oldBlock.setType(Material.AIR);

        var headLocation = getHeadByUUID(hUuid);

        var centeredLoc = newBlock.getLocation().clone().add(0.5, 0, 0.5);

        relocate(headLocation, centeredLoc);
        saveHeadInConfig(headLocation);

        if (hologramService != null) {
            hologramService.removeHolograms(oldBlock.getLocation());
            hologramService.createHolograms(centeredLoc);
        }

        addHeadToSpin(headLocation, 1);
    }

    public void moveEntityHead(HeadLocation headLocation, Location newLocation) {
        var oldLocation = headLocation.getLocation();

        visualService.despawn(headLocation);

        relocate(headLocation, newLocation);
        saveHeadInConfig(headLocation);

        scheduler.runNow(newLocation, () -> visualService.ensureSpawned(headLocation));

        if (hologramService != null && hologramService.isEnabled() && oldLocation != null) {
            hologramService.removeHolograms(oldLocation);
            hologramService.createHolograms(newLocation, huntService.configOf(headLocation.getHuntId()));
        }

        addHeadToSpin(headLocation, 1);
    }

    public void rotateHead(HeadLocation headLocation) {
        if (visualService.isEntityRendered(headLocation)) {
            var huntConfig = huntService.configOf(headLocation.getHuntId());
            visualService.spin(headLocation, huntConfig.isSpinLinked() ? configService.delayGlobalTask() : huntConfig.getSpinSpeed());
            return;
        }

        var block = headLocation.getLocation().getBlock();
        if (block.getType() != Material.PLAYER_HEAD) {
            return;
        }

        var currentRotation = InternalUtils.getKeyByValue(HeadUtils.skullRotationList, HeadUtils.getRotation(block));
        if (currentRotation == null) {
            currentRotation = 0;
        }

        var rotation = HeadUtils.skullRotationList.get((currentRotation + 1) % HeadUtils.skullRotationList.size());
        HeadUtils.rotateHead(block, rotation);
    }

}
