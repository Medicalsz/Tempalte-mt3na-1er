package com.medicare.controllers;

import com.medicare.models.ForumTopic;
import com.medicare.models.User;
import com.medicare.services.ForumService;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ForumListController extends ForumController {

    @FXML private VBox forumListContent;
    @FXML private HBox forumHeaderBox;
    @FXML private HBox forumSearchCard;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private CheckBox showHiddenCheckBox;
    @FXML private Label headerStatsLabel;
    @FXML private Label errorLabel;
    @FXML private Label emptyLabel;
    @FXML private Button addButton;
    @FXML private Button moderationButton;
    @FXML private VBox topicsContainer;

    private final ForumService forumService = new ForumService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private List<ForumTopic> allTopics = new ArrayList<>();

    @FXML
    private void initialize() {
        typeFilterCombo.getItems().setAll("Tous", "Article", "Video");
        typeFilterCombo.setValue("Tous");
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        typeFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        showHiddenCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> loadTopics());
        configureHeaderDesign();
        playHeaderIntro();
        loadTopics();
    }

    @Override
    protected void onForumContextReady() {
        User user = resolveCurrentUser();
        boolean admin = isAdmin();
        addButton.setDisable(user == null);
        addButton.setVisible(true);
        moderationButton.setVisible(admin);
        moderationButton.setManaged(admin);
        showHiddenCheckBox.setVisible(admin);
        showHiddenCheckBox.setManaged(admin);
        if (!admin) {
            showHiddenCheckBox.setSelected(false);
        }
        loadTopics();
    }

    @FXML
    private void onAddTopicClick() {
        if (resolveCurrentUser() == null) {
            errorLabel.setText("Connectez-vous pour publier un sujet.");
            return;
        }
        errorLabel.setText("");
        openForumForm(null);
    }

    @FXML
    private void onModerationClick() {
        if (!isAdmin()) {
            showError("Cet espace est reserve aux administrateurs.", null);
            return;
        }
        openForumModeration();
    }

    private void loadTopics() {
        try {
            allTopics = forumService.findAll(isAdmin() && showHiddenCheckBox.isSelected());
            errorLabel.setText("");
            applyFilters();
        } catch (Exception e) {
            errorLabel.setText("Impossible de charger les sujets du forum.");
            e.printStackTrace();
        }
    }

    private void configureHeaderDesign() {
        addButton.setGraphic(createButtonIcon(FontAwesomeSolid.PLUS));
        moderationButton.setGraphic(createButtonIcon(FontAwesomeSolid.FLAG, Color.web("#C2410C")));
        installButtonHover(addButton);
        installButtonHover(moderationButton);
        installSoftHover(forumSearchCard);
    }

    private FontIcon createButtonIcon(FontAwesomeSolid iconType) {
        return createButtonIcon(iconType, Color.WHITE);
    }

    private FontIcon createButtonIcon(FontAwesomeSolid iconType, Color color) {
        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(13);
        icon.setIconColor(color);
        return icon;
    }

    private void playHeaderIntro() {
        fadeIn(forumHeaderBox, 0);
        fadeIn(forumSearchCard, 90);
    }

    private void fadeIn(Node node, int delayMillis) {
        if (node == null) {
            return;
        }
        node.setOpacity(0);
        node.setTranslateY(8);
        FadeTransition fade = new FadeTransition(Duration.millis(260), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setDelay(Duration.millis(delayMillis));
        fade.play();

        javafx.animation.TranslateTransition slide = new javafx.animation.TranslateTransition(Duration.millis(260), node);
        slide.setFromY(8);
        slide.setToY(0);
        slide.setDelay(Duration.millis(delayMillis));
        slide.play();
    }

    private void installButtonHover(Node node) {
        if (node == null) {
            return;
        }
        node.setOnMouseEntered(event -> animateScale(node, 1.04));
        node.setOnMouseExited(event -> animateScale(node, 1.0));
    }

    private void installSoftHover(Node node) {
        if (node == null) {
            return;
        }
        node.setOnMouseEntered(event -> animateScale(node, 1.006));
        node.setOnMouseExited(event -> animateScale(node, 1.0));
    }

    private void animateScale(Node node, double scale) {
        ScaleTransition transition = new ScaleTransition(Duration.millis(130), node);
        transition.setToX(scale);
        transition.setToY(scale);
        transition.play();
    }

    private void applyFilters() {
        String query = searchField == null ? "" : searchField.getText();
        String typeFilter = typeFilterCombo == null ? "Tous" : typeFilterCombo.getValue();

        List<ForumTopic> filtered = allTopics.stream()
                .filter(topic -> topic.matchesSearch(query))
                .filter(topic -> "Tous".equalsIgnoreCase(typeFilter) || topic.getDisplayType().equalsIgnoreCase(typeFilter))
                .toList();

        renderTopics(filtered);
    }

    private void renderTopics(List<ForumTopic> topics) {
        if (topicsContainer == null) {
            return;
        }

        topicsContainer.getChildren().clear();
        long hiddenCount = topics.stream().filter(ForumTopic::isHidden).count();
        if (isAdmin() && showHiddenCheckBox.isSelected()) {
            headerStatsLabel.setText(
                    topics.size() + (topics.size() > 1 ? " sujets charges" : " sujet charge") +
                            (hiddenCount > 0 ? " - " + hiddenCount + " masque(s)" : "")
            );
        } else {
            headerStatsLabel.setText(topics.size() + (topics.size() > 1 ? " sujets visibles" : " sujet visible"));
        }
        emptyLabel.setVisible(topics.isEmpty());
        emptyLabel.setManaged(topics.isEmpty());

        if (topics.isEmpty()) {
            return;
        }

        for (ForumTopic topic : topics) {
            VBox card = new VBox(12);
            card.setPadding(new Insets(18));
            String borderColor = topic.isHidden() ? "#94a3b8" : (topic.isReported() ? "#f59e0b" : "transparent");
            card.setStyle("-fx-background-color: white; -fx-background-radius: 14; " +
                    "-fx-border-color: " + borderColor + "; -fx-border-radius: 14; -fx-border-width: " +
                    (topic.hasModerationFlag() ? "1.2" : "0") + "; " +
                    "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.08), 14, 0, 0, 3);");

            HBox topRow = new HBox(12);
            topRow.setAlignment(Pos.CENTER_LEFT);

            VBox titleBox = new VBox(6);
            HBox.setHgrow(titleBox, Priority.ALWAYS);

            Label titleLabel = new Label(topic.getTitle());
            titleLabel.setWrapText(true);
            titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

            HBox metaRow = new HBox(8);
            metaRow.setAlignment(Pos.CENTER_LEFT);

            Label authorBadge = new Label(roleLabel(topic.getAuthorRoles()));
            authorBadge.setStyle("-fx-background-color: " + roleColor(topic.getAuthorRoles()) + "; " +
                    "-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; " +
                    "-fx-background-radius: 999; -fx-padding: 3 10;");

            Label metaLabel = new Label(
                    (topic.getAuthorName() != null ? topic.getAuthorName() : "Auteur inconnu") +
                            " - " + (topic.getCreatedAt() != null ? topic.getCreatedAt().format(dateFormatter) : "-") +
                            " - " + topic.getCommentCount() + (topic.getCommentCount() > 1 ? " commentaires" : " commentaire")
            );
            metaLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

            Label typeBadge = new Label(topic.getDisplayType());
            typeBadge.setStyle("-fx-background-color: " + (topic.isVideo() ? "#fed7aa" : "#dbeafe") + "; " +
                    "-fx-text-fill: " + (topic.isVideo() ? "#c2410c" : "#1d4ed8") + "; " +
                    "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 999; -fx-padding: 3 10;");

            metaRow.getChildren().addAll(authorBadge, metaLabel, typeBadge);
            titleBox.getChildren().addAll(titleLabel, metaRow);

            FlowPane moderationPane = buildModerationBadges(topic);
            if (!moderationPane.getChildren().isEmpty()) {
                titleBox.getChildren().add(moderationPane);
            }

            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_RIGHT);

            Button detailsButton = createActionButton(FontAwesomeSolid.EYE, "#1d4ed8", "#dbeafe", "Voir les details");
            detailsButton.setOnAction(event -> {
                logDetailOpenRequest(topic, "button");
                openForumDetail(topic.getId());
            });
            actions.getChildren().add(detailsButton);

            if (isAdmin()) {
                Button reportButton = createActionButton(
                        FontAwesomeSolid.FLAG,
                        topic.isReported() ? "#b91c1c" : "#b45309",
                        topic.isReported() ? "#fee2e2" : "#fef3c7",
                        topic.isReported() ? "Retirer le signalement" : "Marquer comme signale"
                );
                reportButton.setOnAction(event -> toggleTopicReported(topic));

                Button hiddenButton = createActionButton(
                        topic.isHidden() ? FontAwesomeSolid.EYE : FontAwesomeSolid.EYE_SLASH,
                        topic.isHidden() ? "#0f766e" : "#475569",
                        topic.isHidden() ? "#ccfbf1" : "#e2e8f0",
                        topic.isHidden() ? "Afficher le sujet" : "Masquer le sujet"
                );
                hiddenButton.setOnAction(event -> toggleTopicHidden(topic));
                actions.getChildren().addAll(reportButton, hiddenButton);
            }

            if (canManageTopic(topic)) {
                Button editButton = createActionButton(FontAwesomeSolid.PEN, "#c2410c", "#ffedd5", "Modifier");
                editButton.setOnAction(event -> openForumForm(topic));

                Button deleteButton = createActionButton(FontAwesomeSolid.TRASH_ALT, "#dc2626", "#fee2e2", "Supprimer");
                deleteButton.setOnAction(event -> deleteTopic(topic));
                actions.getChildren().addAll(editButton, deleteButton);
            }

            topRow.getChildren().addAll(titleBox, actions);

            Label summaryLabel = new Label(topic.getDisplaySummary());
            summaryLabel.setWrapText(true);
            summaryLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155; -fx-line-spacing: 2;");

            VBox optionalBlock = new VBox(8);
            if (topic.isVideo()) {
                StackPane thumbnail = createVideoThumbnail(topic);
                if (thumbnail != null) {
                    optionalBlock.getChildren().add(thumbnail);
                }
            }

            FlowPane tagPane = buildTags(topic);

            card.getChildren().addAll(topRow, summaryLabel);
            if (!optionalBlock.getChildren().isEmpty()) {
                card.getChildren().add(optionalBlock);
            }
            if (!tagPane.getChildren().isEmpty()) {
                card.getChildren().add(tagPane);
            }

            topicsContainer.getChildren().add(card);
        }
    }

    private FlowPane buildModerationBadges(ForumTopic topic) {
        FlowPane pane = new FlowPane();
        pane.setHgap(8);
        pane.setVgap(8);

        if (topic.isReported()) {
            pane.getChildren().add(createBadge("Signale", "#fef3c7", "#b45309"));
        }
        if (topic.isHidden()) {
            pane.getChildren().add(createBadge("Masque", "#e2e8f0", "#475569"));
        }

        return pane;
    }

    private FlowPane buildTags(ForumTopic topic) {
        FlowPane pane = new FlowPane();
        pane.setHgap(8);
        pane.setVgap(8);

        String tagsDisplay = topic.getTagsDisplay();
        if (tagsDisplay.isBlank()) {
            return pane;
        }

        String[] tags = tagsDisplay.split(",");
        for (String tag : tags) {
            String clean = tag.trim();
            if (clean.isEmpty()) {
                continue;
            }
            Label label = new Label("#" + clean);
            label.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; " +
                    "-fx-font-size: 11px; -fx-background-radius: 999; -fx-padding: 4 10;");
            pane.getChildren().add(label);
        }
        return pane;
    }

    private StackPane createVideoThumbnail(ForumTopic topic) {
        String videoId = extractYouTubeVideoId(topic.getVideoUrl());
        if (videoId == null) {
            return null;
        }

        ImageView imageView = new ImageView(new Image("https://img.youtube.com/vi/" + videoId + "/0.jpg", true));
        imageView.setFitWidth(360);
        imageView.setFitHeight(205);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);

        Label playButton = new Label("Play");
        playButton.setStyle("-fx-background-color: rgba(37, 99, 235, 0.92); -fx-text-fill: white; " +
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 999; -fx-padding: 9 18;");

        Label hint = new Label("Voir la video");
        hint.setStyle("-fx-background-color: rgba(15, 23, 42, 0.76); -fx-text-fill: white; " +
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 999; -fx-padding: 5 10;");
        StackPane.setAlignment(hint, Pos.BOTTOM_LEFT);
        StackPane.setMargin(hint, new Insets(0, 0, 12, 12));

        StackPane thumbnail = new StackPane(imageView, playButton, hint);
        thumbnail.setMaxWidth(360);
        thumbnail.setPrefSize(360, 205);
        thumbnail.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 14; " +
                "-fx-border-color: #bfdbfe; -fx-border-radius: 14; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.14), 12, 0, 0, 3);");
        thumbnail.setOnMouseClicked(event -> {
            logDetailOpenRequest(topic, "video-thumbnail");
            openForumDetail(topic.getId());
        });
        return thumbnail;
    }

    private void logDetailOpenRequest(ForumTopic topic, String source) {
        int topicId = topic != null ? topic.getId() : -1;
        System.out.println("[ForumListController] Ouverture detail demandee depuis " + source +
                " topicId=" + topicId +
                " fxml=forum-detail-view.fxml" +
                " type=" + (topic != null ? topic.getType() : "-") +
                " videoUrl=" + (topic != null ? topic.getVideoUrl() : null));
    }

    private String extractYouTubeVideoId(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }

        String value = rawUrl.trim();
        int watchIndex = value.indexOf("watch?v=");
        if (watchIndex >= 0) {
            return cleanYouTubeId(value.substring(watchIndex + "watch?v=".length()));
        }

        int shortIndex = value.indexOf("youtu.be/");
        if (shortIndex >= 0) {
            return cleanYouTubeId(value.substring(shortIndex + "youtu.be/".length()));
        }

        int embedIndex = value.indexOf("/embed/");
        if (embedIndex >= 0) {
            return cleanYouTubeId(value.substring(embedIndex + "/embed/".length()));
        }

        return null;
    }

    private String cleanYouTubeId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String cleaned = value;
        int queryIndex = cleaned.indexOf('&');
        if (queryIndex >= 0) {
            cleaned = cleaned.substring(0, queryIndex);
        }
        int slashIndex = cleaned.indexOf('/');
        if (slashIndex >= 0) {
            cleaned = cleaned.substring(0, slashIndex);
        }
        int questionIndex = cleaned.indexOf('?');
        if (questionIndex >= 0) {
            cleaned = cleaned.substring(0, questionIndex);
        }
        cleaned = cleaned.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private Label createBadge(String text, String backgroundColor, String textColor) {
        Label label = new Label(text);
        label.setStyle("-fx-background-color: " + backgroundColor + "; -fx-text-fill: " + textColor + "; " +
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 999; -fx-padding: 4 10;");
        return label;
    }

    private Button createActionButton(FontAwesomeSolid iconType, String iconColor, String backgroundColor, String tooltip) {
        Button button = new Button();
        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(13);
        icon.setIconColor(Color.web(iconColor));
        button.setGraphic(icon);
        button.setStyle("-fx-background-color: " + backgroundColor + "; -fx-padding: 7; " +
                "-fx-background-radius: 8; -fx-cursor: hand;");
        button.setTooltip(new Tooltip(tooltip));
        return button;
    }

    private void toggleTopicReported(ForumTopic topic) {
        User user = resolveCurrentUser();
        if (user == null || !user.hasRole("ROLE_ADMIN")) {
            showError("Seul un administrateur peut modifier le signalement d'un sujet.", null);
            return;
        }

        boolean newState = !topic.isReported();
        String action = newState ? "signaler" : "retirer le signalement de";
        if (!confirm("Moderation sujet", "Voulez-vous " + action + " ce sujet ?")) {
            return;
        }

        try {
            forumService.setTopicReported(topic.getId(), newState, newState ? user.getId() : null);
            loadTopics();
        } catch (Exception e) {
            showError("Impossible de mettre a jour le signalement du sujet.", e);
        }
    }

    private void toggleTopicHidden(ForumTopic topic) {
        User user = resolveCurrentUser();
        if (user == null || !user.hasRole("ROLE_ADMIN")) {
            showError("Seul un administrateur peut masquer ou afficher un sujet.", null);
            return;
        }

        boolean newState = !topic.isHidden();
        String action = newState ? "masquer" : "rendre visible";
        if (!confirm("Moderation sujet", "Voulez-vous " + action + " ce sujet ?")) {
            return;
        }

        try {
            forumService.setTopicHidden(topic.getId(), newState);
            loadTopics();
        } catch (Exception e) {
            showError("Impossible de mettre a jour la visibilite du sujet.", e);
        }
    }

    private void deleteTopic(ForumTopic topic) {
        if (!confirm("Supprimer le sujet", "Voulez-vous vraiment supprimer ce sujet et ses commentaires ?")) {
            return;
        }

        try {
            forumService.deleteTopic(topic.getId());
            showInfo("Forum", "Le sujet a bien ete supprime.");
            loadTopics();
        } catch (Exception e) {
            showError("Impossible de supprimer le sujet.", e);
        }
    }
}
