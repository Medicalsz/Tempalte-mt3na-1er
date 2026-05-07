package com.medicare.models;

import java.time.LocalDate;
import java.time.LocalTime;

public class ListeAttente {
    private int id;
    private int patientId;
    private int medecinId;
    private LocalDate date;
    private LocalTime heureSouhaitee;
    private int position;
    private boolean notifie;

    // Champs affichage
    private String medecinNom;
    private String medecinPrenom;
    private String specialite;

    public ListeAttente() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public int getMedecinId() { return medecinId; }
    public void setMedecinId(int medecinId) { this.medecinId = medecinId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getHeureSouhaitee() { return heureSouhaitee; }
    public void setHeureSouhaitee(LocalTime heureSouhaitee) { this.heureSouhaitee = heureSouhaitee; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public boolean isNotifie() { return notifie; }
    public void setNotifie(boolean notifie) { this.notifie = notifie; }

    public String getMedecinNom() { return medecinNom; }
    public void setMedecinNom(String medecinNom) { this.medecinNom = medecinNom; }

    public String getMedecinPrenom() { return medecinPrenom; }
    public void setMedecinPrenom(String medecinPrenom) { this.medecinPrenom = medecinPrenom; }

    public String getSpecialite() { return specialite; }
    public void setSpecialite(String specialite) { this.specialite = specialite; }

    public String getMedecinFullName() {
        return (medecinPrenom != null ? medecinPrenom : "") + " " + (medecinNom != null ? medecinNom : "");
    }
}

