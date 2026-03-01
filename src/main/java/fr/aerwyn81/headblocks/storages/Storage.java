package fr.aerwyn81.headblocks.storages;

import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.utils.internal.InternalException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.UUID;

public interface Storage {

    void init() throws InternalException;

    void close() throws InternalException;

    boolean containsPlayer(UUID playerUuid) throws InternalException;

    void resetPlayer(UUID playerUuid) throws InternalException;

    void resetPlayerHead(UUID playerUuid, UUID headUuid) throws InternalException;

    void addHead(UUID playerUuid, UUID headUuid) throws InternalException;

    boolean hasHead(UUID playerUuid, UUID headUuid) throws InternalException;

    void removeHead(UUID headUuid) throws InternalException;

    ArrayList<UUID> getHeadsPlayer(UUID pUuid) throws InternalException;

    // New cache methods for player heads
    Set<UUID> getCachedPlayerHeads(UUID playerUuid) throws InternalException;

    void setCachedPlayerHeads(UUID playerUuid, Set<UUID> heads) throws InternalException;

    void addCachedPlayerHead(UUID playerUuid, UUID headUuid) throws InternalException;

    void removeCachedPlayerHeads(UUID playerUuid) throws InternalException;

    // New cache methods for top players
    LinkedHashMap<PlayerProfileLight, Integer> getCachedTopPlayers() throws InternalException;

    void setCachedTopPlayers(LinkedHashMap<PlayerProfileLight, Integer> topPlayers) throws InternalException;

    void clearCachedTopPlayers() throws InternalException;

    // New cache methods for all heads
    Set<UUID> getCachedHeads() throws InternalException;

    void addCachedHead(UUID headUuid) throws InternalException;

    void removeCachedHead(UUID headUuid) throws InternalException;

    // --- Hunt-specific cache: player heads per hunt ---
    Set<UUID> getCachedPlayerHeadsForHunt(UUID playerUuid, String huntId) throws InternalException;

    void setCachedPlayerHeadsForHunt(UUID playerUuid, String huntId, Set<UUID> heads) throws InternalException;

    void addCachedPlayerHeadForHunt(UUID playerUuid, String huntId, UUID headUuid) throws InternalException;

    void removeCachedPlayerHeadsForHunt(UUID playerUuid, String huntId) throws InternalException;

    void clearCachedPlayerHeadsForHunt(String huntId) throws InternalException;

    void clearAllCachedHuntDataForPlayer(UUID playerUuid) throws InternalException;

    // --- Hunt-specific cache: top players per hunt ---
    LinkedHashMap<PlayerProfileLight, Integer> getCachedTopPlayersForHunt(String huntId) throws InternalException;

    void setCachedTopPlayersForHunt(String huntId, LinkedHashMap<PlayerProfileLight, Integer> topPlayers) throws InternalException;

    void clearCachedTopPlayersForHunt(String huntId) throws InternalException;

    void clearAllCachedTopPlayersForHunt() throws InternalException;

    // --- Hunt-specific cache: timed leaderboard ---
    LinkedHashMap<PlayerProfileLight, Long> getCachedTimedLeaderboard(String huntId) throws InternalException;

    void setCachedTimedLeaderboard(String huntId, LinkedHashMap<PlayerProfileLight, Long> lb) throws InternalException;

    void clearCachedTimedLeaderboard(String huntId) throws InternalException;

    // --- Hunt-specific cache: best time (sentinel -1L = "no run, but cached") ---
    Long getCachedBestTime(UUID playerUuid, String huntId) throws InternalException;

    void setCachedBestTime(UUID playerUuid, String huntId, Long timeMs) throws InternalException;

    void clearCachedBestTime(UUID playerUuid, String huntId) throws InternalException;

    // --- Hunt-specific cache: run count ---
    Integer getCachedTimedRunCount(UUID playerUuid, String huntId) throws InternalException;

    void setCachedTimedRunCount(UUID playerUuid, String huntId, int count) throws InternalException;

    void clearCachedTimedRunCount(UUID playerUuid, String huntId) throws InternalException;

    // Hunt sync version (cross-server)
    long getHuntVersion() throws InternalException;

    void incrementHuntVersion() throws InternalException;
}
