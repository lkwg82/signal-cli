package org.asamk.signal;

import net.sourceforge.argparse4j.inf.Namespace;
import org.asamk.Signal;
import org.asamk.signal.util.IOUtils;
import org.asamk.signal.util.Util;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.whispersystems.signalservice.api.push.exceptions.EncapsulatedExceptions;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.asamk.signal.util.ErrorUtils.*;
import static org.asamk.signal.util.LogUtils.debug;

class SendCommand {

    private final Namespace namespace;
    private final Signal signal;

    SendCommand(Namespace namespace, Signal signal) {
        this.namespace = namespace;
        this.signal = signal;
    }

    int execute() {
        debug("try sending");

        if (namespace.getBoolean("endsession")) {
            return endSession();
        } else {
            String messageText = retrieveMessageText();
            debug(" message with text '%s'", messageText);

            try {
                List<String> attachments = namespace.getList("attachment");
                if (attachments == null) {
                    attachments = new ArrayList<>();
                }
                if (namespace.getString("group") != null) {
                    byte[] groupId = Util.decodeGroupId(namespace.getString("group"));
                    signal.sendGroupMessage(messageText, attachments, groupId);
                } else {
                    List<String> recipient = namespace.getList("recipient");
                    signal.sendMessage(messageText, attachments, recipient);
                }
            } catch (IOException e) {
                handleIOException(e);
                return 3;
            } catch (EncapsulatedExceptions e) {
                handleEncapsulatedExceptions(e);
                return 3;
            } catch (AssertionError e) {
                handleAssertionError(e);
                return 1;
            } catch (GroupNotFoundException e) {
                handleGroupNotFoundException(e);
                return 1;
            } catch (NotAGroupMemberException e) {
                handleNotAGroupMemberException(e);
                return 1;
            } catch (AttachmentInvalidException e) {
                System.err.println("Failed to add attachment: " + e.getMessage());
                System.err.println("Aborting sending.");
                return 1;
            } catch (DBusExecutionException e) {
                handleDBusExecutionException(e);
                return 1;
            } catch (GroupIdFormatException e) {
                handleGroupIdFormatException(e);
                return 1;
            }
        }
        return 0;
    }

    private String retrieveMessageText() {
        String messageText = namespace.getString("message");
        if (messageText == null) {
            try {
                messageText = IOUtils.readAll(System.in, Charset.defaultCharset());
            } catch (IOException e) {
                String err = "Failed to read message from stdin: " + e.getMessage() + "\n" + "Aborting sending.";
                throw new ExitCodeException(1, err, e);
            }
        }
        return messageText;
    }

    private int endSession() {
        if (namespace.getList("recipient") == null) {
            System.err.println("No recipients given");
            System.err.println("Aborting sending.");
            return 1;
        }
        try {
            signal.sendEndSessionMessage(namespace.<String>getList("recipient"));
            return 0;
        } catch (IOException e) {
            handleIOException(e);
            return 3;
        } catch (EncapsulatedExceptions e) {
            handleEncapsulatedExceptions(e);
            return 3;
        } catch (AssertionError e) {
            handleAssertionError(e);
            return 1;
        } catch (DBusExecutionException e) {
            handleDBusExecutionException(e);
            return 1;
        }
    }
}
