package com.medicare.models;

import java.util.ArrayList;
import java.util.List;

public class ChatAssistantResponse {
    private String reply;
    private String intent;
    private double confidence;
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

    public List<ChatAssistantRecommendation> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<ChatAssistantRecommendation> recommendations) {
        this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
    }
}
