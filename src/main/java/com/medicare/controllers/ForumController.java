package com.medicare.controllers;

import com.medicare.HelloApplication;
import com.medicare.models.ForumTopic;
import com.medicare.models.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;

public abstract class ForumController {

    protected StackPane contentArea;
    protected User currentUser;

    public void setForumContext(StackPane contentArea, User currentUser) {
        this.contentArea = contentArea;
        this.currentUser = currentUser != null ? currentUser : resolveCurrentUser();
        onForumContextReady();
    }

    protected void onForumContextReady() {
    }

    protected User resolveCurrentUser() {
        if (currentUser != null) {
            return currentUser;
        }
        if (DashboardAdminController.getCurrentUser() != null) {
            return DashboardAdminController.getCurrentUser();
        }
        if (DashboardPatientController.getCurrentUser() != null) {
            return DashboardPatientController.getCurrentUser();
        }
        if (DashboardMedecinController.getCurrentUser() != null) {
            return DashboardMedecinController.getCurrentUser();
        }
        return null;
    }

    protected boolean canManageTopic(ForumTopic topic) {
        User user = resolveCurrentUser();
        return user != null && (user.hasRole("ROLE_ADMIN") || user.getId() == topic.getAuthorId());
    }

    protected boolean canManageComment(int authorId) {
        User user = resolveCurrentUser();
        return user != null && (user.hasRole("ROLE_ADMIN") || user.getId() == authorId);
    }

    protected boolean isAdmin() {
        User user = resolveCurrentUser();
        return user != null && user.hasRole("ROLE_ADMIN");
    }

    protected void openForumList() {
        if (contentArea == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("forum-list-view.fxml"));
            Node view = loader.load();
            ForumListController controller = loader.getController();
            controller.setForumContext(contentArea, resolveCurrentUser());
            swapContent(view);
        } catch (Exception e) {
            showError("Impossible d'ouvrir la liste du forum.", e);
        }
    }

    protected void openForumDetail(int topicId) {
        if (contentArea == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("forum-detail-view.fxml"));
            Node view = loader.load();
            ForumDetailController controller = loader.getController();
            controller.setForumContext(contentArea, resolveCurrentUser());
            controller.setTopicId(topicId);
            swapContent(view);
        } catch (Exception e) {
            showError("Impossible d'ouvrir le detail du sujet.", e);
        }
    }

    protected void openForumForm(ForumTopic topic) {
        if (contentArea == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("forum-form-view.fxml"));
            Node view = loader.load();
            ForumFormController controller = loader.getController();
            controller.setForumContext(contentArea, resolveCurrentUser());
            controller.setTopicToEdit(topic);
            swapContent(view);
        } catch (Exception e) {
            showError("Impossible d'ouvrir le formulaire du forum.", e);
        }
    }

    protected void openForumModeration() {
        if (contentArea == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("forum-moderation-view.fxml"));
            Node view = loader.load();
            ForumModerationController controller = loader.getController();
            controller.setForumContext(contentArea, resolveCurrentUser());
            swapContent(view);
        } catch (Exception e) {
            showError("Impossible d'ouvrir la moderation du forum.", e);
        }
    }

    protected String roleLabel(String roles) {
        if (roles == null || roles.isBlank()) {
            return "Utilisateur";
        }
        if (roles.contains("ROLE_ADMIN")) {
            return "Admin";
        }
        if (roles.contains("ROLE_MEDECIN")) {
            return "Medecin";
        }
        return "Patient";
    }

    protected String roleColor(String roles) {
        if (roles == null || roles.isBlank()) {
            return "#64748b";
        }
        if (roles.contains("ROLE_ADMIN")) {
            return "#7c3aed";
        }
        if (roles.contains("ROLE_MEDECIN")) {
            return "#0d9488";
        }
        return "#1a73e8";
    }

    protected void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    protected boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().filter(response -> response.getButtonData().isDefaultButton()).isPresent();
    }

    protected void showError(String message, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Forum");
        alert.setHeaderText("Une erreur est survenue");
        alert.setContentText(message);
        alert.showAndWait();
        if (exception != null) {
            exception.printStackTrace();
        }
    }

    private void swapContent(Node view) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(view);
    }
}
