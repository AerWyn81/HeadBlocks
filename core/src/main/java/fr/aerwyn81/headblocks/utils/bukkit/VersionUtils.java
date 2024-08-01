package fr.aerwyn81.headblocks.utils.bukkit;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;

import java.util.Arrays;

public enum VersionUtils {
    v1_20_R1(120, 1201),
    v1_20_R2(1202),
    v1_20_R3(1203),
    v1_20_R4(1204),
    v1_20_R5(1205),
    v1_20_R6(1206),
    v1_21_R1(121, 1211);

    private static VersionUtils version;
    private final int[] versionId;

    private final int currentVersionId;
    VersionUtils(int... id) {
        this.versionId = id;
        this.currentVersionId = id[0];
    }

    public int getVersionId() {
        return currentVersionId;
    }

    public static VersionUtils getVersion() {
        if (version != null) {
            return version;
        }
        try {
            version = extractFromString(Bukkit.getBukkitVersion().split("\\.")[3]);
        } catch (Exception e) {
            try {
                version = extractFromString(Bukkit.getServer().getBukkitVersion().split("-")[0].replaceAll("\\.", ""));
            } catch (Exception ex) {
                HeadBlocks.log.sendMessage(MessageUtils.colorize("&c" + ex.getMessage()));
                version = v1_21_R1;
            }
        }

        return version;
    }

    private static VersionUtils extractFromString(String ver) throws IllegalArgumentException {
        for (VersionUtils version : VersionUtils.values())
            if (Arrays.stream(version.versionId).anyMatch(v -> ver.equals(String.valueOf(v))))
                return version;

        throw new RuntimeException("Unknown version " + ver + ". Please report to developer. HeadBlocks will use latest.");
    }

    public static boolean isAtLeastVersion(VersionUtils version) {
        return getVersion().getVersionId() >= version.getVersionId();
    }

    public static boolean isNewerThan(VersionUtils version) {
        return getVersion().getVersionId() > version.getVersionId();
    }

    public static boolean isOlderThan(VersionUtils version) {
        return getVersion().getVersionId() < version.getVersionId();
    }
}