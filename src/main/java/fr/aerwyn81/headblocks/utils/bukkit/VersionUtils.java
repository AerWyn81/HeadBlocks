package fr.aerwyn81.headblocks.utils.bukkit;

import fr.aerwyn81.headblocks.utils.internal.LogUtil;
import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum VersionUtils {
    V1_20_R1(1201, 120),
    V1_20_R2(1202),
    V1_20_R3(1203),
    V1_20_R4(1204),
    V1_20_R5(1205),
    V1_20_R6(1206),
    V1_21_R1(1211, 121),
    V1_21_R2(1212, 122),
    V1_21_R3(1213, 123),
    V1_21_R4(1214, 124),
    V1_21_R5(1215, 125),
    V1_21_R6(1216, 126),
    V1_21_R7(1217, 127),
    V1_21_R8(1218, 128),
    V1_21_R9(1219, 129),
    V1_21_R10(12110, 1210),
    V1_21_R11(12111, 12111),
    V26_1(26010000, 261, 26),
    V26_1_1(26010001, 2611),
    V26_1_2(26010002, 2612),
    V26_2(26020000, 262),
    V26_3(26030000, 263);

    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

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
            version = extractFromString(Bukkit.getBukkitVersion());
        } catch (Exception e) {
            LogUtil.error("Error extracting server version: {0}. Using default: {1}", e.getMessage(), V26_3.name());
            version = V26_3;
        }

        return version;
    }

    private static VersionUtils extractFromString(String bukkitVersion) {
        if (bukkitVersion == null || bukkitVersion.isEmpty()) {
            throw new IllegalArgumentException("Empty bukkit version string");
        }

        Matcher matcher = VERSION_PATTERN.matcher(bukkitVersion);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Unrecognized bukkit version format: " + bukkitVersion);
        }

        String major = matcher.group(1);
        String minor = matcher.group(2);
        String patch = matcher.group(3);
        String key = patch != null ? major + minor + patch : major + minor;

        for (VersionUtils v : VersionUtils.values()) {
            if (Arrays.stream(v.versionId).anyMatch(id -> key.equals(String.valueOf(id)))) {
                return v;
            }
        }

        throw new IllegalStateException("Unknown version: " + bukkitVersion + ". Please report to developer. HeadBlocks will use latest.");
    }

    public static boolean isAtLeastVersion(VersionUtils version) {
        return getVersion().getVersionId() >= version.getVersionId();
    }

    public static boolean isNewerOrEqualsTo(VersionUtils version) {
        return getVersion().getVersionId() >= version.getVersionId();
    }
}
