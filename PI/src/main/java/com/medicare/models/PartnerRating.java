package com.medicare.models;

import java.time.LocalDateTime;

public class PartnerRating {
    private int id;
    private int partnerId;
    private String comment;
    private int rating;
    private String sentiment;
    private LocalDateTime createdAt;
    private String userName;

    public PartnerRating() {
    }

    public PartnerRating(int id, int partnerId, String comment, int rating, String sentiment, LocalDateTime createdAt, String userName) {
        this.id = id;
        this.partnerId = partnerId;
        this.comment = comment;
        this.rating = rating;
        this.sentiment = sentiment;
        this.createdAt = createdAt;
        this.userName = userName;
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getPartnerId() {
        return partnerId;
    }

    public String getComment() {
        return comment;
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

    public String getUserName() {
        return userName;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setPartnerId(int partnerId) {
        this.partnerId = partnerId;
    }



    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}