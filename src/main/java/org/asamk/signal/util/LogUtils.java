package org.asamk.signal.util;

public class LogUtils {

    public static final boolean DEFAULT = false;
    public static boolean DEBUG_ENABLED = DEFAULT;

    public static void debug(String entry) {
        if (DEBUG_ENABLED) {
            System.err.println("DEBUG " + entry);
        }
    }
}
