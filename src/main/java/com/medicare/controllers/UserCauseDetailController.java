package com.medicare.controllers;

import java.io.File;

import com.medicare.models.Donation;
import com.medicare.models.User;
import com.medicare.services.DonationService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
    @FXML private VBox badgeProgressionBox;

    private static final Object[][] BADGE_TIERS = {
        {"Badge Bronze",  "bronze.png",  0.0,      "#cd7f32"},
        {"Badge Argent",  "argent.png",  1000.0,   "#94a3b8"},
        {"Badge Or",      "or.png",      5000.0,   "#f59e0b"},
        {"Badge Emeraude","emeraude.jpg",10000.0,  "#10b981"},
        {"Badge Platine", "platine.jpg", 50000.0,  "#6366f1"},
        {"Badge Diamond", "diamond.jpg", 100000.0, "#06b6d4"},
    };

    private Donation selectedCause;
    private final DonationService donationService = new DonationService();

    private Image loadBadgeImage(String imageFile) {
        File f = new File(System.getProperty("user.dir"), "badges/" + imageFile);
        return f.exists() ? new Image(f.toURI().toString()) : null;
    }

    public void setCause(Donation cause) {
        if (cause != null && cause.getId() > 0) {
            Donation refreshed = donationService.getCauseById(cause.getId());
            this.selectedCause = refreshed != null ? refreshed : cause;
        } else {
            this.selectedCause = cause;
        }
        displayCauseDetails();
        updateBadge();
    }

    private void updateBadge() {
        User user = DashboardPatientController.getCurrentUser();
        if (user == null) return;

        double total = donationService.getTotalMoneyDonatedByUser(user.getId());
        totalDonatedLabel.setText(String.format("Total des dons : %.0f DT", total));

        // Determine current tier index
        int currentTier = 0;
        for (int i = BADGE_TIERS.length - 1; i >= 0; i--) {
            if (total >= (double) BADGE_TIERS[i][2]) {
                currentTier = i;
                break;
            }
        }

        String badgeName  = (String) BADGE_TIERS[currentTier][0];
        String imageFile  = (String) BADGE_TIERS[currentTier][1];

        badgeNameLabel.setText(badgeName);

        Image img = loadBadgeImage(imageFile);
        if (img != null) badgeImageView.setImage(img);

        buildBadgeProgression(total, currentTier);
    }

    private void buildBadgeProgression(double total, int currentTier) {
        if (badgeProgressionBox == null) return;
        badgeProgressionBox.getChildren().clear();

        Label title = new Label("Progression des badges");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        badgeProgressionBox.getChildren().add(title);

        FlowPane grid = new FlowPane(10, 10);
        grid.setPrefWrapLength(300);

        for (int i = 0; i < BADGE_TIERS.length; i++) {
            String name      = (String) BADGE_TIERS[i][0];
            String imageFile = (String) BADGE_TIERS[i][1];
            double threshold = (double) BADGE_TIERS[i][2];
            String color     = (String) BADGE_TIERS[i][3];

            boolean isCurrent  = (i == currentTier);
            boolean isUnlocked = (total >= threshold);

            VBox tile = new VBox(5);
            tile.setPrefWidth(84);
            tile.setAlignment(Pos.CENTER);
            tile.setPadding(new Insets(8));

            String borderColor = isCurrent ? color : (isUnlocked ? "#e2e8f0" : "#f1f5f9");
            String bg          = isCurrent ? "rgba(0,0,0,0.04)" : "transparent";
            String borderWidth = isCurrent ? "2" : "1";
            tile.setStyle(
                "-fx-background-color: " + bg + ";" +
                "-fx-background-radius: 10;" +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-radius: 10;" +
                "-fx-border-width: " + borderWidth + ";"
            );

            ImageView iv = new ImageView();
            iv.setFitWidth(36);
            iv.setFitHeight(36);
            iv.setPreserveRatio(true);
            Image img = loadBadgeImage(imageFile);
            if (img != null) {
                iv.setImage(img);
                if (!isUnlocked) iv.setOpacity(0.3);
            }

            // Short name (remove "Badge " prefix)
            Label nameLbl = new Label(name.replace("Badge ", ""));
            nameLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " +
                (isCurrent ? color : (isUnlocked ? "#1e293b" : "#94a3b8")) + ";");
            nameLbl.setWrapText(true);
            nameLbl.setAlignment(Pos.CENTER);

            Label threshLbl = new Label(threshold == 0 ? "Débutant" : String.format("≥ %.0f DT", threshold));
            threshLbl.setStyle("-fx-font-size: 9px; -fx-text-fill: #94a3b8;");

            tile.getChildren().addAll(iv, nameLbl, threshLbl);
            grid.getChildren().add(tile);
        }

        badgeProgressionBox.getChildren().add(grid);
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
            StackPane contentArea = (StackPane) causeTitleLabel.getScene().lookup("#contentArea");
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            System.err.println("Erreur ouverture formulaire de don : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
