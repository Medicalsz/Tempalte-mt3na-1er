package com.medicare.controllers;

import com.medicare.models.Partner;
import com.medicare.services.PartnerService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javafx.collections.FXCollections;
import javafx.scene.control.Alert;

public class PartnerFormController {

    @FXML private Label formTitle;
    @FXML private ImageView partnerImageView;
    @FXML private TextField nomField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private ComboBox<String> statutComboBox;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField adresseField;
    @FXML private DatePicker datePicker;
    @FXML private Button saveBtn;

    private Partner currentPartner;
    private PartnerService partnerService = new PartnerService();
    private Consumer<Partner> saveHandler;
    private File selectedImageFile;
    private Runnable onSaveCallback;


    @FXML
    public void initialize() {
        typeComboBox.setItems(FXCollections.observableArrayList("Fournisseur", "Sponsor", "Organisme de santé"));
        statutComboBox.setItems(FXCollections.observableArrayList("Actif", "Inactif", "En attente"));
    }

    public void setPartner(Partner partner) {
        this.currentPartner = partner;
        if (partner != null) {
            formTitle.setText("Modifier le Partenaire : " + partner.getName());
            nomField.setText(partner.getName());
            typeComboBox.setValue(partner.getTypePartenaire());
            statutComboBox.setValue(partner.getStatut());
            emailField.setText(partner.getEmail());
            phoneField.setText(partner.getTelephone());
            adresseField.setText(partner.getAdresse());
            datePicker.setValue(partner.getDatePartenariat());

            if (partner.getImageName() != null && !partner.getImageName().isEmpty()) {
                try {
                    Path imagePath = Paths.get("src/main/resources/uploads/partners", partner.getImageName());
                    if (Files.exists(imagePath)) {
                        Image image = new Image(imagePath.toUri().toString());
                        partnerImageView.setImage(image);
                    }
                } catch (Exception e) {
                    System.err.println("Could not load image: " + e.getMessage());
                }
            }

        } else {
            formTitle.setText("Ajouter un Partenaire");
            this.currentPartner = new Partner();
        }
    }

    public void setOnSave(Runnable callback) {
        this.onSaveCallback = callback;
    }

    @FXML
    private void handleChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        selectedImageFile = fileChooser.showOpenDialog(formTitle.getScene().getWindow());
        if (selectedImageFile != null) {
            try {
                Image image = new Image(selectedImageFile.toURI().toString());
                partnerImageView.setImage(image);
            } catch (Exception e) {
                System.err.println("Error loading selected image: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleSave() {
        if (!validateInputs()) {
            return; // Stop if validation fails
        }

        currentPartner.setName(nomField.getText());
        currentPartner.setTypePartenaire(typeComboBox.getValue());
        currentPartner.setStatut(statutComboBox.getValue());
        currentPartner.setEmail(emailField.getText());
        currentPartner.setTelephone(phoneField.getText());
        currentPartner.setAdresse(adresseField.getText());
        currentPartner.setDatePartenariat(datePicker.getValue());

        if (selectedImageFile != null) {
            try {
                Path uploadDir = Paths.get("src/main/resources/uploads/partners");
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }
                String fileName = System.currentTimeMillis() + "_" + selectedImageFile.getName();
                Path dest = uploadDir.resolve(fileName);
                Files.copy(selectedImageFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                currentPartner.setImageName(fileName);
            } catch (IOException e) {
                e.printStackTrace();
                // Handle error (e.g., show an alert)
                return;
            }
        }

        if (currentPartner.getId() == 0) { // Assuming 0 is the default for a new partner
            partnerService.add(currentPartner);
        } else {
            partnerService.update(currentPartner);
        }

        if (onSaveCallback != null) {
            onSaveCallback.run();
        }

        closeStage();
    }

    @FXML
    private void handleCancel() {
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) saveBtn.getScene().getWindow();
        stage.close();
    }

    private boolean validateInputs() {
        resetValidationStyles();
        List<String> errors = new ArrayList<>();

        if (nomField.getText().isBlank()) {
            errors.add("Le champ 'Nom' est obligatoire.");
            nomField.getStyleClass().add("validation-error");
        }
        if (typeComboBox.getValue() == null) {
            errors.add("Le champ 'Type' est obligatoire.");
            typeComboBox.getStyleClass().add("validation-error");
        }

        // Email validation with regex
        String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,6}$";
        if (emailField.getText().isBlank()) {
            errors.add("Le champ 'Email' est obligatoire.");
            emailField.getStyleClass().add("validation-error");
        } else if (!emailField.getText().matches(emailRegex)) {
            errors.add("Le format de l\'Email est invalide.");
            emailField.getStyleClass().add("validation-error");
        }

        // Phone validation for digits only
        if (phoneField.getText().isBlank()) {
            errors.add("Le champ 'Téléphone' est obligatoire.");
            phoneField.getStyleClass().add("validation-error");
        } else if (!phoneField.getText().matches("\\d+")) {
            errors.add("Le 'Téléphone' ne doit contenir que des chiffres.");
            phoneField.getStyleClass().add("validation-error");
        }

        if (adresseField.getText().isBlank()) {
            errors.add("Le champ 'Adresse' est obligatoire.");
            adresseField.getStyleClass().add("validation-error");
        }
        if (statutComboBox.getValue() == null) {
            errors.add("Le champ 'Statut' est obligatoire.");
            statutComboBox.getStyleClass().add("validation-error");
        }
        if (datePicker.getValue() == null) {
            errors.add("Le champ 'Date Partenariat' est obligatoire.");
            datePicker.getStyleClass().add("validation-error");
        } else if (datePicker.getValue().isAfter(LocalDate.now())) {
            errors.add("La 'Date Partenariat' ne peut pas être dans le futur.");
            datePicker.getStyleClass().add("validation-error");
        }

        if (errors.isEmpty()) {
            return true;
        } else {
            String errorHeader = "Veuillez corriger les erreurs suivantes :";
            String errorContent = String.join("\n- ", errors);
            showAlert(Alert.AlertType.ERROR, "Erreur de Validation", errorHeader, "- " + errorContent);
            return false;
        }
    }

    private void resetValidationStyles() {
        Stream.of(nomField, typeComboBox, emailField, phoneField, adresseField, statutComboBox, datePicker)
              .forEach(node -> node.getStyleClass().remove("validation-error"));
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}