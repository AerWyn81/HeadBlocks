package fr.aerwyn81.headblocks.services;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ZoneRunManager {
    private static final ConcurrentHashMap<UUID, String> engaged = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, String> released = new ConcurrentHashMap<>();

    public static void engage(UUID playerUuid, String huntId) {
        engaged.put(playerUuid, huntId);
    }

    public static void disengage(UUID playerUuid) {
        engaged.remove(playerUuid);
    }

    public static String getEngaged(UUID playerUuid) {
        return engaged.get(playerUuid);
    }

    public static boolean isEngaged(UUID playerUuid) {
        return engaged.containsKey(playerUuid);
    }

    public static void markReleased(UUID playerUuid, String huntId) {
        released.put(playerUuid, huntId);
    }

    public static boolean isReleased(UUID playerUuid, String huntId) {
        return huntId.equals(released.get(playerUuid));
    }

    public static void clearReleased(UUID playerUuid) {
        released.remove(playerUuid);
    }

    public static void clear(UUID playerUuid) {
        engaged.remove(playerUuid);
        released.remove(playerUuid);
    }

    public static void clearAll() {
        engaged.clear();
        released.clear();
    }
}
