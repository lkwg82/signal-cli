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
import org.apache.commons.io.FileUtils;
import org.asamk.signal.manager.BaseConfig;
import org.asamk.signal.manager.Manager;
import org.asamk.signal.storage.SignalAccount;
import org.asamk.signal.util.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.signal.libsignal.metadata.*;
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
import org.whispersystems.signalservice.api.messages.SignalServiceEnvelope;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.util.UptimeSleepTimer;
import org.whispersystems.signalservice.internal.util.Base64;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.charset.Charset.defaultCharset;
import static org.asamk.signal.Main.installSecurityProviderWorkaround;
import static org.asamk.signal.Main.retrieveLocalSettingsPath;
import static org.asamk.signal.util.LogUtils.debug;

class SendTest {

    private Manager manager1;
    private String username1;
    private String username2;
    private SignalUser user1;
    private SignalContact contact1;
    private SignalUser user2;
    private SignalContact contact2;

    @BeforeEach
    void setUp() throws IOException, InterruptedException {
        TimeUnit.SECONDS.sleep(5);

        LogUtils.DEBUG_ENABLED = true;
        debug("THREAD" + Thread.currentThread());

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
        Manager manager2 = new Manager(username2, settingsPath);
        manager2.init();

        user1 = new SignalUser(username1, manager1);
        user2 = new SignalUser(username2, manager2);
        contact1 = new SignalContact(username1);
        contact2 = new SignalContact(username2);
    }

    @Test
    void send() {
        user1.send()
             .to(contact1)
             .message("test  " + System.nanoTime())
             .execute();
        user2.send()
             .to(contact1)
             .message("test  " + System.nanoTime())
             .execute();
    }

    @Test
    void sendAndReceive() throws Exception {
        SignalAccount account = manager1.getAccount();

        SignalServiceMessageReceiver messageReceiver = new SignalServiceMessageReceiver(BaseConfig.serviceConfiguration,
                username1,
                account.getPassword(),
                account.getDeviceId(),
                account.getSignalingKey(),
                BaseConfig.USER_AGENT,
                new DefaultConnectivityListener(),
                new UptimeSleepTimer());

        SignalServiceMessagePipe messagePipe = messageReceiver.createMessagePipe();

        BlockingQueue<SignalReceivedMessage> receivedMessages = new ArrayBlockingQueue<>(2);

        Runnable receiver = () -> {
            while (true) {
                try {
                    SignalServiceEnvelope envelope = messagePipe.read(30, TimeUnit.SECONDS);
                    SignalServiceContent message = decryptMessage(envelope, account);

                    SignalReceivedMessage receivedMessage = new SignalReceivedMessage(envelope, message);
                    receivedMessages.add(receivedMessage);
                } catch (InvalidVersionException | IOException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    debug(e.getMessage());
                }
            }
        };

        ExecutorService receiverService = Executors.newSingleThreadExecutor();
        receiverService.submit(receiver);

        String expectedMessage = "qq  " + System.nanoTime();
        user2.send()
             .to(contact1)
             .message(expectedMessage)
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

        protected OptionalSerialzer(Class<Optional> t) {
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