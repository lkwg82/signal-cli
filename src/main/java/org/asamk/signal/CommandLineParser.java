package org.asamk.signal;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.*;
import org.asamk.signal.manager.BaseConfig;
import org.whispersystems.signalservice.api.util.PhoneNumberFormatter;

class CommandLineParser {

    Namespace parse(String... args) {
        ArgumentParser parser = ArgumentParsers.newFor("signal-cli")
                .build()
                .defaultHelp(true)
                .description("Commandline interface for Signal.")
                .version(BaseConfig.PROJECT_NAME + " " + BaseConfig.PROJECT_VERSION);

        parser.addArgument("-v", "--version")
                .help("Show package version.")
                .action(Arguments.version());
        parser.addArgument("-d", "--debug")
                .help("Enable debugging.")
                .action(Arguments.storeTrue());

        parser.addArgument("--config")
                .help("Set the path, where to store the config (Default: $HOME/.config/signal).");

        MutuallyExclusiveGroup mut = parser.addMutuallyExclusiveGroup();
        mut.addArgument("-u", "--username")
                .help("Specify your phone number, that will be used for verification.");
        mut.addArgument("--dbus")
                .help("Make request via user dbus.")
                .action(Arguments.storeTrue());
        mut.addArgument("--dbus-system")
                .help("Make request via system dbus.")
                .action(Arguments.storeTrue());

        Subparsers subparsers = parser.addSubparsers()
                .title("subcommands")
                .dest("command")
                .description("valid subcommands")
                .help("additional help");

        Subparser parserLink = subparsers.addParser("link");
        parserLink.addArgument("-n", "--name")
                .help("Specify a name to describe this new device.");

        Subparser parserAddDevice = subparsers.addParser("addDevice");
        parserAddDevice.addArgument("--uri")
                .required(true)
                .help("Specify the uri contained in the QR code shown by the new device.");

        Subparser parserDevices = subparsers.addParser("listDevices");

        Subparser parserRemoveDevice = subparsers.addParser("removeDevice");
        parserRemoveDevice.addArgument("-d", "--deviceId")
                .type(int.class)
                .required(true)
                .help("Specify the device you want to remove. Use listDevices to see the deviceIds.");

        Subparser parserRegister = subparsers.addParser("register");
        parserRegister.addArgument("-v", "--voice")
                .help("The verification should be done over voice, not sms.")
                .action(Arguments.storeTrue());

        Subparser parserUnregister = subparsers.addParser("unregister");
        parserUnregister.help("Unregister the current device from the signal server.");

        Subparser parserUpdateAccount = subparsers.addParser("updateAccount");
        parserUpdateAccount.help("Update the account attributes on the signal server.");

        Subparser parserSetPin = subparsers.addParser("setPin");
        parserSetPin.addArgument("registrationLockPin")
                .help("The registration lock PIN, that will be required for new registrations (resets after 7 days of inactivity)");

        Subparser parserRemovePin = subparsers.addParser("removePin");

        Subparser parserVerify = subparsers.addParser("verify");
        parserVerify.addArgument("verificationCode")
                .help("The verification code you received via sms or voice call.");
        parserVerify.addArgument("-p", "--pin")
                .help("The registration lock PIN, that was set by the user (Optional)");

        Subparser parserSend = subparsers.addParser("send");
        parserSend.addArgument("-g", "--group")
                .help("Specify the recipient group ID.");
        parserSend.addArgument("recipient")
                .help("Specify the recipients' phone number.")
                .nargs("*");
        parserSend.addArgument("-m", "--message")
                .help("Specify the message, if missing standard input is used.");
        parserSend.addArgument("-a", "--attachment")
                .nargs("*")
                .help("Add file as attachment");
        parserSend.addArgument("-e", "--endsession")
                .help("Clear session state and send end session message.")
                .action(Arguments.storeTrue());

        Subparser parserLeaveGroup = subparsers.addParser("quitGroup");
        parserLeaveGroup.addArgument("-g", "--group")
                .required(true)
                .help("Specify the recipient group ID.");

        Subparser parserUpdateGroup = subparsers.addParser("updateGroup");
        parserUpdateGroup.addArgument("-g", "--group")
                .help("Specify the recipient group ID.");
        parserUpdateGroup.addArgument("-n", "--name")
                .help("Specify the new group name.");
        parserUpdateGroup.addArgument("-a", "--avatar")
                .help("Specify a new group avatar image file");
        parserUpdateGroup.addArgument("-m", "--member")
                .nargs("*")
                .help("Specify one or more members to add to the group");

        Subparser parserListGroups = subparsers.addParser("listGroups");
        parserListGroups.addArgument("-d", "--detailed").action(Arguments.storeTrue())
                .help("List members of each group");
        parserListGroups.help("List group name and ids");

        Subparser parserListIdentities = subparsers.addParser("listIdentities");
        parserListIdentities.addArgument("-n", "--number")
                .help("Only show identity keys for the given phone number.");

        Subparser parserTrust = subparsers.addParser("trust");
        parserTrust.addArgument("number")
                .help("Specify the phone number, for which to set the trust.")
                .required(true);
        MutuallyExclusiveGroup mutTrust = parserTrust.addMutuallyExclusiveGroup();
        mutTrust.addArgument("-a", "--trust-all-known-keys")
                .help("Trust all known keys of this user, only use this for testing.")
                .action(Arguments.storeTrue());
        mutTrust.addArgument("-v", "--verified-fingerprint")
                .help("Specify the fingerprint of the key, only use this option if you have verified the fingerprint.");

        Subparser parserReceive = subparsers.addParser("receive");
        parserReceive.addArgument("-t", "--timeout")
                .type(double.class)
                .help("Number of seconds to wait for new messages (negative values disable timeout)");
        parserReceive.addArgument("--ignore-attachments")
                .help("Don’t download attachments of received messages.")
                .action(Arguments.storeTrue());
        parserReceive.addArgument("--json")
                .help("Output received messages in json format, one json object per line.")
                .action(Arguments.storeTrue());

        Subparser parserDaemon = subparsers.addParser("daemon");
        parserDaemon.addArgument("--system")
                .action(Arguments.storeTrue())
                .help("Use DBus system bus instead of user bus.");
        parserDaemon.addArgument("--ignore-attachments")
                .help("Don’t download attachments of received messages.")
                .action(Arguments.storeTrue());
        parserDaemon.addArgument("--json")
                .help("Output received messages in json format, one json object per line.")
                .action(Arguments.storeTrue());

        try {
            Namespace ns = parser.parseArgs(args);
            if ("link".equals(ns.getString("command"))) {
                if (ns.getString("username") != null) {
                    parser.printUsage();
                    System.err.println("You cannot specify a username (phone number) when linking");
                    System.exit(2);
                }
            } else if (!ns.getBoolean("dbus") && !ns.getBoolean("dbus_system")) {
                if (ns.getString("username") == null) {
                    parser.printUsage();
                    System.err.println("You need to specify a username (phone number)");
                    System.exit(2);
                }
                if (!PhoneNumberFormatter.isValidNumber(ns.getString("username"))) {
                    System.err.println("Invalid username (phone number), make sure you include the country code.");
                    System.exit(2);
                }
            }
            if (ns.getList("recipient") != null && !ns.getList("recipient").isEmpty() && ns.getString("group") != null) {
                System.err.println("You cannot specify recipients by phone number and groups a the same time");
                System.exit(2);
            }
            return ns;
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            return null;
        }
    }
}
