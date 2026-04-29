package com.medicare.controllers;

import com.medicare.HelloApplication;
import com.medicare.models.ForumComment;
import com.medicare.models.ForumModerationItem;
import com.medicare.models.ForumTopic;
import com.medicare.models.User;
import com.medicare.services.CommentService;
import com.medicare.services.ForumDashboardService;
import com.medicare.services.ForumService;
import com.medicare.services.NotificationService;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DashboardAdminController {

    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;
    @FXML private Label avatarInitialsLabel;
    @FXML private Label roleBadgeLabel;
    @FXML private VBox profileCard;
    @FXML private StackPane contentArea;
    @FXML private ScrollPane adminHomeScrollPane;
    @FXML private VBox dashboardForumContent;
    @FXML private Label forumDashboardStatusLabel;
    @FXML private Label totalTopicsLabel;
    @FXML private Label totalCommentsLabel;
    @FXML private Label reportedTopicsLabel;
    @FXML private Label reportedCommentsLabel;
    @FXML private Label hiddenTopicsLabel;
    @FXML private VBox totalTopicsCard;
    @FXML private VBox totalCommentsCard;
    @FXML private VBox reportedTopicsCard;
    @FXML private VBox reportedCommentsCard;
    @FXML private VBox hiddenTopicsCard;
    @FXML private VBox topTopicsPanel;
    @FXML private VBox recentModerationPanel;
    @FXML private VBox allTopicsPanel;
    @FXML private TableView<ForumTopic> topTopicsTable;
    @FXML private TableColumn<ForumTopic, String> topTopicsTitleColumn;
    @FXML private TableColumn<ForumTopic, String> topTopicsAuthorColumn;
    @FXML private TableColumn<ForumTopic, String> topTopicsCountColumn;
    @FXML private TableView<ForumModerationItem> recentModerationTable;
    @FXML private TableColumn<ForumModerationItem, String> moderationTypeColumn;
    @FXML private TableColumn<ForumModerationItem, String> moderationContentColumn;
    @FXML private TableColumn<ForumModerationItem, String> moderationAuthorColumn;
    @FXML private TableColumn<ForumModerationItem, String> moderationStatusColumn;
    @FXML private TableColumn<ForumModerationItem, String> moderationDateColumn;
    @FXML private TextField topicSearchField;
    @FXML private ComboBox<String> topicSortComboBox;
    @FXML private TableView<ForumTopic> allTopicsTable;
    @FXML private TableColumn<ForumTopic, String> allTopicsIdColumn;
    @FXML private TableColumn<ForumTopic, String> allTopicsTitleColumn;
    @FXML private TableColumn<ForumTopic, String> allTopicsAuthorColumn;
    @FXML private TableColumn<ForumTopic, String> allTopicsTypeColumn;
    @FXML private TableColumn<ForumTopic, String> allTopicsCommentCountColumn;
    @FXML private TableColumn<ForumTopic, String> allTopicsReportedColumn;
    @FXML private TableColumn<ForumTopic, String> allTopicsHiddenColumn;
    @FXML private TableColumn<ForumTopic, String> allTopicsCreatedAtColumn;

    @FXML private Button btnAccueil;
    @FXML private Button btnUtilisateurs;
    @FXML private Button btnRendezVous;
    @FXML private Button btnDonation;
    @FXML private Button btnProduit;
    @FXML private Button btnCollaboration;
    @FXML private Button btnForum;
    @FXML private Button btnNotifications;
    @FXML private Button btnLogout;

    private static User currentUser;
    private final ForumService forumService = new ForumService();
    private final CommentService commentService = new CommentService();
    private final ForumDashboardService forumDashboardService = new ForumDashboardService();
    private final NotificationService notificationService = new NotificationService();
    private final DateTimeFormatter dashboardDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private Node adminHomeView;
    private boolean dashboardIntroPlayed;
    private Timeline notificationBadgeTimeline;

    public static void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser() { return currentUser; }

    private Button[] allButtons() {
        return new Button[]{btnAccueil, btnUtilisateurs, btnRendezVous,
                            btnDonation, btnProduit, btnCollaboration, btnForum, btnNotifications};
    }

    @FXML
    private void initialize() {
        if (currentUser != null) {
            configureProfile(currentUser, "System Administrator", "Admin", "sidebar-role-admin", "");
        }

        btnAccueil.setGraphic(icon(FontAwesomeSolid.HOME));
        btnUtilisateurs.setGraphic(icon(FontAwesomeSolid.USERS));
        btnRendezVous.setGraphic(icon(FontAwesomeSolid.CALENDAR_ALT));
        btnDonation.setGraphic(icon(FontAwesomeSolid.HEART));
        btnProduit.setGraphic(icon(FontAwesomeSolid.SHOPPING_CART));
        btnCollaboration.setGraphic(icon(FontAwesomeSolid.HANDSHAKE));
        btnForum.setGraphic(icon(FontAwesomeSolid.COMMENTS));
        btnNotifications.setGraphic(icon(FontAwesomeSolid.BELL));
        btnLogout.setGraphic(icon(FontAwesomeSolid.SIGN_OUT_ALT, Color.web("#fecaca")));
        refreshNotificationBadge();
        startNotificationBadgeAutoRefresh();

        adminHomeView = adminHomeScrollPane;
        configureForumDashboard();
        configureForumDashboardAnimations();
        highlightButton(btnAccueil);
        showAccueilDashboard();
    }

    private void configureProfile(User user, String roleText, String badgeText, String badgeClass, String namePrefix) {
        String fullName = fullName(user, namePrefix);
        userNameLabel.setText(fullName);
        userEmailLabel.setText(roleText);
        avatarInitialsLabel.setText(initials(user));
        roleBadgeLabel.setText(badgeText);
        roleBadgeLabel.getStyleClass().setAll("sidebar-role-badge", badgeClass);
        playProfileIntro();
    }

    private String fullName(User user, String prefix) {
        String name = ((user.getPrenom() != null ? user.getPrenom() : "") + " " +
                (user.getNom() != null ? user.getNom() : "")).trim();
        if (name.isEmpty()) {
            name = "Utilisateur";
        }
        return prefix == null || prefix.isBlank() ? name : prefix + name;
    }

    private String initials(User user) {
        String first = firstInitial(user.getPrenom());
        String second = firstInitial(user.getNom());
        String value = (first + second).trim();
        return value.isEmpty() ? "U" : value.toUpperCase();
    }

    private String firstInitial(String value) {
        return value == null || value.isBlank() ? "" : value.trim().substring(0, 1);
    }

    private void playProfileIntro() {
        if (profileCard == null) {
            return;
        }
        profileCard.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(300), profileCard);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
        profileCard.setOnMouseEntered(event -> animateCardScale(profileCard, 1.02));
        profileCard.setOnMouseExited(event -> animateCardScale(profileCard, 1.0));
    }

    private FontIcon icon(FontAwesomeSolid type) { return icon(type, Color.WHITE); }

    private FontIcon icon(FontAwesomeSolid type, Color color) {
        FontIcon fi = new FontIcon(type);
        fi.setIconSize(16);
        fi.setIconColor(color);
        return fi;
    }

    // ========== NAVIGATION ==========

    @FXML private void onAccueilClick() {
        highlightButton(btnAccueil);
        showAccueilDashboard();
    }

    @FXML private void onUtilisateursClick() {
        highlightButton(btnUtilisateurs);
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("admin-users-view.fxml"));
            Node view = loader.load();
            setContent(view);
        } catch (Exception e) {
            e.printStackTrace();
            setContent(new Label("Gestion des utilisateurs (a venir)") {{
                setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
            }});
        }
    }

    @FXML private void onRendezVousClick() {
        highlightButton(btnRendezVous);
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("admin-rdv-list-view.fxml"));
            Node view = loader.load();
            setContent(view);
        } catch (Exception e) {
            e.printStackTrace();
            setContent(new Label("Erreur chargement rendez-vous") {{
                setStyle("-fx-font-size: 16px; -fx-text-fill: #dc2626;");
            }});
        }
    }

    @FXML private void onDonationClick() {
        highlightButton(btnDonation);
        setContent(new Label("Gestion des Donations") {{
            setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
        }});
    }

    @FXML private void onProduitClick() {
        highlightButton(btnProduit);
        setContent(new Label("Gestion des Produits") {{
            setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
        }});
    }

    @FXML private void onCollaborationClick() {
        highlightButton(btnCollaboration);
        setContent(new Label("Gestion des Collaborations") {{
            setStyle("-fx-font-size: 20px; -fx-text-fill: #333;");
        }});
    }

    @FXML private void onForumClick() {
        highlightButton(btnForum);
        refreshNotificationBadge();
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("forum-list-view.fxml"));
            Node view = loader.load();
            ForumListController controller = loader.getController();
            controller.setForumContext(contentArea, currentUser);
            setContent(view);
        } catch (Exception e) {
            e.printStackTrace();
            setContent(new Label("Erreur chargement forum") {{
                setStyle("-fx-font-size: 16px; -fx-text-fill: #dc2626;");
            }});
        }
    }

    @FXML private void onNotificationsClick() {
        highlightButton(btnNotifications);
        refreshNotificationBadge();
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("notification-list-view.fxml"));
            Node view = loader.load();
            NotificationController controller = loader.getController();
            controller.setNotificationChangeListener(this::refreshNotificationBadge);
            controller.setNotificationContext(contentArea, currentUser);
            setContent(view);
            refreshNotificationBadge();
        } catch (Exception e) {
            e.printStackTrace();
            setContent(new Label("Erreur chargement notifications") {{
                setStyle("-fx-font-size: 16px; -fx-text-fill: #dc2626;");
            }});
        }
    }

    @FXML private void onLogoutClick() {
        stopNotificationBadgeAutoRefresh();
        currentUser = null;
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("accueil-view.fxml"));
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Medicare");
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ========== UTILITAIRES ==========

    private void setContent(Node node) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(node);
    }

    private void showAccueilDashboard() {
        if (adminHomeView != null) {
            setContent(adminHomeView);
        }
        loadForumDashboard();
        playDashboardIntro();
        refreshNotificationBadge();
    }

    private void refreshNotificationBadge() {
        if (btnNotifications == null || currentUser == null) {
            return;
        }
        try {
            int unread = notificationService.countUnreadByUserId(currentUser.getId());
            btnNotifications.setText(unread > 0 ? "  Notifications (" + unread + ")" : "  Notifications");
        } catch (Exception e) {
            btnNotifications.setText("  Notifications");
            System.err.println("Impossible de charger le compteur notifications admin: " + e.getMessage());
        }
    }

    private void startNotificationBadgeAutoRefresh() {
        stopNotificationBadgeAutoRefresh();
        notificationBadgeTimeline = new Timeline(
                new KeyFrame(Duration.seconds(5), event -> refreshNotificationBadge())
        );
        notificationBadgeTimeline.setCycleCount(Animation.INDEFINITE);
        notificationBadgeTimeline.play();
    }

    private void stopNotificationBadgeAutoRefresh() {
        if (notificationBadgeTimeline != null) {
            notificationBadgeTimeline.stop();
            notificationBadgeTimeline = null;
        }
    }

    private void configureForumDashboard() {
        topTopicsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        recentModerationTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        allTopicsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        topTopicsTitleColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(safeText(cellData.getValue().getTitle(), "Sujet sans titre")));
        topTopicsAuthorColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(displayAuthorName(cellData.getValue().getAuthorName())));
        topTopicsCountColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(String.valueOf(cellData.getValue().getCommentCount())));

        moderationTypeColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue().getTypeLabel()));
        moderationContentColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue().getContentLabel()));
        moderationAuthorColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue().getAuthorLabel()));
        moderationStatusColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue().getStatusLabel()));
        moderationDateColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue().getDateLabel()));

        topTopicsTable.setPlaceholder(new Label("Aucun sujet forum a afficher."));
        recentModerationTable.setPlaceholder(new Label("Aucun contenu modere recent."));
        configureAllTopicsTable();
    }

    private void configureAllTopicsTable() {
        topicSortComboBox.setItems(FXCollections.observableArrayList(
                ForumDashboardService.SORT_RECENT,
                ForumDashboardService.SORT_OLDEST,
                ForumDashboardService.SORT_MOST_COMMENTED,
                ForumDashboardService.SORT_REPORTED_FIRST,
                ForumDashboardService.SORT_HIDDEN_FIRST,
                ForumDashboardService.SORT_ARTICLE,
                ForumDashboardService.SORT_VIDEO
        ));
        topicSortComboBox.getSelectionModel().select(ForumDashboardService.SORT_RECENT);
        topicSortComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshAllTopicsTable());
        topicSearchField.textProperty().addListener((observable, oldValue, newValue) -> refreshAllTopicsTable());

        allTopicsIdColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(String.valueOf(cellData.getValue().getId())));
        allTopicsTitleColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(safeText(cellData.getValue().getTitle(), "Sujet sans titre")));
        allTopicsAuthorColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(displayAuthorName(cellData.getValue().getAuthorName())));
        allTopicsTypeColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue().getDisplayType()));
        allTopicsCommentCountColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(String.valueOf(cellData.getValue().getCommentCount())));
        allTopicsReportedColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue().isReported() ? "Oui" : "Non"));
        allTopicsHiddenColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue().isHidden() ? "Oui" : "Non"));
        allTopicsCreatedAtColumn.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(formatDashboardDate(cellData.getValue().getCreatedAt())));

        allTopicsTypeColumn.setCellFactory(column -> badgeCell(
                value -> "Video".equalsIgnoreCase(value) ? "#ffedd5" : "#dbeafe",
                value -> "Video".equalsIgnoreCase(value) ? "#c2410c" : "#1d4ed8"
        ));
        allTopicsCommentCountColumn.setCellFactory(column -> badgeCell(
                value -> "#eef2ff",
                value -> "#4338ca"
        ));
        allTopicsReportedColumn.setCellFactory(column -> badgeCell(
                value -> "Oui".equalsIgnoreCase(value) ? "#ffedd5" : "#ecfdf5",
                value -> "Oui".equalsIgnoreCase(value) ? "#c2410c" : "#047857"
        ));
        allTopicsHiddenColumn.setCellFactory(column -> badgeCell(
                value -> "Oui".equalsIgnoreCase(value) ? "#e2e8f0" : "#f0f9ff",
                value -> "Oui".equalsIgnoreCase(value) ? "#334155" : "#0369a1"
        ));

        allTopicsTable.setPlaceholder(new Label("Aucun sujet ne correspond a la recherche."));
    }

    private void configureForumDashboardAnimations() {
        for (Node card : dashboardCards()) {
            installCardHover(card);
        }
        installPanelHover(allTopicsPanel);
    }

    private void loadForumDashboard() {
        try {
            totalTopicsLabel.setText(String.valueOf(forumService.countTopics()));
            totalCommentsLabel.setText(String.valueOf(commentService.countComments()));
            reportedTopicsLabel.setText(String.valueOf(forumService.countReportedTopics()));
            reportedCommentsLabel.setText(String.valueOf(commentService.countReportedComments()));
            hiddenTopicsLabel.setText(String.valueOf(forumService.countHiddenTopics()));

            topTopicsTable.getItems().setAll(forumService.findTopCommentedTopics(5));
            recentModerationTable.getItems().setAll(buildRecentModerationItems());
            refreshAllTopicsTable();

            updateDashboardStatus(
                    "Synchronise avec forum_topic et forum_comment.",
                    "#6366f1"
            );
        } catch (Exception e) {
            topTopicsTable.getItems().clear();
            recentModerationTable.getItems().clear();
            allTopicsTable.getItems().clear();
            totalTopicsLabel.setText("-");
            totalCommentsLabel.setText("-");
            reportedTopicsLabel.setText("-");
            reportedCommentsLabel.setText("-");
            hiddenTopicsLabel.setText("-");
            updateDashboardStatus(
                    "Impossible de charger les statistiques forum.",
                    "#dc2626"
            );
            e.printStackTrace();
        }
    }

    private void refreshAllTopicsTable() {
        String keyword = topicSearchField == null ? "" : topicSearchField.getText();
        String sortMode = topicSortComboBox == null ? ForumDashboardService.SORT_RECENT : topicSortComboBox.getValue();
        allTopicsTable.getItems().setAll(forumDashboardService.searchTopics(keyword, sortMode));
        playTableRefreshAnimation();
    }

    private TableCell<ForumTopic, String> badgeCell(java.util.function.Function<String, String> backgroundResolver,
                                                    java.util.function.Function<String, String> textResolver) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null || value.isBlank()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label badge = new Label(value);
                badge.setStyle("-fx-background-color: " + backgroundResolver.apply(value) + "; " +
                        "-fx-text-fill: " + textResolver.apply(value) + "; " +
                        "-fx-font-size: 11px; -fx-font-weight: bold; " +
                        "-fx-background-radius: 999; -fx-padding: 4 10;");
                setText(null);
                setGraphic(badge);
            }
        };
    }

    private List<ForumModerationItem> buildRecentModerationItems() {
        List<ForumModerationItem> items = new ArrayList<>();

        for (ForumTopic topic : forumService.findRecentModeratedTopics(6)) {
            LocalDateTime moderationDate = resolveTopicModerationDate(topic);
            items.add(new ForumModerationItem(
                    "Sujet",
                    compactText(topic.getTitle(), 88),
                    displayAuthorName(topic.getAuthorName()),
                    safeText(topic.getModerationSummary(), "Modere"),
                    formatDashboardDate(moderationDate),
                    moderationDate
            ));
        }

        for (ForumComment comment : commentService.findRecentModeratedComments(6)) {
            LocalDateTime moderationDate = resolveCommentModerationDate(comment);
            String topicContext = comment.getTopicTitle() != null && !comment.getTopicTitle().isBlank()
                    ? "Sur " + comment.getTopicTitle() + " : "
                    : "";

            items.add(new ForumModerationItem(
                    "Commentaire",
                    compactText(topicContext + safeText(comment.getContent(), ""), 104),
                    displayAuthorName(comment.getAuthorName()),
                    safeText(comment.getModerationSummary(), "Modere"),
                    formatDashboardDate(moderationDate),
                    moderationDate
            ));
        }

        items.sort(
                Comparator.comparing(
                                ForumModerationItem::getSortDate,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                        .thenComparing(ForumModerationItem::getTypeLabel)
        );

        return items.size() > 8 ? new ArrayList<>(items.subList(0, 8)) : items;
    }

    private LocalDateTime resolveTopicModerationDate(ForumTopic topic) {
        if (topic.getReportedAt() != null) {
            return topic.getReportedAt();
        }
        if (topic.getUpdatedAt() != null) {
            return topic.getUpdatedAt();
        }
        return topic.getCreatedAt();
    }

    private LocalDateTime resolveCommentModerationDate(ForumComment comment) {
        if (comment.getReportedAt() != null) {
            return comment.getReportedAt();
        }
        return comment.getCreatedAt();
    }

    private String formatDashboardDate(LocalDateTime value) {
        return value != null ? value.format(dashboardDateFormatter) : "-";
    }

    private String displayAuthorName(String authorName) {
        return safeText(authorName, "Auteur inconnu");
    }

    private String compactText(String value, int maxLength) {
        String normalized = safeText(value, "-").replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private String safeText(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private void updateDashboardStatus(String message, String color) {
        forumDashboardStatusLabel.setText(message);
        forumDashboardStatusLabel.setTextFill(Color.web(color));
    }

    private List<Node> dashboardCards() {
        return List.of(
                totalTopicsCard,
                totalCommentsCard,
                reportedTopicsCard,
                reportedCommentsCard,
                hiddenTopicsCard
        );
    }

    private void playDashboardIntro() {
        if (dashboardIntroPlayed || dashboardForumContent == null) {
            return;
        }
        dashboardIntroPlayed = true;

        List<Node> animatedNodes = new ArrayList<>(dashboardCards());
        animatedNodes.add(topTopicsPanel);
        animatedNodes.add(recentModerationPanel);
        animatedNodes.add(allTopicsPanel);

        for (int i = 0; i < animatedNodes.size(); i++) {
            Node node = animatedNodes.get(i);
            if (node == null) {
                continue;
            }
            node.setOpacity(0);
            node.setTranslateY(12);

            FadeTransition fade = new FadeTransition(Duration.millis(260), node);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setDelay(Duration.millis(45L * i));

            TranslateTransition slide = new TranslateTransition(Duration.millis(260), node);
            slide.setFromY(12);
            slide.setToY(0);
            slide.setDelay(Duration.millis(45L * i));

            new ParallelTransition(fade, slide).play();
        }
    }

    private void installCardHover(Node card) {
        if (card == null) {
            return;
        }
        card.setOnMouseEntered(event -> animateCardScale(card, 1.03));
        card.setOnMouseExited(event -> animateCardScale(card, 1.0));
    }

    private void installPanelHover(Node panel) {
        if (panel == null) {
            return;
        }
        panel.setOnMouseEntered(event -> animateCardScale(panel, 1.006));
        panel.setOnMouseExited(event -> animateCardScale(panel, 1.0));
    }

    private void playTableRefreshAnimation() {
        if (allTopicsTable == null) {
            return;
        }
        FadeTransition fade = new FadeTransition(Duration.millis(150), allTopicsTable);
        fade.setFromValue(0.72);
        fade.setToValue(1);
        fade.play();
    }

    private void animateCardScale(Node card, double scale) {
        ScaleTransition transition = new ScaleTransition(Duration.millis(140), card);
        transition.setToX(scale);
        transition.setToY(scale);
        transition.play();
    }

    private void highlightButton(Button active) {
        String normal = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";
        String activeS = "-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand;";
        for (Button b : allButtons()) b.setStyle(normal);
        active.setStyle(activeS);
    }
}
