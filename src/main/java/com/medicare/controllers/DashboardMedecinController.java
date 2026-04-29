package com.medicare.controllers;

import com.medicare.HelloApplication;
import com.medicare.models.User;
import com.medicare.services.NotificationService;
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

public class DashboardMedecinController {

    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;
    @FXML private Label avatarInitialsLabel;
    @FXML private Label roleBadgeLabel;
    @FXML private VBox profileCard;
    @FXML private StackPane contentArea;

    @FXML private Button btnRendezVous;
    @FXML private Button btnPlanning;
    @FXML private Button btnForum;
    @FXML private Button btnNotifications;
    @FXML private Button btnLogout;

    private static User currentUser;
    private static int medecinId;
    private final NotificationService notificationService = new NotificationService();
    private Timeline notificationBadgeTimeline;

    public static void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser() { return currentUser; }
    public static void setMedecinId(int id) { medecinId = id; }
    public static int getMedecinId() { return medecinId; }

    @FXML
    private void initialize() {
        if (currentUser != null) {
            configureProfile(currentUser);
        }

        btnRendezVous.setGraphic(icon(FontAwesomeSolid.CALENDAR_ALT));
        btnPlanning.setGraphic(icon(FontAwesomeSolid.CLOCK));
        btnForum.setGraphic(icon(FontAwesomeSolid.COMMENTS));
        btnNotifications.setGraphic(icon(FontAwesomeSolid.BELL));
        btnLogout.setGraphic(icon(FontAwesomeSolid.SIGN_OUT_ALT, Color.web("#fecaca")));
        refreshNotificationBadge();
        startNotificationBadgeAutoRefresh();

        highlightButton(btnRendezVous);
        onRendezVousClick();
    }

    private void configureProfile(User user) {
        userNameLabel.setText(fullName(user, "Dr. "));
        userEmailLabel.setText("Doctor");
        avatarInitialsLabel.setText(initials(user));
        roleBadgeLabel.setText("Doctor");
        roleBadgeLabel.getStyleClass().setAll("sidebar-role-badge", "sidebar-role-doctor");
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
        return value.isEmpty() ? "DR" : value.toUpperCase();
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
    private void onForumClick() {
        highlightButton(btnForum);
        refreshNotificationBadge();
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("forum-list-view.fxml"));
            Node view = loader.load();
            ForumListController controller = loader.getController();
            controller.setForumContext(contentArea, currentUser);
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onNotificationsClick() {
        highlightButton(btnNotifications);
        refreshNotificationBadge();
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("notification-list-view.fxml"));
            Node view = loader.load();
            NotificationController controller = loader.getController();
            controller.setNotificationChangeListener(this::refreshNotificationBadge);
            controller.setNotificationContext(contentArea, currentUser);
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
            refreshNotificationBadge();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onLogoutClick() {
        stopNotificationBadgeAutoRefresh();
        currentUser = null;
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("accueil-view.fxml"));
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Medicare");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void highlightButton(Button active) {
        String normal = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";
        String activeS = "-fx-background-color: #14b8a6; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";
        btnRendezVous.setStyle(normal);
        btnPlanning.setStyle(normal);
        btnForum.setStyle(normal);
        btnNotifications.setStyle(normal);
        active.setStyle(activeS);
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
            System.err.println("Impossible de charger le compteur notifications medecin: " + e.getMessage());
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
}
