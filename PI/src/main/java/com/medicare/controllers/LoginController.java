package com.medicare.controllers;

import com.medicare.HelloApplication;
import com.medicare.models.User;
import com.medicare.services.RendezVousService;
import com.medicare.services.UserService;
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

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs.");
            return;
        }

        User user = userService.login(email, password);

        if (user != null) {
            System.out.println("Connexion reussie : " + user);

            if (user.getRoles().contains("ROLE_ADMIN")) {
                // Admin
                DashboardAdminController.setCurrentUser(user);
                navigateTo("dashboard-admin-view.fxml", "Medicare - Administration");
            } else {
                // Vérifier si c'est un médecin
                RendezVousService rvService = new RendezVousService();
                int medecinId = rvService.getMedecinIdByUserId(user.getId());

                if (medecinId > 0 && user.getRoles().contains("ROLE_MEDECIN")) {
                    DashboardMedecinController.setCurrentUser(user);
                    DashboardMedecinController.setMedecinId(medecinId);
                    navigateTo("dashboard-medecin-view.fxml", "Medicare - Espace Medecin");
                } else {
                    DashboardPatientController.setCurrentUser(user);
                    navigateTo("dashboard-patient-view.fxml", "Medicare - Dashboard");
                }
            }
        } else {
            errorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 13px;");
            errorLabel.setText("Email ou mot de passe incorrect.");
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
            e.printStackTrace();
        }
    }
}

