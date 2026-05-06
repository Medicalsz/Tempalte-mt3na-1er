package com.medicare.controllers;

import com.medicare.HelloApplication;
import com.medicare.models.LoginResult;
import com.medicare.models.User;
import com.medicare.services.ProfileAlertService;
import com.medicare.services.RendezVousService;
import com.medicare.services.UserService;
import com.medicare.utils.AuthPreferenceUtil;
import com.medicare.utils.BiometricUtil;
import com.medicare.utils.CaptchaWidget;
import com.medicare.utils.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.prefs.Preferences;

public class LoginController {

    private static final String PREF_EMAIL = "last_login_email";
    private static final String PREF_NAME = "last_login_name";
    private static final String PREF_PHOTO = "last_login_photo";

    @FXML private Button previousLoginCard;
    @FXML private Label previousLoginTitle;
    @FXML private Label previousLoginName;
    @FXML private ImageView previousLoginAvatar;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private Button googleBtn;
    @FXML private Button fingerprintBtn;
    @FXML private VBox   captchaContainer;

    private CaptchaWidget captchaWidget;

    private final UserService userService = new UserService();
    private final Preferences preferences = Preferences.userNodeForPackage(LoginController.class);
    
    private int loginAttempts = 0;
    private static final int MAX_ATTEMPTS = 5;

    @FXML
    private void initialize() {
        previousLoginAvatar.setClip(new Circle(23, 23, 23));
        loadPreviousLoginCard();
        emailField.textProperty().addListener((obs, oldVal, newVal) -> updateFingerprintBtn(newVal.trim().toLowerCase()));

        captchaWidget = new CaptchaWidget();
        captchaContainer.getChildren().add(captchaWidget);
    }

    private void updateFingerprintBtn(String email) {
        boolean show = !email.isBlank() && AuthPreferenceUtil.isBiometricEnabled(email);
        fingerprintBtn.setVisible(show);
        fingerprintBtn.setManaged(show);
    }

    @FXML
    private void onLoginClick() {
        if (isLockedOut()) return;

        if (!captchaWidget.verify()) {
            showError("Veuillez valider le reCAPTCHA avant de continuer.");
            captchaWidget.reset();
            return;
        }

        String email = emailField.getText().trim().toLowerCase();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Signing in...");

        LoginResult loginResult = userService.loginByAccountType(email, password);
        loginButton.setDisable(false);
        loginButton.setText("Sign In");

        if (loginResult != null) {
            loginAttempts = 0;
            completeLogin(loginResult);
        } else {
            loginAttempts++;
            captchaWidget.reset();
            showError("Informations incorrectes. Tentative " + loginAttempts + "/" + MAX_ATTEMPTS);
        }
    }

    @FXML
    private void onUsePreviousLogin() {
        String previousEmail = preferences.get(PREF_EMAIL, "");
        if (!previousEmail.isBlank()) {
            emailField.setText(previousEmail);
            passwordField.requestFocus();
            errorLabel.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 13px;");
            errorLabel.setText("Entrez votre mot de passe pour continuer.");
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

    @FXML
    private void onFingerprintLogin() {
        if (isLockedOut()) return;

        String rawEmail = emailField.getText().trim().toLowerCase();
        if (rawEmail.isBlank()) rawEmail = preferences.get(PREF_EMAIL, "").trim().toLowerCase();
        final String email = rawEmail;

        if (email.isBlank()) {
            showError("Veuillez entrer votre email.");
            return;
        }

        String localToken = AuthPreferenceUtil.getBiometricToken(email);
        if (localToken == null) {
            showError("Aucune empreinte enregistrée pour cet email.");
            return;
        }

        fingerprintBtn.setDisable(true);
        loginButton.setDisable(true);
        errorLabel.setStyle("-fx-text-fill: #2563EB; -fx-font-size: 13px;");
        errorLabel.setText("☝️  Posez votre doigt sur le capteur…");
        errorLabel.setVisible(true);

        // Trigger real Windows Hello sensor in background; UI stays responsive
        Thread t = new Thread(() -> {
            BiometricUtil.Result bio = BiometricUtil.verify("Medicare — Connexion");
            javafx.application.Platform.runLater(() -> {
                fingerprintBtn.setDisable(false);
                loginButton.setDisable(false);

                if (bio == BiometricUtil.Result.VERIFIED) {
                    LoginResult loginResult = userService.loginByBiometric(localToken);
                    if (loginResult != null) {
                        loginAttempts = 0;
                        errorLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 13px;");
                        errorLabel.setText("✅  Empreinte reconnue. Connexion en cours…");
                        completeLogin(loginResult);
                    } else {
                        loginAttempts++;
                        showError("Session expirée — empreinte non reconnue en base. Tentative "
                                + loginAttempts + "/" + MAX_ATTEMPTS);
                    }
                } else if (bio == BiometricUtil.Result.UNAVAILABLE) {
                    showError("Capteur Windows Hello non disponible sur ce PC.");
                } else {
                    loginAttempts++;
                    showError("Empreinte non reconnue ou annulée. Tentative "
                            + loginAttempts + "/" + MAX_ATTEMPTS);
                }
            });
        });
        t.setDaemon(true);
        t.start();
    }

    private boolean isLockedOut() {
        if (loginAttempts >= MAX_ATTEMPTS) {
            showError("Trop de tentatives. Veuillez réessayer plus tard.");
            return true;
        }
        return false;
    }

    private void loadPreviousLoginCard() {
        String previousEmail = preferences.get(PREF_EMAIL, "");
        if (previousEmail.isBlank()) {
            previousLoginCard.setManaged(false);
            previousLoginCard.setVisible(false);
            return;
        }
        String name = preferences.get(PREF_NAME, previousEmail);
        String photo = preferences.get(PREF_PHOTO, "");
        previousLoginTitle.setText("Login as " + name);
        previousLoginName.setText(previousEmail);
        updatePreviousLoginAvatar(photo);
        previousLoginCard.setManaged(true);
        previousLoginCard.setVisible(true);
        // Pre-fill email and show fingerprint button if biometric is set up
        emailField.setText(previousEmail);
        updateFingerprintBtn(previousEmail.trim().toLowerCase());
    }

    private void rememberLastLogin(User user) {
        String displayName = (safe(user.getPrenom()) + " " + safe(user.getNom())).trim();
        preferences.put(PREF_EMAIL, safe(user.getEmail()));
        preferences.put(PREF_NAME, displayName.isBlank() ? safe(user.getEmail()) : displayName);
        preferences.put(PREF_PHOTO, safe(user.getPhoto()));
    }

    private void updatePreviousLoginAvatar(String photoPath) {
        try {
            if (photoPath == null || photoPath.isBlank()) {
                previousLoginAvatar.setViewport(null);
                previousLoginAvatar.setImage(new Image(HelloApplication.class.getResource("images/logo.png").toExternalForm(), true));
                return;
            }
            String source = photoPath.startsWith("file:/") ? photoPath : Path.of(photoPath).toUri().toString();
            Image image = new Image(source, false);
            previousLoginAvatar.setImage(image);
            applyCenteredSquareViewport(previousLoginAvatar, image);
        } catch (Exception e) {
            previousLoginAvatar.setViewport(null);
            previousLoginAvatar.setImage(new Image(HelloApplication.class.getResource("images/logo.png").toExternalForm(), true));
        }
    }

    private void applyCenteredSquareViewport(ImageView view, Image image) {
        double width = image.getWidth();
        double height = image.getHeight();
        if (width <= 0 || height <= 0) return;
        double side = Math.min(width, height);
        view.setViewport(new javafx.geometry.Rectangle2D((width - side) / 2, (height - side) / 2, side, side));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 13px;");
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
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

    @FXML
    private void handleGoogleLogin() {
        if (isLockedOut()) return;
        
        errorLabel.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 13px;");
        errorLabel.setText("Connexion via Google en cours...");
        errorLabel.setVisible(true);

        new Thread(() -> {
            try {
                com.medicare.services.GoogleAuthService auth = new com.medicare.services.GoogleAuthService();
                com.google.api.services.oauth2.model.Userinfo info = auth.authenticateAndGetUser();
                javafx.application.Platform.runLater(() -> processGoogleUser(info, auth.getAccessToken()));
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> showError("Erreur Google Login: " + e.getMessage()));
            }
        }).start();
    }

    private void processGoogleUser(com.google.api.services.oauth2.model.Userinfo info, String token) {
        String email = info.getEmail();
        LoginResult loginResult = userService.loginWithoutPassword(email);

        if (loginResult == null) {
            User user = new User();
            user.setEmail(email);
            user.setNom(safe(info.getFamilyName()));
            user.setPrenom(safe(info.getGivenName()));
            user.setPhoto(info.getPicture());
            user.setGoogleId(info.getId());
            user.setPassword(java.util.UUID.randomUUID().toString());
            user.setIsVerified(true);
            user.setProfileCompleted(false);
            userService.add(user);
            user.setRoles("[\"ROLE_USER\"]");
            loginResult = new LoginResult(user, "user", -1);
        } else {
            User user = loginResult.getUser();
            if (user.getGoogleId() == null || user.getGoogleId().isBlank()) {
                userService.linkGoogleAccount(user.getId(), info.getId(), token);
                user.setGoogleId(info.getId());
            }
        }
        loginAttempts = 0;
        completeLogin(loginResult);
    }

    private void completeLogin(LoginResult loginResult) {
        User user = loginResult.getUser();
        rememberLastLogin(user);
        Session.setCurrentUser(user);
        Session.setAccountType(loginResult.getAccountType());

        if (loginResult.isAdmin()) {
            DashboardAdminController.setCurrentUser(user);
            navigateTo("dashboard-admin-view.fxml", "Medicare - Administration");
        } else if (loginResult.isMedecin()) {
            int medecinId = loginResult.getMedecinId();
            if (medecinId <= 0) {
                RendezVousService rvService = new RendezVousService();
                medecinId = rvService.getMedecinIdByUserId(user.getId());
            }
            Session.setMedecinId(medecinId);
            DashboardMedecinController.setCurrentUser(user);
            DashboardMedecinController.setMedecinId(medecinId);

            int pct = ProfileAlertService.getCompletionPercent(user);
            if (pct < 50) navigateTo("complete-profile-view.fxml", "Medicare - Complete Profile");
            else navigateTo("dashboard-medecin-view.fxml", "Medicare - Espace Medecin");
        } else {
            DashboardPatientController.setCurrentUser(user);
            int pct = ProfileAlertService.getCompletionPercent(user);
            if (pct < 50) navigateTo("complete-profile-view.fxml", "Medicare - Complete Profile");
            else navigateTo("dashboard-patient-view.fxml", "Medicare - Dashboard");
        }
    }
}
