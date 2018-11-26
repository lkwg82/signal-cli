package de.lgohlke.signal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.asamk.signal.manager.Manager;
import org.whispersystems.signalservice.api.push.exceptions.EncapsulatedExceptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
class SignalUser {

    private final String username;
    private final Manager manager;

    SignalUser.Send send() {
        SendContext sendContext = new SendContext(manager);
        return new Send(sendContext);
    }

    @RequiredArgsConstructor
    @Getter
    @Setter
    private static class SendContext {

        private final Manager manager;

    }

    @RequiredArgsConstructor
    public class Send {

        private final SendContext context;

        private To to;
        private Message message;

        private void _execute() {
            try {
                List<String> recipients = Collections.singletonList(to.contact.id);
                List<String> attachments = new ArrayList<>();
                String message = this.message.message;

                Manager manager = context.getManager();

                manager.sendMessage(message, attachments, recipients);
            } catch (IOException | EncapsulatedExceptions e) {
                e.printStackTrace();
            }
        }

        To to(SignalContact contact) {
            to = new To(contact);
            return to;
        }

        @RequiredArgsConstructor
        class To {

            private final SignalContact contact;

            Message message(String text) {
                message = new Message(text);
                return message;
            }
        }

        @RequiredArgsConstructor
        class Message {

            private final String message;

            void execute() {
                _execute();
            }
        }
    }

}
