package com.medicare.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Partner {
    private int id;
    private String name;
    private String typePartenaire;
    private String email;
    private String telephone;
    private String imageName;
    private LocalDate datePartenariat;
    private String adresse;
    private String statut;
    private LocalDateTime updatedAt;

    public Partner() {}

    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTypePartenaire() {
        return typePartenaire;
    }

    public void setTypePartenaire(String typePartenaire) {
        this.typePartenaire = typePartenaire;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public LocalDate getDatePartenariat() {
        return datePartenariat;
    }

    public void setDatePartenariat(LocalDate datePartenariat) {
        this.datePartenariat = datePartenariat;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}