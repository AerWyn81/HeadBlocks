package fr.aerwyn81.headblocks.storages.types;

import fr.aerwyn81.headblocks.storages.Storage;
import fr.aerwyn81.headblocks.utils.internal.InternalException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

public class Memory implements Storage {

    private HashMap<UUID, ArrayList<UUID>> headsFound;

    public Memory() { }

    @Override
    public void init() {
        headsFound = new HashMap<>();
    }

    @Override
    public void close() throws InternalException { }

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
}
