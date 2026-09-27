package fr.aerwyn81.headblocks.utils.config;

import org.bukkit.configuration.file.FileConfiguration;

public class KeyBuilder {

    private final FileConfiguration config;
    private final char separator;
    private final StringBuilder builder;

    public KeyBuilder(FileConfiguration config, char separator) {
        this.config = config;
        this.separator = separator;
        this.builder = new StringBuilder();
    }

    public void parseLine(String line) {
        line = line.trim();
        String[] currentSplitLine = line.split(":");
        String key = currentSplitLine[0].replace("'", "").replace("\"", "");

        while (!isEmpty() && !config.contains(builder.toString() + separator + key)) {
            removeLastKey();
        }

        if (!isEmpty())
            builder.append(separator);

        builder.append(key);
    }

    public String getLastKey() {
        if (isEmpty())
            return "";

        return builder.toString().split("[" + separator + "]")[0];
    }

    public boolean isEmpty() {
        return builder.isEmpty();
    }

    public boolean isSubKey(String subKey) {
        return isSubKeyOf(builder.toString(), subKey, separator);
    }

    public boolean isSubKeyOf(String parentKey) {
        return isSubKeyOf(parentKey, builder.toString(), separator);
    }

    public static boolean isSubKeyOf(String parentKey, String subKey, char separator) {
        if (parentKey.isEmpty())
            return false;

        return subKey.startsWith(parentKey)
                && subKey.startsWith(String.valueOf(separator), parentKey.length());
    }

    public static String getIndents(String key, char separator) {
        String[] splitKey = key.split("[" + separator + "]");

        return "  ".repeat(Math.max(0, splitKey.length - 1));
    }

    public boolean isConfigSection() {
        String key = builder.toString();
        return config.isConfigurationSection(key);
    }

    public boolean isConfigSectionWithKeys() {
        String key = builder.toString();
        var section = config.getConfigurationSection(key);
        return section != null && !section.getKeys(false).isEmpty();
    }

    public void removeLastKey() {
        if (isEmpty())
            return;

        builder.setLength(Math.max(0, builder.lastIndexOf(String.valueOf(separator))));
    }

    @Override
    public String toString() {
        return builder.toString();
    }

}