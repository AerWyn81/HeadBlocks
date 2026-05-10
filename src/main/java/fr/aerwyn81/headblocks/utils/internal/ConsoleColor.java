package fr.aerwyn81.headblocks.utils.internal;

/**
 * ANSI escape codes for colored console output.
 * Paper's TerminalConsoleAppender renders these natively, so they can be embedded
 * in messages passed to {@link java.util.logging.Logger} (i.e. via {@link LogUtil}).
 * Note: WARNING/SEVERE levels are already colored by Paper's log4j config, so this
 * is mostly useful for INFO messages that need explicit color (success banners, etc.).
 */
public enum ConsoleColor {
    RESET("[0m"),
    BLACK("[30m"),
    RED("[31m"),
    GREEN("[32m"),
    YELLOW("[33m"),
    BLUE("[34m"),
    PURPLE("[35m"),
    CYAN("[36m"),
    WHITE("[37m"),
    BRIGHT_BLACK("[90m"),
    BRIGHT_RED("[91m"),
    BRIGHT_GREEN("[92m"),
    BRIGHT_YELLOW("[93m"),
    BRIGHT_BLUE("[94m"),
    BRIGHT_PURPLE("[95m"),
    BRIGHT_CYAN("[96m"),
    BRIGHT_WHITE("[97m");

    private final String code;

    ConsoleColor(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return code;
    }

    /**
     * Wraps {@code text} with this color and a trailing reset.
     */
    public String apply(String text) {
        return code + text + RESET.code;
    }
}
