package com.medicare.models;

import java.time.LocalDateTime;

public class Avis {
    private int id;
    private int userId;
    private int medecinId;
    private int note; // 1 to 5
    private String commentaire;
    private LocalDateTime createdAt;

    // Jointure display fields
    private String userName;
    private String medecinName;

    public Avis() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getMedecinId() { return medecinId; }
    public void setMedecinId(int medecinId) { this.medecinId = medecinId; }

    public int getNote() { return note; }
    public void setNote(int note) { this.note = Math.max(1, Math.min(5, note)); }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getMedecinName() { return medecinName; }
    public void setMedecinName(String medecinName) { this.medecinName = medecinName; }

    public String getStarDisplay() {
        return "★".repeat(note) + "☆".repeat(5 - note);
    }

    @Override
    public String toString() {
        return "Avis{" + getStarDisplay() + " - " + commentaire + "}";
    }
}
