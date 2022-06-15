package fr.aerwyn81.headblocks.storages;

import fr.aerwyn81.headblocks.utils.InternalException;

import java.util.UUID;

public interface Storage {

    void init() throws InternalException;

    void close() throws InternalException;

    boolean hasAlreadyClaimedHead(UUID playerUuid, UUID headUuid) throws InternalException;

    boolean containsPlayer(UUID playerUuid) throws InternalException;

    void savePlayer(UUID playerUuid, UUID headUuid) throws InternalException;

    void resetPlayer(UUID playerUuid) throws InternalException;

    void removeHead(UUID headUuid) throws InternalException;
}
