package com.medicare.controllers;

import java.io.File;

import com.medicare.models.Donation;
import com.medicare.models.User;
import com.medicare.services.DonationService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class UserCauseDetailController {

    @FXML private Label causeTitleLabel;
    @FXML private ImageView causeImageView;
    @FXML private Label causeDescLabel;
    @FXML private Label currentAmountLabel;
    @FXML private Label goalAmountLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label percentReachedLabel;
    @FXML private Button btnBack;
    @FXML private Button btnDonateNow;

    @FXML private ImageView badgeImageView;
    @FXML private Label badgeNameLabel;
    @FXML private Label totalDonatedLabel;

    private Donation selectedCause;
    private final DonationService donationService = new DonationService();

    public void setCause(Donation cause) {
        this.selectedCause = cause;
        displayCauseDetails();
        updateBadge();
    }

    private void updateBadge() {
        User user = DashboardPatientController.getCurrentUser();
        if (user == null) return;

        double total = donationService.getTotalMoneyDonatedByUser(user.getId());
        totalDonatedLabel.setText(String.format("Total des dons : %.0f DT", total));

        String badgeName;
        String imageFile;

        if (total >= 100000) {
            badgeName = "Badge Diamond";
            imageFile = "diamond.jpg";
        } else if (total >= 50000) {
            badgeName = "Badge Platine";
            imageFile = "platine.jpg";
        } else if (total >= 10000) {
            badgeName = "Badge Emeraude";
            imageFile = "emeraude.jpg";
        } else if (total >= 5000) {
            badgeName = "Badge Or";
            imageFile = "or.png";
        } else if (total >= 1000) {
            badgeName = "Badge Argent";
            imageFile = "argent.png";
        } else {
            badgeName = "Badge Bronze";
            imageFile = "bronze.png";
        }

        badgeNameLabel.setText(badgeName);
        
        try {
            File file = new File("c:/Users/USER/Downloads/Tempalte-mt3na-1er-main/Tempalte java/badges/" + imageFile);
            if (file.exists()) {
                badgeImageView.setImage(new Image(file.toURI().toString()));
            } else {
                System.err.println("Fichier badge non trouvé : " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement badge : " + e.getMessage());
        }
    }

    private void displayCauseDetails() {
        if (selectedCause == null) return;

        causeTitleLabel.setText(selectedCause.getNom());
        causeDescLabel.setText(selectedCause.getDescription());
        currentAmountLabel.setText(String.format("%.0f DT", selectedCause.getMontantActuel()));
        goalAmountLabel.setText(String.format("Objectif : %.0f DT", selectedCause.getObjectifMontant()));
        
        double progress = selectedCause.getPourcentage() / 100.0;
        progressBar.setProgress(progress);
        percentReachedLabel.setText(String.format("%.0f%% de l'objectif atteint", selectedCause.getPourcentage()));

        // Chargement de l'image
        try {
            if (selectedCause.getImage() != null && !selectedCause.getImage().isEmpty()) {
                String imagePath = selectedCause.getImage();
                if (imagePath.startsWith("http") || imagePath.startsWith("file:")) {
                    causeImageView.setImage(new Image(imagePath, true));
                } else {
                    File file = new File(imagePath);
                    if (file.exists()) {
                        causeImageView.setImage(new Image(file.toURI().toString(), true));
                    } else {
                        causeImageView.setImage(new Image(getClass().getResourceAsStream("/com/medicare/images/logo.png")));
                    }
                }
            } else {
                causeImageView.setImage(new Image(getClass().getResourceAsStream("/com/medicare/images/logo.png")));
            }
        } catch (Exception e) {
            causeImageView.setImage(new Image(getClass().getResourceAsStream("/com/medicare/images/logo.png")));
        }

        // Coins arrondis pour l'image
        Rectangle clip = new Rectangle(causeImageView.getFitWidth(), causeImageView.getFitHeight());
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        causeImageView.setClip(clip);
    }

    @FXML
    private void onBackClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/user-donation-view.fxml"));
            Node view = loader.load();
            
            // On remonte au StackPane principal (contentArea)
            StackPane contentArea = (StackPane) causeTitleLabel.getScene().lookup("#contentArea");
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onDonateNowClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/user-donation-form-view.fxml"));
            Node view = loader.load();
            
            UserDonationFormController controller = loader.getController();
            controller.setCause(selectedCause);
            
            // On remonte au StackPane principal (contentArea)
            StackPane contentArea = (StackPane) causeTitleLabel.getScene().lookup("#contentArea");
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            System.err.println("Erreur ouverture formulaire de don : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
