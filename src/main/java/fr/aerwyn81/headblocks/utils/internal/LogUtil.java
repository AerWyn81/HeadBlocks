package fr.aerwyn81.headblocks.utils.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LogUtil {

    private static Logger logger;

    public static void initialize(Logger pluginLogger) {
        logger = pluginLogger;
    }

    public static void info(String message, Object... args) {
        if (logger == null) {
            return;
        }

        logger.log(Level.INFO, message, args);
    }

    public static void warning(String message, Object... args) {
        if (logger == null) {
            return;
        }

        logger.log(Level.WARNING, message, args);
    }

    public static void error(String message, Object... args) {
        if (logger == null) {
            return;
        }

        logger.log(Level.SEVERE, message, args);
    }
}
