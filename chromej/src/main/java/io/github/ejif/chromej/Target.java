/*
 * Copyright © 2019 Jenny Liang
 */

package io.github.ejif.chromej;

import lombok.Data;

/**
 * Object returned by {@link HttpProtocol#getTargets()} and {@link HttpProtocol#newTab()}.
 */
@Data
public final class Target {

    private final String description;
    private final String devtoolsFrontendUrl;
    private final String id;
    private final String title;
    private final String type;
    private final String url;
    private final String webSocketDebuggerUrl;
}
