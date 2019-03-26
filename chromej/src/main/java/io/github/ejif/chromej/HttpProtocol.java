/*
 * Copyright © 2019 Jenny Liang
 */

package io.github.ejif.chromej;

import java.util.List;

import feign.RequestLine;

/**
 * The HTTP protocol used to interface with Chrome.
 */
public interface HttpProtocol {

    @RequestLine("GET /json/version")
    Browser getBrowser();

    @RequestLine("GET /json/list")
    List<Target> getTargets();

    @RequestLine("GET /json/new")
    Target newTab();
}
