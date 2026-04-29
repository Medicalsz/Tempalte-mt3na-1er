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
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MedecinRdvListController {

    @FXML private VBox container;

    private final RendezVousService service = new RendezVousService();
    private int medecinId;
    private StackPane contentArea;

    public void setContentArea(StackPane contentArea) { this.contentArea = contentArea; }

    public void setMedecinId(int medecinId) {
        this.medecinId = medecinId;
        loadRendezVous();
    }

    private void reloadFullList() {
        if (contentArea == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                com.medicare.HelloApplication.class.getResource("medecin-rdv-list-view.fxml"));
            Node view = loader.load();
            MedecinRdvListController ctrl = loader.getController();
            ctrl.setContentArea(contentArea);
            ctrl.setMedecinId(medecinId);
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadRendezVous() {
        container.getChildren().clear();

        Label title = new Label("Rendez-vous de mes patients");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #0d9488;");
        container.getChildren().add(title);

        List<RendezVous> rdvs = service.getByMedecin(medecinId);

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

            FontIcon userIcon = new FontIcon(FontAwesomeSolid.USER);
            userIcon.setIconSize(26);
            userIcon.setIconColor(Color.web("#0d9488"));

            VBox infos = new VBox(3);
            HBox.setHgrow(infos, Priority.ALWAYS);

            // medecinNom/Prenom contiennent le nom du patient (réutilisé dans getByMedecin)
            Label patientLabel = new Label(rv.getMedecinPrenom() + " " + rv.getMedecinNom());
            patientLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #333;");

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

            infos.getChildren().addAll(patientLabel, dateLabel, statutLabel);

            HBox actions = new HBox(6);
            actions.setAlignment(Pos.CENTER);

            // Boutons uniquement si en_attente
            if (rv.getStatut().equals("en_attente")) {
                Button btnAccept = actionBtn(FontAwesomeSolid.CHECK, "#16a34a", "#dcfce7", "Accepter");
                btnAccept.setOnAction(e -> {
                    service.accept(rv.getId());
                    showPopup("Rendez-vous accepte", "Le rendez-vous a ete confirme.",
                              FontAwesomeSolid.CHECK_CIRCLE, "#16a34a");
                });

                Button btnRefuse = actionBtn(FontAwesomeSolid.TIMES, "#dc2626", "#fee2e2", "Refuser");
                btnRefuse.setOnAction(e -> {
                    service.refuse(rv.getId());
                    showPopup("Rendez-vous refuse", "Le rendez-vous a ete refuse.",
                              FontAwesomeSolid.TIMES_CIRCLE, "#dc2626");
                });

                Button btnReport = actionBtn(FontAwesomeSolid.CALENDAR_ALT, "#f59e0b", "#fef3c7", "Reporter");
                btnReport.setOnAction(e -> showReportDialog(rv));

                actions.getChildren().addAll(btnAccept, btnRefuse, btnReport);
            }

            Button btnVoir = actionBtn(FontAwesomeSolid.EYE, "#0d9488", "#ccfbf1", "Voir");
            btnVoir.setOnAction(e -> showDetails(rv));
            actions.getChildren().add(btnVoir);

            card.getChildren().addAll(userIcon, infos, actions);
            container.getChildren().add(card);
        }
    }

    private Button actionBtn(FontAwesomeSolid iconType, String iconColor, String bgColor, String tip) {
        Button btn = new Button();
        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(14);
        icon.setIconColor(Color.web(iconColor));
        btn.setGraphic(icon);
        btn.setStyle("-fx-background-color: " + bgColor + "; -fx-cursor: hand; -fx-padding: 6; -fx-background-radius: 8;");
        btn.setTooltip(new Tooltip(tip));
        return btn;
    }

    // ========== REPORT DIALOG ==========

    private void showReportDialog(RendezVous rv) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        FontIcon calIcon = new FontIcon(FontAwesomeSolid.CALENDAR_ALT);
        calIcon.setIconSize(40);
        calIcon.setIconColor(Color.web("#f59e0b"));

        Label titleLbl = new Label("Proposer un nouveau creneau");
        titleLbl.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #333;");

        DatePicker dp = new DatePicker();
        dp.setPromptText("Nouvelle date");
        dp.setPrefWidth(250);
        dp.setDayCellFactory(d -> new DateCell() {
            @Override public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(empty || item.isBefore(LocalDate.now()));
            }
        });

        ComboBox<String> heureCombo = new ComboBox<>();
        heureCombo.setPromptText("Nouvelle heure");
        heureCombo.setPrefWidth(250);
        for (int h = 8; h < 18; h++) {
            heureCombo.getItems().add(String.format("%02d:00", h));
            heureCombo.getItems().add(String.format("%02d:30", h));
        }

        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");

        Button btnConfirm = new Button("Proposer");
        btnConfirm.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-size: 13px; " +
                            "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 25;");

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #333; -fx-font-size: 13px; " +
                            "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 25;");

        HBox btns = new HBox(15, btnAnnuler, btnConfirm);
        btns.setAlignment(Pos.CENTER);

        VBox box = new VBox(12, calIcon, titleLbl, dp, heureCombo, errorLbl, btns);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(25));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 16; " +
                     "-fx-border-color: #e0e0e0; -fx-border-radius: 16; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 4);");

        popup.setScene(new Scene(box, 340, 340));
        popup.show();

        btnAnnuler.setOnAction(e -> popup.close());
        btnConfirm.setOnAction(e -> {
            if (dp.getValue() == null || heureCombo.getValue() == null) {
                errorLbl.setText("Remplissez la date et l'heure.");
                return;
            }
            popup.close();
            service.proposeReport(rv.getId(), dp.getValue(), LocalTime.parse(heureCombo.getValue()));
            showPopup("Report propose", "Le nouveau creneau a ete propose au patient.",
                      FontAwesomeSolid.CALENDAR_CHECK, "#f59e0b");
        });
    }

    // ========== DETAILS MODAL ==========

    private void showDetails(RendezVous rv) {
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter heureFmt = DateTimeFormatter.ofPattern("HH:mm");

        VBox modal = new VBox(12);
        modal.setAlignment(Pos.CENTER);
        modal.setPadding(new Insets(30));
        modal.setMaxWidth(420);
        modal.setMaxHeight(350);
        modal.setStyle("-fx-background-color: white; -fx-background-radius: 16; " +
                       "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 20, 0, 0, 5);");

        FontIcon icon = new FontIcon(FontAwesomeSolid.USER_CIRCLE);
        icon.setIconSize(40);
        icon.setIconColor(Color.web("#0d9488"));

        Label titleLabel = new Label("Details du rendez-vous");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0d9488;");

        Label details = new Label(
            "Patient :  " + rv.getMedecinPrenom() + " " + rv.getMedecinNom() + "\n" +
            "Date :  " + rv.getDate().format(dateFmt) + "\n" +
            "Heure :  " + rv.getHeure().format(heureFmt) + "\n" +
            "Statut :  " + rv.getStatut()
        );
        details.setStyle("-fx-font-size: 14px; -fx-text-fill: #444; -fx-line-spacing: 4;");

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #0d9488; -fx-text-fill: white; -fx-font-size: 13px; " +
                          "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 30;");
        closeBtn.setOnAction(e -> reloadFullList());

        modal.getChildren().addAll(icon, titleLabel, details, closeBtn);

        StackPane overlay = new StackPane(modal);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.4);");
        overlay.setOnMouseClicked(e -> { if (e.getTarget() == overlay) reloadFullList(); });

        contentArea.getChildren().clear();
        contentArea.getChildren().add(overlay);
    }

    // ========== POPUP ==========

    private void showPopup(String title, String message, FontAwesomeSolid iconType, String color) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(48);
        icon.setIconColor(Color.web(color));

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0d9488;");

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
}

