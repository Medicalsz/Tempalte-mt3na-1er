package com.medicare.controllers;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import com.medicare.models.Collaboration;
import com.medicare.models.Partner;
import com.medicare.services.PartnerService;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.property.ProductId;


public class CollaborationDetailsController {

    @FXML private Label titleLabel;
    @FXML private Label partnerLabel;
    @FXML private Label datesLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label statusLabel;
    @FXML private Button backButton;
    @FXML private ImageView collaborationImageView;
    @FXML private ImageView qrCodeImageView;

    private StackPane contentArea;
    private UserPartnershipsController listController;
    private Collaboration currentCollaboration;

    public void setContentArea(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    public void setListController(UserPartnershipsController listController) {
        this.listController = listController;
    }

    public void setData(Collaboration collaboration) {
        if (collaboration == null) return;
        this.currentCollaboration = collaboration; // Store for later use

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");

        titleLabel.setText(collaboration.getTitre());
        partnerLabel.setText("En collaboration avec: " + collaboration.getPartnerName());
        
        String dateDebut = collaboration.getDateDebut() != null ? collaboration.getDateDebut().format(formatter) : "N/A";
        String dateFin = collaboration.getDateFin() != null ? collaboration.getDateFin().format(formatter) : "N/A";
        datesLabel.setText("Période: " + dateDebut + " au " + dateFin);

        descriptionLabel.setText(collaboration.getDescription());
        statusLabel.setText("Statut: " + collaboration.getStatut());

        // Style the status label based on its content
        String statusColor = switch (collaboration.getStatut().toLowerCase()) {
            case "approuvée", "en cours" -> "#16a34a"; // Green
            case "terminé" -> "#6b7280"; // Gray
            case "annulé" -> "#dc2626"; // Red
            default -> "#f59e0b"; // Yellow for "en attente" etc.
        };
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: white; -fx-padding: 3 10; " +
                             "-fx-background-color: " + statusColor + "; -fx-background-radius: 12;");

        // Load the image
        loadImage(collaboration.getImageName());

        // Generate and display QR code
        generateQRCode(collaboration);
    }

    @FXML
    private void onPartnerClick() {
        if (currentCollaboration == null) {
            System.err.println("Error: No collaboration data available to navigate to partner.");
            return;
        }

        try {
            PartnerService partnerService = new PartnerService();
            Partner partner = partnerService.getById(currentCollaboration.getPartnerId());

            if (partner == null) {
                System.err.println("Error: Could not find partner with ID: " + currentCollaboration.getPartnerId());
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/partner-detail-view.fxml"));
            Parent view = loader.load();

            PartnerDetailController controller = loader.getController();
            controller.setPartner(partner);

            contentArea.getChildren().setAll(view);

        } catch (Exception e) {
            System.err.println("Failed to load partner detail view.");
            e.printStackTrace();
        }
    }

    private void generateQRCode(Collaboration collaboration) {
        // Create a JSON-like string for better machine readability
        String details = String.format(
            "{\n" +
            "  \"type\": \"medicare_collaboration\",\n" +
            "  \"title\": \"%s\",\n" +
            "  \"partner\": \"%s\",\n" +
            "  \"startDate\": \"%s\",\n" +
            "  \"endDate\": \"%s\",\n" +
            "  \"description\": \"%s\"\n" +
            "}",
            escapeJson(collaboration.getTitre()),
            escapeJson(collaboration.getPartnerName()),
            collaboration.getDateDebut().format(DateTimeFormatter.ISO_LOCAL_DATE),
            collaboration.getDateFin().format(DateTimeFormatter.ISO_LOCAL_DATE),
            escapeJson(collaboration.getDescription())
        );

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = qrCodeWriter.encode(details, BarcodeFormat.QR_CODE, 200, 200);
            BufferedImage bufferedImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < 200; x++) {
                for (int y = 0; y < 200; y++) {
                    bufferedImage.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            Image qrImage = SwingFXUtils.toFXImage(bufferedImage, null);
            qrCodeImageView.setImage(qrImage);
        } catch (WriterException e) {
            System.err.println("Could not generate QR Code, WriterException :: " + e.getMessage());
        }
    }

    // Helper method to escape characters for JSON string
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    private void loadImage(String imageName) {
        if (imageName != null && !imageName.isEmpty()) {
            try {
                String projectDir = System.getProperty("user.dir");
                File imageFile = new File(projectDir, "src/main/resources/uploads/partners/" + imageName);

                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    collaborationImageView.setImage(image);
                    return; // Exit after successful load
                } else {
                    System.err.println("Image file not found at: " + imageFile.getAbsolutePath());
                }
            } catch (Exception e) {
                System.err.println("Error loading image: " + imageName);
                e.printStackTrace();
            }
        }
        // If specific image fails or is not provided, load the default
        loadDefaultImage();
    }

    private void loadDefaultImage() {
        try (InputStream stream = getClass().getResourceAsStream("/com/medicare/images/logo.png")) {
            if (stream != null) {
                collaborationImageView.setImage(new Image(stream));
            } else {
                System.err.println("Default logo image not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onQRCodeClick() {
        if (qrCodeImageView.getImage() == null) return;

        // Create a new stage (window) for the enlarged QR code
        Stage qrStage = new Stage();
        qrStage.initModality(Modality.APPLICATION_MODAL);
        qrStage.setTitle("QR Code");

        ImageView enlargedQrView = new ImageView(qrCodeImageView.getImage());
        enlargedQrView.setFitWidth(400);
        enlargedQrView.setFitHeight(400);
        enlargedQrView.setPreserveRatio(true);

        StackPane layout = new StackPane(enlargedQrView);
        layout.setStyle("-fx-background-color: white;");

        Scene scene = new Scene(layout);
        qrStage.setScene(scene);
        qrStage.show();
    }

    @FXML
    private void onBackClick() {
        if (listController != null) {
            listController.reloadCollaborationsView();
        } else {
            System.err.println("Error: ListController is not set. Cannot go back.");
        }
    }

    @FXML
    private void onAddToCalendarClick() {
        if (currentCollaboration == null) {
            showError("No collaboration data available.");
            return;
        }

        ICalendar ical = new ICalendar();
        ical.setProductId(new ProductId("-//Medicare App//Collaboration Calendar//EN"));

        VEvent event = new VEvent();
        event.setSummary(currentCollaboration.getTitre());
        event.setDescription(currentCollaboration.getDescription());

        // Convert LocalDate to Date for biweekly
        Date startDate = Date.from(currentCollaboration.getDateDebut().atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(currentCollaboration.getDateFin().atStartOfDay(ZoneId.systemDefault()).toInstant());
        event.setDateStart(startDate, false); // false for date-only, not time
        event.setDateEnd(endDate, false);

        ical.addEvent(event);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save to Calendar");
        String fileName = currentCollaboration.getTitre().replaceAll("[^a-zA-Z0-9]", "_") + ".ics";
        fileChooser.setInitialFileName(fileName);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("iCalendar File (*.ics)", "*.ics"));

        File file = fileChooser.showSaveDialog(contentArea.getScene().getWindow());

        if (file != null) {
            try {
                Biweekly.write(ical).go(file);
                showSuccess("Event saved successfully to " + file.getName());
            } catch (IOException e) {
                e.printStackTrace();
                showError("Failed to save calendar event: " + e.getMessage());
            }
        }
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
