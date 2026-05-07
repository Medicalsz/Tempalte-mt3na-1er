package com.medicare.controllers;

import com.medicare.models.Don;
import com.medicare.services.DonationService;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class DonationAddressPopupController {

    @FXML private VBox rootPane;
    @FXML private Label thankYouLabel;
    @FXML private Label objectsLabel;
    @FXML private TextField addressField;
    @FXML private Label errorLabel;
    @FXML private Button confirmButton;

    private Don currentDon;
    private final DonationService donationService = new DonationService();

    public void setDon(Don don) {
        this.currentDon = don;
        if (don.getMateriels() != null && !don.getMateriels().isEmpty()) {
            objectsLabel.setText(don.getMateriels().replace("\n", ", "));
        }
    }

    @FXML
    private void onConfirm() {
        String address = addressField.getText().trim();
        
        // Contrôle de saisie simple
        if (address.length() < 10) {
            errorLabel.setVisible(true);
            addressField.setStyle("-fx-border-color: #ef4444; -fx-padding: 12; -fx-background-radius: 8; -fx-border-radius: 8;");
            return;
        }

        if (donationService.updateDonAdresse(currentDon.getId(), address)) {
            currentDon.setAdresse(address);
            showSuccessAnimation();
        }
    }

    private void showSuccessAnimation() {
        confirmButton.setDisable(true);
        addressField.setDisable(true);
        errorLabel.setVisible(false);
        
        thankYouLabel.setText("Adresse enregistrée avec succès !");
        thankYouLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 16px;");
        objectsLabel.setText("Nos équipes vous contacteront bientôt.");
        
        // Petite animation de fermeture
        ScaleTransition scale = new ScaleTransition(Duration.millis(300), rootPane);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(0.8);
        scale.setToY(0.8);
        
        FadeTransition fade = new FadeTransition(Duration.millis(300), rootPane);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        
        SequentialTransition seq = new SequentialTransition(scale, fade);
        seq.setOnFinished(e -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.close();
        });
        
        seq.play();
    }
}
