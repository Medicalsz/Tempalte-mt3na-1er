package com.medicare.models;

import java.util.ArrayList;
import java.util.List;

public class ChatAssistantResponse {
    private String reply;
    private String intent;
    private double confidence;
    private boolean fallbackResponse;
    private String model;
    private List<ChatAssistantRecommendation> recommendations = new ArrayList<>();

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public boolean isFallbackResponse() {
        return fallbackResponse;
    }

    public void setFallbackResponse(boolean fallbackResponse) {
        this.fallbackResponse = fallbackResponse;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<ChatAssistantRecommendation> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<ChatAssistantRecommendation> recommendations) {
        this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
    }
}
