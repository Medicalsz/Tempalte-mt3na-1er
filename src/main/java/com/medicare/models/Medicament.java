package com.medicare.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Medicament {
    private int id;
    private int userId;
    private String nom;
    private String dosage;
    private String frequence;
    private String heurePrise;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private int stockActuel;
    private int stockMinimum = 5;
    private boolean estActif = true;
    private String notes;
    private LocalDateTime createdAt;

    public Medicament() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getFrequence() { return frequence; }
    public void setFrequence(String frequence) { this.frequence = frequence; }

    public String getHeurePrise() { return heurePrise; }
    public void setHeurePrise(String heurePrise) { this.heurePrise = heurePrise; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public int getStockActuel() { return stockActuel; }
    public void setStockActuel(int stockActuel) { this.stockActuel = stockActuel; }

    public int getStockMinimum() { return stockMinimum; }
    public void setStockMinimum(int stockMinimum) { this.stockMinimum = stockMinimum; }

    public boolean isEstActif() { return estActif; }
    public void setEstActif(boolean estActif) { this.estActif = estActif; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isLowStock() { return stockActuel <= stockMinimum; }

    @Override
    public String toString() {
        return "Medicament{" + nom + " - " + dosage + " (" + frequence + ")}";
    }
}
