package com.medicare.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class DashboardUserController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Button btnAccueil;
    @FXML private Button btnRendezVous;
    @FXML private Button btnDon;
    @FXML private Button btnProduits;
    @FXML private Button btnCollaborer;
    @FXML private Button btnForum;
    @FXML private Button btnDevenirMedecin;
    @FXML private Button btnLogout;

    private Button selectedButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupButtonIcons();
        onAccueilClick(); // Set initial view
    }

    private void setupButtonIcons() {
        btnAccueil.setGraphic(new FontIcon(FontAwesomeSolid.HOME));
        btnRendezVous.setGraphic(new FontIcon(FontAwesomeSolid.CALENDAR_CHECK));
        btnDon.setGraphic(new FontIcon(FontAwesomeSolid.HEART));
        btnProduits.setGraphic(new FontIcon(FontAwesomeSolid.BOX_OPEN));
        btnCollaborer.setGraphic(new FontIcon(FontAwesomeSolid.HANDSHAKE));
        btnForum.setGraphic(new FontIcon(FontAwesomeSolid.COMMENTS));
        btnDevenirMedecin.setGraphic(new FontIcon(FontAwesomeSolid.USER_MD));
        btnLogout.setGraphic(new FontIcon(FontAwesomeSolid.SIGN_OUT_ALT));
    }

    private void highlightButton(Button button) {
        if (selectedButton != null) {
            selectedButton.getStyleClass().remove("sidebar-button-selected");
        }
        button.getStyleClass().add("sidebar-button-selected");
        selectedButton = button;
    }

    private void setContent(Node node) {
        contentArea.getChildren().setAll(node);
    }

    @FXML
    private void onAccueilClick() {
        highlightButton(btnAccueil);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/user-home-view.fxml"));
            Node view = loader.load();
            setContent(view);
        } catch (IOException e) {
            e.printStackTrace();
            setContent(new Label("Erreur de chargement de la page d'accueil."));
        }
    }

    @FXML
    private void onRendezVousClick() {
        highlightButton(btnRendezVous);
        setContent(new Label("Mes Rendez-vous"));
    }

    @FXML
    private void onDonClick() {
        highlightButton(btnDon);
        setContent(new Label("Faire un don"));
    }

    @FXML
    private void onProduitsClick() {
        highlightButton(btnProduits);
        setContent(new Label("Nos Produits"));
    }

    @FXML
    private void onCollaborerClick() {
        highlightButton(btnCollaborer);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/user-collaboration-view.fxml"));
            Node view = loader.load();

            // Pass the main content area to the controller to enable navigation
            UserCollaborationController controller = loader.getController();
            controller.setDashboardStackPane(contentArea);

            setContent(view);
        } catch (IOException e) {
            e.printStackTrace();
            setContent(new Label("Erreur de chargement de la page de collaboration."));
        }
    }

    @FXML
    private void onForumClick() {
        highlightButton(btnForum);
        setContent(new Label("Forum"));
    }

    @FXML
    private void onDevenirMedecinClick() {
        highlightButton(btnDevenirMedecin);
        setContent(new Label("Devenir Médecin"));
    }

    @FXML
    private void onLogoutClick() {
        // Implement logout logic
        System.out.println("Logout clicked");
    }
}