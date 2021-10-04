package fr.aerwyn81.headblocks.storages;

import java.util.UUID;

public interface Storage {

    void init();

    void close();

    boolean hasAlreadyClaimedHead(UUID playerUuid, UUID headUuid);

    boolean containsPlayer(UUID playerUuid);

    void savePlayer(UUID playerUuid, UUID headUuid);

    void resetPlayer(UUID playerUuid);

    void removeHead(UUID headUuid);
}
