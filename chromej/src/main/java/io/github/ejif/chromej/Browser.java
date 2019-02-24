/*
 * Copyright © 2019 Jenny Liang
 */

package io.github.ejif.chromej;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * Object returned by {@link HttpProtocol#getBrowser()}.
 */
@Data
public final class Browser {

    @JsonProperty("Browser")
    private final String browser;

    @JsonProperty("Protocol-Version")
    private final String protocolVersion;

    @JsonProperty("User-Agent")
    private final String userAgent;

    @JsonProperty("V8-Version")
    private final String v8Version;

    @JsonProperty("WebKit-Version")
    private final String webkitVersion;

    @JsonProperty("webSocketDebuggerUrl")
    private final String webSocketDebuggerUrl;
}
