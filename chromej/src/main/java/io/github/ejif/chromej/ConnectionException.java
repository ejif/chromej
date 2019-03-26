package io.github.ejif.chromej;

/**
 * An exception indicating that some error occurred while establishing a WebSocket connection.
 */
public final class ConnectionException extends Exception {

    private static final long serialVersionUID = 1L;

    public ConnectionException(Throwable cause) {
        super(cause);
    }
}
