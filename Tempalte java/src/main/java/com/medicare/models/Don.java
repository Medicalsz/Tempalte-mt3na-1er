package com.medicare.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Don {
    private int id;
    private String donateurNom;
    private String donateurPrenom;
    private String donateurEmail;
    private String causeNom;
    private Double montant;
    private String materiels;
    private Integer quantite;
    private String adresse;
    private LocalDateTime date;
    private String mode;
    private String statut;
    private String type; // "argent" ou "materiel"
    private Double latitude;
    private Double longitude;
    private List<String> objectPhotos = new ArrayList<>();

    public Don() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDonateurNom() { return donateurNom; }
    public void setDonateurNom(String donateurNom) { this.donateurNom = donateurNom; }

    public String getDonateurPrenom() { return donateurPrenom; }
    public void setDonateurPrenom(String donateurPrenom) { this.donateurPrenom = donateurPrenom; }

    public String getDonateurEmail() { return donateurEmail; }
    public void setDonateurEmail(String donateurEmail) { this.donateurEmail = donateurEmail; }

    public String getCauseNom() { return causeNom; }
    public void setCauseNom(String causeNom) { this.causeNom = causeNom; }

    public Double getMontant() { return montant; }
    public void setMontant(Double montant) { this.montant = montant; }

    public String getMateriels() { return materiels; }
    public void setMateriels(String materiels) { this.materiels = materiels; }

    public Integer getQuantite() { return quantite; }
    public void setQuantite(Integer quantite) { this.quantite = quantite; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public List<String> getObjectPhotos() { return objectPhotos; }
    public void setObjectPhotos(List<String> objectPhotos) { this.objectPhotos = objectPhotos; }

    public String getFullDonateurName() {
        return (donateurPrenom != null ? donateurPrenom : "") + " " + (donateurNom != null ? donateurNom : "");
    }
}
