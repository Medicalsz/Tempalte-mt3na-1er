package com.medicare.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Collaboration {

    private int id;
    private int partnerId;
    private String partnerName;
    private Integer userId;
    private String userName;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String titre;
    private String description;
    private String statut;
    private String imageName;
    private LocalDateTime updatedAt;

    public Collaboration() {
    }

    // Full constructor
    public Collaboration(int id, int partnerId, String partnerName, Integer userId, String userName, LocalDate dateDebut, LocalDate dateFin, String titre, String description, String statut, String imageName, LocalDateTime updatedAt) {
        this.id = id;
        this.partnerId = partnerId;
        this.partnerName = partnerName;
        this.userId = userId;
        this.userName = userName;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.titre = titre;
        this.description = description;
        this.statut = statut;
        this.imageName = imageName;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(int partnerId) {
        this.partnerId = partnerId;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}