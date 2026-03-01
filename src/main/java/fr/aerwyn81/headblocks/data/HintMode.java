package fr.aerwyn81.headblocks.data;

import fr.aerwyn81.headblocks.services.LanguageService;

public enum HintMode {
    SOUND,
    ACTIONBAR;

    public String getLocalizedName(LanguageService ls) {
        return switch (this) {
            case SOUND -> ls.message("Other.Sound");
            case ACTIONBAR -> ls.message("Other.ActionBar");
        };
    }

    public HintMode next() {
        return switch (this) {
            case SOUND -> ACTIONBAR;
            case ACTIONBAR -> SOUND;
        };
    }
}
