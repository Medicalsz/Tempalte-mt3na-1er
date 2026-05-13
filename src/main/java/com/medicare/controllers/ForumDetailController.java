ackage com.medicare.controllers;

import com.medicare.models.ChatMessage;
import com.medicare.models.ChatAssistantRecommendation;
import com.medicare.models.ChatAssistantResponse;
import com.medicare.models.ContentModerationResult;
import com.medicare.models.ForumComment;
import com.medicare.models.ForumTopic;
import com.medicare.models.User;
import com.medicare.services.ChatAssistantService;
import com.medicare.services.ForumCommentService;
import com.medicare.services.CommentReactionService;
import com.medicare.services.ContentModerationService;
import com.medicare.services.ForumService;
import com.medicare.services.ForumRecommendationService;
import com.medicare.services.TranslationService;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDateTime;
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
    @FXML private Label topicTranslationTitleLabel;
    @FXML private ComboBox<TranslationService.SupportedLanguage> topicTranslationLanguageComboBox;
    @FXML private Button topicTranslateButton;
    @FXML private Label topicTranslationStatusLabel;
    @FXML private VBox topicTranslationContainer;
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
    @FXML private ProgressIndicator assistantLoadingIndicator;
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
    private final ForumCommentService ForumCommentService = new ForumCommentService();
    private final CommentReactionService commentReactionService = new CommentReactionService();
    private final ChatAssistantService chatAssistantService = new ChatAssistantService();
    private final ContentModerationService contentModerationService = new ContentModerationService();
    private final ForumRecommendationService forumRecommendationService = new ForumRecommendationService();
    private final TranslationService translationService = new TranslationService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String DEFAULT_TRANSLATION_LANGUAGE_CODE = "en";
    private final ObservableList<TranslationService.SupportedLanguage> translationLanguages =
            FXCollections.observableArrayList(translationService.supportedLanguages());

    private int topicId;
    private ForumTopic currentTopic;
    private List<ForumComment> currentComments = List.of();
    private List<ForumTopic> currentRelatedTopics = List.of();
    private final ObservableList<ChatMessage> assistantConversation = FXCollections.observableArrayList();
    private boolean assistantRequestInProgress;
    private int assistantInitializedTopicId = -1;
    private int assistantWarmupTopicId = -1;
    private Timeline assistantTypingTimeline;
    private Label assistantTypingLabel;
    private HBox assistantTypingRow;
    private ChatMessage assistantTypingMessage;
    private int assistantTypingFrameIndex;
    private final DateTimeFormatter assistantTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final Map<String, TranslationService.TopicTranslation> topicTranslationCache = new LinkedHashMap<>();
    private final Map<String, TranslationService.CommentTranslation> commentTranslationCache = new LinkedHashMap<>();
    private final Map<Integer, String> activeCommentTranslationLanguageByCommentId = new LinkedHashMap<>();
    private final Map<Integer, String> selectedCommentLanguageByCommentId = new LinkedHashMap<>();
    private String activeTopicTranslationLanguageCode;

    @FXML
    private void initialize() {
        initializeTranslationControls();
        assistantSendButton.setDisable(true);
        assistantInputArea.setDisable(true);
        assistantStatusLabel.setText("Chargez un sujet pour discuter avec l'assistant.");
        if (assistantMessagesContainer != null) {
            assistantMessagesContainer.setFillWidth(true);
            assistantMessagesContainer.setSpacing(16);
            assistantMessagesContainer.setPadding(new Insets(22, 20, 22, 20));
            assistantMessagesContainer.heightProperty().addListener((observable, oldValue, newValue) -> scrollAssistantToBottom());
        }
        if (assistantMessagesScrollPane != null) {
            assistantMessagesScrollPane.setFitToWidth(true);
        }
        if (assistantLoadingIndicator != null) {
            assistantLoadingIndicator.setVisible(false);
            assistantLoadingIndicator.setManaged(false);
        }
        clearAssistantConversation();
        appendChatMessage(createAssistantMessage(
                "Assistant Medicare AI",
                "Je peux repondre a des questions medicales generales, resumer un sujet de forum, suggerer des conseils bien-etre simples et recommander des discussions proches.\n\n"
                        + "Ces informations ne remplacent pas un avis medical professionnel.",
                List.of()
        ));
    }

    public void setTopicId(int topicId) {
        System.out.println("[ForumDetailController] setTopicId topicId=" + topicId);
        if (this.topicId != topicId) {
            resetTranslationState();
            assistantInitializedTopicId = -1;
            assistantWarmupTopicId = -1;
            assistantRequestInProgress = false;
            currentComments = List.of();
            currentRelatedTopics = List.of();
            stopTypingIndicator();
            clearAssistantConversation();
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

        appendChatMessage(createUserMessage(message));
        List<ChatMessage> historySnapshot = new ArrayList<>(assistantConversation);
        assistantInputArea.clear();
        setAssistantLoading(true, "Assistant en train d'ecrire...");
        showTypingIndicator();

        ForumTopic topicSnapshot = currentTopic;
        List<ForumComment> commentsSnapshot = new ArrayList<>(currentComments);
        List<ForumTopic> relatedTopicsSnapshot = new ArrayList<>(currentRelatedTopics);

        Task<ChatAssistantResponse> task = new Task<>() {
            @Override
            protected ChatAssistantResponse call() {
                return chatAssistantService.askAssistant(message, topicSnapshot, commentsSnapshot, relatedTopicsSnapshot, historySnapshot);
            }
        };

        task.setOnSucceeded(event -> {
            stopTypingIndicator();
            ChatAssistantResponse response = task.getValue();
            String status = "Assistant pret.";
            if ("local-medical-fallback".equalsIgnoreCase(response.getModel())) {
                status = "Mode local actif.";
            } else if (response.isFallbackResponse()) {
                status = "Reponse securisee prete.";
            }
            setAssistantLoading(false, status);
            appendChatMessage(createAssistantMessage("Assistant Medicare AI", response.getReply(), response.getRecommendations()));
        });

        task.setOnFailed(event -> {
            stopTypingIndicator();
            setAssistantLoading(false, "Assistant indisponible.");
            appendChatMessage(createErrorMessage(formatAssistantError(task.getException())));
        });

        Thread assistantThread = new Thread(task, "forum-chat-assistant");
        assistantThread.setDaemon(true);
        assistantThread.start();
    }

    @FXML
    private void onTopicTranslateClick() {
        if (currentTopic == null) {
            setTranslationStatus(topicTranslationStatusLabel, "Le sujet n'est pas disponible pour la traduction.", true);
            return;
        }

        TranslationService.SupportedLanguage selectedLanguage = getSelectedTopicTranslationLanguage();
        if (selectedLanguage == null) {
            setTranslationStatus(topicTranslationStatusLabel, "Veuillez choisir une langue de traduction.", true);
            return;
        }

        String translationKey = buildTopicTranslationKey(currentTopic.getId(), selectedLanguage.code());
        if (translationKey.equals(activeTopicTranslationLanguageCode) && topicTranslationContainer.isVisible()) {
            hideTranslationBox(topicTranslationContainer);
            activeTopicTranslationLanguageCode = null;
            clearTranslationStatus(topicTranslationStatusLabel);
            updateTopicTranslateButtonLabel(false);
            return;
        }

        TranslationService.TopicTranslation cachedTranslation = topicTranslationCache.get(translationKey);
        if (cachedTranslation != null) {
            renderTopicTranslation(cachedTranslation);
            activeTopicTranslationLanguageCode = translationKey;
            clearTranslationStatus(topicTranslationStatusLabel);
            updateTopicTranslateButtonLabel(false);
            return;
        }

        setTopicTranslationLoading(true, "Traduction en cours...", false);
        ForumTopic topicSnapshot = currentTopic;
        Task<TranslationService.TopicTranslation> task = new Task<>() {
            @Override
            protected TranslationService.TopicTranslation call() {
                return translationService.translateTopic(topicSnapshot, selectedLanguage.code());
            }
        };

        task.setOnSucceeded(event -> {
            TranslationService.TopicTranslation translation = task.getValue();
            topicTranslationCache.put(translationKey, translation);
            activeTopicTranslationLanguageCode = translationKey;
            renderTopicTranslation(translation);
            setTopicTranslationLoading(false,
                    translation.partialTranslation()
                            ? "Traduction affichee. Les textes tres longs sont envoyes partiellement."
                            : "",
                    false
            );
        });

        task.setOnFailed(event -> setTopicTranslationLoading(false, formatTranslationError(task.getException()), true));

        Thread translationThread = new Thread(task, "forum-topic-translation");
        translationThread.setDaemon(true);
        translationThread.start();
    }

    @FXML
    private void onBackClick() {
        openForumList();
    }

    private void initializeTranslationControls() {
        if (topicTranslationTitleLabel != null) {
            topicTranslationTitleLabel.setGraphic(createToolbarIcon(FontAwesomeSolid.GLOBE, "#4f46e5", 13));
            topicTranslationTitleLabel.setContentDisplay(ContentDisplay.LEFT);
            topicTranslationTitleLabel.setGraphicTextGap(8);
        }

        if (topicTranslationLanguageComboBox != null) {
            topicTranslationLanguageComboBox.setItems(translationLanguages);
            configureTranslationLanguageComboBox(topicTranslationLanguageComboBox);
            topicTranslationLanguageComboBox.getSelectionModel().select(findTranslationLanguage(DEFAULT_TRANSLATION_LANGUAGE_CODE));
            topicTranslationLanguageComboBox.valueProperty().addListener((observable, oldValue, newValue) -> syncTopicTranslationSelection());
        }

        if (topicTranslateButton != null) {
            topicTranslateButton.setDisable(true);
            topicTranslateButton.setText("Traduire");
        }

        clearTranslationStatus(topicTranslationStatusLabel);
        hideTranslationBox(topicTranslationContainer);
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
            ForumCommentService.addComment(comment);
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
        if (topicTranslateButton != null) {
            topicTranslateButton.setDisable(false);
        }
        syncTopicTranslationSelection();

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

    private void syncTopicTranslationSelection() {
        if (topicTranslationContainer == null || topicTranslateButton == null) {
            return;
        }

        TranslationService.SupportedLanguage selectedLanguage = getSelectedTopicTranslationLanguage();
        if (currentTopic == null || selectedLanguage == null) {
            hideTranslationBox(topicTranslationContainer);
            activeTopicTranslationLanguageCode = null;
            updateTopicTranslateButtonLabel(false);
            return;
        }

        String translationKey = buildTopicTranslationKey(currentTopic.getId(), selectedLanguage.code());
        TranslationService.TopicTranslation cachedTranslation = topicTranslationCache.get(translationKey);
        if (cachedTranslation != null) {
            renderTopicTranslation(cachedTranslation);
            activeTopicTranslationLanguageCode = translationKey;
        } else {
            hideTranslationBox(topicTranslationContainer);
            activeTopicTranslationLanguageCode = null;
        }

        clearTranslationStatus(topicTranslationStatusLabel);
        updateTopicTranslateButtonLabel(false);
    }

    private TranslationService.SupportedLanguage getSelectedTopicTranslationLanguage() {
        if (topicTranslationLanguageComboBox == null) {
            return null;
        }
        TranslationService.SupportedLanguage selected = topicTranslationLanguageComboBox.getValue();
        if (selected != null) {
            return selected;
        }
        TranslationService.SupportedLanguage fallback = findTranslationLanguage(DEFAULT_TRANSLATION_LANGUAGE_CODE);
        topicTranslationLanguageComboBox.getSelectionModel().select(fallback);
        return fallback;
    }

    private void renderTopicTranslation(TranslationService.TopicTranslation translation) {
        if (topicTranslationContainer == null || translation == null) {
            return;
        }

        topicTranslationContainer.getChildren().clear();

        Label headerLabel = new Label("Traduction " + translation.languageLabel());
        headerLabel.getStyleClass().add("translation-card-header");
        headerLabel.setGraphic(createToolbarIcon(FontAwesomeSolid.GLOBE, "#4f46e5", 12));
        headerLabel.setContentDisplay(ContentDisplay.LEFT);
        headerLabel.setGraphicTextGap(7);

        if (translation.translatedTitle() != null && !translation.translatedTitle().isBlank()) {
            topicTranslationContainer.getChildren().add(createTranslationField(
                    "Titre traduit",
                    translation.translatedTitle(),
                    translation.rightToLeft(),
                    true
            ));
        }

        if (translation.translatedSummary() != null && !translation.translatedSummary().isBlank()) {
            topicTranslationContainer.getChildren().add(createTranslationField(
                    "Resume traduit",
                    translation.translatedSummary(),
                    translation.rightToLeft(),
                    false
            ));
        }

        if (translation.translatedContent() != null && !translation.translatedContent().isBlank()) {
            topicTranslationContainer.getChildren().add(createTranslationField(
                    "Contenu traduit",
                    translation.translatedContent(),
                    translation.rightToLeft(),
                    false
            ));
        }

        topicTranslationContainer.getChildren().add(0, headerLabel);
        if (translation.partialTranslation()) {
            topicTranslationContainer.getChildren().add(createTranslationNote(
                    "Les textes tres longs sont tronques avant l'appel a l'API pour garder une reponse fiable."
            ));
        }
        showTranslationBox(topicTranslationContainer);
    }

    private VBox createTranslationField(String fieldTitle, String text, boolean rightToLeft, boolean prominent) {
        VBox fieldBox = new VBox(5);
        fieldBox.getStyleClass().add("translation-field");

        Label fieldLabel = new Label(fieldTitle);
        fieldLabel.getStyleClass().add("translation-field-title");

        Label content = new Label(text);
        content.setWrapText(true);
        content.setMaxWidth(Double.MAX_VALUE);
        content.getStyleClass().add(prominent ? "translation-topic-title" : "translation-field-text");
        applyTranslatedTextOrientation(content, rightToLeft);

        fieldBox.getChildren().addAll(fieldLabel, content);
        return fieldBox;
    }

    private Label createTranslationNote(String text) {
        Label noteLabel = new Label(text);
        noteLabel.setWrapText(true);
        noteLabel.getStyleClass().add("translation-note");
        return noteLabel;
    }

    private void applyTranslatedTextOrientation(Label label, boolean rightToLeft) {
        if (label == null) {
            return;
        }

        label.setNodeOrientation(rightToLeft ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
        label.setAlignment(rightToLeft ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        label.setTextAlignment(rightToLeft ? TextAlignment.RIGHT : TextAlignment.LEFT);
    }

    private void setTopicTranslationLoading(boolean loading, String status, boolean error) {
        if (topicTranslationLanguageComboBox != null) {
            topicTranslationLanguageComboBox.setDisable(loading);
        }
        if (topicTranslateButton != null) {
            topicTranslateButton.setDisable(loading || currentTopic == null);
        }
        updateTopicTranslateButtonLabel(loading);
        if (status == null || status.isBlank()) {
            clearTranslationStatus(topicTranslationStatusLabel);
        } else {
            setTranslationStatus(topicTranslationStatusLabel, status, error);
        }
    }

    private void updateTopicTranslateButtonLabel(boolean loading) {
        if (topicTranslateButton == null) {
            return;
        }

        if (loading) {
            topicTranslateButton.setText("Traduction...");
            return;
        }

        boolean visible = topicTranslationContainer != null && topicTranslationContainer.isVisible();
        topicTranslateButton.setText(visible ? "Masquer traduction" : "Traduire");
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

        List<ForumComment> comments = ForumCommentService.findByTopicId(topicId, isAdmin());
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
        contentBox.getChildren().add(createCommentTranslationSection(comment));

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

    private VBox createCommentTranslationSection(ForumComment comment) {
        VBox section = new VBox(8);
        section.getStyleClass().add("comment-translation-section");

        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("translation-toolbar");

        Label toolbarTitle = new Label("Traduction");
        toolbarTitle.getStyleClass().addAll("translation-toolbar-title", "translation-toolbar-title-small");
        toolbarTitle.setGraphic(createToolbarIcon(FontAwesomeSolid.GLOBE, "#4f46e5", 11));
        toolbarTitle.setContentDisplay(ContentDisplay.LEFT);
        toolbarTitle.setGraphicTextGap(7);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ComboBox<TranslationService.SupportedLanguage> languageComboBox = new ComboBox<>(FXCollections.observableArrayList(translationLanguages));
        languageComboBox.getStyleClass().add("translation-language-combo");
        languageComboBox.setPrefWidth(160);
        configureTranslationLanguageComboBox(languageComboBox);

        Button translateButton = new Button("Traduire");
        translateButton.getStyleClass().addAll("translation-action-button", "translation-action-button-small");

        toolbar.getChildren().addAll(toolbarTitle, spacer, languageComboBox, translateButton);

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
        statusLabel.getStyleClass().add("translation-status");

        VBox translationBox = new VBox(10);
        translationBox.getStyleClass().addAll("translation-card", "translation-card-compact");
        hideTranslationBox(translationBox);

        languageComboBox.valueProperty().addListener((observable, oldValue, newValue) ->
                syncCommentTranslationSelection(comment.getId(), newValue, translationBox, translateButton, statusLabel));
        translateButton.setOnAction(event -> onCommentTranslateClick(comment, languageComboBox, translateButton, statusLabel, translationBox));

        TranslationService.SupportedLanguage selectedLanguage = findTranslationLanguage(
                selectedCommentLanguageByCommentId.getOrDefault(comment.getId(), DEFAULT_TRANSLATION_LANGUAGE_CODE)
        );
        languageComboBox.getSelectionModel().select(selectedLanguage);

        section.getChildren().addAll(toolbar, statusLabel, translationBox);
        return section;
    }

    private void configureTranslationLanguageComboBox(ComboBox<TranslationService.SupportedLanguage> comboBox) {
        if (comboBox == null) {
            return;
        }

        comboBox.setButtonCell(createTranslationLanguageCell());
        comboBox.setCellFactory(listView -> createTranslationLanguageCell());
    }

    private ListCell<TranslationService.SupportedLanguage> createTranslationLanguageCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(TranslationService.SupportedLanguage item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.displayLabel());
            }
        };
    }

    private void syncCommentTranslationSelection(int commentId,
                                                 TranslationService.SupportedLanguage language,
                                                 VBox translationBox,
                                                 Button translateButton,
                                                 Label statusLabel) {
        if (language == null) {
            hideTranslationBox(translationBox);
            activeCommentTranslationLanguageByCommentId.remove(commentId);
            translateButton.setText("Traduire");
            clearTranslationStatus(statusLabel);
            return;
        }

        selectedCommentLanguageByCommentId.put(commentId, language.code());
        String translationKey = buildCommentTranslationKey(commentId, language.code());
        TranslationService.CommentTranslation cachedTranslation = commentTranslationCache.get(translationKey);
        if (cachedTranslation != null) {
            renderCommentTranslation(translationBox, cachedTranslation);
            activeCommentTranslationLanguageByCommentId.put(commentId, translationKey);
            translateButton.setText("Masquer traduction");
        } else {
            hideTranslationBox(translationBox);
            activeCommentTranslationLanguageByCommentId.remove(commentId);
            translateButton.setText("Traduire");
        }
        clearTranslationStatus(statusLabel);
    }

    private void onCommentTranslateClick(ForumComment comment,
                                         ComboBox<TranslationService.SupportedLanguage> languageComboBox,
                                         Button translateButton,
                                         Label statusLabel,
                                         VBox translationBox) {
        TranslationService.SupportedLanguage selectedLanguage = languageComboBox.getValue();
        if (selectedLanguage == null) {
            setTranslationStatus(statusLabel, "Veuillez choisir une langue de traduction.", true);
            return;
        }

        String translationKey = buildCommentTranslationKey(comment.getId(), selectedLanguage.code());
        if (translationKey.equals(activeCommentTranslationLanguageByCommentId.get(comment.getId())) && translationBox.isVisible()) {
            hideTranslationBox(translationBox);
            activeCommentTranslationLanguageByCommentId.remove(comment.getId());
            translateButton.setText("Traduire");
            clearTranslationStatus(statusLabel);
            return;
        }

        TranslationService.CommentTranslation cachedTranslation = commentTranslationCache.get(translationKey);
        if (cachedTranslation != null) {
            renderCommentTranslation(translationBox, cachedTranslation);
            activeCommentTranslationLanguageByCommentId.put(comment.getId(), translationKey);
            translateButton.setText("Masquer traduction");
            clearTranslationStatus(statusLabel);
            return;
        }

        setInlineTranslationLoading(languageComboBox, translateButton, statusLabel, true, "Traduction en cours...", false);
        ForumComment commentSnapshot = comment;
        Task<TranslationService.CommentTranslation> task = new Task<>() {
            @Override
            protected TranslationService.CommentTranslation call() {
                return translationService.translateComment(commentSnapshot, selectedLanguage.code());
            }
        };

        task.setOnSucceeded(event -> {
            TranslationService.CommentTranslation translation = task.getValue();
            commentTranslationCache.put(translationKey, translation);
            activeCommentTranslationLanguageByCommentId.put(comment.getId(), translationKey);
            renderCommentTranslation(translationBox, translation);
            setInlineTranslationLoading(
                    languageComboBox,
                    translateButton,
                    statusLabel,
                    false,
                    translation.partialTranslation()
                            ? "Traduction partielle affichee pour ce texte long."
                            : "",
                    false
            );
            translateButton.setText("Masquer traduction");
        });

        task.setOnFailed(event -> setInlineTranslationLoading(
                languageComboBox,
                translateButton,
                statusLabel,
                false,
                formatTranslationError(task.getException()),
                true
        ));

        Thread translationThread = new Thread(task, "forum-comment-translation-" + comment.getId());
        translationThread.setDaemon(true);
        translationThread.start();
    }

    private void renderCommentTranslation(VBox translationBox, TranslationService.CommentTranslation translation) {
        translationBox.getChildren().clear();

        Label headerLabel = new Label("Traduction " + translation.languageLabel());
        headerLabel.getStyleClass().add("translation-card-header");
        headerLabel.setGraphic(createToolbarIcon(FontAwesomeSolid.GLOBE, "#4f46e5", 11));
        headerLabel.setContentDisplay(ContentDisplay.LEFT);
        headerLabel.setGraphicTextGap(6);

        Label translatedContent = new Label(translation.translatedContent());
        translatedContent.setWrapText(true);
        translatedContent.setMaxWidth(Double.MAX_VALUE);
        translatedContent.getStyleClass().add("translation-field-text");
        applyTranslatedTextOrientation(translatedContent, translation.rightToLeft());

        translationBox.getChildren().addAll(headerLabel, translatedContent);
        if (translation.partialTranslation()) {
            translationBox.getChildren().add(createTranslationNote(
                    "Les commentaires tres longs sont tronques avant la traduction."
            ));
        }
        showTranslationBox(translationBox);
    }

    private void setInlineTranslationLoading(ComboBox<TranslationService.SupportedLanguage> languageComboBox,
                                             Button translateButton,
                                             Label statusLabel,
                                             boolean loading,
                                             String status,
                                             boolean error) {
        languageComboBox.setDisable(loading);
        translateButton.setDisable(loading);
        translateButton.setText(loading ? "Traduction..." : "Traduire");
        if (status == null || status.isBlank()) {
            clearTranslationStatus(statusLabel);
        } else {
            setTranslationStatus(statusLabel, status, error);
        }
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
            ForumCommentService.addComment(reply);
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
        assistantInputArea.clear();
        clearAssistantConversation();
        appendChatMessage(createAssistantMessage("Assistant Medicare AI", buildAssistantWelcomeMessage(), buildWelcomeRecommendations()));
        assistantStatusLabel.setText("Verification de la configuration de l'API externe...");
        warmUpAssistant();
    }

    private String buildAssistantWelcomeMessage() {
        StringBuilder builder = new StringBuilder("Je suis votre assistant medical educatif pour ce sujet de forum, alimente par une API externe gratuite.");
        String quickSummary = currentTopic.getDisplaySummary();
        if (quickSummary != null && !quickSummary.isBlank()) {
            builder.append("\n\nResume rapide du sujet : ").append(quickSummary);
        }
        builder.append("\n\nJe peux ensuite : resumer plus finement la discussion, repondre a une question sante generale, proposer des conseils bien-etre prudents et recommander des sujets similaires.");
        builder.append("\n\nExemples : \"Resume ce sujet\", \"Quels sujets similaires me recommandes-tu ?\", \"Quels conseils simples pour mieux gerer le stress ?\"");
        builder.append("\n\nCes informations ne remplacent pas un avis medical professionnel.");
        return builder.toString();
    }

    private List<ChatAssistantRecommendation> buildWelcomeRecommendations() {
        List<ChatAssistantRecommendation> recommendations = new ArrayList<>();
        if (currentRelatedTopics == null) {
            return recommendations;
        }

        for (ForumTopic relatedTopic : currentRelatedTopics) {
            if (relatedTopic == null || relatedTopic.getTitle() == null || relatedTopic.getTitle().isBlank()) {
                continue;
            }
            recommendations.add(new ChatAssistantRecommendation(relatedTopic.getId(), relatedTopic.getTitle()));
            if (recommendations.size() >= 2) {
                break;
            }
        }
        return recommendations;
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
            ForumCommentService.setCommentReported(comment.getId(), newState, newState ? user.getId() : null);
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
            ForumCommentService.deleteComment(commentId);
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

    private void resetTranslationState() {
        topicTranslationCache.clear();
        commentTranslationCache.clear();
        activeCommentTranslationLanguageByCommentId.clear();
        selectedCommentLanguageByCommentId.clear();
        activeTopicTranslationLanguageCode = null;
        clearTranslationStatus(topicTranslationStatusLabel);
        hideTranslationBox(topicTranslationContainer);
        if (topicTranslationLanguageComboBox != null) {
            topicTranslationLanguageComboBox.getSelectionModel().select(findTranslationLanguage(DEFAULT_TRANSLATION_LANGUAGE_CODE));
            topicTranslationLanguageComboBox.setDisable(false);
        }
        if (topicTranslateButton != null) {
            topicTranslateButton.setDisable(true);
            topicTranslateButton.setText("Traduire");
        }
    }

    private TranslationService.SupportedLanguage findTranslationLanguage(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return translationLanguages.isEmpty() ? null : translationLanguages.get(0);
        }

        for (TranslationService.SupportedLanguage language : translationLanguages) {
            if (language != null && language.code().equalsIgnoreCase(languageCode)) {
                return language;
            }
        }
        return translationLanguages.isEmpty() ? null : translationLanguages.get(0);
    }

    private String buildTopicTranslationKey(int translatedTopicId, String languageCode) {
        return "topic:" + translatedTopicId + ":" + languageCode;
    }

    private String buildCommentTranslationKey(int commentId, String languageCode) {
        return "comment:" + commentId + ":" + languageCode;
    }

    private FontIcon createToolbarIcon(FontAwesomeSolid iconType, String color, int size) {
        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(size);
        icon.setIconColor(Color.web(color));
        return icon;
    }

    private void showTranslationBox(VBox translationBox) {
        if (translationBox == null) {
            return;
        }
        translationBox.setVisible(true);
        translationBox.setManaged(true);
    }

    private void hideTranslationBox(VBox translationBox) {
        if (translationBox == null) {
            return;
        }
        translationBox.setVisible(false);
        translationBox.setManaged(false);
    }

    private void setTranslationStatus(Label statusLabel, String message, boolean error) {
        if (statusLabel == null) {
            return;
        }

        statusLabel.setText(message != null ? message : "");
        statusLabel.setVisible(message != null && !message.isBlank());
        statusLabel.setManaged(statusLabel.isVisible());
        statusLabel.getStyleClass().remove("translation-status-error");
        if (error && !statusLabel.getStyleClass().contains("translation-status-error")) {
            statusLabel.getStyleClass().add("translation-status-error");
        }
    }

    private void clearTranslationStatus(Label statusLabel) {
        if (statusLabel == null) {
            return;
        }

        statusLabel.setText("");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
        statusLabel.getStyleClass().remove("translation-status-error");
    }

    private String formatTranslationError(Throwable throwable) {
        if (throwable != null && throwable.getMessage() != null && !throwable.getMessage().isBlank()) {
            return throwable.getMessage();
        }
        return "La traduction externe est indisponible pour le moment. Verifiez votre connexion et reessayez.";
    }

    private void setAssistantLoading(boolean loading, String status) {
        assistantRequestInProgress = loading;
        assistantInputArea.setDisable(loading);
        assistantSendButton.setDisable(loading || currentTopic == null);
        assistantSendButton.setText(loading ? "Envoi..." : "Envoyer");
        if (assistantLoadingIndicator != null) {
            assistantLoadingIndicator.setVisible(loading);
            assistantLoadingIndicator.setManaged(loading);
        }
        assistantStatusLabel.setText(status);
    }

    private ChatMessage createUserMessage(String message) {
        ChatMessage chatMessage = new ChatMessage(ChatMessage.ROLE_USER, "Vous", message);
        chatMessage.setCreatedAt(LocalDateTime.now());
        return chatMessage;
    }

    private ChatMessage createAssistantMessage(String author, String message, List<ChatAssistantRecommendation> recommendations) {
        ChatMessage chatMessage = new ChatMessage(ChatMessage.ROLE_ASSISTANT, author, message);
        chatMessage.setCreatedAt(LocalDateTime.now());
        chatMessage.setRecommendations(recommendations);
        return chatMessage;
    }

    private ChatMessage createTypingMessage() {
        ChatMessage chatMessage = new ChatMessage(ChatMessage.ROLE_ASSISTANT, "Assistant Medicare AI", "Assistant en train d'ecrire");
        chatMessage.setCreatedAt(LocalDateTime.now());
        chatMessage.setTyping(true);
        return chatMessage;
    }

    private ChatMessage createErrorMessage(String message) {
        ChatMessage chatMessage = createAssistantMessage("Assistant Medicare AI", message, List.of());
        chatMessage.setError(true);
        return chatMessage;
    }

    private void appendChatMessage(ChatMessage chatMessage) {
        assistantConversation.add(chatMessage);
        HBox row = buildChatRow(chatMessage);
        assistantMessagesContainer.getChildren().add(row);
        scrollAssistantToBottom();
    }

    private HBox buildChatRow(ChatMessage chatMessage) {
        boolean userMessage = chatMessage.isUser();

        HBox row = new HBox(12);
        row.setAlignment(Pos.TOP_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        row.getStyleClass().addAll("chat-row", userMessage ? "chat-row-user" : "chat-row-assistant");

        Label avatar = new Label(userMessage ? "U" : "AI");
        avatar.getStyleClass().addAll("assistant-avatar", userMessage ? "assistant-avatar-user" : "assistant-avatar-ai");

        VBox bubble = new VBox(10);
        bubble.setMinWidth(Region.USE_PREF_SIZE);
        bubble.setFillWidth(true);
        bubble.getStyleClass().add("chat-bubble");
        applyAdaptiveBubbleWidth(bubble);
        if (chatMessage.isError()) {
            bubble.getStyleClass().add("chat-bubble-error");
        } else if (userMessage) {
            bubble.getStyleClass().add("chat-bubble-user");
        } else {
            bubble.getStyleClass().add("chat-bubble-assistant");
        }

        Label authorLabel = new Label(chatMessage.getAuthor() != null ? chatMessage.getAuthor() : (userMessage ? "Vous" : "Assistant"));
        authorLabel.getStyleClass().addAll("chat-author", userMessage ? "chat-author-user" : "chat-author-assistant");

        Node messageNode = buildChatMessageNode(chatMessage, userMessage);
        VBox.setMargin(messageNode, new Insets(2, 0, 0, 0));

        Label timeLabel = new Label((chatMessage.getCreatedAt() != null ? chatMessage.getCreatedAt() : LocalDateTime.now()).format(assistantTimeFormatter));
        timeLabel.getStyleClass().add("chat-time");

        bubble.getChildren().addAll(authorLabel, messageNode);

        if (chatMessage.getRecommendations() != null && !chatMessage.getRecommendations().isEmpty()) {
            Label recommendationsLabel = new Label("Sujets suggeres");
            recommendationsLabel.getStyleClass().add("chat-recommendations-title");

            FlowPane recommendationsPane = new FlowPane(10, 10);
            recommendationsPane.getStyleClass().add("chat-recommendations-pane");
            recommendationsPane.prefWrapLengthProperty().bind(Bindings.createDoubleBinding(
                    () -> Math.max(220.0d, bubble.getMaxWidth() - 40.0d),
                    bubble.maxWidthProperty()
            ));
            for (ChatAssistantRecommendation recommendation : chatMessage.getRecommendations()) {
                if (recommendation.getTitle() == null || recommendation.getTitle().isBlank()) {
                    continue;
                }

                Button recommendationButton = new Button(recommendation.getTitle());
                recommendationButton.getStyleClass().add("chat-recommendation-button");
                recommendationButton.setDisable(recommendation.getId() <= 0 || recommendation.getId() == topicId);
                recommendationButton.setOnAction(event -> openForumDetail(recommendation.getId()));
                recommendationsPane.getChildren().add(recommendationButton);
            }

            if (!recommendationsPane.getChildren().isEmpty()) {
                VBox.setMargin(recommendationsLabel, new Insets(8, 0, 0, 0));
                bubble.getChildren().addAll(recommendationsLabel, recommendationsPane);
            }
        }

        VBox.setMargin(timeLabel, new Insets(6, 0, 0, 0));
        bubble.getChildren().add(timeLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (userMessage) {
            row.getChildren().addAll(spacer, bubble, avatar);
        } else {
            row.getChildren().addAll(avatar, bubble, spacer);
        }

        row.setOpacity(0.0d);
        row.setTranslateY(10.0d);
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(220), row);
        fadeTransition.setFromValue(0.0d);
        fadeTransition.setToValue(1.0d);

        TranslateTransition slideTransition = new TranslateTransition(Duration.millis(260), row);
        slideTransition.setFromY(10.0d);
        slideTransition.setToY(0.0d);
        slideTransition.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition appearanceTransition = new ParallelTransition(fadeTransition, slideTransition);
        appearanceTransition.setOnFinished(event -> scrollAssistantToBottom());
        appearanceTransition.play();

        if (chatMessage.isTyping() && messageNode instanceof Label messageLabel) {
            assistantTypingLabel = messageLabel;
            assistantTypingRow = row;
        }

        return row;
    }

    private Node buildChatMessageNode(ChatMessage chatMessage, boolean userMessage) {
        String content = chatMessage.getContent() != null ? chatMessage.getContent() : "";
        if (userMessage || chatMessage.isTyping() || chatMessage.isError()) {
            Label messageLabel = createWrappedChatLabel(content);
            messageLabel.getStyleClass().add("chat-message");
            if (chatMessage.isError()) {
                messageLabel.getStyleClass().add("chat-message-error");
            } else if (chatMessage.isTyping()) {
                messageLabel.getStyleClass().add("chat-message-typing");
            } else if (userMessage) {
                messageLabel.getStyleClass().add("chat-message-user");
            } else {
                messageLabel.getStyleClass().add("chat-message-assistant");
            }
            return messageLabel;
        }
        return buildFormattedAssistantContent(content);
    }

    private Label createWrappedChatLabel(String text) {
        Label label = new Label(text != null ? text : "");
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private VBox buildFormattedAssistantContent(String rawContent) {
        VBox contentBox = new VBox(12);
        contentBox.setFillWidth(true);
        contentBox.setMaxWidth(Double.MAX_VALUE);
        contentBox.getStyleClass().add("chat-markdown-content");

        String normalizedContent = rawContent != null
                ? rawContent.replace("\r\n", "\n").replace('\r', '\n').replace("\u2022 ", "* ")
                : "";
        List<String> paragraphLines = new ArrayList<>();
        List<String> listItems = new ArrayList<>();

        for (String rawLine : normalizedContent.split("\n", -1)) {
            String trimmedLine = rawLine.trim();

            if (trimmedLine.isEmpty()) {
                flushAssistantParagraph(contentBox, paragraphLines);
                flushAssistantList(contentBox, listItems);
                continue;
            }

            if (isMarkdownSeparator(trimmedLine)) {
                flushAssistantParagraph(contentBox, paragraphLines);
                flushAssistantList(contentBox, listItems);
                contentBox.getChildren().add(createMarkdownSeparator());
                continue;
            }

            String headingText = extractMarkdownHeading(trimmedLine);
            if (headingText != null) {
                flushAssistantParagraph(contentBox, paragraphLines);
                flushAssistantList(contentBox, listItems);
                contentBox.getChildren().add(createMarkdownTitleLabel(headingText));
                continue;
            }

            String listItemText = extractMarkdownListItem(trimmedLine);
            if (listItemText != null) {
                flushAssistantParagraph(contentBox, paragraphLines);
                listItems.add(listItemText);
                continue;
            }

            flushAssistantList(contentBox, listItems);
            paragraphLines.add(cleanInlineMarkdown(rawLine));
        }

        flushAssistantParagraph(contentBox, paragraphLines);
        flushAssistantList(contentBox, listItems);

        if (contentBox.getChildren().isEmpty()) {
            Label fallbackLabel = createWrappedChatLabel(cleanInlineMarkdown(normalizedContent));
            fallbackLabel.getStyleClass().addAll("chat-message", "chat-message-assistant", "chat-markdown-text");
            contentBox.getChildren().add(fallbackLabel);
        }

        return contentBox;
    }

    private void flushAssistantParagraph(VBox contentBox, List<String> paragraphLines) {
        if (paragraphLines.isEmpty()) {
            return;
        }

        String paragraphText = String.join("\n", paragraphLines).trim();
        paragraphLines.clear();
        if (paragraphText.isEmpty()) {
            return;
        }

        Label paragraphLabel = createWrappedChatLabel(paragraphText);
        paragraphLabel.getStyleClass().addAll("chat-message", "chat-message-assistant", "chat-markdown-text");
        contentBox.getChildren().add(paragraphLabel);
    }

    private void flushAssistantList(VBox contentBox, List<String> listItems) {
        if (listItems.isEmpty()) {
            return;
        }

        VBox listBox = new VBox(8);
        listBox.setFillWidth(true);
        listBox.getStyleClass().add("chat-markdown-list");

        for (String itemText : listItems) {
            HBox itemRow = new HBox(8);
            itemRow.setAlignment(Pos.TOP_LEFT);
            itemRow.getStyleClass().add("chat-markdown-list-item");

            Label bulletLabel = new Label("\u2022");
            bulletLabel.getStyleClass().add("chat-markdown-bullet");

            Label itemLabel = createWrappedChatLabel(itemText);
            itemLabel.setMaxWidth(Double.MAX_VALUE);
            itemLabel.getStyleClass().addAll("chat-message", "chat-markdown-list-text");
            HBox.setHgrow(itemLabel, Priority.ALWAYS);

            itemRow.getChildren().addAll(bulletLabel, itemLabel);
            listBox.getChildren().add(itemRow);
        }

        listItems.clear();
        contentBox.getChildren().add(listBox);
    }

    private Label createMarkdownTitleLabel(String text) {
        Label titleLabel = createWrappedChatLabel(text);
        titleLabel.getStyleClass().add("chat-markdown-title");
        return titleLabel;
    }

    private Region createMarkdownSeparator() {
        Region separator = new Region();
        separator.setMinHeight(1);
        separator.setPrefHeight(1);
        separator.setMaxHeight(1);
        separator.setMaxWidth(Double.MAX_VALUE);
        separator.getStyleClass().add("chat-markdown-separator");
        VBox.setMargin(separator, new Insets(6, 0, 6, 0));
        return separator;
    }

    private boolean isMarkdownSeparator(String line) {
        String compact = line.replace(" ", "");
        if (compact.length() < 3) {
            return false;
        }

        char marker = compact.charAt(0);
        if (marker != '-' && marker != '*' && marker != '_') {
            return false;
        }

        for (int i = 1; i < compact.length(); i++) {
            if (compact.charAt(i) != marker) {
                return false;
            }
        }
        return true;
    }

    private String extractMarkdownHeading(String line) {
        int markerCount = 0;
        while (markerCount < line.length() && line.charAt(markerCount) == '#') {
            markerCount++;
        }

        if (markerCount == 0 || markerCount > 6 || markerCount >= line.length() || !Character.isWhitespace(line.charAt(markerCount))) {
            return null;
        }

        String heading = line.substring(markerCount).trim();
        while (!heading.isEmpty() && heading.charAt(heading.length() - 1) == '#') {
            heading = heading.substring(0, heading.length() - 1).trim();
        }

        heading = cleanInlineMarkdown(heading);
        return heading.isEmpty() ? null : heading;
    }

    private String extractMarkdownListItem(String line) {
        if (line.startsWith("- ") || line.startsWith("* ") || line.startsWith("â€¢ ")) {
            String item = cleanInlineMarkdown(line.substring(2).trim());
            return item.isEmpty() ? null : item;
        }

        int cursor = 0;
        while (cursor < line.length() && Character.isDigit(line.charAt(cursor))) {
            cursor++;
        }

        if (cursor > 0
                && cursor + 1 < line.length()
                && (line.charAt(cursor) == '.' || line.charAt(cursor) == ')')
                && Character.isWhitespace(line.charAt(cursor + 1))) {
            String item = cleanInlineMarkdown(line.substring(cursor + 1).trim());
            return item.isEmpty() ? null : item;
        }

        return null;
    }

    private String cleanInlineMarkdown(String text) {
        if (text == null) {
            return "";
        }

        String cleaned = text.replace('\t', ' ').trim();
        if (cleaned.startsWith(">")) {
            cleaned = cleaned.substring(1).trim();
        }

        cleaned = cleaned
                .replace("**", "")
                .replace("__", "")
                .replace("~~", "")
                .replace("`", "")
                .replace("*", "");

        while (cleaned.contains("  ")) {
            cleaned = cleaned.replace("  ", " ");
        }

        return cleaned.trim();
    }

    private void applyAdaptiveBubbleWidth(VBox bubble) {
        if (bubble == null) {
            return;
        }

        if (assistantMessagesContainer == null) {
            bubble.setMaxWidth(520);
            return;
        }

        bubble.maxWidthProperty().bind(Bindings.createDoubleBinding(
                () -> {
                    double containerWidth = assistantMessagesContainer.getWidth();
                    if (containerWidth <= 0) {
                        return 520.0d;
                    }

                    double responsiveWidth = containerWidth * 0.76d;
                    return Math.max(220.0d, Math.min(520.0d, responsiveWidth));
                },
                assistantMessagesContainer.widthProperty()
        ));
    }

    private void showTypingIndicator() {
        stopTypingIndicator();
        assistantTypingFrameIndex = 0;
        assistantTypingMessage = createTypingMessage();
        appendChatMessage(assistantTypingMessage);
        assistantTypingTimeline = new Timeline(new KeyFrame(Duration.millis(450), event -> advanceTypingFrame()));
        assistantTypingTimeline.setCycleCount(Timeline.INDEFINITE);
        assistantTypingTimeline.play();
    }

    private void advanceTypingFrame() {
        if (assistantTypingLabel == null) {
            return;
        }

        String[] frames = {
                "Assistant en train d'ecrire",
                "Assistant en train d'ecrire.",
                "Assistant en train d'ecrire..",
                "Assistant en train d'ecrire..."
        };
        assistantTypingLabel.setText(frames[assistantTypingFrameIndex % frames.length]);
        assistantTypingFrameIndex++;
    }

    private void stopTypingIndicator() {
        if (assistantTypingTimeline != null) {
            assistantTypingTimeline.stop();
            assistantTypingTimeline = null;
        }

        if (assistantTypingMessage != null) {
            assistantConversation.remove(assistantTypingMessage);
        }
        if (assistantTypingRow != null) {
            assistantMessagesContainer.getChildren().remove(assistantTypingRow);
        }

        assistantTypingLabel = null;
        assistantTypingRow = null;
        assistantTypingMessage = null;
        assistantTypingFrameIndex = 0;
    }

    private void clearAssistantConversation() {
        stopTypingIndicator();
        assistantConversation.clear();
        if (assistantMessagesContainer != null) {
            assistantMessagesContainer.getChildren().clear();
        }
    }

    private void scrollAssistantToBottom() {
        Platform.runLater(() -> {
            if (assistantMessagesScrollPane != null && assistantMessagesContainer != null) {
                assistantMessagesContainer.applyCss();
                assistantMessagesContainer.layout();
                assistantMessagesScrollPane.layout();
                assistantMessagesScrollPane.setVvalue(1.0);
                Platform.runLater(() -> assistantMessagesScrollPane.setVvalue(1.0));
            } else if (assistantMessagesScrollPane != null) {
                assistantMessagesScrollPane.setVvalue(1.0);
            }
        });
    }

    private String formatAssistantError(Throwable throwable) {
        if (throwable != null && throwable.getMessage() != null && !throwable.getMessage().isBlank()) {
            return throwable.getMessage();
        }
        return "L'assistant externe est indisponible pour le moment. Verifiez votre connexion reseau et la configuration de l'API.";
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
            if (!assistantRequestInProgress && currentTopic != null && "Verification de la configuration de l'API externe...".equals(assistantStatusLabel.getText())) {
                assistantStatusLabel.setText("Assistant pret. Essayez par exemple : Resume ce sujet.");
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
            assistantInputArea.setDisable(false);
            assistantSendButton.setDisable(false);
            if (!assistantRequestInProgress) {
                assistantStatusLabel.setText("Assistant pret. Mode local disponible si besoin.");
            }
        });

        task.setOnFailed(event -> {
            assistantWarmupTopicId = -1;
            assistantInputArea.setDisable(false);
            assistantSendButton.setDisable(false);
            if (!assistantRequestInProgress) {
                assistantStatusLabel.setText("Mode local actif.");
            }
            appendChatMessage(createAssistantMessage(
                    "Assistant Medicare AI",
                    "Le mode local Medicare prend le relais. Vous pouvez continuer a poser des questions sur le sujet, demander un resume ou chercher des sujets similaires.\n\nCes informations ne remplacent pas un avis medical professionnel.",
                    buildWelcomeRecommendations()
            ));
        });

        Thread warmupThread = new Thread(task, "forum-chat-assistant-warmup");
        warmupThread.setDaemon(true);
        warmupThread.start();
    }

}

