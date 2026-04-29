package com.medicare.controllers;

import com.medicare.HelloApplication;
import com.medicare.models.User;
import com.medicare.services.RendezVousService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

public class DashboardPatientController {

    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;
    @FXML private StackPane contentArea;

    @FXML private Button btnAccueil;
    @FXML private Button btnRendezVous;
    @FXML private Button btnDonation;
    @FXML private Button btnProduit;
    @FXML private Button btnCollaboration;
    @FXML private Button btnForum;
    @FXML private Button btnDevenirMedecin;
    @FXML private Button btnLogout;

    private static User currentUser;

    public static void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser() { return currentUser; }

    private final RendezVousService rvService = new RendezVousService();
    private final com.medicare.services.DonationService donationService = new com.medicare.services.DonationService();

    @FXML
    private void initialize() {
        if (currentUser != null) {
            userNameLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom());
            userEmailLabel.setText(currentUser.getEmail());
            
            // Vérifier s'il y a des dons confirmés sans adresse
            javafx.application.Platform.runLater(this::checkConfirmedDonsWithoutAddress);
        }

        // Icônes FontAwesome sur chaque bouton
        btnAccueil.setGraphic(icon(FontAwesomeSolid.HOME));
        btnRendezVous.setGraphic(icon(FontAwesomeSolid.CALENDAR_ALT));
        btnDonation.setGraphic(icon(FontAwesomeSolid.HEART));
        btnProduit.setGraphic(icon(FontAwesomeSolid.SHOPPING_CART));
        btnCollaboration.setGraphic(icon(FontAwesomeSolid.HANDSHAKE));
        btnForum.setGraphic(icon(FontAwesomeSolid.COMMENTS));
        btnDevenirMedecin.setGraphic(icon(FontAwesomeSolid.USER_MD, Color.web("#ffd700")));
        btnLogout.setGraphic(icon(FontAwesomeSolid.SIGN_OUT_ALT, Color.web("#ffcccb")));

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

    @FXML private void onProduitClick() {
        highlightButton(btnProduit);
        setContent(new Label("Nos Produits") {{
            setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
        }});
    }

    @FXML private void onCollaborationClick() {
        highlightButton(btnCollaboration);
        setContent(new Label("Collaborer") {{
            setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
        }});
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

    @FXML private void onLogoutClick() {
        currentUser = null;
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("accueil-view.fxml"));
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Medicare");
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ========== UTILITAIRES ==========

    private void setContent(Node node) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(node);
    }

    private void checkConfirmedDonsWithoutAddress() {
        if (currentUser == null) return;
        
        java.util.List<com.medicare.models.Don> myDons = donationService.getDonsByUserId(currentUser.getId());
        for (com.medicare.models.Don don : myDons) {
            // Si le don est matériel, confirmé et n'a pas encore d'adresse
            if ("materiel".equals(don.getType()) && 
                "confirmé".equalsIgnoreCase(don.getStatut()) && 
                (don.getAdresse() == null || don.getAdresse().trim().isEmpty() || don.getAdresse().equalsIgnoreCase("pas d'adresse"))) {
                
                showAddressPopup(don);
                break; // On n'en montre qu'un à la fois
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
            
            // Centrer sur la fenêtre principale
            if (userNameLabel.getScene() != null && userNameLabel.getScene().getWindow() != null) {
                Stage mainStage = (Stage) userNameLabel.getScene().getWindow();
                stage.initOwner(mainStage);
            }
            
            stage.show();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'affichage de la popup d'adresse : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void highlightButton(Button active) {
        String normalStyle = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";
        String normalGoldStyle = "-fx-background-color: transparent; -fx-text-fill: #ffd700; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";
        String activeStyle = "-fx-background-color: #4a9af5; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";

        btnAccueil.setStyle(normalStyle);
        btnRendezVous.setStyle(normalStyle);
        btnDonation.setStyle(normalStyle);
        btnProduit.setStyle(normalStyle);
        btnCollaboration.setStyle(normalStyle);
        btnForum.setStyle(normalStyle);
        btnDevenirMedecin.setStyle(normalGoldStyle);

        active.setStyle(activeStyle);
    }
}
