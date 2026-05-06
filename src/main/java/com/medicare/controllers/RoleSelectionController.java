package com.medicare.controllers;

import com.medicare.HelloApplication;
import com.medicare.services.NotificationService;
import com.medicare.services.UserService;
import com.medicare.utils.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class RoleSelectionController {

    @FXML private VBox patientCard;
    @FXML private VBox medecinCard;

    private static final String PATIENT_BASE =
        "-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 28 20; -fx-cursor: hand;" +
        "-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 28 20; -fx-cursor: hand;" +
        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 14, 0, 0, 4);" +
        "-fx-border-width: 2; -fx-border-radius: 20;";
    private static final String MEDECIN_BASE =
        "-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 28 20; -fx-cursor: hand;" +
        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 14, 0, 0, 4);" +
        "-fx-border-width: 2; -fx-border-radius: 20;";

    private final UserService userService = new UserService();
    private final NotificationService notifService = new NotificationService();

    // ---- Hover effects ----
    @FXML private void onPatientHover() {
        patientCard.setStyle(PATIENT_BASE +
            "-fx-border-color: #C2185B;" +
            "-fx-effect: dropshadow(gaussian, rgba(194,24,91,0.25), 18, 0, 0, 6);");
    }
    @FXML private void onPatientExit() {
        patientCard.setStyle(PATIENT_BASE + "-fx-border-color: #F48FB1;");
    }
    @FXML private void onMedecinHover() {
        medecinCard.setStyle(MEDECIN_BASE +
            "-fx-border-color: #2E7D32;" +
            "-fx-effect: dropshadow(gaussian, rgba(46,125,50,0.25), 18, 0, 0, 6);");
    }
    @FXML private void onMedecinExit() {
        medecinCard.setStyle(MEDECIN_BASE + "-fx-border-color: #A5D6A7;");
    }

    // ---- Role selection ----
    @FXML
    private void selectPatient() {
        var user = Session.getCurrentUser();
        if (user == null) return;
        userService.updateUserRole(user.getId(), "[\"ROLE_USER\"]");
        user.setRoles("[\"ROLE_USER\"]");
        Session.setCurrentUser(user);
        Session.setAccountType("user");
        notifService.createWelcome(user.getId(), false);
        notifService.createRoleSelected(user.getId(), "user");
        navigateTo("complete-profile-view.fxml", "Medicare - Completer votre profil");
    }

    @FXML
    private void selectMedecin() {
        var user = Session.getCurrentUser();
        if (user == null) return;
        userService.updateUserRole(user.getId(), "[\"ROLE_MEDECIN\"]");
        user.setRoles("[\"ROLE_MEDECIN\"]");
        int medecinId = userService.createMedecinForUser(user.getId());
        Session.setCurrentUser(user);
        Session.setAccountType("medecin");
        Session.setMedecinId(medecinId);
        notifService.createWelcome(user.getId(), true);
        notifService.createRoleSelected(user.getId(), "medecin");
        notifService.createVerificationPending(user.getId());
        navigateTo("complete-profile-view.fxml", "Medicare - Completer votre profil");
    }

    private void navigateTo(String fxml, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxml));
            Stage stage = (Stage) patientCard.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
