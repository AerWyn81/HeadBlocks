package fr.aerwyn81.headblocks.data.hunt;

import fr.aerwyn81.headblocks.services.LanguageService;

public enum HuntState {
    ACTIVE,
    INACTIVE,
    ARCHIVED;

    public String getLocalizedName(LanguageService ls) {
        return switch (this) {
            case ACTIVE -> ls.message("Hunt.State.Active");
            case INACTIVE -> ls.message("Hunt.State.Inactive");
            case ARCHIVED -> ls.message("Hunt.State.Archived");
        };
    }

    public static HuntState of(String value) {
        if (value == null) {
            return ACTIVE;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ACTIVE;
        }
    }
}
