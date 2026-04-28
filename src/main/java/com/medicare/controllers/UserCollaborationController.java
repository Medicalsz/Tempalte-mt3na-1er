package com.medicare.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import com.medicare.models.Partner;
import com.medicare.services.PartnerService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

public class UserCollaborationController implements Initializable {

    @FXML
    private TilePane tilePane;

    private StackPane dashboardStackPane;

    private final PartnerService partnerService = new PartnerService();

    public void setDashboardStackPane(StackPane dashboardStackPane) {
        this.dashboardStackPane = dashboardStackPane;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadPartners();
    }

    private void loadPartners() {
        List<Partner> partners = partnerService.getAll();
        tilePane.getChildren().clear();

        if (partners.isEmpty()) {
            Label noPartnersLabel = new Label("Aucun partenaire trouvé.");
            noPartnersLabel.getStyleClass().add("no-data-label");
            tilePane.getChildren().add(noPartnersLabel);
            return;
        }

        for (Partner partner : partners) {
            tilePane.getChildren().add(createPartnerCard(partner));
        }
    }

    private Node createPartnerCard(Partner partner) {
        VBox card = new VBox();
        card.getStyleClass().add("partner-card");

        // Partner Image
        ImageView imageView = new ImageView();
        imageView.setFitHeight(80);
        imageView.setFitWidth(80);
        
        try {
            // CORRECTED IMAGE PATH: Load image as a resource stream.
            String imageName = partner.getImageName();
            if (imageName == null || imageName.isEmpty()) {
                throw new Exception("Image name is empty.");
            }
            Image image = new Image(getClass().getResourceAsStream("/uploads/partners/" + imageName));
            if(image.isError()) {
                 throw new Exception("Failed to load image: " + imageName);
            }
            imageView.setImage(image);
        } catch (Exception e) {
            // If the image fails to load, print an error but don't crash.
            // The card will be created without an image.
            System.err.println("Could not load image for partner " + partner.getName() + ". Reason: " + e.getMessage());
        }
        Circle clip = new Circle(40, 40, 40);
        imageView.setClip(clip);
        imageView.getStyleClass().add("partner-image");

        // Partner Info
        Label nameLabel = new Label(partner.getName());
        nameLabel.getStyleClass().add("partner-name");

        Label typeLabel = new Label(partner.getTypePartenaire());
        typeLabel.getStyleClass().add("partner-type");

        // Details Button
        Button detailsButton = new Button("Voir les détails");
        detailsButton.getStyleClass().add("details-button");
        detailsButton.setOnAction(event -> showPartnerDetails(partner));

        card.getChildren().addAll(imageView, nameLabel, typeLabel, detailsButton);
        return card;
    }

    private void showPartnerDetails(Partner partner) {
        if (dashboardStackPane == null) {
            System.err.println("Dashboard StackPane is not set. Cannot navigate.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/partner-detail-view.fxml"));
            Node detailView = loader.load();

            PartnerDetailController controller = loader.getController();
            controller.setPartner(partner);

            // Load the detail view into the main dashboard's content area
            dashboardStackPane.getChildren().setAll(detailView);

        } catch (IOException e) {
            e.printStackTrace();
            // Handle error (e.g., show an alert)
        }
    }
}