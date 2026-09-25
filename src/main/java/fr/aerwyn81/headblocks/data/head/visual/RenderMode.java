package fr.aerwyn81.headblocks.data.head.visual;

public enum RenderMode {
    BLOCK,
    DISPLAY;

    public static RenderMode of(String value) {
        if (value == null) {
            return null;
        }

        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public RenderMode next() {
        return this == BLOCK ? DISPLAY : BLOCK;
    }
}
