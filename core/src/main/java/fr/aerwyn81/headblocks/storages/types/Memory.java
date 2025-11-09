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

    public Memory() { }

    @Override
    public void init() {
        headsFound = new HashMap<>();
        cachePlayerHeads = new ConcurrentHashMap<>();
        cacheTopPlayers = new LinkedHashMap<>();
        cacheHeads = ConcurrentHashMap.newKeySet();
    }

    @Override
    public void close() throws InternalException {
        headsFound.clear();
        cachePlayerHeads.clear();
        cacheTopPlayers.clear();
        cacheHeads.clear();
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
    }
}
