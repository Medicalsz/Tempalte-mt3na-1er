package com.medicare.controllers;

import com.medicare.HelloApplication;
import com.medicare.models.User;
import com.medicare.services.UserService;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.regex.Pattern;

public class RegisterController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-zA-ZÀ-ÿ\\s'-]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+\\s]{8,15}$");

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField numeroField;
    @FXML private TextField adresseField;
    @FXML private Label errorLabel;

    private final UserService userService = new UserService();

    @FXML
    private void onRegisterClick() {
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim().toLowerCase();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        String numero = numeroField.getText().trim();
        String adresse = adresseField.getText().trim();

        String validationError = validateForm(nom, prenom, email, password, confirm, numero);
        if (validationError != null) {
            showError(validationError);
            return;
        }

        User user = new User(
            nom,
            prenom,
            email,
            password,
            numero,
            adresse.isEmpty() ? null : adresse,
            null,
            "[\"ROLE_USER\"]",
            false
        );

        boolean success = userService.register(user);

        if (success) {
            errorLabel.setText("");
            showSuccessAndRedirect();
        } else {
            showError("Cet email est deja utilise.");
        }
    }

    private String validateForm(String nom, String prenom, String email, String password, String confirm, String numero) {
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty() || numero.isEmpty()) {
            return "Veuillez remplir tous les champs obligatoires.";
        }
        if (!NAME_PATTERN.matcher(nom).matches() || !NAME_PATTERN.matcher(prenom).matches()) {
            return "Le nom et le prenom doivent contenir au moins 2 lettres.";
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return "Email invalide.";
        }
        if (!PHONE_PATTERN.matcher(numero).matches()) {
            return "Numero invalide. Utilisez entre 8 et 15 chiffres.";
        }
        if (password.length() < 6) {
            return "Le mot de passe doit contenir au moins 6 caracteres.";
        }
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            return "Le mot de passe doit contenir au moins une lettre et un chiffre.";
        }
        if (!password.equals(confirm)) {
            return "Les mots de passe ne correspondent pas.";
        }
        if (userService.emailExists(email)) {
            return "Cet email est deja utilise.";
        }
        return null;
    }

    private void showError(String msg) {
        errorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 13px;");
        errorLabel.setText(msg);
    }

    private void showSuccessAndRedirect() {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(emailField.getScene().getWindow());

        FontIcon checkIcon = new FontIcon(FontAwesomeSolid.CHECK_CIRCLE);
        checkIcon.setIconSize(48);
        checkIcon.setIconColor(Color.web("#16a34a"));

        Label title = new Label("Inscription reussie !");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1a73e8;");

        Label msg = new Label("Votre compte a ete cree avec succes.\nRedirection vers la connexion...");
        msg.setStyle("-fx-font-size: 13px; -fx-text-fill: #555; -fx-text-alignment: center;");
        msg.setWrapText(true);

        VBox box = new VBox(15, checkIcon, title, msg);
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
            navigateTo("login-view.fxml", "Medicare - Connexion");
        });
        pause.play();
    }

    @FXML
    private void onGoToLogin() {
        navigateTo("login-view.fxml", "Medicare - Connexion");
    }

    @FXML
    private void onGoToAccueil() {
        navigateTo("accueil-view.fxml", "Medicare");
    }

    private void navigateTo(String fxml, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxml));
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
