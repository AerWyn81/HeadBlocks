package fr.aerwyn81.headblocks.utils.internal;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogUtil {

    private static Logger logger;

    public static void initialize(Logger pluginLogger) {
        logger = pluginLogger;
    }

    public static void info(String message, Object... args) {
        log(Level.INFO, ConsoleColor.BRIGHT_BLUE, message, args);
    }

    public static void success(String message, Object... args) {
        log(Level.INFO, ConsoleColor.GREEN, message, args);
    }

    public static void warning(String message, Object... args) {
        log(Level.WARNING, ConsoleColor.YELLOW, message, args);
    }

    public static void error(String message, Object... args) {
        log(Level.SEVERE, ConsoleColor.RED, message, args);
    }

    private static void log(Level level, ConsoleColor color, String message, Object[] args) {
        if (logger == null) {
            return;
        }

        var formatted = (args == null || args.length == 0)
                ? message
                : MessageFormat.format(message, args);

        logger.log(level, color.apply(formatted));
    }
}
