package io.github.ejif.chromej;

import java.io.IOException;

import javax.websocket.DeploymentException;

import io.github.ejif.chromej.protocol.WsProtocol;
import io.github.ejif.chromej.protocol.dom.GetDocumentRequest;
import io.github.ejif.chromej.protocol.dom.GetOuterHTMLRequest;
import io.github.ejif.chromej.protocol.dom.NodeId;
import io.github.ejif.chromej.protocol.page.NavigateRequest;
import io.github.ejif.chromej.protocol.runtime.EvaluateRequest;
import io.github.ejif.chromej.protocol.runtime.EvaluateResponse;
import io.github.ejif.chromej.protocol.target.CloseTargetRequest;
import io.github.ejif.chromej.protocol.target.TargetID;

public final class ConnectedTarget extends ConnectedWebSocket {

    private final Target target;

    /**
     * Creates a {@link ConnectedTarget}, which establishes a WebSocket connection to the target at its URL
     *
     * @param target
     *            The target to connect to
     * @param timeoutMillis
     *            How long to wait for a response to each command executed over the WebSocket
     *            connection
     * @throws DeploymentException
     * @throws InterruptedException
     * @throws IOException
     */
    public ConnectedTarget(Target target, int timeoutMillis) throws DeploymentException, InterruptedException, IOException {
        super(target.getWebSocketDebuggerUrl(), timeoutMillis);
        this.target = target;
    }

    public Target getTarget() {
        return target;
    }

    /**
     * Navigates to the given URL. Does not wait for the page to load.
     *
     * @param url
     *            The URL to navigate to
     */
    public void navigate(String url) {
        getProtocol().getPage()
            .navigate(NavigateRequest.builder()
                .url(url)
                .build());
    }

    /**
     * Fetches the outer HTML of the current page.
     *
     * @return the outer HTML
     */
    public String getOuterHtml() {
        WsProtocol protocol = getProtocol();
        NodeId nodeId = protocol.getDOM()
            .getDocument(GetDocumentRequest.builder()
                .build())
            .getRoot()
            .getNodeId();
        return protocol.getDOM()
            .getOuterHTML(GetOuterHTMLRequest.builder()
                .nodeId(nodeId)
                .build())
            .getOuterHTML();
    }

    /**
     * Executes the given Javascript expression and returns the result.
     *
     * @param expression
     *            The expression to evaluate
     * @return The return value. If the return value is not serializable, null is returned.
     */
    public Object evaluate(String expression) {
        EvaluateResponse result = getProtocol().getRuntime().evaluate(EvaluateRequest.builder()
            .expression(expression)
            .build());
        return result.getResult().getValue();
    }

    /**
     * Waits for a given amount of time. Useful for waiting for pages to load, etc.
     *
     * @param millis
     *            The number of milliseconds to wait
     */
    public void wait(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Closes the tab represented by this target.
     */
    public void closeTab() {
        getProtocol().getTarget().closeTarget(CloseTargetRequest.builder()
            .targetId(TargetID.of(target.getId()))
            .build());
    }
}
