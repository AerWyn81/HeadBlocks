package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.data.TimedRunData;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TimedRunManager {
    private static final ConcurrentHashMap<UUID, TimedRunData> activeRuns = new ConcurrentHashMap<>();

    public static void startRun(UUID playerUuid, String huntId) {
        activeRuns.put(playerUuid, new TimedRunData(huntId, System.currentTimeMillis()));
    }

    public static void leaveRun(UUID playerUuid) {
        activeRuns.remove(playerUuid);
    }

    public static boolean isInRun(UUID playerUuid) {
        return activeRuns.containsKey(playerUuid);
    }

    public static boolean isInRun(UUID playerUuid, String huntId) {
        TimedRunData data = activeRuns.get(playerUuid);
        return data != null && data.huntId().equals(huntId);
    }

    public static TimedRunData getRun(UUID playerUuid) {
        return activeRuns.get(playerUuid);
    }

    public static long getElapsedMillis(UUID playerUuid) {
        TimedRunData data = activeRuns.get(playerUuid);
        if (data == null) {
            return 0;
        }

        return System.currentTimeMillis() - data.startTimeMillis();
    }

    public static ConcurrentHashMap<UUID, TimedRunData> getActiveRuns() {
        return activeRuns;
    }

    public static String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long ms = millis % 1000;
        return String.format("%02d:%02d.%03d", minutes, seconds, ms);
    }

    public static void clearAll() {
        activeRuns.clear();
    }
}
