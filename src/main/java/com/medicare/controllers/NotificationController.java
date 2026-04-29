package com.medicare.controllers;

import com.medicare.models.Notification;
import com.medicare.models.User;
import com.medicare.services.NotificationService;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationController extends ForumController {
    @FXML private Label notificationStatsLabel;
    @FXML private Label emptyLabel;
    @FXML private VBox notificationsContainer;
    @FXML private Button markAllButton;

    private final NotificationService notificationService = new NotificationService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private Runnable notificationChangeListener;
    private Timeline refreshTimeline;

    public void setNotificationContext(StackPane contentArea, User currentUser) {
        setForumContext(contentArea, currentUser);
    }

    public void setNotificationChangeListener(Runnable notificationChangeListener) {
        this.notificationChangeListener = notificationChangeListener;
    }

    @Override
    protected void onForumContextReady() {
        loadNotifications();
        startAutoRefresh();
    }

    @FXML
    private void onMarkAllAsReadClick() {
        User user = resolveCurrentUser();
        if (user == null) {
            return;
        }
        notificationService.markAllAsRead(user.getId());
        loadNotifications();
        notifyNotificationsChanged();
    }

    public void loadNotifications() {
        User user = resolveCurrentUser();
        if (user == null || notificationsContainer == null) {
            return;
        }

        List<Notification> notifications = notificationService.findByUserId(user.getId());
        int unread = notificationService.countUnreadByUserId(user.getId());
        notifyNotificationsChanged();
        notificationStatsLabel.setText(unread == 0
                ? notifications.size() + " notification(s), tout est lu"
                : unread + " non lue(s) sur " + notifications.size());
        markAllButton.setDisable(unread == 0);
        emptyLabel.setVisible(notifications.isEmpty());
        emptyLabel.setManaged(notifications.isEmpty());

        notificationsContainer.getChildren().clear();
        for (Notification notification : notifications) {
            notificationsContainer.getChildren().add(createNotificationCard(notification));
        }

        FadeTransition fade = new FadeTransition(Duration.millis(220), notificationsContainer);
        fade.setFromValue(0.75);
        fade.setToValue(1);
        fade.play();
    }

    private void startAutoRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }

        refreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(5), event -> loadNotifications())
        );
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        refreshTimeline.play();

        if (notificationsContainer != null) {
            notificationsContainer.sceneProperty().addListener((observable, oldScene, newScene) -> {
                if (newScene == null && refreshTimeline != null) {
                    refreshTimeline.stop();
                }
            });
        }
    }

    private VBox createNotificationCard(Notification notification) {
        VBox card = new VBox(10);
        card.getStyleClass().setAll("notification-card");
        if (!notification.isRead()) {
            card.getStyleClass().add("notification-card-unread");
        }

        HBox header = new HBox(10);
        header.getStyleClass().setAll("notification-card-header");

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().setAll(notification.isRead() ? "notification-icon-read" : "notification-icon-unread");
        FontIcon icon = new FontIcon(resolveIcon(notification.getType()));
        icon.setIconSize(15);
        iconBox.getChildren().add(icon);

        VBox textBox = new VBox(4);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label title = new Label(safeText(notification.getTitle(), "Notification"));
        title.getStyleClass().setAll(notification.isRead() ? "notification-title" : "notification-title-unread");

        Label message = new Label(safeText(notification.getMessage(), ""));
        message.setWrapText(true);
        message.getStyleClass().setAll(notification.isRead() ? "notification-message" : "notification-message-unread");

        Label meta = new Label(formatDate(notification.getCreatedAt()));
        meta.getStyleClass().setAll("notification-meta");
        textBox.getChildren().addAll(title, message, meta);

        header.getChildren().addAll(iconBox, textBox);
        card.getChildren().add(header);

        HBox actions = new HBox(10);
        actions.getStyleClass().setAll("notification-actions");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        actions.getChildren().add(spacer);

        if (!notification.isRead()) {
            Button markReadButton = new Button("Marquer comme lu");
            markReadButton.getStyleClass().setAll("notification-secondary-button");
            markReadButton.setOnAction(event -> {
                notificationService.markAsRead(notification.getId());
                loadNotifications();
                notifyNotificationsChanged();
            });
            actions.getChildren().add(markReadButton);
        }

        if (notification.getRelatedTopicId() != null && notification.getRelatedTopicId() > 0) {
            Button viewTopicButton = new Button("Voir le sujet");
            viewTopicButton.getStyleClass().setAll("notification-primary-button");
            viewTopicButton.setOnAction(event -> {
                if (!notification.isRead()) {
                    notificationService.markAsRead(notification.getId());
                    notifyNotificationsChanged();
                }
                openForumDetail(notification.getRelatedTopicId());
            });
            actions.getChildren().add(viewTopicButton);
        }

        card.getChildren().add(actions);
        return card;
    }

    private FontAwesomeSolid resolveIcon(String type) {
        if (NotificationService.TYPE_NEW_COMMENT.equalsIgnoreCase(type)) {
            return FontAwesomeSolid.COMMENT_DOTS;
        }
        return FontAwesomeSolid.COMMENTS;
    }

    private String formatDate(LocalDateTime date) {
        return date == null ? "-" : date.format(dateFormatter);
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private void notifyNotificationsChanged() {
        if (notificationChangeListener != null) {
            notificationChangeListener.run();
        }
    }
}
