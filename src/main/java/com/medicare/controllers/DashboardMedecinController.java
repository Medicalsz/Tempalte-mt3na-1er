package com.medicare.controllers;

import com.medicare.HelloApplication;
import com.medicare.models.User;
import com.medicare.ui.UserSectionFactory;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.nio.file.Path;

public class DashboardMedecinController {

    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;
    @FXML private Label userRoleLabel;
    @FXML private ImageView userAvatarView;
    @FXML private Button userProfileButton;
    @FXML private StackPane contentArea;

    @FXML private Button btnRendezVous;
    @FXML private Button btnPlanning;
    @FXML private Button btnSettings;
    @FXML private Button btnLogout;

    private static User currentUser;
    private static int medecinId;

    public static void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser() { return currentUser; }
    public static void setMedecinId(int id) { medecinId = id; }
    public static int getMedecinId() { return medecinId; }

    @FXML
    private void initialize() {
        initAvatar();
        refreshUserHeader();

        btnRendezVous.setGraphic(icon(FontAwesomeSolid.CALENDAR_ALT));
        btnPlanning.setGraphic(icon(FontAwesomeSolid.CLOCK));
        btnSettings.setGraphic(icon(FontAwesomeSolid.COG, Color.web("#ccfbf1")));
        btnLogout.setGraphic(icon(FontAwesomeSolid.SIGN_OUT_ALT, Color.web("#fecaca")));

        onRendezVousClick();
    }

    private void initAvatar() {
        userAvatarView.setClip(new Circle(28, 28, 28));
    }

    private FontIcon icon(FontAwesomeSolid type) { return icon(type, Color.WHITE); }

    private FontIcon icon(FontAwesomeSolid type, Color color) {
        FontIcon fi = new FontIcon(type);
        fi.setIconSize(16);
        fi.setIconColor(color);
        return fi;
    }

    @FXML
    private void onProfileClick() {
        highlightButton(btnSettings);
        openProfilePage();
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
    private void onSettingsClick() {
        highlightButton(btnSettings);
        openProfilePage();
    }

    @FXML
    private void onLogoutClick() {
        logoutToAccueil();
    }

    private void refreshUserHeader() {
        if (currentUser != null) {
            userNameLabel.setText("Dr. " + currentUser.getPrenom() + " " + currentUser.getNom());
            userEmailLabel.setText(currentUser.getEmail());
            userRoleLabel.setText("Medecin");
            updateAvatar(currentUser.getPhoto());
        }
    }

    private void updateAvatar(String photoPath) {
        try {
            if (photoPath == null || photoPath.isBlank()) {
                userAvatarView.setImage(new Image(HelloApplication.class.getResource("images/logo.png").toExternalForm(), true));
                return;
            }
            String source = photoPath.startsWith("file:/") ? photoPath : Path.of(photoPath).toUri().toString();
            userAvatarView.setImage(new Image(source, true));
        } catch (Exception e) {
            userAvatarView.setImage(new Image(HelloApplication.class.getResource("images/logo.png").toExternalForm(), true));
        }
    }

    private void openProfilePage() {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(UserSectionFactory.createProfileSection(
            currentUser,
            contentArea.getScene().getWindow(),
            user -> {
                currentUser = user;
                refreshUserHeader();
            },
            this::logoutToAccueil
        ));
    }

    private void logoutToAccueil() {
        currentUser = null;
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("accueil-view.fxml"));
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Medicare");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void highlightButton(Button active) {
        String normal = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";
        String activeS = "-fx-background-color: #14b8a6; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";
        btnRendezVous.setStyle(normal);
        btnPlanning.setStyle(normal);
        btnSettings.setStyle(normal);
        userProfileButton.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 16; -fx-cursor: hand; -fx-padding: 12;");
        active.setStyle(activeS);
    }
}
