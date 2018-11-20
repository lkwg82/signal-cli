package org.asamk.signal.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

    @DisplayName("should log when enabled")
    @Test
    void test1() {
        LogUtils.DEBUG_ENABLED = true;

        LogUtils.debug("test");

        assertThat(baos.toString()).isEqualTo("DEBUG test\n");
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