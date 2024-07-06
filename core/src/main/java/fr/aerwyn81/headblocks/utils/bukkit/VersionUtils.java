package fr.aerwyn81.headblocks.utils.bukkit;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import org.bukkit.Bukkit;

public enum VersionUtils {
    v1_16_R1(1161),
    v1_16_R2(1162),
    v1_16_R3(1163),
    v1_17_R1(1171),
    v1_18_R1(1181),
    v1_18_R2(1182),
    v1_19_R1(1191),
    v1_19_R2(1192),
    v1_19_R3(1193),
    v1_20_R1(1201),
    v1_20_R2(1202),
    v1_20_R3(1203),
    v1_20_R4(1204),
    v1_20_R5(1205),
    v1_20_R6(1206),
    v1_21_R1(1211);

    private static VersionUtils version;
    private final int versionId;

    VersionUtils(int id) {
        this.versionId = id;
    }

    public int getVersionId() {
        return versionId;
    }

    public static VersionUtils getVersion() {
        if (version != null) {
            return version;
        }
        try {
            version = extractFromString(Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3]);
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
            if (ver.equals(String.valueOf(version.versionId)))
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