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
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class UserCollaborationController implements Initializable {

    @FXML
    private TilePane tilePane;

    private final PartnerService partnerService = new PartnerService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadPartners();
    }

    private void loadPartners() {
        List<Partner> partners = partnerService.getAll();
        tilePane.getChildren().clear();

        for (Partner partner : partners) {
            tilePane.getChildren().add(createPartnerCard(partner));
        }
    }

    private Node createPartnerCard(Partner partner) {
        VBox card = new VBox();
        card.getStyleClass().add("partner-card");

        // Partner Image
        ImageView imageView = new ImageView();
        try {
            Image image = new Image("file:src/main/resources/uploads/partners/" + partner.getImageName(), 80, 80, false, true);
            imageView.setImage(image);
        } catch (Exception e) {
            // Fallback image if loading fails
            Image fallbackImage = new Image(getClass().getResourceAsStream("/com/medicare/images/default-partner.png"));
            imageView.setImage(fallbackImage);
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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/partner-detail-view.fxml"));
            Parent root = loader.load();

            PartnerDetailController controller = loader.getController();
            controller.setPartner(partner);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Détails du Partenaire : " + partner.getName());
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            // Handle error (e.g., show an alert)
        }
    }
}