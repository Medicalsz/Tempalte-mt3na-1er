package com.medicare.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.util.Base64;

public class GeminiService {
    private final String apiKey;
    
    // Liste des modèles identifiés via le diagnostic réussi (Mise à jour pour forcer le commit)
    private static final String[][] CONFIGS = {
        {"v1beta", "gemini-2.0-flash"},
        {"v1beta", "gemini-flash-latest"},
        {"v1beta", "gemini-pro-latest"},
        {"v1beta", "gemini-1.5-flash"},
        {"v1", "gemini-1.5-flash"}
    };

    private final HttpClient httpClient;

    public GeminiService(String apiKey) {
        this.apiKey = (apiKey != null) ? apiKey.trim() : "";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(20))
                .build();
        
        if (this.apiKey.isEmpty()) {
            System.err.println("GeminiService: ATTENTION - Clé API manquante !");
        }
    }

    public String analyzeImageCondition(String imagePath) throws IOException, InterruptedException {
        File file = new File(imagePath);
        if (!file.exists()) {
            System.err.println("GeminiService: Fichier introuvable -> " + imagePath);
            return "Fichier introuvable";
        }

        byte[] imageBytes = resizeImageIfNeeded(file);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String mimeType = getMimeType(file);

        // Construction du JSON
        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();

        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", "Analyse cette photo d'un objet. Quel est son état ? Réponds uniquement par l'un de ces termes : Très bon état, Bon état, En état, Mauvais état, ou Indéterminé.");
        parts.add(textPart);

        JsonObject imagePart = new JsonObject();
        JsonObject inlineData = new JsonObject();
        inlineData.addProperty("mime_type", mimeType);
        inlineData.addProperty("data", base64Image);
        imagePart.add("inline_data", inlineData);
        parts.add(imagePart);

        content.add("parts", parts);
        contents.add(content);
        requestBody.add("contents", contents);

        String jsonPayload = requestBody.toString();
        String lastError = "Erreur technique";

        // Nettoyage de la clé (par précaution)
        String cleanApiKey = apiKey.trim().replace("\n", "").replace("\r", "");

        // Tentative avec différentes URLs
        for (String[] config : CONFIGS) {
            String version = config[0];
            String model = config[1];
            
            // On tente deux formats d'URL par modèle
            String[] urlPatterns = {
                "https://generativelanguage.googleapis.com/%s/models/%s:generateContent?key=%s",
                "https://generativelanguage.googleapis.com/%s/%s:generateContent?key=%s" // Sans /models/
            };

            for (String pattern : urlPatterns) {
                String url = String.format(pattern, version, model, cleanApiKey);
                System.out.println("GeminiService: Essai " + model + " (" + version + ") via " + (pattern.contains("/models/") ? "standard" : "direct"));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                try {
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    String responseBody = response.body();

                    if (response.statusCode() == 200) {
                        JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                        if (jsonResponse.has("candidates")) {
                            JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
                            if (candidates.size() > 0) {
                                JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
                                if (firstCandidate.has("content")) {
                                    JsonArray resParts = firstCandidate.getAsJsonObject("content").getAsJsonArray("parts");
                                    if (resParts.size() > 0) {
                                        String result = resParts.get(0).getAsJsonObject().get("text").getAsString().trim();
                                        System.out.println("GeminiService: SUCCÈS !");
                                        return result;
                                    }
                                }
                            }
                        }
                    } else if (response.statusCode() == 404) {
                        lastError = "Erreur 404 (Non trouvé)";
                        continue; 
                    } else if (response.statusCode() == 400 && responseBody.contains("API_KEY_INVALID")) {
                        System.err.println("GeminiService: Clé rejetée par Google : " + responseBody);
                        lastError = "Clé API Invalide";
                    } else {
                        System.err.println("GeminiService: Erreur " + response.statusCode() + " : " + responseBody);
                        lastError = "Erreur API (" + response.statusCode() + ")";
                        // Si c'est un problème de quota ou autre erreur critique, on continue quand même sur les autres modèles
                    }
                } catch (Exception e) {
                    System.err.println("GeminiService: Erreur réseau : " + e.getMessage());
                    lastError = "Problème réseau";
                }
            }
        }

        // Si tout a échoué, on tente le diagnostic final
        try {
            System.out.println("GeminiService: Diagnostic final...");
            String listUrl = "https://generativelanguage.googleapis.com/v1beta/models?key=" + cleanApiKey;
            HttpRequest listRequest = HttpRequest.newBuilder().uri(URI.create(listUrl)).GET().build();
            HttpResponse<String> listResponse = httpClient.send(listRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("GeminiService: Réponse diagnostic : " + listResponse.statusCode() + " - " + listResponse.body());
        } catch (Exception e) {
            System.err.println("GeminiService: Échec diagnostic.");
        }

        return lastError;
    }

    private String getMimeType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".heic") || name.endsWith(".heif")) return "image/heic";
        return "image/jpeg";
    }

    private byte[] resizeImageIfNeeded(File file) throws IOException {
        BufferedImage originalImage = ImageIO.read(file);
        if (originalImage == null) return Files.readAllBytes(file.toPath());

        int maxWidth = 800;
        int maxHeight = 800;
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        if (width <= maxWidth && height <= maxHeight) {
            return Files.readAllBytes(file.toPath());
        }

        double scale = Math.min((double) maxWidth / width, (double) maxHeight / height);
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);

        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, "jpg", baos);
        return baos.toByteArray();
    }
}
