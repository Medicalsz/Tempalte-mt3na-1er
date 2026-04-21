package com.medicare.controllers;

import com.medicare.models.User;
import com.medicare.services.UserService;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.regex.Pattern;

public class AdminUsersController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-zA-ZÀ-ÿ\\s'-]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+\\s]{8,15}$");

    @FXML private VBox container;

    private final UserService userService = new UserService();
    private TextField searchField;

    @FXML
    private void initialize() {
        loadUsers();
    }

    private void loadUsers() {
        container.getChildren().clear();

        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Gestion des utilisateurs");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #7c3aed;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("Ajouter");
        addBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand;");
        addBtn.setOnAction(e -> showUserForm(null));

        searchField = new TextField();
        searchField.setPromptText("Rechercher...");
        searchField.setPrefWidth(220);
        searchField.setStyle("-fx-background-radius: 8; -fx-font-size: 13px;");
        searchField.textProperty().addListener((obs, old, val) -> filterUsers(val));

        header.getChildren().addAll(title, spacer, addBtn, searchField);
        container.getChildren().add(header);

        HBox tableHeader = new HBox();
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        tableHeader.setPadding(new Insets(10, 15, 10, 15));
        tableHeader.setStyle("-fx-background-color: #7c3aed; -fx-background-radius: 8 8 0 0;");

        tableHeader.getChildren().addAll(
            colLabel("Nom", 130),
            colLabel("Prenom", 120),
            colLabel("Email", 210),
            colLabel("Role", 100),
            colLabel("Verifie", 70),
            colLabel("Actions", 170)
        );
        container.getChildren().add(tableHeader);

        addUserRows(userService.getAllUsers());
    }

    private void addUserRows(List<User> users) {
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
            User user = users.get(i);
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 15, 10, 15));
            row.setStyle("-fx-background-color: " + (i % 2 == 0 ? "white" : "#f9fafb") + ";");

            Label nom = plainLabel(user.getNom(), 130);
            Label prenom = plainLabel(user.getPrenom(), 120);
            Label email = plainLabel(user.getEmail(), 210);

            Label role = roleBadge(user);

            FontIcon verifiedIcon = new FontIcon(user.isVerified() ? FontAwesomeSolid.CHECK_CIRCLE : FontAwesomeSolid.TIMES_CIRCLE);
            verifiedIcon.setIconSize(16);
            verifiedIcon.setIconColor(Color.web(user.isVerified() ? "#16a34a" : "#dc2626"));
            Label verified = new Label();
            verified.setGraphic(verifiedIcon);
            verified.setPrefWidth(70);

            HBox actions = new HBox(6);
            actions.setAlignment(Pos.CENTER);
            actions.setPrefWidth(170);

            Button btnView = actionBtn(FontAwesomeSolid.EYE, "#7c3aed", "#ede9fe");
            btnView.setTooltip(new Tooltip("Voir"));
            btnView.setOnAction(e -> showUserDetails(user));

            Button btnEdit = actionBtn(FontAwesomeSolid.EDIT, "#2563eb", "#dbeafe");
            btnEdit.setTooltip(new Tooltip("Modifier"));
            btnEdit.setOnAction(e -> showUserForm(user));

            Button btnToggle = actionBtn(
                user.isVerified() ? FontAwesomeSolid.BAN : FontAwesomeSolid.CHECK,
                user.isVerified() ? "#dc2626" : "#16a34a",
                user.isVerified() ? "#fee2e2" : "#dcfce7"
            );
            btnToggle.setTooltip(new Tooltip(user.isVerified() ? "Bloquer" : "Activer"));
            btnToggle.setOnAction(e -> {
                userService.toggleVerified(user.getId(), !user.isVerified());
                showPopup(user.isVerified() ? "Utilisateur bloque" : "Utilisateur active",
                    user.getPrenom() + " " + user.getNom(),
                    user.isVerified() ? FontAwesomeSolid.BAN : FontAwesomeSolid.CHECK_CIRCLE,
                    user.isVerified() ? "#dc2626" : "#16a34a");
            });

            Button btnDelete = actionBtn(FontAwesomeSolid.TRASH_ALT, "#dc2626", "#fee2e2");
            btnDelete.setTooltip(new Tooltip("Supprimer"));
            btnDelete.setOnAction(e -> showDeleteConfirm(user));

            actions.getChildren().addAll(btnView, btnEdit, btnToggle, btnDelete);
            row.getChildren().addAll(nom, prenom, email, role, verified, actions);
            container.getChildren().add(row);
        }
    }

    private void filterUsers(String search) {
        List<User> all = userService.getAllUsers();
        if (search == null || search.trim().isEmpty()) {
            addUserRows(all);
            return;
        }

        String value = search.toLowerCase();
        List<User> filtered = all.stream()
            .filter(user ->
                safe(user.getNom()).contains(value)
                    || safe(user.getPrenom()).contains(value)
                    || safe(user.getEmail()).contains(value))
            .toList();
        addUserRows(filtered);
    }

    private void showUserForm(User existingUser) {
        boolean editing = existingUser != null;

        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        Label title = new Label(editing ? "Modifier l'utilisateur" : "Ajouter un utilisateur");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #7c3aed;");

        TextField nomField = field(editing ? existingUser.getNom() : "", "Nom");
        TextField prenomField = field(editing ? existingUser.getPrenom() : "", "Prenom");
        TextField emailField = field(editing ? existingUser.getEmail() : "", "Email");
        TextField numeroField = field(editing ? existingUser.getNumero() : "", "Numero");
        TextField adresseField = field(editing ? existingUser.getAdresse() : "", "Adresse");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText(editing ? "Nouveau mot de passe (optionnel)" : "Mot de passe");
        passwordField.setPrefHeight(38);
        passwordField.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d1d5db;");

        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("Patient", "Admin");
        roleBox.setValue(editing && safe(existingUser.getRoles()).contains("ROLE_ADMIN") ? "Admin" : "Patient");
        roleBox.setPrefHeight(38);
        roleBox.setStyle("-fx-background-radius: 8;");

        CheckBox verifiedBox = new CheckBox("Compte verifie");
        verifiedBox.setSelected(editing && existingUser.isVerified());

        Label errorLabel = new Label();
        errorLabel.setWrapText(true);
        errorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #dc2626;");

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #333; -fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> popup.close());

        Button saveBtn = new Button(editing ? "Enregistrer" : "Creer");
        saveBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand;");
        saveBtn.setOnAction(e -> {
            String validationError = validateUserForm(
                nomField.getText().trim(),
                prenomField.getText().trim(),
                emailField.getText().trim().toLowerCase(),
                numeroField.getText().trim(),
                passwordField.getText(),
                editing,
                editing ? existingUser.getId() : null
            );

            if (validationError != null) {
                errorLabel.setText(validationError);
                return;
            }

            User user = editing ? existingUser : new User();
            user.setNom(nomField.getText().trim());
            user.setPrenom(prenomField.getText().trim());
            user.setEmail(emailField.getText().trim().toLowerCase());
            user.setNumero(numeroField.getText().trim());
            user.setAdresse(adresseField.getText().trim().isEmpty() ? null : adresseField.getText().trim());
            user.setRoles("Admin".equals(roleBox.getValue()) ? "[\"ROLE_ADMIN\"]" : "[\"ROLE_USER\"]");
            user.setIsVerified(verifiedBox.isSelected());
            if (!passwordField.getText().isBlank()) {
                user.setPassword(passwordField.getText());
            } else if (!editing) {
                user.setPassword("");
            } else {
                user.setPassword(null);
            }

            if (editing) {
                userService.update(user);
                popup.close();
                showPopup("Utilisateur modifie", user.getPrenom() + " " + user.getNom(), FontAwesomeSolid.SAVE, "#2563eb");
            } else {
                userService.add(user);
                popup.close();
                showPopup("Utilisateur ajoute", user.getPrenom() + " " + user.getNom(), FontAwesomeSolid.USER_PLUS, "#16a34a");
            }
        });

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.add(nomField, 0, 0);
        form.add(prenomField, 1, 0);
        form.add(emailField, 0, 1, 2, 1);
        form.add(numeroField, 0, 2);
        form.add(roleBox, 1, 2);
        form.add(adresseField, 0, 3, 2, 1);
        form.add(passwordField, 0, 4, 2, 1);
        form.add(verifiedBox, 0, 5, 2, 1);

        HBox buttons = new HBox(10, cancelBtn, saveBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox box = new VBox(16, title, form, errorLabel, buttons);
        box.setPadding(new Insets(26));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-border-color: #e5e7eb; "
            + "-fx-border-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 4);");

        popup.setScene(new Scene(new ScrollPane(box), 520, 430));
        popup.show();
    }

    private String validateUserForm(String nom, String prenom, String email, String numero, String password, boolean editing, Integer userId) {
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || numero.isEmpty()) {
            return "Veuillez remplir les champs obligatoires.";
        }
        if (!NAME_PATTERN.matcher(nom).matches() || !NAME_PATTERN.matcher(prenom).matches()) {
            return "Nom et prenom invalides.";
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return "Email invalide.";
        }
        if (!PHONE_PATTERN.matcher(numero).matches()) {
            return "Numero invalide.";
        }
        if ((!editing || !password.isBlank()) && password.length() < 6) {
            return "Le mot de passe doit contenir au moins 6 caracteres.";
        }
        if (userService.emailExists(email, userId)) {
            return "Cet email est deja utilise.";
        }
        return null;
    }

    private void showUserDetails(User user) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        FontIcon icon = new FontIcon(FontAwesomeSolid.USER_CIRCLE);
        icon.setIconSize(44);
        icon.setIconColor(Color.web("#7c3aed"));

        Label titleLbl = new Label(user.getPrenom() + " " + user.getNom());
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #7c3aed;");

        Label details = new Label(
            "Email : " + user.getEmail() + "\n" +
            "Numero : " + (user.getNumero() != null ? user.getNumero() : "-") + "\n" +
            "Adresse : " + (user.getAdresse() != null ? user.getAdresse() : "-") + "\n" +
            "Roles : " + user.getRoles() + "\n" +
            "Verifie : " + (user.isVerified() ? "Oui" : "Non")
        );
        details.setStyle("-fx-font-size: 13px; -fx-text-fill: #444; -fx-line-spacing: 3;");

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-size: 13px; "
            + "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 30;");
        closeBtn.setOnAction(e -> popup.close());

        VBox box = new VBox(12, icon, titleLbl, details, closeBtn);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 16; "
            + "-fx-border-color: #e0e0e0; -fx-border-radius: 16; "
            + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 4);");

        popup.setScene(new Scene(box, 380, 320));
        popup.show();
    }

    private void showDeleteConfirm(User user) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        FontIcon warnIcon = new FontIcon(FontAwesomeSolid.EXCLAMATION_TRIANGLE);
        warnIcon.setIconSize(44);
        warnIcon.setIconColor(Color.web("#dc2626"));

        Label titleLbl = new Label("Supprimer cet utilisateur ?");
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");

        Label msgLbl = new Label(user.getPrenom() + " " + user.getNom() + "\n" + user.getEmail());
        msgLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #666; -fx-text-alignment: center;");

        Button btnOui = new Button("Supprimer");
        btnOui.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-size: 13px; "
            + "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 25;");

        Button btnNon = new Button("Annuler");
        btnNon.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #333; -fx-font-size: 13px; "
            + "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 25;");

        HBox btns = new HBox(15, btnNon, btnOui);
        btns.setAlignment(Pos.CENTER);

        VBox box = new VBox(15, warnIcon, titleLbl, msgLbl, btns);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 16; "
            + "-fx-border-color: #e0e0e0; -fx-border-radius: 16; "
            + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 4);");

        popup.setScene(new Scene(box, 380, 250));
        popup.show();

        btnNon.setOnAction(e -> popup.close());
        btnOui.setOnAction(e -> {
            popup.close();
            userService.deleteUser(user.getId());
            showPopup("Utilisateur supprime", user.getPrenom() + " " + user.getNom(),
                FontAwesomeSolid.TRASH_ALT, "#dc2626");
        });
    }

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
        box.setStyle("-fx-background-color: white; -fx-background-radius: 16; "
            + "-fx-border-color: #e0e0e0; -fx-border-radius: 16; "
            + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 4);");

        popup.setScene(new Scene(box, 340, 220));
        popup.show();

        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> {
            popup.close();
            loadUsers();
        });
        pause.play();
    }

    private Label colLabel(String text, double width) {
        Label label = new Label(text);
        label.setPrefWidth(width);
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");
        return label;
    }

    private Label plainLabel(String text, double width) {
        Label label = new Label(text == null ? "-" : text);
        label.setPrefWidth(width);
        label.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");
        return label;
    }

    private Label roleBadge(User user) {
        String roleText = user.getRoles().contains("ADMIN") ? "Admin"
            : user.getRoles().contains("MEDECIN") ? "Medecin" : "Patient";
        String roleColor = user.getRoles().contains("ADMIN") ? "#7c3aed"
            : user.getRoles().contains("MEDECIN") ? "#0d9488" : "#1a73e8";

        Label role = new Label(roleText);
        role.setPrefWidth(100);
        role.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-padding: 2 10; "
            + "-fx-background-color: " + roleColor + "; -fx-background-radius: 12;");
        return role;
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

    private TextField field(String value, String prompt) {
        TextField field = new TextField(value == null ? "" : value);
        field.setPromptText(prompt);
        field.setPrefHeight(38);
        field.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d1d5db;");
        return field;
    }

    private String safe(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
