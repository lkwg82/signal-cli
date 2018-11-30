package de.lgohlke.signal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.apache.commons.io.FileUtils;
import org.asamk.signal.manager.BaseConfig;
import org.asamk.signal.manager.Manager;
import org.asamk.signal.storage.SignalAccount;
import org.asamk.signal.util.LogUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.signal.libsignal.metadata.InvalidMetadataMessageException;
import org.signal.libsignal.metadata.InvalidMetadataVersionException;
import org.signal.libsignal.metadata.ProtocolDuplicateMessageException;
import org.signal.libsignal.metadata.ProtocolInvalidKeyException;
import org.signal.libsignal.metadata.ProtocolInvalidKeyIdException;
import org.signal.libsignal.metadata.ProtocolInvalidMessageException;
import org.signal.libsignal.metadata.ProtocolInvalidVersionException;
import org.signal.libsignal.metadata.ProtocolLegacyMessageException;
import org.signal.libsignal.metadata.ProtocolNoSessionException;
import org.signal.libsignal.metadata.ProtocolUntrustedIdentityException;
import org.signal.libsignal.metadata.SelfSendException;
import org.signal.libsignal.metadata.certificate.CertificateValidator;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.InvalidVersionException;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECPublicKey;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceMessagePipe;
import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.api.crypto.SignalServiceCipher;
import org.whispersystems.signalservice.api.messages.SignalServiceContent;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.messages.SignalServiceEnvelope;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.util.UptimeSleepTimer;
import org.whispersystems.signalservice.internal.util.Base64;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.charset.Charset.defaultCharset;
import static org.asamk.signal.Main.installSecurityProviderWorkaround;
import static org.asamk.signal.Main.retrieveLocalSettingsPath;
import static org.asamk.signal.util.LogUtils.debug;

@Ignore
class SendTest {

    private Manager manager1;
    private String username1;
    private String username2;
    private SignalUser user1;
    private SignalContact contact1;
    private SignalUser user2;
    private SignalContact contact2;
    private Manager manager2;

    @BeforeEach
    void setUp() throws IOException, InterruptedException {

        LogUtils.DEBUG_ENABLED = true;
        debug("THREAD " + Thread.currentThread());

        installSecurityProviderWorkaround();

        String settingsPath = retrieveLocalSettingsPath();
        username1 = FileUtils.readFileToString(Paths.get(settingsPath, "test-username1")
                .toFile(), defaultCharset())
                .trim();
        username2 = FileUtils.readFileToString(Paths.get(settingsPath, "test-username2")
                .toFile(), defaultCharset())
                .trim();

        manager1 = new Manager(username1, settingsPath);
        manager1.init();
        manager2 = new Manager(username2, settingsPath);
        manager2.init();

        user1 = new SignalUser(username1, manager1);
        user2 = new SignalUser(username2, manager2);
        contact1 = new SignalContact(username1);
        contact2 = new SignalContact(username2);
    }

    @AfterEach
    void tearDown() throws IOException {
        manager1.shutdown();
        manager2.shutdown();
    }

    //    @Test
    void send() {
        user1.send()
                .to(contact1)
                .text("test  " + System.nanoTime())
                .execute();
        user2.send()
                .to(contact1)
                .text("test  " + System.nanoTime())
                .execute();
    }

    //    @Test
    void sendAndReceive() throws Exception {
        SignalAccount account = manager1.getAccount();

        BlockingQueue<SignalReceivedMessage> receivedMessages = new ArrayBlockingQueue<>(2);

        ExecutorService receiverService = Executors.newSingleThreadExecutor();
        Runnable receiver = createReceiverJob(account, receivedMessages);
        receiverService.submit(receiver);

        String expectedMessage = "qq  " + System.nanoTime();
        user2.send()
                .to(contact1)
                .text(expectedMessage)
                .execute();

        AtomicBoolean found = new AtomicBoolean(false);
        while (!found.get()) {
            SignalReceivedMessage message = receivedMessages.poll(1, TimeUnit.SECONDS);
            if (null != message) {
                message.dataMessage()
                        .map(m -> {
                            debug("received message");
                            m.getBody()
                                    .transform(body -> {
                                        debug(" text '%s'", body);
                                        if (expectedMessage.equals(body)) {
                                            found.set(true);
                                            debug(" as expected");
                                        }
                                        return body;
                                    });

                            return m;
                        });
            }
        }

        receiverService.shutdownNow();
    }


    // interactive
//    @Test
    void receive() throws Exception {

        manager1.receiveMessages(1, TimeUnit.HOURS, false, true, new Manager.ReceiveMessageHandler() {
            @Override
            public void handleMessage(SignalServiceEnvelope envelope, SignalServiceContent decryptedContent, Throwable e) {
                if (null != decryptedContent) {
                    debug("FROM: " + decryptedContent.getSender());
                    debug("Text: " + decryptedContent.getDataMessage().transform(SignalServiceDataMessage::getBody));
                }
            }
        });

    }

    private Runnable createReceiverJob(SignalAccount account, BlockingQueue<SignalReceivedMessage> receivedMessages) {
        SignalServiceMessageReceiver messageReceiver = new SignalServiceMessageReceiver(BaseConfig.serviceConfiguration,
                username1,
                account.getPassword(),
                account.getDeviceId(),
                account.getSignalingKey(),
                BaseConfig.USER_AGENT,
                new DefaultConnectivityListener(),
                new UptimeSleepTimer());

        SignalServiceMessagePipe messagePipe = messageReceiver.createMessagePipe();

        return () -> {
            while (true) {
                try {
                    SignalServiceEnvelope envelope = messagePipe.read(5, TimeUnit.SECONDS);
                    SignalServiceContent message = decryptMessage(envelope, account);

                    SignalReceivedMessage receivedMessage = new SignalReceivedMessage(envelope, message);
                    debug("got message from " + receivedMessage.getSource());
                    receivedMessages.add(receivedMessage);
                } catch (InvalidVersionException | IOException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    debug(e.getMessage());
                }
            }
        };
    }

    @Test
    void sendAttachment() throws Exception {
        SignalAccount account = manager1.getAccount();

        BlockingQueue<SignalReceivedMessage> receivedMessages = new ArrayBlockingQueue<>(2);

        ExecutorService receiverService = Executors.newSingleThreadExecutor();
        Runnable receiver = createReceiverJob(account, receivedMessages);
        receiverService.submit(receiver);

        Path tempJpeg = Files.createTempFile(System.nanoTime() + "", ".jpg");

        BufferedImage image = createImage();
        ImageIO.write(image, "jpg", tempJpeg.toFile());

        String expectedMessage = "qq  " + System.nanoTime();
        user2.send()
                .to(contact1)
                .text(expectedMessage)
                .attachment(tempJpeg)
                .execute();

        AtomicBoolean found = new AtomicBoolean(false);
        while (!found.get()) {
            SignalReceivedMessage message = receivedMessages.poll(1, TimeUnit.SECONDS);
            if (null != message) {
                message.dataMessage()
                        .map(m -> {
                            debug("received message");
                            m.getBody()
                                    .transform(body -> {
                                        debug(" text '%s'", body);
                                        if (expectedMessage.equals(body) && m.getAttachments()
                                                .isPresent()) {
                                            found.set(true);
                                            debug(" as expected");
                                        }
                                        return body;
                                    });

                            return m;
                        });
            }
        }

        receiverService.shutdownNow();
    }

    private BufferedImage createImage() {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        g.drawString("Hello World!!!", 10, 20);
        return image;
    }

    private SignalServiceContent decryptMessage(SignalServiceEnvelope envelope, SignalAccount account) {
        SignalServiceAddress localAddress = new SignalServiceAddress(username1);
        SignalServiceCipher cipher = new SignalServiceCipher(localAddress, account.getSignalProtocolStore(), getCertificateValidator());
        try {
            return cipher.decrypt(envelope);
        } catch (InvalidMetadataMessageException | InvalidMetadataVersionException | ProtocolLegacyMessageException | ProtocolInvalidKeyIdException | ProtocolUntrustedIdentityException | ProtocolNoSessionException | ProtocolInvalidVersionException | ProtocolInvalidMessageException | ProtocolInvalidKeyException | ProtocolDuplicateMessageException | SelfSendException e) {
            throw new IllegalStateException(e);
        }
    }

    private static CertificateValidator getCertificateValidator() {
        try {
            ECPublicKey unidentifiedSenderTrustRoot = Curve.decodePoint(Base64.decode(BaseConfig.UNIDENTIFIED_SENDER_TRUST_ROOT), 0);
            return new CertificateValidator(unidentifiedSenderTrustRoot);
        } catch (InvalidKeyException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void printJson(Object o) {
        System.out.println("class " + o.getClass());
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
            objectMapper.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
            SimpleModule module = new SimpleModule();
            module.addSerializer(Optional.class, new OptionalSerialzer());
            objectMapper.registerModule(module);

            String json = objectMapper.writeValueAsString(o);
            System.out.println(json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private class OptionalSerialzer extends StdSerializer<Optional> {

        private OptionalSerialzer() {
            this(null);
        }

        OptionalSerialzer(Class<Optional> t) {
            super(t);
        }

        @Override
        public void serialize(Optional optional, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {

            if (optional.isPresent()) {
                Object value = optional.get();
                if (value.getClass()
                        .isPrimitive() || value instanceof String) {
                    jgen.writeString(value + "");
                } else {
                    jgen.writeStartObject();
                    jgen.writeFieldName("value");
                    jgen.writeObject(value);
                    jgen.writeEndObject();
                }
            } else {
                jgen.writeNull();
            }
        }
    }
}