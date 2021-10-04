package fr.aerwyn81.headblocks.utils;

import org.bukkit.Bukkit;

public enum Version {
    v1_8, v1_9, v1_10, v1_11, v1_12, v1_13, v1_14, v1_15, v1_16, v1_17, v1_18;

    private Integer value;

    private static Version current;

    static {
        current = null;
        getCurrent();
    }

    Version() {
        try {
            this.value = Integer.valueOf(name().replaceAll("[^\\d.]", ""));
        } catch (Exception ignored) {
        }
    }

    public static Version getCurrent() {
        if (current != null)
            return current;

        try {
            String serverVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            current = valueOf(serverVersion.substring(0, serverVersion.length() - 3));
        } catch (IllegalArgumentException e) {
            current = v1_13;
        }

        return current;
    }

    public static String getCurrentFormatted() {
        return current.name().replace("v", "").replace("_", ".");
    }

    public boolean isNewerThan(Version version) {
        return this.value > version.value;
    }

    public boolean isNewerOrSameThan(Version version) {
        return this.value >= version.value;
    }

    public boolean isOlderThan(Version version) {
        return this.value < version.value;
    }

    public boolean isOlderOrSameThan(Version version) {
        return this.value <= version.value;
    }
}