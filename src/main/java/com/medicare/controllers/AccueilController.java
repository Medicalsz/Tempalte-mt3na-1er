package com.medicare.controllers;

import com.medicare.HelloApplication;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.InputStream;

public class AccueilController {

    @FXML private ImageView logoView;

    @FXML
    private void initialize() {
        // Charge le logo s'il existe dans resources/com/medicare/images/logo.png
        InputStream logoStream = getClass().getResourceAsStream("/com/medicare/images/logo.png");
        if (logoStream != null) {
            logoView.setImage(new Image(logoStream));
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
            Stage stage = (Stage) logoView.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
