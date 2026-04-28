package com.medicare.controllers;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class UserHomeController implements Initializable {

    @FXML
    private GridPane actionsGrid;
    @FXML
    private Label welcomeTitle;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Add a timestamp to the title to check for updates
        String timeStamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        welcomeTitle.setText("Bienvenue (Mise à jour à " + timeStamp + ")");

        // Create and add action cards
        actionsGrid.add(createActionCard(
                FontAwesomeSolid.CALENDAR_CHECK,
                "Prendre un rendez-vous",
                "Gérez vos consultations avec nos médecins.",
                () -> System.out.println("Navigate to Rendez-vous")
        ), 0, 0);

        actionsGrid.add(createActionCard(
                FontAwesomeSolid.HANDSHAKE,
                "Voir nos partenaires",
                "Découvrez les organisations qui nous soutiennent.",
                () -> System.out.println("Navigate to Collaborer")
        ), 1, 0);

        actionsGrid.add(createActionCard(
                FontAwesomeSolid.COMMENTS,
                "Accéder au forum",
                "Échangez avec la communauté et nos experts.",
                () -> System.out.println("Navigate to Forum")
        ), 2, 0);
    }

    private VBox createActionCard(FontAwesomeSolid icon, String title, String description, Runnable action) {
        VBox card = new VBox();
        card.getStyleClass().add("action-card");

        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.getStyleClass().add("action-card-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("action-card-title");

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("action-card-description");

        card.getChildren().addAll(fontIcon, titleLabel, descLabel);
        card.setOnMouseClicked(event -> action.run());

        return card;
    }
}