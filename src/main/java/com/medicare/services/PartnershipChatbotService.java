package com.medicare.services;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class PartnershipChatbotService {

    private static final Dotenv DOTENV = Dotenv.configure().ignoreIfMissing().load();
    private static final String API_KEY = readApiKey();

    public String getResponse(String userInput) {
        if (API_KEY == null || API_KEY.isBlank()) {
            return "AI service is not configured. Set GEMINI_API_KEY to enable partnership insights.";
        }

        try {
            String apiUrl = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + API_KEY;
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonPayload = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapeJson(userInput) + "\"}]}]}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) response.append(line.trim());
                    return parseResponse(response.toString());
                }
            }
            return "AI service request failed. Status: " + responseCode;
        } catch (Exception e) {
            return "AI service request failed: " + e.getMessage();
        }
    }

    private static String readApiKey() {
        String env = System.getenv("GEMINI_API_KEY");
        if (env != null && !env.isBlank()) return env.trim();
        String dotenvValue = DOTENV.get("GEMINI_API_KEY");
        return dotenvValue == null ? "" : dotenvValue.trim();
    }

    private String parseResponse(String jsonResponse) {
        int textIndex = jsonResponse.indexOf("\"text\": \"");
        if (textIndex == -1) return "Could not parse the AI response.";
        int startIndex = textIndex + 9;
        int endIndex = jsonResponse.indexOf("\"", startIndex);
        if (endIndex == -1) return "Could not parse the AI response.";
        return jsonResponse.substring(startIndex, endIndex).replace("\\n", "\n");
    }

    private String escapeJson(String text) {
        return text == null ? "" : text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
