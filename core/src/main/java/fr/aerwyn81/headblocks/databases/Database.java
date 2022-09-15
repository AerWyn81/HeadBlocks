package fr.aerwyn81.headblocks.databases;

import fr.aerwyn81.headblocks.utils.internal.InternalException;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public interface Database {
    int version = 2;

    void close() throws InternalException;

    void open() throws InternalException;

    void load() throws InternalException;

    int checkVersion();

    void updatePlayerInfo(UUID pUUID, String pName) throws InternalException;

    void createNewHead(UUID hUUID, String texture) throws InternalException;

    boolean containsPlayer(UUID pUUID) throws InternalException;

    ArrayList<UUID> getHeadsPlayer(UUID pUUID) throws InternalException;

    void addHead(UUID pUUID, UUID hUUID) throws InternalException;

    void resetPlayer(UUID pUUID) throws InternalException;

    void removeHead(UUID hUUID, boolean withDelete) throws InternalException;

    ArrayList<UUID> getAllPlayers() throws InternalException;

    Map<String, Integer> getTopPlayers(int limit) throws InternalException;

    boolean hasPlayerRenamed(UUID pUUID, String playerName) throws InternalException;

    boolean isHeadExist(UUID headUuid) throws InternalException;

    void migrate() throws InternalException;

    void upsertTableVersion() throws InternalException;

    ArrayList<AbstractMap.SimpleEntry<String, Boolean>> getTableHeads() throws InternalException;

    ArrayList<AbstractMap.SimpleEntry<String, String>> getTablePlayerHeads() throws InternalException;

    ArrayList<AbstractMap.SimpleEntry<String, String>> getTablePlayers() throws InternalException;

    void addColumnHeadTexture() throws InternalException;

    String getHeadTexture(UUID headUuid) throws InternalException;

    ArrayList<UUID> getPlayers(UUID headUuid) throws InternalException;
}
