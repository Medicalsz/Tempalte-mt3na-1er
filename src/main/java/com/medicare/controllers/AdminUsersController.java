package com.medicare.controllers;

import java.util.List;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import com.medicare.models.User;
import com.medicare.services.UserService;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class AdminUsersController {

    @FXML private VBox container;

    private final UserService userService = new UserService();

    @FXML
    private void initialize() {
        loadUsers();
    }

    private void loadUsers() {
        container.getChildren().clear();

        // Header
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Gestion des utilisateurs");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #7c3aed;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Recherche
        TextField searchField = new TextField();
        searchField.setPromptText("Rechercher...");
        searchField.setPrefWidth(220);
        searchField.setStyle("-fx-background-radius: 8; -fx-font-size: 13px;");
        searchField.textProperty().addListener((obs, old, val) -> filterUsers(val));

        header.getChildren().addAll(title, spacer, searchField);
        container.getChildren().add(header);

        // Tableau header
        HBox tableHeader = new HBox();
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        tableHeader.setPadding(new Insets(10, 15, 10, 15));
        tableHeader.setStyle("-fx-background-color: #7c3aed; -fx-background-radius: 8 8 0 0;");

        tableHeader.getChildren().addAll(
            colLabel("Nom", 150), colLabel("Email", 200),
            colLabel("Role", 120), colLabel("Verifie", 70), colLabel("Actions", 120)
        );
        container.getChildren().add(tableHeader);

        // Lignes
        List<User> users = userService.getAllUsers();
        addUserRows(users);
    }

    private void addUserRows(List<User> users) {
        // Retirer les anciennes lignes (garder header + table header)
        while (container.getChildren().size() > 2) {
            container.getChildren().remove(2);
        }

        if (users.isEmpty()) {
            Label empty = new Label("Aucun utilisateur trouve.");
            empty.setStyle("-fx-font-size: 14px; -fx-text-fill: #888; -fx-padding: 20;");
            container.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 15, 10, 15));
            row.setStyle("-fx-background-color: " + (i % 2 == 0 ? "white" : "#f9fafb") + ";");

            Label nom = new Label(u.getNom());
            nom.setPrefWidth(150);
            nom.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");

            Label email = new Label(u.getEmail());
            email.setPrefWidth(200);
            email.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

            // Badge rôle
            String roleText = u.getRoles().contains("ADMIN") ? "Admin" :
                              u.getRoles().contains("MEDECIN") ? "Medecin" : "Patient";
            String roleColor = u.getRoles().contains("ADMIN") ? "#7c3aed" :
                               u.getRoles().contains("MEDECIN") ? "#0d9488" : "#1a73e8";
            Label role = new Label(roleText);
            role.setPrefWidth(120);
            role.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-padding: 2 10; " +
                          "-fx-background-color: " + roleColor + "; -fx-background-radius: 12;");

            // Vérifié
            FontIcon verifiedIcon = new FontIcon(u.isVerified() ? FontAwesomeSolid.CHECK_CIRCLE : FontAwesomeSolid.TIMES_CIRCLE);
            verifiedIcon.setIconSize(16);
            verifiedIcon.setIconColor(Color.web(u.isVerified() ? "#16a34a" : "#dc2626"));
            Label verified = new Label();
            verified.setGraphic(verifiedIcon);
            verified.setPrefWidth(70);

            // Actions
            HBox actions = new HBox(6);
            actions.setAlignment(Pos.CENTER);
            actions.setPrefWidth(120);

            Button btnVoir = actionBtn(FontAwesomeSolid.EYE, "#7c3aed", "#ede9fe");
            btnVoir.setTooltip(new Tooltip("Voir"));
            btnVoir.setOnAction(e -> showUserDetails(u));

            Button btnToggle = actionBtn(
                u.isVerified() ? FontAwesomeSolid.BAN : FontAwesomeSolid.CHECK,
                u.isVerified() ? "#dc2626" : "#16a34a",
                u.isVerified() ? "#fee2e2" : "#dcfce7"
            );
            btnToggle.setTooltip(new Tooltip(u.isVerified() ? "Bloquer" : "Activer"));
            btnToggle.setOnAction(e -> {
                userService.toggleVerified(u.getId(), !u.isVerified());
                showPopup(u.isVerified() ? "Utilisateur bloque" : "Utilisateur active",
                          u.getNom(),
                          u.isVerified() ? FontAwesomeSolid.BAN : FontAwesomeSolid.CHECK_CIRCLE,
                          u.isVerified() ? "#dc2626" : "#16a34a");
            });

            Button btnDelete = actionBtn(FontAwesomeSolid.TRASH_ALT, "#dc2626", "#fee2e2");
            btnDelete.setTooltip(new Tooltip("Supprimer"));
            btnDelete.setOnAction(e -> showDeleteConfirm(u));

            actions.getChildren().addAll(btnVoir, btnToggle, btnDelete);

            row.getChildren().addAll(nom, email, role, verified, actions);
            container.getChildren().add(row);
        }
    }

    private Label colLabel(String text, double width) {
        Label l = new Label(text);
        l.setPrefWidth(width);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");
        return l;
    }

    private Button actionBtn(FontAwesomeSolid iconType, String iconColor, String bgColor) {
        Button btn = new Button();
        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(13);
        icon.setIconColor(Color.web(iconColor));
        btn.setGraphic(icon);
        btn.setStyle("-fx-background-color: " + bgColor + "; -fx-cursor: hand; -fx-padding: 5; -fx-background-radius: 6;");
        return btn;
    }

    private void filterUsers(String search) {
        List<User> all = userService.getAllUsers();
        if (search == null || search.trim().isEmpty()) {
            addUserRows(all);
            return;
        }
        String s = search.toLowerCase();
        List<User> filtered = all.stream()
            .filter(u -> u.getNom().toLowerCase().contains(s) ||
                         u.getEmail().toLowerCase().contains(s))
            .toList();
        addUserRows(filtered);
    }

    // ========== DETAILS ==========

    private void showUserDetails(User u) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        FontIcon icon = new FontIcon(FontAwesomeSolid.USER_CIRCLE);
        icon.setIconSize(44);
        icon.setIconColor(Color.web("#7c3aed"));

        Label titleLbl = new Label(u.getNom());
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #7c3aed;");

        Label details = new Label(
            "Email :  " + u.getEmail() + "\n" +
            "Numero :  " + (u.getNumero() != null ? u.getNumero() : "-") + "\n" +
            "Roles :  " + u.getRoles() + "\n" +
            "Verifie :  " + (u.isVerified() ? "Oui" : "Non")
        );
        details.setStyle("-fx-font-size: 13px; -fx-text-fill: #444; -fx-line-spacing: 3;");

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-size: 13px; " +
                          "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 30;");
        closeBtn.setOnAction(e -> popup.close());

        VBox box = new VBox(12, icon, titleLbl, details, closeBtn);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 16; " +
                     "-fx-border-color: #e0e0e0; -fx-border-radius: 16; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 4);");

        popup.setScene(new Scene(box, 380, 320));
        popup.show();
    }

    // ========== SUPPRESSION ==========

    private void showDeleteConfirm(User u) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        FontIcon warnIcon = new FontIcon(FontAwesomeSolid.EXCLAMATION_TRIANGLE);
        warnIcon.setIconSize(44);
        warnIcon.setIconColor(Color.web("#dc2626"));

        Label titleLbl = new Label("Supprimer cet utilisateur ?");
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");

        Label msgLbl = new Label(u.getNom() + "\n" + u.getEmail());
        msgLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #666; -fx-text-alignment: center;");

        Button btnOui = new Button("Supprimer");
        btnOui.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-size: 13px; " +
                        "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 25;");

        Button btnNon = new Button("Annuler");
        btnNon.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #333; -fx-font-size: 13px; " +
                        "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 25;");

        HBox btns = new HBox(15, btnNon, btnOui);
        btns.setAlignment(Pos.CENTER);

        VBox box = new VBox(15, warnIcon, titleLbl, msgLbl, btns);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 16; " +
                     "-fx-border-color: #e0e0e0; -fx-border-radius: 16; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 4);");

        popup.setScene(new Scene(box, 380, 250));
        popup.show();

        btnNon.setOnAction(e -> popup.close());
        btnOui.setOnAction(e -> {
            popup.close();
            userService.deleteUser(u.getId());
            showPopup("Utilisateur supprime", u.getNom(),
                      FontAwesomeSolid.TRASH_ALT, "#dc2626");
        });
    }

    // ========== POPUP ==========

    private void showPopup(String title, String message, FontAwesomeSolid iconType, String color) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(48);
        icon.setIconColor(Color.web(color));

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #7c3aed;");

        Label msgLbl = new Label(message);
        msgLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #555;");

        VBox box = new VBox(15, icon, titleLbl, msgLbl);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 16; " +
                     "-fx-border-color: #e0e0e0; -fx-border-radius: 16; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 4);");

        popup.setScene(new Scene(box, 340, 220));
        popup.show();

        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> {
            popup.close();
            loadUsers();
        });
        pause.play();
    }
}