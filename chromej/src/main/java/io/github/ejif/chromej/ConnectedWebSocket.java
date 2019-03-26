/*
 * Copyright © 2019 Jenny Liang
 */

package io.github.ejif.chromej;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.ClientEndpoint;
import javax.websocket.DeploymentException;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.ejif.chromej.protocol.WsProtocol;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Represents a target that has a WebSocket connection established with ChromeJ. Use
 * {@link #getProtocol()} to execute commands through the WebSocket connection.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class ConnectedWebSocket implements AutoCloseable {

    public static int DEFAULT_TIMEOUT_MILLIS = 10_000;

    private static final Logger log = LoggerFactory.getLogger(ConnectedWebSocket.class);

    private static final AtomicInteger commandId = new AtomicInteger();
    private static final ObjectMapper mapper = new ObjectMapper();

    private final long timeoutMillis;
    private final WsProtocol protocol;
    private final Map<Integer, Result> resultsByCommandId = new ConcurrentHashMap<>();
    private final CountDownLatch initializationLatch = new CountDownLatch(1);

    private Session session;

    /**
     * Establishes a WebSocket connection to the given URL.
     *
     * @param webSocketDebuggerUrl
     *            The URL to connect to
     * @param timeoutMillis
     *            How long to wait for a response to each command executed over the WebSocket
     * @throws ConnectionException
     *             if an error occurred when establishing a connection
     */
    public ConnectedWebSocket(String webSocketDebuggerUrl, long timeoutMillis) throws ConnectionException {
        this.timeoutMillis = timeoutMillis;
        this.protocol = createProxy(WsProtocol.class, (proxy, method, args) -> {
            return createDomainProxy(method.getReturnType());
        });

        log.debug("Connecting to {}...", webSocketDebuggerUrl);
        try {
            ClientManager.createClient()
                .connectToServer(new Endpoint(), URI.create(webSocketDebuggerUrl));
            initializationLatch.await();
        } catch (DeploymentException | InterruptedException | IOException e) {
            throw new ConnectionException(e);
        }
        log.debug("Connected to {}.", webSocketDebuggerUrl);
    }

    /**
     * Gets the {@link WsProtocol} instance to execute generic commands
     *
     * @return The {@link WsProtocol} instance
     */
    public final WsProtocol getProtocol() {
        return protocol;
    }

    private <T> T createDomainProxy(Class<T> domain) {
        return createProxy(domain, (proxy, method, args) -> {
            Object response = send(
                domain.getSimpleName() + '.' + method.getName(),
                args == null ? null : args[0]);
            return mapper.convertValue(response, method.getReturnType());
        });
    }

    private static <T> T createProxy(Class<T> interface_, InvocationHandler h) {
        return interface_.cast(Proxy.newProxyInstance(interface_.getClassLoader(), new Class[] { interface_ }, h));
    }

    private Object send(String method, Object params) throws Throwable {
        Result result = new Result();
        int id = commandId.getAndIncrement();
        resultsByCommandId.put(id, result);

        log.debug("Sending '{}' command to Chrome (ID {})...", method, id);
        log.trace("Params: {}", params);
        session.getBasicRemote().sendText(mapper.writeValueAsString(new Request(id, method, params)));
        if (!result.latch.await(timeoutMillis, TimeUnit.MILLISECONDS))
            throw new RuntimeException(String.format("Timeout when running %s (ID %s)", method, id));

        log.debug("Received response for command with ID {}.", id);
        if (result.error != null)
            throw new RuntimeException(String.format("%s (%s)", result.error.message, result.error.data));
        return result.result;
    }

    @Override
    public void close() throws IOException {
        log.debug("Closing websocket session.");
        session.close();
    }

    @ClientEndpoint
    public final class Endpoint {

        @OnOpen
        public void onOpen(Session session) {
            log.trace("Opened websocket.");
            ConnectedWebSocket.this.session = session;
            initializationLatch.countDown();
        }

        @OnMessage
        public void onMessage(String message) throws IOException {
            log.trace("Received message: {}", message);
            Response response = mapper.readValue(message, Response.class);
            Result result = resultsByCommandId.get(response.id);
            result.result = response.result;
            result.error = response.error;
            result.latch.countDown();
        }

        @OnError
        public void onError(Throwable t) throws InterruptedException {
            log.warn("Error in Websocket", t);
        }
    }

    @Data
    private static final class Result {

        private final CountDownLatch latch = new CountDownLatch(1);
        private Object result;
        private Error error;
    }

    @Data
    private static final class Request {

        private final int id;
        private final String method;
        private final Object params;
    }

    @Data
    private static final class Response {

        private final int id;
        private final Object result;
        private final Error error;
    }

    @Data
    private static final class Error {

        private final int code;
        private final String message;
        private final Object data;
    }
}
