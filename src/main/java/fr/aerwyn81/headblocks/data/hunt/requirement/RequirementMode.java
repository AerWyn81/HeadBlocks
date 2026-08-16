package fr.aerwyn81.headblocks.data.hunt.requirement;

import fr.aerwyn81.headblocks.services.LanguageService;

/**
 * How the requirements of a {@link RequirementSet} combine.
 */
public enum RequirementMode {
    /**
     * Every requirement must be met (AND).
     */
    ALL,
    /**
     * At least one requirement must be met (OR).
     */
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
