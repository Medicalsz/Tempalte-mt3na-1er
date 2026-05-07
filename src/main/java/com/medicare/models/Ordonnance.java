package com.medicare.models;

import java.time.LocalDateTime;

public class Ordonnance {
    private int id;
    private int rendezVousId;
    private String contenu;
    private LocalDateTime dateCreation;

    // Champs pour affichage (jointure)
    private String medecinNom;
    private String medecinPrenom;
    private String specialite;
    private String cabinet;
    private String patientNom;
    private String patientPrenom;
    private String dateRdv;

    public Ordonnance() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRendezVousId() { return rendezVousId; }
    public void setRendezVousId(int rendezVousId) { this.rendezVousId = rendezVousId; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public String getMedecinNom() { return medecinNom; }
    public void setMedecinNom(String medecinNom) { this.medecinNom = medecinNom; }

    public String getMedecinPrenom() { return medecinPrenom; }
    public void setMedecinPrenom(String medecinPrenom) { this.medecinPrenom = medecinPrenom; }

    public String getSpecialite() { return specialite; }
    public void setSpecialite(String specialite) { this.specialite = specialite; }

    public String getCabinet() { return cabinet; }
    public void setCabinet(String cabinet) { this.cabinet = cabinet; }

    public String getPatientNom() { return patientNom; }
    public void setPatientNom(String patientNom) { this.patientNom = patientNom; }

    public String getPatientPrenom() { return patientPrenom; }
    public void setPatientPrenom(String patientPrenom) { this.patientPrenom = patientPrenom; }

    public String getDateRdv() { return dateRdv; }
    public void setDateRdv(String dateRdv) { this.dateRdv = dateRdv; }

    public String getMedecinFullName() { return medecinPrenom + " " + medecinNom; }
    public String getPatientFullName() { return patientPrenom + " " + patientNom; }
}

