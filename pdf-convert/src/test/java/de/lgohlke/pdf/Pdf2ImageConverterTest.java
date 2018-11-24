package de.lgohlke.pdf;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class Pdf2ImageConverterTest {

    private File tempFile;

    @BeforeEach
    void setUp() throws IOException {
        tempFile = File.createTempFile("x" + System.nanoTime(), "test.jpeg");
    }

    @AfterEach
    void tearDown() {
        if (tempFile.exists()) {
            if (!tempFile.delete()) {
                throw new IllegalStateException("could not delete : " + tempFile.getAbsolutePath());
            }
        }
    }

    @Test
    void shouldConvert() throws Exception {
        assertThat(tempFile.length()).isZero();

        String pdfFilename = "src/test/resources/test.pdf";
        String jpegTargetFilename = tempFile.getAbsolutePath();
        ConversionConfig conversionConfig = new ConversionConfig(200, 25, 8, 50, 12, 30);

        new Pdf2ImageConverter(pdfFilename, jpegTargetFilename, conversionConfig).convert();

        assertThat(tempFile.length()).isBetween(9 * 1024L, 10 * 1024L);
    }
}
