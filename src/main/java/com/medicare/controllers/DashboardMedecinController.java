package com.medicare.controllers;

import com.medicare.HelloApplication;
import com.medicare.models.User;
import com.medicare.services.ProfileAlertService;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
    @FXML private Label profileBadge;

    @FXML private Button btnRendezVous;
    @FXML private Button btnPlanning;
    @FXML private Button btnEvaluations;
    @FXML private Button btnSettings;
    @FXML private Button btnLogout;
    @FXML private VBox   rdvSubMenu;

    private FontIcon chevronIcon;

    private static User currentUser;
    private static int medecinId;

    public static void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser() { return currentUser; }
    public static void setMedecinId(int id) { medecinId = id; }
    public static int getMedecinId() { return medecinId; }

    @FXML
    private void initialize() {
        if (currentUser == null) {
            currentUser = Session.getCurrentUser();
        }
        if (currentUser == null) return;

        initAvatar();
        refreshUserHeader();

        // Bouton Rendez-vous : icône calendrier à gauche + chevron à droite
        chevronIcon = new FontIcon(FontAwesomeSolid.CHEVRON_DOWN);
        chevronIcon.setIconSize(11);
        chevronIcon.setIconColor(Color.WHITE);
        btnRendezVous.setGraphic(buildButtonGraphic(FontAwesomeSolid.CALENDAR_ALT, "Rendez-vous", chevronIcon));
        btnRendezVous.setText("");

        btnPlanning.setGraphic(icon(FontAwesomeSolid.CLOCK));
        btnEvaluations.setGraphic(icon(FontAwesomeSolid.AWARD, Color.web("#fde68a")));
        btnSettings.setGraphic(icon(FontAwesomeSolid.COG, Color.web("#ccfbf1")));
        btnLogout.setGraphic(icon(FontAwesomeSolid.SIGN_OUT_ALT, Color.web("#fecaca")));

        int pct = ProfileAlertService.getCompletionPercent(currentUser);
        if (profileBadge != null) {
            profileBadge.setText("Profile " + pct + "%");
            if (pct >= 80) profileBadge.setVisible(false);
            profileBadge.setOnMouseClicked(e -> navigateToCompleteProfile());
        }

        highlightButton(btnRendezVous);
        loadRdvView();
        setSubMenuOpen(true);
    }

    /** Construit le contenu graphique d'un bouton parent : [icône] [label]  ←espace→  [chevron]. */
    private HBox buildButtonGraphic(FontAwesomeSolid mainIcon, String label, FontIcon trailing) {
        FontIcon main = new FontIcon(mainIcon);
        main.setIconSize(16);
        main.setIconColor(Color.WHITE);
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox h = new HBox(8, main, l, sp, trailing);
        h.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        h.setPrefWidth(180);
        return h;
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

    private void setSubMenuOpen(boolean open) {
        rdvSubMenu.setVisible(open);
        rdvSubMenu.setManaged(open);
        if (chevronIcon != null) {
            chevronIcon.setRotate(open ? 180 : 0);
        }
    }

    @FXML
    private void onProfileClick() {
        openProfilePage();
    }

    @FXML
    private void onRendezVousClick() {
        setSubMenuOpen(!rdvSubMenu.isVisible());
        highlightButton(btnRendezVous);
        loadRdvView();
    }

    private void loadRdvView() {
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
        setSubMenuOpen(false);
        highlightButton(btnPlanning);
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("planning-view.fxml"));
            Node view = loader.load();
            PlanningController ctrl = loader.getController();
            ctrl.setContentArea(contentArea);
            ctrl.setMedecinId(medecinId);
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onEvaluationsClick() {
        if (!rdvSubMenu.isVisible()) setSubMenuOpen(true);
        highlightSubButton(btnEvaluations);
        try {
            MedecinEvaluationsController ctrl = new MedecinEvaluationsController(medecinId);
            Node view = ctrl.buildView();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onSettingsClick() {
        setSubMenuOpen(false);
        highlightButton(btnSettings);
        openSettingsPage();
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
        setSubMenuOpen(false);
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
        userProfileButton.setStyle("-fx-background-color: rgba(255,255,255,0.22); -fx-background-radius: 16; -fx-cursor: hand; -fx-padding: 12;");
    }

    private void openSettingsPage() {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(UserSectionFactory.createSettingsSection(
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

    private void highlightButton(Button active) {
        String normal  = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";
        String activeS = "-fx-background-color: #14b8a6; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";
        resetSidebarButtons();
        // reset sous-bouton
        btnEvaluations.setStyle("-fx-background-color: transparent; -fx-text-fill: #ccfbf1; -fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand;");
        active.setStyle(activeS);
    }

    private void highlightSubButton(Button active) {
        String normal = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";
        resetSidebarButtons();
        active.setStyle("-fx-background-color: rgba(255,255,255,0.18); -fx-text-fill: white; -fx-font-size: 13px; " +
                "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
    }

    private void resetSidebarButtons() {
        String normal = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";
        btnRendezVous.setStyle(normal);
        btnPlanning.setStyle(normal);
        btnSettings.setStyle(normal);
        userProfileButton.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 16; -fx-cursor: hand; -fx-padding: 12;");
    }
}
