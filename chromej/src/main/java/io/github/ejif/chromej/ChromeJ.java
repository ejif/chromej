package io.github.ejif.chromej;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.websocket.DeploymentException;

import feign.Feign;
import feign.jackson.JacksonDecoder;
import lombok.Data;

@Data
public final class ChromeJ {

    private final HttpProtocol httpProtocol;
    private int websocketTimeoutMillis = 10_000;

    private ChromeJ(String url) {
        this.httpProtocol = Feign.builder()
            .decoder(new JacksonDecoder())
            .target(HttpProtocol.class, url);
    }

    public static ChromeJ create() {
        try {
            return create("localhost", 9222, false);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    public static ChromeJ create(String host, int port, boolean secure) throws MalformedURLException {
        return create(new URL(secure ? "https" : "http", host, port, "").toString());
    }

    public static ChromeJ create(String url) {
        return new ChromeJ(url);
    }

    public ConnectedTarget getBrowser() throws DeploymentException, InterruptedException, IOException {
        Browser browser = httpProtocol.getBrowser();
        return ConnectedTarget.initialize(browser.getWebSocketDebuggerUrl(), websocketTimeoutMillis);
    }

    public ConnectedTarget newTab() throws DeploymentException, InterruptedException, IOException {
        WsTarget target = httpProtocol.newTab();
        return ConnectedTarget.initialize(target.getWebSocketDebuggerUrl(), websocketTimeoutMillis);
    }
}
