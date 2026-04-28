package com.medicare.models;

import java.time.LocalTime;

public class Disponibilite {
    private int id;
    private int medecinId;
    private String jourSemaine;
    private boolean ferme;
    private LocalTime matinDebut;
    private LocalTime matinFin;
    private LocalTime pauseDebut;
    private LocalTime pauseFin;
    private LocalTime apresMidiDebut;
    private LocalTime apresMidiFin;

    public Disponibilite() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMedecinId() { return medecinId; }
    public void setMedecinId(int medecinId) { this.medecinId = medecinId; }

    public String getJourSemaine() { return jourSemaine; }
    public void setJourSemaine(String jourSemaine) { this.jourSemaine = jourSemaine; }

    public boolean isFerme() { return ferme; }
    public void setFerme(boolean ferme) { this.ferme = ferme; }

    public LocalTime getMatinDebut() { return matinDebut; }
    public void setMatinDebut(LocalTime matinDebut) { this.matinDebut = matinDebut; }

    public LocalTime getMatinFin() { return matinFin; }
    public void setMatinFin(LocalTime matinFin) { this.matinFin = matinFin; }

    public LocalTime getPauseDebut() { return pauseDebut; }
    public void setPauseDebut(LocalTime pauseDebut) { this.pauseDebut = pauseDebut; }

    public LocalTime getPauseFin() { return pauseFin; }
    public void setPauseFin(LocalTime pauseFin) { this.pauseFin = pauseFin; }

    public LocalTime getApresMidiDebut() { return apresMidiDebut; }
    public void setApresMidiDebut(LocalTime apresMidiDebut) { this.apresMidiDebut = apresMidiDebut; }

    public LocalTime getApresMidiFin() { return apresMidiFin; }
    public void setApresMidiFin(LocalTime apresMidiFin) { this.apresMidiFin = apresMidiFin; }
}

