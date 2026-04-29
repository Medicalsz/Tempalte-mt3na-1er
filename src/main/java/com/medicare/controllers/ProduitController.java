package com.medicare.controllers;

import com.medicare.models.Produit;
import com.medicare.services.CloudinaryService;
import com.medicare.services.ProduitService;
import com.medicare.utils.Validators;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ProduitController {

    @FXML private VBox container;

    private final ProduitService service = new ProduitService();
    private final CloudinaryService cloudinaryService = new CloudinaryService();
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    private void initialize() {
        loadProduits();
    }

    private void loadProduits() {
        container.getChildren().clear();

        // Header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Gestion des produits");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #7c3aed;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TextField searchField = new TextField();
        searchField.setPromptText("Rechercher...");
        searchField.setPrefWidth(220);
        searchField.setStyle("-fx-background-radius: 8; -fx-font-size: 13px;");
        searchField.textProperty().addListener((obs, old, val) -> filterProduits(val));

        Button btnAdd = new Button("  Ajouter");
        FontIcon addIcon = new FontIcon(FontAwesomeSolid.PLUS);
        addIcon.setIconSize(13);
        addIcon.setIconColor(Color.WHITE);
        btnAdd.setGraphic(addIcon);
        btnAdd.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-size: 13px; " +
                        "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 7 18;");
        btnAdd.setOnAction(e -> showForm(null));

        header.getChildren().addAll(title, spacer, searchField, btnAdd);
        container.getChildren().add(header);

        // Table header
        HBox tableHeader = new HBox();
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        tableHeader.setPadding(new Insets(10, 15, 10, 15));
        tableHeader.setStyle("-fx-background-color: #7c3aed; -fx-background-radius: 8 8 0 0;");

        tableHeader.getChildren().addAll(
            colLabel("Image", 70), colLabel("Nom", 130), colLabel("SKU", 90), colLabel("Type", 95),
            colLabel("Prix", 75), colLabel("Stock", 55), colLabel("Expire", 85),
            colLabel("Actif", 50), colLabel("Actions", 130)
        );
        container.getChildren().add(tableHeader);

        addRows(service.getAll());
    }

    private void addRows(List<Produit> produits) {
        while (container.getChildren().size() > 2) {
            container.getChildren().remove(2);
        }

        if (produits.isEmpty()) {
            Label empty = new Label("Aucun produit trouve.");
            empty.setStyle("-fx-font-size: 14px; -fx-text-fill: #888; -fx-padding: 20;");
            container.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < produits.size(); i++) {
            Produit p = produits.get(i);
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 15, 10, 15));
            row.setStyle("-fx-background-color: " + (i % 2 == 0 ? "white" : "#f9fafb") + ";");

            HBox imgCell = new HBox();
            imgCell.setPrefWidth(70);
            imgCell.setAlignment(Pos.CENTER_LEFT);
            ImageView thumb = new ImageView();
            thumb.setFitWidth(48);
            thumb.setFitHeight(48);
            thumb.setPreserveRatio(true);
            if (p.getImageUrl() != null && !p.getImageUrl().isBlank()) {
                try { thumb.setImage(new Image(p.getImageUrl(), 48, 48, true, true, true)); } catch (Exception ignored) {}
            } else {
                FontIcon ph = new FontIcon(FontAwesomeSolid.IMAGE);
                ph.setIconSize(28);
                ph.setIconColor(Color.web("#d1d5db"));
                imgCell.getChildren().add(ph);
            }
            if (thumb.getImage() != null) imgCell.getChildren().add(thumb);

            Label name = cellLabel(p.getName(), 130, "#333");
            Label sku = cellLabel(p.getSku() != null ? p.getSku() : "-", 90, "#555");
            Label type = cellLabel(p.getType() != null ? p.getType() : "-", 95, "#555");
            Label price = cellLabel(p.getPrice() != null ? p.getPrice() + " DT" : "-", 75, "#16a34a");
            Label stock = cellLabel(String.valueOf(p.getQuantity()), 55, p.getQuantity() > 0 ? "#333" : "#dc2626");
            Label expire = cellLabel(p.getExpiryDate() != null ? p.getExpiryDate().format(DF) : "-", 85, "#555");

            FontIcon activeIcon = new FontIcon(p.isActive() ? FontAwesomeSolid.CHECK_CIRCLE : FontAwesomeSolid.TIMES_CIRCLE);
            activeIcon.setIconSize(16);
            activeIcon.setIconColor(Color.web(p.isActive() ? "#16a34a" : "#dc2626"));
            Label active = new Label();
            active.setGraphic(activeIcon);
            active.setPrefWidth(50);

            HBox actions = new HBox(6);
            actions.setAlignment(Pos.CENTER);
            actions.setPrefWidth(130);

            Button btnView = actionBtn(FontAwesomeSolid.EYE, "#7c3aed", "#ede9fe");
            btnView.setTooltip(new Tooltip("Voir"));
            btnView.setOnAction(e -> showDetails(p));

            Button btnEdit = actionBtn(FontAwesomeSolid.EDIT, "#1a73e8", "#dbeafe");
            btnEdit.setTooltip(new Tooltip("Modifier"));
            btnEdit.setOnAction(e -> showForm(p));

            Button btnToggle = actionBtn(
                p.isActive() ? FontAwesomeSolid.BAN : FontAwesomeSolid.CHECK,
                p.isActive() ? "#f59e0b" : "#16a34a",
                p.isActive() ? "#fef3c7" : "#dcfce7"
            );
            btnToggle.setTooltip(new Tooltip(p.isActive() ? "Desactiver" : "Activer"));
            btnToggle.setOnAction(e -> {
                service.toggleActive(p.getId(), !p.isActive());
                showPopup(p.isActive() ? "Produit desactive" : "Produit active",
                          p.getName(),
                          p.isActive() ? FontAwesomeSolid.BAN : FontAwesomeSolid.CHECK_CIRCLE,
                          p.isActive() ? "#f59e0b" : "#16a34a");
            });

            Button btnDelete = actionBtn(FontAwesomeSolid.TRASH_ALT, "#dc2626", "#fee2e2");
            btnDelete.setTooltip(new Tooltip("Supprimer"));
            btnDelete.setOnAction(e -> showDeleteConfirm(p));

            actions.getChildren().addAll(btnView, btnEdit, btnToggle, btnDelete);

            row.getChildren().addAll(imgCell, name, sku, type, price, stock, expire, active, actions);
            container.getChildren().add(row);
        }
    }

    private void filterProduits(String search) {
        if (search == null || search.trim().isEmpty()) {
            addRows(service.getAll());
        } else {
            addRows(service.search(search));
        }
    }

    // ========== FORM ADD/EDIT ==========

    private void showForm(Produit existing) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        boolean isEdit = existing != null;

        Label titleLbl = new Label(isEdit ? "Modifier le produit" : "Nouveau produit");
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #7c3aed;");

        TextField nameField = styledField("Nom du produit");
        TextField skuField = styledField("SKU");
        TextField typeField = styledField("Type (ex: medicament, equipement)");
        TextField dosageField = styledField("Dosage (ex: 500mg)");
        TextField priceField = styledField("Prix (DT)");
        TextField qtyField = styledField("Quantite");
        DatePicker expiryPicker = new DatePicker();
        expiryPicker.setPromptText("Date d'expiration");
        expiryPicker.setPrefHeight(36);
        expiryPicker.setMaxWidth(Double.MAX_VALUE);
        expiryPicker.setStyle("-fx-background-radius: 8; -fx-font-size: 13px;");
        TextArea descArea = new TextArea();
        descArea.setPromptText("Description");
        descArea.setPrefRowCount(2);
        descArea.setStyle("-fx-background-radius: 8; -fx-font-size: 13px;");
        CheckBox activeBox = new CheckBox("Produit actif");
        activeBox.setSelected(true);
        activeBox.setStyle("-fx-font-size: 13px; -fx-text-fill: #555;");

        // Image picker (Cloudinary)
        ImageView preview = new ImageView();
        preview.setFitWidth(120);
        preview.setFitHeight(120);
        preview.setPreserveRatio(true);
        StackPane previewBox = new StackPane(preview);
        previewBox.setPrefSize(120, 120);
        previewBox.setStyle("-fx-background-color: #f3f4f6; -fx-background-radius: 10; -fx-border-color: #e5e7eb; -fx-border-radius: 10;");
        Label imageHint = new Label("Aucune image");
        imageHint.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");
        previewBox.getChildren().add(imageHint);

        Button btnPickImage = new Button("Choisir une image");
        FontIcon imgIcon = new FontIcon(FontAwesomeSolid.IMAGE);
        imgIcon.setIconSize(13);
        imgIcon.setIconColor(Color.WHITE);
        btnPickImage.setGraphic(imgIcon);
        btnPickImage.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-size: 12px; "
                + "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 14;");
        Label fileNameLbl = new Label("");
        fileNameLbl.setStyle("-fx-text-fill: #555; -fx-font-size: 11px;");

        final File[] pickedFile = new File[1];
        btnPickImage.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choisir une image produit");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));
            File f = fc.showOpenDialog(popup);
            if (f != null) {
                pickedFile[0] = f;
                fileNameLbl.setText(f.getName());
                try {
                    preview.setImage(new Image(f.toURI().toString(), 120, 120, true, true, true));
                    imageHint.setVisible(false);
                } catch (Exception ignored) {}
            }
        });

        if (isEdit) {
            nameField.setText(existing.getName());
            skuField.setText(existing.getSku());
            typeField.setText(existing.getType());
            dosageField.setText(existing.getDosage());
            priceField.setText(existing.getPrice() != null ? existing.getPrice().toString() : "");
            qtyField.setText(String.valueOf(existing.getQuantity()));
            if (existing.getExpiryDate() != null) expiryPicker.setValue(existing.getExpiryDate().toLocalDate());
            descArea.setText(existing.getDescription());
            activeBox.setSelected(existing.isActive());
            if (existing.getImageUrl() != null && !existing.getImageUrl().isBlank()) {
                try {
                    preview.setImage(new Image(existing.getImageUrl(), 120, 120, true, true, true));
                    imageHint.setVisible(false);
                    fileNameLbl.setText("(image actuelle)");
                } catch (Exception ignored) {}
            }
        }

        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");

        Button btnSave = new Button(isEdit ? "Enregistrer" : "Creer");
        btnSave.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-size: 13px; " +
                         "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 25;");

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #333; -fx-font-size: 13px; " +
                           "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 25;");
        btnCancel.setOnAction(e -> popup.close());

        btnSave.setOnAction(e -> {
            // Reset styles
            Validators.markValid(nameField);
            Validators.markValid(skuField);
            Validators.markValid(priceField);
            Validators.markValid(qtyField);
            errorLbl.setText("");

            // === Controle de saisie ===
            if (!Validators.isSafeName(nameField.getText())) {
                Validators.markInvalid(nameField);
                errorLbl.setText("Nom invalide (2-150 caracteres, lettres/chiffres).");
                return;
            }
            if (!Validators.isSku(skuField.getText())) {
                Validators.markInvalid(skuField);
                errorLbl.setText("SKU invalide (lettres, chiffres, '-' uniquement).");
                return;
            }
            if (!Validators.isPositiveDecimal(priceField.getText())) {
                Validators.markInvalid(priceField);
                errorLbl.setText("Prix invalide (nombre positif requis, ex: 12.50).");
                return;
            }
            if (!Validators.isNonNegativeInt(qtyField.getText())) {
                Validators.markInvalid(qtyField);
                errorLbl.setText("Quantite invalide (entier >= 0 requis).");
                return;
            }
            if (expiryPicker.getValue() != null && !Validators.isStrictlyFuture(expiryPicker.getValue())) {
                errorLbl.setText("La date d'expiration doit etre dans le futur.");
                return;
            }

            Produit p = isEdit ? existing : new Produit();
            p.setName(nameField.getText().trim());
            p.setSku(skuField.getText().trim());
            p.setType(typeField.getText().trim());
            p.setDosage(dosageField.getText().trim());
            p.setPrice(Validators.parseDecimal(priceField.getText()));
            p.setQuantity(Validators.parseInt(qtyField.getText()));
            p.setDescription(descArea.getText());
            p.setActive(activeBox.isSelected());
            p.setExpiryDate(expiryPicker.getValue() != null ? expiryPicker.getValue().atStartOfDay() : null);
            if (!isEdit) p.setCreatedAt(LocalDateTime.now());

            // Upload image to Cloudinary if a new file was picked
            if (pickedFile[0] != null) {
                btnSave.setDisable(true);
                btnSave.setText("Upload...");
                try {
                    // delete old image first when editing
                    if (isEdit && existing.getImagePublicId() != null) {
                        cloudinaryService.delete(existing.getImagePublicId());
                    }
                    CloudinaryService.UploadResult ur = cloudinaryService.upload(pickedFile[0]);
                    p.setImageUrl(ur.secureUrl);
                    p.setImagePublicId(ur.publicId);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    btnSave.setDisable(false);
                    btnSave.setText(isEdit ? "Enregistrer" : "Creer");
                    errorLbl.setText("Echec upload Cloudinary : " + ex.getMessage());
                    return;
                }
            }

            if (isEdit) service.update(p); else service.add(p);
            popup.close();
            showPopup(isEdit ? "Produit modifie" : "Produit cree",
                      p.getName(),
                      FontAwesomeSolid.CHECK_CIRCLE, "#16a34a");
        });

        HBox btns = new HBox(12, btnCancel, btnSave);
        btns.setAlignment(Pos.CENTER_RIGHT);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(labeled("Nom *", nameField), 0, 0);
        grid.add(labeled("SKU", skuField), 1, 0);
        grid.add(labeled("Type", typeField), 0, 1);
        grid.add(labeled("Dosage", dosageField), 1, 1);
        grid.add(labeled("Prix", priceField), 0, 2);
        grid.add(labeled("Quantite", qtyField), 1, 2);
        grid.add(labeled("Expire le", expiryPicker), 0, 3);
        grid.add(labeled("Statut", activeBox), 1, 3);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c1, c2);

        VBox imageRight = new VBox(8, btnPickImage, fileNameLbl);
        imageRight.setAlignment(Pos.CENTER_LEFT);
        HBox imageRow = new HBox(15, previewBox, imageRight);
        imageRow.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(15, titleLbl, labeled("Image produit", imageRow),
                grid, labeled("Description", descArea), errorLbl, btns);
        box.setPadding(new Insets(25));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 16; " +
                     "-fx-border-color: #e0e0e0; -fx-border-radius: 16; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 4);");
        box.setPrefWidth(620);

        popup.setScene(new Scene(box));
        popup.showAndWait();
        loadProduits();
    }

    // ========== DETAILS ==========

    private void showDetails(Produit p) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        FontIcon icon = new FontIcon(FontAwesomeSolid.BOX);
        icon.setIconSize(44);
        icon.setIconColor(Color.web("#7c3aed"));

        Label titleLbl = new Label(p.getName());
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #7c3aed;");

        Label details = new Label(
            "SKU :  " + (p.getSku() != null ? p.getSku() : "-") + "\n" +
            "Type :  " + (p.getType() != null ? p.getType() : "-") + "\n" +
            "Dosage :  " + (p.getDosage() != null ? p.getDosage() : "-") + "\n" +
            "Prix :  " + (p.getPrice() != null ? p.getPrice() + " DT" : "-") + "\n" +
            "Stock :  " + p.getQuantity() + "\n" +
            "Expire :  " + (p.getExpiryDate() != null ? p.getExpiryDate().format(DF) : "-") + "\n" +
            "Actif :  " + (p.isActive() ? "Oui" : "Non") + "\n" +
            "Description :  " + (p.getDescription() != null ? p.getDescription() : "-")
        );
        details.setWrapText(true);
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

        popup.setScene(new Scene(box, 420, 420));
        popup.show();
    }

    // ========== DELETE ==========

    private void showDeleteConfirm(Produit p) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        FontIcon warnIcon = new FontIcon(FontAwesomeSolid.EXCLAMATION_TRIANGLE);
        warnIcon.setIconSize(44);
        warnIcon.setIconColor(Color.web("#dc2626"));

        Label titleLbl = new Label("Supprimer ce produit ?");
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");

        Label msgLbl = new Label(p.getName() + "\nSKU: " + (p.getSku() != null ? p.getSku() : "-"));
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

        popup.setScene(new Scene(box, 400, 260));
        popup.show();

        btnNon.setOnAction(e -> popup.close());
        btnOui.setOnAction(e -> {
            popup.close();
            if (p.getImagePublicId() != null) cloudinaryService.delete(p.getImagePublicId());
            service.delete(p.getId());
            showPopup("Produit supprime", p.getName(),
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

        PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
        pause.setOnFinished(e -> {
            popup.close();
            loadProduits();
        });
        pause.play();
    }

    // ========== HELPERS ==========

    private Label colLabel(String text, double width) {
        Label l = new Label(text);
        l.setPrefWidth(width);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");
        return l;
    }

    private Label cellLabel(String text, double width, String color) {
        Label l = new Label(text);
        l.setPrefWidth(width);
        l.setStyle("-fx-font-size: 13px; -fx-text-fill: " + color + ";");
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

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefHeight(36);
        tf.setStyle("-fx-background-radius: 8; -fx-font-size: 13px;");
        return tf;
    }

    private VBox labeled(String label, javafx.scene.Node field) {
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #555;");
        VBox v = new VBox(4, l, field);
        return v;
    }
}
