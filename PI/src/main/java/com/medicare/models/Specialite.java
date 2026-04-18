package com.medicare.models;

public class Specialite {
    private int id;
    private String nom;
    private String slug;
    private boolean active;

    public Specialite() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() { return nom; }
}

