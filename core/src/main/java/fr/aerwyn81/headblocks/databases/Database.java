package fr.aerwyn81.headblocks.databases;

import fr.aerwyn81.headblocks.utils.InternalException;

import java.util.ArrayList;
import java.util.Map;
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

    void addHead(UUID pUUID, UUID hUUID) throws InternalException;

    void resetPlayer(UUID pUUID) throws InternalException;

    void removeHead(UUID hUUID, boolean withDelete) throws InternalException;

    ArrayList<UUID> getAllPlayers() throws InternalException;

    Map<String, Integer> getTopPlayers(int limit) throws InternalException;

    boolean hasPlayerRenamed(UUID pUUID, String playerName) throws InternalException;
}
