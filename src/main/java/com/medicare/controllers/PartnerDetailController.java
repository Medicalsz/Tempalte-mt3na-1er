package com.medicare.controllers;

import java.awt.Desktop;
import java.io.InputStream;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.kordamp.ikonli.javafx.FontIcon;

import com.medicare.models.Badge;
import com.medicare.models.Comment;
import com.medicare.models.Partner;
import com.medicare.models.User;
import com.medicare.services.BadgeService;
import com.medicare.services.CommentService;
import com.medicare.utils.Session;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

public class PartnerDetailController {

    @FXML
    private ImageView partnerImageView;
    @FXML
    private Label partnerNameLabel;
    @FXML
    private Label partnerTypeLabel;
    @FXML
    private GridPane detailsGrid;
    @FXML
    private HBox ratingBox;
    @FXML
    private HBox badgesBox;
    @FXML
    private VBox commentsVBox;
    @FXML
    private Label commentsToggleLabel;
    @FXML
    private TextArea commentTextArea;
    @FXML
    private HBox addRatingBox;
    @FXML
    private Button submitCommentButton;

    private Partner currentPartner;
    private int selectedRating = 0;

    private final CommentService commentService = new CommentService();
    private final BadgeService badgeService = new BadgeService();

    public void setPartner(Partner partner) {
        this.currentPartner = partner; // Stocker le partenaire actuel

        // Informations de base
        partnerNameLabel.setText(partner.getName());
        partnerTypeLabel.setText(partner.getTypePartenaire());

        // Image du partenaire avec une gestion d'erreur robuste
        InputStream imageStream = null;
        try {
            // 1. Essayer de charger l'image spécifique au partenaire
            String imageName = partner.getImageName();
            if (imageName != null && !imageName.isEmpty()) {
                imageStream = getClass().getResourceAsStream("/uploads/partners/" + imageName);
            }

            // 2. Si l'image spécifique n'est pas trouvée, essayer de charger l'image par défaut
            if (imageStream == null) {
                System.err.println("Image non trouvée pour le partenaire: " + imageName + ". Tentative avec l'image par défaut.");
                imageStream = getClass().getResourceAsStream("/images/default_partner.png");
            }

            // 3. Si une image a été trouvée (spécifique ou par défaut), l'afficher
            if (imageStream != null) {
                Image image = new Image(imageStream);
                partnerImageView.setImage(image);
                Circle clip = new Circle(40, 40, 40); // Assure que l'image est bien circulaire
                partnerImageView.setClip(clip);
            } else {
                // 4. Si même l'image par défaut n'est pas trouvée, afficher un message d'erreur clair
                System.err.println("ERREUR CRITIQUE: L'image par défaut '/images/default_partner.png' est introuvable. L'affichage de l'image est annulé.");
                // On pourrait ici définir une couleur de fond ou une icône de remplacement
            }
        } catch (Exception e) {
            System.err.println("Une erreur inattendue est survenue lors du chargement de l'image : " + e.getMessage());
            e.printStackTrace();
        }

        // Remplissage dynamique de la grille de détails
        populateDetailsGrid(partner);

        // Réputation
        double avgRating = commentService.getAverageRatingForPartner(partner.getId());
        setStarRating(avgRating);

        // Badges
        List<Badge> badges = badgeService.getBadgesForPartner(partner.getId());
        badgesBox.getChildren().clear();
        if (badges.isEmpty()) {
            badgesBox.getChildren().add(new Label("Aucun badge obtenu."));
        } else {
            for (Badge badge : badges) {
                badgesBox.getChildren().add(createBadgeView(badge));
            }
        }


        // Commentaires
        refreshComments();

        // Initialisation du formulaire d'ajout de commentaire
        setupAddCommentSection();
    }

    private void refreshComments() {
        if (currentPartner == null) return;
        List<Comment> comments = commentService.getCommentsForPartner(currentPartner.getId());
        commentsVBox.getChildren().clear();
        for (Comment comment : comments) {
            commentsVBox.getChildren().add(createCommentCard(comment));
        }

        // Mettre à jour le label de basculement
        updateToggleLabel(comments.size());

        // Mettre à jour également la note moyenne
        double avgRating = commentService.getAverageRatingForPartner(currentPartner.getId());
        setStarRating(avgRating);
    }

    private void populateDetailsGrid(Partner partner) {
        detailsGrid.getChildren().clear();
        detailsGrid.getColumnConstraints().clear(); // Vider les anciennes contraintes

        // Contrainte pour la colonne des libellés (fixe)
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPrefWidth(120); // Largeur fixe pour les libellés
        col1.setHgrow(Priority.NEVER);

        // Contrainte pour la colonne des valeurs (qui s'étend)
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);

        detailsGrid.getColumnConstraints().addAll(col1, col2);

        // Création du bouton de contact une seule fois
        Button contactBtn = new Button();
        FontIcon mailIcon = new FontIcon("fas-envelope");
        mailIcon.getStyleClass().add("contact-icon");
        contactBtn.setGraphic(mailIcon);
        contactBtn.getStyleClass().add("contact-button");
        contactBtn.setOnAction(e -> handleContactPartner());

        Map<String, String> details = Map.of(
                "Téléphone", partner.getTelephone(),
                "Date Partenariat", partner.getDatePartenariat() != null ? partner.getDatePartenariat().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) : "N/A",
                "Statut", partner.getStatut(),
                "Email", partner.getEmail(),
                "Adresse", partner.getAdresse()
        );

        int row = 0;
        for (Map.Entry<String, String> entry : details.entrySet()) {
            Label headerLabel = new Label(entry.getKey());
            headerLabel.getStyleClass().add("grid-label-header");
            detailsGrid.add(headerLabel, 0, row);

            if (entry.getKey().equals("Email")) {
                HBox emailBox = new HBox(10);
                emailBox.setAlignment(Pos.CENTER_LEFT);
                Label valueLabel = new Label(entry.getValue());
                valueLabel.setWrapText(true);
                emailBox.getChildren().addAll(valueLabel, contactBtn);
                detailsGrid.add(emailBox, 1, row);
            } else {
                Label valueLabel = new Label(entry.getValue());
                valueLabel.setWrapText(true);
                detailsGrid.add(valueLabel, 1, row);
            }
            row++;
        }
    }

    private void setStarRating(double rating) {
        ratingBox.getChildren().clear();
        Label ratingLabel = new Label(String.format("Note moyenne : %.1f / 5", rating));
        ratingLabel.getStyleClass().add("reputation-label");
        ratingBox.getChildren().add(ratingLabel);

        HBox stars = new HBox();
        stars.setSpacing(2);
        updateRatingStars(stars, (int) Math.round(rating));
        ratingBox.getChildren().add(stars);
    }

    private VBox createCommentCard(Comment comment) {
        VBox card = new VBox(5);
        card.getStyleClass().add("comment-card");

        HBox header = new HBox(10);
        Label authorLabel = new Label(comment.getUserName() != null ? comment.getUserName() : "Anonyme");
        authorLabel.getStyleClass().add("comment-header");

        Label dateLabel = new Label("- " + comment.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        dateLabel.getStyleClass().add("comment-date");

        // Ajout du sentiment avec gestion du null
        String sentiment = comment.getSentiment();
        if (sentiment == null) {
            sentiment = "NEUTRAL"; // Valeur par défaut si le sentiment est null
        }

        Label sentimentLabel = new Label(sentiment);
        sentimentLabel.getStyleClass().add("sentiment-label");
        switch (sentiment.toUpperCase()) {
            case "POSITIVE":
                sentimentLabel.getStyleClass().add("sentiment-positive");
                break;
            case "NEGATIVE":
                sentimentLabel.getStyleClass().add("sentiment-negative");
                break;
            default:
                sentimentLabel.getStyleClass().add("sentiment-neutral");
                break;
        }

        header.getChildren().addAll(authorLabel, dateLabel, sentimentLabel);

        Label textLabel = new Label(comment.getContent());
        textLabel.getStyleClass().add("comment-text");

        card.getChildren().addAll(header, textLabel);
        return card;
    }

    @FXML
    private void toggleCommentsVisibility() {
        boolean isVisible = !commentsVBox.isVisible();
        commentsVBox.setVisible(isVisible);
        commentsVBox.setManaged(isVisible);
        updateToggleLabel(commentsVBox.getChildren().size());
    }

    private void updateToggleLabel(int commentCount) {
        if (commentCount == 0) {
            commentsToggleLabel.setText("(0 avis)");
            commentsToggleLabel.setDisable(true); // Désactiver le clic si pas de commentaires
        } else {
            commentsToggleLabel.setDisable(false);
            if (commentsVBox.isVisible()) {
                commentsToggleLabel.setText(String.format("(%d) - Masquer", commentCount));
            } else {
                commentsToggleLabel.setText(String.format("(%d) - Afficher", commentCount));
            }
        }
    }

    private void setupAddCommentSection() {
        // Crée les étoiles initiales (vides) et les ajoute au conteneur
        updateRatingStars(addRatingBox, 0);

        // Ajoute les gestionnaires de clics aux étoiles qui viennent d'être créées
        for (int i = 1; i <= 5; i++) {
            FontIcon star = (FontIcon) addRatingBox.getChildren().get(i - 1);
            final int rating = i;
            star.setOnMouseClicked(event -> {
                selectedRating = rating;
                updateRatingStars(addRatingBox, rating); // Met à jour visuellement les étoiles
            });
        }
    }

    @FXML
    private void handleSubmitComment() {
        String content = commentTextArea.getText();
        if (content == null || content.trim().isEmpty()) {
            // Idéalement, afficher une alerte à l'utilisateur
            System.err.println("Le commentaire ne peut pas être vide.");
            return;
        }
        if (selectedRating == 0) {
            System.err.println("Veuillez sélectionner une note.");
            return;
        }

        // IMPORTANT: Remplacez 1 par l'ID de l'utilisateur actuellement connecté
        // Vous aurez besoin d'un système de gestion de session pour cela.
        User currentUser = Session.getInstance().getCurrentUser();
        if (currentUser == null) {
            System.err.println("Aucun utilisateur n'est connecté. Impossible de commenter.");
            // Idéalement, afficher une alerte à l'utilisateur ici.
            return;
        }
        int currentUserId = currentUser.getId();

        commentService.addComment(currentPartner.getId(), currentUserId, content, selectedRating);

        // Rafraîchir la liste des commentaires et réinitialiser le formulaire
        refreshComments();
        commentTextArea.clear();
        selectedRating = 0;
        updateRatingStars(addRatingBox, 0);
    }

    @FXML
    private void handleContactPartner() {
        if (currentPartner == null || currentPartner.getEmail() == null || currentPartner.getEmail().isEmpty()) {
            System.err.println("Email du partenaire non disponible.");
            // Optionnel: Afficher une alerte à l'utilisateur
            return;
        }

        try {
            Desktop.getDesktop().mail(new URI("mailto:" + currentPartner.getEmail()));
        } catch (Exception e) {
            System.err.println("Impossible d'ouvrir le client de messagerie: " + e.getMessage());
            // Optionnel: Afficher une alerte à l'utilisateur
        }
    }

    private void updateRatingStars(HBox ratingContainer, int rating) {
        ratingContainer.getChildren().clear();
        for (int i = 1; i <= 5; i++) {
            FontIcon star = new FontIcon("fas-star");
            star.getStyleClass().add("star-icon");
            if (i <= rating) {
                star.getStyleClass().add("filled");
            } else {
                star.getStyleClass().add("empty");
            }
            ratingContainer.getChildren().add(star);
        }
    }

    private HBox createBadgeView(Badge badge) {
        HBox badgeView = new HBox(5);
        badgeView.setAlignment(Pos.CENTER_LEFT);

        // Icône du badge
        ImageView iconView = new ImageView();
        try {
            // Supposant que les icônes sont dans les ressources
            String iconPath = badge.getIconPath();
            if (iconPath != null && !iconPath.isEmpty()) {
                InputStream iconStream = getClass().getResourceAsStream(iconPath);
                if (iconStream != null) {
                    iconView.setImage(new Image(iconStream, 20, 20, true, true));
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'icône du badge: " + e.getMessage());
        }

        // Nom du badge
        Label nameLabel = new Label(badge.getName());
        nameLabel.getStyleClass().add("badge-name");

        badgeView.getChildren().addAll(iconView, nameLabel);
        return badgeView;
    }
}