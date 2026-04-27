package com.medicare.controllers;

import com.medicare.models.RendezVous;
import com.medicare.services.RendezVousService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminRdvListController {

    @FXML private VBox container;

    private final RendezVousService service = new RendezVousService();

    @FXML
    private void initialize() {
        loadRendezVous();
    }

    private void loadRendezVous() {
        container.getChildren().clear();

        // Header
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Tous les rendez-vous");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #7c3aed;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TextField searchField = new TextField();
        searchField.setPromptText("Rechercher...");
        searchField.setPrefWidth(220);
        searchField.setStyle("-fx-background-radius: 8; -fx-font-size: 13px;");
        searchField.textProperty().addListener((obs, old, val) -> filterRdv(val));

        header.getChildren().addAll(title, spacer, searchField);
        container.getChildren().add(header);

        // Tableau header
        HBox tableHeader = new HBox();
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        tableHeader.setPadding(new Insets(10, 15, 10, 15));
        tableHeader.setStyle("-fx-background-color: #7c3aed; -fx-background-radius: 8 8 0 0;");

        Label hPatient = colLabel("Patient"); HBox.setHgrow(hPatient, Priority.ALWAYS);
        Label hMedecin = colLabel("Medecin"); HBox.setHgrow(hMedecin, Priority.ALWAYS);
        Label hSpec = colLabel("Specialite"); HBox.setHgrow(hSpec, Priority.ALWAYS);
        Label hDate = colLabel("Date"); hDate.setMinWidth(90);
        Label hHeure = colLabel("Heure"); hHeure.setMinWidth(55);
        Label hStatut = colLabel("Statut"); hStatut.setMinWidth(90);
        Label hAction = colLabel(""); hAction.setMinWidth(45);

        tableHeader.getChildren().addAll(hPatient, hMedecin, hSpec, hDate, hHeure, hStatut, hAction);
        container.getChildren().add(tableHeader);

        List<RendezVous> rdvs = service.getAllRendezVous();
        addRows(rdvs);
    }

    private void addRows(List<RendezVous> rdvs) {
        while (container.getChildren().size() > 2) {
            container.getChildren().remove(2);
        }

        if (rdvs.isEmpty()) {
            Label empty = new Label("Aucun rendez-vous.");
            empty.setStyle("-fx-font-size: 14px; -fx-text-fill: #888; -fx-padding: 20;");
            container.getChildren().add(empty);
            return;
        }

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter heureFmt = DateTimeFormatter.ofPattern("HH:mm");

        for (int i = 0; i < rdvs.size(); i++) {
            RendezVous rv = rdvs.get(i);
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 15, 10, 15));
            row.setStyle("-fx-background-color: " + (i % 2 == 0 ? "white" : "#f9fafb") + ";");

            Label patient = new Label(rv.getPatientFullName());
            patient.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(patient, Priority.ALWAYS);
            patient.setStyle("-fx-font-size: 13px; -fx-text-fill: #333; -fx-font-weight: bold;");

            Label medecin = new Label("Dr. " + rv.getMedecinFullName());
            medecin.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(medecin, Priority.ALWAYS);
            medecin.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");

            Label spec = new Label(rv.getSpecialite() != null ? rv.getSpecialite() : "-");
            spec.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(spec, Priority.ALWAYS);
            spec.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");

            Label date = new Label(rv.getDate().format(dateFmt));
            date.setMinWidth(90);
            date.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

            Label heure = new Label(rv.getHeure().format(heureFmt));
            heure.setMinWidth(55);
            heure.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

            // Badge statut
            String statutColor = switch (rv.getStatut()) {
                case "confirme" -> "#16a34a";
                case "annule" -> "#dc2626";
                case "termine" -> "#6b7280";
                default -> "#f59e0b";
            };
            Label statut = new Label(rv.getStatut());
            statut.setPrefWidth(90);
            statut.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-padding: 2 8; " +
                            "-fx-background-color: " + statutColor + "; -fx-background-radius: 12;");

            // Bouton voir
            Button btnVoir = new Button();
            FontIcon eyeIcon = new FontIcon(FontAwesomeSolid.EYE);
            eyeIcon.setIconSize(13);
            eyeIcon.setIconColor(Color.web("#7c3aed"));
            btnVoir.setGraphic(eyeIcon);
            btnVoir.setStyle("-fx-background-color: #ede9fe; -fx-cursor: hand; -fx-padding: 5; -fx-background-radius: 6;");
            btnVoir.setTooltip(new Tooltip("Voir details"));
            btnVoir.setOnAction(e -> showDetails(rv));

            row.getChildren().addAll(patient, medecin, spec, date, heure, statut, btnVoir);
            container.getChildren().add(row);
        }
    }

    private Label colLabel(String text) {
        Label l = new Label(text);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");
        return l;
    }

    private void filterRdv(String search) {
        List<RendezVous> all = service.getAllRendezVous();
        if (search == null || search.trim().isEmpty()) {
            addRows(all);
            return;
        }
        String s = search.toLowerCase();
        List<RendezVous> filtered = all.stream()
            .filter(rv -> rv.getPatientFullName().toLowerCase().contains(s) ||
                          rv.getMedecinFullName().toLowerCase().contains(s) ||
                          rv.getStatut().toLowerCase().contains(s) ||
                          (rv.getSpecialite() != null && rv.getSpecialite().toLowerCase().contains(s)))
            .toList();
        addRows(filtered);
    }

    // ========== DETAILS MODAL ==========

    private void showDetails(RendezVous rv) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        FontIcon icon = new FontIcon(FontAwesomeSolid.CALENDAR_CHECK);
        icon.setIconSize(40);
        icon.setIconColor(Color.web("#7c3aed"));

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter heureFmt = DateTimeFormatter.ofPattern("HH:mm");

        Label titleLbl = new Label("Details du rendez-vous");
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #7c3aed;");

        Label details = new Label(
            "Patient :  " + rv.getPatientFullName() + "\n" +
            "Medecin :  Dr. " + rv.getMedecinFullName() + "\n" +
            "Specialite :  " + (rv.getSpecialite() != null ? rv.getSpecialite() : "-") + "\n" +
            "Date :  " + rv.getDate().format(dateFmt) + "\n" +
            "Heure :  " + rv.getHeure().format(heureFmt) + "\n" +
            "Statut :  " + rv.getStatut()
        );
        details.setStyle("-fx-font-size: 14px; -fx-text-fill: #444; -fx-line-spacing: 4;");

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-size: 13px; " +
                          "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 30;");
        closeBtn.setOnAction(e -> popup.close());

        VBox box = new VBox(12, icon, titleLbl, details, closeBtn);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 16; " +
                     "-fx-border-color: #e0e0e0; -fx-border-radius: 16; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 4);");

        popup.setScene(new Scene(box, 400, 350));
        popup.show();
    }
}

