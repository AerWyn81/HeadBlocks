package fr.aerwyn81.headblocks.data.hunt.behavior.spawn;

public enum AfterGoal {
    DENY,
    CONTINUE;

    public static AfterGoal of(String raw) {
        try {
            return raw == null ? DENY : valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DENY;
        }
    }
}
