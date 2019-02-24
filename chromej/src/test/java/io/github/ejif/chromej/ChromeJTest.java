package io.github.ejif.chromej;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import io.github.ejif.chromej.ChromeJ;
import io.github.ejif.chromej.protocol.WsProtocol;
import io.github.ejif.chromej.protocol.runtime.EvaluateRequest;
import io.github.ejif.chromej.protocol.runtime.EvaluateResponse;

public final class ChromeJTest {

    @Test
    public void testGetTargets() throws Exception {
        WsProtocol ws = ChromeJ.create()
            .getBrowser()
            .getProtocol();
        ws.getTarget().getTargets();
    }

    @Test
    public void testRunCommand() throws Exception {
        WsProtocol ws = ChromeJ.create()
            .newTab()
            .getProtocol();
        EvaluateResponse result = ws.getRuntime().evaluate(EvaluateRequest.builder()
            .expression("1 + 1")
            .build());
        assertThat(result.getResult().getValue()).isEqualTo(2);
    }
}
