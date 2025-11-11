package fr.aerwyn81.headblocks.utils.internal;

import fr.aerwyn81.headblocks.HeadBlocks;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DebugLog {
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    private static PrintStream ps;
    private static boolean canWrite;

    public static void Init() {
        File logFile = new File(HeadBlocks.getInstance().getDataFolder(), "debugLog.txt");

        if (logFile.exists()) {
            logFile.delete();
        }

        try {
            ps = new PrintStream(logFile);
            canWrite = true;
        } catch (IOException e) {
            LogUtil.error("Cannot create a debuglog file: {0}", e.getMessage());
            canWrite = false;
        }
    }

    public static void Write(String varName, String varValue) {
        if (!canWrite) {
            return;
        }

        ps.println(dtf.format(LocalDateTime.now()) + " | " + varName + ": " + varValue);
    }

    public static void Close() {
        if (!canWrite) {
            return;
        }

        ps.close();
    }
}
