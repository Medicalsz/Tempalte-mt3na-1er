package com.medicare.controllers;

import java.io.File;
import java.util.List;

import com.medicare.models.Donation;
import com.medicare.services.DonationService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class UserDonationController {

    @FXML private FlowPane donationContainer;
    @FXML private Button btnAssistant;
    @FXML private VBox assistantBubble;
    @FXML private Label assistantText;

    private final DonationService donationService = new DonationService();
    private Donation suggestedCause;

    @FXML
    public void initialize() {
        loadCauses();
        assistantBubble.setVisible(false);
    }

    @FXML
    private void onAssistantClick() {
        if (assistantBubble.isVisible()) {
            assistantBubble.setVisible(false);
            return;
        }

        List<Donation> causes = donationService.getAllDonations();
        if (causes.isEmpty()) {
            showAssistantMessage("Il n'y a actuellement aucune cause à soutenir. Revenez plus tard !");
            return;
        }

        suggestedCause = null;
        double maxRemaining = -1;

        for (Donation cause : causes) {
            double remaining = cause.getObjectifMontant() - cause.getMontantActuel();
            if (remaining > maxRemaining) {
                maxRemaining = remaining;
                suggestedCause = cause;
            }
        }

        if (suggestedCause != null) {
            String message = "Bonjour ! J'ai analysé les causes pour vous. La cause \"" + suggestedCause.getNom() + 
                "\" a actuellement le plus grand besoin de soutien.\n\n" +
                "• Objectif : " + String.format("%.0f", suggestedCause.getObjectifMontant()) + " DT\n" +
                "• Montant atteint : " + String.format("%.0f", suggestedCause.getMontantActuel()) + " DT (" + 
                String.format("%.1f", suggestedCause.getPourcentage()) + "%)\n" +
                "• Reste à collecter : " + String.format("%.0f", maxRemaining) + " DT\n\n" +
                "C'est le moment idéal pour faire un geste ! Souhaitez-vous faire un don à cette cause ?";
            
            showAssistantMessage(message);
            speak("D'après mes analyses, la cause " + suggestedCause.getNom() + 
                  " est celle qui a le plus besoin de soutien actuellement. Il manque encore " + 
                  String.format("%.0f", maxRemaining) + " DT. Souhaitez-vous l'aider ?");
        }
    }

    private void showAssistantMessage(String message) {
        assistantText.setText(message);
        assistantBubble.setVisible(true);
    }

    @FXML
    private void onAssistantYes() {
        assistantBubble.setVisible(false);
        if (suggestedCause != null) {
            showCauseDetail(suggestedCause);
        }
    }

    @FXML
    private void onAssistantNo() {
        assistantBubble.setVisible(false);
    }

    private void speak(String text) {
        new Thread(() -> {
            java.io.File tempVbs = null;
            try {
                // Créer un script VBS temporaire pour la parole (méthode la plus fiable sur Windows)
                tempVbs = java.io.File.createTempFile("medicare_speech", ".vbs");
                String cleanText = text.replace("\"", "'").replace("\n", " ");
                String vbsContent = "Set sapi = CreateObject(\"SAPI.SpVoice\")\nsapi.Speak \"" + cleanText + "\"";
                
                java.nio.file.Files.writeString(tempVbs.toPath(), vbsContent);
                
                System.out.println("Lancement de la voix via VBScript...");
                ProcessBuilder pb = new ProcessBuilder("wscript", tempVbs.getAbsolutePath());
                Process p = pb.start();
                p.waitFor();
            } catch (Exception e) {
                System.err.println("Erreur lors de la lecture vocale VBS : " + e.getMessage());
            } finally {
                if (tempVbs != null && tempVbs.exists()) {
                    tempVbs.delete();
                }
            }
        }).start();
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void onMyDonsClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/user-my-dons-view.fxml"));
            Node view = loader.load();
            StackPane contentArea = (StackPane) donationContainer.getScene().lookup("#contentArea");
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCauses() {
        donationContainer.getChildren().clear();
        List<Donation> causes = donationService.getAllDonations();
        System.out.println("DonationController: " + causes.size() + " causes reçues.");

        if (causes.isEmpty()) {
            Label noData = new Label("Aucune cause disponible pour le moment.");
            noData.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748b; -fx-padding: 20;");
            donationContainer.getChildren().add(noData);
            return;
        }

        for (Donation cause : causes) {
            System.out.println("Affichage cause: " + cause.getNom() + " (" + cause.getMontantActuel() + "/" + cause.getObjectifMontant() + ")");
            donationContainer.getChildren().add(createCauseCard(cause));
        }
    }

    private VBox createCauseCard(Donation cause) {
        VBox card = new VBox(15);
        card.setPrefWidth(320);
        card.setStyle("-fx-background-color: white; " +
                     "-fx-background-radius: 15; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 5); " +
                     "-fx-cursor: hand;");

        // 1. Image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(320);
        imageView.setFitHeight(180);
        imageView.setPreserveRatio(false);
        
        // Clip for rounded corners on top
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(320, 180);
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        imageView.setClip(clip);

        try {
            if (cause.getImage() != null && !cause.getImage().isEmpty()) {
                String imagePath = cause.getImage();
                if (imagePath.startsWith("http") || imagePath.startsWith("file:")) {
                    imageView.setImage(new Image(imagePath, true));
                } else {
                    File file = new File(imagePath);
                    if (file.exists()) {
                        imageView.setImage(new Image(file.toURI().toString(), true));
                    } else {
                        imageView.setImage(new Image(getClass().getResourceAsStream("/com/medicare/images/logo.png")));
                    }
                }
            } else {
                imageView.setImage(new Image(getClass().getResourceAsStream("/com/medicare/images/logo.png")));
            }
        } catch (Exception e) {
            imageView.setImage(new Image(getClass().getResourceAsStream("/com/medicare/images/logo.png")));
        }

        // 2. Contenu texte
        VBox content = new VBox(10);
        content.setPadding(new Insets(0, 20, 20, 20));

        Label title = new Label(cause.getNom());
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        title.setWrapText(true);

        Label desc = new Label(cause.getDescription());
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        desc.setWrapText(true);
        desc.setMinHeight(50);
        desc.setMaxHeight(50);

        // 3. Barre de progression
        VBox progressBox = new VBox(5);
        double pourcentage = cause.getPourcentage();
        
        ProgressBar pb = new ProgressBar(pourcentage / 100.0);
        pb.setPrefWidth(280);
        pb.setPrefHeight(10);
        pb.setStyle("-fx-accent: #10b981; -fx-control-inner-background: #f1f5f9;");

        HBox stats = new HBox();
        Label percentLabel = new Label(String.format("%.0f%%", pourcentage));
        percentLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #10b981; -fx-font-size: 13px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Label amountLabel = new Label(String.format("%.0f / %.0f DT", cause.getMontantActuel(), cause.getObjectifMontant()));
        amountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        
        stats.getChildren().addAll(percentLabel, spacer, amountLabel);
        progressBox.getChildren().addAll(pb, stats);

        // 4. Bouton
        Button btnMore = new Button("En savoir plus");
        btnMore.setMaxWidth(Double.MAX_VALUE);
        btnMore.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12; -fx-background-radius: 30; -fx-cursor: hand;");
        
        btnMore.setOnAction(e -> {
            showCauseDetail(cause);
        });

        content.getChildren().addAll(title, desc, progressBox, btnMore);
        card.getChildren().addAll(imageView, content);
        
        return card;
    }

    private void showCauseDetail(Donation cause) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/user-cause-detail-view.fxml"));
            Node view = loader.load();
            
            UserCauseDetailController controller = loader.getController();
            controller.setCause(cause);
            
            // Accéder au contentArea du Dashboard pour changer la vue
            StackPane contentArea = (StackPane) donationContainer.getScene().lookup("#contentArea");
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
