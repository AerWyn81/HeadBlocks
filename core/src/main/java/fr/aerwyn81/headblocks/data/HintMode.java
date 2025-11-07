package fr.aerwyn81.headblocks.data;

public enum HintMode {
    SOUND,
    ACTIONBAR;

    public String getLocalizedName() {
        return switch (this) {
            case SOUND -> "Sound";
            case ACTIONBAR -> "ActionBar";
        };
    }

    public HintMode next() {
        return switch (this) {
            case SOUND -> ACTIONBAR;
            case ACTIONBAR -> SOUND;
        };
    }
}
