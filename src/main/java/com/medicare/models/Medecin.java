package com.medicare.models;

public class Medecin {
    private int id;
    private int userId;
    private String specialite;
    private String cabinet;
    private String bio;
    private int specialiteRefId;

    // Champs jointure user
    private String nom;
    private String prenom;
    private String email;

    public Medecin() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getSpecialite() { return specialite; }
    public void setSpecialite(String specialite) { this.specialite = specialite; }

    public String getCabinet() { return cabinet; }
    public void setCabinet(String cabinet) { this.cabinet = cabinet; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public int getSpecialiteRefId() { return specialiteRefId; }
    public void setSpecialiteRefId(int specialiteRefId) { this.specialiteRefId = specialiteRefId; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return prenom + " " + nom; }

    @Override
    public String toString() { return "Dr. " + prenom + " " + nom + " (" + specialite + ")"; }
}

