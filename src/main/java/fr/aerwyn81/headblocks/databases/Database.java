package fr.aerwyn81.headblocks.databases;

import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.utils.internal.InternalException;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;

public interface Database {
    int version = 4;

    void close() throws InternalException;

    void open() throws InternalException;

    void load() throws InternalException;

    int checkVersion();

    void updatePlayerInfo(PlayerProfileLight profile) throws InternalException;

    void createNewHead(UUID hUUID, String texture, String serverIdentifier) throws InternalException;

    boolean containsPlayer(UUID pUUID) throws InternalException;

    ArrayList<UUID> getHeadsPlayer(UUID pUUID) throws InternalException;

    void addHead(UUID pUUID, UUID hUUID) throws InternalException;

    void resetPlayer(UUID pUUID) throws InternalException;

    void resetPlayerHead(UUID pUUID, UUID hUUID) throws InternalException;

    void removeHead(UUID hUUID, boolean withDelete) throws InternalException;

    ArrayList<UUID> getAllPlayers() throws InternalException;

    LinkedHashMap<PlayerProfileLight, Integer> getTopPlayers() throws InternalException;

    boolean hasPlayerRenamed(PlayerProfileLight profile) throws InternalException;

    boolean isHeadExist(UUID headUuid) throws InternalException;

    void migrate() throws InternalException;

    void upsertTableVersion(int oldVersion) throws InternalException;

    ArrayList<AbstractMap.SimpleEntry<String, Boolean>> getTableHeads() throws InternalException;

    ArrayList<AbstractMap.SimpleEntry<String, String>> getTablePlayerHeads() throws InternalException;

    ArrayList<AbstractMap.SimpleEntry<String, String>> getTablePlayers() throws InternalException;

    void addColumnHeadTexture() throws InternalException;

    String getHeadTexture(UUID headUuid) throws InternalException;

    ArrayList<UUID> getPlayers(UUID headUuid) throws InternalException;

    PlayerProfileLight getPlayerByName(String pName) throws InternalException;

    boolean isDefaultTablesExist();

    void insertVersion() throws InternalException;

    void addColumnDisplayName() throws InternalException;

    ArrayList<UUID> getHeads() throws InternalException;

    ArrayList<UUID> getHeads(String serverId) throws InternalException;

    void addColumnServerIdentifier() throws InternalException;

    ArrayList<String> getDistinctServerIds() throws InternalException;
}
