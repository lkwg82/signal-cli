package org.asamk.signal;

class ExitCodeException extends RuntimeException {

    private final int exitCode;

    ExitCodeException(final int exitCode) {
        this(exitCode, "");
    }

    ExitCodeException(int exitCode, String message) {
        super(message);
        this.exitCode = exitCode;
    }

    ExitCodeException(int exitCode, String message, Throwable wrappedException) {
        super(message, wrappedException);
        this.exitCode = exitCode;
    }

    int getExitCode() {
        return exitCode;
    }
}
