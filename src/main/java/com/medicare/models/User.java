package com.medicare.models;

public class User {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String password;
    private String numero;
    private String adresse;
    private String photo;
    private String roles;
    private String emailPrivacy;
    private String phonePrivacy;
    private String adressePrivacy;
    private boolean isVerified;

    public User() {}

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

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

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

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }

    public String getEmailPrivacy() { return emailPrivacy; }
    public void setEmailPrivacy(String emailPrivacy) { this.emailPrivacy = emailPrivacy; }

    public String getPhonePrivacy() { return phonePrivacy; }
    public void setPhonePrivacy(String phonePrivacy) { this.phonePrivacy = phonePrivacy; }

    public String getAdressePrivacy() { return adressePrivacy; }
    public void setAdressePrivacy(String adressePrivacy) { this.adressePrivacy = adressePrivacy; }

    public boolean isVerified() { return isVerified; }
    public void setIsVerified(boolean isVerified) { this.isVerified = isVerified; }

    @Override
    public String toString() {
        return "User{id=" + id + ", nom='" + nom + "', prenom='" + prenom + "', email='" + email + "'}";
    }
}
