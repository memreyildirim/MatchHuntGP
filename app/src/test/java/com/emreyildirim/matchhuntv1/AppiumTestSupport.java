package com.emreyildirim.matchhuntv1;

import org.testng.SkipException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Locale;

/**
 * Appium E2E bases ({@link BaseTest}, {@link BaseTest2}) run on the JVM with an external
 * Appium server. When the server or device is not available, skip instead of failing
 * the whole {@code testDebugUnitTest} task (e.g. release gate on a machine without Appium).
 */
public final class AppiumTestSupport {

    private static final int CONNECT_TIMEOUT_MS = 1_000;

    private AppiumTestSupport() {
    }

    public static void skipIfAppiumUnreachable() {
        if (isEnvTrue(System.getenv("MATCHHUNT_SKIP_APPIUM"))) {
            throw new SkipException(
                    "MATCHHUNT_SKIP_APPIUM is set; skipping Appium E2E.");
        }
        String host = firstNonBlank(
                System.getenv("MATCHHUNT_APPIUM_HOST"),
                "127.0.0.1");
        int port = parsePort(
                firstNonBlank(System.getenv("MATCHHUNT_APPIUM_PORT"), "4723"),
                4723);
        if (!isTcpReachable(host, port, CONNECT_TIMEOUT_MS)) {
            throw new SkipException(String.format(
                    Locale.US,
                    "Appium not reachable at %s:%d (start server or set MATCHHUNT_SKIP_APPIUM=true to skip).",
                    host,
                    port));
        }
    }

    private static boolean isEnvTrue(String value) {
        if (value == null) {
            return false;
        }
        String v = value.trim().toLowerCase(Locale.US);
        return v.equals("1") || v.equals("true") || v.equals("yes");
    }

    private static String firstNonBlank(String a, String fallback) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        return fallback;
    }

    private static int parsePort(String raw, int defaultPort) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return defaultPort;
        }
    }

    private static boolean isTcpReachable(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
