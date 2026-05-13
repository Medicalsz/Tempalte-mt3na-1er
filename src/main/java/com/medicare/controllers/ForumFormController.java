package com.medicare.controllers;

import com.medicare.models.ContentModerationResult;
import com.medicare.models.ForumTopic;
import com.medicare.models.User;
import com.medicare.services.ContentModerationService;
import com.medicare.services.ForumService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class ForumFormController extends ForumController {

    @FXML private Label formTitleLabel;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;
    @FXML private TextField titleField;
    @FXML private TextArea summaryArea;
    @FXML private TextField tagsField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private VBox videoUrlBox;
    @FXML private TextField videoUrlField;
    @FXML private TextArea contentInputArea;

    private final ForumService forumService = new ForumService();
    private final ContentModerationService contentModerationService = new ContentModerationService();

    private ForumTopic topicToEdit;

    @FXML
    private void initialize() {
        typeCombo.setItems(FXCollections.observableArrayList("Article", "Video"));
        typeCombo.setValue("Article");
        typeCombo.valueProperty().addListener((obs, oldValue, newValue) -> updateVideoFieldVisibility());
        updateVideoFieldVisibility();
    }

    public void setTopicToEdit(ForumTopic topic) {
        this.topicToEdit = topic;
        if (topic == null) {
            formTitleLabel.setText("Nouveau sujet");
            saveButton.setText("Publier le sujet");
            typeCombo.setValue("Article");
            updateVideoFieldVisibility();
            return;
        }

        formTitleLabel.setText("Modifier le sujet");
        saveButton.setText("Enregistrer les modifications");
        titleField.setText(topic.getTitle());
        summaryArea.setText(topic.getSummary());
        tagsField.setText(topic.getTagsDisplay());
        typeCombo.setValue(topic.isVideo() ? "Video" : "Article");
        videoUrlField.setText(topic.getVideoUrl());
        contentInputArea.setText(topic.getContent());
        updateVideoFieldVisibility();
    }

    @FXML
    private void onBackClick() {
        openForumList();
    }

    @FXML
    private void onSaveClick() {
        errorLabel.setText("");
        User user = resolveCurrentUser();
        if (user == null) {
            errorLabel.setText("Connectez-vous pour publier un sujet.");
            return;
        }

        if (titleField.getText().isBlank()) {
            errorLabel.setText("Le titre est obligatoire.");
            return;
        }
        if (contentInputArea.getText().isBlank()) {
            errorLabel.setText("Le contenu est obligatoire.");
            return;
        }
        if ("Video".equals(typeCombo.getValue()) && videoUrlField.getText().isBlank()) {
            errorLabel.setText("Ajoutez une URL video pour un sujet de type video.");
            return;
        }

        ForumTopic topic = topicToEdit != null ? topicToEdit : new ForumTopic();
        topic.setAuthorId(topicToEdit != null ? topicToEdit.getAuthorId() : user.getId());
        topic.setTitle(titleField.getText());
        topic.setSummary(summaryArea.getText());
        topic.setTags(tagsField.getText());
        topic.setType("Video".equals(typeCombo.getValue()) ? "video" : "text");
        topic.setVideoUrl("Video".equals(typeCombo.getValue()) ? videoUrlField.getText() : null);
        topic.setContent(contentInputArea.getText());

        ContentModerationResult titleModeration = contentModerationService.moderate(titleField.getText());
        if (titleModeration.hasToxicContent()) {
            errorLabel.setText(titleModeration.getMessage());
            return;
        }

        ContentModerationResult summaryModeration = contentModerationService.moderate(summaryArea.getText());
        if (summaryModeration.hasToxicContent()) {
            errorLabel.setText(summaryModeration.getMessage());
            return;
        }

        ContentModerationResult contentModeration = contentModerationService.moderate(contentInputArea.getText());
        if (contentModeration.hasToxicContent()) {
            errorLabel.setText(contentModeration.getMessage());
            return;
        }

        ContentModerationResult combinedModeration = contentModerationService.moderate(
                titleField.getText() + "\n" + summaryArea.getText() + "\n" + contentInputArea.getText()
        );
        if (combinedModeration.hasToxicContent()) {
            errorLabel.setText(combinedModeration.getMessage());
            return;
        }

        applyAutomaticFlag(topic, combinedModeration);

        try {
            if (topicToEdit == null) {
                forumService.addTopic(topic);
                showInfo("Forum", "Le sujet a bien ete ajoute.");
            } else {
                if (!canManageTopic(topicToEdit)) {
                    errorLabel.setText("Vous n'avez pas le droit de modifier ce sujet.");
                    return;
                }
                forumService.updateTopic(topic);
                showInfo("Forum", "Le sujet a bien ete modifie.");
            }
            openForumList();
        } catch (Exception e) {
            errorLabel.setText(e.getMessage());
        }
    }

    private void applyAutomaticFlag(ForumTopic topic, ContentModerationResult moderationResult) {
        if (topicToEdit != null || !moderationResult.shouldAutoReport()) {
            return;
        }

        topic.setReported(true);
        topic.setReportedReason("Signalement automatique moderation: " + moderationResult.getType());
        topic.setReportedAt(java.time.LocalDateTime.now());
        topic.setReportedById(null);
    }

    private void updateVideoFieldVisibility() {
        boolean show = "Video".equals(typeCombo.getValue());
        videoUrlBox.setVisible(show);
        videoUrlBox.setManaged(show);
        if (!show) {
            videoUrlField.clear();
        }
    }
}
