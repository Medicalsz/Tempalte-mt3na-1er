package com.medicare.controllers;

import com.medicare.HelloApplication;
import com.medicare.models.User;
import com.medicare.services.RendezVousService;
import com.medicare.services.UserService;
import com.medicare.utils.MyConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final UserService userService = new UserService();

    @FXML
    private void onLoginClick() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        clearError();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        if (!MyConnection.getInstance().isConnected()) {
            showError("Connexion MySQL indisponible. Verifiez XAMPP/MySQL.");
            return;
        }

        User user = userService.login(email, password);

        if (user == null) {
            showError("Email ou mot de passe incorrect.");
            return;
        }

        System.out.println("Connexion reussie : " + user);

        if (user.hasRole("ROLE_ADMIN")) {
            DashboardAdminController.setCurrentUser(user);
            navigateTo("dashboard-admin-view.fxml", "Medicare - Administration");
            return;
        }

        RendezVousService rvService = new RendezVousService();
        int medecinId = rvService.getMedecinIdByUserId(user.getId());

        if (medecinId > 0 && user.hasRole("ROLE_MEDECIN")) {
            DashboardMedecinController.setCurrentUser(user);
            DashboardMedecinController.setMedecinId(medecinId);
            navigateTo("dashboard-medecin-view.fxml", "Medicare - Espace Medecin");
        } else {
            DashboardPatientController.setCurrentUser(user);
            navigateTo("dashboard-patient-view.fxml", "Medicare - Dashboard");
        }
    }

    @FXML
    private void onGoToRegister() {
        navigateTo("register-view.fxml", "Medicare - Inscription");
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
            showError("Impossible d'ouvrir l'ecran suivant.");
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 13px;");
        errorLabel.setText(message);
    }

    private void clearError() {
        errorLabel.setText("");
    }
}
