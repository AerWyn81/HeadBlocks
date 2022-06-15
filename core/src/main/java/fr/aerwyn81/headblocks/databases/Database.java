package fr.aerwyn81.headblocks.databases;

import fr.aerwyn81.headblocks.utils.InternalException;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.UUID;

public interface Database {

    void close() throws InternalException;

    void open() throws InternalException;

    void load() throws InternalException;

    void updatePlayerInfo(UUID pUUID, String pName) throws InternalException;

    void createNewHead(UUID hUUID) throws InternalException;

    boolean hasHead(UUID pUUID, UUID hUUID) throws InternalException;

    boolean containsPlayer(UUID pUUID) throws InternalException;

    ArrayList<UUID> getHeadsPlayer(UUID pUUID) throws InternalException;

    void savePlayer(UUID pUUID, UUID hUUID) throws InternalException;

    void resetPlayer(UUID pUUID) throws InternalException;

    void removeHead(UUID hUUID) throws InternalException;

    ArrayList<UUID> getAllPlayers() throws InternalException;

    ArrayList<Pair<String, Integer>> getTopPlayers(int limit) throws InternalException;
}
