package fr.aerwyn81.headblocks.databases;

import fr.aerwyn81.headblocks.data.PlayerProfileLight;
import fr.aerwyn81.headblocks.utils.internal.InternalException;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;

public interface Database {
    int version = 5;

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

    // --- Hunt CRUD (v5) ---

    void createHunt(String huntId, String name, String state) throws InternalException;

    void updateHuntState(String huntId, String state) throws InternalException;

    void updateHuntName(String huntId, String name) throws InternalException;

    void deleteHunt(String huntId) throws InternalException;

    ArrayList<String[]> getHunts() throws InternalException;

    String[] getHuntById(String huntId) throws InternalException;

    // --- Head-Hunt linking (v5) ---

    void linkHeadToHunt(UUID headUUID, String huntId) throws InternalException;

    void unlinkHeadFromHunt(UUID headUUID, String huntId) throws InternalException;

    void unlinkAllHeadsFromHunt(String huntId) throws InternalException;

    ArrayList<String> getHuntsForHead(UUID headUUID) throws InternalException;

    ArrayList<UUID> getHeadsForHunt(String huntId) throws InternalException;

    // --- Hunt-aware player progression (v5) ---

    void addHeadForHunt(UUID pUUID, UUID hUUID, String huntId) throws InternalException;

    ArrayList<UUID> getHeadsPlayerForHunt(UUID pUUID, String huntId) throws InternalException;

    void resetPlayerHunt(UUID pUUID, String huntId) throws InternalException;

    void resetPlayerHeadHunt(UUID pUUID, UUID hUUID, String huntId) throws InternalException;

    LinkedHashMap<PlayerProfileLight, Integer> getTopPlayersForHunt(String huntId) throws InternalException;

    int getPlayerCountForHeadInHunt(UUID hUUID, String huntId) throws InternalException;

    // --- Migration v5 ---

    void addColumnHuntId() throws InternalException;

    void migrateToV5() throws InternalException;
}
