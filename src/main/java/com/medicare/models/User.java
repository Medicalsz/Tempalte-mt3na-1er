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
    private boolean isVerified;

    public User() {}

    public User(int id, String email, String password) {
        this.id = id;
        this.email = email;
        this.password = password;
    }

    public User(String email, String password) {
        this(0, email, password);
    }

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

    public boolean isVerified() { return isVerified; }
    public void setIsVerified(boolean isVerified) { this.isVerified = isVerified; }

    public boolean hasRole(String role) {
        return roles != null && role != null && roles.contains(role);
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", nom='" + nom + "', prenom='" + prenom + "', email='" + email + "'}";
    }
}
