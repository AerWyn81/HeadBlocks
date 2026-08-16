package fr.aerwyn81.headblocks.data.hunt.requirement.area;

import fr.aerwyn81.headblocks.services.LanguageService;

/**
 * How an area tells a player they walked in: chat, title, action bar, or not at all.
 */
public enum AreaMessageMode {
    CHAT,
    ACTION_BAR,
    TITLE;

    public String getLocalizedName(LanguageService ls) {
        return switch (this) {
            case ACTION_BAR -> ls.message("Gui.AreaConfigMessageModeActionBar");
            case TITLE -> ls.message("Gui.AreaConfigMessageModeTitle");
            case CHAT -> ls.message("Gui.AreaConfigMessageModeChat");
        };
    }

    public static AreaMessageMode fromString(String value) {
        if (value == null) {
            return CHAT;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CHAT;
        }
    }

    public AreaMessageMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
