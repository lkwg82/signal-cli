package org.asamk.signal;

import net.sourceforge.argparse4j.inf.Namespace;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommandLineParserTest {

    private CommandLineParser parser = new CommandLineParser();

    @Nested
    @DisplayName("debug flag")
    class TestDebug {

        @DisplayName("should be activated")
        @Test
        void test1() {
            Namespace namespace = parser.parse("-d", "link");
            Boolean debug = namespace.getBoolean("debug");

            assertThat(debug).isTrue();
        }

        @DisplayName("should be deactivated")
        @Test
        void test2() {
            Namespace namespace = parser.parse("link");
            Boolean debug = namespace.getBoolean("debug");

            assertThat(debug).isFalse();
        }
    }
}