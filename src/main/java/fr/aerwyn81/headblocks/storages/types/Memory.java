package fr.aerwyn81.headblocks.storages.types;

import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.storages.Storage;
import fr.aerwyn81.headblocks.utils.internal.InternalException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Memory implements Storage {

    private HashMap<UUID, ArrayList<UUID>> headsFound;
    private ConcurrentHashMap<UUID, Set<UUID>> cachePlayerHeads;
    private LinkedHashMap<PlayerProfileLight, Integer> cacheTopPlayers;
    private Set<UUID> cacheHeads;

    // Hunt-specific caches
    private ConcurrentHashMap<String, ConcurrentHashMap<UUID, Set<UUID>>> cacheHuntPlayerHeads;
    private ConcurrentHashMap<String, LinkedHashMap<PlayerProfileLight, Integer>> cacheHuntTopPlayers;
    private ConcurrentHashMap<String, LinkedHashMap<PlayerProfileLight, Long>> cacheTimedLeaderboard;
    private ConcurrentHashMap<String, Long> cacheBestTime;
    private ConcurrentHashMap<String, Integer> cacheTimedRunCount;

    public Memory() {
    }

    @Override
    public void init() {
        headsFound = new HashMap<>();
        cachePlayerHeads = new ConcurrentHashMap<>();
        cacheTopPlayers = new LinkedHashMap<>();
        cacheHeads = ConcurrentHashMap.newKeySet();

        cacheHuntPlayerHeads = new ConcurrentHashMap<>();
        cacheHuntTopPlayers = new ConcurrentHashMap<>();
        cacheTimedLeaderboard = new ConcurrentHashMap<>();
        cacheBestTime = new ConcurrentHashMap<>();
        cacheTimedRunCount = new ConcurrentHashMap<>();
    }

    @Override
    public void close() throws InternalException {
        headsFound.clear();
        cachePlayerHeads.clear();
        cacheTopPlayers.clear();
        cacheHeads.clear();

        cacheHuntPlayerHeads.clear();
        cacheHuntTopPlayers.clear();
        cacheTimedLeaderboard.clear();
        cacheBestTime.clear();
        cacheTimedRunCount.clear();
    }

    @Override
    public boolean hasHead(UUID playerUuid, UUID headUuid) {
        return containsPlayer(playerUuid) && headsFound.get(playerUuid).contains(headUuid);
    }

    @Override
    public boolean containsPlayer(UUID playerUuid) {
        return headsFound.get(playerUuid) != null;
    }

    @Override
    public void addHead(UUID playerUuid, UUID headUuid) {
        if (!containsPlayer(playerUuid)) {
            headsFound.put(playerUuid, new ArrayList<>(Collections.singletonList(headUuid)));
            return;
        }

        headsFound.get(playerUuid).add(headUuid);
    }

    @Override
    public void resetPlayer(UUID playerUuid) {
        headsFound.remove(playerUuid);
    }

    @Override
    public void resetPlayerHead(UUID playerUuid, UUID headUuid) {
        if (containsPlayer(playerUuid)) {
            headsFound.get(playerUuid).remove(headUuid);
        }
    }

    @Override
    public void removeHead(UUID headUuid) {
        headsFound.values().forEach(u -> u.remove(headUuid));
    }

    @Override
    public ArrayList<UUID> getHeadsPlayer(UUID pUuid) throws InternalException {
        return containsPlayer(pUuid) ? headsFound.get(pUuid) : new ArrayList<>();
    }

    @Override
    public Set<UUID> getCachedPlayerHeads(UUID playerUuid) {
        return cachePlayerHeads.get(playerUuid);
    }

    @Override
    public void setCachedPlayerHeads(UUID playerUuid, Set<UUID> heads) {
        cachePlayerHeads.put(playerUuid, heads);
    }

    @Override
    public void addCachedPlayerHead(UUID playerUuid, UUID headUuid) {
        if (!cachePlayerHeads.containsKey(playerUuid)) {
            cachePlayerHeads.put(playerUuid, ConcurrentHashMap.newKeySet());
        }
        cachePlayerHeads.get(playerUuid).add(headUuid);
    }

    @Override
    public void removeCachedPlayerHeads(UUID playerUuid) {
        cachePlayerHeads.remove(playerUuid);
    }

    @Override
    public LinkedHashMap<PlayerProfileLight, Integer> getCachedTopPlayers() {
        return cacheTopPlayers;
    }

    @Override
    public void setCachedTopPlayers(LinkedHashMap<PlayerProfileLight, Integer> topPlayers) {
        cacheTopPlayers.clear();
        cacheTopPlayers.putAll(topPlayers);
    }

    @Override
    public void clearCachedTopPlayers() {
        cacheTopPlayers.clear();
    }

    @Override
    public Set<UUID> getCachedHeads() {
        return cacheHeads;
    }

    @Override
    public void addCachedHead(UUID headUuid) {
        cacheHeads.add(headUuid);
    }

    @Override
    public void removeCachedHead(UUID headUuid) {
        cacheHeads.remove(headUuid);

        cachePlayerHeads.values().forEach(playerHeads -> playerHeads.remove(headUuid));

        clearCachedTopPlayers();
    }

    // --- Hunt-specific cache: player heads per hunt ---

    @Override
    public Set<UUID> getCachedPlayerHeadsForHunt(UUID playerUuid, String huntId) {
        var huntMap = cacheHuntPlayerHeads.get(huntId);
        if (huntMap == null) {
            return null;
        }
        return huntMap.get(playerUuid);
    }

    @Override
    public void setCachedPlayerHeadsForHunt(UUID playerUuid, String huntId, Set<UUID> heads) {
        cacheHuntPlayerHeads.computeIfAbsent(huntId, k -> new ConcurrentHashMap<>()).put(playerUuid, heads);
    }

    @Override
    public void addCachedPlayerHeadForHunt(UUID playerUuid, String huntId, UUID headUuid) {
        var huntMap = cacheHuntPlayerHeads.computeIfAbsent(huntId, k -> new ConcurrentHashMap<>());
        huntMap.computeIfAbsent(playerUuid, k -> ConcurrentHashMap.newKeySet()).add(headUuid);
    }

    @Override
    public void removeCachedPlayerHeadsForHunt(UUID playerUuid, String huntId) {
        var huntMap = cacheHuntPlayerHeads.get(huntId);
        if (huntMap != null) {
            huntMap.remove(playerUuid);
        }
    }

    @Override
    public void clearCachedPlayerHeadsForHunt(String huntId) {
        cacheHuntPlayerHeads.remove(huntId);
    }

    @Override
    public void clearAllCachedHuntDataForPlayer(UUID playerUuid) {
        cacheHuntPlayerHeads.values().forEach(huntMap -> huntMap.remove(playerUuid));
        cacheBestTime.entrySet().removeIf(e -> e.getKey().endsWith(":" + playerUuid.toString()));
        cacheTimedRunCount.entrySet().removeIf(e -> e.getKey().endsWith(":" + playerUuid.toString()));
    }

    // --- Hunt-specific cache: top players per hunt ---

    @Override
    public LinkedHashMap<PlayerProfileLight, Integer> getCachedTopPlayersForHunt(String huntId) {
        return cacheHuntTopPlayers.get(huntId);
    }

    @Override
    public void setCachedTopPlayersForHunt(String huntId, LinkedHashMap<PlayerProfileLight, Integer> topPlayers) {
        cacheHuntTopPlayers.put(huntId, topPlayers);
    }

    @Override
    public void clearCachedTopPlayersForHunt(String huntId) {
        cacheHuntTopPlayers.remove(huntId);
    }

    @Override
    public void clearAllCachedTopPlayersForHunt() {
        cacheHuntTopPlayers.clear();
    }

    // --- Hunt-specific cache: timed leaderboard ---

    @Override
    public LinkedHashMap<PlayerProfileLight, Long> getCachedTimedLeaderboard(String huntId) {
        return cacheTimedLeaderboard.get(huntId);
    }

    @Override
    public void setCachedTimedLeaderboard(String huntId, LinkedHashMap<PlayerProfileLight, Long> lb) {
        cacheTimedLeaderboard.put(huntId, lb);
    }

    @Override
    public void clearCachedTimedLeaderboard(String huntId) {
        cacheTimedLeaderboard.remove(huntId);
    }

    // --- Hunt-specific cache: best time ---

    @Override
    public Long getCachedBestTime(UUID playerUuid, String huntId) {
        return cacheBestTime.get(huntId + ":" + playerUuid.toString());
    }

    @Override
    public void setCachedBestTime(UUID playerUuid, String huntId, Long timeMs) {
        cacheBestTime.put(huntId + ":" + playerUuid.toString(), timeMs);
    }

    @Override
    public void clearCachedBestTime(UUID playerUuid, String huntId) {
        cacheBestTime.remove(huntId + ":" + playerUuid.toString());
    }

    // --- Hunt-specific cache: run count ---

    @Override
    public Integer getCachedTimedRunCount(UUID playerUuid, String huntId) {
        return cacheTimedRunCount.get(huntId + ":" + playerUuid.toString());
    }

    @Override
    public void setCachedTimedRunCount(UUID playerUuid, String huntId, int count) {
        cacheTimedRunCount.put(huntId + ":" + playerUuid.toString(), count);
    }

    @Override
    public void clearCachedTimedRunCount(UUID playerUuid, String huntId) {
        cacheTimedRunCount.remove(huntId + ":" + playerUuid.toString());
    }

    @Override
    public long getHuntVersion() {
        return 0;
    }

    @Override
    public void incrementHuntVersion() {
        // No-op for single-server Memory storage
    }
}
