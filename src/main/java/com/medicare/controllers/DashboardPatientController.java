package com.medicare.controllers;

import com.medicare.HelloApplication;
import com.medicare.controllers.ForumListController;
import com.medicare.models.User;
import com.medicare.services.RendezVousService;
import com.medicare.ui.UserSectionFactory;
import com.medicare.utils.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import com.medicare.services.ProfileAlertService;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
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
    @FXML private VBox alertZone;
    @FXML private Label profileBadge;

    @FXML private Button btnAccueil;
    @FXML private Button btnRendezVous;
    @FXML private Button btnDonation;
    @FXML private Button btnProduit;
    @FXML private Button btnCollaboration;
    @FXML private Button btnPartenaire;
    @FXML private Button btnForum;
    @FXML private Button btnMatchDoctors;
    @FXML private Button btnNotifications;
    @FXML private Circle notifRedDot;
    @FXML private Button btnDevenirMedecin;
    @FXML private Button btnBlockList;
    @FXML private Button btnSettings;
    @FXML private Button btnLogout;
    private boolean notifSeen = false;
    private int dismissCount = 0;
    private final com.medicare.services.DonationService donationService = new com.medicare.services.DonationService();

    private static User currentUser;

    public static void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser() { return currentUser; }

    @FXML
    private void initialize() {
        if (currentUser == null) {
            currentUser = Session.getCurrentUser();
        }
        if (currentUser == null) {
            // Safety: if still null, return early or handle
            return;
        }
        initAvatar();
        refreshUserHeader();
        javafx.application.Platform.runLater(this::checkConfirmedDonsWithoutAddress);

        btnAccueil.setGraphic(icon(FontAwesomeSolid.HOME));
        btnRendezVous.setGraphic(icon(FontAwesomeSolid.CALENDAR_ALT));
        btnDonation.setGraphic(icon(FontAwesomeSolid.HEART));
        btnProduit.setGraphic(icon(FontAwesomeSolid.SHOPPING_CART));
        btnCollaboration.setGraphic(icon(FontAwesomeSolid.HANDSHAKE));
        btnPartenaire.setGraphic(icon(FontAwesomeSolid.BUILDING));
        btnForum.setGraphic(icon(FontAwesomeSolid.COMMENTS));
        btnMatchDoctors.setGraphic(icon(FontAwesomeSolid.MAP_MARKER_ALT));
        btnNotifications.setGraphic(icon(FontAwesomeSolid.BELL, Color.web("#fef3c7")));
        btnDevenirMedecin.setGraphic(icon(FontAwesomeSolid.USER_MD, Color.web("#ffd700")));
        btnBlockList.setGraphic(icon(FontAwesomeSolid.BAN, Color.web("#fca5a5")));
        btnSettings.setGraphic(icon(FontAwesomeSolid.COG, Color.web("#dbeafe")));
        btnLogout.setGraphic(icon(FontAwesomeSolid.SIGN_OUT_ALT, Color.web("#ffcccb")));

        // Show red dot if there is an unread notification (account not yet verified)
        updateNotifDot();

        onAccueilClick();

        int pct = ProfileAlertService.getCompletionPercent(currentUser);
        if (profileBadge != null) {
            profileBadge.setText("Profile " + pct + "%");
            if (pct >= 80) profileBadge.setVisible(false);
            profileBadge.setOnMouseClicked(e -> navigateToCompleteProfile());
        }

        // Show alert after 1.5s delay
        PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
        delay.setOnFinished(e -> injectAlert());
        delay.play();
    }

    private void updateNotifDot() {
        boolean hasUnread = currentUser != null && !currentUser.isVerified() && !notifSeen;
        if (notifRedDot != null) {
            notifRedDot.setVisible(hasUnread);
            if (hasUnread) {
                // Pulsing animation on the red dot
                Timeline pulse = new Timeline(
                    new KeyFrame(Duration.ZERO,   new KeyValue(notifRedDot.scaleXProperty(), 1.0),
                                                  new KeyValue(notifRedDot.scaleYProperty(), 1.0)),
                    new KeyFrame(Duration.millis(700), new KeyValue(notifRedDot.scaleXProperty(), 1.4),
                                                       new KeyValue(notifRedDot.scaleYProperty(), 1.4)),
                    new KeyFrame(Duration.millis(1400), new KeyValue(notifRedDot.scaleXProperty(), 1.0),
                                                        new KeyValue(notifRedDot.scaleYProperty(), 1.0))
                );
                pulse.setCycleCount(Timeline.INDEFINITE);
                pulse.play();
            }
        }
    }

    private void injectAlert() {
        if (alertZone == null || currentUser == null) return;
        ProfileAlertService.SmartAlert alert = ProfileAlertService.getSmartAlert(currentUser);
        if (alert.level() == ProfileAlertService.AlertLevel.NONE) return;

        alertZone.getChildren().clear();

        HBox banner = new HBox(12);
        String cssClass = switch (alert.level()) {
            case CRITICAL -> "alert-critical";
            case HIGH     -> "alert-high";
            case MEDIUM   -> "alert-medium";
            default       -> "alert-low";
        };
        banner.getStyleClass().addAll("alert-banner", cssClass);
        banner.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox body = new VBox(3);
        Label title = new Label(alert.title());
        String titleCss = switch (alert.level()) {
            case CRITICAL -> "alert-title-critical";
            case HIGH     -> "alert-title-high";
            default       -> "alert-title-medium";
        };
        title.getStyleClass().add(titleCss);

        Label msg = new Label(alert.message());
        msg.getStyleClass().add("alert-msg");
        msg.setMaxWidth(600);
        body.getChildren().addAll(title, msg);
        HBox.setHgrow(body, Priority.ALWAYS);

        Button complete = new Button("Complete now →");
        complete.getStyleClass().add("btn-pink");
        complete.setStyle("-fx-font-size: 12; -fx-padding: 6 14;");
        complete.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("complete-profile-view.fxml"));
                Stage stage = (Stage) contentArea.getScene().getWindow();
                stage.setScene(new Scene(loader.load()));
                stage.setTitle("Medicare - Complete Profile");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Button close = new Button("✕");
        close.getStyleClass().add("btn-outline");
        close.setStyle("-fx-font-size: 12; -fx-padding: 5 10;");
        close.setOnAction(e -> dismissAndReschedule());

        banner.getChildren().addAll(body, complete, close);

        // Slide-in animation
        FadeTransition ft = new FadeTransition(Duration.millis(300), banner);
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), banner);
        tt.setFromY(-15); tt.setToY(0);
        new ParallelTransition(ft, tt).play();

        alertZone.getChildren().add(banner);
    }

    private void dismissAndReschedule() {
        if (alertZone == null) return;
        alertZone.getChildren().clear();
        dismissCount++;
        int pct = ProfileAlertService.getCompletionPercent(currentUser);
        if (pct >= 50 || dismissCount > 5) return;

        int[] waitSecs = {5, 5, 10, 15, 25};
        int secs = waitSecs[Math.min(dismissCount - 1, waitSecs.length - 1)];
        PauseTransition retry = new PauseTransition(Duration.seconds(secs));
        retry.setOnFinished(e -> injectAlert());
        retry.play();
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
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("user-donation-view.fxml"));
            Node view = loader.load();
            setContent(view);
        } catch (Exception e) {
            e.printStackTrace();
            setContent(new Label("Erreur chargement donations") {{
                setStyle("-fx-font-size: 16px; -fx-text-fill: #dc2626;");
            }});
        }
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
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("user-collaborations-view.fxml"));
            Node view = loader.load();
            UserPartnershipsController ctrl = loader.getController();
            ctrl.setDashboardStackPane(contentArea);
            setContent(view);
        } catch (Exception e) {
            e.printStackTrace();
            setContent(new Label("Erreur chargement collaborations") {{
                setStyle("-fx-font-size: 16px; -fx-text-fill: #dc2626;");
            }});
        }
    }

    @FXML
    private void onPartenaireClick() {
        highlightButton(btnPartenaire);
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("user-partners-view.fxml"));
            Node view = loader.load();
            UserPartnersController ctrl = loader.getController();
            ctrl.loadPartners(contentArea);
            setContent(view);
        } catch (Exception e) {
            e.printStackTrace();
            setContent(new Label("Erreur chargement partenaires") {{
                setStyle("-fx-font-size: 16px; -fx-text-fill: #dc2626;");
            }});
        }
    }

    @FXML
    private void onForumClick() {
        highlightButton(btnForum);
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

    @FXML
    private void onMatchDoctorsClick() {
        highlightButton(btnMatchDoctors);
        setContent(UserSectionFactory.createMatchDoctorsSection(currentUser));
    }

    @FXML
    private void onNotificationsClick() {
        highlightButton(btnNotifications);
        notifSeen = true;
        if (notifRedDot != null) notifRedDot.setVisible(false);
        setContent(UserSectionFactory.createNotificationsSection(currentUser));
    }

    @FXML
    private void onDevenirMedecinClick() {
        highlightButton(btnDevenirMedecin);
        setContent(UserSectionFactory.createDoctorRequestSection(currentUser, contentArea.getScene().getWindow()));
    }

    @FXML
    private void onBlockListClick() {
        highlightButton(btnBlockList);
        setContent(UserSectionFactory.createBlockListSection(currentUser));
    }

    @FXML
    private void onSettingsClick() {
        highlightButton(btnSettings);
        openSettingsPage();
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
                userAvatarView.setViewport(null);
                userAvatarView.setImage(new Image(HelloApplication.class.getResource("images/logo.png").toExternalForm(), true));
                return;
            }
            String source = photoPath.startsWith("file:/") ? photoPath : Path.of(photoPath).toUri().toString();
            Image image = new Image(source, false);
            userAvatarView.setImage(image);
            applyCenteredSquareViewport(userAvatarView, image);
        } catch (Exception e) {
            userAvatarView.setViewport(null);
            userAvatarView.setImage(new Image(HelloApplication.class.getResource("images/logo.png").toExternalForm(), true));
        }
    }

    private void applyCenteredSquareViewport(ImageView view, Image image) {
        double width = image.getWidth();
        double height = image.getHeight();
        if (width <= 0 || height <= 0) return;
        double side = Math.min(width, height);
        view.setViewport(new javafx.geometry.Rectangle2D((width - side) / 2, (height - side) / 2, side, side));
    }

    private void openProfilePage() {
        resetSidebarButtons();
        java.util.Map<String, Runnable> quickNav = new java.util.LinkedHashMap<>();
        quickNav.put("posts",      this::onForumClick);
        quickNav.put("rendezvous", this::onRendezVousClick);
        quickNav.put("collab",     this::onCollaborationClick);
        quickNav.put("donation",   this::onDonationClick);
        setContent(UserSectionFactory.createProfileSection(
            currentUser,
            contentArea.getScene().getWindow(),
            user -> {
                currentUser = user;
                refreshUserHeader();
            },
            this::logoutToAccueil,
            quickNav
        ));
        userProfileButton.setStyle("-fx-background-color: rgba(255,255,255,0.22); -fx-background-radius: 16; -fx-cursor: hand; -fx-padding: 12;");
    }

    private void openSettingsPage() {
        setContent(UserSectionFactory.createSettingsSection(
            currentUser,
            contentArea.getScene().getWindow(),
            user -> {
                currentUser = user;
                refreshUserHeader();
            },
            this::logoutToAccueil,
            this::navigateToCompleteProfile
        ));
    }

    private void navigateToCompleteProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("complete-profile-view.fxml"));
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Medicare - Complete Profile");
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private void checkConfirmedDonsWithoutAddress() {
        if (currentUser == null) return;

        java.util.List<com.medicare.models.Don> myDons = donationService.getDonsByUserId(currentUser.getId());
        for (com.medicare.models.Don don : myDons) {
            if ("materiel".equals(don.getType())
                    && "confirme".equalsIgnoreCase(don.getStatut())
                    && (don.getAdresse() == null || don.getAdresse().trim().isEmpty()
                    || don.getAdresse().equalsIgnoreCase("pas d'adresse"))) {
                showAddressPopup(don);
                break;
            }
        }
    }

    private void showAddressPopup(com.medicare.models.Don don) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("donation-address-popup.fxml"));
            javafx.scene.Parent root = loader.load();

            DonationAddressPopupController controller = loader.getController();
            controller.setDon(don);

            Stage stage = new Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            stage.setScene(new Scene(root));
            stage.getScene().setFill(Color.TRANSPARENT);

            if (userNameLabel.getScene() != null && userNameLabel.getScene().getWindow() != null) {
                stage.initOwner(userNameLabel.getScene().getWindow());
            }

            stage.show();
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

        resetSidebarButtons();
        btnDevenirMedecin.setStyle(normalGoldStyle);
        active.setStyle(activeStyle);
    }

    private void resetSidebarButtons() {
        String normalStyle = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";
        btnAccueil.setStyle(normalStyle);
        btnRendezVous.setStyle(normalStyle);
        btnDonation.setStyle(normalStyle);
        btnProduit.setStyle(normalStyle);
        btnCollaboration.setStyle(normalStyle);
        btnPartenaire.setStyle(normalStyle);
        btnForum.setStyle(normalStyle);
        btnMatchDoctors.setStyle(normalStyle);
        btnNotifications.setStyle(normalStyle);
        btnBlockList.setStyle("-fx-background-color: transparent; -fx-text-fill: #fca5a5; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;");
        btnSettings.setStyle(normalStyle);
        userProfileButton.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 16; -fx-cursor: hand; -fx-padding: 12;");
    }
}
