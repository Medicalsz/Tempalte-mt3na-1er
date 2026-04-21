package com.medicare.ui;

import com.medicare.HelloApplication;
import com.medicare.models.User;
import com.medicare.services.UserService;
import com.medicare.utils.FileStorageUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
        VBox page = new VBox(24,
            buildProfileHeader(currentUser),
            buildSettingsCard(currentUser, owner, onUpdated, onDeleteAccount)
        );
        page.setPadding(new Insets(6));
        return wrap(page);
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
        return box;
    }

    private static VBox buildSettingsCard(
        User currentUser,
        Window owner,
        Consumer<User> onUpdated,
        Runnable onDeleteAccount
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

        ComboBox<String> emailPrivacyBox = privacyBox(currentUser.getEmailPrivacy());
        ComboBox<String> phonePrivacyBox = privacyBox(currentUser.getPhonePrivacy());
        ComboBox<String> adressePrivacyBox = privacyBox(currentUser.getAdressePrivacy());

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
                updatedUser.setEmailPrivacy(emailPrivacyBox.getValue());
                updatedUser.setPhonePrivacy(phonePrivacyBox.getValue());
                updatedUser.setAdressePrivacy(adressePrivacyBox.getValue());

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
                currentUser.setEmailPrivacy(updatedUser.getEmailPrivacy());
                currentUser.setPhonePrivacy(updatedUser.getPhonePrivacy());
                currentUser.setAdressePrivacy(updatedUser.getAdressePrivacy());

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

        HBox photoRow = new HBox(16, preview, buildPhotoPickerBox(photoPathLabel, choosePhotoBtn));
        photoRow.setAlignment(Pos.CENTER_LEFT);

        HBox actions = new HBox(12, saveBtn, deleteBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(16,
            title,
            helper,
            new Separator(),
            photoRow,
            twoColumns(nomField, prenomField),
            emailField,
            twoColumns(numeroField, adresseField),
            privacyRow("Confidentialite email", emailPrivacyBox, "Confidentialite telephone", phonePrivacyBox),
            privacyRow("Confidentialite adresse", adressePrivacyBox, "", null),
            passwordField,
            confirmPasswordField,
            statusLabel,
            actions
        );
        card.setPadding(new Insets(28));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-color: #dbeafe; -fx-border-radius: 20;");
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
        field.setStyle(
            "-fx-background-radius: 10; -fx-border-radius: 10; -fx-font-size: 13px; -fx-font-weight: bold;"
                + (verified
                ? "-fx-text-fill: #16a34a; -fx-background-color: #f0fdf4; -fx-border-color: #86efac;"
                : "-fx-text-fill: #dc2626; -fx-background-color: #fef2f2; -fx-border-color: #fca5a5;")
        );

        HBox box = new HBox(8, icon, field);
        box.setAlignment(Pos.CENTER);
        return box;
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
                preview.setImage(new Image(HelloApplication.class.getResource("images/logo.png").toExternalForm(), true));
                return;
            }
            String source = photoPath.startsWith("file:/") ? photoPath : Path.of(photoPath).toUri().toString();
            preview.setImage(new Image(source, true));
        } catch (Exception ignored) {
            preview.setImage(new Image(HelloApplication.class.getResource("images/logo.png").toExternalForm(), true));
        }
    }

    private static String resolveRole(User user) {
        if (user.getRoles() != null && user.getRoles().contains("ROLE_MEDECIN")) {
            return "Medecin";
        }
        return "Patient";
    }

    private static String normalizePrivacy(String value) {
        return "public".equalsIgnoreCase(value) ? "public" : "private";
    }
}
