package com.medicare.controllers;

import com.medicare.models.Collaboration;
import com.medicare.models.Partner;
import com.medicare.services.CollaborationService;
import com.medicare.services.PartnerService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class CollaborationFormController {

    @FXML
    private Label titleLabel;
    @FXML
    private TextField titreField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private ComboBox<Partner> partnerComboBox;
    @FXML
    private ComboBox<String> statutComboBox;
    @FXML
    private DatePicker dateDebutPicker;
    @FXML
    private DatePicker dateFinPicker;
    @FXML
    private Label imageNameLabel;

    private CollaborationService collaborationService = new CollaborationService();
    private PartnerService partnerService = new PartnerService();
    private Collaboration currentCollaboration;
    private File selectedImageFile;
    private Consumer<Boolean> onFormClose;

    @FXML
    private void initialize() {
        // Populate Partner ComboBox
        List<Partner> partners = partnerService.getAll();
        partnerComboBox.setItems(FXCollections.observableArrayList(partners));
        partnerComboBox.setConverter(new StringConverter<Partner>() {
            @Override
            public String toString(Partner partner) {
                return partner == null ? "" : partner.getName();
            }

            @Override
            public Partner fromString(String string) {
                return partnerComboBox.getItems().stream().filter(p ->
                        p.getName().equals(string)).findFirst().orElse(null);
            }
        });

        // Populate Statut ComboBox
        statutComboBox.setItems(FXCollections.observableArrayList("Actif", "Inactif", "En attente", "Terminé"));
    }

    public void setCollaboration(Collaboration collaboration) {
        this.currentCollaboration = collaboration;
        if (collaboration != null) {
            titleLabel.setText("Modifier la Collaboration");
            titreField.setText(collaboration.getTitre());
            descriptionArea.setText(collaboration.getDescription());
            dateDebutPicker.setValue(collaboration.getDateDebut());
            dateFinPicker.setValue(collaboration.getDateFin());
            statutComboBox.setValue(collaboration.getStatut());
            imageNameLabel.setText(collaboration.getImageName() != null ? collaboration.getImageName() : "Aucun fichier choisi");

            // Select the correct partner in the ComboBox
            partnerComboBox.getItems().stream()
                    .filter(p -> p.getId() == collaboration.getPartnerId())
                    .findFirst()
                    .ifPresent(partnerComboBox::setValue);
        } else {
            titleLabel.setText("Ajouter une Collaboration");
        }
    }

    public void setOnFormClose(Consumer<Boolean> onFormClose) {
        this.onFormClose = onFormClose;
    }

    @FXML
    private void onSelectImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        selectedImageFile = fileChooser.showOpenDialog(titleLabel.getScene().getWindow());
        if (selectedImageFile != null) {
            imageNameLabel.setText(selectedImageFile.getName());
        }
    }

    @FXML
    private void onSave() {
        // Basic Validation
        if (titreField.getText().isEmpty() || partnerComboBox.getValue() == null || dateDebutPicker.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur de validation", "Les champs Titre, Partenaire et Date de début sont obligatoires.");
            return;
        }

        boolean isNew = currentCollaboration == null;
        if (isNew) {
            currentCollaboration = new Collaboration();
        }

        currentCollaboration.setTitre(titreField.getText());
        currentCollaboration.setDescription(descriptionArea.getText());
        currentCollaboration.setPartnerId(partnerComboBox.getValue().getId());
        currentCollaboration.setStatut(statutComboBox.getValue());
        currentCollaboration.setDateDebut(dateDebutPicker.getValue());
        currentCollaboration.setDateFin(dateFinPicker.getValue());

        // Handle file upload
        if (selectedImageFile != null) {
            String newImageName = saveImage(selectedImageFile);
            currentCollaboration.setImageName(newImageName);
        }

        if (isNew) {
            collaborationService.add(currentCollaboration);
        } else {
            collaborationService.update(currentCollaboration);
        }

        if (onFormClose != null) {
            onFormClose.accept(true); // Signal that the data was updated
        }
        closeStage();
    }

    private String saveImage(File imageFile) {
        try {
            // Define your upload directory
            Path uploadDir = Paths.get("uploads/images");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // Create a unique file name
            String originalName = imageFile.getName();
            String fileExtension = originalName.substring(originalName.lastIndexOf("."));
            String uniqueName = UUID.randomUUID().toString() + fileExtension;

            Path destination = uploadDir.resolve(uniqueName);
            Files.copy(imageFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

            return uniqueName;
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur Fichier", "Impossible d'enregistrer l'image.");
            return null;
        }
    }

    @FXML
    private void onCancel() {
        if (onFormClose != null) {
            onFormClose.accept(false); // Signal that no changes were made
        }
        closeStage();
    }

    private void closeStage() {
        ((Stage) titleLabel.getScene().getWindow()).close();
    }



    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}