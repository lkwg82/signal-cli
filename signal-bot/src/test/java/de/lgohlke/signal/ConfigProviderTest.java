package de.lgohlke.signal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigProviderTest {

    private Path tempDirectory;
    private Properties testProperties = new Properties();

    @BeforeEach
    void setUp() throws Exception {
        tempDirectory = Files.createTempDirectory("xx" + System.nanoTime());
        testProperties.put("user.home", tempDirectory.toFile()
                                                     .getAbsolutePath());
    }

    @Test
    void shouldLoadUrlFromConfig() throws IOException {
        Path testConfig = Paths.get(tempDirectory.toFile()
                                                 .getAbsolutePath(), ".config", "signal", "commandConfig");
        testConfig.getParent()
                  .toFile()
                  .mkdirs();
        byte[] content = "url=x\n".getBytes();
        Files.write(testConfig, content);

        String url = new ConfigProvider(testProperties).getUrlFromConfig("url");

        assertThat(url).isEqualTo("x");
    }
}
