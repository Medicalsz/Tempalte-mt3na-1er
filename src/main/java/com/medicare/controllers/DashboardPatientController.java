package com.medicare.controllers;

<<<<<<< HEAD
import java.io.IOException;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import com.medicare.HelloApplication;
import com.medicare.models.User;
import com.medicare.services.RendezVousService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
=======
import com.medicare.HelloApplication;
import com.medicare.models.User;
import com.medicare.services.RendezVousService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f
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

public class DashboardPatientController {

    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;
    @FXML private StackPane contentArea;

    @FXML private Button btnAccueil;
    @FXML private Button btnRendezVous;
    @FXML private Button btnDonation;
    @FXML private Button btnProduit;
<<<<<<< HEAD
    @FXML private Button btnPartnerships;
    @FXML private Button btnMesCollaborations;
    @FXML private Button btnForum;
    @FXML private Button btnDevenirMedecin;
    @FXML private Button btnLogout;
    @FXML private Button chatbotButton;
=======
    @FXML private Button btnCollaboration;
    @FXML private Button btnForum;
    @FXML private Button btnDevenirMedecin;
    @FXML private Button btnLogout;
>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f

    private static User currentUser;

    public static void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser() { return currentUser; }

    @FXML
    private void initialize() {
        if (currentUser != null) {
<<<<<<< HEAD
            userNameLabel.setText(currentUser.getNom());
=======
            userNameLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom());
>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f
            userEmailLabel.setText(currentUser.getEmail());
        }

        // Icônes FontAwesome sur chaque bouton
        btnAccueil.setGraphic(icon(FontAwesomeSolid.HOME));
        btnRendezVous.setGraphic(icon(FontAwesomeSolid.CALENDAR_ALT));
        btnDonation.setGraphic(icon(FontAwesomeSolid.HEART));
        btnProduit.setGraphic(icon(FontAwesomeSolid.SHOPPING_CART));
<<<<<<< HEAD
        btnPartnerships.setGraphic(icon(FontAwesomeSolid.HANDSHAKE));
        btnMesCollaborations.setGraphic(icon(FontAwesomeSolid.LIST_ALT));
        btnForum.setGraphic(icon(FontAwesomeSolid.COMMENTS));
        btnDevenirMedecin.setGraphic(icon(FontAwesomeSolid.USER_MD, Color.web("#ffd700")));
        btnLogout.setGraphic(icon(FontAwesomeSolid.SIGN_OUT_ALT, Color.web("#ffcccb")));
        chatbotButton.setGraphic(icon(FontAwesomeSolid.COMMENTS, Color.web("#1a73e8")));
=======
        btnCollaboration.setGraphic(icon(FontAwesomeSolid.HANDSHAKE));
        btnForum.setGraphic(icon(FontAwesomeSolid.COMMENTS));
        btnDevenirMedecin.setGraphic(icon(FontAwesomeSolid.USER_MD, Color.web("#ffd700")));
        btnLogout.setGraphic(icon(FontAwesomeSolid.SIGN_OUT_ALT, Color.web("#ffcccb")));
>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f

        highlightButton(btnAccueil);
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

<<<<<<< HEAD
    @FXML private void onPartnershipsClick() {
        highlightButton(btnPartnerships);
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("user-collaboration-view.fxml"));
            Node view = loader.load();
            UserCollaborationController controller = loader.getController();
            controller.setDashboardStackPane(contentArea);
            setContent(view);
        } catch (IOException e) {
            e.printStackTrace();
            setContent(new Label("Erreur de chargement de la page des partenariats."));
        }
    }

    @FXML private void onMesCollaborationsClick() {
        highlightButton(btnMesCollaborations);
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("user-collaborations-view.fxml"));
            Node view = loader.load();
            setContent(view);
        } catch (IOException e) {
            e.printStackTrace();
            setContent(new Label("Erreur de chargement de la page des collaborations."));
        }
=======
    @FXML private void onCollaborationClick() {
        highlightButton(btnCollaboration);
        setContent(new Label("Collaborer") {{
            setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
        }});
>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f
    }

    @FXML private void onForumClick() {
        highlightButton(btnForum);
        setContent(new Label("Forum") {{
            setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
        }});
    }

    @FXML private void onDevenirMedecinClick() {
        highlightButton(btnDevenirMedecin);
        setContent(new Label("Devenir Medecin") {{
            setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
        }});
        // TODO : formulaire de demande pour devenir médecin
    }

<<<<<<< HEAD
    @FXML
    private void onChatbotClick() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("chatbot-view.fxml"));
            Parent root = loader.load();
            Stage chatbotStage = new Stage();
                chatbotStage.setTitle("Medicare AI Assistant");
                Scene scene = new Scene(root);
                scene.getStylesheets().add(HelloApplication.class.getResource("chatbot-style.css").toExternalForm());
                chatbotStage.setScene(scene);
                chatbotStage.setResizable(false);
                chatbotStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            // Optionally, show an error alert to the user
        }
    }

=======
>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f
    @FXML private void onLogoutClick() {
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

    private void highlightButton(Button active) {
        String normalStyle = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";
        String normalGoldStyle = "-fx-background-color: transparent; -fx-text-fill: #ffd700; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";
        String activeStyle = "-fx-background-color: #4a9af5; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";

        btnAccueil.setStyle(normalStyle);
        btnRendezVous.setStyle(normalStyle);
        btnDonation.setStyle(normalStyle);
        btnProduit.setStyle(normalStyle);
<<<<<<< HEAD
        btnPartnerships.setStyle(normalStyle);
        btnMesCollaborations.setStyle(normalStyle);
=======
        btnCollaboration.setStyle(normalStyle);
>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f
        btnForum.setStyle(normalStyle);
        btnDevenirMedecin.setStyle(normalGoldStyle);

        active.setStyle(activeStyle);
    }
<<<<<<< HEAD
}
=======
}
>>>>>>> 75109ed9a765b50d8f229f0e8f802d201bdaab2f
