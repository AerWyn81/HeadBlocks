package fr.aerwyn81.headblocks.data.hunt.requirement;

import fr.aerwyn81.headblocks.services.LanguageService;

/**
 * Whether a hunt needs all of its requirements or just one of them.
 */
public enum RequirementMode {
    ALL,
    ANY;

    public String getLocalizedName(LanguageService ls) {
        return ls.message(this == ALL ? "Gui.RequirementsModeAll" : "Gui.RequirementsModeAny");
    }

    public static RequirementMode fromString(String value) {
        if (value == null) {
            return ALL;
        }

        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ALL;
        }
    }

    public RequirementMode next() {
        return this == ALL ? ANY : ALL;
    }
}
