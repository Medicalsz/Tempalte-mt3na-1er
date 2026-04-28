package com.medicare.controllers;

import com.medicare.HelloApplication;
import com.medicare.models.*;
import com.medicare.services.RendezVousService;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class RendezVousFormController {

    @FXML private ComboBox<Specialite> specialiteCombo;
    @FXML private ComboBox<Medecin> medecinCombo;
    @FXML private DatePicker datePicker;
    @FXML private FlowPane creneauxPane;
    @FXML private Label selectedHeureLabel;
    @FXML private Button btnValider;
    @FXML private Button btnRetour;
    @FXML private Label errorLabel;
    @FXML private Label formTitle;

    private final RendezVousService service = new RendezVousService();
    private StackPane contentArea;
    private int patientId;
    private LocalTime selectedHeure;
    private RendezVous rvToEdit;

    public void setContentArea(StackPane contentArea) { this.contentArea = contentArea; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public void setRendezVousToEdit(RendezVous rv) {
        this.rvToEdit = rv;
        formTitle.setText("Modifier le rendez-vous");
    }

    @FXML
    private void initialize() {
        // Icône flèche retour
        FontIcon arrowIcon = new FontIcon(FontAwesomeSolid.ARROW_LEFT);
        arrowIcon.setIconSize(16);
        arrowIcon.setIconColor(Color.web("#1a73e8"));
        btnRetour.setGraphic(arrowIcon);

        // Charger les spécialités
        specialiteCombo.setItems(FXCollections.observableArrayList(service.getAllSpecialites()));

        // Quand on choisit une spécialité → charger les médecins
        specialiteCombo.setOnAction(e -> {
            Specialite selected = specialiteCombo.getValue();
            if (selected != null) {
                List<Medecin> medecins = service.getMedecinsBySpecialite(selected.getId());
                medecinCombo.setItems(FXCollections.observableArrayList(medecins));
                medecinCombo.setDisable(false);
                creneauxPane.getChildren().clear();
                selectedHeure = null;
                selectedHeureLabel.setText("");
            }
        });

        // Quand on choisit un médecin → activer le date picker
        medecinCombo.setOnAction(e -> {
            if (medecinCombo.getValue() != null) {
                datePicker.setDisable(false);
                datePicker.setValue(null);
                creneauxPane.getChildren().clear();
                selectedHeure = null;
                selectedHeureLabel.setText("");
            }
        });

        // Quand on choisit une date → charger les créneaux
        datePicker.setOnAction(e -> loadCreneaux());

        // Pas de dates passées
        datePicker.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(empty || item.isBefore(LocalDate.now()));
            }
        });

        medecinCombo.setDisable(true);
        datePicker.setDisable(true);
    }

    private void loadCreneaux() {
        creneauxPane.getChildren().clear();
        selectedHeure = null;
        selectedHeureLabel.setText("");

        Medecin medecin = medecinCombo.getValue();
        LocalDate date = datePicker.getValue();
        if (medecin == null || date == null) return;

        List<LocalTime> tousCreneaux = service.getCreneauxDisponibles(medecin.getId(), date);
        List<LocalTime> prises = service.getHeuresPrises(medecin.getId(), date);

        if (tousCreneaux.isEmpty()) {
            Label noSlot = new Label("Le medecin ne travaille pas ce jour.");
            noSlot.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 13px;");
            creneauxPane.getChildren().add(noSlot);
            return;
        }

        for (LocalTime creneau : tousCreneaux) {
            Button btn = new Button(creneau.toString());
            btn.setPrefWidth(80);
            btn.setPrefHeight(35);

            boolean pris = prises.contains(creneau);
            if (pris) {
                // Rouge, désactivé
                btn.setStyle("-fx-background-color: #fecaca; -fx-text-fill: #dc2626; " +
                             "-fx-background-radius: 8; -fx-font-size: 12px;");
                btn.setDisable(true);
            } else {
                // Vert/disponible
                btn.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; " +
                             "-fx-background-radius: 8; -fx-font-size: 12px; -fx-cursor: hand;");
                btn.setOnAction(e -> {
                    selectedHeure = creneau;
                    selectedHeureLabel.setText("Heure choisie : " + creneau);
                    // Reset les styles de tous les boutons
                    for (Node n : creneauxPane.getChildren()) {
                        if (n instanceof Button b && !b.isDisable()) {
                            b.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; " +
                                       "-fx-background-radius: 8; -fx-font-size: 12px; -fx-cursor: hand;");
                        }
                    }
                    // Highlight le bouton sélectionné
                    btn.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; " +
                                 "-fx-background-radius: 8; -fx-font-size: 12px; -fx-cursor: hand;");
                });
            }
            creneauxPane.getChildren().add(btn);
        }
    }

    @FXML
    private void onValiderClick() {
        if (specialiteCombo.getValue() == null) { showError("Choisissez une specialite."); return; }
        if (medecinCombo.getValue() == null) { showError("Choisissez un medecin."); return; }
        if (datePicker.getValue() == null) { showError("Choisissez une date."); return; }
        if (selectedHeure == null) { showError("Choisissez un creneau horaire."); return; }

        Medecin medecin = medecinCombo.getValue();

        if (rvToEdit != null) {
            rvToEdit.setMedecinId(medecin.getId());
            rvToEdit.setDate(datePicker.getValue());
            rvToEdit.setHeure(selectedHeure);
            service.update(rvToEdit);
            showSuccessPopup("Rendez-vous modifie",
                    "Votre rendez-vous a ete modifie avec succes.",
                    FontAwesomeSolid.CALENDAR_CHECK, "#f59e0b");
        } else {
            RendezVous rv = new RendezVous(medecin.getId(), patientId,
                    datePicker.getValue(), selectedHeure, "en_attente");
            service.create(rv);
            showSuccessPopup("Rendez-vous pris !",
                    "Votre rendez-vous a ete enregistre avec succes.",
                    FontAwesomeSolid.CHECK_CIRCLE, "#16a34a");
        }
    }

    private void showSuccessPopup(String title, String message, FontAwesomeSolid iconType, String color) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(specialiteCombo.getScene().getWindow());

        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(48);
        icon.setIconColor(Color.web(color));

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1a73e8;");

        Label msgLbl = new Label(message);
        msgLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #555; -fx-text-alignment: center;");
        msgLbl.setWrapText(true);

        VBox box = new VBox(15, icon, titleLbl, msgLbl);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 16; " +
                     "-fx-border-color: #e0e0e0; -fx-border-radius: 16; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 4);");

        popup.setScene(new Scene(box, 340, 220));
        popup.show();

        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> {
            popup.close();
            goBackToList();
        });
        pause.play();
    }

    @FXML
    private void onRetourClick() {
        goBackToList();
    }

    private void goBackToList() {
        if (contentArea == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("rendez-vous-list-view.fxml"));
            Node list = loader.load();
            RendezVousListController ctrl = loader.getController();
            ctrl.setContentArea(contentArea);
            ctrl.setPatientId(patientId);
            contentArea.getChildren().clear();
            contentArea.getChildren().add(list);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showError(String msg) {
        errorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 13px;");
        errorLabel.setText(msg);
    }
}

