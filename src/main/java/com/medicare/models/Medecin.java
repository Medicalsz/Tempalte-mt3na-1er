package com.medicare.models;

import java.time.LocalDateTime;

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
    private String password;
    private String numero;
    private String adresse;
    private String ville;
    private double prixConsultation;
    private String photo;
    private String certificate;
    private boolean isVerified;
    private String dateNaissance;
    private boolean wantsEmailNotifications = true;
    private int rank;
    private LocalDateTime lastLoginAt;
    private int profileViews;

    // Google OAuth
    private String googleId;
    private String googleAccessToken;

    // Location (CRITICAL — Nearest Doctor)
    private Double latitude;
    private Double longitude;

    // Doctor profile quality
    private Integer experienceYears;
    private int consultationDuration = 30;
    private boolean isAvailableOnline;
    private double ratingAverage;
    private int ratingCount;
    private String disponibilite; // JSON: available slots
    private String languages;
    private String gender;
    private String privacyLevel = "public";
    private boolean profileCompleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Medecin() {}

    // --- ID ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    // --- Basic Info ---
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

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public double getPrixConsultation() { return prixConsultation; }
    public void setPrixConsultation(double prixConsultation) { this.prixConsultation = prixConsultation; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public String getCertificate() { return certificate; }
    public void setCertificate(String certificate) { this.certificate = certificate; }

    public boolean isVerified() { return isVerified; }
    public void setIsVerified(boolean isVerified) { this.isVerified = isVerified; }

    public String getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(String dateNaissance) { this.dateNaissance = dateNaissance; }

    public boolean isWantsEmailNotifications() { return wantsEmailNotifications; }
    public void setWantsEmailNotifications(boolean wantsEmailNotifications) { this.wantsEmailNotifications = wantsEmailNotifications; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public int getProfileViews() { return profileViews; }
    public void setProfileViews(int profileViews) { this.profileViews = profileViews; }

    // --- Google OAuth ---
    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }

    public String getGoogleAccessToken() { return googleAccessToken; }
    public void setGoogleAccessToken(String googleAccessToken) { this.googleAccessToken = googleAccessToken; }

    // --- Location ---
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    // --- Doctor Profile ---
    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }

    public int getConsultationDuration() { return consultationDuration; }
    public void setConsultationDuration(int consultationDuration) { this.consultationDuration = consultationDuration; }

    public boolean isAvailableOnline() { return isAvailableOnline; }
    public void setAvailableOnline(boolean availableOnline) { isAvailableOnline = availableOnline; }

    public double getRatingAverage() { return ratingAverage; }
    public void setRatingAverage(double ratingAverage) { this.ratingAverage = ratingAverage; }

    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }

    public String getDisponibilite() { return disponibilite; }
    public void setDisponibilite(String disponibilite) { this.disponibilite = disponibilite; }

    public String getLanguages() { return languages; }
    public void setLanguages(String languages) { this.languages = languages; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getPrivacyLevel() { return privacyLevel; }
    public void setPrivacyLevel(String privacyLevel) { this.privacyLevel = privacyLevel; }

    public boolean isProfileCompleted() { return profileCompleted; }
    public void setProfileCompleted(boolean profileCompleted) { this.profileCompleted = profileCompleted; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // --- Display helpers ---
    public String getFullName() { return prenom + " " + nom; }

    @Override
    public String toString() { return "Dr. " + prenom + " " + nom + " (" + specialite + ")"; }
}
