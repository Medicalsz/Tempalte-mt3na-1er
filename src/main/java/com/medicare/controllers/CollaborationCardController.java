package com.medicare.controllers;

import com.medicare.models.Collaboration;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;

public class CollaborationCardController {

    @FXML
    private VBox card;

    @FXML
    private ImageView partnerImageView;

    @FXML
    private Label titleLabel;

    @FXML
    private Label partnerNameLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label dateLabel;

    public void setData(Collaboration collaboration) {
        titleLabel.setText(collaboration.getTitre());
        partnerNameLabel.setText("Avec: " + collaboration.getPartnerName());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        String dateText = collaboration.getDateDebut().format(formatter) + " - " + collaboration.getDateFin().format(formatter);
        dateLabel.setText(dateText);

        statusLabel.setText(collaboration.getStatut());
        card.getStyleClass().removeAll("status-en-cours", "status-termine", "status-annule", "status-en-attente");
        switch (collaboration.getStatut().toLowerCase()) {
            case "en cours":
                card.getStyleClass().add("status-en-cours");
                break;
            case "terminé":
                card.getStyleClass().add("status-termine");
                break;
            case "annulé":
                card.getStyleClass().add("status-annule");
                break;
            default:
                card.getStyleClass().add("status-en-attente");
                break;
        }

        if (collaboration.getImageName() != null && !collaboration.getImageName().isEmpty()) {
            File imageFile = new File("uploads/partners/" + collaboration.getImageName());
            if (imageFile.exists()) {
                Image image = new Image(imageFile.toURI().toString());
                partnerImageView.setImage(image);
            } else {
                loadDefaultImage();
            }
        } else {
            loadDefaultImage();
        }
    }

    private void loadDefaultImage() {
        try (InputStream stream = getClass().getResourceAsStream("/images/default-partner.png")) {
            if (stream != null) {
                partnerImageView.setImage(new Image(stream));
            } else {
                System.err.println("Default partner image not found.");
                partnerImageView.setImage(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}