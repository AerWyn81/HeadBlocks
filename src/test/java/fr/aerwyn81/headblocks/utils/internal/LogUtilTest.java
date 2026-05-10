package fr.aerwyn81.headblocks.utils.internal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.mockito.Mockito.*;

class LogUtilTest {

    @AfterEach
    void tearDown() {
        LogUtil.initialize(null);
    }

    // --- no logger initialized: never throw ---

    @Test
    void info_withoutInitialize_doesNotThrow() {
        LogUtil.initialize(null);
        LogUtil.info("test");
    }

    @Test
    void success_withoutInitialize_doesNotThrow() {
        LogUtil.initialize(null);
        LogUtil.success("test");
    }

    @Test
    void warning_withoutInitialize_doesNotThrow() {
        LogUtil.initialize(null);
        LogUtil.warning("test");
    }

    @Test
    void error_withoutInitialize_doesNotThrow() {
        LogUtil.initialize(null);
        LogUtil.error("test");
    }

    // --- color wrapping per level ---

    @Test
    void info_wrapsMessageWithBrightBlue() {
        Logger logger = mock(Logger.class);
        LogUtil.initialize(logger);

        LogUtil.info("hello");

        verify(logger).log(Level.INFO, ConsoleColor.BRIGHT_BLUE.apply("hello"));
    }

    @Test
    void success_wrapsMessageWithGreen() {
        Logger logger = mock(Logger.class);
        LogUtil.initialize(logger);

        LogUtil.success("done");

        verify(logger).log(Level.INFO, ConsoleColor.GREEN.apply("done"));
    }

    @Test
    void warning_wrapsMessageWithYellow() {
        Logger logger = mock(Logger.class);
        LogUtil.initialize(logger);

        LogUtil.warning("careful");

        verify(logger).log(Level.WARNING, ConsoleColor.YELLOW.apply("careful"));
    }

    @Test
    void error_wrapsMessageWithRed() {
        Logger logger = mock(Logger.class);
        LogUtil.initialize(logger);

        LogUtil.error("boom");

        verify(logger).log(Level.SEVERE, ConsoleColor.RED.apply("boom"));
    }

    // --- args formatting ---

    @Test
    void info_withArgs_substitutesPlaceholdersBeforeColoring() {
        Logger logger = mock(Logger.class);
        LogUtil.initialize(logger);

        LogUtil.info("count: {0}", 42);

        verify(logger).log(Level.INFO, ConsoleColor.BRIGHT_BLUE.apply("count: 42"));
    }

    @Test
    void success_withMultipleArgs_substitutesAll() {
        Logger logger = mock(Logger.class);
        LogUtil.initialize(logger);

        LogUtil.success("loaded {0} of {1}", 10, 20);

        verify(logger).log(Level.INFO, ConsoleColor.GREEN.apply("loaded 10 of 20"));
    }

    // --- always uses 2-arg log to preserve ANSI ---

    @Test
    void allMethods_useTwoArgLogToPreserveAnsi() {
        // The 3-arg logger.log(level, msg, params) routes through log4j's
        // ParameterizedMessage which strips ANSI escape codes — we always go
        // through the 2-arg form to keep colors intact.
        Logger logger = mock(Logger.class);
        LogUtil.initialize(logger);

        LogUtil.info("a {0}", 1);
        LogUtil.success("b");
        LogUtil.warning("c {0}", 2);
        LogUtil.error("d");

        verify(logger, never()).log(any(Level.class), anyString(), any(Object[].class));
    }
}
