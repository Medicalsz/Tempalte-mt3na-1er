package com.medicare.models;

import java.time.LocalDateTime;

public class Evaluation {
    private int id;
    private int rendezVousId;
    private int patientId;
    private int medecinId;
    private int note;             // note globale 1-5
    private int notePonctualite;
    private int noteEcoute;
    private int noteClarte;
    private String commentaire;
    private LocalDateTime createdAt;

    // Champs jointure (affichage côté médecin)
    private String patientNom;
    private String patientPrenom;

    public Evaluation() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRendezVousId() { return rendezVousId; }
    public void setRendezVousId(int rendezVousId) { this.rendezVousId = rendezVousId; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public int getMedecinId() { return medecinId; }
    public void setMedecinId(int medecinId) { this.medecinId = medecinId; }

    public int getNote() { return note; }
    public void setNote(int note) { this.note = note; }

    public int getNotePonctualite() { return notePonctualite; }
    public void setNotePonctualite(int n) { this.notePonctualite = n; }

    public int getNoteEcoute() { return noteEcoute; }
    public void setNoteEcoute(int n) { this.noteEcoute = n; }

    public int getNoteClarte() { return noteClarte; }
    public void setNoteClarte(int n) { this.noteClarte = n; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String c) { this.commentaire = c; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime d) { this.createdAt = d; }

    public String getPatientNom() { return patientNom; }
    public void setPatientNom(String n) { this.patientNom = n; }

    public String getPatientPrenom() { return patientPrenom; }
    public void setPatientPrenom(String p) { this.patientPrenom = p; }

    public String getPatientFullName() {
        return (patientPrenom != null ? patientPrenom : "") + " " +
               (patientNom != null ? patientNom : "");
    }

    public static class Stats {
        public double moyenneGlobale;
        public double moyennePonctualite;
        public double moyenneEcoute;
        public double moyenneClarte;
        public int total;
    }
}

