package com.medicare.controllers;

import java.io.IOException;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import com.medicare.HelloApplication;
import com.medicare.models.User;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
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
    @FXML private Button btnStatistiques;
    @FXML private Button btnLogout;

    private static User currentUser;

    public static void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser() { return currentUser; }

    private Button[] allButtons() {
        return new Button[]{btnAccueil, btnUtilisateurs, btnRendezVous,
                            btnDonation, btnProduit, btnCollaboration, btnPartenaire, btnStatistiques};
    }

    @FXML
    private void initialize() {
        if (currentUser != null) {
            userNameLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom());
            userEmailLabel.setText(currentUser.getEmail());
        }

        btnAccueil.setGraphic(icon(FontAwesomeSolid.HOME));
        btnUtilisateurs.setGraphic(icon(FontAwesomeSolid.USERS));
        btnRendezVous.setGraphic(icon(FontAwesomeSolid.CALENDAR_ALT));
        btnDonation.setGraphic(icon(FontAwesomeSolid.HEART));
        btnProduit.setGraphic(icon(FontAwesomeSolid.SHOPPING_CART));
        btnCollaboration.setGraphic(icon(FontAwesomeSolid.HANDSHAKE));
        btnPartenaire.setGraphic(icon(FontAwesomeSolid.BUILDING));
        btnStatistiques.setGraphic(icon(FontAwesomeSolid.CHART_PIE));
        btnLogout.setGraphic(icon(FontAwesomeSolid.SIGN_OUT_ALT, Color.web("#fecaca")));

        // Highlight the home button by default, but don't load the content yet
        highlightButton(btnAccueil);
        
        // Load the home view by default after the scene is set
        Platform.runLater(this::onAccueilClick);
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
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("admin-home-view.fxml"));
            Node view = loader.load();

            // Get the controller and pass a reference to this dashboard
            AdminHomeController homeController = loader.getController();
            homeController.setDashboardController(this);

            setContent(view);
        } catch (IOException e) {
            e.printStackTrace();
            setContent(new Label("Erreur de chargement du tableau de bord.") {{
                setStyle("-fx-font-size: 18px; -fx-text-fill: #ef4444;");
            }});
        }
    }

    @FXML private void onUtilisateursClick() {
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
        navigateToCollaborationsWithFilter(null); // No filter
    }

    @FXML private void onPartenaireClick() {
        highlightButton(btnPartenaire);
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("admin-partners-view.fxml"));
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

    @FXML private void onStatistiquesClick() {
        highlightButton(btnStatistiques);
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("statistics-view.fxml"));
            Node view = loader.load();
            // Pass this controller to the statistics controller to enable navigation callbacks
            StatisticsController statisticsController = loader.getController();
            statisticsController.setDashboardController(this);
            setContent(view);
        } catch (Exception e) {
            e.printStackTrace();
            setContent(new Label("Erreur chargement statistiques") {{
                setStyle("-fx-font-size: 16px; -fx-text-fill: #dc2626;");
            }});
        }
    }

    /**
     * Navigates to the collaborations view and applies an optional status filter.
     * This method is called from the statistics controller.
     * @param status The status to filter by, or null for no filter.
     */
    public void navigateToCollaborationsWithFilter(String status) {
        highlightButton(btnCollaboration);
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("admin-collaborations-view.fxml"));
            Node view = loader.load();
            // Pass the filter to the collaborations controller before it initializes
            AdminCollaborationsController collaborationsController = loader.getController();
            collaborationsController.setInitialStatusFilter(status);
            setContent(view);
        } catch (IOException e) {
            e.printStackTrace();
            setContent(new Label("Erreur chargement collaborations") {{
                setStyle("-fx-font-size: 16px; -fx-text-fill: #dc2626;");
            }});
        }
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
        contentArea.getChildren().setAll(node);
    }

    private void highlightButton(Button active) {
        String normalStyle = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";
        String activeStyle = "-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";
        for (Button b : allButtons()) {
            b.setStyle(normalStyle);
        }
        active.setStyle(activeStyle);
    }
}