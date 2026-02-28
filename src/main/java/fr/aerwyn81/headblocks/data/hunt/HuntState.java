package fr.aerwyn81.headblocks.data.hunt;

import fr.aerwyn81.headblocks.services.LanguageService;

public enum HuntState {
    ACTIVE,
    INACTIVE,
    ARCHIVED;

    public String getLocalizedName() {
        return switch (this) {
            case ACTIVE -> LanguageService.getMessage("Hunt.State.Active");
            case INACTIVE -> LanguageService.getMessage("Hunt.State.Inactive");
            case ARCHIVED -> LanguageService.getMessage("Hunt.State.Archived");
        };
    }

    public static HuntState of(String value) {
        if (value == null) return ACTIVE;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ACTIVE;
        }
    }
}
