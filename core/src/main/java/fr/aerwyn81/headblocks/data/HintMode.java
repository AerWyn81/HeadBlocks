package fr.aerwyn81.headblocks.data;

import fr.aerwyn81.headblocks.services.LanguageService;

public enum HintMode {
    SOUND,
    ACTIONBAR;

    public String getLocalizedName() {
        return switch (this) {
            case SOUND -> LanguageService.getMessage("Other.Sound");
            case ACTIONBAR -> LanguageService.getMessage("Other.ActionBar");
        };
    }

    public HintMode next() {
        return switch (this) {
            case SOUND -> ACTIONBAR;
            case ACTIONBAR -> SOUND;
        };
    }
}
