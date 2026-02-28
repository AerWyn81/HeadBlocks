package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.data.hunt.Hunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;

import java.util.*;
import java.util.stream.Collectors;

public class HuntService {
    private static final Map<String, Hunt> huntsById = new LinkedHashMap<>();
    private static final Map<UUID, List<Hunt>> headToHunts = new HashMap<>();
    private static final Map<UUID, String> selectedHunt = new HashMap<>();
    private static long knownHuntVersion = 0;

    public static void initialize() {
        huntsById.clear();
        headToHunts.clear();
        selectedHunt.clear();

        // 1. Load hunts from YAML files
        List<Hunt> fileHunts = HuntConfigService.loadHunts();

        // 2. Ensure default hunt exists
        boolean hasDefault = fileHunts.stream().anyMatch(h -> "default".equals(h.getId()));
        if (!hasDefault) {
            Hunt defaultHunt = createDefaultHunt();
            fileHunts.add(0, defaultHunt);
        }

        // 3. Register hunts
        for (Hunt hunt : fileHunts) {
            huntsById.put(hunt.getId(), hunt);
        }

        // 4. Sync hunt registry with database
        syncHuntsWithDb();

        // 5. Load head-hunt mappings from DB and populate Hunt.headUUIDs
        loadHeadMappingsFromDb();

        // 6. Build headToHunts cache
        rebuildHeadToHuntsCache();

        // 7. Snapshot current hunt version
        knownHuntVersion = StorageService.getHuntVersion();

        LogUtil.info("Loaded {0} hunt(s): [{1}]", huntsById.size(),
                huntsById.values().stream().map(Hunt::getId).collect(Collectors.joining(", ")));
    }

    private static Hunt createDefaultHunt() {
        Hunt hunt = new Hunt("default", "Default", HuntState.ACTIVE, 0, "PLAYER_HEAD");
        HuntConfigService.saveHunt(hunt);
        return hunt;
    }

    private static void syncHuntsWithDb() {
        try {
            Set<String> dbHuntIds = new HashSet<>();
            for (String[] row : StorageService.getHuntsFromDb()) {
                dbHuntIds.add(row[0]);
            }

            for (Hunt hunt : huntsById.values()) {
                if (!dbHuntIds.contains(hunt.getId())) {
                    StorageService.createHuntInDb(hunt.getId(), hunt.getDisplayName(), hunt.getState().name());
                }
            }
        } catch (Exception e) {
            LogUtil.error("Failed to sync hunts with database: {0}", e.getMessage());
        }
    }

    private static void loadHeadMappingsFromDb() {
        for (Hunt hunt : huntsById.values()) {
            try {
                ArrayList<UUID> heads = StorageService.getHeadsForHunt(hunt.getId());
                for (UUID headUUID : heads) {
                    hunt.addHead(headUUID);
                }
            } catch (Exception e) {
                LogUtil.error("Failed to load head mappings for hunt {0}: {1}", hunt.getId(), e.getMessage());
            }
        }
    }

    public static void rebuildHeadToHuntsCache() {
        headToHunts.clear();
        for (Hunt hunt : huntsById.values()) {
            for (UUID headUUID : hunt.getHeadUUIDs()) {
                headToHunts.computeIfAbsent(headUUID, k -> new ArrayList<>()).add(hunt);
            }
        }
        // Sort by priority descending (highest priority first)
        for (List<Hunt> hunts : headToHunts.values()) {
            hunts.sort(Comparator.comparingInt(Hunt::getPriority).reversed());
        }
    }

    // --- Getters (O(1) cache) ---

    public static Hunt getHuntById(String huntId) {
        return huntsById.get(huntId);
    }

    public static Hunt getDefaultHunt() {
        return huntsById.get("default");
    }

    public static List<Hunt> getHuntsForHead(UUID headUUID) {
        return headToHunts.getOrDefault(headUUID, Collections.emptyList());
    }

    public static Hunt getHighestPriorityHuntForHead(UUID headUUID) {
        List<Hunt> hunts = headToHunts.get(headUUID);
        if (hunts == null || hunts.isEmpty()) return null;
        // Return first active hunt (list sorted by priority desc)
        for (Hunt hunt : hunts) {
            if (hunt.isActive()) return hunt;
        }
        return null;
    }

    public static Collection<Hunt> getAllHunts() {
        return Collections.unmodifiableCollection(huntsById.values());
    }

    public static List<Hunt> getActiveHunts() {
        return huntsById.values().stream()
                .filter(Hunt::isActive)
                .collect(Collectors.toList());
    }

    public static boolean isMultiHunt() {
        return huntsById.size() > 1;
    }

    public static boolean huntExists(String huntId) {
        return huntsById.containsKey(huntId);
    }

    public static int getHuntCount() {
        return huntsById.size();
    }

    // --- Runtime mutation ---

    public static void registerHunt(Hunt hunt) {
        huntsById.put(hunt.getId(), hunt);
        rebuildHeadToHuntsCache();
    }

    public static void unregisterHunt(String huntId) {
        Hunt removed = huntsById.remove(huntId);
        if (removed != null) {
            rebuildHeadToHuntsCache();
        }
    }

    public static List<String> getHuntNames() {
        return new ArrayList<>(huntsById.keySet());
    }

    // --- Session-based selected hunt ---

    public static void setSelectedHunt(UUID playerUUID, String huntId) {
        selectedHunt.put(playerUUID, huntId);
    }

    public static String getSelectedHunt(UUID playerUUID) {
        return selectedHunt.getOrDefault(playerUUID, "default");
    }

    public static void clearSelectedHunt(UUID playerUUID) {
        selectedHunt.remove(playerUUID);
    }

    // --- Head transfer between hunts ---

    public static void transferHead(UUID headUUID, String toHuntId) throws Exception {
        Hunt toHunt = huntsById.get(toHuntId);
        if (toHunt == null) {
            throw new IllegalArgumentException("Target hunt not found: " + toHuntId);
        }

        // Remove from current hunt(s)
        for (Hunt hunt : huntsById.values()) {
            if (hunt.containsHead(headUUID)) {
                hunt.removeHead(headUUID);
                StorageService.unlinkHeadFromHunt(headUUID, hunt.getId());
            }
        }

        // Add to target hunt
        toHunt.addHead(headUUID);
        StorageService.linkHeadToHunt(headUUID, toHuntId);

        rebuildHeadToHuntsCache();
    }

    /**
     * Checks if the hunt version in shared storage has changed (e.g. another server
     * created/deleted/toggled a hunt). If so, re-initializes the hunt registry.
     * Designed to be called periodically from GlobalTask.
     *
     * @return true if a re-sync was triggered
     */
    public static boolean checkRemoteChanges() {
        try {
            long remoteVersion = StorageService.getHuntVersion();
            if (remoteVersion != knownHuntVersion) {
                LogUtil.info("Hunt version changed ({0} -> {1}), re-syncing hunts...", knownHuntVersion, remoteVersion);
                initialize();
                return true;
            }
        } catch (Exception e) {
            LogUtil.error("Failed to check remote hunt version: {0}", e.getMessage());
        }
        return false;
    }

    public static void assignHeadToHunt(UUID headUUID, String huntId) throws Exception {
        Hunt hunt = huntsById.get(huntId);
        if (hunt == null) {
            throw new IllegalArgumentException("Hunt not found: " + huntId);
        }

        hunt.addHead(headUUID);
        StorageService.linkHeadToHunt(headUUID, huntId);
        rebuildHeadToHuntsCache();
    }
}
