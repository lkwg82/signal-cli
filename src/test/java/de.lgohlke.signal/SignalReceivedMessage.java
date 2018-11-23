package de.lgohlke.signal;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.messages.SignalServiceContent;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.messages.SignalServiceEnvelope;

@RequiredArgsConstructor
class SignalReceivedMessage {

    private final @NonNull SignalServiceEnvelope envelope;
    private final @NonNull SignalServiceContent message;

    String getSource() {
        return envelope.getSource();
    }

    java.util.Optional<SignalServiceDataMessage> dataMessage() {
        return toJavaUtil(message.getDataMessage());
    }

    /*
     * converts from whispersystem Optional to Optional from java
     */
    private static <T> java.util.Optional<T> toJavaUtil(Optional<T> opt) {
        return opt.transform(java.util.Optional::of)
                  .or(java.util.Optional.empty());
    }
}
