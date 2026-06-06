package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.data.TimedRunData;
import org.bukkit.Location;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TimedRunManager {
    private static final ConcurrentHashMap<UUID, TimedRunData> activeRuns = new ConcurrentHashMap<>();

    public static void startRun(UUID playerUuid, String huntId) {
        startRun(playerUuid, huntId, 0f);
    }

    public static void startRun(UUID playerUuid, String huntId, float yaw) {
        activeRuns.put(playerUuid, new TimedRunData(huntId, System.currentTimeMillis(), yaw));
    }

    public static void leaveRun(UUID playerUuid) {
        activeRuns.remove(playerUuid);
    }

    public static void leaveAllForHunt(String huntId) {
        activeRuns.entrySet().removeIf(entry -> entry.getValue().huntId().equals(huntId));
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

    public static long getRemainingMillis(long elapsedMillis, int limitSeconds) {
        if (limitSeconds <= 0) {
            return Long.MAX_VALUE;
        }

        return (long) limitSeconds * 1000L - elapsedMillis;
    }

    // Horizontal offset (one block) on the approach side of the plate, i.e. opposite to the
    // direction the player was facing when they started. Index 0 = X, index 1 = Z.
    public static double[] backwardOffset(float yaw) {
        double rad = Math.toRadians(yaw);
        return new double[]{Math.sin(rad), -Math.cos(rad)};
    }

    // One block off the plate on the approach side (so it does not re-trigger), facing the
    // direction the player was looking when they started (toward the course). Pitch is forced
    // level since the start pitch is unreliable (the player may be looking down while stepping on).
    public static Location buildReturnLocation(Location plate, float yaw) {
        double[] offset = backwardOffset(yaw);
        return new Location(plate.getWorld(),
                plate.getBlockX() + 0.5 + offset[0],
                plate.getBlockY(),
                plate.getBlockZ() + 0.5 + offset[1],
                yaw, 0f);
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
