package com.medicare.utils;

import com.sun.net.httpserver.HttpServer;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

/**
 * Minimal embedded HTTP server that serves recaptcha.html from localhost
 * so Google reCAPTCHA accepts the origin (localhost is a valid registered domain).
 * Starts once (lazy singleton) and shuts down with the JVM.
 */
public final class CaptchaServer {

    private static volatile int    port   = -1;
    private static volatile HttpServer server = null;

    private CaptchaServer() {}

    /** Returns the full URL to load in the WebView, e.g. http://localhost:51234/recaptcha.html */
    public static String getUrl() {
        ensureStarted();
        return "http://localhost:" + port + "/recaptcha.html";
    }

    private static synchronized void ensureStarted() {
        if (server != null) return;
        try {
            // Pick any free port
            try (ServerSocket ss = new ServerSocket(0)) {
                port = ss.getLocalPort();
            }
            server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
            server.createContext("/recaptcha.html", exchange -> {
                try (InputStream in = CaptchaServer.class
                        .getResourceAsStream("/com/medicare/recaptcha.html")) {
                    if (in == null) {
                        exchange.sendResponseHeaders(404, -1);
                        return;
                    }
                    byte[] body = in.readAllBytes();
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, body.length);
                    try (var out = exchange.getResponseBody()) {
                        out.write(body);
                    }
                }
            });
            server.setExecutor(null);
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
        } catch (Exception e) {
            System.out.println("CaptchaServer error: " + e.getMessage());
        }
    }
}
