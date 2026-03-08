package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.data.hunt.HBHunt;
import fr.aerwyn81.headblocks.data.hunt.HuntState;
import fr.aerwyn81.headblocks.utils.internal.LogUtil;

import java.util.*;
import java.util.stream.Collectors;

public class HuntService {
    private final ConfigService configService;
    private final HuntConfigService huntConfigService;
    private final StorageService storageService;

    private final Map<String, HBHunt> huntsById = new LinkedHashMap<>();
    private final Map<UUID, String> selectedHunt = new HashMap<>();
    private long knownHuntVersion = 0;

    // --- Constructor ---

    public HuntService(ConfigService configService, HuntConfigService huntConfigService, StorageService storageService) {
        this.configService = configService;
        this.huntConfigService = huntConfigService;
        this.storageService = storageService;

        initialize();
    }

    // --- Instance methods ---

    public void initialize() {
        huntsById.clear();
        selectedHunt.clear();

        List<HBHunt> fileHunts = huntConfigService.loadHunts();

        boolean hasDefault = fileHunts.stream().anyMatch(h -> "default".equals(h.getId()));
        if (!hasDefault) {
            HBHunt defaultHunt = createDefaultHunt();
            fileHunts.add(0, defaultHunt);
        }

        for (HBHunt hunt : fileHunts) {
            huntsById.put(hunt.getId(), hunt);
        }

        syncHuntsWithDb();

        knownHuntVersion = storageService.getHuntVersion();

        LogUtil.info("Loaded {0} hunt(s): [{1}]", huntsById.size(),
                huntsById.values().stream().map(HBHunt::getId).collect(Collectors.joining(", ")));
    }

    private HBHunt createDefaultHunt() {
        HBHunt hunt = new HBHunt(configService, "default", "Default", HuntState.ACTIVE, 0, "PLAYER_HEAD");
        huntConfigService.saveHunt(hunt);
        return hunt;
    }

    private void syncHuntsWithDb() {
        try {
            Set<String> dbHuntIds = new HashSet<>();
            for (String[] row : storageService.getHuntsFromDb()) {
                dbHuntIds.add(row[0]);
            }

            for (HBHunt hunt : huntsById.values()) {
                if (!dbHuntIds.contains(hunt.getId())) {
                    storageService.createHuntInDb(hunt.getId(), hunt.getDisplayName(), hunt.getState().name());
                }
            }
        } catch (Exception e) {
            LogUtil.error("Failed to sync hunts with database: {0}", e.getMessage());
        }
    }

    // --- Getters ---

    public HBHunt getHuntById(String huntId) {
        return huntsById.get(huntId);
    }

    public HBHunt getDefaultHunt() {
        return huntsById.get("default");
    }

    public Collection<HBHunt> getAllHunts() {
        return Collections.unmodifiableCollection(huntsById.values());
    }

    public List<HBHunt> getActiveHunts() {
        return huntsById.values().stream()
                .filter(HBHunt::isActive)
                .collect(Collectors.toList());
    }

    public boolean isMultiHunt() {
        return huntsById.size() > 1;
    }

    public boolean huntExists(String huntId) {
        return huntsById.containsKey(huntId);
    }

    public int getHuntCount() {
        return huntsById.size();
    }

    // --- Runtime mutation ---

    public void registerHunt(HBHunt hunt) {
        huntsById.put(hunt.getId(), hunt);
    }

    public void unregisterHunt(String huntId) {
        huntsById.remove(huntId);
    }

    public List<String> getHuntNames() {
        return new ArrayList<>(huntsById.keySet());
    }

    // --- Session-based selected hunt ---

    public void setSelectedHunt(UUID playerUUID, String huntId) {
        selectedHunt.put(playerUUID, huntId);
    }

    public String getSelectedHunt(UUID playerUUID) {
        return selectedHunt.getOrDefault(playerUUID, "default");
    }

    public void clearSelectedHunt(UUID playerUUID) {
        selectedHunt.remove(playerUUID);
    }

    // --- Head transfer ---

    public void transferHead(HeadLocation headLocation, String toHuntId) {
        HBHunt toHunt = huntsById.get(toHuntId);
        if (toHunt == null) {
            throw new IllegalArgumentException("Target hunt not found: " + toHuntId);
        }

        // Remove from source hunt
        String sourceHuntId = headLocation.getHuntId();
        HBHunt sourceHunt = huntsById.get(sourceHuntId);
        if (sourceHunt != null) {
            sourceHunt.removeHead(headLocation.getUuid());
        }
        huntConfigService.removeLocationFromHunt(sourceHuntId, headLocation.getUuid());

        // Add to target hunt
        headLocation.setHuntId(toHuntId);
        toHunt.addHead(headLocation.getUuid());
        huntConfigService.saveLocationInHunt(toHuntId, headLocation);

        storageService.incrementHuntVersion();
    }

    public void checkRemoteChanges() {
        try {
            long remoteVersion = storageService.getHuntVersion();
            if (remoteVersion != knownHuntVersion) {
                LogUtil.info("Hunt version changed ({0} -> {1}), re-syncing hunts...", knownHuntVersion, remoteVersion);
                initialize();
            }
        } catch (Exception e) {
            LogUtil.error("Failed to check remote hunt version: {0}", e.getMessage());
        }
    }

}
