package com.medicare.models;

public class Donation {
    private int id;
    private String nom;
    private String description;
    private String cause;
    private String image;
    private double objectifMontant;
    private double montantActuel;

    public Donation() {}

    public Donation(int id, String nom, String description, String cause, String image, double objectifMontant) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.cause = cause;
        this.image = image;
        this.objectifMontant = objectifMontant;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCause() { return cause; }
    public void setCause(String cause) { this.cause = cause; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public double getObjectifMontant() { return objectifMontant; }
    public void setObjectifMontant(double objectifMontant) { this.objectifMontant = objectifMontant; }

    public double getMontantActuel() { return montantActuel; }
    public void setMontantActuel(double montantActuel) { this.montantActuel = montantActuel; }

    public double getPourcentage() {
        if (objectifMontant <= 0) return 0;
        double p = (montantActuel / objectifMontant) * 100;
        return Math.min(p, 100);
    }

    @Override
    public String toString() {
        return "Donation{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", cause='" + cause + '\'' +
                '}';
    }
}
