package org.asamk.signal.util;

import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

class LogUtilsTest {

    /* based on https://stackoverflow.com/questions/8708342/redirect-console-output-to-string-in-java */
    private PrintStream old = System.err;
    private ByteArrayOutputStream baos = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() {
        // Create a stream to hold the output
        PrintStream ps = new PrintStream(baos);
        // Tell Java to use your special stream
        System.setErr(ps);

        LogUtils.DEBUG_ENABLED = LogUtils.DEFAULT;
    }

    @AfterEach
    void tearDown() {
        System.setErr(old);
    }

    @Nested
    class ActivationTests {

        @DisplayName("should log when enabled")
        @Test
        void test1() {
            LogUtils.DEBUG_ENABLED = true;

            LogUtils.debug("test");

            expectedLog("test");
        }

        @DisplayName("should not log when disabled")
        @Test
        void test2() {
            LogUtils.DEBUG_ENABLED = false;

            LogUtils.debug("test");

            assertThat(baos.toString()).isEmpty();
        }

        @DisplayName("should not log by default")
        @Test
        void test3() {
            LogUtils.debug("test");

            assertThat(baos.toString()).isEmpty();
        }
    }

    @Nested
    class ArgumentTests {

        @DisplayName("should emit error on empty args")
        @Test
        void test4() {
            LogUtils.DEBUG_ENABLED = true;

            LogUtils.debug();

            expectedLog("empty args - not nice");
        }
    }

    private void expectedLog(String expected) {
        assertThat(baos.toString()).isEqualTo("DEBUG " + expected + "\n");
    }
}