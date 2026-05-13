package com.medicare.ui;

import com.medicare.HelloApplication;
import com.medicare.controllers.LoginController;
import com.medicare.models.User;
import com.medicare.services.GoogleAuthService;
import com.medicare.services.UserService;
import com.medicare.services.UserService.DoctorDistance;
import com.medicare.utils.AuthPreferenceUtil;
import com.medicare.utils.BiometricUtil;
import com.medicare.utils.FileStorageUtil;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import netscape.javascript.JSObject;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.prefs.Preferences;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public final class UserSectionFactory {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-zA-ZÀ-ÿ\\s'-]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+\\s]{8,15}$");

    private UserSectionFactory() {
    }

    public static Node createWelcomeSection(String title, String subtitle, String accentColor) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setWrapText(true);
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4b5563;");

        VBox card = new VBox(12, titleLabel, subtitleLabel);
        card.setPadding(new Insets(28));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; "
            + "-fx-border-color: " + accentColor + "; -fx-border-radius: 20; -fx-border-width: 1.2;");

        return wrap(card);
    }

    public static Node createProfileSection(
        User currentUser,
        Window owner,
        Consumer<User> onUpdated,
        Runnable onDeleteAccount
    ) {
        return createProfileSection(currentUser, owner, onUpdated, onDeleteAccount, null);
    }

    public static Node createProfileSection(
        User currentUser,
        Window owner,
        Consumer<User> onUpdated,
        Runnable onDeleteAccount,
        Map<String, Runnable> quickNavActions
    ) {
        VBox page = new VBox(24,
            buildProfileHeader(currentUser, quickNavActions),
            buildPublicInfoCard(currentUser),
            buildEmptyPostsCard()
        );
        page.setPadding(new Insets(6));
        return wrap(page);
    }

    // =========================================================
    //  TARGETED TILE SECTIONS (used by CompleteProfileController)
    // =========================================================

    /** Tile 1 — nom, prenom, gender + languages (medecin only) */
    public static Node createPersonalSection(User user, Consumer<User> onUpdated) {
        UserService svc = new UserService();
        boolean isMedecin = user.getRoles() != null && user.getRoles().contains("ROLE_MEDECIN");

        TextField prenomField = styledTextField(user.getPrenom(), "Prénom");
        TextField nomField    = styledTextField(user.getNom(),    "Nom");

        ComboBox<String> genderBox = new ComboBox<>();
        genderBox.getItems().addAll("Homme", "Femme", "Autre");
        genderBox.setPromptText("Choisir votre genre");
        if (user.getGender() != null) genderBox.setValue(genderToLabel(user.getGender()));
        styleCombo(genderBox);

        TextField langField = null;
        if (isMedecin) {
            String langs = svc.getMedecinLanguages(user.getId());
            langField = styledTextField(langs, "Langues parlées (ex: Français, Arabe, Anglais)");
        }
        final TextField finalLangField = langField;

        Label status = statusLabel();
        Button save = primaryButton("Enregistrer", "#C2185B");
        Button enableFingerprintBtn = secondaryButton("Activer empreinte locale", "#0EA5E9");
        Button deleteBioBtn = secondaryButton("Supprimer l'empreinte", "#D32F2F");
        save.setOnAction(e -> {
            String nom    = nomField.getText().trim();
            String prenom = prenomField.getText().trim();
            if (nom.isEmpty() || prenom.isEmpty()) {
                showFieldError(status, "Nom et prénom sont obligatoires."); return;
            }
            if (!NAME_PATTERN.matcher(nom).matches() || !NAME_PATTERN.matcher(prenom).matches()) {
                showFieldError(status, "Le nom/prénom doit contenir au moins 2 lettres."); return;
            }
            User copy = copyUser(user);
            copy.setNom(nom); copy.setPrenom(prenom); copy.setGender(labelToGender(genderBox.getValue()));
            if (svc.updateProfile(copy, null)) {
                user.setNom(nom); user.setPrenom(prenom); user.setGender(labelToGender(genderBox.getValue()));
                if (isMedecin && finalLangField != null) {
                    String langs = finalLangField.getText().trim();
                    if (!langs.isEmpty()) svc.updateMedecinLanguages(user.getId(), langs);
                }
                onUpdated.accept(user);
                showFieldSuccess(status, "Informations personnelles mises à jour !");
            } else {
                showFieldError(status, "Erreur lors de la sauvegarde.");
            }
        });

        String userEmail = user.getEmail() == null ? "" : user.getEmail().trim().toLowerCase();

        Runnable updateBioUI = () -> {
            boolean hasBio = user.getBiometricId() != null && !user.getBiometricId().isBlank();
            if (hasBio) {
                enableFingerprintBtn.setText("Changer l'empreinte");
                deleteBioBtn.setVisible(true);
                deleteBioBtn.setManaged(true);
            } else {
                enableFingerprintBtn.setText("Activer l'empreinte locale");
                deleteBioBtn.setVisible(false);
                deleteBioBtn.setManaged(false);
            }
            enableFingerprintBtn.setDisable(false);
            deleteBioBtn.setDisable(false);
        };

        updateBioUI.run();

        enableFingerprintBtn.setOnAction(e -> {
            Stage owner = enableFingerprintBtn.getScene() != null
                    ? (Stage) enableFingerprintBtn.getScene().getWindow() : null;
            showFingerprintSetupDialog(owner, user, userEmail, svc, status, updateBioUI);
        });

        deleteBioBtn.setOnAction(e -> {
            if (svc.updateBiometricId(user.getId(), null)) {
                AuthPreferenceUtil.setBiometricEnabled(userEmail, false);
                AuthPreferenceUtil.setBiometricToken(userEmail, null);
                user.setBiometricId(null);
                showFieldSuccess(status, "Empreinte supprimée de votre profil.");
            } else {
                showFieldError(status, "Impossible de supprimer l'empreinte.");
            }
            updateBioUI.run();
        });

        List<Node> items = new ArrayList<>();
        items.add(fieldGroup("Prénom *", prenomField));
        items.add(fieldGroup("Nom *", nomField));
        items.add(fieldGroup("Genre", genderBox));
        if (isMedecin && langField != null) items.add(fieldGroup("Langues parlées", langField));
        items.add(status);
        items.add(new HBox(10, save, enableFingerprintBtn, deleteBioBtn));

        return wrap(tileCard("#F48FB1", items));
    }

    /** Tile 2 — blood type (dropdown) + allergies (text area) */
    public static Node createMedicalSection(User user, Consumer<User> onUpdated) {
        UserService svc = new UserService();

        ComboBox<String> bloodBox = new ComboBox<>();
        bloodBox.getItems().addAll("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
        bloodBox.setPromptText("Groupe sanguin");
        if (user.getBloodType() != null) bloodBox.setValue(user.getBloodType());
        styleCombo(bloodBox);

        javafx.scene.control.TextArea allergyArea = new javafx.scene.control.TextArea(
            user.getAllergies() != null ? user.getAllergies() : "");
        allergyArea.setPromptText("Ex: Pénicilline, Arachides, Latex...");
        allergyArea.setPrefRowCount(3);
        allergyArea.setWrapText(true);
        allergyArea.setStyle("-fx-background-radius: 10; -fx-border-radius: 10;" +
                             "-fx-border-color: #d1d5db; -fx-font-size: 13px;");

        Label status = statusLabel();
        Button save = primaryButton("Enregistrer", "#43A047");
        save.setOnAction(e -> {
            if (bloodBox.getValue() == null) {
                showFieldError(status, "Veuillez sélectionner votre groupe sanguin."); return;
            }
            User copy = copyUser(user);
            copy.setBloodType(bloodBox.getValue());
            copy.setAllergies(allergyArea.getText().trim().isEmpty() ? null : allergyArea.getText().trim());
            if (svc.updateProfile(copy, null)) {
                user.setBloodType(bloodBox.getValue());
                user.setAllergies(copy.getAllergies());
                onUpdated.accept(user);
                showFieldSuccess(status, "Informations médicales mises à jour !");
            } else {
                showFieldError(status, "Erreur lors de la sauvegarde.");
            }
        });

        return wrap(tileCard("#A5D6A7",
            List.of(fieldGroup("Groupe sanguin *", bloodBox),
                    fieldGroup("Allergies connues", allergyArea),
                    status, save)));
    }

    // Bridge: JS → Java
    public static class MapBridge {
        private final TextField cityField;
        private final TextField adresseField;
        private final double[] latHolder;
        private final double[] lngHolder;
        private final boolean[] hasCoordinates;
        private final Label status;
        private final UserService svc;
        private final User user;
        private final Consumer<User> onUpdated;

        public MapBridge(TextField cityField, TextField adresseField, double[] latHolder, double[] lngHolder, 
                         boolean[] hasCoordinates, Label status, UserService svc, User user, Consumer<User> onUpdated) {
            this.cityField = cityField;
            this.adresseField = adresseField;
            this.latHolder = latHolder;
            this.lngHolder = lngHolder;
            this.hasCoordinates = hasCoordinates;
            this.status = status;
            this.svc = svc;
            this.user = user;
            this.onUpdated = onUpdated;
        }

        public void onLocationPicked(String lat, String lng, String city, String address) {
            System.out.println("Bridge: onLocationPicked -> " + lat + ", " + lng);
            Platform.runLater(() -> {
                cityField.setText(city != null ? city : "");
                adresseField.setText(address != null ? address : "");
                try {
                    latHolder[0] = Double.parseDouble(lat);
                    lngHolder[0] = Double.parseDouble(lng);
                    hasCoordinates[0] = true;
                    status.setText(""); 
                } catch (Exception e) { System.out.println("Error parsing coords: " + e.getMessage()); }
            });
        }

        public void onSearchResolved(String lat, String lng, String city, String address) {
            onLocationPicked(lat, lng, city, address);
        }

        public void onSaveLocationRequested(String lat, String lng, String city, String address) {
            Platform.runLater(() -> {
                onLocationPicked(lat, lng, city, address);
                saveLocationSelection(svc, user, onUpdated, status, cityField.getText(), adresseField.getText(), true, latHolder[0], lngHolder[0]);
            });
        }
    }

    public static Node createLocationSection(User user, Consumer<User> onUpdated) {
        UserService svc = new UserService();
        TextField cityField = new TextField(user.getCity() != null ? user.getCity() : "");
        TextField adresseField = new TextField(user.getAdresse() != null ? user.getAdresse() : "");
        double[] latHolder = { user.getLatitude() != null ? user.getLatitude() : 0.0 };
        double[] lngHolder = { user.getLongitude() != null ? user.getLongitude() : 0.0 };
        boolean[] hasCoordinates = { user.getLatitude() != null && user.getLongitude() != null };
        Label status = statusLabel();

        WebView mapView = new WebView();
        mapView.setMinHeight(450);
        mapView.setPrefHeight(600);
        VBox.setVgrow(mapView, Priority.ALWAYS);
        WebEngine engine = mapView.getEngine();

        MapBridge bridge = new MapBridge(cityField, adresseField, latHolder, lngHolder, hasCoordinates, status, svc, user, onUpdated);
        mapView.setUserData(bridge); // Prevent GC

        engine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject win = (JSObject) engine.executeScript("window");
                win.setMember("javaApp", bridge);
                engine.executeScript("forceMapResize()");
                if (hasCoordinates[0]) {
                    engine.executeScript("setInitialLocation(" + latHolder[0] + "," + lngHolder[0] + ","
                            + jsString(user.getCity()) + "," + jsString(user.getAdresse()) + ")");
                } else if (user.getCity() != null && !user.getCity().isBlank()) {
                    String q = (user.getAdresse() == null || user.getAdresse().isBlank()) ? user.getCity() : user.getAdresse() + ", " + user.getCity();
                    engine.executeScript("searchAndCenter(" + jsString(q) + ")");
                }
            }
        });

        var mapUrl = HelloApplication.class.getResource("map.html");
        if (mapUrl != null) engine.load(mapUrl.toExternalForm());
        else engine.loadContent("<html><body style='display:flex;align-items:center;justify-content:center;height:100%;font-family:Arial;color:#999'>Carte non disponible</body></html>");

        // ── Action panel — only status and save button ──
        Button save = primaryButton("Confirmer et Enregistrer la position", "#F57C00");
        save.setOnAction(e -> {
            saveLocationSelection(svc, user, onUpdated, status, cityField.getText(), adresseField.getText(), hasCoordinates[0], latHolder[0], lngHolder[0]);
        });

        VBox actionPanel = new VBox(15, status, save);
        actionPanel.setPadding(new Insets(20));
        actionPanel.setAlignment(Pos.CENTER);
        actionPanel.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-width: 1 0 0 0;");

        VBox container = new VBox(0, mapView, actionPanel);
        container.setStyle("-fx-background-color: white;");
        return container;
    }

    private static void saveLocationSelection(
            UserService svc,
            User user,
            Consumer<User> onUpdated,
            Label status,
            String cityValue,
            String addressValue,
            boolean hasCoordinates,
            double latitude,
            double longitude
    ) {
        String city = cityValue == null ? "" : cityValue.trim();
        String address = addressValue == null ? "" : addressValue.trim();
        
        if (city.isEmpty()) {
            showFieldError(status, "La ville est obligatoire.");
            return;
        }
        
        if (!hasCoordinates) {
            showFieldError(status, "Position manquante. Cliquez sur la carte ou recherchez une adresse.");
            return;
        }

        String savedAddress = address.isEmpty() ? null : address;
        boolean success = svc.updateLocation(user.getId(), city, savedAddress, latitude, longitude);
        
        if (success) {
            user.setCity(city);
            user.setAdresse(savedAddress);
            user.setLatitude(latitude);
            user.setLongitude(longitude);
            onUpdated.accept(user);
            showFieldSuccess(status, "Localisation enregistrée avec succès !");
        } else {
            showFieldError(status, "Échec de l'enregistrement. Vérifiez votre connexion ou contactez le support.");
        }
    }

    /** Tile 4 — phone + email */
    public static Node createContactSection(User user, Consumer<User> onUpdated) {
        UserService svc = new UserService();

        TextField phoneField = styledTextField(user.getNumero(), "Numéro de téléphone *");
        TextField emailField = styledTextField(user.getEmail(),  "Adresse email *");

        Label status = statusLabel();
        Button save = primaryButton("Enregistrer", "#C2185B");
        save.setOnAction(e -> {
            String phone = phoneField.getText().trim();
            String email = emailField.getText().trim().toLowerCase();
            if (phone.isEmpty() || email.isEmpty()) {
                showFieldError(status, "Téléphone et email sont obligatoires."); return;
            }
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                showFieldError(status, "Numéro invalide (8-15 chiffres, + optionnel)."); return;
            }
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                showFieldError(status, "Adresse email invalide."); return;
            }
            User copy = copyUser(user);
            copy.setNumero(phone); copy.setEmail(email);
            if (svc.updateProfile(copy, null)) {
                user.setNumero(phone); user.setEmail(email);
                onUpdated.accept(user);
                showFieldSuccess(status, "Coordonnées mises à jour !");
            } else {
                showFieldError(status, "Erreur — email peut-être déjà utilisé.");
            }
        });

        return wrap(tileCard("#F48FB1",
            List.of(fieldGroup("Téléphone *", phoneField),
                    fieldGroup("Email *", emailField),
                    status, save)));
    }

    /** Tile 5 — photo picker with live preview + camera capture */
    public static Node createPhotoSection(User user, Window owner, Consumer<User> onUpdated) {
        UserService svc = new UserService();

        ImageView preview = createCircularPreview(100);
        updatePreview(preview, user.getPhoto());

        Label pathLabel = new Label(user.getPhoto() == null ? "Aucune photo sélectionnée." : user.getPhoto());
        pathLabel.setWrapText(true);
        pathLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9E9E9E;");

        final java.nio.file.Path[] chosen = {null};

        Button chooseBtn = secondaryButton("📁 Parcourir…", "#546E7A");
        chooseBtn.setOnAction(ev -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choisir une photo");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File f = fc.showOpenDialog(owner);
            if (f != null) {
                try {
                    Path cropped = openProfileCropDialog(f.toPath(), owner);
                    if (cropped != null) {
                        chosen[0] = cropped;
                        pathLabel.setText(f.getName() + " (recadree)");
                        updatePreview(preview, cropped.toUri().toString());
                    }
                } catch (Exception ex) {
                    chosen[0] = f.toPath();
                    pathLabel.setText(f.getName());
                    updatePreview(preview, f.toURI().toString());
                }
            }
        });

        Button cameraBtn = secondaryButton("📸 Prendre une photo", "#C2185B");
        cameraBtn.setOnAction(ev -> openCameraPopup(preview, pathLabel, chosen, owner));

        Label status = statusLabel();
        Button save = primaryButton("Enregistrer la photo", "#546E7A");
        save.setOnAction(e -> {
            if (chosen[0] == null) { showFieldError(status, "Veuillez choisir ou capturer une photo."); return; }
            try {
                String stored = FileStorageUtil.copyToUploads(chosen[0], "profiles");
                User copy = copyUser(user); copy.setPhoto(stored);
                if (svc.updateProfile(copy, null)) {
                    user.setPhoto(stored);
                    onUpdated.accept(user);
                    showFieldSuccess(status, "Photo de profil mise à jour !");
                } else { showFieldError(status, "Erreur lors de la sauvegarde."); }
            } catch (Exception ex) { showFieldError(status, "Erreur: " + ex.getMessage()); }
        });

        HBox btnRow = new HBox(10, chooseBtn, cameraBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        HBox previewRow = new HBox(16, preview, new VBox(10, btnRow, pathLabel));
        previewRow.setAlignment(Pos.CENTER_LEFT);

        return wrap(tileCard("#B0BEC5", List.of(previewRow, status, save)));
    }

    private static void openCameraPopup(ImageView previewImg, Label pathLbl,
                                         java.nio.file.Path[] chosen, Window owner) {
        WebView camView = new WebView();
        camView.setPrefSize(400, 360);
        WebEngine engine = camView.getEngine();

        final Stage[] camRef = {null};
        Object bridge = new Object() {
            public void onPhoto(String base64DataUrl) {
                Platform.runLater(() -> {
                    try {
                        String base64 = base64DataUrl.substring(base64DataUrl.indexOf(',') + 1);
                        byte[] bytes = Base64.getDecoder().decode(base64);
                        // Write to temp file, then copy via FileStorageUtil
                        File tmp = File.createTempFile("cam_", ".jpg",
                            new File(System.getProperty("java.io.tmpdir")));
                        try (FileOutputStream fos = new FileOutputStream(tmp)) { fos.write(bytes); }
                        if (camRef[0] != null) camRef[0].close();
                        Path cropped = openProfileCropDialog(tmp.toPath(), owner);
                        chosen[0] = cropped != null ? cropped : createProfilePhotoCrop(tmp.toPath());
                        pathLbl.setText("photo_camera_recadree.jpg");
                        updatePreview(previewImg, chosen[0].toUri().toString());
                    } catch (Exception ex) {
                        System.out.println("Camera save error: " + ex.getMessage());
                        if (camRef[0] != null) camRef[0].close();
                    }
                });
            }
            public void onError(String msg) {
                Platform.runLater(() -> { if (camRef[0] != null) camRef[0].close(); });
            }
        };
        engine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject win = (JSObject) engine.executeScript("window");
                win.setMember("javaBridge", bridge);
            }
        });

        var camUrl = HelloApplication.class.getResource("camera.html");
        if (camUrl != null) engine.load(camUrl.toExternalForm());
        else engine.loadContent("<p style='padding:20px;color:red'>Caméra non disponible</p>");

        Stage stage = new Stage();
        camRef[0] = stage;
        stage.setTitle("📸 Prendre une photo de profil");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setScene(new Scene(camView, 400, 360));
        stage.setResizable(false);
        stage.show();
    }

    /** Tile 6 — email notifications toggle + privacy level */
    public static Node createNotifSection(User user, Consumer<User> onUpdated) {
        UserService svc = new UserService();

        javafx.scene.control.CheckBox notifCheck = new javafx.scene.control.CheckBox(
            "Recevoir les notifications par email");
        notifCheck.setSelected(user.isWantsEmailNotifications());
        notifCheck.setStyle("-fx-font-size: 13px; -fx-text-fill: #212121;");

        ComboBox<String> privacyBox = new ComboBox<>();
        privacyBox.getItems().addAll("public", "private");
        privacyBox.setValue(user.getPrivacyLevel() != null ? user.getPrivacyLevel() : "public");
        styleCombo(privacyBox);

        Label status = statusLabel();
        Button save = primaryButton("Enregistrer", "#F57C00");
        save.setOnAction(e -> {
            boolean wantsEmail = notifCheck.isSelected();
            String privacy = privacyBox.getValue();
            if (svc.updateNotifPreferences(user.getId(), wantsEmail, privacy)) {
                user.setWantsEmailNotifications(wantsEmail);
                user.setPrivacyLevel(privacy);
                onUpdated.accept(user);
                showFieldSuccess(status, "Préférences mises à jour !");
            } else {
                showFieldError(status, "Erreur lors de la sauvegarde.");
            }
        });

        return wrap(tileCard("#FFCC80",
            List.of(fieldGroup("Notifications email", notifCheck),
                    fieldGroup("Niveau de confidentialité", privacyBox),
                    status, save)));
    }

    public static Node createAdminDashboardSection() {
        UserService svc = new UserService();
        
        long totalUsers = svc.getTotalUsersCount();
        long totalMedecins = svc.getTotalMedecinsCount();
        long totalAppts = svc.getTotalAppointmentsCount();
        long newUsers = svc.getNewUsersCount(7);
        long newMedecins = svc.getNewMedecinsCount(7);
        
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(18);
        statsGrid.setVgap(18);
        statsGrid.setAlignment(Pos.CENTER_LEFT);
        
        statsGrid.add(createStatCard("Utilisateurs", String.valueOf(totalUsers), FontAwesomeSolid.USERS, "#7c3aed"), 0, 0);
        statsGrid.add(createStatCard("Médecins", String.valueOf(totalMedecins), FontAwesomeSolid.USER_MD, "#0d9488"), 1, 0);
        statsGrid.add(createStatCard("Rendez-vous", String.valueOf(totalAppts), FontAwesomeSolid.CALENDAR_CHECK, "#2563eb"), 2, 0);
        statsGrid.add(createStatCard("Nouveaux Patients (7j)", "+" + newUsers, FontAwesomeSolid.USER_PLUS, "#ea580c"), 0, 1);
        statsGrid.add(createStatCard("Nouveaux Médecins (7j)", "+" + newMedecins, FontAwesomeSolid.PLUS_CIRCLE, "#16a34a"), 1, 1);
        
        VBox layout = new VBox(25, 
            sectionTitle("Tableau de Bord Admin", FontAwesomeSolid.CHART_BAR, "#111827"),
            statsGrid
        );
        layout.setPadding(new Insets(10));
        return wrap(layout);
    }

    private static VBox createStatCard(String title, String value, FontAwesomeSolid iconType, String color) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setMinWidth(220);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 4); -fx-border-color: #f3f4f6; -fx-border-width: 1;");
        
        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(24);
        icon.setIconColor(Color.web(color));
        
        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");
        
        card.getChildren().addAll(icon, valLbl, titleLbl);
        return card;
    }

    public static Node createAdminManagementSection(Runnable onRefresh) {
        UserService svc = new UserService();
        VBox container = new VBox(25);
        container.setPadding(new Insets(10));

        // Pending Requests
        List<Map<String, Object>> requests = svc.getPendingDoctorRequests();
        VBox requestsBox = new VBox(12);
        if (requests.isEmpty()) {
            Label none = new Label("Aucune demande d'approbation en attente.");
            none.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280; -fx-padding: 10 0;");
            requestsBox.getChildren().add(none);
        } else {
            for (Map<String, Object> req : requests) {
                requestsBox.getChildren().add(createRequestRow(req, svc, onRefresh));
            }
        }

        container.getChildren().addAll(
            sectionTitle("Approbation Médecins", FontAwesomeSolid.USER_CHECK, "#7c3aed"),
            tileCard("#7c3aed", List.of(new Label("Demandes d'inscription en attente de vérification :"), requestsBox))
        );

        return wrap(container);
    }

    private static HBox createRequestRow(Map<String, Object> req, UserService svc, Runnable onRefresh) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 15, 12, 15));
        row.setStyle("-fx-background-color: #f9fafb; -fx-background-radius: 8;");
        
        VBox info = new VBox(4);
        Label name = new Label("Dr. " + req.get("prenom") + " " + req.get("nom"));
        name.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");
        Label email = new Label(String.valueOf(req.get("email")));
        email.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        info.getChildren().addAll(name, email);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button approveBtn = new Button("Approuver");
        approveBtn.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-font-size: 12px; -fx-background-radius: 6; -fx-cursor: hand;");
        approveBtn.setOnAction(e -> {
            if (svc.approveDoctorRequest((int)req.get("id"), (int)req.get("user_id"))) {
                onRefresh.run();
            }
        });
        
        Button rejectBtn = new Button("Rejeter");
        rejectBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-size: 12px; -fx-background-radius: 6; -fx-cursor: hand;");
        rejectBtn.setOnAction(e -> {
            if (svc.rejectDoctorRequest((int)req.get("id"))) {
                onRefresh.run();
            }
        });
        
        row.getChildren().addAll(info, spacer, approveBtn, rejectBtn);
        return row;
    }

    public static Node createMatchDoctorsSection(User user) {
        UserService svc = new UserService();
        if (user.getLatitude() == null || user.getLongitude() == null) {
            Label missing = new Label("Veuillez enregistrer votre position dans le profil avant d'utiliser Match Doctors.");
            missing.setWrapText(true);
            missing.setStyle("-fx-font-size: 14px; -fx-text-fill: #b91c1c;");
            return wrap(tileCard("#FCA5A5", List.of(missing)));
        }

        List<DoctorDistance> nearest = svc.getNearestDoctors(user.getId(), 5);
        List<DoctorDistance> allDoctors = svc.getDoctorsWithCoordinates(user.getId(), user.getLatitude(), user.getLongitude());

        VBox nearestBox = new VBox(10);
        if (nearest.isEmpty()) {
            Label none = new Label("Aucun médecin géolocalisé trouvé pour le moment.");
            none.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");
            nearestBox.getChildren().add(none);
        } else {
            int rank = 1;
            for (DoctorDistance d : nearest) {
                Label row = new Label(
                    "#" + rank + "  Dr. " + d.getFullName()
                        + "  •  " + formatKm(d.getDistanceKm())
                        + (d.getSpecialite() != null && !d.getSpecialite().isBlank() ? "  •  " + d.getSpecialite() : "")
                );
                row.setWrapText(true);
                row.setStyle("-fx-font-size: 13px; -fx-text-fill: #111827;");
                nearestBox.getChildren().add(row);
                rank++;
            }
        }

        VBox nearestCard = tileCard("#93C5FD", List.of(
            sectionTitle("Match Doctors (KNN)", FontAwesomeSolid.USER_MD, "#2563EB"),
            new Label("Les 5 médecins les plus proches de votre position :"),
            nearestBox
        ));

        WebView mapView = new WebView();
        mapView.setMinHeight(430);
        mapView.setPrefHeight(520);
        mapView.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(mapView, Priority.ALWAYS);
        WebEngine engine = mapView.getEngine();
        engine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                engine.executeScript("setPatientLocation(" + user.getLatitude() + "," + user.getLongitude() + "," + jsString(user.getPrenom() + " " + user.getNom()) + ")");
                engine.executeScript("setDoctors(" + buildDoctorsJsArray(allDoctors) + ")");
            }
        });
        var doctorsMapUrl = HelloApplication.class.getResource("doctor-match-map.html");
        if (doctorsMapUrl != null) engine.load(doctorsMapUrl.toExternalForm());
        else engine.loadContent("<html><body style='display:flex;align-items:center;justify-content:center;font-family:Arial;color:#999;height:100%'>Carte médecins indisponible</body></html>");

        VBox mapCard = tileCard("#86EFAC", List.of(
            sectionTitle("Carte des médecins", FontAwesomeSolid.MAP_MARKER_ALT, "#16A34A"),
            mapView
        ));

        VBox page = new VBox(20, nearestCard, mapCard);
        page.setPadding(new Insets(6));
        return wrap(page);
    }

    // ---- helpers for tile sections ----
    private static VBox tileCard(String borderColor, List<Node> children) {
        VBox card = new VBox(14);
        card.getChildren().addAll(children);
        card.setPadding(new Insets(24));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16;" +
                      "-fx-border-color: " + borderColor + "; -fx-border-radius: 16; -fx-border-width: 1.5;");
        return card;
    }

    private static VBox fieldGroup(String labelText, Node field) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #6B7280;");
        return new VBox(6, lbl, field);
    }

    private static Label statusLabel() {
        Label lbl = new Label();
        lbl.setWrapText(true);
        lbl.setStyle("-fx-font-size: 12px;");
        return lbl;
    }

    private static void showFieldSuccess(Label lbl, String msg) {
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #16A34A;");
        lbl.setText("✓  " + msg);
    }

    private static void showFieldError(Label lbl, String msg) {
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #DC2626;");
        lbl.setText("✕  " + msg);
    }

    private static void styleCombo(ComboBox<?> box) {
        box.setPrefHeight(38);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setStyle("-fx-background-radius: 10; -fx-border-radius: 10;" +
                     "-fx-border-color: #d1d5db; -fx-font-size: 13px;");
    }

    private static void showFingerprintSetupDialog(Stage owner, User user, String userEmail,
                                                    UserService svc, Label status, Runnable updateBioUI) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        if (owner != null) dialog.initOwner(owner);

        // Header
        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-background-color: #0EA5E9; -fx-padding: 22 24; -fx-background-radius: 16 16 0 0;");
        Label icon = new Label("☝️");
        icon.setStyle("-fx-font-size: 40;");
        Label title = new Label("Activer l'empreinte digitale");
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: white;");
        Label subtitle = new Label("Utilisez votre capteur Windows Hello");
        subtitle.setStyle("-fx-font-size: 12; -fx-text-fill: rgba(255,255,255,0.85);");
        header.getChildren().addAll(icon, title, subtitle);

        // Body
        Label info = new Label(
            "Appuyez sur « Démarrer » pour que Windows Hello ouvre le capteur d'empreinte.\n" +
            "Posez votre doigt sur le capteur quand la fenêtre Windows apparaît.");
        info.setWrapText(true);
        info.setStyle("-fx-font-size: 13; -fx-text-fill: #444; -fx-line-spacing: 3;");

        Label errLabel = new Label("");
        errLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12;");
        errLabel.setVisible(false);
        errLabel.setManaged(false);

        // Spinner — visible while Windows Hello dialog is open
        ProgressIndicator spinner = new ProgressIndicator(-1);
        spinner.setPrefSize(44, 44);
        Label scanLabel = new Label("En attente du capteur…");
        scanLabel.setStyle("-fx-text-fill: #0EA5E9; -fx-font-weight: bold; -fx-font-size: 13;");
        VBox scanBox = new VBox(10, spinner, scanLabel);
        scanBox.setAlignment(Pos.CENTER);
        scanBox.setVisible(false);
        scanBox.setManaged(false);

        Button startBtn = new Button("Démarrer");
        startBtn.setStyle("-fx-background-color: #0EA5E9; -fx-text-fill: white; -fx-font-weight: bold; " +
                          "-fx-background-radius: 10; -fx-padding: 10 28; -fx-cursor: hand; -fx-font-size: 13;");
        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: #F5F5F5; -fx-text-fill: #555; " +
                           "-fx-background-radius: 10; -fx-padding: 10 20; -fx-cursor: hand; -fx-font-size: 13;");
        cancelBtn.setOnAction(ev -> dialog.close());

        HBox btns = new HBox(12, cancelBtn, startBtn);
        btns.setAlignment(Pos.CENTER_RIGHT);

        VBox body = new VBox(18, info, errLabel, scanBox, btns);
        body.setStyle("-fx-padding: 24;");

        VBox root = new VBox(0, header, body);
        root.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-border-radius: 16; " +
                      "-fx-border-color: #E0E0E0; -fx-border-width: 1; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20, 0, 0, 6);");
        root.setPrefWidth(420);

        startBtn.setOnAction(ev -> {
            // Check sensor availability first
            if (!BiometricUtil.isAvailable()) {
                errLabel.setText("Capteur Windows Hello non disponible ou non configuré sur ce PC.");
                errLabel.setVisible(true); errLabel.setManaged(true);
                return;
            }
            startBtn.setDisable(true);
            cancelBtn.setDisable(true);
            errLabel.setVisible(false); errLabel.setManaged(false);
            info.setVisible(false); info.setManaged(false);
            scanBox.setVisible(true); scanBox.setManaged(true);

            // Run in background — Windows Hello dialog will appear natively on top
            Thread t = new Thread(() -> {
                BiometricUtil.Result result = BiometricUtil.verify("Medicare — Enregistrement de l'empreinte");
                Platform.runLater(() -> {
                    if (result == BiometricUtil.Result.VERIFIED) {
                        scanLabel.setText("✅  Empreinte vérifiée !");
                        spinner.setVisible(false);
                        PauseTransition delay = new PauseTransition(Duration.seconds(0.8));
                        delay.setOnFinished(done -> {
                            String token = UUID.randomUUID().toString();
                            if (svc.updateBiometricId(user.getId(), token)) {
                                AuthPreferenceUtil.setBiometricEnabled(userEmail, true);
                                AuthPreferenceUtil.setBiometricToken(userEmail, token);
                                user.setBiometricId(token);
                                showFieldSuccess(status, "Empreinte enregistrée avec succès !");
                            } else {
                                showFieldError(status, "Erreur lors de l'enregistrement en base.");
                            }
                            updateBioUI.run();
                            dialog.close();
                        });
                        delay.play();
                    } else if (result == BiometricUtil.Result.UNAVAILABLE) {
                        scanBox.setVisible(false); scanBox.setManaged(false);
                        info.setVisible(true); info.setManaged(true);
                        errLabel.setText("Windows Hello non disponible ou non configuré sur ce poste.");
                        errLabel.setVisible(true); errLabel.setManaged(true);
                        startBtn.setDisable(false);
                        cancelBtn.setDisable(false);
                    } else {
                        // FAILED = user cancelled or wrong finger
                        scanBox.setVisible(false); scanBox.setManaged(false);
                        info.setVisible(true); info.setManaged(true);
                        errLabel.setText("Empreinte non reconnue ou annulée. Réessayez.");
                        errLabel.setVisible(true); errLabel.setManaged(true);
                        startBtn.setDisable(false);
                        cancelBtn.setDisable(false);
                    }
                });
            });
            t.setDaemon(true);
            t.start();
        });

        Scene scene = new Scene(root);
        scene.setFill(null);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private static String labelToGender(String label) {
        if (label == null) return null;
        return switch (label) {
            case "Homme" -> "male";
            case "Femme" -> "female";
            case "Autre" -> "other";
            default -> label;
        };
    }

    private static String genderToLabel(String gender) {
        if (gender == null) return null;
        return switch (gender) {
            case "male"   -> "Homme";
            case "female" -> "Femme";
            case "other"  -> "Autre";
            default -> gender;
        };
    }

    private static User copyUser(User src) {
        User copy = new User();
        copy.setId(src.getId());
        copy.setNom(src.getNom());
        copy.setPrenom(src.getPrenom());
        copy.setEmail(src.getEmail());
        copy.setNumero(src.getNumero());
        copy.setAdresse(src.getAdresse());
        copy.setPhoto(src.getPhoto());
        copy.setRoles(src.getRoles());
        copy.setIsVerified(src.isVerified());
        copy.setPrivacyLevel(src.getPrivacyLevel());
        copy.setCity(src.getCity());
        copy.setLatitude(src.getLatitude());
        copy.setLongitude(src.getLongitude());
        copy.setGender(src.getGender());
        copy.setBloodType(src.getBloodType());
        copy.setAllergies(src.getAllergies());
        copy.setWantsEmailNotifications(src.isWantsEmailNotifications());
        return copy;
    }

    // =========================================================
    //  ORIGINAL FULL-SETTINGS SECTION (kept for dashboard use)
    // =========================================================

    public static Node createSettingsSection(
        User currentUser,
        Window owner,
        Consumer<User> onUpdated,
        Runnable onDeleteAccount,
        Runnable onAdvancedSettings
    ) {
        VBox page = new VBox(24,
            buildProfileHeader(currentUser),
            buildSettingsCard(currentUser, owner, onUpdated, onDeleteAccount, onAdvancedSettings)
        );
        page.setPadding(new Insets(6));
        return wrap(page);
    }

    public static Node createNotificationsSection(User currentUser) {
        VBox page = new VBox(24, buildNotificationCard(currentUser));
        page.setPadding(new Insets(6));
        return wrap(page);
    }

    // =========================================================
    //  BLOCK LIST SECTION
    // =========================================================
    public static Node createBlockListSection(User currentUser) {
        com.medicare.services.BlockService blockSvc = new com.medicare.services.BlockService();

        // Header card
        VBox header = new VBox(4);
        header.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                        "-fx-padding: 20 24; -fx-border-color: #FEE2E2; -fx-border-radius: 12; -fx-border-width: 1.5;");
        Label title = new Label("🚫  Liste noire");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #DC2626;");
        Label sub = new Label("Utilisateurs que vous avez bloqués. Ils ne peuvent plus vous contacter ni voir votre profil.");
        sub.setWrapText(true);
        sub.setStyle("-fx-font-size: 13; -fx-text-fill: #6B7280;");
        header.getChildren().addAll(title, sub);

        // List container — rebuilt by refresh lambda
        VBox listBox = new VBox(10);
        listBox.setStyle("-fx-padding: 0;");

        Label emptyLabel = new Label("Aucun utilisateur bloqué.");
        emptyLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #9CA3AF; -fx-padding: 30 0;");
        emptyLabel.setMaxWidth(Double.MAX_VALUE);
        emptyLabel.setAlignment(javafx.geometry.Pos.CENTER);

        Runnable[] refresh = {null};
        refresh[0] = () -> {
            listBox.getChildren().clear();
            java.util.List<com.medicare.services.BlockService.BlockedUser> blocked =
                    blockSvc.getBlockList(currentUser.getId());
            if (blocked.isEmpty()) {
                listBox.getChildren().add(emptyLabel);
                return;
            }
            for (com.medicare.services.BlockService.BlockedUser bu : blocked) {
                listBox.getChildren().add(buildBlockedUserRow(bu, currentUser, blockSvc, refresh));
            }
        };
        refresh[0].run();

        VBox page = new VBox(16, header, listBox);
        page.setStyle("-fx-padding: 16; -fx-background-color: #F9FAFB;");
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #F9FAFB; -fx-background-color: #F9FAFB; -fx-border-color: transparent;");
        return scroll;
    }

    private static javafx.scene.Node buildBlockedUserRow(
            com.medicare.services.BlockService.BlockedUser bu,
            User currentUser,
            com.medicare.services.BlockService blockSvc,
            Runnable[] refresh) {

        HBox row = new HBox(14);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 14 18; " +
                     "-fx-border-color: #F3F4F6; -fx-border-radius: 12; -fx-border-width: 1;");

        // Avatar / initials
        StackPane avatar = new StackPane();
        avatar.setPrefSize(46, 46);
        avatar.setMinSize(46, 46);
        Circle circle = new Circle(23, Color.web("#FEE2E2"));
        String initials = "";
        if (bu.prenom() != null && !bu.prenom().isEmpty()) initials += Character.toUpperCase(bu.prenom().charAt(0));
        if (bu.nom()    != null && !bu.nom().isEmpty())    initials += Character.toUpperCase(bu.nom().charAt(0));
        Label initLbl = new Label(initials.isEmpty() ? "?" : initials);
        initLbl.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #DC2626;");
        avatar.getChildren().addAll(circle, initLbl);

        if (bu.photo() != null && !bu.photo().isBlank()) {
            try {
                String src = bu.photo().startsWith("file:/") ? bu.photo() : java.nio.file.Path.of(bu.photo()).toUri().toString();
                ImageView iv = new ImageView(new Image(src, 46, 46, false, true));
                iv.setClip(new Circle(23, 23, 23));
                iv.setFitWidth(46); iv.setFitHeight(46);
                avatar.getChildren().clear();
                avatar.getChildren().add(iv);
            } catch (Exception ignored) {}
        }

        // Info
        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nameLbl = new Label(bu.displayName());
        nameLbl.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #111827;");
        Label emailLbl = new Label(bu.email() != null ? bu.email() : "");
        emailLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #6B7280;");

        // Role badge + date
        HBox meta = new HBox(8);
        meta.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label roleLbl = new Label(bu.roleLabel());
        String roleColor = bu.roleLabel().equals("Médecin") ? "#D1FAE5" : "#DBEAFE";
        String roleTxt  = bu.roleLabel().equals("Médecin") ? "#065F46" : "#1E40AF";
        roleLbl.setStyle("-fx-background-color: " + roleColor + "; -fx-text-fill: " + roleTxt + "; " +
                         "-fx-background-radius: 20; -fx-padding: 2 10; -fx-font-size: 11; -fx-font-weight: bold;");
        Label dateLbl = new Label("Bloqué le " + (bu.blockedAt() != null ? bu.blockedAt() : "—"));
        dateLbl.setStyle("-fx-font-size: 11; -fx-text-fill: #9CA3AF;");
        meta.getChildren().addAll(roleLbl, dateLbl);
        info.getChildren().addAll(nameLbl, emailLbl, meta);

        // Unblock button
        Button unblockBtn = new Button("Débloquer");
        unblockBtn.setStyle("-fx-background-color: white; -fx-text-fill: #DC2626; -fx-font-weight: bold; " +
                            "-fx-background-radius: 8; -fx-border-color: #FCA5A5; -fx-border-radius: 8; " +
                            "-fx-border-width: 1.5; -fx-cursor: hand; -fx-padding: 7 16; -fx-font-size: 12;");
        unblockBtn.setOnAction(e -> {
            unblockBtn.setDisable(true);
            unblockBtn.setText("...");
            if (blockSvc.unblockUser(currentUser.getId(), bu.id())) {
                refresh[0].run();
            } else {
                unblockBtn.setDisable(false);
                unblockBtn.setText("Débloquer");
            }
        });

        row.getChildren().addAll(avatar, info, unblockBtn);
        return row;
    }

    public static Node createDoctorRequestSection(User currentUser, Window owner) {
        UserService userService = new UserService();

        Label title = sectionTitle("Devenir un medecin", FontAwesomeSolid.USER_MD, "#ca8a04");
        Label helper = new Label("Joignez 1 fichier PDF pour le certificat et 2 images pour la CIN.");
        helper.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        Label certificateLabel = new Label("Aucun certificat selectionne.");
        certificateLabel.setWrapText(true);
        certificateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        Label cinLabel = new Label("Aucune image CIN selectionnee.");
        cinLabel.setWrapText(true);
        cinLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #dc2626;");

        final Path[] certificatePath = {null};
        final List<Path> cinPaths = new ArrayList<>();

        Button chooseCertificateBtn = secondaryButton("Choisir le PDF", "#ca8a04");
        chooseCertificateBtn.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choisir le certificat");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            File file = chooser.showOpenDialog(owner);
            if (file != null) {
                certificatePath[0] = file.toPath();
                certificateLabel.setText(file.getAbsolutePath());
            }
        });

        Button chooseCinBtn = secondaryButton("Choisir les 2 images", "#ca8a04");
        chooseCinBtn.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choisir les photos CIN");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            List<File> files = chooser.showOpenMultipleDialog(owner);
            if (files != null && !files.isEmpty()) {
                cinPaths.clear();
                files.stream().limit(2).map(File::toPath).forEach(cinPaths::add);
                cinLabel.setText(cinPaths.size() == 2
                    ? files.get(0).getAbsolutePath() + "\n" + files.get(1).getAbsolutePath()
                    : "Veuillez choisir exactement 2 images.");
            }
        });

        Button sendBtn = primaryButton("Envoyer la demande", "#ca8a04");
        sendBtn.setOnAction(event -> {
            if (certificatePath[0] == null) {
                statusLabel.setText("Le certificat PDF est obligatoire.");
                return;
            }
            if (cinPaths.size() != 2) {
                statusLabel.setText("Vous devez choisir exactement 2 images CIN.");
                return;
            }

            try {
                String storedCertificate = FileStorageUtil.copyToUploads(certificatePath[0], "doctor-requests/user-" + currentUser.getId());
                List<String> storedCinPaths = new ArrayList<>();
                for (Path cinPath : cinPaths) {
                    storedCinPaths.add(FileStorageUtil.copyToUploads(cinPath, "doctor-requests/user-" + currentUser.getId()));
                }

                boolean created = userService.createDoctorRequest(currentUser.getId(), storedCertificate, storedCinPaths);
                if (!created) {
                    statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #dc2626;");
                    statusLabel.setText("Impossible d'envoyer la demande. Verifiez la table demande_medecin.");
                    return;
                }

                certificatePath[0] = null;
                cinPaths.clear();
                certificateLabel.setText("Aucun certificat selectionne.");
                cinLabel.setText("Aucune image CIN selectionnee.");
                statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #16a34a;");
                statusLabel.setText("Votre demande a ete envoyee avec succes.");
            } catch (Exception ex) {
                statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #dc2626;");
                statusLabel.setText("Erreur lors de l'envoi: " + ex.getMessage());
            }
        });

        VBox card = new VBox(16,
            title,
            helper,
            new Separator(),
            pickerRow("Certificat professionnel (PDF)", certificateLabel, chooseCertificateBtn),
            pickerRow("Photos CIN (2 images)", cinLabel, chooseCinBtn),
            statusLabel,
            sendBtn
        );
        card.setPadding(new Insets(28));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-color: #fde68a; -fx-border-radius: 20;");

        return wrap(card);
    }

    private static VBox buildProfileHeader(User currentUser) {
        return buildProfileHeader(currentUser, null);
    }

    private static VBox buildProfileHeader(User currentUser, Map<String, Runnable> quickNavActions) {
        ImageView profileImage = createCircularPreview(120);
        updatePreview(profileImage, currentUser.getPhoto());

        Label fullName = new Label(currentUser.getPrenom() + " " + currentUser.getNom());
        fullName.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label role = new Label(resolveRole(currentUser));
        role.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #4b5563;");

        HBox verificationField = createVerificationField(currentUser.isVerified());

        VBox box = new VBox(10, profileImage, fullName, role, verificationField);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(28));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 22; -fx-border-color: #e5e7eb; -fx-border-radius: 22;");

        if (quickNavActions != null && !quickNavActions.isEmpty()) {
            HBox quickNav = buildQuickNavRow(quickNavActions);
            box.getChildren().add(quickNav);
        }
        return box;
    }

    private static HBox buildQuickNavRow(Map<String, Runnable> actions) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(14, 0, 0, 0));

        addQuickNavButton(row, actions, "posts",       "Posts",       FontAwesomeSolid.COMMENTS,      "#7c3aed", "#ede9fe");
        addQuickNavButton(row, actions, "rendezvous",  "Rendez-vous", FontAwesomeSolid.CALENDAR_ALT,  "#0d9488", "#ccfbf1");
        addQuickNavButton(row, actions, "collab",      "Collab",      FontAwesomeSolid.HANDSHAKE,     "#c2410c", "#ffedd5");
        addQuickNavButton(row, actions, "donation",    "Donation",    FontAwesomeSolid.HEART,         "#dc2626", "#fee2e2");

        return row;
    }

    private static void addQuickNavButton(HBox row, Map<String, Runnable> actions, String key,
                                          String label, FontAwesomeSolid iconType,
                                          String fg, String bg) {
        Runnable action = actions.get(key);
        if (action == null) {
            return;
        }
        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(14);
        icon.setIconColor(Color.web(fg));

        Button btn = new Button(label, icon);
        btn.setContentDisplay(ContentDisplay.LEFT);
        btn.setGraphicTextGap(8);
        btn.setStyle(
            "-fx-background-color: " + bg + ";"
            + " -fx-text-fill: " + fg + ";"
            + " -fx-font-size: 13px; -fx-font-weight: bold;"
            + " -fx-background-radius: 999;"
            + " -fx-border-color: " + fg + "; -fx-border-radius: 999; -fx-border-width: 1;"
            + " -fx-padding: 8 16; -fx-cursor: hand;"
        );
        btn.setOnAction(e -> action.run());
        row.getChildren().add(btn);
    }

    private static VBox buildPublicInfoCard(User currentUser) {
        Label title = sectionTitle("Informations publiques", FontAwesomeSolid.ID_CARD, "#2563eb");
        Label helper = new Label("Seules les informations marquees publiques sont affichees ici.");
        helper.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        VBox infoList = new VBox(14);
        boolean hasPublicInfo = false;

        boolean isPublicProfile = isPublic(currentUser.getPrivacyLevel());
        if (isPublicProfile && hasValue(currentUser.getNumero())) {
            infoList.getChildren().add(createPublicInfoRow("Telephone", currentUser.getNumero(), FontAwesomeSolid.PHONE, "#2563eb"));
            hasPublicInfo = true;
        }
        if (isPublicProfile && hasValue(currentUser.getAdresse())) {
            infoList.getChildren().add(createPublicInfoRow("Adresse", currentUser.getAdresse(), FontAwesomeSolid.MAP_MARKER_ALT, "#2563eb"));
            hasPublicInfo = true;
        }
        if (isPublicProfile && hasValue(currentUser.getEmail())) {
            infoList.getChildren().add(createPublicInfoRow("Email", currentUser.getEmail(), FontAwesomeSolid.ENVELOPE, "#2563eb"));
            hasPublicInfo = true;
        }

        if (!hasPublicInfo) {
            Label empty = new Label("Aucune information publique a afficher.");
            empty.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280;");
            infoList.getChildren().add(empty);
        }

        VBox card = new VBox(16, title, helper, new Separator(), infoList);
        card.setPadding(new Insets(28));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-color: #dbeafe; -fx-border-radius: 20;");
        return card;
    }

    private static VBox buildNotificationCard(User currentUser) {
        // ── Header banner ──────────────────────────────────────────────
        FontIcon bigBell = new FontIcon(FontAwesomeSolid.BELL);
        bigBell.setIconSize(28);
        bigBell.setIconColor(Color.web("#fef3c7"));

        Label headerTitle = new Label("Notifications");
        headerTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label headerSub = new Label("Centre de notifications de votre compte");
        headerSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #fde68a;");

        VBox headerText = new VBox(2, headerTitle, headerSub);
        headerText.setAlignment(Pos.CENTER_LEFT);

        // Unread badge in header
        int unreadCount = currentUser.isVerified() ? 0 : 1;
        Label badge = new Label(unreadCount > 0 ? String.valueOf(unreadCount) : "0");
        badge.setStyle("-fx-background-color: " + (unreadCount > 0 ? "#ef4444" : "#6b7280")
            + "; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;"
            + " -fx-background-radius: 10; -fx-padding: 2 8;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox headerRow = new HBox(14, bigBell, headerText, spacer, badge);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(22, 24, 22, 24));
        headerRow.setStyle(
            "-fx-background-color: linear-gradient(to right, #1a56db, #1e40af);"
            + " -fx-background-radius: 18 18 0 0;"
        );

        // ── Divider label ──────────────────────────────────────────────
        Label sectionLabel = new Label("AUJOURD'HUI");
        sectionLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #9ca3af;"
            + " -fx-letter-spacing: 1.5;");

        // ── Notification item: account verification status ─────────────
        boolean verified = currentUser.isVerified();

        FontIcon stateIcon = new FontIcon(verified ? FontAwesomeSolid.CHECK_CIRCLE : FontAwesomeSolid.CLOCK);
        stateIcon.setIconSize(22);
        stateIcon.setIconColor(Color.web(verified ? "#16a34a" : "#f59e0b"));

        // Icon container with colored background circle
        StackPane iconCircle = new StackPane(stateIcon);
        iconCircle.setMinSize(44, 44);
        iconCircle.setMaxSize(44, 44);
        iconCircle.setStyle("-fx-background-color: " + (verified ? "#f0fdf4" : "#fffbeb")
            + "; -fx-background-radius: 22;");

        Label headline = new Label(verified
            ? "Compte verifie avec succes !"
            : "En attente de verification");
        headline.setWrapText(true);
        headline.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label body = new Label(verified
            ? "L'administrateur a valide votre compte. Vous beneficiez maintenant du statut Verifie."
            : "Votre compte est en cours d'examen. Vous serez notifie des que l'administrateur le valide.");
        body.setWrapText(true);
        body.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        Label timeLabel = new Label("Aujourd'hui");
        timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");

        // Status chip
        Label chip = new Label(verified ? "Verifie" : "En attente");
        chip.setStyle("-fx-background-color: " + (verified ? "#dcfce7" : "#fef9c3")
            + "; -fx-text-fill: " + (verified ? "#16a34a" : "#a16207")
            + "; -fx-font-size: 11px; -fx-font-weight: bold;"
            + " -fx-background-radius: 8; -fx-padding: 2 10;");

        HBox chipRow = new HBox(8, timeLabel, chip);
        chipRow.setAlignment(Pos.CENTER_LEFT);

        VBox textBox = new VBox(5, headline, body, chipRow);
        textBox.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox notifItem = new HBox(14, iconCircle, textBox);
        notifItem.setAlignment(Pos.TOP_LEFT);
        notifItem.setPadding(new Insets(16, 18, 16, 0));

        // Left accent border
        Region leftBorder = new Region();
        leftBorder.setMinWidth(4);
        leftBorder.setMaxWidth(4);
        leftBorder.setStyle("-fx-background-color: " + (verified ? "#22c55e" : "#f59e0b")
            + "; -fx-background-radius: 4 0 0 4;");

        HBox notifRow = new HBox(0, leftBorder, notifItem);
        notifRow.setAlignment(Pos.TOP_LEFT);
        notifRow.setPadding(new Insets(0, 18, 0, 16));
        notifRow.setStyle("-fx-background-color: " + (verified ? "#f0fdf4" : "#fffbeb")
            + "; -fx-background-radius: 14; -fx-border-color: " + (verified ? "#bbf7d0" : "#fde68a")
            + "; -fx-border-radius: 14; -fx-border-width: 1;");

        // ── "No more notifications" footer ────────────────────────────
        FontIcon doneIcon = new FontIcon(FontAwesomeSolid.CHECK_DOUBLE);
        doneIcon.setIconSize(14);
        doneIcon.setIconColor(Color.web("#d1d5db"));
        Label doneLabel = new Label("Vous etes a jour");
        doneLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #d1d5db;");
        HBox doneRow = new HBox(6, doneIcon, doneLabel);
        doneRow.setAlignment(Pos.CENTER);
        doneRow.setPadding(new Insets(12, 0, 4, 0));

        // ── Assemble card ─────────────────────────────────────────────
        VBox body2 = new VBox(14, sectionLabel, notifRow, doneRow);
        body2.setPadding(new Insets(20, 24, 20, 24));

        VBox card = new VBox(0, headerRow, body2);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20;"
            + " -fx-border-color: #e5e7eb; -fx-border-radius: 20;"
            + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 18, 0, 0, 4);");
        return card;
    }

    private static VBox buildSettingsCard(
        User currentUser,
        Window owner,
        Consumer<User> onUpdated,
        Runnable onDeleteAccount,
        Runnable onAdvancedSettings
    ) {
        UserService userService = new UserService();

        Label title = sectionTitle("Parametres du compte", FontAwesomeSolid.COG, "#2563eb");
        Label helper = new Label("Modifiez vos informations, votre photo, votre mot de passe et vos regles de confidentialite.");
        helper.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        ImageView preview = createCircularPreview(92);
        updatePreview(preview, currentUser.getPhoto());

        TextField nomField = styledTextField(currentUser.getNom(), "Nom");
        TextField prenomField = styledTextField(currentUser.getPrenom(), "Prenom");
        TextField emailField = styledTextField(currentUser.getEmail(), "Email");
        TextField numeroField = styledTextField(currentUser.getNumero(), "Numero de telephone");
        TextField adresseField = styledTextField(currentUser.getAdresse(), "Adresse");
        PasswordField passwordField = styledPasswordField("Nouveau mot de passe");
        PasswordField confirmPasswordField = styledPasswordField("Confirmer le mot de passe");

        ComboBox<String> privacyLevelBox = privacyBox(currentUser.getPrivacyLevel());

        Label photoPathLabel = new Label(currentUser.getPhoto() == null ? "Aucune photo selectionnee." : currentUser.getPhoto());
        photoPathLabel.setWrapText(true);
        photoPathLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        final Path[] selectedPhotoPath = {null};

        Button choosePhotoBtn = secondaryButton("Choisir une photo", "#2563eb");
        choosePhotoBtn.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choisir une photo");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File file = chooser.showOpenDialog(owner);
            if (file != null) {
                selectedPhotoPath[0] = file.toPath();
                photoPathLabel.setText(file.getAbsolutePath());
                updatePreview(preview, file.toURI().toString());
            }
        });

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #dc2626;");

        Button saveBtn = primaryButton("Enregistrer", "#2563eb");
        saveBtn.setOnAction(event -> {
            String validationError = validateProfileFields(
                nomField.getText(), prenomField.getText(), emailField.getText(), numeroField.getText(),
                passwordField.getText(), confirmPasswordField.getText()
            );
            if (validationError != null) {
                statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #dc2626;");
                statusLabel.setText(validationError);
                return;
            }

            try {
                User updatedUser = new User();
                updatedUser.setId(currentUser.getId());
                updatedUser.setNom(nomField.getText().trim());
                updatedUser.setPrenom(prenomField.getText().trim());
                updatedUser.setEmail(emailField.getText().trim().toLowerCase());
                updatedUser.setNumero(numeroField.getText().trim());
                updatedUser.setAdresse(adresseField.getText().trim().isEmpty() ? null : adresseField.getText().trim());
                updatedUser.setRoles(currentUser.getRoles());
                updatedUser.setIsVerified(currentUser.isVerified());
                updatedUser.setPhoto(currentUser.getPhoto());
                updatedUser.setPrivacyLevel(privacyLevelBox.getValue());
                updatedUser.setCity(currentUser.getCity());
                updatedUser.setLatitude(currentUser.getLatitude());
                updatedUser.setLongitude(currentUser.getLongitude());
                updatedUser.setGender(currentUser.getGender());
                updatedUser.setBloodType(currentUser.getBloodType());
                updatedUser.setAllergies(currentUser.getAllergies());

                if (selectedPhotoPath[0] != null) {
                    updatedUser.setPhoto(FileStorageUtil.copyToUploads(selectedPhotoPath[0], "profiles"));
                }

                boolean updated = userService.updateProfile(updatedUser, passwordField.getText().trim());
                if (!updated) {
                    statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #dc2626;");
                    statusLabel.setText("Impossible d'enregistrer. Email deja utilise ou erreur base.");
                    return;
                }

                currentUser.setNom(updatedUser.getNom());
                currentUser.setPrenom(updatedUser.getPrenom());
                currentUser.setEmail(updatedUser.getEmail());
                currentUser.setNumero(updatedUser.getNumero());
                currentUser.setAdresse(updatedUser.getAdresse());
                currentUser.setPhoto(updatedUser.getPhoto());
                currentUser.setPrivacyLevel(updatedUser.getPrivacyLevel());

                passwordField.clear();
                confirmPasswordField.clear();
                selectedPhotoPath[0] = null;
                photoPathLabel.setText(currentUser.getPhoto() == null ? "Aucune photo selectionnee." : currentUser.getPhoto());
                updatePreview(preview, currentUser.getPhoto());
                onUpdated.accept(currentUser);

                statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #16a34a;");
                statusLabel.setText("Profil mis a jour avec succes.");
            } catch (Exception ex) {
                statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #dc2626;");
                statusLabel.setText("Erreur lors de l'enregistrement: " + ex.getMessage());
            }
        });

        Button deleteBtn = new Button("Supprimer mon compte");
        deleteBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-size: 14px; "
            + "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 0 24;");
        deleteBtn.setPrefHeight(40);
        deleteBtn.setOnAction(event -> {
            boolean deleted = userService.deleteUser(currentUser.getId());
            if (!deleted) {
                statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #dc2626;");
                statusLabel.setText("Impossible de supprimer le compte.");
                return;
            }
            onDeleteAccount.run();
        });

        Button advancedBtn = secondaryButton("Parametres avances", "#374151");
        advancedBtn.setOnAction(event -> onAdvancedSettings.run());

        HBox photoRow = new HBox(16, preview, buildPhotoPickerBox(photoPathLabel, choosePhotoBtn));
        photoRow.setAlignment(Pos.CENTER_LEFT);

        HBox actions = new HBox(12, saveBtn, advancedBtn, deleteBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(16,
            title,
            helper,
            new Separator(),
            photoRow,
            twoColumns(nomField, prenomField),
            emailField,
            twoColumns(numeroField, adresseField),
            privacyRow("Niveau de confidentialite", privacyLevelBox, "", null),
            passwordField,
            confirmPasswordField,
            statusLabel,
            actions
        );
        card.setPadding(new Insets(28));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-color: #dbeafe; -fx-border-radius: 20;");
        return card;
    }

    private static VBox buildEmptyPostsCard() {
        Label title = sectionTitle("Posts", FontAwesomeSolid.COMMENTS, "#7c3aed");
        Label helper = new Label("Votre activite publique apparaitra ici.");
        helper.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        FontIcon emptyIcon = new FontIcon(FontAwesomeSolid.COMMENT_SLASH);
        emptyIcon.setIconSize(72);
        emptyIcon.setIconColor(Color.web("#c4b5fd"));

        Label emptyTitle = new Label("Empty");
        emptyTitle.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #6d28d9;");

        Label emptyText = new Label("No post yet");
        emptyText.setStyle("-fx-font-size: 15px; -fx-text-fill: #7c3aed;");

        VBox emptyState = new VBox(12, emptyIcon, emptyTitle, emptyText);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(34, 12, 34, 12));
        emptyState.setStyle("-fx-background-color: linear-gradient(to bottom, #faf5ff, #f5f3ff); -fx-background-radius: 18;");

        VBox card = new VBox(16, title, helper, new Separator(), emptyState);
        card.setPadding(new Insets(28));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-color: #ddd6fe; -fx-border-radius: 20;");
        return card;
    }

    private static String validateProfileFields(String nom, String prenom, String email, String numero, String password, String confirmPassword) {
        if (nom == null || prenom == null || email == null || numero == null
            || nom.trim().isEmpty() || prenom.trim().isEmpty() || email.trim().isEmpty() || numero.trim().isEmpty()) {
            return "Veuillez remplir tous les champs obligatoires.";
        }
        if (!NAME_PATTERN.matcher(nom.trim()).matches() || !NAME_PATTERN.matcher(prenom.trim()).matches()) {
            return "Le nom et le prenom doivent contenir au moins 2 lettres.";
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return "Email invalide.";
        }
        if (!PHONE_PATTERN.matcher(numero.trim()).matches()) {
            return "Numero invalide. Utilisez entre 8 et 15 chiffres.";
        }
        if (!password.isBlank()) {
            if (password.length() < 6) {
                return "Le mot de passe doit contenir au moins 6 caracteres.";
            }
            if (!password.equals(confirmPassword)) {
                return "Les mots de passe ne correspondent pas.";
            }
        }
        return null;
    }

    private static ScrollPane wrap(Node node) {
        ScrollPane scrollPane = new ScrollPane(node);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return scrollPane;
    }

    private static Label sectionTitle(String text, FontAwesomeSolid iconName, String color) {
        FontIcon icon = new FontIcon(iconName);
        icon.setIconColor(Color.web(color));
        icon.setIconSize(18);
        Label title = new Label(text, icon);
        title.setContentDisplay(ContentDisplay.LEFT);
        title.setGraphicTextGap(10);
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        return title;
    }

    private static VBox pickerRow(String labelText, Label valueLabel, Button button) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        return new VBox(8, label, button, valueLabel);
    }

    private static HBox createVerificationField(boolean verified) {
        FontIcon icon = new FontIcon(verified ? FontAwesomeSolid.CHECK_CIRCLE : FontAwesomeSolid.TIMES_CIRCLE);
        icon.setIconSize(16);
        icon.setIconColor(Color.web(verified ? "#16a34a" : "#dc2626"));

        TextField field = new TextField(verified ? "Verified" : "Not verified");
        field.setEditable(false);
        field.setFocusTraversable(false);
        field.setPrefWidth(170);
        field.setAlignment(Pos.CENTER);
        field.setStyle(
            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-background-color: transparent; "
                + "-fx-border-color: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;"
                + (verified ? "-fx-text-fill: #16a34a;" : "-fx-text-fill: #dc2626;")
        );

        HBox box = new HBox(3, icon, field);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private static HBox createPublicInfoRow(String labelText, String valueText, FontAwesomeSolid iconName, String color) {
        FontIcon icon = new FontIcon(iconName);
        icon.setIconSize(15);
        icon.setIconColor(Color.web(color));

        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #374151;");

        Label value = new Label(valueText);
        value.setWrapText(true);
        value.setStyle("-fx-font-size: 14px; -fx-text-fill: #111827;");

        VBox textBox = new VBox(4, label, value);
        HBox row = new HBox(12, icon, textBox);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static Button primaryButton(String text, String color) {
        Button button = new Button(text);
        button.setPrefHeight(40);
        button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-size: 14px; "
            + "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 0 24;");
        return button;
    }

    private static Button secondaryButton(String text, String color) {
        Button button = new Button(text);
        button.setPrefHeight(36);
        button.setStyle("-fx-background-color: white; -fx-text-fill: " + color + "; -fx-font-size: 13px; "
            + "-fx-border-color: " + color + "; -fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;");
        return button;
    }

    private static TextField styledTextField(String value, String prompt) {
        TextField field = new TextField(value == null ? "" : value);
        field.setPromptText(prompt);
        field.setPrefHeight(38);
        field.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #d1d5db; -fx-font-size: 13px;");
        return field;
    }

    private static PasswordField styledPasswordField(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.setPrefHeight(38);
        field.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #d1d5db; -fx-font-size: 13px;");
        return field;
    }

    private static ComboBox<String> privacyBox(String currentValue) {
        ComboBox<String> box = new ComboBox<>();
        box.getItems().addAll("public", "private");
        box.setValue(normalizePrivacy(currentValue));
        box.setPrefHeight(38);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setStyle("-fx-background-radius: 10; -fx-font-size: 13px;");
        return box;
    }

    private static HBox twoColumns(Node left, Node right) {
        HBox row = new HBox(14, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        if (left instanceof Region leftRegion) {
            leftRegion.setMaxWidth(Double.MAX_VALUE);
        }
        if (right instanceof Region rightRegion) {
            rightRegion.setMaxWidth(Double.MAX_VALUE);
        }
        return row;
    }

    private static HBox privacyRow(String leftText, ComboBox<String> leftBox, String rightText, ComboBox<String> rightBox) {
        VBox left = new VBox(6, privacyLabel(leftText), leftBox);
        HBox.setHgrow(left, Priority.ALWAYS);
        left.setMaxWidth(Double.MAX_VALUE);

        if (rightBox == null) {
            return new HBox(14, left);
        }

        VBox right = new VBox(6, privacyLabel(rightText), rightBox);
        HBox.setHgrow(right, Priority.ALWAYS);
        right.setMaxWidth(Double.MAX_VALUE);
        return new HBox(14, left, right);
    }

    private static Label privacyLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        return label;
    }

    private static VBox buildPhotoPickerBox(Label photoPathLabel, Button choosePhotoBtn) {
        VBox box = new VBox(10, choosePhotoBtn, photoPathLabel);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static ImageView createCircularPreview(double size) {
        ImageView preview = new ImageView();
        preview.setFitWidth(size);
        preview.setFitHeight(size);
        preview.setPreserveRatio(false);
        preview.setClip(new Circle(size / 2, size / 2, size / 2));
        return preview;
    }

    private static void updatePreview(ImageView preview, String photoPath) {
        try {
            if (photoPath == null || photoPath.isBlank()) {
                Image fallback = new Image(HelloApplication.class.getResource("images/logo.png").toExternalForm(), true);
                preview.setViewport(null);
                preview.setImage(fallback);
                return;
            }
            String source = photoPath.startsWith("file:/") ? photoPath : Path.of(photoPath).toUri().toString();
            Image image = new Image(source, false);
            preview.setImage(image);
            applyCenteredSquareViewport(preview, image);
        } catch (Exception ignored) {
            preview.setViewport(null);
            preview.setImage(new Image(HelloApplication.class.getResource("images/logo.png").toExternalForm(), true));
        }
    }

    private static void applyCenteredSquareViewport(ImageView view, Image image) {
        double width = image.getWidth();
        double height = image.getHeight();
        if (width <= 0 || height <= 0) {
            image.widthProperty().addListener((obs, oldValue, newValue) -> applyCenteredSquareViewport(view, image));
            image.heightProperty().addListener((obs, oldValue, newValue) -> applyCenteredSquareViewport(view, image));
            return;
        }
        double side = Math.min(width, height);
        view.setViewport(new javafx.geometry.Rectangle2D(
                (width - side) / 2,
                (height - side) / 2,
                side,
                side
        ));
    }

    private static Path createProfilePhotoCrop(Path source) throws IOException {
        BufferedImage original = ImageIO.read(source.toFile());
        if (original == null) {
            return source;
        }

        int side = Math.min(original.getWidth(), original.getHeight());
        int x = (original.getWidth() - side) / 2;
        int y = (original.getHeight() - side) / 2;
        return createProfilePhotoCrop(source, x, y, side);
    }

    private static Path openProfileCropDialog(Path source, Window owner) throws IOException {
        Image image = new Image(source.toUri().toString(), false);
        if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return createProfilePhotoCrop(source);
        }

        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();
        double minSide = Math.min(imageWidth, imageHeight);

        ImageView cropView = new ImageView(image);
        cropView.setFitWidth(320);
        cropView.setFitHeight(320);
        cropView.setPreserveRatio(false);
        cropView.setSmooth(true);

        ImageView circlePreview = createCircularPreview(92);
        circlePreview.setImage(image);

        Slider zoom = new Slider(1, 3, 1);
        zoom.setShowTickMarks(true);
        zoom.setShowTickLabels(true);
        zoom.setMajorTickUnit(1);
        Slider horizontal = new Slider();
        Slider vertical = new Slider();

        Runnable updateCrop = () -> {
            double side = minSide / zoom.getValue();
            horizontal.setMax(Math.max(0, imageWidth - side));
            vertical.setMax(Math.max(0, imageHeight - side));
            double x = Math.min(horizontal.getValue(), horizontal.getMax());
            double y = Math.min(vertical.getValue(), vertical.getMax());
            horizontal.setValue(x);
            vertical.setValue(y);
            javafx.geometry.Rectangle2D viewport = new javafx.geometry.Rectangle2D(x, y, side, side);
            cropView.setViewport(viewport);
            circlePreview.setViewport(viewport);
        };

        double initialSide = minSide / zoom.getValue();
        horizontal.setMax(Math.max(0, imageWidth - initialSide));
        vertical.setMax(Math.max(0, imageHeight - initialSide));
        horizontal.setValue(horizontal.getMax() / 2);
        vertical.setValue(vertical.getMax() / 2);
        updateCrop.run();

        zoom.valueProperty().addListener((obs, oldValue, newValue) -> updateCrop.run());
        horizontal.valueProperty().addListener((obs, oldValue, newValue) -> updateCrop.run());
        vertical.valueProperty().addListener((obs, oldValue, newValue) -> updateCrop.run());

        Label title = new Label("Recadrer la photo de profil");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        Label helper = new Label("Ajustez le cadrage pour obtenir une photo nette et centree.");
        helper.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        StackPane cropFrame = new StackPane(cropView);
        cropFrame.setMinSize(320, 320);
        cropFrame.setMaxSize(320, 320);
        cropFrame.setStyle("-fx-background-color: #111827; -fx-border-color: #e5e7eb; -fx-border-width: 1;");

        VBox controls = new VBox(8,
                privacyLabel("Zoom"), zoom,
                privacyLabel("Position horizontale"), horizontal,
                privacyLabel("Position verticale"), vertical
        );
        controls.setMinWidth(240);

        VBox previewBox = new VBox(10, new Label("Apercu"), circlePreview);
        previewBox.setAlignment(Pos.CENTER);
        previewBox.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        HBox body = new HBox(22, cropFrame, new VBox(18, previewBox, controls));
        body.setAlignment(Pos.CENTER_LEFT);

        Button cancel = secondaryButton("Annuler", "#6b7280");
        Button apply = primaryButton("Utiliser cette photo", "#C2185B");
        HBox actions = new HBox(10, cancel, apply);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(14, title, helper, body, actions);
        root.setPadding(new Insets(22));
        root.setStyle("-fx-background-color: white;");

        Stage stage = new Stage();
        stage.setTitle("Recadrer la photo");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setResizable(false);
        stage.setScene(new Scene(root));

        final Path[] result = { null };
        cancel.setOnAction(event -> stage.close());
        apply.setOnAction(event -> {
            try {
                double side = minSide / zoom.getValue();
                result[0] = createProfilePhotoCrop(
                        source,
                        (int) Math.round(horizontal.getValue()),
                        (int) Math.round(vertical.getValue()),
                        (int) Math.round(side)
                );
                stage.close();
            } catch (IOException ex) {
                stage.close();
            }
        });
        stage.showAndWait();
        return result[0];
    }

    private static Path createProfilePhotoCrop(Path source, int x, int y, int side) throws IOException {
        BufferedImage original = ImageIO.read(source.toFile());
        if (original == null) {
            return source;
        }

        side = Math.max(1, Math.min(side, Math.min(original.getWidth(), original.getHeight())));
        x = Math.max(0, Math.min(x, original.getWidth() - side));
        y = Math.max(0, Math.min(y, original.getHeight() - side));
        BufferedImage cropped = original.getSubimage(x, y, side, side);

        int targetSize = 512;
        BufferedImage output = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        graphics.setColor(java.awt.Color.WHITE);
        graphics.fillRect(0, 0, targetSize, targetSize);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(cropped, 0, 0, targetSize, targetSize, null);
        graphics.dispose();

        File temp = File.createTempFile("profile_crop_", ".jpg");
        temp.deleteOnExit();
        ImageIO.write(output, "jpg", temp);
        return temp.toPath();
    }

    private static String resolveRole(User user) {
        if (user.getRoles() != null && user.getRoles().contains("ROLE_MEDECIN")) {
            return "Medecin";
        }
        return "Patient";
    }

    private static boolean isPublic(String value) {
        return "public".equalsIgnoreCase(value);
    }

    private static boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }

    private static String jsString(String value) {
        if (value == null) {
            return "''";
        }
        return "'" + value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", " ")
                .replace("\n", " ")
                + "'";
    }

    private static String normalizePrivacy(String value) {
        return "public".equalsIgnoreCase(value) ? "public" : "private";
    }

    private static String formatKm(double distanceKm) {
        return String.format(Locale.US, "%.2f km", distanceKm);
    }

    private static String buildDoctorsJsArray(List<DoctorDistance> doctors) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < doctors.size(); i++) {
            DoctorDistance d = doctors.get(i);
            if (i > 0) sb.append(",");
            sb.append("{")
                .append("name:").append(jsString("Dr. " + d.getFullName())).append(",")
                .append("lat:").append(d.getLatitude()).append(",")
                .append("lng:").append(d.getLongitude()).append(",")
                .append("city:").append(jsString(d.getCity())).append(",")
                .append("address:").append(jsString(d.getAdresse())).append(",")
                .append("specialite:").append(jsString(d.getSpecialite())).append(",")
                .append("distanceKm:").append(String.format(Locale.US, "%.4f", d.getDistanceKm()))
                .append("}");
        }
        sb.append("]");
        return sb.toString();
    }
}
