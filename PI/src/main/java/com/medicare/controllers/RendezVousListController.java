package com.medicare.controllers;

import com.medicare.models.RendezVous;
import com.medicare.services.RendezVousService;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class RendezVousListController {

    @FXML private VBox container;

    private final RendezVousService service = new RendezVousService();
    private int patientId;
    private StackPane contentArea;

    public void setContentArea(StackPane contentArea) { this.contentArea = contentArea; }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
        loadRendezVous();
    }

    private void reloadFullList() {
        if (contentArea == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                com.medicare.HelloApplication.class.getResource("rendez-vous-list-view.fxml"));
            Node view = loader.load();
            RendezVousListController ctrl = loader.getController();
            ctrl.setContentArea(contentArea);
            ctrl.setPatientId(patientId);
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadRendezVous() {
        container.getChildren().clear();

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(20);

        Label title = new Label("Mes Rendez-vous");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1a73e8;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnNew = new Button("  Prendre un rendez-vous");
        FontIcon plusIcon = new FontIcon(FontAwesomeSolid.PLUS_CIRCLE);
        plusIcon.setIconSize(16);
        plusIcon.setIconColor(Color.WHITE);
        btnNew.setGraphic(plusIcon);
        btnNew.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-size: 14px; " +
                        "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 20;");
        btnNew.setOnAction(e -> openForm(null));

        header.getChildren().addAll(title, spacer, btnNew);
        container.getChildren().add(header);

        List<RendezVous> rdvs = service.getByPatient(patientId);

        if (rdvs.isEmpty()) {
            Label empty = new Label("Aucun rendez-vous pour le moment.");
            empty.setStyle("-fx-font-size: 15px; -fx-text-fill: #888; -fx-padding: 40 0 0 0;");
            container.getChildren().add(empty);
            return;
        }

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter heureFmt = DateTimeFormatter.ofPattern("HH:mm");

        for (RendezVous rv : rdvs) {
            HBox card = new HBox(15);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(15));
            card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                          "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");

            FontIcon calIcon = new FontIcon(FontAwesomeSolid.CALENDAR_CHECK);
            calIcon.setIconSize(28);
            calIcon.setIconColor(Color.web("#1a73e8"));

            VBox infos = new VBox(3);
            HBox.setHgrow(infos, Priority.ALWAYS);

            Label medecinLabel = new Label("Dr. " + rv.getMedecinFullName());
            medecinLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #333;");

            Label specLabel = new Label(rv.getSpecialite());
            specLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");

            Label dateLabel = new Label(rv.getDate().format(dateFmt) + " a " + rv.getHeure().format(heureFmt));
            dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #555;");

            Label statutLabel = new Label(rv.getStatut());
            String statutColor = switch (rv.getStatut()) {
                case "confirme" -> "#16a34a";
                case "annule" -> "#dc2626";
                case "termine" -> "#6b7280";
                default -> "#f59e0b";
            };
            statutLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-padding: 2 10; " +
                                 "-fx-background-color: " + statutColor + "; -fx-background-radius: 12;");

            infos.getChildren().addAll(medecinLabel, specLabel, dateLabel, statutLabel);

            // Boutons d'action HORIZONTAUX
            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER);

            Button btnVoir = createActionBtn(FontAwesomeSolid.EYE, "#1a73e8", "#e8f0fe", "Voir");
            btnVoir.setOnAction(e -> showDetails(rv));
            actions.getChildren().add(btnVoir);

            if (!rv.getStatut().equals("annule") && !rv.getStatut().equals("termine")) {
                Button btnEdit = createActionBtn(FontAwesomeSolid.PEN, "#f59e0b", "#fef3c7", "Modifier");
                btnEdit.setOnAction(e -> openForm(rv));

                Button btnCancel = createActionBtn(FontAwesomeSolid.BAN, "#dc2626", "#fee2e2", "Annuler");
                btnCancel.setOnAction(e -> showCancelConfirmation(rv));

                actions.getChildren().addAll(btnEdit, btnCancel);
            }

            card.getChildren().addAll(calIcon, infos, actions);
            container.getChildren().add(card);
        }
    }

    private Button createActionBtn(FontAwesomeSolid iconType, String iconColor, String bgColor, String tooltipText) {
        Button btn = new Button();
        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(14);
        icon.setIconColor(Color.web(iconColor));
        btn.setGraphic(icon);
        btn.setStyle("-fx-background-color: " + bgColor + "; -fx-cursor: hand; -fx-padding: 6; " +
                     "-fx-background-radius: 8;");
        btn.setTooltip(new Tooltip(tooltipText));
        return btn;
    }

    // ========== CONFIRMATION ANNULATION ==========

    private void showCancelConfirmation(RendezVous rv) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        FontIcon warnIcon = new FontIcon(FontAwesomeSolid.EXCLAMATION_TRIANGLE);
        warnIcon.setIconSize(44);
        warnIcon.setIconColor(Color.web("#f59e0b"));

        Label titleLbl = new Label("Annuler ce rendez-vous ?");
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");

        Label msgLbl = new Label("Dr. " + rv.getMedecinFullName() + "\n" +
                rv.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " a " +
                rv.getHeure().format(DateTimeFormatter.ofPattern("HH:mm")));
        msgLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #666; -fx-text-alignment: center;");
        msgLbl.setWrapText(true);

        Button btnOui = new Button("Oui, annuler");
        btnOui.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-size: 13px; " +
                        "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 25;");

        Button btnNon = new Button("Non, garder");
        btnNon.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #333; -fx-font-size: 13px; " +
                        "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 25;");

        HBox buttons = new HBox(15, btnNon, btnOui);
        buttons.setAlignment(Pos.CENTER);

        VBox box = new VBox(15, warnIcon, titleLbl, msgLbl, buttons);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 16; " +
                     "-fx-border-color: #e0e0e0; -fx-border-radius: 16; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 4);");

        popup.setScene(new Scene(box, 380, 250));
        popup.show();

        btnNon.setOnAction(e -> popup.close());
        btnOui.setOnAction(e -> {
            popup.close();
            service.cancel(rv.getId());
            showSuccessPopup("Rendez-vous annule",
                    "Votre rendez-vous a ete annule avec succes.",
                    FontAwesomeSolid.CALENDAR_TIMES, "#dc2626");
        });
    }

    // ========== POPUP SUCCES ==========

    private void showSuccessPopup(String title, String message, FontAwesomeSolid iconType, String color) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

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
            reloadFullList();
        });
        pause.play();
    }

    // ========== DETAILS MODAL ==========

    private void showDetails(RendezVous rv) {
        RendezVous full = service.getById(rv.getId());
        if (full == null) return;

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter heureFmt = DateTimeFormatter.ofPattern("HH:mm");

        VBox modal = new VBox(12);
        modal.setAlignment(Pos.CENTER);
        modal.setPadding(new Insets(30));
        modal.setMaxWidth(420);
        modal.setMaxHeight(380);
        modal.setStyle("-fx-background-color: white; -fx-background-radius: 16; " +
                       "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 20, 0, 0, 5);");

        FontIcon icon = new FontIcon(FontAwesomeSolid.NOTES_MEDICAL);
        icon.setIconSize(40);
        icon.setIconColor(Color.web("#1a73e8"));

        Label titleLabel = new Label("Details du rendez-vous");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1a73e8;");

        Label details = new Label(
            "Medecin :  Dr. " + full.getMedecinFullName() + "\n" +
            "Specialite :  " + full.getSpecialite() + "\n" +
            "Date :  " + full.getDate().format(dateFmt) + "\n" +
            "Heure :  " + full.getHeure().format(heureFmt) + "\n" +
            "Statut :  " + full.getStatut()
        );
        details.setStyle("-fx-font-size: 14px; -fx-text-fill: #444; -fx-line-spacing: 4;");

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-size: 13px; " +
                          "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 30;");
        closeBtn.setOnAction(e -> reloadFullList());

        modal.getChildren().addAll(icon, titleLabel, details, closeBtn);

        StackPane overlay = new StackPane(modal);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.4);");
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) reloadFullList(); });

        contentArea.getChildren().clear();
        contentArea.getChildren().add(overlay);
    }

    // ========== NAVIGATION ==========

    private void openForm(RendezVous rvToEdit) {
        if (contentArea == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                com.medicare.HelloApplication.class.getResource("rendez-vous-form-view.fxml"));
            Node form = loader.load();
            RendezVousFormController ctrl = loader.getController();
            ctrl.setContentArea(contentArea);
            ctrl.setPatientId(patientId);
            if (rvToEdit != null) ctrl.setRendezVousToEdit(rvToEdit);
            contentArea.getChildren().clear();
            contentArea.getChildren().add(form);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
