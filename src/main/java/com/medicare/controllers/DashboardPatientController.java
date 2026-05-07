package com.medicare.controllers;

import com.medicare.services.RendezVousService;
import com.medicare.models.User;
import com.medicare.utils.Session;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.kordamp.ikonli.javafx.FontIcon;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class DashboardPatientController implements Initializable {

    @FXML private Button btnAccueil;
    @FXML private Button btnRendezvous;
    @FXML private Button btnDon;
    @FXML private Button btnProduits;
    @FXML private Button btnPartenariats;
    @FXML private Button btnCollaborations;
    @FXML private Button btnForum;
    @FXML private Button btnGame;
    @FXML private Button btnBecomeDoctor;
    @FXML private Button btnLogout;

    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private StackPane contentArea;

    private Map<Button, String> buttonMap;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        buttonMap = new HashMap<>();
        buttonMap.put(btnAccueil, "Accueil");
        buttonMap.put(btnRendezvous, "Mes Rendez-vous");
        buttonMap.put(btnDon, "Faire un don");
        buttonMap.put(btnProduits, "Nos Produits");
        buttonMap.put(btnPartenariats, "Partenariats");
        buttonMap.put(btnCollaborations, "Collaborations");
        buttonMap.put(btnForum, "Forum");
        buttonMap.put(btnGame, "Mini Game");
        buttonMap.put(btnBecomeDoctor, "Devenir Medecin");


        // Set initial content & highlight
        onAccueilClick();

        // Set user info from session
        User currentUser = Session.getInstance().getCurrentUser();
        if (currentUser != null) {
            nameLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom());
            emailLabel.setText(currentUser.getEmail());
        }
    }

    private void highlightButton(Button selectedButton) {
        for (Button button : buttonMap.keySet()) {
            if (button != null) { // Safety check
                if (button.equals(selectedButton)) {
                    button.getStyleClass().add("nav-button-selected");
                } else {
                    button.getStyleClass().remove("nav-button-selected");
                }
            }
        }
    }

    private void setContent(Node node) {
        contentArea.getChildren().setAll(node);
    }

    @FXML
    private void onAccueilClick() {
        highlightButton(btnAccueil);
        setContent(new Label("Accueil page is under construction."));
    }

    @FXML
    private void onRendezvousClick() {
        highlightButton(btnRendezvous);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/rendez-vous-list-view.fxml"));
            Node view = loader.load();
            RendezVousListController ctrl = loader.getController();
            ctrl.setContentArea(contentArea);

            // Find the patient_id from the user_id
            RendezVousService rvService = new RendezVousService();
            int patientId = rvService.getPatientIdByUserId(Session.getInstance().getCurrentUser().getId());
            ctrl.setPatientId(patientId);

            setContent(view);
        } catch (Exception e) {
            e.printStackTrace();
            setContent(new Label("Error loading Rendez-vous page."));
        }
    }

    @FXML
    private void onDonClick() {
        highlightButton(btnDon);
        setContent(new Label("Faire un don page is under construction."));
    }

    @FXML
    private void onProduitsClick() {
        highlightButton(btnProduits);
        setContent(new Label("Nos Produits page is under construction."));
    }

    @FXML
    private void onPartenariatsClick() {
        highlightButton(btnPartenariats);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/user-partners-view.fxml"));
            Node view = loader.load();

            // Get the controller and pass the content area to it
            UserPartnersController controller = loader.getController();
            controller.loadPartners(contentArea);

            setContent(view);
        } catch (IOException e) {
            e.printStackTrace();
            setContent(new Label("Error loading Partenariats page."));
        }
    }

    @FXML
    private void onCollaborationsClick() {
        highlightButton(btnCollaborations);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/user-collaborations-view.fxml"));
            Node view = loader.load();

            // Get the controller and pass the content area to it for navigation
            UserPartnershipsController controller = loader.getController();
            controller.setDashboardStackPane(contentArea);

            setContent(view);
        } catch (IOException e) {
            e.printStackTrace();
            setContent(new Label("Error loading Collaborations page."));
        }
    }

    @FXML
    private void onForumClick() {
        highlightButton(btnForum);
        setContent(new Label("Forum page is under construction."));
    }

    @FXML
    private void onBecomeDoctorClick() {
        highlightButton(btnBecomeDoctor);
        setContent(new Label("Become Doctor page is under construction."));
    }

    @FXML
    private void onLogoutClick() {
        // 1. Clear the user session
        Session.getInstance().clearSession();

        try {
            // 2. Load the login view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/login-view.fxml"));
            Parent loginView = loader.load(); // Cast to Parent

            // 3. Get the current stage and set the new scene
            Stage stage = (Stage) btnLogout.getScene().getWindow();
            stage.getScene().setRoot(loginView);

        } catch (IOException e) {
            System.err.println("Failed to load login view after logout.");
            e.printStackTrace();
        }
    }
}