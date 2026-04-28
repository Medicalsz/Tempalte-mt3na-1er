package com.medicare.models;

public class User {
    private int id;
    private String nom;
    private String email;
    private String password;
    private String numero;
    private String roles;
    private boolean isVerified;

    public User() {}

    public User(String nom, String email, String password,
                String numero, String roles, boolean isVerified) {
        this.nom = nom;
        this.email = email;
        this.password = password;
        this.numero = numero;
        this.roles = roles;
        this.isVerified = isVerified;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }

    public boolean isVerified() { return isVerified; }
    public void setIsVerified(boolean isVerified) { this.isVerified = isVerified; }

    @Override
    public String toString() {
        return "User{id=" + id + ", nom='" + nom + "', email='" + email + "'}";
    }
}