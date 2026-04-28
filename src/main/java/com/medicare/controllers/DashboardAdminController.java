package com.medicare.controllers;

<<<<<<< HEAD
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import com.medicare.models.User;

import java.io.IOException;

=======
import com.medicare.HelloApplication;
import com.medicare.models.User;
>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
<<<<<<< HEAD
=======
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f

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
<<<<<<< HEAD
    @FXML private Button btnPartenaire;
=======
>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f
    @FXML private Button btnForum;
    @FXML private Button btnLogout;

    private static User currentUser;
<<<<<<< HEAD
    private Button selectedButton;
=======
>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f

    public static void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser() { return currentUser; }

<<<<<<< HEAD
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
=======
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
        btnRendezVous.setGraphic(icon(FontAwesomeSolid.CALENDAR_ALT));
        btnDonation.setGraphic(icon(FontAwesomeSolid.HEART));
        btnProduit.setGraphic(icon(FontAwesomeSolid.SHOPPING_CART));
        btnCollaboration.setGraphic(icon(FontAwesomeSolid.HANDSHAKE));
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
>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f
    }

    // ========== NAVIGATION ==========

    @FXML private void onAccueilClick() {
        highlightButton(btnAccueil);
<<<<<<< HEAD
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
=======
        setContent(new Label("Bienvenue sur le panneau d'administration !") {{
            setStyle("-fx-font-size: 22px; -fx-text-fill: #333; -fx-font-weight: bold;");
        }});
>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f
    }

    @FXML private void onUtilisateursClick() {
        highlightButton(btnUtilisateurs);
        try {
<<<<<<< HEAD
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/admin-users-view.fxml"));
=======
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("admin-users-view.fxml"));
>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f
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
<<<<<<< HEAD
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/admin-rdv-list-view.fxml"));
=======
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("admin-rdv-list-view.fxml"));
>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f
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
<<<<<<< HEAD
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
=======
        setContent(new Label("Gestion des Collaborations") {{
            setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
        }});
>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f
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
<<<<<<< HEAD
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/accueil-view.fxml"));
=======
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("accueil-view.fxml"));
>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Medicare");
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ========== UTILITAIRES ==========

    private void setContent(Node node) {
<<<<<<< HEAD
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
=======
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

>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f
