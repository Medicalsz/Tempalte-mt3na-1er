package com.medicare.models;

import java.time.LocalDateTime;

public class Comment {
    private int id;
    private int partnerId;
    private int userId;
    private String userName; // Nom de l'utilisateur qui a posté le commentaire
    private String content;
    private int rating; // Note sur 5
    private String sentiment;
    private LocalDateTime createdAt;

    // Constructeur complet
    public Comment(int id, int partnerId, int userId, String userName, String content, int rating, String sentiment, LocalDateTime createdAt) {
        this.id = id;
        this.partnerId = partnerId;
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.rating = rating;
        this.sentiment = sentiment;
        this.createdAt = createdAt;
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getPartnerId() {
        return partnerId;
    }

    public int getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getContent() {
        return content;
    }

    public int getRating() {
        return rating;
    }

    public String getSentiment() {
        return sentiment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}