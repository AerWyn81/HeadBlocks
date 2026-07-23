package fr.aerwyn81.headblocks.data.head;

import java.util.Arrays;

public enum HeadType {
    DEFAULT(null), PLAYER(null), HDB("HeadDatabase"), HEADDB("HeadDB");

    private final String pluginName;

    HeadType(String pluginName) {
        this.pluginName = pluginName;
    }

    public String getPluginName() {
        return pluginName;
    }

    public boolean requiresPlugin() {
        return pluginName != null;
    }

    public static HeadType fromPrefix(String prefix) {
        return Arrays.stream(values())
                .filter(t -> t.name().equalsIgnoreCase(prefix))
                .findFirst()
                .orElse(null);
    }
}
