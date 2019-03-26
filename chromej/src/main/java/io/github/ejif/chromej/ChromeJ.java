/*
 * Copyright © 2019 Jenny Liang
 */

package io.github.ejif.chromej;

import java.net.MalformedURLException;
import java.net.URL;

import feign.Feign;
import feign.jackson.JacksonDecoder;
import lombok.Data;

/**
 * An instance of {@link ChromeJ} is configured with the URL of Chrome's HTTP protocol, which can be
 * used to fetch existing targets and create new ones.
 */
@Data
public final class ChromeJ {

    private final HttpProtocol httpProtocol;

    private ChromeJ(String url) {
        this.httpProtocol = Feign.builder()
            .decoder(new JacksonDecoder())
            .target(HttpProtocol.class, url);
    }

    /**
     * Creates a {@link ChromeJ} instance that talks to Chrome at http://localhost:9222.
     *
     * @return The {@link ChromeJ} instance
     */
    public static ChromeJ create() {
        try {
            return create("localhost", 9222, false);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Creates a {@link ChromeJ} instance that talks to Chrome at the specified host, port, and
     * scheme.
     *
     * @param host
     *            The host that Chrome is on
     * @param port
     *            The port that Chrome's HTTP protocol is listening to
     * @param secure
     *            Whether the scheme should be https instead of http
     * @return The {@link ChromeJ} instance
     * @throws MalformedURLException
     *             If the host is an invalid URL host
     */
    public static ChromeJ create(String host, int port, boolean secure) throws MalformedURLException {
        return create(new URL(secure ? "https" : "http", host, port, "").toString());
    }

    /**
     * Creates a {@link ChromeJ} instance that talks to Chrome at the specified URL.
     *
     * @param url
     *            The URL that Chrome's HTTP protocol is on
     * @return The {@link ChromeJ} instance
     */
    public static ChromeJ create(String url) {
        return new ChromeJ(url);
    }

    /**
     * Creates a WebSocket connection with the browser, which can be used to fetch targets and
     * create new ones.
     *
     * @return The {@link ConnectedWebSocket} instance
     * @throws ConnectionException
     *             if an error occurred when establishing a connection
     */
    public ConnectedBrowser getBrowser() throws ConnectionException {
        Browser browser = httpProtocol.getBrowser();
        return new ConnectedBrowser(browser, ConnectedWebSocket.DEFAULT_TIMEOUT_MILLIS);
    }

    /**
     * Opens a new tab and creates a WebSocket connection with it.
     *
     * @return The {@link ConnectedTarget} instance
     * @throws ConnectionException
     *             if an error occurred when establishing a connection
     */
    public ConnectedTarget newTab() throws ConnectionException {
        Target target = httpProtocol.newTab();
        return new ConnectedTarget(target, ConnectedWebSocket.DEFAULT_TIMEOUT_MILLIS);
    }
}
