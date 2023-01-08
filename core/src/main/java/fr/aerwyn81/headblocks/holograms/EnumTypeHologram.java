package fr.aerwyn81.headblocks.holograms;

public enum EnumTypeHologram {
    DEFAULT("DEFAULT"),
    DECENT("DECENT"),
    HD("HD");

    private final String value;

    EnumTypeHologram(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static EnumTypeHologram fromString(String text) {
        for (EnumTypeHologram b : EnumTypeHologram.values()) {
            if (b.value.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }
}