package com.medicare.controllers;

import com.medicare.HelloApplication;
import com.medicare.models.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

public class DashboardAdminController {

    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;
    @FXML private StackPane contentArea;

    @FXML private Button btnAccueil;
    @FXML private Button btnUtilisateurs;
    @FXML private Button btnRendezVous;
    @FXML private Button btnEvaluations;
    @FXML private Button btnDonation;
    @FXML private Button btnProduit;
    @FXML private Button btnCollaboration;
    @FXML private Button btnForum;
    @FXML private Button btnLogout;
    @FXML private VBox   rdvSubMenu;

    private FontIcon chevronIcon;

    private static User currentUser;

    public static void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser() { return currentUser; }

    private Button[] allButtons() {
        return new Button[]{btnAccueil, btnUtilisateurs, btnRendezVous,
                            btnDonation, btnProduit, btnCollaboration, btnForum};
    }

    @FXML
    private void initialize() {
        if (currentUser != null) {
            userNameLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom());
            userEmailLabel.setText(currentUser.getEmail());
        }

        btnAccueil.setGraphic(icon(FontAwesomeSolid.HOME));
        btnUtilisateurs.setGraphic(icon(FontAwesomeSolid.USERS));

        // Bouton Rendez-vous : icône calendrier + label + chevron à droite
        chevronIcon = new FontIcon(FontAwesomeSolid.CHEVRON_DOWN);
        chevronIcon.setIconSize(11);
        chevronIcon.setIconColor(Color.WHITE);
        btnRendezVous.setGraphic(buildButtonGraphic(FontAwesomeSolid.CALENDAR_ALT, "Rendez-vous", chevronIcon));
        btnRendezVous.setText("");

        btnEvaluations.setGraphic(icon(FontAwesomeSolid.AWARD, Color.web("#fde68a")));
        btnDonation.setGraphic(icon(FontAwesomeSolid.HEART));
        btnProduit.setGraphic(icon(FontAwesomeSolid.SHOPPING_CART));
        btnCollaboration.setGraphic(icon(FontAwesomeSolid.HANDSHAKE));
        btnForum.setGraphic(icon(FontAwesomeSolid.COMMENTS));
        btnLogout.setGraphic(icon(FontAwesomeSolid.SIGN_OUT_ALT, Color.web("#fecaca")));

        highlightButton(btnAccueil);
    }

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
        if (chevronIcon != null) chevronIcon.setRotate(open ? 180 : 0);
    }

    // ========== NAVIGATION ==========

    @FXML private void onAccueilClick() {
        setSubMenuOpen(false);
        highlightButton(btnAccueil);
        setContent(new Label("Bienvenue sur le panneau d'administration !") {{
            setStyle("-fx-font-size: 22px; -fx-text-fill: #333; -fx-font-weight: bold;");
        }});
    }

    @FXML private void onUtilisateursClick() {
        setSubMenuOpen(false);
        highlightButton(btnUtilisateurs);
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("admin-users-view.fxml"));
            Node view = loader.load();
            setContent(view);
        } catch (Exception e) {
            e.printStackTrace();
            setContent(new Label("Gestion des utilisateurs (a venir)") {{
                setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
            }});
        }
    }

    @FXML private void onRendezVousClick() {
        setSubMenuOpen(!rdvSubMenu.isVisible());
        highlightButton(btnRendezVous);
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("admin-rdv-list-view.fxml"));
            Node view = loader.load();
            setContent(view);
        } catch (Exception e) {
            e.printStackTrace();
            setContent(new Label("Erreur chargement rendez-vous") {{
                setStyle("-fx-font-size: 16px; -fx-text-fill: #dc2626;");
            }});
        }
    }

    @FXML private void onEvaluationsClick() {
        if (!rdvSubMenu.isVisible()) setSubMenuOpen(true);
        highlightSubButton(btnEvaluations);
        try {
            AdminEvaluationsController ctrl = new AdminEvaluationsController();
            setContent(ctrl.buildView());
        } catch (Exception e) {
            e.printStackTrace();
            setContent(new Label("Erreur chargement évaluations") {{
                setStyle("-fx-font-size: 16px; -fx-text-fill: #dc2626;");
            }});
        }
    }

    @FXML private void onDonationClick() {
        setSubMenuOpen(false);
        highlightButton(btnDonation);
        setContent(new Label("Gestion des Donations") {{
            setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
        }});
    }

    @FXML private void onProduitClick() {
        setSubMenuOpen(false);
        highlightButton(btnProduit);
        setContent(new Label("Gestion des Produits") {{
            setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
        }});
    }

    @FXML private void onCollaborationClick() {
        setSubMenuOpen(false);
        highlightButton(btnCollaboration);
        setContent(new Label("Gestion des Collaborations") {{
            setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
        }});
    }

    @FXML private void onForumClick() {
        setSubMenuOpen(false);
        highlightButton(btnForum);
        setContent(new Label("Gestion du Forum") {{
            setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
        }});
    }

    @FXML private void onLogoutClick() {
        currentUser = null;
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("accueil-view.fxml"));
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Medicare");
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ========== UTILITAIRES ==========

    private void setContent(Node node) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(node);
    }

    private void highlightButton(Button active) {
        String normal  = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";
        String activeS = "-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";
        for (Button b : allButtons()) b.setStyle(normal);
        // reset sous-bouton
        btnEvaluations.setStyle("-fx-background-color: transparent; -fx-text-fill: #ddd6fe; -fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand;");
        active.setStyle(activeS);
    }

    private void highlightSubButton(Button active) {
        String normal = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";
        for (Button b : allButtons()) b.setStyle(normal);
        active.setStyle("-fx-background-color: rgba(255,255,255,0.18); -fx-text-fill: white; -fx-font-size: 13px; " +
                "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
    }
}

