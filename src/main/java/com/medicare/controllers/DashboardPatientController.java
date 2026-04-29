package com.medicare.controllers;

import com.medicare.HelloApplication;
import com.medicare.models.User;
import com.medicare.services.NotificationService;
import com.medicare.services.RendezVousService;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

public class DashboardPatientController {

    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;
    @FXML private Label avatarInitialsLabel;
    @FXML private Label roleBadgeLabel;
    @FXML private VBox profileCard;
    @FXML private StackPane contentArea;

    @FXML private Button btnAccueil;
    @FXML private Button btnRendezVous;
    @FXML private Button btnDonation;
    @FXML private Button btnProduit;
    @FXML private Button btnCollaboration;
    @FXML private Button btnForum;
    @FXML private Button btnNotifications;
    @FXML private Button btnDevenirMedecin;
    @FXML private Button btnLogout;

    private static User currentUser;
    private final NotificationService notificationService = new NotificationService();
    private Timeline notificationBadgeTimeline;

    public static void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser() { return currentUser; }

    @FXML
    private void initialize() {
        if (currentUser != null) {
            configureProfile(currentUser);
        }

        // Icônes FontAwesome sur chaque bouton
        btnAccueil.setGraphic(icon(FontAwesomeSolid.HOME));
        btnRendezVous.setGraphic(icon(FontAwesomeSolid.CALENDAR_ALT));
        btnDonation.setGraphic(icon(FontAwesomeSolid.HEART));
        btnProduit.setGraphic(icon(FontAwesomeSolid.SHOPPING_CART));
        btnCollaboration.setGraphic(icon(FontAwesomeSolid.HANDSHAKE));
        btnForum.setGraphic(icon(FontAwesomeSolid.COMMENTS));
        btnNotifications.setGraphic(icon(FontAwesomeSolid.BELL));
        btnDevenirMedecin.setGraphic(icon(FontAwesomeSolid.USER_MD, Color.web("#ffd700")));
        btnLogout.setGraphic(icon(FontAwesomeSolid.SIGN_OUT_ALT, Color.web("#ffcccb")));
        refreshNotificationBadge();
        startNotificationBadgeAutoRefresh();

        highlightButton(btnAccueil);
    }

    private void configureProfile(User user) {
        userNameLabel.setText(fullName(user, ""));
        userEmailLabel.setText("Patient Member");
        avatarInitialsLabel.setText(initials(user));
        roleBadgeLabel.setText("Patient");
        roleBadgeLabel.getStyleClass().setAll("sidebar-role-badge", "sidebar-role-patient");
        playProfileIntro();
    }

    private String fullName(User user, String prefix) {
        String name = ((user.getPrenom() != null ? user.getPrenom() : "") + " " +
                (user.getNom() != null ? user.getNom() : "")).trim();
        if (name.isEmpty()) {
            name = "Utilisateur";
        }
        return prefix == null || prefix.isBlank() ? name : prefix + name;
    }

    private String initials(User user) {
        String first = firstInitial(user.getPrenom());
        String second = firstInitial(user.getNom());
        String value = (first + second).trim();
        return value.isEmpty() ? "U" : value.toUpperCase();
    }

    private String firstInitial(String value) {
        return value == null || value.isBlank() ? "" : value.trim().substring(0, 1);
    }

    private void playProfileIntro() {
        if (profileCard == null) {
            return;
        }
        profileCard.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(300), profileCard);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
        profileCard.setOnMouseEntered(event -> animateScale(profileCard, 1.02));
        profileCard.setOnMouseExited(event -> animateScale(profileCard, 1.0));
    }

    private void animateScale(Node node, double scale) {
        ScaleTransition transition = new ScaleTransition(Duration.millis(140), node);
        transition.setToX(scale);
        transition.setToY(scale);
        transition.play();
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

    // ========== NAVIGATION SIDEBAR ==========

    @FXML private void onAccueilClick() {
        highlightButton(btnAccueil);
        setContent(new Label("Bienvenue sur votre espace patient !") {{
            setStyle("-fx-font-size: 22px; -fx-text-fill: #333; -fx-font-weight: bold;");
        }});
    }

    @FXML private void onRendezVousClick() {
        highlightButton(btnRendezVous);
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("rendez-vous-list-view.fxml"));
            Node view = loader.load();
            RendezVousListController ctrl = loader.getController();
            ctrl.setContentArea(contentArea);
            // Trouver le patient_id à partir du user_id
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

    @FXML private void onDonationClick() {
        highlightButton(btnDonation);
        setContent(new Label("Faire un don") {{
            setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
        }});
    }

    @FXML private void onProduitClick() {
        highlightButton(btnProduit);
        setContent(new Label("Nos Produits") {{
            setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
        }});
    }

    @FXML private void onCollaborationClick() {
        highlightButton(btnCollaboration);
        setContent(new Label("Collaborer") {{
            setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
        }});
    }

    @FXML private void onForumClick() {
        highlightButton(btnForum);
        refreshNotificationBadge();
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("forum-list-view.fxml"));
            Node view = loader.load();
            ForumListController controller = loader.getController();
            controller.setForumContext(contentArea, currentUser);
            setContent(view);
        } catch (Exception e) {
            e.printStackTrace();
            setContent(new Label("Erreur chargement forum") {{
                setStyle("-fx-font-size: 16px; -fx-text-fill: #dc2626;");
            }});
        }
    }

    @FXML private void onNotificationsClick() {
        highlightButton(btnNotifications);
        refreshNotificationBadge();
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("notification-list-view.fxml"));
            Node view = loader.load();
            NotificationController controller = loader.getController();
            controller.setNotificationChangeListener(this::refreshNotificationBadge);
            controller.setNotificationContext(contentArea, currentUser);
            setContent(view);
            refreshNotificationBadge();
        } catch (Exception e) {
            e.printStackTrace();
            setContent(new Label("Erreur chargement notifications") {{
                setStyle("-fx-font-size: 16px; -fx-text-fill: #dc2626;");
            }});
        }
    }

    @FXML private void onDevenirMedecinClick() {
        highlightButton(btnDevenirMedecin);
        setContent(new Label("Devenir Medecin") {{
            setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
        }});
        // TODO : formulaire de demande pour devenir médecin
    }

    @FXML private void onLogoutClick() {
        stopNotificationBadgeAutoRefresh();
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

    // ========== UTILITAIRES ==========

    private void setContent(Node node) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(node);
    }

    private void refreshNotificationBadge() {
        if (btnNotifications == null || currentUser == null) {
            return;
        }
        try {
            int unread = notificationService.countUnreadByUserId(currentUser.getId());
            btnNotifications.setText(unread > 0 ? "  Notifications (" + unread + ")" : "  Notifications");
        } catch (Exception e) {
            btnNotifications.setText("  Notifications");
            System.err.println("Impossible de charger le compteur notifications patient: " + e.getMessage());
        }
    }

    private void startNotificationBadgeAutoRefresh() {
        stopNotificationBadgeAutoRefresh();
        notificationBadgeTimeline = new Timeline(
                new KeyFrame(Duration.seconds(5), event -> refreshNotificationBadge())
        );
        notificationBadgeTimeline.setCycleCount(Animation.INDEFINITE);
        notificationBadgeTimeline.play();
    }

    private void stopNotificationBadgeAutoRefresh() {
        if (notificationBadgeTimeline != null) {
            notificationBadgeTimeline.stop();
            notificationBadgeTimeline = null;
        }
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
        btnDevenirMedecin.setStyle(normalGoldStyle);

        active.setStyle(activeStyle);
    }
}
