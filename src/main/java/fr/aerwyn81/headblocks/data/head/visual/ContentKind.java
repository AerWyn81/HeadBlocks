package fr.aerwyn81.headblocks.data.head.visual;

public enum ContentKind {
    HEAD,
    BLOCK,
    ITEM,
    MOB,
    EXTERNAL;

    public static ContentKind of(String value) {
        if (value == null) {
            return null;
        }

        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
