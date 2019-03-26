package io.github.ejif.chromej;

import java.util.List;

import io.github.ejif.chromej.protocol.target.TargetInfo;

public final class ConnectedBrowser extends ConnectedWebSocket {

    private final Browser browser;

    /**
     * Creates a {@link ConnectedBrowser}, which establishes a WebSocket connection to the target at
     * its URL.
     *
     * @param browser
     *            The browser to connect to
     * @param timeoutMillis
     *            How long to wait for a response to each command executed over the WebSocket
     *            connection
     * @throws ConnectionException
     *             if an error occurred when establishing a connection
     */
    public ConnectedBrowser(Browser browser, int timeoutMillis) throws ConnectionException {
        super(browser.getWebSocketDebuggerUrl(), timeoutMillis);
        this.browser = browser;
    }

    public Browser getBrowser() {
        return browser;
    }

    /**
     * Return the targets currently available for this browser.
     *
     * @return the list of targets
     */
    public List<TargetInfo> getTargets() {
        return getProtocol().getTarget().getTargets().getTargetInfos();
    }
}
