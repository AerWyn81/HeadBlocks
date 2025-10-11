package fr.aerwyn81.headblocks.utils.bukkit;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;

import java.util.Arrays;

public enum VersionUtils {
    v1_20_R1(1201, 120),
    v1_20_R2(1202),
    v1_20_R3(1203),
    v1_20_R4(1204),
    v1_20_R5(1205),
    v1_20_R6(1206),
    v1_21_R1(1211, 121),
    v1_21_R2(1212, 122),
    v1_21_R3(1213, 123),
    v1_21_R4(1214, 124),
    v1_21_R5(1215, 125),
    v1_21_R6(1216, 126),
    v1_21_R7(1217, 127),
    v1_21_R8(1218, 128),
    v1_21_R9(1219, 129),
    v1_21_R10(12110, 1210);

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
            version = extractFromString(Bukkit.getBukkitVersion().split("-")[0].replaceAll("\\.", ""));
        } catch (Exception e) {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError extracting server version" + e.getMessage() + ". Using " + v1_21_R8.name()));
            version = v1_21_R8;
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

    public static boolean isNewerOrEqualsTo(VersionUtils version) {
        return getVersion().getVersionId() >= version.getVersionId();
    }
}
