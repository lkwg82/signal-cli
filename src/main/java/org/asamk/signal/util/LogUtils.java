package org.asamk.signal.util;

import java.util.Arrays;

public class LogUtils {

    static final boolean DEFAULT = false;
    public static boolean DEBUG_ENABLED = DEFAULT;

    public static void debug(String... entries) {
        if (DEBUG_ENABLED) {
            if (entries == null) {
                _debug("null args - plz fix");
            } else {
                switch (entries.length) {
                    case 0:
                        _debug("empty args - not nice");
                        break;
                    case 1:
                        _debug(entries[0]);
                        break;
                    default:
                        String[] args = Arrays.copyOfRange(entries, 1, entries.length);
                        String format = entries[0];
                        _debug(String.format(format, (Object[]) args));
                }
            }
        }
    }

    private static void _debug(final String text) {
        System.err.println("DEBUG " + text);
    }
}
