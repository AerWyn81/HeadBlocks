package fr.aerwyn81.headblocks.storages.types;

import fr.aerwyn81.headblocks.storages.Storage;

import java.util.*;

public class Memory implements Storage {
    private HashMap<UUID, ArrayList<UUID>> headsFound;

    public Memory() {
    }

    public void init() {
        headsFound = new HashMap<>();
    }

    public void close() {
    }

    public boolean hasAlreadyClaimedHead(UUID pUuid, UUID hUuid) {
        return containsPlayer(pUuid) && headsFound.get(pUuid).contains(hUuid);
    }

    public void savePlayer(UUID pUuid, UUID hUuid) {
        if (!containsPlayer(pUuid)) {
            headsFound.put(pUuid, new ArrayList<>(Collections.singletonList(hUuid)));
            return;
        }

        headsFound.get(pUuid).add(hUuid);
    }

    public boolean containsPlayer(UUID pUuid) {
        return headsFound.get(pUuid) != null;
    }

    public List<UUID> getHeadsPlayer(UUID pUuid) {
        return containsPlayer(pUuid) ? headsFound.get(pUuid) : new ArrayList<>();
    }

    public void resetPlayer(UUID pUuid) {
        headsFound.remove(pUuid);
    }

    public void removeHead(UUID hUuid) {
        headsFound.values().forEach(u -> u.remove(hUuid));
    }
}
