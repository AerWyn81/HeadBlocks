package fr.aerwyn81.headblocks.holograms;

import java.util.Arrays;

public enum EnumTypeHologram {
    DEFAULT("DEFAULT"),
    ADVANCED("DEFAULT_ADVANCED");

    private final String value;

    EnumTypeHologram(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static EnumTypeHologram getEnumFromText(String text) {
        try {
            return EnumTypeHologram.valueOf(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static EnumTypeHologram getPluginName(EnumTypeHologram e) {
        return Arrays.stream(EnumTypeHologram.values()).filter(enumTypeHologram -> enumTypeHologram == e).findFirst().orElse(null);
    }
}