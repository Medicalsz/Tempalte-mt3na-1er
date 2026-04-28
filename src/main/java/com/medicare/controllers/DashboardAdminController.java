package com.medicare.controllers;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import com.medicare.models.User;

import java.io.IOException;

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
    private Button selectedButton;

    public static void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser() { return currentUser; }

    @FXML
    private void initialize() {
        if (currentUser != null) {
            userNameLabel.setText(currentUser.getNom());
            userEmailLabel.setText(currentUser.getEmail());
        }

        setupButtonIcons();
        onAccueilClick();
    }

    private void setupButtonIcons() {
        btnAccueil.setGraphic(new FontIcon(FontAwesomeSolid.HOME));
        btnUtilisateurs.setGraphic(new FontIcon(FontAwesomeSolid.USERS));
        btnRendezVous.setGraphic(new FontIcon(FontAwesomeSolid.CALENDAR_ALT));
        btnDonation.setGraphic(new FontIcon(FontAwesomeSolid.HEART));
        btnProduit.setGraphic(new FontIcon(FontAwesomeSolid.SHOPPING_CART));
        btnCollaboration.setGraphic(new FontIcon(FontAwesomeSolid.HANDSHAKE));
        btnPartenaire.setGraphic(new FontIcon(FontAwesomeSolid.BUILDING));
        btnForum.setGraphic(new FontIcon(FontAwesomeSolid.COMMENTS));
        btnLogout.setGraphic(new FontIcon(FontAwesomeSolid.SIGN_OUT_ALT));
    }

    // ========== NAVIGATION ==========

    @FXML private void onAccueilClick() {
        highlightButton(btnAccueil);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/admin-home-view.fxml"));
            Node view = loader.load();
            setContent(view);
        } catch (IOException e) {
            e.printStackTrace();
            // Fallback in case of error
            setContent(new Label("Erreur de chargement du tableau de bord.") {{
                setStyle("-fx-font-size: 18px; -fx-text-fill: #ef4444;");
            }});
        }
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
        contentArea.getChildren().setAll(node);
    }

    private void highlightButton(Button button) {
        if (selectedButton != null) {
            selectedButton.getStyleClass().remove("sidebar-button-selected");
        }
        button.getStyleClass().add("sidebar-button-selected");
        selectedButton = button;
    }
}