package de.lgohlke.signal;

import org.apache.commons.io.FileUtils;
import org.asamk.signal.manager.Manager;
import org.asamk.signal.util.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.whispersystems.signalservice.api.push.exceptions.EncapsulatedExceptions;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

import static java.nio.charset.Charset.defaultCharset;
import static org.asamk.signal.Main.installSecurityProviderWorkaround;
import static org.asamk.signal.Main.retrieveLocalSettingsPath;

class SendTest {

    @BeforeEach
    void setUp() {
        LogUtils.DEBUG_ENABLED = true;
    }

    @Test
    void send() throws IOException, EncapsulatedExceptions {
        installSecurityProviderWorkaround();

        String settingsPath = retrieveLocalSettingsPath();
        String username = FileUtils.readFileToString(Paths.get(settingsPath, "test-username").toFile(), defaultCharset()).trim();
        Manager m = new Manager(username, settingsPath);
        m.init();
        m.sendMessage("test  " + System.nanoTime(), new ArrayList<>(), Collections.singletonList(username));
    }
}