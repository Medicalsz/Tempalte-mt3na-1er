package com.medicare.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class User {
    private int id;
    private String nom;
    private String prenom;
    private String username;
    private String email;
    private String password;
    private String numero;
    private String adresse;
    private String photo;
    private String coverPhoto;
    private boolean isVerified;
    private String roles;
    private LocalDate dateNaissance;
    private boolean wantsEmailNotifications = true;
    private String passwordResetToken;
    private LocalDateTime passwordResetTokenExpiry;
    private LocalDateTime lastLoginAt;

    // Google OAuth
    private String googleId;
    private String googleAccessToken;

    // Location
    private Double latitude;
    private Double longitude;
    private String city;

    // Profile
    private String gender;
    private String bloodType;
    private String allergies;
    private boolean profileCompleted;
    private int skipCount;
    private String privacyLevel = "public";

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Admin/Doctor specific
    private Double ratingAverage;
    private String biometricId;

    public User() {}

    public Double getRatingAverage() { return ratingAverage; }
    public void setRatingAverage(Double ratingAverage) { this.ratingAverage = ratingAverage; }

    public String getBiometricId() { return biometricId; }
    public void setBiometricId(String biometricId) { this.biometricId = biometricId; }

    public User(String nom, String prenom, String email, String password,
                String numero, String adresse, String photo, String roles, boolean isVerified) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.numero = numero;
        this.adresse = adresse;
        this.photo = photo;
        this.roles = roles;
        this.isVerified = isVerified;
    }

    // --- ID ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    // --- Basic Info ---
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public String getCoverPhoto() { return coverPhoto; }
    public void setCoverPhoto(String coverPhoto) { this.coverPhoto = coverPhoto; }

    public boolean isVerified() { return isVerified; }
    public void setIsVerified(boolean isVerified) { this.isVerified = isVerified; }

    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }
    public boolean hasRole(String role) { return roles != null && roles.contains(role); }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }

    public boolean isWantsEmailNotifications() { return wantsEmailNotifications; }
    public void setWantsEmailNotifications(boolean wantsEmailNotifications) { this.wantsEmailNotifications = wantsEmailNotifications; }

    public String getPasswordResetToken() { return passwordResetToken; }
    public void setPasswordResetToken(String passwordResetToken) { this.passwordResetToken = passwordResetToken; }

    public LocalDateTime getPasswordResetTokenExpiry() { return passwordResetTokenExpiry; }
    public void setPasswordResetTokenExpiry(LocalDateTime passwordResetTokenExpiry) { this.passwordResetTokenExpiry = passwordResetTokenExpiry; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

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

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    // --- Profile ---
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }

    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }

    public boolean isProfileCompleted() { return profileCompleted; }
    public void setProfileCompleted(boolean profileCompleted) { this.profileCompleted = profileCompleted; }

    public int getSkipCount() { return skipCount; }
    public void setSkipCount(int skipCount) { this.skipCount = skipCount; }

    public String getPrivacyLevel() { return privacyLevel; }
    public void setPrivacyLevel(String privacyLevel) { this.privacyLevel = privacyLevel; }

    // --- Audit ---
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "User{id=" + id + ", nom='" + nom + "', prenom='" + prenom + "', email='" + email + "'}";
    }
}
