package fr.aerwyn81.headblocks.databases;

import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.UUID;

public interface Database {

    void close();

    void open();

    void load();

    boolean hasHead(UUID playerUuid, UUID headUuid);

    boolean containsPlayer(UUID playerUuid);

    ArrayList<UUID> getHeadsPlayer(UUID playerUuid);

    void savePlayer(UUID playerUuid, UUID headUuid);

    void resetPlayer(UUID playerUuid);

    void removeHead(UUID headUuid);

    ArrayList<UUID> getAllPlayers();

    ArrayList<Pair<UUID, Integer>> getTopPlayers(int limit);
}
