package com.medicare.controllers;

import com.medicare.HelloApplication;
import com.medicare.utils.MyConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.InputStream;

public class AccueilController {

    @FXML private ImageView logoView;
    @FXML private Label dbStatusLabel;

    @FXML
    private void initialize() {
        // Charge le logo s'il existe dans resources/com/medicare/images/logo.png
        InputStream logoStream = getClass().getResourceAsStream("/com/medicare/images/logo.png");
        if (logoStream != null) {
            logoView.setImage(new Image(logoStream));
        }

        // Affiche le statut DB
        boolean dbOk = MyConnection.getInstance().getCnx() != null;
        if (dbOk) {
            dbStatusLabel.setText("Base de donnees connectee");
            dbStatusLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 11px; -fx-font-weight: bold;");
        } else {
            dbStatusLabel.setText("Echec connexion DB");
            dbStatusLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11px; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void onConnexionClick() {
        navigateTo("login-view.fxml", "Medicare - Connexion");
    }

    @FXML
    private void onCreerCompteClick() {
        navigateTo("register-view.fxml", "Medicare - Inscription");
    }

    private void navigateTo(String fxml, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxml));
            Stage stage = (Stage) dbStatusLabel.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle(title);
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

