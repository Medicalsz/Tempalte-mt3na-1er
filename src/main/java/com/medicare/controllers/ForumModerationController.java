package com.medicare.controllers;

import com.medicare.models.ForumComment;
import com.medicare.models.ForumTopic;
import com.medicare.services.CommentService;
import com.medicare.services.ForumService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ForumModerationController extends ForumController {

    private static final String TOPIC_FILTER_REPORTED = "Sujets signales";
    private static final String TOPIC_FILTER_HIDDEN = "Sujets masques";
    private static final String TOPIC_FILTER_ALL = "Tous les sujets moderes";

    private static final String COMMENT_FILTER_REPORTED = "Commentaires signales";
    private static final String COMMENT_FILTER_HIDDEN = "Commentaires masques";
    private static final String COMMENT_FILTER_ALL = "Tous les commentaires moderes";

    @FXML private ComboBox<String> topicFilterCombo;
    @FXML private ComboBox<String> commentFilterCombo;
    @FXML private Label headerStatsLabel;
    @FXML private Label errorLabel;
    @FXML private Label topicStatsLabel;
    @FXML private Label emptyTopicsLabel;
    @FXML private VBox moderatedTopicsContainer;
    @FXML private Label commentStatsLabel;
    @FXML private Label emptyCommentsLabel;
    @FXML private VBox moderatedCommentsContainer;

    private final ForumService forumService = new ForumService();
    private final CommentService commentService = new CommentService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {
        topicFilterCombo.getItems().setAll(TOPIC_FILTER_REPORTED, TOPIC_FILTER_HIDDEN, TOPIC_FILTER_ALL);
        topicFilterCombo.setValue(TOPIC_FILTER_REPORTED);
        commentFilterCombo.getItems().setAll(COMMENT_FILTER_REPORTED, COMMENT_FILTER_HIDDEN, COMMENT_FILTER_ALL);
        commentFilterCombo.setValue(COMMENT_FILTER_REPORTED);

        topicFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> loadModeratedTopics());
        commentFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> loadModeratedComments());
    }

    @Override
    protected void onForumContextReady() {
        if (!isAdmin()) {
            showError("Cet espace est reserve aux administrateurs.", null);
            openForumList();
            return;
        }
        loadModerationData();
    }

    @FXML
    private void onBackClick() {
        openForumList();
    }

    @FXML
    private void onRefreshClick() {
        loadModerationData();
    }

    private void loadModerationData() {
        headerStatsLabel.setText("Synchronise avec les colonnes MySQL is_reported et is_hidden");
        errorLabel.setText("");
        loadModeratedTopics();
        loadModeratedComments();
    }

    private void loadModeratedTopics() {
        try {
            List<ForumTopic> topics = forumService.findModeratedTopics(
                    TOPIC_FILTER_REPORTED.equals(topicFilterCombo.getValue()),
                    TOPIC_FILTER_HIDDEN.equals(topicFilterCombo.getValue())
            );
            renderModeratedTopics(topics);
        } catch (Exception e) {
            errorLabel.setText("Impossible de charger les sujets moderes.");
            e.printStackTrace();
        }
    }

    private void loadModeratedComments() {
        try {
            List<ForumComment> comments = commentService.findModeratedComments(
                    COMMENT_FILTER_REPORTED.equals(commentFilterCombo.getValue()),
                    COMMENT_FILTER_HIDDEN.equals(commentFilterCombo.getValue())
            );
            renderModeratedComments(comments);
        } catch (Exception e) {
            errorLabel.setText("Impossible de charger les commentaires moderes.");
            e.printStackTrace();
        }
    }

    private void renderModeratedTopics(List<ForumTopic> topics) {
        moderatedTopicsContainer.getChildren().clear();
        topicStatsLabel.setText(topics.size() + (topics.size() > 1 ? " sujets" : " sujet"));
        emptyTopicsLabel.setVisible(topics.isEmpty());
        emptyTopicsLabel.setManaged(topics.isEmpty());

        for (ForumTopic topic : topics) {
            VBox card = new VBox(12);
            card.setPadding(new Insets(16));
            card.setStyle("-fx-background-color: white; -fx-background-radius: 14; " +
                    "-fx-border-color: #e2e8f0; -fx-border-radius: 14; " +
                    "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.06), 12, 0, 0, 2);");

            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);

            VBox titleBox = new VBox(6);
            HBox.setHgrow(titleBox, Priority.ALWAYS);

            Label titleLabel = new Label(topic.getTitle());
            titleLabel.setWrapText(true);
            titleLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

            Label metaLabel = new Label(
                    (topic.getAuthorName() != null ? topic.getAuthorName() : "Auteur inconnu") +
                            " - " + roleLabel(topic.getAuthorRoles()) +
                            " - " + (topic.getCreatedAt() != null ? topic.getCreatedAt().format(dateFormatter) : "-")
            );
            metaLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

            FlowPane badges = new FlowPane();
            badges.setHgap(8);
            badges.setVgap(8);
            if (topic.isReported()) {
                badges.getChildren().add(createBadge("Signale", "#fef3c7", "#b45309"));
            }
            if (topic.isHidden()) {
                badges.getChildren().add(createBadge("Masque", "#e2e8f0", "#475569"));
            }

            titleBox.getChildren().addAll(titleLabel, metaLabel, badges);

            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_RIGHT);

            Button detailButton = createActionButton(FontAwesomeSolid.EYE, "#1d4ed8", "#dbeafe", "Voir le sujet");
            detailButton.setOnAction(event -> openForumDetail(topic.getId()));

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
                    topic.isHidden() ? "Reafficher le sujet" : "Masquer le sujet"
            );
            hiddenButton.setOnAction(event -> toggleTopicHidden(topic));

            actions.getChildren().addAll(detailButton, reportButton, hiddenButton);
            header.getChildren().addAll(titleBox, actions);

            Label summaryLabel = new Label(topic.getDisplaySummary());
            summaryLabel.setWrapText(true);
            summaryLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155; -fx-line-spacing: 2;");

            card.getChildren().addAll(header, summaryLabel);
            moderatedTopicsContainer.getChildren().add(card);
        }
    }

    private void renderModeratedComments(List<ForumComment> comments) {
        moderatedCommentsContainer.getChildren().clear();
        commentStatsLabel.setText(comments.size() + (comments.size() > 1 ? " commentaires" : " commentaire"));
        emptyCommentsLabel.setVisible(comments.isEmpty());
        emptyCommentsLabel.setManaged(comments.isEmpty());

        for (ForumComment comment : comments) {
            VBox card = new VBox(12);
            card.setPadding(new Insets(16));
            card.setStyle("-fx-background-color: white; -fx-background-radius: 14; " +
                    "-fx-border-color: #e2e8f0; -fx-border-radius: 14; " +
                    "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.06), 12, 0, 0, 2);");

            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);

            VBox infoBox = new VBox(6);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            Label topicLabel = new Label(comment.getTopicTitle() != null ? comment.getTopicTitle() : "Sujet #" + comment.getTopicId());
            topicLabel.setWrapText(true);
            topicLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

            Label metaLabel = new Label(
                    (comment.getAuthorName() != null ? comment.getAuthorName() : "Auteur inconnu") +
                            " - " + roleLabel(comment.getAuthorRoles()) +
                            " - " + (comment.getCreatedAt() != null ? comment.getCreatedAt().format(dateFormatter) : "-")
            );
            metaLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

            FlowPane badges = new FlowPane();
            badges.setHgap(8);
            badges.setVgap(8);
            if (comment.isReported()) {
                badges.getChildren().add(createBadge("Signale", "#fef3c7", "#b45309"));
            }
            if (comment.isHidden()) {
                badges.getChildren().add(createBadge("Masque", "#e2e8f0", "#475569"));
            }

            infoBox.getChildren().addAll(topicLabel, metaLabel, badges);

            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_RIGHT);

            Button openTopicButton = createActionButton(FontAwesomeSolid.COMMENTS, "#1d4ed8", "#dbeafe", "Ouvrir le sujet");
            openTopicButton.setOnAction(event -> openForumDetail(comment.getTopicId()));

            Button reportButton = createActionButton(
                    FontAwesomeSolid.FLAG,
                    comment.isReported() ? "#b91c1c" : "#b45309",
                    comment.isReported() ? "#fee2e2" : "#fef3c7",
                    comment.isReported() ? "Retirer le signalement" : "Marquer comme signale"
            );
            reportButton.setOnAction(event -> toggleCommentReported(comment));

            Button hiddenButton = createActionButton(
                    comment.isHidden() ? FontAwesomeSolid.EYE : FontAwesomeSolid.EYE_SLASH,
                    comment.isHidden() ? "#0f766e" : "#475569",
                    comment.isHidden() ? "#ccfbf1" : "#e2e8f0",
                    comment.isHidden() ? "Reafficher le commentaire" : "Masquer le commentaire"
            );
            hiddenButton.setOnAction(event -> toggleCommentHidden(comment));

            actions.getChildren().addAll(openTopicButton, reportButton, hiddenButton);
            header.getChildren().addAll(infoBox, actions);

            Label contentLabel = new Label(comment.getContent());
            contentLabel.setWrapText(true);
            contentLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155; -fx-line-spacing: 2;");

            card.getChildren().addAll(header, contentLabel);
            moderatedCommentsContainer.getChildren().add(card);
        }
    }

    private void toggleTopicReported(ForumTopic topic) {
        boolean newState = !topic.isReported();
        String action = newState ? "signaler" : "retirer le signalement de";
        if (!confirm("Moderation sujet", "Voulez-vous " + action + " ce sujet ?")) {
            return;
        }

        try {
            forumService.setTopicReported(topic.getId(), newState, newState ? resolveCurrentUser().getId() : null);
            loadModeratedTopics();
        } catch (Exception e) {
            showError("Impossible de mettre a jour le signalement du sujet.", e);
        }
    }

    private void toggleTopicHidden(ForumTopic topic) {
        boolean newState = !topic.isHidden();
        String action = newState ? "masquer" : "reafficher";
        if (!confirm("Moderation sujet", "Voulez-vous " + action + " ce sujet ?")) {
            return;
        }

        try {
            forumService.setTopicHidden(topic.getId(), newState);
            loadModeratedTopics();
        } catch (Exception e) {
            showError("Impossible de mettre a jour la visibilite du sujet.", e);
        }
    }

    private void toggleCommentReported(ForumComment comment) {
        boolean newState = !comment.isReported();
        String action = newState ? "signaler" : "retirer le signalement de";
        if (!confirm("Moderation commentaire", "Voulez-vous " + action + " ce commentaire ?")) {
            return;
        }

        try {
            commentService.setCommentReported(comment.getId(), newState, newState ? resolveCurrentUser().getId() : null);
            loadModeratedComments();
        } catch (Exception e) {
            showError("Impossible de mettre a jour le signalement du commentaire.", e);
        }
    }

    private void toggleCommentHidden(ForumComment comment) {
        boolean newState = !comment.isHidden();
        String action = newState ? "masquer" : "reafficher";
        if (!confirm("Moderation commentaire", "Voulez-vous " + action + " ce commentaire ?")) {
            return;
        }

        try {
            commentService.setCommentHidden(comment.getId(), newState);
            loadModeratedComments();
        } catch (Exception e) {
            showError("Impossible de mettre a jour la visibilite du commentaire.", e);
        }
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
}
