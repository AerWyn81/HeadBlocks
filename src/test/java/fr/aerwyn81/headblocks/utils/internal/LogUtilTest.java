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

    // --- initialize ---

    @Test
    void info_withoutInitialize_doesNotThrow() {
        LogUtil.initialize(null);

        LogUtil.info("test message");
        // No exception = pass
    }

    @Test
    void warning_withoutInitialize_doesNotThrow() {
        LogUtil.initialize(null);

        LogUtil.warning("test message");
    }

    @Test
    void error_withoutInitialize_doesNotThrow() {
        LogUtil.initialize(null);

        LogUtil.error("test message");
    }

    // --- with logger ---

    @Test
    void info_withLogger_logsAtInfoLevel() {
        Logger logger = mock(Logger.class);
        LogUtil.initialize(logger);

        LogUtil.info("hello {0}", "world");

        verify(logger).log(eq(Level.INFO), eq("hello {0}"), any(Object[].class));
    }

    @Test
    void warning_withLogger_logsAtWarningLevel() {
        Logger logger = mock(Logger.class);
        LogUtil.initialize(logger);

        LogUtil.warning("warn {0}", "msg");

        verify(logger).log(eq(Level.WARNING), eq("warn {0}"), any(Object[].class));
    }

    @Test
    void error_withLogger_logsAtSevereLevel() {
        Logger logger = mock(Logger.class);
        LogUtil.initialize(logger);

        LogUtil.error("err {0}", "detail");

        verify(logger).log(eq(Level.SEVERE), eq("err {0}"), any(Object[].class));
    }
}
