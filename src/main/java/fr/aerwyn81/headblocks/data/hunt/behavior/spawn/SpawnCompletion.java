package fr.aerwyn81.headblocks.data.hunt.behavior.spawn;

public enum SpawnCompletion {
    PER_PLAYER,
    FIRST_WINS;

    public static SpawnCompletion of(String raw) {
        try {
            return raw == null ? PER_PLAYER : valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PER_PLAYER;
        }
    }
}
