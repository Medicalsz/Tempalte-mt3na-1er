package com.medicare.controllers;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

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

public class DashboardAdminController {

    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;
    @FXML private StackPane contentArea;

    @FXML private Button btnAccueil;
    @FXML private Button btnUtilisateurs;
    @FXML private Button btnRendezVous;
    @FXML private Button btnDonation;
    @FXML private Button btnProduit;
    @FXML private Button btnCollaboration;
    @FXML private Button btnPartenaire;
    @FXML private Button btnForum;
    @FXML private Button btnLogout;

    private static User currentUser;

    public static void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser() { return currentUser; }

    private Button[] allButtons() {
        return new Button[]{btnAccueil, btnUtilisateurs, btnRendezVous,
                            btnDonation, btnProduit, btnCollaboration, btnPartenaire, btnForum};
    }

    @FXML
    private void initialize() {
        if (currentUser != null) {
            userNameLabel.setText(currentUser.getNom());
            userEmailLabel.setText(currentUser.getEmail());
        }

        btnAccueil.setGraphic(icon(FontAwesomeSolid.HOME));
        btnUtilisateurs.setGraphic(icon(FontAwesomeSolid.USERS));
        btnRendezVous.setGraphic(icon(FontAwesomeSolid.CALENDAR_ALT));
        btnDonation.setGraphic(icon(FontAwesomeSolid.HEART));
        btnProduit.setGraphic(icon(FontAwesomeSolid.SHOPPING_CART));
        btnCollaboration.setGraphic(icon(FontAwesomeSolid.HANDSHAKE));
        btnPartenaire.setGraphic(icon(FontAwesomeSolid.BUILDING));
        btnForum.setGraphic(icon(FontAwesomeSolid.COMMENTS));
        btnLogout.setGraphic(icon(FontAwesomeSolid.SIGN_OUT_ALT, Color.web("#fecaca")));

        highlightButton(btnAccueil);
    }

    private FontIcon icon(FontAwesomeSolid type) { return icon(type, Color.WHITE); }

    private FontIcon icon(FontAwesomeSolid type, Color color) {
        FontIcon fi = new FontIcon(type);
        fi.setIconSize(16);
        fi.setIconColor(color);
        return fi;
    }

    // ========== NAVIGATION ==========

    @FXML private void onAccueilClick() {
        highlightButton(btnAccueil);
        setContent(new Label("Bienvenue sur le panneau d'administration !") {{
            setStyle("-fx-font-size: 22px; -fx-text-fill: #333; -fx-font-weight: bold;");
        }});
    }

    @FXML private void onUtilisateursClick() {
        highlightButton(btnUtilisateurs);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/admin-users-view.fxml"));
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
        highlightButton(btnRendezVous);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/admin-rdv-list-view.fxml"));
            Node view = loader.load();
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
        setContent(new Label("Gestion des Donations") {{
            setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
        }});
    }

    @FXML private void onProduitClick() {
        highlightButton(btnProduit);
        setContent(new Label("Gestion des Produits") {{
            setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
        }});
    }

    @FXML private void onCollaborationClick() {
        highlightButton(btnCollaboration);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/admin-collaborations-view.fxml"));
            Node view = loader.load();
            setContent(view);
        } catch (Exception e) {
            e.printStackTrace();
            setContent(new Label("Erreur chargement collaborations") {{
                setStyle("-fx-font-size: 16px; -fx-text-fill: #dc2626;");
            }});
        }
    }

    @FXML private void onPartenaireClick() {
        highlightButton(btnPartenaire);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/admin-partners-view.fxml"));
            Node view = loader.load();
            setContent(view);
        } catch (Exception e) {
            e.printStackTrace();
            setContent(new Label("Erreur chargement partenaires") {{
                setStyle("-fx-font-size: 16px; -fx-text-fill: #dc2626;");
            }});
        }
    }

    @FXML private void onForumClick() {
        highlightButton(btnForum);
        setContent(new Label("Gestion du Forum") {{
            setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
        }});
    }

    @FXML private void onLogoutClick() {
        currentUser = null;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/accueil-view.fxml"));
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
        String normal = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";
        String activeS = "-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";
        for (Button b : allButtons()) b.setStyle(normal);
        active.setStyle(activeS);
    }
}