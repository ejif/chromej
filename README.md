# ChromeJ

ChromeJ is a Java library for interfacing with Chrome or Chrome Headless through the [DevTools protocol](https://chromedevtools.github.io/devtools-protocol/).

1. ChromeJ expects a Chrome process to already be running with its debugging port open. For example, on OSX, run the following command (make sure to fully quit the current Chrome process if one is already running):

        "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --remote-debugging-port=9222

1. Add the following maven coordinates to your project:

        <dependency>
            <groupId>io.github.ejif.chromej</groupId>
            <artifactId>chromej</artifactId>
            <version>{version}</version>
        </dependency>

1. Use the ChromeJ library to interface with Chrome:

        WsProtocol ws = ChromeJ.create()
            .newTab()
            .getProtocol();
        EvaluateResponse result = ws.getRuntime().evaluate(EvaluateRequest.builder()
            .expression("1 + 1")
            .build());
        assertThat(result.getResult().getValue()).isEqualTo(2);

## API details

### Configuring the debugging port

ChromeJ assumes that Chrome's debugging port is at `http://localhost:9222` by default. This can be customized by using `ChromeJ`'s other factory constructors:

    // these three calls do the same thing
    ChromeJ.create();
    ChromeJ.create('localhost', 9222, false);
    ChromeJ.create('http://localhost:9222');

### Fetching and creating targets (Chrome tabs)

Every tab in Chrome is a separate _target_, and each target has a distinct websocket URL that can be connected to in order to control that target. Chrome provides an HTTP protocol to fetch targets and create new targets. In ChromeJ, use the following code:

    HttpProtocol http = ChromeJ.create().getHttpProtocol();
    List<WsTarget> targets = http.getTargets(); // get all targets
    WsTarget newTarget = http.newTab(); // create a new target

### Establishing a WebSocket connection to a target

Every `WsTarget` object has a `websocketDebuggerUrl` field. This can be used to initialize a `ConnectedTarget` object, which establishes a websocket connection with Chrome at that target. The `ConnectedTarget` object provides a `getProtocol()` method which provides the full API shown in https://chromedevtools.github.io/devtools-protocol/.

    HttpProtocol http = ChromeJ.create().getHttpProtocol();

    /*
     * Initialize a ConnectedTarget by passing in two parameters:
     * - websocketDebuggerUrl, which can be obtained from the target
     * - websocketTimeoutMillis, how long to wait for a response for each command
     */
    String websocketDebuggerUrl = http.newTab().getWebsocketDebuggerUrl();
    int websocketTimeoutMillis = 10_000;
    WsProtocol ws = ConnectedTarget.initialize(webSocketDebuggerUrl, websocketTimeoutMillis)
        .getProtocol();

    // corresponds to https://chromedevtools.github.io/devtools-protocol/tot/Runtime#method-evaluate
    EvaluateResponse result = ws.getRuntime().evaluate(EvaluateRequest.builder()
        .expression("1 + 1")
        .build());

### Convenience functions

`ChromeJ` provides a `newTab()` convenience function which creates a new tab and returns the connected target with much less code:

    WsProtocol ws = ChromeJ.create()
        .newTab()
        .getProtocol();
    // execute commands for this target

