package com.medicare.models;

public class User {
    private int id;
    private String nom;
<<<<<<< HEAD
    private String email;
    private String password;
    private String numero;
=======
    private String prenom;
    private String email;
    private String password;
    private String numero;
    private String adresse;
    private String photo;
>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f
    private String roles;
    private boolean isVerified;

    public User() {}

<<<<<<< HEAD
    public User(String nom, String email, String password,
                String numero, String roles, boolean isVerified) {
        this.nom = nom;
        this.email = email;
        this.password = password;
        this.numero = numero;
=======
    public User(String nom, String prenom, String email, String password,
                String numero, String adresse, String photo, String roles, boolean isVerified) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.numero = numero;
        this.adresse = adresse;
        this.photo = photo;
>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f
        this.roles = roles;
        this.isVerified = isVerified;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

<<<<<<< HEAD
=======
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

<<<<<<< HEAD
=======
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f
    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }

    public boolean isVerified() { return isVerified; }
    public void setIsVerified(boolean isVerified) { this.isVerified = isVerified; }

    @Override
    public String toString() {
<<<<<<< HEAD
        return "User{id=" + id + ", nom='" + nom + "', email='" + email + "'}";
    }
}
=======
        return "User{id=" + id + ", nom='" + nom + "', prenom='" + prenom + "', email='" + email + "'}";
    }
}
>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f
