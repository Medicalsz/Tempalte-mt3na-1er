package com.medicare.controllers;

import com.medicare.models.Badge;
import com.medicare.models.Partner;
import com.medicare.models.PartnerRating;
import com.medicare.services.ReputationService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PartnerDetailController {

    @FXML
    private VBox mainContainer;
    @FXML
    private Label partnerNameLabel;
    @FXML
    private ImageView partnerImageView;
    @FXML
    private Label typeLabel;
    @FXML
    private Label statutLabel;
    @FXML
    private Label emailLabel;
    @FXML
    private Label telephoneLabel;
    @FXML
    private Label adresseLabel;
    @FXML
    private Label datePartenariatLabel;

    // Reputation section
    @FXML
    private Label averageRatingLabel;
    @FXML
    private FlowPane badgesPane;
    @FXML
    private ListView<PartnerRating> ratingsListView;

    // Add Comment Section
    @FXML
    private HBox ratingStarsContainer;
    @FXML
    private TextArea commentTextArea;

    private ReputationService reputationService;
    private Partner currentPartner;
    private int selectedRating = 0;
    private List<Text> ratingStars = new ArrayList<>();

    public PartnerDetailController() {
        this.reputationService = new ReputationService();
    }

    @FXML
    private void initialize() {
        setupRatingStars();
    }

    public void setPartner(Partner partner) {
        if (partner == null) {
            return;
        }
        this.currentPartner = partner;

        // --- Formatters ---
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");

        // --- Populate Basic Info ---
        partnerNameLabel.setText(partner.getName());
        typeLabel.setText(partner.getTypePartenaire());
        statutLabel.setText(partner.getStatut());
        emailLabel.setText(partner.getEmail());
        telephoneLabel.setText(partner.getTelephone());
        adresseLabel.setText(partner.getAdresse());

        if (partner.getDatePartenariat() != null) {
            datePartenariatLabel.setText(partner.getDatePartenariat().format(dateFormatter));
        } else {
            datePartenariatLabel.setText("Non spécifiée");
        }

        // --- Load Image ---
        if (partner.getImageName() != null && !partner.getImageName().isEmpty()) {
            try {
                Path imagePath = Paths.get("src/main/resources/uploads/partners", partner.getImageName());
                if (Files.exists(imagePath)) {
                    Image image = new Image(imagePath.toUri().toString());
                    partnerImageView.setImage(image);

                    // Apply a circular clip to the image view
                    Circle clip = new Circle(40, 40, 40); // The image view is 80x80, so radius is 40
                    partnerImageView.setClip(clip);
                }
            } catch (Exception e) {
                System.err.println("Could not load image for partner " + partner.getId() + ": " + e.getMessage());
                partnerImageView.setImage(null);
            }
        }

        // --- Load and Display Reputation Data ---
        loadReputationData(partner.getId());
    }

    private void loadReputationData(int partnerId) {
        // --- Ratings ---
        List<PartnerRating> ratings = reputationService.getRatingsForPartner(partnerId);
        if (ratings.isEmpty()) {
            averageRatingLabel.setText("N/A");
            ratingsListView.setPlaceholder(new Label("Aucun commentaire pour le moment."));
            ratingsListView.getItems().clear();
        } else {
            double average = ratings.stream().mapToInt(PartnerRating::getRating).average().orElse(0.0);
            averageRatingLabel.setText(String.format("%.1f / 5", average));
            ratingsListView.getItems().setAll(ratings);
        }

        // --- Badges ---
        List<Badge> badges = reputationService.getBadgesForPartner(partnerId);
        badgesPane.getChildren().clear();
        if (badges.isEmpty()) {
            badgesPane.getChildren().add(new Label("Aucun badge obtenu."));
        } else {
            for (Badge badge : badges) {
                VBox badgeView = new VBox();
                badgeView.getStyleClass().add("badge-view");

                // Use a static placeholder icon
                Text badgeIcon = new Text("🏆");
                badgeIcon.getStyleClass().add("badge-icon");

                // Make the "Top Rated" badge gold
                if ("Top Rated".equalsIgnoreCase(badge.getName())) {
                    badgeIcon.setFill(Color.GOLD);
                }

                Label badgeName = new Label(badge.getName());
                badgeName.getStyleClass().add("badge-name");

                badgeView.getChildren().addAll(badgeIcon, badgeName);
                badgesPane.getChildren().add(badgeView);
            }
        }

        // --- Customize ListView Cell ---
        ratingsListView.setCellFactory(lv -> new ListCell<PartnerRating>() {
            @Override
            protected void updateItem(PartnerRating item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // --- Main container for the comment ---
                    VBox commentBox = new VBox(10); // Spacing between header and text
                    commentBox.getStyleClass().add("comment-box");

                    // --- Header: Rating, Username, Date, Sentiment ---
                    HBox header = new HBox();
                    header.getStyleClass().add("comment-header");

                    Label ratingLabel = new Label(String.format("%d/5", item.getRating()));
                    ratingLabel.getStyleClass().add("comment-rating");

                    String userName = (item.getUserName() != null && !item.getUserName().isEmpty()) ? item.getUserName() : "Anonyme";
                    Label nameLabel = new Label(userName);
                    nameLabel.getStyleClass().add("comment-user-name");

                    // Sentiment Icon
                    Label sentimentIcon = new Label();
                    sentimentIcon.getStyleClass().add("sentiment-icon");
                    String sentiment = item.getSentiment();
                    if (sentiment != null) {
                        switch (sentiment.toLowerCase()) {
                            case "positive":
                                sentimentIcon.setText("😊");
                                sentimentIcon.getStyleClass().add("sentiment-positive");
                                break;
                            case "negative":
                                sentimentIcon.setText("😠");
                                sentimentIcon.getStyleClass().add("sentiment-negative");
                                break;
                            default:
                                sentimentIcon.setText("😐");
                                sentimentIcon.getStyleClass().add("sentiment-neutral");
                                break;
                        }
                    }

                    Label dateLabel = new Label();
                    dateLabel.getStyleClass().add("comment-date");
                    if (item.getCreatedAt() != null) {
                        dateLabel.setText("- " + item.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
                    } else {
                        dateLabel.setText("Date inconnue");
                    }

                    header.getChildren().addAll(ratingLabel, nameLabel, sentimentIcon, dateLabel);

                    commentBox.getChildren().add(header);

                    // --- Comment Text (if it exists) ---
                    if (item.getComment() != null && !item.getComment().trim().isEmpty()) {
                        Text commentText = new Text(item.getComment());
                        commentText.getStyleClass().add("comment-text");
                        commentText.setWrappingWidth(300); // Adjust width as needed
                        commentBox.getChildren().add(commentText);
                    }

                    setGraphic(commentBox);
                }
            }
        });
    }

    private void setupRatingStars() {
        for (int i = 1; i <= 5; i++) {
            Text star = new Text("☆");
            star.getStyleClass().add("rating-star");
            final int rating = i;

            star.setOnMouseClicked(event -> {
                selectedRating = rating;
                updateRatingStars();
            });

            ratingStars.add(star);
            ratingStarsContainer.getChildren().add(star);
        }
    }

    private void updateRatingStars() {
        for (int i = 0; i < ratingStars.size(); i++) {
            Text star = ratingStars.get(i);
            if (i < selectedRating) {
                star.setText("★");
                star.setFill(Color.GOLD);
            } else {
                star.setText("☆");
                star.setFill(Color.BLACK);
            }
        }
    }

    @FXML
    private void submitComment() {
        String commentText = commentTextArea.getText();
        if (selectedRating == 0 || commentText.trim().isEmpty()) {
            // You can show an alert here to inform the user
            System.out.println("Please select a rating and write a comment.");
            return;
        }

        reputationService.addRating(currentPartner.getId(), selectedRating, commentText);

        // Clear the form and reload data
        commentTextArea.clear();
        selectedRating = 0;
        updateRatingStars();
        loadReputationData(currentPartner.getId());
    }
}