package com.medicare.services;

import com.medicare.models.Collaboration;
import com.medicare.models.Partner;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class OpportunityRadarService {

    private final PartnerService partnerService;
    private final CollaborationService collaborationService;
    private final ChatbotService chatbotService; // Re-using the existing Gemini service

    // Simple in-memory cache to avoid hitting API rate limits
    private static String cachedInsights = null;
    private static LocalDateTime cacheTimestamp = null;

    public OpportunityRadarService() {
        this.partnerService = new PartnerService();
        this.collaborationService = new CollaborationService();
        this.chatbotService = new ChatbotService();
    }

    public String generateInsights() {
        // Check if we have a recent, valid cache
        if (cachedInsights != null && cacheTimestamp != null && Duration.between(cacheTimestamp, LocalDateTime.now()).toHours() < 1) {
            System.out.println("Returning cached AI insights.");
            return cachedInsights;
        }

        System.out.println("Fetching new AI insights from the API.");
        // Step 1: Fetch all data from the database
        List<Partner> allPartners = partnerService.getAll();
        List<Collaboration> allCollaborations = collaborationService.getAll();

        // Step 2: Format the data into a comprehensive context string for the AI
        String context = buildContext(allPartners, allCollaborations);

        // Step 3: Create a powerful prompt for the AI
        String prompt = buildPrompt(context);

        try {
            // Step 4: Get the raw insights from the AI
            String newInsights = chatbotService.getResponse(prompt);

            // Step 5: Cache the new response
            cachedInsights = newInsights;
            cacheTimestamp = LocalDateTime.now();

            return newInsights;
        } catch (Exception e) {
            // If the API call fails, return the old cached version if it exists, otherwise return an error message.
            e.printStackTrace();
            if (cachedInsights != null) {
                return cachedInsights; // Fallback to stale cache
            }
            return "Error: AI service request failed. Status: " + e.getMessage();
        }
    }

    private String buildContext(List<Partner> partners, List<Collaboration> collaborations) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Database Snapshot ===\n");

        sb.append("\n--- Partners ---\n");
        for (Partner p : partners) {
            sb.append(String.format("ID: %d, Name: %s, Type: %s, Status: %s\n",
                    p.getId(), p.getName(), p.getTypePartenaire(), p.getStatut()));
        }

        sb.append("\n--- Collaborations ---\n");
        for (Collaboration c : collaborations) {
            sb.append(String.format("ID: %d, Title: %s, Partner ID: %d, Status: %s, Start: %s, End: %s\n",
                    c.getId(), c.getTitre(), c.getPartnerId(), c.getStatut(), c.getDateDebut(), c.getDateFin()));
        }
        
        sb.append("\n=== End of Snapshot ===\n");
        return sb.toString();
    }

    private String buildPrompt(String context) {
        return "You are a world-class strategic advisor for a healthcare organization. " +
               "Analyze the following data snapshot of our partners and collaborations. " +
               "Your task is to identify 3-4 actionable, strategic insights. " +
               "For each insight, provide a clear title (like 'OPPORTUNITY:', 'SYNERGY:', or 'RISK:'), a brief analysis, and a concrete 'Suggestion:'. " +
               "Focus on finding non-obvious connections, potential risks, and untapped opportunities. " +
               "Do not just summarize the data; provide true strategic advice.\n\n" +
               context;
    }
}