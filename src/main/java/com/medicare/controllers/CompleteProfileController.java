package com.medicare.controllers;

import com.medicare.HelloApplication;
import com.medicare.models.User;
import com.medicare.services.NotificationService;
import com.medicare.services.NotificationService.Notification;
import com.medicare.services.ProfileAlertService;
import com.medicare.services.ProfileAlertService.AlertLevel;
import com.medicare.services.ProfileAlertService.SmartAlert;
import com.medicare.services.UserService;
import com.medicare.utils.Session;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CompleteProfileController {

    // --- Root layout refs ---
    @FXML private BorderPane rootPane;
    @FXML private VBox       tilesCenter;

    // --- Existing FXML fields ---
    @FXML private VBox       alertZone;
    @FXML private ProgressBar progressBar;
    @FXML private Label      pctLabel;
    @FXML private Button     goBtn;
    @FXML private Hyperlink  skipLink;
    @FXML private Button     tilePersonal, tileMedical, tileLocation,
                             tileContact,  tilePhoto,   tileNotif;

    // --- New sidebar FXML fields ---
    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private ImageView avatarImage;
    @FXML private Label avatarInitials;
    @FXML private Label roleBadge;
    @FXML private HBox  verifBox;
    @FXML private Label verifIcon;
    @FXML private Label verifLabel;
    @FXML private Label notifBadge;
    @FXML private Button notifBtn;

    private User  currentUser;
    private Stage primaryStage;
    private int   dismissCount = 0;
    private final UserService         userService  = new UserService();
    private final NotificationService notifService = new NotificationService();

    // Store original tile styles so markTile() can reset them on each refresh
    private final Map<Button, String> originalStyles = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        currentUser = Session.getCurrentUser();
        if (currentUser == null) {
            // Defer navigation until the node is actually in a scene
            alertZone.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    primaryStage = (Stage) newScene.getWindow();
                    navigateTo("login-view.fxml", "Medicare - Connexion");
                }
            });
            return;
        }
        // Cache the Stage as soon as the node enters a Scene/Window
        alertZone.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obs2, oldWin, newWin) -> {
                    if (newWin instanceof Stage s) primaryStage = s;
                });
                if (newScene.getWindow() instanceof Stage s) primaryStage = s;
            }
        });
        avatarImage.setClip(new Circle(46, 46, 46));
        populateSidebar();
        refreshProgress();
        refreshNotifBadge();
        scheduleAlert();

        // Notify user if profile is incomplete
        if (ProfileAlertService.getCompletionPercent(currentUser) < 50) {
            notifService.createProfileIncomplete(currentUser.getId());
            refreshNotifBadge();
        }
    }

    // =========================================================
    //  SIDEBAR
    // =========================================================
    private void populateSidebar() {
        String nom    = currentUser.getNom()    != null ? currentUser.getNom()    : "";
        String prenom = currentUser.getPrenom() != null ? currentUser.getPrenom() : "";
        String full   = (prenom + " " + nom).trim();

        nameLabel.setText(full.isEmpty() ? "Utilisateur" : full);
        emailLabel.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");

        // Initials
        String initials = "";
        if (!prenom.isEmpty()) initials += Character.toUpperCase(prenom.charAt(0));
        if (!nom.isEmpty())    initials += Character.toUpperCase(nom.charAt(0));
        avatarInitials.setText(initials.isEmpty() ? "?" : initials);
        updateSidebarAvatar(currentUser.getPhoto());

        // Role badge
        if (Session.isMedecin()) {
            roleBadge.setText("🩺  MÉDECIN");
            roleBadge.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32;" +
                               "-fx-background-radius: 20; -fx-padding: 5 16;" +
                               "-fx-font-size: 11; -fx-font-weight: bold;");
        } else if (Session.isAdmin()) {
            roleBadge.setText("🛡️  ADMIN");
            roleBadge.setStyle("-fx-background-color: #EDE7F6; -fx-text-fill: #512DA8;" +
                               "-fx-background-radius: 20; -fx-padding: 5 16;" +
                               "-fx-font-size: 11; -fx-font-weight: bold;");
        } else {
            roleBadge.setText("👤  PATIENT");
        }

        updateVerificationBadge();
    }

    private void updateSidebarAvatar(String photoPath) {
        try {
            if (photoPath == null || photoPath.isBlank()) {
                avatarImage.setVisible(false);
                avatarInitials.setVisible(true);
                return;
            }

            String source = photoPath.startsWith("file:/") ? photoPath : Path.of(photoPath).toUri().toString();
            Image image = new Image(source, false);
            avatarImage.setImage(image);
            applyCenteredSquareViewport(avatarImage, image);
            avatarImage.setVisible(true);
            avatarInitials.setVisible(false);
        } catch (Exception ignored) {
            avatarImage.setVisible(false);
            avatarInitials.setVisible(true);
        }
    }

    private void applyCenteredSquareViewport(ImageView view, Image image) {
        double width = image.getWidth();
        double height = image.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        double side = Math.min(width, height);
        view.setViewport(new javafx.geometry.Rectangle2D(
                (width - side) / 2,
                (height - side) / 2,
                side,
                side
        ));
    }

    private void updateVerificationBadge() {
        if (currentUser.isVerified()) {
            verifIcon.setText("✅");
            verifLabel.setText("Compte vérifié");
            verifLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");
            verifBox.setStyle("-fx-background-color: #E8F5E9; -fx-background-radius: 20; -fx-padding: 6 14;");
        } else if (Session.isMedecin()) {
            verifIcon.setText("⏳");
            verifLabel.setText("Vérification en attente");
            verifLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #E65100;");
            verifBox.setStyle("-fx-background-color: #FFF3E0; -fx-background-radius: 20; -fx-padding: 6 14;");
        } else {
            verifIcon.setText("✉️");
            verifLabel.setText("Email non vérifié");
            verifLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #9E9E9E;");
            verifBox.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 20; -fx-padding: 6 14;");
        }
    }

    // =========================================================
    //  PROGRESS
    // =========================================================
    private void refreshProgress() {
        int pct = ProfileAlertService.getCompletionPercent(currentUser);

        progressBar.setProgress(pct / 100.0);
        pctLabel.setText(pct + "%");

        String color = pct < 33 ? "#C2185B" : pct < 66 ? "#F57C00" : "#43A047";
        progressBar.setStyle("-fx-accent: " + color + "; -fx-pref-height: 10; -fx-background-radius: 5;");
        pctLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 30;");

        goBtn.setVisible(pct >= 50);

        markTile(tilePersonal, currentUser.getNom()       != null && !currentUser.getNom().isEmpty(),   "#C2185B");
        markTile(tileMedical,  currentUser.getBloodType() != null,                                       "#43A047");
        markTile(tileLocation, currentUser.getCity()      != null && !currentUser.getCity().isEmpty(),   "#F57C00");
        markTile(tileContact,  currentUser.getNumero()    != null && !currentUser.getNumero().isEmpty(), "#C2185B");
        markTile(tilePhoto,    currentUser.getPhoto()     != null && !currentUser.getPhoto().isEmpty(),  "#546E7A");
        markTile(tileNotif,    currentUser.isWantsEmailNotifications(),                                  "#F57C00");
    }

    private void markTile(Button tile, boolean done, String doneColor) {
        // Save original style once
        if (!originalStyles.containsKey(tile)) {
            originalStyles.put(tile, tile.getStyle());
        }
        String base = originalStyles.get(tile);

        if (done) {
            // Replace border color and thicken it
            String updated = base.replaceAll("-fx-border-color: #[A-Fa-f0-9]{6}", "-fx-border-color: " + doneColor)
                               + " -fx-border-width: 3;";
            tile.setStyle(updated);
            if (!tile.getText().startsWith("✓")) {
                tile.setText("✓ " + tile.getText());
            }
        } else {
            tile.setStyle(base);
            if (tile.getText().startsWith("✓ ")) {
                tile.setText(tile.getText().substring(2));
            }
        }
    }

    // =========================================================
    //  SMART ALERTS
    // =========================================================
    private void scheduleAlert() {
        PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
        delay.setOnFinished(e -> showAlert());
        delay.play();
    }

    private void showAlert() {
        SmartAlert alert = ProfileAlertService.getSmartAlert(currentUser);
        if (alert.level() == AlertLevel.NONE) return;

        alertZone.getChildren().clear();

        HBox banner = new HBox(10);
        banner.setStyle(
            "-fx-background-color: " + alert.color() + ";" +
            "-fx-border-radius: 12; -fx-background-radius: 12;" +
            "-fx-padding: 12; -fx-alignment: CENTER_LEFT;"
        );

        String icon = switch (alert.level()) {
            case CRITICAL -> "⛔";
            case HIGH     -> "⚠️";
            case MEDIUM   -> "💊";
            case LOW      -> "🔔";
            default       -> "ℹ️";
        };

        VBox body = new VBox(2);
        Label title = new Label(icon + "  " + alert.title());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");
        Label msg = new Label(alert.message());
        msg.setStyle("-fx-font-size: 12; -fx-text-fill: #555; -fx-wrap-text: true;");
        msg.setMaxWidth(380);
        body.getChildren().addAll(title, msg);
        HBox.setHgrow(body, Priority.ALWAYS);

        Button close = new Button("✕");
        close.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #888;");
        close.setOnAction(e -> dismissAndReschedule());

        banner.getChildren().addAll(body, close);

        TranslateTransition slide = new TranslateTransition(Duration.millis(250), banner);
        slide.setFromY(-20); slide.setToY(0);
        FadeTransition fade = new FadeTransition(Duration.millis(250), banner);
        fade.setFromValue(0); fade.setToValue(1);
        new ParallelTransition(slide, fade).play();

        if (alert.level() == AlertLevel.CRITICAL) {
            ScaleTransition pulse = new ScaleTransition(Duration.millis(400), banner);
            pulse.setByX(0.03); pulse.setByY(0.03);
            pulse.setCycleCount(6); pulse.setAutoReverse(true);
            PauseTransition wait = new PauseTransition(Duration.millis(300));
            wait.setOnFinished(e -> pulse.play());
            wait.play();
        }

        alertZone.getChildren().add(banner);
    }

    private void dismissAndReschedule() {
        alertZone.getChildren().clear();
        dismissCount++;
        int pct = ProfileAlertService.getCompletionPercent(currentUser);
        if (pct >= 50) return;
        int[] delays = {4, 4, 8, 12, 20};
        if (dismissCount <= delays.length) {
            PauseTransition retry = new PauseTransition(Duration.seconds(delays[dismissCount - 1]));
            retry.setOnFinished(e -> showAlert());
            retry.play();
        }
    }

    // =========================================================
    //  NOTIFICATIONS PANEL
    // =========================================================
    private void refreshNotifBadge() {
        int count = notifService.getUnreadCount(currentUser.getId());
        if (count > 0) {
            notifBadge.setText(count > 9 ? "9+" : String.valueOf(count));
            notifBadge.setVisible(true);
        } else {
            notifBadge.setVisible(false);
        }
    }

    @FXML
    private void openNotifications() {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(alertZone.getScene().getWindow());

        VBox root = new VBox(0);
        root.setPrefWidth(370);
        root.setStyle("-fx-background-color: white; -fx-border-color: #E0E0E0;" +
                      "-fx-border-width: 1; -fx-background-radius: 14; -fx-border-radius: 14;" +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 18, 0, 0, 5);");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #C2185B; -fx-padding: 14 18;" +
                        "-fx-background-radius: 14 14 0 0;");
        Label title = new Label("🔔  Notifications");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button markAllBtn = new Button("Tout marquer lu");
        markAllBtn.setStyle("-fx-background-color: rgba(255,255,255,0.20); -fx-text-fill: white;" +
                            "-fx-font-size: 10; -fx-background-radius: 8; -fx-padding: 4 10; -fx-cursor: hand;");
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white;" +
                          "-fx-font-size: 14; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> popup.close());
        header.getChildren().addAll(title, spacer, markAllBtn, closeBtn);

        // Notification rows
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: white; -fx-background-color: white; -fx-border-color: transparent;");
        VBox list = new VBox(0);
        list.setStyle("-fx-background-color: white;");

        List<Notification> notifs = notifService.getAll(currentUser.getId());
        if (notifs.isEmpty()) {
            Label empty = new Label("Aucune notification pour le moment");
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setAlignment(Pos.CENTER);
            empty.setStyle("-fx-text-fill: #9E9E9E; -fx-padding: 30 16; -fx-font-size: 13;");
            list.getChildren().add(empty);
        } else {
            for (Notification n : notifs) {
                VBox row = buildNotifRow(n);
                list.getChildren().add(row);
                Separator sep = new Separator();
                sep.setStyle("-fx-background-color: #F5F5F5;");
                list.getChildren().add(sep);
            }
        }

        scroll.setContent(list);
        scroll.setPrefHeight(Math.min(notifs.size() * 82 + 10, 390));

        markAllBtn.setOnAction(e -> {
            notifService.markAllRead(currentUser.getId());
            popup.close();
            refreshNotifBadge();
        });

        root.getChildren().addAll(header, scroll);

        popup.setScene(new Scene(root));
        popup.show();

        // Auto-mark as read
        notifService.markAllRead(currentUser.getId());
        refreshNotifBadge();
    }

    private VBox buildNotifRow(Notification n) {
        VBox row = new VBox(4);
        String bg = n.isRead() ? "white" : "#FFF8E1";
        row.setStyle("-fx-background-color: " + bg + "; -fx-padding: 12 16;");

        String icon = switch (n.type()) {
            case "appointment" -> "📅";
            case "medication"  -> "💊";
            case "reminder"    -> "⏰";
            default            -> "ℹ️";
        };

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 14;");
        Label titleLbl = new Label(n.titre());
        titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12; -fx-text-fill: #212121;");
        titleRow.getChildren().addAll(iconLbl, titleLbl);

        if (!n.isRead()) {
            Region rowSpacer = new Region();
            HBox.setHgrow(rowSpacer, Priority.ALWAYS);
            Circle dot = new Circle(5, Color.web("#C2185B"));
            titleRow.getChildren().addAll(rowSpacer, dot);
        }

        Label msgLbl = new Label(n.message());
        msgLbl.setStyle("-fx-font-size: 11; -fx-text-fill: #757575;");
        msgLbl.setMaxWidth(330);
        msgLbl.setWrapText(true);

        Label timeLbl = new Label("🕐  " + formatDate(n.createdAt()));
        timeLbl.setStyle("-fx-font-size: 10; -fx-text-fill: #BDBDBD; -fx-padding: 2 0 0 0;");

        row.getChildren().addAll(titleRow, msgLbl, timeLbl);
        return row;
    }

    private String formatDate(String raw) {
        if (raw == null || raw.length() < 16) return raw != null ? raw : "";
        return raw.substring(0, 16);
    }

    // =========================================================
    //  SKIP
    // =========================================================
    @FXML
    private void skipProfile() {
        userService.incrementSkipCount(currentUser.getId());
        alertZone.getChildren().clear();

        Label note = new Label("⏰  Rappel activé — vous verrez cette page à la prochaine connexion jusqu'à 50%.");
        note.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #E65100;" +
                      "-fx-padding: 10 14; -fx-background-radius: 10; -fx-font-size: 12;" +
                      "-fx-wrap-text: true;");
        alertZone.getChildren().add(note);

        PauseTransition skipDelay = new PauseTransition(Duration.seconds(2));
        skipDelay.setOnFinished(e -> goToDashboard());
        skipDelay.play();
    }

    // =========================================================
    //  DASHBOARD NAVIGATION
    // =========================================================
    @FXML
    private void goDashboard() {
        int pct = ProfileAlertService.getCompletionPercent(currentUser);
        if (pct >= 50) {
            currentUser.setProfileCompleted(true);
            userService.updateProfileCompleted(currentUser.getId(), true);
            goToDashboard();
        }
    }

    private void goToDashboard() {
        if (Session.isAdmin()) {
            DashboardAdminController.setCurrentUser(currentUser);
            navigateTo("dashboard-admin-view.fxml", "Medicare - Administration");
        } else if (Session.isMedecin()) {
            DashboardMedecinController.setCurrentUser(currentUser);
            DashboardMedecinController.setMedecinId(Session.getMedecinId());
            navigateTo("dashboard-medecin-view.fxml", "Medicare - Espace Medecin");
        } else {
            DashboardPatientController.setCurrentUser(currentUser);
            navigateTo("dashboard-patient-view.fxml", "Medicare - Dashboard");
        }
    }

    // =========================================================
    //  TILE NAVIGATION  (inline — replaces center content)
    // =========================================================
    @FXML private void openPersonal() {
        showSection("👤 Infos personnelles",
                    com.medicare.ui.UserSectionFactory.createPersonalSection(currentUser, this::syncAndRefresh));
    }
    @FXML private void openMedical() {
        showSection("🩺 Infos médicales",
                    com.medicare.ui.UserSectionFactory.createMedicalSection(currentUser, this::syncAndRefresh));
    }
    @FXML private void openLocation() {
        showSection("📍 Localisation",
                    com.medicare.ui.UserSectionFactory.createLocationSection(currentUser, this::syncAndRefresh));
    }
    @FXML private void openContact() {
        showSection("📞 Contact",
                    com.medicare.ui.UserSectionFactory.createContactSection(currentUser, this::syncAndRefresh));
    }
    @FXML private void openPhoto() {
        showSection("📷 Photo de profil",
                    com.medicare.ui.UserSectionFactory.createPhotoSection(currentUser,
                        primaryStage != null ? primaryStage : alertZone.getScene().getWindow(),
                        this::syncAndRefresh));
    }
    @FXML private void openNotif() {
        showSection("🔔 Notifications",
                    com.medicare.ui.UserSectionFactory.createNotifSection(currentUser, this::syncAndRefresh));
    }

    private void showSection(String title, javafx.scene.Node content) {
        HBox backBar = new HBox(10);
        backBar.setAlignment(Pos.CENTER_LEFT);
        backBar.setStyle("-fx-background-color: white; -fx-padding: 12 24;" +
                         "-fx-border-color: #EEEEEE; -fx-border-width: 0 0 1 0;");
        Button backBtn = new Button("← Retour");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #C2185B;" +
                         "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13;" +
                         "-fx-border-color: transparent;");
        backBtn.setOnAction(e -> showTiles());
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #212121;");
        javafx.scene.control.Separator divider = new javafx.scene.control.Separator(
            javafx.geometry.Orientation.VERTICAL);
        divider.setPrefHeight(20);
        backBar.getChildren().addAll(backBtn, divider, titleLbl);

        VBox sectionView = new VBox(0);
        sectionView.setStyle("-fx-background-color: #F5F7FA;");
        sectionView.getChildren().addAll(backBar, content);
        VBox.setVgrow(content, Priority.ALWAYS);

        rootPane.setCenter(sectionView);
    }

    private void showTiles() {
        rootPane.setCenter(tilesCenter);
        alertZone.getChildren().clear();
        scheduleAlert();
    }

    private void syncAndRefresh(User updated) {
        currentUser = updated;
        Session.setCurrentUser(currentUser);
        populateSidebar();
        refreshProgress();
        refreshNotifBadge();
        // Brief pause so the section can display its success message before navigating back
        PauseTransition delay = new PauseTransition(Duration.millis(900));
        delay.setOnFinished(e -> showTiles());
        delay.play();
    }

    // =========================================================
    //  NAVIGATION HELPER
    // =========================================================
    private void navigateTo(String fxml, String title) {
        try {
            // Use cached stage; fall back to live lookup if available
            if (primaryStage == null && alertZone.getScene() != null) {
                primaryStage = (Stage) alertZone.getScene().getWindow();
            }
            if (primaryStage == null) return;
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxml));
            primaryStage.setScene(new Scene(loader.load()));
            primaryStage.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
