package com.medicare.controllers;

import com.medicare.models.ChatAssistantRecommendation;
import com.medicare.models.ChatAssistantResponse;
import com.medicare.models.ContentModerationResult;
import com.medicare.models.ForumComment;
import com.medicare.models.ForumTopic;
import com.medicare.models.User;
import com.medicare.services.ChatAssistantService;
import com.medicare.services.CommentService;
import com.medicare.services.CommentReactionService;
import com.medicare.services.ContentModerationService;
import com.medicare.services.ForumService;
import com.medicare.services.ForumRecommendationService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ForumDetailController extends ForumController {

    @FXML private ScrollPane detailScrollPane;
    @FXML private Label titleLabel;
    @FXML private Label metaLabel;
    @FXML private Label typeBadgeLabel;
    @FXML private Label reportedStatusLabel;
    @FXML private Label hiddenStatusLabel;
    @FXML private VBox summaryBox;
    @FXML private Label summaryLabel;
    @FXML private Label contentLabel;
    @FXML private VBox videoBox;
    @FXML private Label videoFallbackLabel;
    @FXML private WebView videoWebView;
    @FXML private FlowPane tagsPane;
    @FXML private Label emptyRecommendationsLabel;
    @FXML private VBox recommendationsContainer;
    @FXML private Label commentsStatsLabel;
    @FXML private Label emptyCommentsLabel;
    @FXML private VBox commentsContainer;
    @FXML private Button assistantToggleButton;
    @FXML private ScrollPane assistantMessagesScrollPane;
    @FXML private VBox assistantMessagesContainer;
    @FXML private TextArea assistantInputArea;
    @FXML private Label assistantStatusLabel;
    @FXML private Button assistantSendButton;
    @FXML private TextArea newCommentArea;
    @FXML private Label commentErrorLabel;
    @FXML private Button addCommentButton;
    @FXML private Button editButton;
    @FXML private Button toggleReportedButton;
    @FXML private Button toggleHiddenButton;
    @FXML private Button deleteButton;

    private final ForumService forumService = new ForumService();
    private final CommentService commentService = new CommentService();
    private final CommentReactionService commentReactionService = new CommentReactionService();
    private final ChatAssistantService chatAssistantService = new ChatAssistantService();
    private final ContentModerationService contentModerationService = new ContentModerationService();
    private final ForumRecommendationService forumRecommendationService = new ForumRecommendationService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int topicId;
    private ForumTopic currentTopic;
    private List<ForumComment> currentComments = List.of();
    private List<ForumTopic> currentRelatedTopics = List.of();
    private boolean assistantRequestInProgress;
    private int assistantInitializedTopicId = -1;
    private int assistantWarmupTopicId = -1;

    @FXML
    private void initialize() {
        assistantSendButton.setDisable(true);
        assistantStatusLabel.setText("Chargez un sujet pour discuter avec l'assistant.");
        assistantMessagesContainer.getChildren().clear();
        appendAssistantBubble(
                "Je peux vous aider a resumer un sujet, suggerer des discussions proches et fournir des informations generales a partir du contenu du forum."
        );
    }

    public void setTopicId(int topicId) {
        System.out.println("[ForumDetailController] setTopicId topicId=" + topicId);
        if (this.topicId != topicId) {
            assistantInitializedTopicId = -1;
            assistantWarmupTopicId = -1;
            assistantRequestInProgress = false;
            currentComments = List.of();
            currentRelatedTopics = List.of();
        }
        this.topicId = topicId;
        loadTopicAndComments();
    }

    @Override
    protected void onForumContextReady() {
        User user = resolveCurrentUser();
        addCommentButton.setDisable(user == null);
        if (topicId > 0) {
            loadTopicAndComments();
        }
    }

    @FXML
    private void onAssistantToggleClick() {
        Platform.runLater(() -> {
            if (detailScrollPane != null) {
                detailScrollPane.setVvalue(0.62);
            }
            assistantInputArea.requestFocus();
            assistantInputArea.positionCaret(assistantInputArea.getText().length());
        });
    }

    @FXML
    private void onAssistantSendClick() {
        if (assistantRequestInProgress) {
            return;
        }
        if (currentTopic == null) {
            assistantStatusLabel.setText("Le sujet n'est pas encore charge.");
            return;
        }

        String message = assistantInputArea.getText() != null ? assistantInputArea.getText().trim() : "";
        if (message.isEmpty()) {
            assistantStatusLabel.setText("Ecrivez une question ou une demande de resume.");
            assistantInputArea.requestFocus();
            return;
        }

        appendUserBubble(message);
        assistantInputArea.clear();
        setAssistantLoading(true, "Assistant en train d'analyser le sujet...");

        ForumTopic topicSnapshot = currentTopic;
        List<ForumComment> commentsSnapshot = new ArrayList<>(currentComments);
        List<ForumTopic> relatedTopicsSnapshot = new ArrayList<>(currentRelatedTopics);

        Task<ChatAssistantResponse> task = new Task<>() {
            @Override
            protected ChatAssistantResponse call() {
                return chatAssistantService.askAssistant(message, topicSnapshot, commentsSnapshot, relatedTopicsSnapshot);
            }
        };

        task.setOnSucceeded(event -> {
            setAssistantLoading(false, "Assistant pret.");
            ChatAssistantResponse response = task.getValue();
            appendAssistantBubble(response.getReply(), response.getRecommendations());
        });

        task.setOnFailed(event -> {
            setAssistantLoading(false, "Assistant indisponible.");
            appendAssistantError(task.getException());
        });

        Thread assistantThread = new Thread(task, "forum-chat-assistant");
        assistantThread.setDaemon(true);
        assistantThread.start();
    }

    @FXML
    private void onBackClick() {
        openForumList();
    }

    @FXML
    private void onEditClick() {
        if (currentTopic != null) {
            openForumForm(currentTopic);
        }
    }

    @FXML
    private void onToggleReportedClick() {
        User user = resolveCurrentUser();
        if (currentTopic == null || user == null || !user.hasRole("ROLE_ADMIN")) {
            showError("Seul un administrateur peut modifier le signalement du sujet.", null);
            return;
        }

        boolean newState = !currentTopic.isReported();
        String action = newState ? "signaler" : "retirer le signalement de";
        if (!confirm("Moderation sujet", "Voulez-vous " + action + " ce sujet ?")) {
            return;
        }

        try {
            forumService.setTopicReported(currentTopic.getId(), newState, newState ? user.getId() : null);
            loadTopicAndComments();
        } catch (Exception e) {
            showError("Impossible de mettre a jour le signalement du sujet.", e);
        }
    }

    @FXML
    private void onToggleHiddenClick() {
        User user = resolveCurrentUser();
        if (currentTopic == null || user == null || !user.hasRole("ROLE_ADMIN")) {
            showError("Seul un administrateur peut masquer ou afficher un sujet.", null);
            return;
        }

        boolean newState = !currentTopic.isHidden();
        String action = newState ? "masquer" : "rendre visible";
        if (!confirm("Moderation sujet", "Voulez-vous " + action + " ce sujet ?")) {
            return;
        }

        try {
            forumService.setTopicHidden(currentTopic.getId(), newState);
            loadTopicAndComments();
        } catch (Exception e) {
            showError("Impossible de mettre a jour la visibilite du sujet.", e);
        }
    }

    @FXML
    private void onDeleteClick() {
        if (currentTopic == null) {
            return;
        }
        if (!confirm("Supprimer le sujet", "Voulez-vous vraiment supprimer ce sujet et tous ses commentaires ?")) {
            return;
        }

        try {
            forumService.deleteTopic(currentTopic.getId());
            showInfo("Forum", "Le sujet a bien ete supprime.");
            openForumList();
        } catch (Exception e) {
            showError("Impossible de supprimer le sujet.", e);
        }
    }

    @FXML
    private void onAddCommentClick() {
        User user = resolveCurrentUser();
        if (user == null) {
            commentErrorLabel.setText("Connectez-vous pour commenter.");
            return;
        }
        if (currentTopic == null) {
            commentErrorLabel.setText("Sujet introuvable.");
            return;
        }
        if (newCommentArea.getText().isBlank()) {
            commentErrorLabel.setText("Le commentaire ne peut pas etre vide.");
            return;
        }

        ForumComment comment = new ForumComment();
        comment.setAuthorId(user.getId());
        comment.setTopicId(currentTopic.getId());
        comment.setContent(newCommentArea.getText());

        ContentModerationResult moderationResult = contentModerationService.moderate(comment.getContent());
        if (moderationResult.hasToxicContent()) {
            commentErrorLabel.setText(moderationResult.getMessage());
            return;
        }
        applyAutomaticFlag(comment, moderationResult);

        try {
            commentService.addComment(comment);
            newCommentArea.clear();
            commentErrorLabel.setText("");
            loadComments();
        } catch (Exception e) {
            commentErrorLabel.setText("Impossible d'ajouter le commentaire.");
            e.printStackTrace();
        }
    }

    private void loadTopicAndComments() {
        if (topicId <= 0) {
            return;
        }

        try {
            System.out.println("[ForumDetailController] Chargement detail topicId=" + topicId +
                    " fxml=forum-detail-view.fxml");
            currentTopic = forumService.findById(topicId, isAdmin());
            System.out.println("[ForumDetailController] ForumService.findById(" + topicId + ") retourne " +
                    (currentTopic != null ? "topic id=" + currentTopic.getId() + " type=" + currentTopic.getType() : "null"));
            if (currentTopic == null) {
                showError("Le sujet demande est introuvable.", null);
                openForumList();
                return;
            }
            currentRelatedTopics = forumRecommendationService.recommendForTopic(currentTopic.getId(), 5);
            populateTopic();
            populateRecommendations();
            loadComments();
            initializeAssistantForCurrentTopic();
        } catch (Exception e) {
            System.err.println("[ForumDetailController] Erreur detail topicId=" + topicId +
                    " message=" + e.getMessage());
            e.printStackTrace();
            showError("Impossible de charger le detail du sujet.", e);
        }
    }

    private void populateTopic() {
        titleLabel.setText(currentTopic.getTitle());
        metaLabel.setText(
                (currentTopic.getAuthorName() != null ? currentTopic.getAuthorName() : "Auteur inconnu") +
                        " - " + roleLabel(currentTopic.getAuthorRoles()) +
                        " - " + (currentTopic.getCreatedAt() != null ? currentTopic.getCreatedAt().format(dateFormatter) : "-")
        );

        typeBadgeLabel.setText(currentTopic.getDisplayType());
        typeBadgeLabel.setStyle("-fx-background-color: " + (currentTopic.isVideo() ? "#fed7aa" : "#dbeafe") + "; " +
                "-fx-text-fill: " + (currentTopic.isVideo() ? "#c2410c" : "#1d4ed8") + "; " +
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 999; -fx-padding: 4 10;");

        updateStatusBadge(reportedStatusLabel, currentTopic.isReported(), "Sujet signale", "#fef3c7", "#b45309");
        updateStatusBadge(hiddenStatusLabel, currentTopic.isHidden(), "Sujet masque", "#e2e8f0", "#475569");

        summaryBox.setVisible(currentTopic.getSummary() != null && !currentTopic.getSummary().isBlank());
        summaryBox.setManaged(summaryBox.isVisible());
        summaryLabel.setText(currentTopic.getSummary());

        contentLabel.setText(currentTopic.getContent());

        renderVideoBlock();

        tagsPane.getChildren().clear();
        String tagsDisplay = currentTopic.getTagsDisplay();
        if (!tagsDisplay.isBlank()) {
            for (String tag : tagsDisplay.split(",")) {
                String clean = tag.trim();
                if (clean.isEmpty()) {
                    continue;
                }
                Label tagLabel = new Label("#" + clean);
                tagLabel.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; " +
                        "-fx-font-size: 11px; -fx-background-radius: 999; -fx-padding: 4 10;");
                tagsPane.getChildren().add(tagLabel);
            }
        }

        boolean canManage = canManageTopic(currentTopic);
        editButton.setVisible(canManage);
        editButton.setManaged(canManage);
        deleteButton.setVisible(canManage);
        deleteButton.setManaged(canManage);

        boolean admin = isAdmin();
        toggleReportedButton.setVisible(admin);
        toggleReportedButton.setManaged(admin);
        toggleHiddenButton.setVisible(admin);
        toggleHiddenButton.setManaged(admin);
        if (admin) {
            toggleReportedButton.setText(currentTopic.isReported() ? "Retirer signalement" : "Signaler");
            toggleHiddenButton.setText(currentTopic.isHidden() ? "Afficher le sujet" : "Masquer le sujet");
        }
    }

    private void populateRecommendations() {
        recommendationsContainer.getChildren().clear();
        boolean empty = currentRelatedTopics == null || currentRelatedTopics.isEmpty();
        emptyRecommendationsLabel.setVisible(empty);
        emptyRecommendationsLabel.setManaged(empty);
        if (empty) {
            return;
        }

        for (ForumTopic recommendation : currentRelatedTopics) {
            recommendationsContainer.getChildren().add(createRecommendationCard(recommendation));
        }
    }

    private VBox createRecommendationCard(ForumTopic recommendation) {
        double score = forumRecommendationService.calculateScore(currentTopic, recommendation);

        VBox card = new VBox(9);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14; " +
                "-fx-border-color: #dbeafe; -fx-border-radius: 14; " +
                "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.05), 10, 0, 0, 2);");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label typeBadge = new Label(recommendation.getDisplayType());
        typeBadge.setStyle("-fx-background-color: " + (recommendation.isVideo() ? "#ffedd5" : "#dbeafe") + "; " +
                "-fx-text-fill: " + (recommendation.isVideo() ? "#c2410c" : "#1d4ed8") + "; " +
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 999; -fx-padding: 4 10;");

        Label relevanceBadge = new Label(relevanceLabel(score));
        relevanceBadge.setStyle("-fx-background-color: #ede9fe; -fx-text-fill: #6d28d9; " +
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 999; -fx-padding: 4 10;");

        Label meta = new Label(
                (recommendation.getCreatedAt() != null ? recommendation.getCreatedAt().format(dateFormatter) : "-") +
                        " - " + recommendation.getCommentCount() +
                        (recommendation.getCommentCount() > 1 ? " commentaires" : " commentaire")
        );
        meta.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button viewButton = new Button("Voir");
        viewButton.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; " +
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 10; " +
                "-fx-cursor: hand; -fx-padding: 8 14;");
        viewButton.setOnAction(event -> openForumDetail(recommendation.getId()));

        header.getChildren().addAll(typeBadge, relevanceBadge, meta, spacer, viewButton);

        Label title = new Label(recommendation.getTitle());
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #172554;");

        Label author = new Label("Par " + (recommendation.getAuthorName() != null ? recommendation.getAuthorName() : "Auteur inconnu"));
        author.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

        FlowPane recommendationTags = new FlowPane(7, 7);
        String tagsDisplay = recommendation.getTagsDisplay();
        if (!tagsDisplay.isBlank()) {
            for (String tag : tagsDisplay.split(",")) {
                String clean = tag.trim();
                if (!clean.isEmpty()) {
                    Label tagBadge = new Label("#" + clean);
                    tagBadge.setStyle("-fx-background-color: #eff6ff; -fx-text-fill: #1d4ed8; " +
                            "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 999; -fx-padding: 4 9;");
                    recommendationTags.getChildren().add(tagBadge);
                }
            }
        }

        card.getChildren().addAll(header, title, author);
        if (!recommendationTags.getChildren().isEmpty()) {
            card.getChildren().add(recommendationTags);
        }
        return card;
    }

    private String relevanceLabel(double score) {
        if (score >= 12) {
            return "Pertinence forte";
        }
        if (score >= 6) {
            return "Pertinence moyenne";
        }
        return "Pertinence recente";
    }

    private void renderVideoBlock() {
        boolean videoTopic = currentTopic != null && currentTopic.isVideo();
        videoBox.setVisible(videoTopic);
        videoBox.setManaged(videoTopic);
        if (!videoTopic) {
            videoFallbackLabel.setVisible(false);
            videoFallbackLabel.setManaged(false);
            videoWebView.setVisible(false);
            videoWebView.setManaged(false);
            clearVideoWebView();
            return;
        }

        String embedUrl = toYouTubeEmbedUrl(currentTopic.getVideoUrl());
        if (embedUrl == null) {
            showVideoFallback("Video indisponible : lien YouTube absent ou invalide.");
            return;
        }

        try {
            videoFallbackLabel.setVisible(false);
            videoFallbackLabel.setManaged(false);
            videoWebView.setVisible(true);
            videoWebView.setManaged(true);
            videoWebView.getEngine().loadContent(buildYoutubeEmbedHtml(embedUrl));
        } catch (Exception e) {
            System.err.println("[ForumDetailController] Erreur video topicId=" + currentTopic.getId() +
                    " videoUrl=" + currentTopic.getVideoUrl() +
                    " embedUrl=" + embedUrl +
                    " message=" + e.getMessage());
            e.printStackTrace();
            showVideoFallback("Video indisponible : impossible de charger le lecteur integre.");
        }
    }

    private void showVideoFallback(String message) {
        clearVideoWebView();
        videoFallbackLabel.setText(message);
        videoFallbackLabel.setVisible(true);
        videoFallbackLabel.setManaged(true);
        videoWebView.setVisible(false);
        videoWebView.setManaged(false);
    }

    private void clearVideoWebView() {
        try {
            if (videoWebView != null) {
                videoWebView.getEngine().loadContent("");
            }
        } catch (Exception e) {
            System.err.println("[ForumDetailController] Nettoyage WebView impossible: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String toYouTubeEmbedUrl(String rawUrl) {
        String videoId = extractYouTubeVideoId(rawUrl);
        return videoId == null ? null : "https://www.youtube.com/embed/" + videoId;
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

    private String buildYoutubeEmbedHtml(String embedUrl) {
        return """
                <!doctype html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        html, body { margin: 0; padding: 0; height: 100%; background: #0f172a; overflow: hidden; }
                        iframe { width: 100%; height: 100%; border: 0; display: block; }
                    </style>
                </head>
                <body>
                    <iframe src="__EMBED_URL__" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>
                </body>
                </html>
                """.replace("__EMBED_URL__", embedUrl);
    }

    private void loadComments() {
        commentsContainer.getChildren().clear();

        List<ForumComment> comments = commentService.findByTopicId(topicId, isAdmin());
        currentComments = comments;
        User currentUser = resolveCurrentUser();
        commentsStatsLabel.setText(comments.size() + (comments.size() > 1 ? " commentaires" : " commentaire"));
        emptyCommentsLabel.setVisible(comments.isEmpty());
        emptyCommentsLabel.setManaged(comments.isEmpty());

        if (comments.isEmpty()) {
            return;
        }

        Map<Integer, ForumComment> commentsById = new LinkedHashMap<>();
        Map<Integer, List<ForumComment>> repliesByParentId = new LinkedHashMap<>();
        List<ForumComment> rootComments = new ArrayList<>();

        for (ForumComment comment : comments) {
            commentsById.put(comment.getId(), comment);
        }

        for (ForumComment comment : comments) {
            Integer parentId = comment.getParentId();
            if (parentId == null || !commentsById.containsKey(parentId)) {
                rootComments.add(comment);
            } else {
                repliesByParentId.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(comment);
            }
        }

        for (ForumComment comment : rootComments) {
            commentsContainer.getChildren().add(buildCommentThread(comment, repliesByParentId, commentsById, currentUser, 0));
        }
    }

    private VBox buildCommentThread(ForumComment comment,
                                    Map<Integer, List<ForumComment>> repliesByParentId,
                                    Map<Integer, ForumComment> commentsById,
                                    User currentUser,
                                    int depth) {
        VBox thread = new VBox(8);
        if (depth > 0) {
            thread.setPadding(new Insets(0, 0, 0, Math.min(depth, 4) * 26));
        }

        VBox card = createCommentCard(comment, commentsById, currentUser, depth);
        thread.getChildren().add(card);

        List<ForumComment> replies = repliesByParentId.get(comment.getId());
        if (replies != null) {
            for (ForumComment reply : replies) {
                thread.getChildren().add(buildCommentThread(reply, repliesByParentId, commentsById, currentUser, depth + 1));
            }
        }

        return thread;
    }

    private VBox createCommentCard(ForumComment comment,
                                   Map<Integer, ForumComment> commentsById,
                                   User currentUser,
                                   int depth) {
        Map<String, Integer> reactionCounts = commentReactionService.getReactionCounts(comment.getId());
        String userReaction = currentUser != null
                ? commentReactionService.getUserReactionForComment(comment.getId(), currentUser.getId())
                : null;

        VBox card = new VBox(10);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: " + (depth == 0 ? "#ffffff" : "#f8fbff") + "; " +
                "-fx-background-radius: 12; " +
                "-fx-border-color: " + (depth == 0 ? "#e2e8f0" : "#d8e6f8") + "; " +
                "-fx-border-radius: 12;");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = new FontIcon(FontAwesomeSolid.USER_CIRCLE);
        icon.setIconSize(18);
        icon.setIconColor(Color.web(roleColor(comment.getAuthorRoles())));

        Label roleBadge = new Label(roleLabel(comment.getAuthorRoles()));
        roleBadge.setStyle("-fx-background-color: " + roleColor(comment.getAuthorRoles()) + "; " +
                "-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; " +
                "-fx-background-radius: 999; -fx-padding: 3 9;");

        Label authorLabel = new Label(
                displayAuthorName(comment) +
                        " - " + (comment.getCreatedAt() != null ? comment.getCreatedAt().format(dateFormatter) : "-")
        );
        authorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        header.getChildren().addAll(icon, roleBadge, authorLabel);

        if (comment.isReported()) {
            header.getChildren().add(createBadge("Signale", "#fef3c7", "#b45309"));
        }
        if (comment.isHidden()) {
            header.getChildren().add(createBadge("Masque", "#e2e8f0", "#475569"));
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().add(spacer);

        if (isAdmin()) {
            Button reportCommentButton = createIconButton(
                    FontAwesomeSolid.FLAG,
                    comment.isReported() ? "#b91c1c" : "#b45309",
                    comment.isReported() ? "#fee2e2" : "#fef3c7",
                    comment.isReported() ? "Retirer le signalement" : "Marquer comme signale"
            );
            reportCommentButton.setOnAction(event -> toggleCommentReported(comment));
            header.getChildren().add(reportCommentButton);
        }

        if (canManageComment(comment.getAuthorId())) {
            Button deleteCommentButton = createIconButton(
                    FontAwesomeSolid.TRASH_ALT,
                    "#dc2626",
                    "#fee2e2",
                    "Supprimer"
            );
            deleteCommentButton.setOnAction(event -> deleteComment(comment.getId()));
            header.getChildren().add(deleteCommentButton);
        }

        VBox contentBox = new VBox(6);
        if (comment.getParentId() != null) {
            Label replyToLabel = new Label("Reponse a " + resolveReplyTarget(comment, commentsById));
            replyToLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2563eb;");
            contentBox.getChildren().add(replyToLabel);
        }

        Label content = new Label(comment.getContent());
        content.setWrapText(true);
        content.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155; -fx-line-spacing: 2;");
        contentBox.getChildren().add(content);

        HBox actionsRow = new HBox(10);
        actionsRow.setAlignment(Pos.CENTER_LEFT);

        Button likeButton = createReactionButton(
                CommentReactionService.TYPE_LIKE,
                "J'aime",
                reactionCounts.getOrDefault(CommentReactionService.TYPE_LIKE, 0),
                CommentReactionService.TYPE_LIKE.equals(userReaction)
        );
        likeButton.setDisable(currentUser == null);
        likeButton.setOnAction(event -> toggleCommentReaction(comment, CommentReactionService.TYPE_LIKE));

        Button loveButton = createReactionButton(
                CommentReactionService.TYPE_LOVE,
                "J'adore",
                reactionCounts.getOrDefault(CommentReactionService.TYPE_LOVE, 0),
                CommentReactionService.TYPE_LOVE.equals(userReaction)
        );
        loveButton.setDisable(currentUser == null);
        loveButton.setOnAction(event -> toggleCommentReaction(comment, CommentReactionService.TYPE_LOVE));

        if (currentUser == null) {
            likeButton.setTooltip(new Tooltip("Connectez-vous pour reagir aux commentaires."));
            loveButton.setTooltip(new Tooltip("Connectez-vous pour reagir aux commentaires."));
        }

        Button replyButton = createReplyButton();
        replyButton.setDisable(currentUser == null);
        if (currentUser == null) {
            replyButton.setTooltip(new Tooltip("Connectez-vous pour repondre aux commentaires."));
        }

        VBox replyFormBox = createReplyForm(comment);
        replyButton.setOnAction(event -> toggleReplyForm(replyFormBox));

        actionsRow.getChildren().addAll(likeButton, loveButton, replyButton);

        card.getChildren().addAll(header, contentBox, actionsRow, replyFormBox);
        return card;
    }

    private VBox createReplyForm(ForumComment parentComment) {
        VBox replyFormBox = new VBox(8);
        replyFormBox.setVisible(false);
        replyFormBox.setManaged(false);
        replyFormBox.setPadding(new Insets(10, 0, 0, 0));
        replyFormBox.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10; -fx-padding: 10;");

        TextArea replyArea = new TextArea();
        replyArea.setPrefRowCount(3);
        replyArea.setWrapText(true);
        replyArea.setPromptText("Ecrivez votre reponse a " + displayAuthorName(parentComment) + "...");
        replyArea.setStyle("-fx-background-radius: 10; -fx-font-size: 13px;");

        Label replyFeedbackLabel = new Label();
        replyFeedbackLabel.setWrapText(true);
        replyFeedbackLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #dc2626;");

        Button cancelButton = new Button("Annuler");
        cancelButton.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #475569; " +
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 10; " +
                "-fx-cursor: hand; -fx-padding: 8 14;");
        cancelButton.setOnAction(event -> {
            replyArea.clear();
            replyFeedbackLabel.setText("");
            replyFormBox.setVisible(false);
            replyFormBox.setManaged(false);
        });

        Button publishReplyButton = new Button("Publier la reponse");
        publishReplyButton.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; " +
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 10; " +
                "-fx-cursor: hand; -fx-padding: 8 14;");
        publishReplyButton.setOnAction(event -> publishReply(parentComment, replyArea, replyFeedbackLabel));

        HBox actions = new HBox(10, cancelButton, publishReplyButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        replyFormBox.getChildren().addAll(replyArea, replyFeedbackLabel, actions);
        return replyFormBox;
    }

    private void toggleReplyForm(VBox replyFormBox) {
        boolean show = !replyFormBox.isVisible();
        replyFormBox.setVisible(show);
        replyFormBox.setManaged(show);
        if (show && !replyFormBox.getChildren().isEmpty() && replyFormBox.getChildren().getFirst() instanceof TextArea replyArea) {
            Platform.runLater(replyArea::requestFocus);
        }
    }

    private void publishReply(ForumComment parentComment, TextArea replyArea, Label replyFeedbackLabel) {
        User user = resolveCurrentUser();
        if (user == null) {
            replyFeedbackLabel.setText("Connectez-vous pour repondre a ce commentaire.");
            return;
        }
        if (currentTopic == null) {
            replyFeedbackLabel.setText("Sujet introuvable.");
            return;
        }

        String replyContent = replyArea.getText() != null ? replyArea.getText().trim() : "";
        if (replyContent.isEmpty()) {
            replyFeedbackLabel.setText("Veuillez saisir une reponse avant de publier.");
            return;
        }

        ForumComment reply = new ForumComment();
        reply.setAuthorId(user.getId());
        reply.setTopicId(currentTopic.getId());
        reply.setParentId(parentComment.getId());
        reply.setContent(replyContent);

        ContentModerationResult moderationResult = contentModerationService.moderate(reply.getContent());
        if (moderationResult.hasToxicContent()) {
            replyFeedbackLabel.setText(moderationResult.getMessage());
            return;
        }
        applyAutomaticFlag(reply, moderationResult);

        try {
            commentService.addComment(reply);
            replyArea.clear();
            replyFeedbackLabel.setText("");
            loadComments();
            showInfo("Forum", "Reponse publiee avec succes.");
        } catch (Exception e) {
            replyFeedbackLabel.setText("Impossible de publier la reponse.");
            showError("Impossible de publier la reponse.", e);
        }
    }

    private Button createReplyButton() {
        Button button = new Button("Repondre");
        button.setStyle("-fx-background-color: #eff6ff; -fx-border-color: #bfdbfe; " +
                "-fx-text-fill: #1d4ed8; -fx-font-size: 12px; -fx-font-weight: bold; " +
                "-fx-background-radius: 999; -fx-border-radius: 999; " +
                "-fx-cursor: hand; -fx-padding: 7 12;");
        return button;
    }

    private String resolveReplyTarget(ForumComment comment, Map<Integer, ForumComment> commentsById) {
        if (comment.getParentId() == null) {
            return "ce commentaire";
        }

        ForumComment parentComment = commentsById.get(comment.getParentId());
        if (parentComment == null) {
            return "ce commentaire";
        }
        return displayAuthorName(parentComment);
    }

    private String displayAuthorName(ForumComment comment) {
        if (comment == null || comment.getAuthorName() == null || comment.getAuthorName().isBlank()) {
            return "Auteur inconnu";
        }
        return comment.getAuthorName();
    }

    private void initializeAssistantForCurrentTopic() {
        assistantSendButton.setDisable(false);
        assistantInputArea.setDisable(false);

        if (currentTopic == null) {
            assistantStatusLabel.setText("Le sujet n'est pas disponible.");
            return;
        }

        if (assistantInitializedTopicId == currentTopic.getId()) {
            if (!assistantRequestInProgress) {
                assistantStatusLabel.setText("Posez une question sur ce sujet.");
            }
            return;
        }

        assistantInitializedTopicId = currentTopic.getId();
        assistantMessagesContainer.getChildren().clear();

        StringBuilder welcome = new StringBuilder();
        welcome.append("Je peux vous aider a resumer ce sujet, repondre a des questions simples a partir des commentaires et proposer des sujets similaires");
        if (!currentRelatedTopics.isEmpty()) {
            welcome.append(" deja trouves dans le forum");
        }
        welcome.append(". Ces informations sont generales et ne remplacent pas l'avis d'un professionnel de sante.");
        appendAssistantBubble(welcome.toString());
        assistantStatusLabel.setText("Preparation de l'assistant...");
        warmUpAssistant();
    }

    private void toggleCommentReaction(ForumComment comment, String type) {
        User user = resolveCurrentUser();
        if (user == null) {
            showError("Connectez-vous pour reagir aux commentaires.", null);
            return;
        }

        try {
            commentReactionService.toggleReaction(comment.getId(), user.getId(), type);
            loadComments();
        } catch (Exception e) {
            showError("Impossible de mettre a jour la reaction du commentaire.", e);
        }
    }

    private void toggleCommentReported(ForumComment comment) {
        User user = resolveCurrentUser();
        if (user == null || !user.hasRole("ROLE_ADMIN")) {
            showError("Seul un administrateur peut modifier le signalement d'un commentaire.", null);
            return;
        }

        boolean newState = !comment.isReported();
        String action = newState ? "signaler" : "retirer le signalement de";
        if (!confirm("Moderation commentaire", "Voulez-vous " + action + " ce commentaire ?")) {
            return;
        }

        try {
            commentService.setCommentReported(comment.getId(), newState, newState ? user.getId() : null);
            loadComments();
        } catch (Exception e) {
            showError("Impossible de mettre a jour le signalement du commentaire.", e);
        }
    }

    private void deleteComment(int commentId) {
        if (!confirm("Supprimer le commentaire", "Voulez-vous vraiment supprimer ce commentaire ?")) {
            return;
        }

        try {
            commentService.deleteComment(commentId);
            loadComments();
        } catch (Exception e) {
            showError("Impossible de supprimer le commentaire.", e);
        }
    }

    private void updateStatusBadge(Label label, boolean visible, String text, String backgroundColor, String textColor) {
        label.setVisible(visible);
        label.setManaged(visible);
        if (!visible) {
            label.setText("");
            return;
        }
        label.setText(text);
        label.setStyle("-fx-background-color: " + backgroundColor + "; -fx-text-fill: " + textColor + "; " +
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 999; -fx-padding: 4 10;");
    }

    private Label createBadge(String text, String backgroundColor, String textColor) {
        Label label = new Label(text);
        label.setStyle("-fx-background-color: " + backgroundColor + "; -fx-text-fill: " + textColor + "; " +
                "-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 999; -fx-padding: 3 9;");
        return label;
    }

    private Button createIconButton(FontAwesomeSolid iconType, String iconColor, String backgroundColor, String tooltip) {
        Button button = new Button();
        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(12);
        icon.setIconColor(Color.web(iconColor));
        button.setGraphic(icon);
        button.setStyle("-fx-background-color: " + backgroundColor + "; -fx-background-radius: 8; -fx-cursor: hand;");
        button.setTooltip(new Tooltip(tooltip));
        return button;
    }

    private Button createReactionButton(String type, String label, int count, boolean active) {
        boolean like = CommentReactionService.TYPE_LIKE.equals(type);
        String textColor = like
                ? (active ? "#0369a1" : "#2563eb")
                : (active ? "#be123c" : "#e11d48");
        String borderColor = like
                ? (active ? "#38bdf8" : "#bfdbfe")
                : (active ? "#fb7185" : "#fecdd3");
        String backgroundColor = like
                ? (active ? "#dbeafe" : "#eff6ff")
                : (active ? "#ffe4e6" : "#fff1f2");

        FontIcon icon = new FontIcon(like ? FontAwesomeSolid.THUMBS_UP : FontAwesomeSolid.HEART);
        icon.setIconSize(13);
        icon.setIconColor(Color.web(textColor));

        Button button = new Button(label + " " + count);
        button.setGraphic(icon);
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setGraphicTextGap(6);
        button.setStyle("-fx-background-color: " + backgroundColor + "; " +
                "-fx-border-color: " + borderColor + "; " +
                "-fx-text-fill: " + textColor + "; " +
                "-fx-font-size: 12px; -fx-font-weight: bold; " +
                "-fx-background-radius: 999; -fx-border-radius: 999; " +
                "-fx-cursor: hand; -fx-padding: 7 12;");
        button.setTooltip(new Tooltip("Reagir avec " + label));
        return button;
    }

    private void setAssistantLoading(boolean loading, String status) {
        assistantRequestInProgress = loading;
        assistantInputArea.setDisable(loading);
        assistantSendButton.setDisable(loading || currentTopic == null);
        assistantStatusLabel.setText(status);
    }

    private void appendUserBubble(String message) {
        appendChatBubble(
                "Vous",
                message,
                Pos.CENTER_RIGHT,
                "-fx-background-color: #2563eb; -fx-background-radius: 16; -fx-padding: 10 12;",
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #dbeafe;",
                "-fx-font-size: 13px; -fx-text-fill: white; -fx-line-spacing: 2;",
                List.of()
        );
    }

    private void appendAssistantBubble(String message) {
        appendAssistantBubble(message, List.of());
    }

    private void appendAssistantBubble(String message, List<ChatAssistantRecommendation> recommendations) {
        appendChatBubble(
                "Assistant medical local",
                message,
                Pos.CENTER_LEFT,
                "-fx-background-color: #f8fafc; -fx-background-radius: 16; -fx-border-color: #dbeafe; " +
                        "-fx-border-radius: 16; -fx-padding: 10 12;",
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2563eb;",
                "-fx-font-size: 13px; -fx-text-fill: #1e293b; -fx-line-spacing: 2;",
                recommendations
        );
    }

    private void appendAssistantError(Throwable throwable) {
        appendChatBubble(
                "Assistant medical local",
                formatAssistantError(throwable),
                Pos.CENTER_LEFT,
                "-fx-background-color: #fff7ed; -fx-background-radius: 16; -fx-border-color: #fdba74; " +
                        "-fx-border-radius: 16; -fx-padding: 10 12;",
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #c2410c;",
                "-fx-font-size: 13px; -fx-text-fill: #9a3412; -fx-line-spacing: 2;",
                List.of()
        );
    }

    private void appendChatBubble(String author,
                                  String message,
                                  Pos alignment,
                                  String bubbleStyle,
                                  String authorStyle,
                                  String messageStyle,
                                  List<ChatAssistantRecommendation> recommendations) {
        HBox row = new HBox();
        row.setAlignment(alignment);

        VBox bubble = new VBox(6);
        bubble.setMaxWidth(540);
        bubble.setStyle(bubbleStyle);

        Label authorLabel = new Label(author);
        authorLabel.setStyle(authorStyle);

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(516);
        messageLabel.setStyle(messageStyle);

        bubble.getChildren().addAll(authorLabel, messageLabel);

        if (recommendations != null && !recommendations.isEmpty()) {
            Label recommendationsLabel = new Label("Sujets suggeres :");
            recommendationsLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #475569;");

            FlowPane recommendationsPane = new FlowPane(8, 8);
            for (ChatAssistantRecommendation recommendation : recommendations) {
                if (recommendation.getTitle() == null || recommendation.getTitle().isBlank()) {
                    continue;
                }

                Button recommendationButton = new Button(recommendation.getTitle());
                recommendationButton.setStyle("-fx-background-color: white; -fx-text-fill: #1d4ed8; " +
                        "-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 999; " +
                        "-fx-border-color: #bfdbfe; -fx-border-radius: 999; -fx-cursor: hand; -fx-padding: 6 12;");
                recommendationButton.setDisable(recommendation.getId() <= 0 || recommendation.getId() == topicId);
                recommendationButton.setOnAction(event -> openForumDetail(recommendation.getId()));
                recommendationsPane.getChildren().add(recommendationButton);
            }

            if (!recommendationsPane.getChildren().isEmpty()) {
                bubble.getChildren().addAll(recommendationsLabel, recommendationsPane);
            }
        }

        row.getChildren().add(bubble);
        assistantMessagesContainer.getChildren().add(row);
        scrollAssistantToBottom();
    }

    private void scrollAssistantToBottom() {
        Platform.runLater(() -> {
            if (assistantMessagesScrollPane != null) {
                assistantMessagesScrollPane.setVvalue(1.0);
            }
        });
    }

    private String formatAssistantError(Throwable throwable) {
        if (throwable != null && throwable.getMessage() != null && !throwable.getMessage().isBlank()) {
            return throwable.getMessage();
        }
        return "Le chatbot local est indisponible pour le moment. Verifiez que le service Flask est bien demarre.";
    }

    private void applyAutomaticFlag(ForumComment comment, ContentModerationResult moderationResult) {
        if (!moderationResult.shouldAutoReport()) {
            return;
        }

        comment.setReported(true);
        comment.setReportedReason("Signalement automatique moderation: " + moderationResult.getType());
        comment.setReportedAt(java.time.LocalDateTime.now());
        comment.setReportedById(null);
    }

    private void warmUpAssistant() {
        if (currentTopic == null || assistantWarmupTopicId == currentTopic.getId()) {
            if (!assistantRequestInProgress && currentTopic != null && "Preparation de l'assistant...".equals(assistantStatusLabel.getText())) {
                assistantStatusLabel.setText("Essayez par exemple : Resume ce sujet.");
            }
            return;
        }

        assistantWarmupTopicId = currentTopic.getId();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                chatAssistantService.warmUp();
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            if (!assistantRequestInProgress) {
                assistantStatusLabel.setText("Assistant pret. Essayez par exemple : Resume ce sujet.");
            }
        });

        task.setOnFailed(event -> {
            assistantWarmupTopicId = -1;
            if (!assistantRequestInProgress) {
                assistantStatusLabel.setText("Assistant indisponible.");
            }
        });

        Thread warmupThread = new Thread(task, "forum-chat-assistant-warmup");
        warmupThread.setDaemon(true);
        warmupThread.start();
    }

}
