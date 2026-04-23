package com.medicare.controllers;

import com.medicare.HelloApplication;
import com.medicare.models.*;
import com.medicare.services.EmailService;
import com.medicare.services.ListeAttenteService;
import com.medicare.services.RendezVousService;
import com.medicare.services.UserService;
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

    private final RendezVousService   service             = new RendezVousService();
    private final EmailService        emailService        = new EmailService();
    private final UserService         userService         = new UserService();
    private final ListeAttenteService listeAttenteService = new ListeAttenteService();
    private StackPane contentArea;
    private int patientId;
    private LocalTime selectedHeure;
    private RendezVous rvToEdit;

    public void setContentArea(StackPane contentArea) { this.contentArea = contentArea; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    /**
     * Pre-selectionne une specialite par son nom (appele par le chatbot).
     * Le combo reste modifiable.
     */
    public void preselectSpecialite(String nomSpecialite) {
        if (nomSpecialite == null) return;
        for (Specialite s : specialiteCombo.getItems()) {
            if (s.getNom() != null && s.getNom().equalsIgnoreCase(nomSpecialite)) {
                specialiteCombo.setValue(s);
                // Déclencher le chargement des médecins
                List<Medecin> medecins = service.getMedecinsBySpecialite(s.getId());
                medecinCombo.setItems(FXCollections.observableArrayList(medecins));
                medecinCombo.setDisable(false);
                return;
            }
        }
    }

    public void setRendezVousToEdit(RendezVous rv) {
        this.rvToEdit = rv;
        formTitle.setText("Modifier le rendez-vous");

        // Pré-remplir et bloquer spécialité
        for (Specialite s : specialiteCombo.getItems()) {
            if (s.getNom() != null && s.getNom().equals(rv.getSpecialite())) {
                specialiteCombo.setValue(s);
                break;
            }
        }
        specialiteCombo.setDisable(true);

        // Charger les médecins de cette spécialité et pré-remplir
        if (specialiteCombo.getValue() != null) {
            List<Medecin> medecins = service.getMedecinsBySpecialite(specialiteCombo.getValue().getId());
            medecinCombo.setItems(FXCollections.observableArrayList(medecins));
            for (Medecin m : medecins) {
                if (m.getId() == rv.getMedecinId()) {
                    medecinCombo.setValue(m);
                    break;
                }
            }
        }
        medecinCombo.setDisable(true);

        // Activer le date picker directement
        datePicker.setDisable(false);
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

        // Vérifier si tous les créneaux sont pris
        boolean tousOccupes = prises.containsAll(tousCreneaux);
        if (tousOccupes && rvToEdit == null) {
            Label complet = new Label("Tous les créneaux sont pris ce jour.");
            complet.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 13px; -fx-font-weight: bold;");

            // Vérifier si déjà en attente
            boolean dejaEnAttente = listeAttenteService.estDejaEnAttente(patientId, medecin.getId(), date);

            if (dejaEnAttente) {
                Label dejaLabel = new Label("✅ Vous êtes déjà inscrit en liste d'attente pour ce jour.");
                dejaLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 13px;");
                creneauxPane.getChildren().addAll(complet, dejaLabel);
            } else {
                javafx.scene.control.Button btnAttente = new javafx.scene.control.Button("📋 M'inscrire en liste d'attente");
                btnAttente.setStyle("-fx-background-color: linear-gradient(to right, #f59e0b, #d97706); " +
                        "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; " +
                        "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 20;");
                final Medecin medecinFinal = medecin;
                final LocalDate dateFinal = date;
                btnAttente.setOnAction(ev -> {
                    listeAttenteService.inscrire(patientId, medecinFinal.getId(), dateFinal, null);
                    btnAttente.setDisable(true);
                    btnAttente.setText("✅ Inscrit en liste d'attente !");
                    btnAttente.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; " +
                            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 8 20;");
                });
                creneauxPane.getChildren().addAll(complet, btnAttente);
            }
            return;
        }

        for (LocalTime creneau : tousCreneaux) {
            Button btn = new Button(creneau.toString());
            btn.setPrefWidth(85);
            btn.setPrefHeight(38);

            boolean pris = prises.contains(creneau);
            if (pris) {
                btn.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; " +
                             "-fx-background-radius: 10; -fx-font-size: 12px; -fx-font-weight: bold;");
                btn.setDisable(true);
            } else {
                String normalStyle = "-fx-background-color: #f0fdf4; -fx-text-fill: #16a34a; " +
                        "-fx-background-radius: 10; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; " +
                        "-fx-border-color: #bbf7d0; -fx-border-radius: 10;";
                String selectedStyle = "-fx-background-color: linear-gradient(to right, #3b82f6, #6366f1); -fx-text-fill: white; " +
                        "-fx-background-radius: 10; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; " +
                        "-fx-border-color: transparent; -fx-border-radius: 10; " +
                        "-fx-effect: dropshadow(gaussian, rgba(59,130,246,0.3), 8, 0, 0, 2);";
                btn.setStyle(normalStyle);
                btn.setOnAction(e -> {
                    selectedHeure = creneau;
                    selectedHeureLabel.setText("Heure choisie : " + creneau);
                    for (Node n : creneauxPane.getChildren()) {
                        if (n instanceof Button b && !b.isDisable()) {
                            b.setStyle(normalStyle);
                        }
                    }
                    btn.setStyle(selectedStyle);
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
            // Bloqué si RDV actif avec le même médecin ce jour (sauf le RDV en cours de modif)
            if (service.patientADejaRdvCeJour(patientId, rvToEdit.getMedecinId(), datePicker.getValue(), rvToEdit.getId())) {
                showError("Vous avez déjà un rendez-vous avec ce médecin ce jour.");
                return;
            }
            String ancienneDate = rvToEdit.getDate().toString();
            rvToEdit.setDate(datePicker.getValue());
            rvToEdit.setHeure(selectedHeure);
            service.update(rvToEdit);
            // Email de notification au médecin
            User medecinUser = userService.getUserByMedecinId(rvToEdit.getMedecinId());
            User patient = userService.getUserByPatientId(patientId);
            if (medecinUser != null && medecinUser.getEmail() != null && patient != null) {
                emailService.envoyerReportParPatient(
                        medecinUser.getEmail(),
                        medecinUser.getPrenom() + " " + medecinUser.getNom(),
                        patient.getPrenom() + " " + patient.getNom(),
                        ancienneDate,
                        datePicker.getValue().toString(),
                        selectedHeure.toString()
                );
            }
            showSuccessPopup("Rendez-vous modifie",
                    "Votre rendez-vous a ete modifie avec succes.",
                    FontAwesomeSolid.CALENDAR_CHECK, "#f59e0b");
        } else {
            // Bloqué si RDV actif avec le même médecin ce jour
            if (service.patientADejaRdvCeJour(patientId, medecin.getId(), datePicker.getValue(), -1)) {
                showError("Vous avez déjà un rendez-vous avec ce médecin ce jour.");
                return;
            }
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
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(specialiteCombo.getScene().getWindow());

        StackPane iconCircle = new StackPane();
        iconCircle.setStyle("-fx-background-color: " + color + "20; -fx-background-radius: 50; " +
                            "-fx-pref-width: 70; -fx-pref-height: 70; -fx-min-width: 70; -fx-min-height: 70;");
        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(32);
        icon.setIconColor(Color.web(color));
        iconCircle.getChildren().add(icon);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label msgLbl = new Label(message);
        msgLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b; -fx-text-alignment: center;");
        msgLbl.setWrapText(true);

        javafx.scene.shape.Rectangle progressBar = new javafx.scene.shape.Rectangle(200, 4);
        progressBar.setFill(Color.web(color));
        progressBar.setArcWidth(4);
        progressBar.setArcHeight(4);

        VBox box = new VBox(18, iconCircle, titleLbl, msgLbl, progressBar);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(35));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 0; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 25, 0, 0, 8);");

        StackPane overlay = new StackPane(box);
        overlay.setStyle("-fx-background-color: rgba(15,23,42,0.4);");

        Scene scene = new Scene(overlay, 380, 270);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        popup.setScene(scene);
        popup.show();

        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.ZERO, new javafx.animation.KeyValue(progressBar.widthProperty(), 200)),
            new javafx.animation.KeyFrame(Duration.seconds(2), new javafx.animation.KeyValue(progressBar.widthProperty(), 0))
        );
        timeline.play();

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
            // Réafficher le sidebar
            javafx.scene.layout.BorderPane root = (javafx.scene.layout.BorderPane) contentArea.getScene().getRoot();
            Node sidebar = root.getLeft();
            if (sidebar != null) {
                sidebar.setVisible(true);
                sidebar.setManaged(true);
            }

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

