package fr.aerwyn81.headblocks.databases;

import java.util.List;
import java.util.UUID;

public interface Database {

    void close();

    void open();

    void load();

    boolean hasHead(UUID playerUuid, UUID headUuid);

    boolean containsPlayer(UUID playerUuid);

    List<UUID> getHeadsPlayer(UUID playerUuid);

    void savePlayer(UUID playerUuid, UUID headUuid);

    void resetPlayer(UUID playerUuid);

    void removeHead(UUID headUuid);

    List<UUID> getAllPlayers();
}
