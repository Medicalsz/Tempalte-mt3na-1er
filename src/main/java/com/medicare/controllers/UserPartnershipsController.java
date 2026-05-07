package com.medicare.controllers;

import com.medicare.models.Collaboration;
import com.medicare.services.CollaborationService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

public class UserPartnershipsController {

    @FXML
    private TilePane collaborationsPane;

    private StackPane dashboardStackPane;

    private final CollaborationService collaborationService = new CollaborationService();

    public void setDashboardStackPane(StackPane dashboardStackPane) {
        this.dashboardStackPane = dashboardStackPane;
    }

    @FXML
    private void initialize() {
        loadCollaborations();
    }

    private void loadCollaborations() {
        // Ensure the pane is clear before adding new cards
        collaborationsPane.getChildren().clear();
        List<Collaboration> collaborations = collaborationService.getAll();

        if (collaborations.isEmpty()) {
            collaborationsPane.getChildren().add(new Label("Aucune collaboration disponible pour le moment."));
            return;
        }

        for (Collaboration collaboration : collaborations) {
            try {
                // Load the card view for each collaboration
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/collaboration-card-view.fxml"));
                Node card = loader.load();

                CollaborationCardController controller = loader.getController();
                controller.setData(collaboration);

                // Add click event to show details
                card.setOnMouseClicked(event -> showCollaborationDetails(collaboration));

                collaborationsPane.getChildren().add(card);
            } catch (IOException e) {
                // Print a detailed error if a card fails to load
                System.err.println("Failed to load collaboration card for: " + collaboration.getTitre());
                e.printStackTrace();
            }
        }
    }

    private void showCollaborationDetails(Collaboration collaboration) {
        if (dashboardStackPane == null) {
            System.err.println("Dashboard StackPane is not set. Cannot show details.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/collaboration-details-view.fxml"));
            Node detailsView = loader.load();

            CollaborationDetailsController controller = loader.getController();
            controller.setContentArea(dashboardStackPane);
            controller.setListController(this); // Pass a reference to this controller
            controller.setData(collaboration);

            dashboardStackPane.getChildren().setAll(detailsView);
        } catch (IOException e) {
            System.err.println("Failed to load collaboration details view for: " + collaboration.getTitre());
            e.printStackTrace();
        }
    }

    public void reloadCollaborationsView() {
        if (dashboardStackPane == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/user-collaborations-view.fxml"));
            Node view = loader.load();
            UserPartnershipsController controller = loader.getController();
            controller.setDashboardStackPane(dashboardStackPane);
            dashboardStackPane.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}