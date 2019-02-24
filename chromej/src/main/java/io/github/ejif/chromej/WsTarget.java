package io.github.ejif.chromej;

import lombok.Data;

@Data
public final class WsTarget {

    private final String description;
    private final String devtoolsFrontendUrl;
    private final String id;
    private final String title;
    private final String type;
    private final String url;
    private final String webSocketDebuggerUrl;
}
