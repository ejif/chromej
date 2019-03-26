/*
 * Copyright © 2019 Jenny Liang
 */

package io.github.ejif.chromej;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public final class ChromeJTest {

    @Test
    public void testGetTargets() throws Exception {
        ChromeJ.create().getBrowser().getTargets();
    }

    @Test
    public void testGetOuterHtml() throws Exception {
        try (ConnectedTarget target = ChromeJ.create().newTab()) {
            target.navigate("data:text/html,Hello%2C%20World!");
            target.wait(100);
            assertThat(target.getOuterHtml()).isEqualTo("<html><head></head><body>Hello, World!</body></html>");
            target.closeTab();
        }
    }

    @Test
    public void testEvaluate() throws Exception {
        try (ConnectedTarget target = ChromeJ.create().newTab()) {
            assertThat(target.evaluate("1 + 1")).isEqualTo(2);
            target.closeTab();
        }
    }
}
