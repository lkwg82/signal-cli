package de.lgohlke.signal;

import lombok.NonNull;
import lombok.SneakyThrows;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Properties;

public class ConfigProvider {

    private final Properties systemProperties;

    public ConfigProvider(Properties systemProperties) {
        this.systemProperties = systemProperties;
    }

    public ConfigProvider() {
        this(System.getProperties());
    }

    @SneakyThrows
    public String getUrlFromConfig(@NonNull String key) {
        String homeDirectory = systemProperties.getProperty("user.home");
        Path configPath = Paths.get(homeDirectory, ".config", "signal", "commandConfig");
        File configDirectory = configPath.getParent()
                                         .toFile();
        if (configDirectory.mkdirs()) {
            System.out.println("created " + configDirectory);
        }
        Files.setPosixFilePermissions(configPath.getParent(), PosixFilePermissions.fromString("rwx------"));

        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(configPath.toFile())) {
            properties.load(input);
        }
        return properties.getProperty(key);
    }
}
