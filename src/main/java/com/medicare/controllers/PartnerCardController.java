package com.medicare.controllers;

import com.medicare.models.Partner;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class PartnerCardController {

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

    @FXML
    private Button detailButton;

    private Partner partner;
    private StackPane contentArea;

    public PartnerCardController(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    public void setData(Partner partner) {
        this.partner = partner;
        titleLabel.setText(partner.getName());
        partnerNameLabel.setText(partner.getTypePartenaire());

        // Hide status and date labels as they are not relevant for partners
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
        dateLabel.setVisible(false);
        dateLabel.setManaged(false);

        if (partner.getImageName() != null && !partner.getImageName().isEmpty()) {
            try {
                // Construct an absolute path from the project's root directory
                String projectDir = System.getProperty("user.dir");
                File imageFile = new File(projectDir, "src/main/resources/uploads/partners/" + partner.getImageName());

                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    partnerImageView.setImage(image);
                } else {
                    System.err.println("Image file not found at: " + imageFile.getAbsolutePath());
                    loadDefaultImage();
                }
            } catch (Exception e) {
                System.err.println("Error creating image path for: " + partner.getImageName());
                e.printStackTrace();
                loadDefaultImage();
            }
        } else {
            loadDefaultImage();
        }
    }

    @FXML
    private void onDetailClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/partner-detail-view.fxml"));
            Node view = loader.load();

            PartnerDetailController controller = loader.getController();
            controller.setPartner(partner);
            controller.setContentArea(contentArea); // Pass the contentArea

            contentArea.getChildren().setAll(view);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadDefaultImage() {
        try (InputStream stream = getClass().getResourceAsStream("/com/medicare/images/logo.png")) {
            if (stream != null) {
                partnerImageView.setImage(new Image(stream));
            } else {
                System.err.println("Default partner image (logo.png) not found.");
                partnerImageView.setImage(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
