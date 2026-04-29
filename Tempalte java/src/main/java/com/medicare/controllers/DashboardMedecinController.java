package com.medicare.controllers;

import com.medicare.HelloApplication;
import com.medicare.models.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

public class DashboardMedecinController {

    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;
    @FXML private StackPane contentArea;

    @FXML private Button btnRendezVous;
    @FXML private Button btnPlanning;
    @FXML private Button btnLogout;

    private static User currentUser;
    private static int medecinId;

    public static void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser() { return currentUser; }
    public static void setMedecinId(int id) { medecinId = id; }
    public static int getMedecinId() { return medecinId; }

    @FXML
    private void initialize() {
        if (currentUser != null) {
            userNameLabel.setText("Dr. " + currentUser.getPrenom() + " " + currentUser.getNom());
            userEmailLabel.setText(currentUser.getEmail());
        }

        btnRendezVous.setGraphic(icon(FontAwesomeSolid.CALENDAR_ALT));
        btnPlanning.setGraphic(icon(FontAwesomeSolid.CLOCK));
        btnLogout.setGraphic(icon(FontAwesomeSolid.SIGN_OUT_ALT, Color.web("#fecaca")));

        highlightButton(btnRendezVous);
        onRendezVousClick();
    }

    private FontIcon icon(FontAwesomeSolid type) { return icon(type, Color.WHITE); }

    private FontIcon icon(FontAwesomeSolid type, Color color) {
        FontIcon fi = new FontIcon(type);
        fi.setIconSize(16);
        fi.setIconColor(color);
        return fi;
    }

    @FXML
    private void onRendezVousClick() {
        highlightButton(btnRendezVous);
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("medecin-rdv-list-view.fxml"));
            Node view = loader.load();
            MedecinRdvListController ctrl = loader.getController();
            ctrl.setContentArea(contentArea);
            ctrl.setMedecinId(medecinId);
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onPlanningClick() {
        highlightButton(btnPlanning);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(new Label("Planning (a venir)") {{
            setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
        }});
    }

    @FXML
    private void onLogoutClick() {
        currentUser = null;
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("accueil-view.fxml"));
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Medicare");
            stage.setMaximized(true);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void highlightButton(Button active) {
        String normal = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";
        String activeS = "-fx-background-color: #14b8a6; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";
        btnRendezVous.setStyle(normal);
        btnPlanning.setStyle(normal);
        active.setStyle(activeS);
    }
}

