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
}
