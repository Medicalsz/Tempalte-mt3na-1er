package com.medicare.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import io.github.cdimascio.dotenv.Dotenv;

public class ChatbotService {

    private static final Dotenv dotenv = Dotenv.load();
    private static final String API_KEY = dotenv.get("GEMINI_API_KEY");
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

    public String getResponse(String userInput) {
        if (API_KEY == null || API_KEY.isEmpty()) {
            return "Error: API key not found. Please set GEMINI_API_KEY in your .env file.";
        }

        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // This is the JSON payload for the Gemini API
            String jsonPayload = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapeJson(userInput) + "\"}]}]}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    // The response is JSON, we need to parse it to get the text
                    return parseResponse(response.toString());
                }
            } else {
                System.err.println("API Request failed with response code: " + responseCode);
                // Read the error stream for more details, which is crucial for debugging
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        errorResponse.append(responseLine.trim());
                    }
                    System.err.println("API Error Details: " + errorResponse.toString());
                    return "Error: AI service request failed. Status: " + responseCode + ". Check console for details.";
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: An exception occurred while contacting the AI service.";
        }
    }

    // A simple method to parse the text from the Gemini JSON response
    private String parseResponse(String jsonResponse) {
        try {
            // This is a very basic parser. For a real application, a JSON library like Gson or Jackson is better.
            int textIndex = jsonResponse.indexOf("\"text\": \"");
            if (textIndex != -1) {
                int startIndex = textIndex + 9; // length of "\"text\": \""
                int endIndex = jsonResponse.indexOf("\"", startIndex);
                return jsonResponse.substring(startIndex, endIndex).replace("\\n", "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Could not parse the AI's response.";
    }

    // Helper to escape special characters in the user input for the JSON payload
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\b", "\\b")
                   .replace("\f", "\\f")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}