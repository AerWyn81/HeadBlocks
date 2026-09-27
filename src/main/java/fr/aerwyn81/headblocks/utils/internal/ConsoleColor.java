package fr.aerwyn81.headblocks.utils.internal;

/**
 * ANSI escape codes for colored console output.
 * Paper's TerminalConsoleAppender renders these natively, so they can be embedded
 * in messages passed to {@link java.util.logging.Logger} (i.e. via {@link LogUtil}).
 * Note: WARNING/SEVERE levels are already colored by Paper's log4j config, so this
 * is mostly useful for INFO messages that need explicit color (success banners, etc.).
 */
public enum ConsoleColor {
    RESET("\u001B[0m"),
    BLACK("\u001B[30m"),
    RED("\u001B[31m"),
    GREEN("\u001B[32m"),
    YELLOW("\u001B[33m"),
    BLUE("\u001B[34m"),
    PURPLE("\u001B[35m"),
    CYAN("\u001B[36m"),
    WHITE("\u001B[37m"),
    BRIGHT_BLACK("\u001B[90m"),
    BRIGHT_RED("\u001B[91m"),
    BRIGHT_GREEN("\u001B[92m"),
    BRIGHT_YELLOW("\u001B[93m"),
    BRIGHT_BLUE("\u001B[94m"),
    BRIGHT_PURPLE("\u001B[95m"),
    BRIGHT_CYAN("\u001B[96m"),
    BRIGHT_WHITE("\u001B[97m");

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
