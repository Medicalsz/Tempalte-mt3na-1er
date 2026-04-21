package com.medicare.ui;

import com.medicare.HelloApplication;
import com.medicare.models.User;
import com.medicare.services.UserService;
import com.medicare.utils.FileStorageUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
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

public final class UserSectionFactory {

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
        java.util.function.Consumer<User> onUpdated,
        Runnable onDeleteAccount
    ) {
        VBox page = new VBox(24,
            buildProfileHeader(currentUser),
            buildPublicInfoCard(currentUser),
            buildEmptyPostsCard()
        );
        page.setPadding(new Insets(6));
        return wrap(page);
    }

    public static Node createNotificationsSection(User currentUser) {
        VBox page = new VBox(24, buildNotificationCard(currentUser));
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

    private static VBox buildPublicInfoCard(User currentUser) {
        Label title = sectionTitle("Informations publiques", FontAwesomeSolid.ID_CARD, "#2563eb");
        Label helper = new Label("Seules les informations marquees publiques sont affichees ici.");
        helper.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        VBox infoList = new VBox(14);
        boolean hasPublicInfo = false;

        if (isPublic(currentUser.getPhonePrivacy()) && hasValue(currentUser.getNumero())) {
            infoList.getChildren().add(createPublicInfoRow("Telephone", currentUser.getNumero(), FontAwesomeSolid.PHONE, "#2563eb"));
            hasPublicInfo = true;
        }
        if (isPublic(currentUser.getAdressePrivacy()) && hasValue(currentUser.getAdresse())) {
            infoList.getChildren().add(createPublicInfoRow("Adresse", currentUser.getAdresse(), FontAwesomeSolid.MAP_MARKER_ALT, "#2563eb"));
            hasPublicInfo = true;
        }
        if (isPublic(currentUser.getEmailPrivacy()) && hasValue(currentUser.getEmail())) {
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
        Label title = sectionTitle("Notifications", FontAwesomeSolid.BELL, "#f59e0b");
        Label helper = new Label("Dernieres nouvelles de votre compte.");
        helper.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        FontIcon stateIcon = new FontIcon(currentUser.isVerified() ? FontAwesomeSolid.CHECK_CIRCLE : FontAwesomeSolid.CLOCK);
        stateIcon.setIconSize(18);
        stateIcon.setIconColor(Color.web(currentUser.isVerified() ? "#16a34a" : "#f59e0b"));

        Label headline = new Label(currentUser.isVerified()
            ? "Votre compte a ete verifie par l'administrateur."
            : "Votre compte est en attente de verification.");
        headline.setWrapText(true);
        headline.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label subline = new Label(currentUser.isVerified()
            ? "Vous pouvez maintenant utiliser votre compte avec le statut verifie."
            : "Vous recevrez ici une notification des que l'administrateur valide votre compte.");
        subline.setWrapText(true);
        subline.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        HBox notification = new HBox(12, stateIcon, new VBox(4, headline, subline));
        notification.setAlignment(Pos.TOP_LEFT);
        notification.setPadding(new Insets(16));
        notification.setStyle(
            "-fx-background-radius: 16; -fx-border-radius: 16;"
                + (currentUser.isVerified()
                ? "-fx-background-color: #f0fdf4; -fx-border-color: #86efac;"
                : "-fx-background-color: #fffbeb; -fx-border-color: #fcd34d;")
        );

        VBox card = new VBox(16, title, helper, new Separator(), notification);
        card.setPadding(new Insets(28));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-color: #fde68a; -fx-border-radius: 20;");
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

        HBox box = new HBox(8, icon, field);
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

    private static boolean isPublic(String value) {
        return "public".equalsIgnoreCase(value);
    }

    private static boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }
}
