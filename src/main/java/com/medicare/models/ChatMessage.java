package com.medicare.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChatMessage {
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    private String role;
    private String author;
    private String content;
    private LocalDateTime createdAt;
    private boolean typing;
    private boolean error;
    private List<ChatAssistantRecommendation> recommendations = new ArrayList<>();

    public ChatMessage() {
        this.createdAt = LocalDateTime.now();
    }

    public ChatMessage(String role, String author, String content) {
        this();
        this.role = role;
        this.author = author;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isTyping() {
        return typing;
    }

    public void setTyping(boolean typing) {
        this.typing = typing;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public List<ChatAssistantRecommendation> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<ChatAssistantRecommendation> recommendations) {
        this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
    }

    public boolean isUser() {
        return ROLE_USER.equalsIgnoreCase(role);
    }

    public boolean shouldIncludeInPrompt() {
        return !typing && !error && content != null && !content.isBlank();
    }
}
