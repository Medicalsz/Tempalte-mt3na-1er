package com.medicare.services;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Base64;

public class GeminiService {

    private static final String ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    private static final String PROMPT =
        "Évaluez l'état de cet objet donné. Répondez avec exactement l'une de ces catégories: " +
        "'très bon état', 'bon état', 'en état', 'mauvais état'. " +
        "Répondez uniquement avec la catégorie, sans aucun autre texte.";

    private static final Dotenv DOTENV = Dotenv.configure().ignoreIfMissing().load();

    private final String apiKey;
    private final HttpClient httpClient;

    public GeminiService(String apiKey) {
        String resolved = (apiKey != null && !apiKey.isBlank()) ? apiKey.trim() : DOTENV.get("GEMINI_API_KEY", "");
        this.apiKey = resolved == null ? "" : resolved.trim();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public String analyzeImageCondition(String imagePath) throws IOException {
        File file = new File(imagePath);
        if (!file.exists()) return "Fichier introuvable";
        if (apiKey.isBlank()) return "Analyse non configuree";

        byte[] imageBytes = Files.readAllBytes(file.toPath());
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        String mimeType = detectMimeType(imagePath);

        String body = "{"
            + "\"contents\":[{"
            + "\"parts\":["
            + "{\"text\":\"" + PROMPT + "\"},"
            + "{\"inline_data\":{\"mime_type\":\"" + mimeType + "\",\"data\":\"" + base64 + "\"}}"
            + "]"
            + "}]"
            + "}";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ENDPOINT + apiKey))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.out.println("Gemini API error: " + response.statusCode() + " " + response.body());
                return "Erreur API";
            }
            return extractText(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Délai dépassé";
        } catch (Exception e) {
            System.out.println("Gemini connexion erreur: " + e.getMessage());
            return "Erreur de connexion";
        }
    }

    private String extractText(String json) {
        int idx = json.indexOf("\"text\":");
        if (idx == -1) return "Indéterminé";
        int start = json.indexOf('"', idx + 7) + 1;
        int end = json.indexOf('"', start);
        if (start <= 0 || end <= 0) return "Indéterminé";
        return json.substring(start, end).trim().toLowerCase();
    }

    private String detectMimeType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }
}
