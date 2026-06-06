package fr.aerwyn81.headblocks.data.hunt.behavior.zone;

public enum ZoneMessageMode {
    CHAT,
    ACTION_BAR,
    TITLE;

    public static ZoneMessageMode fromString(String value) {
        if (value == null) {
            return CHAT;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CHAT;
        }
    }

    public ZoneMessageMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
