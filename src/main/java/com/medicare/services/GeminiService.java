package com.medicare.services;

import java.io.File;
import java.io.IOException;

public class GeminiService {
    private final String apiKey;

    public GeminiService(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public String analyzeImageCondition(String imagePath) throws IOException {
        File file = new File(imagePath);
        if (!file.exists()) {
            return "Fichier introuvable";
        }
        if (apiKey.isBlank()) {
            return "Analyse non configuree";
        }

        // The donation workflow is integrated without storing API keys in source.
        // Configure GEMINI_API_KEY and replace this with the API call if live image analysis is required.
        return "Indetermine";
    }
}
