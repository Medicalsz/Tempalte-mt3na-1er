package com.medicare.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import com.medicare.models.Donation;
import com.medicare.models.User;
import com.medicare.services.DonationService;
import com.medicare.services.EmailService;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class UserDonationFormController {

    @FXML private Label causeNameLabel;
    @FXML private VBox btnTypeMaterial;
    @FXML private VBox btnTypeMoney;
    @FXML private FontIcon iconMaterial;
    @FXML private Label labelMaterial;
    @FXML private FontIcon iconMoney;
    @FXML private Label labelMoney;
    @FXML private VBox formMoney;
    @FXML private VBox formMaterial;
    @FXML private ComboBox<Integer> objectsCountCombo;
    @FXML private VBox objectsContainer;
    @FXML private TextField cardNumberField;
    @FXML private TextField amountField;
    @FXML private TextArea descArea;
    @FXML private HBox cardMastercard;
    @FXML private HBox cardVisa;

    private Donation selectedCause;
    private final DonationService donationService = new DonationService();
    private boolean isMoneyMode = true;
    private String selectedCard = "Mastercard";
    private Integer editDonId = null;
    private final java.util.Map<Integer, String> itemPhotos = new java.util.HashMap<>();

    @FXML
    public void initialize() {
        objectsCountCombo.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        objectsCountCombo.setValue(1);
        objectsCountCombo.setOnAction(e -> updateObjectsFields());
        updateObjectsFields();

        // Contrôle de saisie en temps réel pour le numéro de carte
        cardNumberField.textProperty().addListener((observable, oldValue, newValue) -> {
            // Ne garder que les chiffres
            String digits = newValue.replaceAll("[^\\d]", "");
            
            // Limiter à 16 chiffres
            if (digits.length() > 16) {
                digits = digits.substring(0, 16);
            }
            
            // Ajouter des espaces tous les 4 chiffres pour la lisibilité
            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i > 0 && i % 4 == 0) {
                    formatted.append(" ");
                }
                formatted.append(digits.charAt(i));
            }
            
            if (!newValue.equals(formatted.toString())) {
                cardNumberField.setText(formatted.toString());
            }
        });

        // Contrôle de saisie pour le montant (numérique uniquement, accepte point et virgule)
        amountField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty() && !newValue.matches("\\d*([.,]\\d*)?")) {
                amountField.setText(oldValue);
            }
        });
    }

    public void setCause(Donation cause) {
        this.selectedCause = cause;
        causeNameLabel.setText(cause.getNom());
    }

    public void setEditMode(int donId) {
        this.editDonId = donId;
        this.selectedCause = donationService.getCauseByDonId(donId);
        if (selectedCause != null) {
            causeNameLabel.setText(selectedCause.getNom() + " (Modification)");
        }
        
        List<DonationService.MaterialItem> items = donationService.getMaterialItemsForDon(donId);
        descArea.setText(donationService.getDonDescription(donId));
        
        onSelectMaterial(); // Les modifications ne concernent que le matériel ici
        
        objectsCountCombo.setValue(items.size());
        updateObjectsFields();
        
        // Remplir les champs avec les données existantes
        for (int i = 0; i < items.size(); i++) {
            VBox box = (VBox) objectsContainer.getChildren().get(i);
            HBox fields = (HBox) box.getChildren().get(1);
            VBox nameBox = (VBox) fields.getChildren().get(0);
            TextField nameField = (TextField) nameBox.getChildren().get(1);
            VBox quantBox = (VBox) fields.getChildren().get(1);
            TextField quantField = (TextField) quantBox.getChildren().get(1);
            
            nameField.setText(items.get(i).getName());
            quantField.setText(String.valueOf(items.get(i).getQuantity()));
            
            String photo = items.get(i).getPhoto();
            if (photo != null && !photo.isEmpty()) {
                itemPhotos.put(i + 1, photo);
                updatePhotoLabel(box, photo);
            }
        }
    }

    @FXML
    private void onSelectMastercard() {
        selectedCard = "Mastercard";
        cardMastercard.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #10b981; -fx-border-radius: 8; -fx-padding: 10; -fx-cursor: hand;");
        cardVisa.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-padding: 10; -fx-cursor: hand;");
    }

    @FXML
    private void onSelectVisa() {
        selectedCard = "Visa";
        cardVisa.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #10b981; -fx-border-radius: 8; -fx-padding: 10; -fx-cursor: hand;");
        cardMastercard.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-padding: 10; -fx-cursor: hand;");
    }

    @FXML
    private void onSelectMaterial() {
        isMoneyMode = false;
        btnTypeMaterial.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #10b981; -fx-border-radius: 12; -fx-cursor: hand;");
        iconMaterial.setIconColor(Color.web("#10b981"));
        labelMaterial.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");

        btnTypeMoney.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-cursor: hand;");
        iconMoney.setIconColor(Color.web("#64748b"));
        labelMoney.setStyle("-fx-text-fill: #64748b; -fx-font-weight: bold;");

        formMoney.setVisible(false);
        formMoney.setManaged(false);
        formMaterial.setVisible(true);
        formMaterial.setManaged(true);
    }

    @FXML
    private void onSelectMoney() {
        isMoneyMode = true;
        btnTypeMoney.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #10b981; -fx-border-radius: 12; -fx-cursor: hand;");
        iconMoney.setIconColor(Color.web("#10b981"));
        labelMoney.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");

        btnTypeMaterial.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-cursor: hand;");
        iconMaterial.setIconColor(Color.web("#64748b"));
        labelMaterial.setStyle("-fx-text-fill: #64748b; -fx-font-weight: bold;");

        formMaterial.setVisible(false);
        formMaterial.setManaged(false);
        formMoney.setVisible(true);
        formMoney.setManaged(true);
    }

    private void updateObjectsFields() {
        // Sauvegarder les données actuelles avant de vider le conteneur
        List<DonationService.MaterialItem> currentItems = new ArrayList<>();
        for (int i = 0; i < objectsContainer.getChildren().size(); i++) {
            Node node = objectsContainer.getChildren().get(i);
            if (node instanceof VBox box) {
                try {
                    HBox fields = (HBox) box.getChildren().get(1);
                    VBox nameBox = (VBox) fields.getChildren().get(0);
                    TextField nameField = (TextField) nameBox.getChildren().get(1);
                    VBox quantBox = (VBox) fields.getChildren().get(1);
                    TextField quantField = (TextField) quantBox.getChildren().get(1);
                    
                    String photoPath = itemPhotos.get(i + 1);
                    currentItems.add(new DonationService.MaterialItem(nameField.getText(), 
                        quantField.getText().isEmpty() ? 1 : Integer.parseInt(quantField.getText()),
                        photoPath));
                } catch (Exception ignored) {}
            }
        }

        objectsContainer.getChildren().clear();
        int count = objectsCountCombo.getValue();
        for (int i = 1; i <= count; i++) {
            VBox entry = createObjectEntry(i);
            objectsContainer.getChildren().add(entry);
            
            // Restaurer les données si elles existaient
            if (i <= currentItems.size()) {
                HBox fields = (HBox) entry.getChildren().get(1);
                VBox nameBox = (VBox) fields.getChildren().get(0);
                TextField nameField = (TextField) nameBox.getChildren().get(1);
                VBox quantBox = (VBox) fields.getChildren().get(1);
                TextField quantField = (TextField) quantBox.getChildren().get(1);
                
                nameField.setText(currentItems.get(i-1).getName());
                quantField.setText(String.valueOf(currentItems.get(i-1).getQuantity()));
                
                String photo = currentItems.get(i-1).getPhoto();
                if (photo != null) {
                    itemPhotos.put(i, photo);
                    updatePhotoLabel(entry, photo);
                }
            }
        }
    }

    private void updatePhotoLabel(VBox entry, String photoPath) {
        HBox fields = (HBox) entry.getChildren().get(1);
        VBox photoBox = (VBox) fields.getChildren().get(2);
        Label statusLabel = (Label) photoBox.getChildren().get(2);
        if (photoPath != null && !photoPath.isEmpty()) {
            java.io.File file = new java.io.File(photoPath);
            statusLabel.setText("Photo: " + file.getName());
            statusLabel.setTextFill(Color.web("#10b981"));
        } else {
            statusLabel.setText("Aucune photo");
            statusLabel.setTextFill(Color.web("#64748b"));
        }
    }

    private VBox createObjectEntry(int index) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10; -fx-border-color: #e2e8f0; -fx-border-radius: 10;");

        HBox header = new HBox(5);
        header.setAlignment(Pos.CENTER_LEFT);
        
        FontIcon icon = new FontIcon(FontAwesomeSolid.BOX);
        icon.setIconColor(Color.web("#10b981"));
        
        Label title = new Label("Objet n°" + index);
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #10b981;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Button btnDelete = new Button();
        btnDelete.setGraphic(new FontIcon(FontAwesomeSolid.TRASH) {{ setIconColor(Color.web("#ef4444")); setIconSize(14); }});
        btnDelete.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> {
            if (objectsCountCombo.getValue() > 1) {
                itemPhotos.remove(index);
                objectsCountCombo.setValue(objectsCountCombo.getValue() - 1);
            }
        });

        header.getChildren().addAll(icon, title, spacer, btnDelete);

        HBox fields = new HBox(15);
        fields.setAlignment(Pos.BOTTOM_LEFT);

        VBox nameBox = new VBox(5);
        Label nameLabel = new Label("Nom de l'objet");
        TextField nameField = new TextField();
        nameField.setPromptText("Ex: Fauteuil roulant, Médicaments...");
        nameField.setPrefWidth(250);
        nameBox.getChildren().addAll(nameLabel, nameField);

        VBox quantBox = new VBox(5);
        Label quantLabel = new Label("Quantité");
        TextField quantField = new TextField("1");
        quantField.setPrefWidth(60);
        quantBox.getChildren().addAll(quantLabel, quantField);

        VBox photoBox = new VBox(5);
        Label photoLabel = new Label("Photo");
        Button uploadBtn = new Button("Upload");
        uploadBtn.setGraphic(new FontIcon(FontAwesomeSolid.CAMERA));
        uploadBtn.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 5; -fx-cursor: hand;");
        
        Label statusLabel = new Label("Aucune photo");
        statusLabel.setStyle("-fx-font-size: 10px;");
        statusLabel.setTextFill(Color.web("#64748b"));

        uploadBtn.setOnAction(e -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Choisir une photo de l'objet");
            fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );
            java.io.File selectedFile = fileChooser.showOpenDialog(box.getScene().getWindow());
            if (selectedFile != null) {
                try {
                    // Créer le dossier uploads s'il n'existe pas
                    java.io.File uploadDir = new java.io.File("uploads/donations");
                    if (!uploadDir.exists()) uploadDir.mkdirs();

                    // Copier le fichier localement avec un nom unique
                    String extension = selectedFile.getName().substring(selectedFile.getName().lastIndexOf("."));
                    String fileName = "don_" + System.currentTimeMillis() + "_" + index + extension;
                    java.io.File destFile = new java.io.File(uploadDir, fileName);
                    
                    java.nio.file.Files.copy(selectedFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    
                    String savedPath = destFile.getAbsolutePath();
                    itemPhotos.put(index, savedPath);
                    statusLabel.setText("Photo: " + selectedFile.getName());
                    statusLabel.setTextFill(Color.web("#10b981"));
                    
                    System.out.println("Photo sauvegardée : " + savedPath);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur d'upload", "Impossible de sauvegarder la photo.");
                }
            }
        });

        photoBox.getChildren().addAll(photoLabel, uploadBtn, statusLabel);

        fields.getChildren().addAll(nameBox, quantBox, photoBox);
        box.getChildren().addAll(header, fields);
        return box;
    }

    @FXML
    private void onConfirm() {
        System.out.println("Bouton Confirmer cliqué.");
        try {
            User currentUser = DashboardPatientController.getCurrentUser();
            if (currentUser == null) currentUser = DashboardAdminController.getCurrentUser();
            if (currentUser == null) currentUser = DashboardMedecinController.getCurrentUser();

            if (currentUser == null) {
                System.err.println("Aucun utilisateur connecté trouvé.");
                showAlert(Alert.AlertType.ERROR, "Erreur", "Utilisateur non connecté", "Veuillez vous reconnecter pour effectuer un don.");
                return;
            }

            System.out.println("Utilisateur détecté : " + currentUser.getEmail());

            if (isMoneyMode) {
                System.out.println("Mode Argent détecté.");
                String cardNumber = cardNumberField.getText().replaceAll("\\s", "");
                String amountRaw = amountField.getText().trim();
                String amountStr = amountRaw.replace(",", "."); // Gérer les virgules

                System.out.println("Numéro carte : " + cardNumber);
                System.out.println("Montant brut : " + amountRaw + " -> Traité : " + amountStr);

                if (cardNumber.isEmpty() || cardNumber.length() != 16) {
                    showAlert(Alert.AlertType.WARNING, "Données de carte invalides", null, "Veuillez saisir un numéro de carte à 16 chiffres.");
                    return;
                }

                if (amountStr.isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Montant manquant", null, "Veuillez saisir un montant.");
                    return;
                }

                double amount;
                try {
                    amount = Double.parseDouble(amountStr);
                } catch (NumberFormatException e) {
                    System.err.println("Erreur format montant : " + amountStr);
                    showAlert(Alert.AlertType.ERROR, "Format invalide", null, "Le montant doit être un nombre valide (ex: 500.50).");
                    return;
                }

                if (amount <= 0) {
                    showAlert(Alert.AlertType.WARNING, "Montant invalide", null, "Le montant doit être supérieur à 0.");
                    return;
                }

                // Vérification par email si montant > 500 DT
                if (amount > 500) {
                    System.out.println("Montant " + amount + " > 500 DT. Début de la procédure de vérification.");
                    String userEmail = currentUser.getEmail();
                    
                    if (userEmail == null || userEmail.isEmpty()) {
                        System.err.println("Email de l'utilisateur introuvable.");
                        showAlert(Alert.AlertType.ERROR, "Erreur de sécurité", "Email manquant", "Votre adresse email est requise pour valider ce don.");
                        return;
                    }

                    String verificationCode = String.format("%04d", new Random().nextInt(10000));
                    System.out.println("Code généré : " + verificationCode + ". Envoi à : " + userEmail);
                    
                    EmailService emailService = new EmailService();
                    new Thread(() -> {
                        try {
                            System.out.println("Tentative d'envoi de l'email...");
                            emailService.sendVerificationCode(userEmail, verificationCode);
                            System.out.println("Email envoyé.");
                        } catch (Exception e) {
                            System.err.println("Échec envoi email (Thread) : " + e.getMessage());
                            e.printStackTrace();
                        }
                    }).start();

                    System.out.println("Affichage du dialogue de vérification...");
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setTitle("Vérification de sécurité");
                    dialog.setHeaderText("Confirmation par email requise");
                    dialog.setContentText("Un code a été envoyé à : " + userEmail + "\nSaisissez le code à 4 chiffres :");
                    
                    // S'assurer que le dialogue est au premier plan
                    if (cardNumberField.getScene() != null && cardNumberField.getScene().getWindow() != null) {
                        dialog.initOwner(cardNumberField.getScene().getWindow());
                    }

                    Optional<String> result = dialog.showAndWait();
                    if (result.isPresent()) {
                        String codeSaisi = result.get().trim();
                        System.out.println("Code saisi : " + codeSaisi);
                        if (!codeSaisi.equals(verificationCode)) {
                            System.out.println("Code incorrect !");
                            showAlert(Alert.AlertType.ERROR, "Échec", "Code incorrect", "Le code saisi ne correspond pas.");
                            return;
                        }
                        System.out.println("Code vérifié avec succès.");
                    } else {
                        System.out.println("Vérification annulée par l'utilisateur.");
                        return;
                    }
                }

                System.out.println("Appel au service pour enregistrer le don d'argent...");
                if (donationService.addMoneyDon(currentUser.getId(), selectedCause.getId(), amount, descArea.getText(), selectedCard)) {
                    System.out.println("Don d'argent enregistré avec succès.");
                    showSuccessAndBack();
                } else {
                    String error = donationService.getLastError();
                    System.err.println("Échec service addMoneyDon : " + error);
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Échec du don", error);
                }
            } else {
                // Mode Matériel
                System.out.println("Mode Matériel détecté.");
                List<DonationService.MaterialItem> items = new ArrayList<>();
                int idx = 1;
                for (Node node : objectsContainer.getChildren()) {
                    if (node instanceof VBox box) {
                        HBox fields = (HBox) box.getChildren().get(1);
                        VBox nameBox = (VBox) fields.getChildren().get(0);
                        TextField nameField = (TextField) nameBox.getChildren().get(1);
                        VBox quantBox = (VBox) fields.getChildren().get(1);
                        TextField quantField = (TextField) quantBox.getChildren().get(1);
                        
                        String itemName = nameField.getText().trim();
                        if (!itemName.isEmpty()) {
                            int qty = 1;
                            try { 
                                qty = Integer.parseInt(quantField.getText().trim()); 
                                if (qty <= 0) qty = 1;
                            } catch (NumberFormatException ignored) {}
                            
                            String photo = itemPhotos.get(idx);
                            items.add(new DonationService.MaterialItem(itemName, qty, photo));
                        }
                    }
                    idx++;
                }

                if (!items.isEmpty()) {
                    System.out.println("Enregistrement de " + items.size() + " objets...");
                    boolean success;
                    if (editDonId != null) {
                        success = donationService.updateMaterialDon(editDonId, items, descArea.getText());
                    } else {
                        success = donationService.addMaterialDon(currentUser.getId(), selectedCause.getId(), items, descArea.getText());
                    }

                    if (success) {
                        System.out.println("Don matériel enregistré.");
                        showSuccessAndBack();
                    } else {
                        String error = donationService.getLastError();
                        System.err.println("Échec service don matériel : " + error);
                        showAlert(Alert.AlertType.ERROR, "Échec", "Erreur lors du don", 
                            "Une erreur est survenue lors de l'enregistrement de votre don matériel.\n\n" +
                            (error != null ? "Détails : " + error : "Veuillez réessayer."));
                    }
                } else {
                    System.out.println("Don matériel vide !");
                    showAlert(Alert.AlertType.WARNING, "Don vide", null, "Veuillez saisir au moins un objet à donner.");
                }
            }
        } catch (Throwable t) {
            System.err.println("ERREUR CRITIQUE dans onConfirm : " + t.getMessage());
            t.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur système", "Une erreur inattendue est survenue", 
                "Détail : " + t.toString() + "\nConsultez la console pour plus d'infos.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        
        // S'assurer que l'alerte est au premier plan
        if (cardNumberField != null && cardNumberField.getScene() != null && cardNumberField.getScene().getWindow() != null) {
            alert.initOwner(cardNumberField.getScene().getWindow());
        }
        
        alert.showAndWait();
    }

    private void showSuccessAndBack() {
        showAlert(Alert.AlertType.INFORMATION, "Opération réussie", null, 
            editDonId != null ? "Votre don a été mis à jour avec succès." : "Merci pour votre générosité ! Votre don a été enregistré.");
        
        if (editDonId != null) {
            onBackToMyDons();
        } else {
            onCancel();
        }
    }

    private void onBackToMyDons() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/user-my-dons-view.fxml"));
            Node view = loader.load();
            StackPane contentArea = (StackPane) causeNameLabel.getScene().lookup("#contentArea");
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onCancel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/user-cause-detail-view.fxml"));
            Node view = loader.load();
            UserCauseDetailController controller = loader.getController();
            controller.setCause(selectedCause);
            
            StackPane contentArea = (StackPane) causeNameLabel.getScene().lookup("#contentArea");
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
