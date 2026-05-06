package com.medicare.models;

import java.time.LocalDateTime;

public class Notification {
    private int id;
    private Integer userId;
    private Integer medecinId;
    private String type; // appointment, medication, system, reminder
    private String titre;
    private String message;
    private boolean isRead;
    private LocalDateTime createdAt;

    public Notification() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getMedecinId() { return medecinId; }
    public void setMedecinId(Integer medecinId) { this.medecinId = medecinId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Notification{" + type + " - " + titre + " [" + (isRead ? "read" : "unread") + "]}";
    }
}
