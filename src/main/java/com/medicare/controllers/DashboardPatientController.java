package com.medicare.controllers;

import com.medicare.HelloApplication;
import com.medicare.models.User;
import com.medicare.services.RendezVousService;
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

public class DashboardPatientController {

    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;
    @FXML private Label userRoleLabel;
    @FXML private ImageView userAvatarView;
    @FXML private Button userProfileButton;
    @FXML private StackPane contentArea;

    @FXML private Button btnAccueil;
    @FXML private Button btnRendezVous;
    @FXML private Button btnDonation;
    @FXML private Button btnProduit;
    @FXML private Button btnCollaboration;
    @FXML private Button btnForum;
    @FXML private Button btnNotifications;
    @FXML private Button btnDevenirMedecin;
    @FXML private Button btnSettings;
    @FXML private Button btnLogout;

    private static User currentUser;

    public static void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser() { return currentUser; }

    @FXML
    private void initialize() {
        initAvatar();
        refreshUserHeader();

        btnAccueil.setGraphic(icon(FontAwesomeSolid.HOME));
        btnRendezVous.setGraphic(icon(FontAwesomeSolid.CALENDAR_ALT));
        btnDonation.setGraphic(icon(FontAwesomeSolid.HEART));
        btnProduit.setGraphic(icon(FontAwesomeSolid.SHOPPING_CART));
        btnCollaboration.setGraphic(icon(FontAwesomeSolid.HANDSHAKE));
        btnForum.setGraphic(icon(FontAwesomeSolid.COMMENTS));
        btnNotifications.setGraphic(icon(FontAwesomeSolid.BELL, Color.web("#fef3c7")));
        btnDevenirMedecin.setGraphic(icon(FontAwesomeSolid.USER_MD, Color.web("#ffd700")));
        btnSettings.setGraphic(icon(FontAwesomeSolid.COG, Color.web("#dbeafe")));
        btnLogout.setGraphic(icon(FontAwesomeSolid.SIGN_OUT_ALT, Color.web("#ffcccb")));

        onAccueilClick();
    }

    private void initAvatar() {
        Circle clip = new Circle(28, 28, 28);
        userAvatarView.setClip(clip);
    }

    private FontIcon icon(FontAwesomeSolid type) {
        return icon(type, Color.WHITE);
    }

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
    private void onAccueilClick() {
        highlightButton(btnAccueil);
        setContent(UserSectionFactory.createWelcomeSection(
            "Bienvenue sur votre espace patient",
            "Consultez vos rendez-vous, mettez a jour votre compte et envoyez votre demande pour devenir medecin.",
            "#bfdbfe"
        ));
    }

    @FXML
    private void onRendezVousClick() {
        highlightButton(btnRendezVous);
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("rendez-vous-list-view.fxml"));
            Node view = loader.load();
            RendezVousListController ctrl = loader.getController();
            ctrl.setContentArea(contentArea);
            RendezVousService rvService = new RendezVousService();
            int patientId = rvService.getPatientIdByUserId(currentUser.getId());
            ctrl.setPatientId(patientId);
            setContent(view);
        } catch (Exception e) {
            e.printStackTrace();
            setContent(new Label("Erreur chargement rendez-vous") {{
                setStyle("-fx-font-size: 16px; -fx-text-fill: #dc2626;");
            }});
        }
    }

    @FXML
    private void onDonationClick() {
        highlightButton(btnDonation);
        setContent(new Label("Faire un don") {{
            setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
        }});
    }

    @FXML
    private void onProduitClick() {
        highlightButton(btnProduit);
        setContent(new Label("Nos Produits") {{
            setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
        }});
    }

    @FXML
    private void onCollaborationClick() {
        highlightButton(btnCollaboration);
        setContent(new Label("Collaborer") {{
            setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
        }});
    }

    @FXML
    private void onForumClick() {
        highlightButton(btnForum);
        setContent(new Label("Forum") {{
            setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
        }});
    }

    @FXML
    private void onNotificationsClick() {
        highlightButton(btnNotifications);
        setContent(UserSectionFactory.createNotificationsSection(currentUser));
    }

    @FXML
    private void onDevenirMedecinClick() {
        highlightButton(btnDevenirMedecin);
        setContent(UserSectionFactory.createDoctorRequestSection(currentUser, contentArea.getScene().getWindow()));
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
            userNameLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom());
            userEmailLabel.setText(currentUser.getEmail());
            userRoleLabel.setText("Patient");
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
        setContent(UserSectionFactory.createProfileSection(
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

    private void setContent(Node node) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(node);
    }

    private void highlightButton(Button active) {
        String normalStyle = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";
        String normalGoldStyle = "-fx-background-color: transparent; -fx-text-fill: #ffd700; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";
        String activeStyle = "-fx-background-color: #4a9af5; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";

        btnAccueil.setStyle(normalStyle);
        btnRendezVous.setStyle(normalStyle);
        btnDonation.setStyle(normalStyle);
        btnProduit.setStyle(normalStyle);
        btnCollaboration.setStyle(normalStyle);
        btnForum.setStyle(normalStyle);
        btnNotifications.setStyle(normalStyle);
        btnSettings.setStyle(normalStyle);
        btnDevenirMedecin.setStyle(normalGoldStyle);
        userProfileButton.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 16; -fx-cursor: hand; -fx-padding: 12;");

        active.setStyle(activeStyle);
    }
}
