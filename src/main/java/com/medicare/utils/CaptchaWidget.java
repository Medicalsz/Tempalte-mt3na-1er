package com.medicare.utils;

import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * reCAPTCHA v3 (invisible) embedded in a JavaFX WebView.
 *
 * v3 has no checkbox — a token is fetched silently in the background as soon
 * as the widget loads.  Call verify() before form submission:
 *   - if the token is not ready yet it returns false with an "essayez à nouveau" hint;
 *   - once ready it POSTs the token to Google siteverify and returns true only when
 *     Google confirms the score is above 0.5 (human).
 */
public class CaptchaWidget extends VBox {

    private static final String SECRET_KEY   = "6LfimNAsAAAAAHYVerHw8o4gwwmS3nsGTxxkv9js";
    private static final String VERIFY_URL   = "https://www.google.com/recaptcha/api/siteverify";
    private static final double MIN_SCORE    = 0.5;
    private static final long   TOKEN_TIMEOUT_MS = 6000; // fail-open after 6 s

    private final WebEngine engine;
    private final long loadedAt = System.currentTimeMillis();

    public CaptchaWidget() {
        WebView webView = new WebView();
        webView.setPrefWidth(334);
        webView.setPrefHeight(60);
        webView.setMinHeight(60);
        webView.setMaxHeight(60);
        webView.setContextMenuEnabled(false);
        webView.setStyle("-fx-background-color: transparent;");

        engine = webView.getEngine();
        engine.setUserStyleSheetLocation(null);
        engine.load(CaptchaServer.getUrl());

        getChildren().add(webView);
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: transparent;");
    }

    /**
     * Returns true if Google confirms the token (score >= 0.5).
     * Returns false if the token is not ready yet — in that case show
     * "Vérification en cours, réessayez dans un instant." to the user.
     */
    public boolean verify() {
        String token = getToken();
        if (token == null || token.isBlank()) {
            // If we've been waiting longer than the timeout, fail open (don't block the user)
            if (System.currentTimeMillis() - loadedAt > TOKEN_TIMEOUT_MS) {
                System.out.println("reCAPTCHA: token timeout — failing open");
                return true;
            }
            try { engine.executeScript("fetchToken('submit')"); } catch (Exception ignored) {}
            return false;
        }
        boolean ok = verifyWithGoogle(token);
        try { engine.executeScript("resetWidget()"); } catch (Exception ignored) {}
        return ok;
    }

    /** Raw token from the WebView, null until the background fetch completes. */
    public String getToken() {
        try {
            Object r = engine.executeScript("getToken()");
            return (r instanceof String s && !s.isBlank()) ? s : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Resets the stored token and triggers a fresh invisible fetch. */
    public void reset() {
        try { engine.executeScript("resetWidget()"); } catch (Exception ignored) {}
    }

    // ---------------------------------------------------------------

    private boolean verifyWithGoogle(String token) {
        try {
            URL url = new URL(VERIFY_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String body = "secret=" + SECRET_KEY + "&response=" + token;
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200) return false;

            try (java.io.InputStream is = conn.getInputStream()) {
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                if (!json.contains("\"success\": true") && !json.contains("\"success\":true"))
                    return false;
                // Extract score from v3 response, e.g. "score": 0.9
                double score = extractScore(json);
                return score >= MIN_SCORE;
            }
        } catch (Exception e) {
            // Google unreachable — fail open so login is never permanently blocked
            System.out.println("reCAPTCHA verify error (fail open): " + e.getMessage());
            return true;
        }
    }

    private double extractScore(String json) {
        try {
            int idx = json.indexOf("\"score\"");
            if (idx == -1) return 1.0; // field absent → assume OK
            int colon = json.indexOf(':', idx);
            int end   = json.indexOf(',', colon);
            if (end == -1) end = json.indexOf('}', colon);
            return Double.parseDouble(json.substring(colon + 1, end).trim());
        } catch (Exception e) {
            return 1.0;
        }
    }
}
