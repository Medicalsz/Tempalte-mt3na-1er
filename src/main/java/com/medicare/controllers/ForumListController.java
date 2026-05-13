package com.medicare.controllers;

import com.medicare.models.ForumComment;
import com.medicare.models.ForumTopic;
import com.medicare.models.User;
import com.medicare.services.ForumCommentService;
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
import javafx.scene.shape.Circle;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
    private final ForumCommentService commentService = new ForumCommentService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private List<ForumTopic> allTopics = new ArrayList<>();
    private final java.util.Set<Integer> expandedTopicIds = new java.util.HashSet<>();

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
            topicsContainer.getChildren().add(buildTopicCard(topic));
        }
    }

    private VBox buildTopicCard(ForumTopic topic) {
        VBox card = new VBox();
        card.getStyleClass().add("forum-topic-card");
        if (topic.isReported()) card.getStyleClass().add("reported");
        if (topic.isHidden())   card.getStyleClass().add("hidden");

        // ── Header row: avatar + author + date on the left, status badges + actions on the right ──
        HBox headerRow = new HBox(12);
        headerRow.getStyleClass().add("forum-topic-header");
        headerRow.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = createAuthorAvatar(topic.getAuthorPhoto(), topic.getAuthorName(), 42);
        headerRow.getChildren().add(avatar);

        Label authorNameLabel = new Label(
                topic.getAuthorName() != null ? topic.getAuthorName() : "Auteur inconnu"
        );
        authorNameLabel.getStyleClass().add("forum-author-name");

        Label roleBadge = new Label(roleLabel(topic.getAuthorRoles()));
        roleBadge.getStyleClass().add(roleCssClass(topic.getAuthorRoles()));

        HBox authorLine = new HBox(8, authorNameLabel, roleBadge);
        authorLine.setAlignment(Pos.CENTER_LEFT);

        Label dateLabel = new Label(
                topic.getCreatedAt() != null ? topic.getCreatedAt().format(dateFormatter) : "-"
        );
        dateLabel.getStyleClass().add("forum-author-date");

        VBox authorBlock = new VBox(2, authorLine, dateLabel);
        authorBlock.setAlignment(Pos.CENTER_LEFT);
        headerRow.getChildren().add(authorBlock);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        headerRow.getChildren().add(spacer);

        Label typeBadge = new Label(topic.getDisplayType());
        typeBadge.getStyleClass().add(topic.isVideo() ? "badge-type-video" : "badge-type-article");
        headerRow.getChildren().add(typeBadge);

        if (topic.isReported()) {
            Label rBadge = new Label("Signale");
            rBadge.getStyleClass().add("badge-reported");
            headerRow.getChildren().add(rBadge);
        }
        if (topic.isHidden()) {
            Label hBadge = new Label("Masque");
            hBadge.getStyleClass().add("badge-hidden");
            headerRow.getChildren().add(hBadge);
        }

        HBox actions = new HBox(6);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button detailsButton = createActionButton(FontAwesomeSolid.EYE, "#1d4ed8", "btn-view", "Voir les details");
        detailsButton.setOnAction(event -> {
            logDetailOpenRequest(topic, "button");
            openForumDetail(topic.getId());
        });
        actions.getChildren().add(detailsButton);

        if (isAdmin()) {
            String reportClass = topic.isReported() ? "btn-report-active" : "btn-report";
            String reportIconColor = topic.isReported() ? "#b91c1c" : "#b45309";
            Button reportButton = createActionButton(FontAwesomeSolid.FLAG, reportIconColor, reportClass,
                    topic.isReported() ? "Retirer le signalement" : "Marquer comme signale");
            reportButton.setOnAction(event -> toggleTopicReported(topic));

            FontAwesomeSolid hideIcon = topic.isHidden() ? FontAwesomeSolid.EYE : FontAwesomeSolid.EYE_SLASH;
            String hideClass = topic.isHidden() ? "btn-hide-active" : "btn-hide";
            String hideIconColor = topic.isHidden() ? "#0f766e" : "#475569";
            Button hiddenButton = createActionButton(hideIcon, hideIconColor, hideClass,
                    topic.isHidden() ? "Afficher le sujet" : "Masquer le sujet");
            hiddenButton.setOnAction(event -> toggleTopicHidden(topic));
            actions.getChildren().addAll(reportButton, hiddenButton);
        }

        if (canManageTopic(topic)) {
            Button editButton = createActionButton(FontAwesomeSolid.PEN, "#c2410c", "btn-edit", "Modifier");
            editButton.setOnAction(event -> openForumForm(topic));

            Button deleteButton = createActionButton(FontAwesomeSolid.TRASH_ALT, "#dc2626", "btn-delete", "Supprimer");
            deleteButton.setOnAction(event -> deleteTopic(topic));
            actions.getChildren().addAll(editButton, deleteButton);
        }

        headerRow.getChildren().add(actions);
        card.getChildren().add(headerRow);

        // ── Title ──
        Label titleLabel = new Label(topic.getTitle());
        titleLabel.getStyleClass().add("forum-topic-title");
        card.getChildren().add(titleLabel);

        // ── Summary ──
        String summaryText = topic.getDisplaySummary();
        if (summaryText != null && !summaryText.isBlank()) {
            Label summaryLabel = new Label(summaryText);
            summaryLabel.getStyleClass().add("forum-topic-summary");
            card.getChildren().add(summaryLabel);
        }

        // ── Video thumbnail ──
        if (topic.isVideo()) {
            StackPane thumbnail = createVideoThumbnail(topic);
            if (thumbnail != null) {
                VBox videoWrapper = new VBox(thumbnail);
                videoWrapper.setPadding(new Insets(0, 18, 12, 18));
                card.getChildren().add(videoWrapper);
            }
        }

        // ── Tags ──
        FlowPane tagPane = buildTags(topic);
        if (!tagPane.getChildren().isEmpty()) {
            tagPane.getStyleClass().add("forum-topic-tags");
            card.getChildren().add(tagPane);
        }

        // ── Divider ──
        Region divider = new Region();
        divider.getStyleClass().add("forum-topic-divider");
        card.getChildren().add(divider);

        // ── Footer: comment toggle + inline comments (expandable) ──
        VBox commentsBlock = new VBox(10);
        commentsBlock.getStyleClass().add("forum-comments-block");
        commentsBlock.setVisible(false);
        commentsBlock.setManaged(false);

        boolean alreadyExpanded = expandedTopicIds.contains(topic.getId());

        Button commentToggle = new Button();
        commentToggle.getStyleClass().add("forum-comments-toggle");
        FontIcon commentIcon = new FontIcon(FontAwesomeSolid.COMMENTS);
        commentIcon.setIconSize(13);
        commentIcon.setIconColor(Color.web("#2563eb"));
        commentToggle.setGraphic(commentIcon);
        commentToggle.setText(commentToggleLabel(topic, alreadyExpanded));
        commentToggle.setOnAction(event -> {
            boolean willExpand = !commentsBlock.isVisible();
            if (willExpand) {
                expandedTopicIds.add(topic.getId());
                populateCommentsBlock(commentsBlock, topic.getId());
            } else {
                expandedTopicIds.remove(topic.getId());
            }
            commentsBlock.setVisible(willExpand);
            commentsBlock.setManaged(willExpand);
            commentToggle.setText(commentToggleLabel(topic, willExpand));
        });

        HBox footerRow = new HBox(commentToggle);
        footerRow.getStyleClass().add("forum-topic-footer");
        footerRow.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().addAll(footerRow, commentsBlock);

        if (alreadyExpanded) {
            populateCommentsBlock(commentsBlock, topic.getId());
            commentsBlock.setVisible(true);
            commentsBlock.setManaged(true);
        }

        return card;
    }

    private String commentToggleLabel(ForumTopic topic, boolean expanded) {
        int count = topic.getCommentCount();
        String word = count > 1 ? " commentaires" : " commentaire";
        return (expanded ? "Masquer " : "Afficher ") + count + word;
    }

    private void populateCommentsBlock(VBox container, int topicId) {
        container.getChildren().clear();
        try {
            List<ForumComment> comments = commentService.findByTopicId(topicId, isAdmin());
            if (comments.isEmpty()) {
                Label empty = new Label("Aucun commentaire pour ce sujet.");
                empty.getStyleClass().add("forum-comments-empty");
                container.getChildren().add(empty);
                return;
            }
            for (ForumComment comment : comments) {
                container.getChildren().add(buildCommentRow(comment));
            }
        } catch (Exception ex) {
            Label err = new Label("Impossible de charger les commentaires.");
            err.getStyleClass().add("forum-comments-empty");
            container.getChildren().add(err);
            ex.printStackTrace();
        }
    }

    private HBox buildCommentRow(ForumComment comment) {
        StackPane avatar = createAuthorAvatar(comment.getAuthorPhoto(), comment.getAuthorName(), 32);

        Label name = new Label(comment.getAuthorName() != null ? comment.getAuthorName() : "Anonyme");
        name.getStyleClass().add("forum-comment-author");

        Label date = new Label(comment.getCreatedAt() != null ? comment.getCreatedAt().format(dateFormatter) : "");
        date.getStyleClass().add("forum-comment-date");

        HBox topLine = new HBox(8, name, date);
        topLine.setAlignment(Pos.CENTER_LEFT);

        Label body = new Label(comment.getContent() != null ? comment.getContent() : "");
        body.setWrapText(true);
        body.getStyleClass().add("forum-comment-body");

        VBox textBox = new VBox(4, topLine, body);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox row = new HBox(10, avatar, textBox);
        row.getStyleClass().add("forum-comment-row");
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private StackPane createAuthorAvatar(String photoPath, String authorName, double size) {
        Circle background = new Circle(size / 2.0);
        background.setFill(Color.web("#e0e7ff"));
        background.setStroke(Color.web("#c7d2fe"));
        background.setStrokeWidth(1.0);

        StackPane avatar = new StackPane(background);
        avatar.setMinSize(size, size);
        avatar.setPrefSize(size, size);
        avatar.setMaxSize(size, size);

        Image image = tryLoadAuthorImage(photoPath);
        if (image != null) {
            ImageView view = new ImageView(image);
            view.setFitWidth(size);
            view.setFitHeight(size);
            view.setPreserveRatio(false);
            view.setSmooth(true);
            Circle clip = new Circle(size / 2.0, size / 2.0, size / 2.0);
            view.setClip(clip);
            avatar.getChildren().add(view);
        } else {
            Label initials = new Label(initialsFor(authorName));
            initials.setStyle("-fx-font-size: " + Math.round(size * 0.36)
                    + "px; -fx-font-weight: bold; -fx-text-fill: #4338ca;");
            avatar.getChildren().add(initials);
        }
        return avatar;
    }

    private Image tryLoadAuthorImage(String photoPath) {
        if (photoPath == null || photoPath.isBlank()) {
            return null;
        }
        try {
            String source = photoPath.startsWith("file:/") || photoPath.startsWith("http")
                    ? photoPath
                    : java.nio.file.Path.of(photoPath).toUri().toString();
            Image image = new Image(source, true);
            if (image.isError()) {
                return null;
            }
            return image;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String initialsFor(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length && sb.length() < 2; i++) {
            if (!parts[i].isEmpty()) sb.append(Character.toUpperCase(parts[i].charAt(0)));
        }
        return sb.length() == 0 ? "?" : sb.toString();
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
            label.getStyleClass().add("badge-tag");
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

    private String roleCssClass(String roles) {
        if (roles == null) return "badge-role-user";
        if (roles.contains("ROLE_ADMIN")) return "badge-role-admin";
        if (roles.contains("ROLE_MEDECIN")) return "badge-role-medecin";
        return "badge-role-user";
    }

    private Button createActionButton(FontAwesomeSolid iconType, String iconColor, String btnStyleClass, String tooltip) {
        Button button = new Button();
        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(13);
        icon.setIconColor(Color.web(iconColor));
        button.setGraphic(icon);
        button.getStyleClass().addAll("topic-action-btn", btnStyleClass);
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
