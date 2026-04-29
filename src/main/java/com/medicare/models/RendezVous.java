package com.medicare.models;

import java.time.LocalDate;
import java.time.LocalTime;

public class RendezVous {
    private int id;
    private int medecinId;
    private int patientId;
    private LocalDate date;
    private LocalTime heure;
    private String statut; // en_attente, confirme, annule, termine
    private String motifAnnulation;
    private String motif; // Motif / symptômes saisis par le patient lors de la prise du RDV
    private LocalDate proposedDate;
    private LocalTime proposedHeure;
    private boolean reportPending;

    // Champs pour affichage (jointure)
    private String medecinNom;
    private String medecinPrenom;
    private String specialite;
    private String patientNom;
    private String patientPrenom;

    public RendezVous() {}

    public RendezVous(int medecinId, int patientId, LocalDate date, LocalTime heure, String statut) {
        this.medecinId = medecinId;
        this.patientId = patientId;
        this.date = date;
        this.heure = heure;
        this.statut = statut;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMedecinId() { return medecinId; }
    public void setMedecinId(int medecinId) { this.medecinId = medecinId; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getHeure() { return heure; }
    public void setHeure(LocalTime heure) { this.heure = heure; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getMotifAnnulation() { return motifAnnulation; }
    public void setMotifAnnulation(String motifAnnulation) { this.motifAnnulation = motifAnnulation; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public LocalDate getProposedDate() { return proposedDate; }
    public void setProposedDate(LocalDate proposedDate) { this.proposedDate = proposedDate; }

    public LocalTime getProposedHeure() { return proposedHeure; }
    public void setProposedHeure(LocalTime proposedHeure) { this.proposedHeure = proposedHeure; }

    public boolean isReportPending() { return reportPending; }
    public void setReportPending(boolean reportPending) { this.reportPending = reportPending; }

    public String getMedecinNom() { return medecinNom; }
    public void setMedecinNom(String medecinNom) { this.medecinNom = medecinNom; }

    public String getMedecinPrenom() { return medecinPrenom; }
    public void setMedecinPrenom(String medecinPrenom) { this.medecinPrenom = medecinPrenom; }

    public String getSpecialite() { return specialite; }
    public void setSpecialite(String specialite) { this.specialite = specialite; }

    public String getMedecinFullName() {
        return (medecinPrenom != null ? medecinPrenom : "") + " " + (medecinNom != null ? medecinNom : "");
    }

    public String getPatientNom() { return patientNom; }
    public void setPatientNom(String patientNom) { this.patientNom = patientNom; }

    public String getPatientPrenom() { return patientPrenom; }
    public void setPatientPrenom(String patientPrenom) { this.patientPrenom = patientPrenom; }

    public String getPatientFullName() {
        return (patientPrenom != null ? patientPrenom : "") + " " + (patientNom != null ? patientNom : "");
    }
}
