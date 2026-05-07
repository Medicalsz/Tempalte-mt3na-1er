package com.medicare.controllers;

import java.io.File;
import java.util.List;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import com.medicare.models.Donation;
import com.medicare.services.DonationService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class DonationController {

    @FXML private FlowPane donationContainer;

    private final DonationService donationService = new DonationService();

    @FXML
    public void initialize() {
        loadDonations();
    }

    @FXML
    private void onMapViewClick() {
        // Cette méthode semble être l'ancienne version, on garde onMapDonsClick pour la nouvelle
    }

    @FXML
    private void onMapDonsClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/admin-donation-map-view.fxml"));
            StackPane contentArea = (StackPane) donationContainer.getScene().lookup("#contentArea");
            contentArea.getChildren().clear();
            contentArea.getChildren().add(loader.load());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onViewDonsClick() {
        try {
            System.out.println("DonationController: Tentative de chargement de admin-don-list-view.fxml...");
                
                // Utilisation d'un chemin plus sûr via HelloApplication
                FXMLLoader loader = new FXMLLoader(com.medicare.HelloApplication.class.getResource("admin-don-list-view.fxml"));
                Node view = loader.load();
                
                if (view == null) {
                    throw new Exception("Le chargement du FXML a retourné une vue nulle.");
                }
            
            Scene scene = donationContainer.getScene();
            if (scene == null) {
                System.err.println("DonationController: Scène nulle !");
                return;
            }
            
            StackPane contentArea = null;
            if (scene != null) {
                contentArea = (StackPane) scene.lookup("#contentArea");
            }
            
            if (contentArea == null) {
                System.err.println("DonationController: lookup(#contentArea) a échoué, recherche manuelle...");
                contentArea = findContentArea(donationContainer.getScene().getRoot());
            }
            
            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(view);
                
                // Forcer le redimensionnement si nécessaire
                if (view instanceof javafx.scene.layout.Region) {
                    ((javafx.scene.layout.Region) view).prefWidthProperty().bind(contentArea.widthProperty());
                    ((javafx.scene.layout.Region) view).prefHeightProperty().bind(contentArea.heightProperty());
                }
                
                System.out.println("DonationController: Vue des dons injectée avec succès.");
            } else {
                // Si on ne trouve toujours pas le contentArea, on essaie de remplacer le contenu du parent le plus proche
                System.err.println("DonationController: contentArea introuvable, tentative de remplacement direct...");
                if (donationContainer.getParent() instanceof javafx.scene.layout.Pane) {
                    ((javafx.scene.layout.Pane)donationContainer.getParent()).getChildren().setAll(view);
                } else {
                    throw new Exception("Impossible de trouver un conteneur pour afficher les dons.");
                }
            }
        } catch (Exception e) {
            System.err.println("DonationController: ERREUR lors du chargement :");
            e.printStackTrace();
            
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            
            // Feedback visuel plus détaillé
            Label errorLabel = new Label("Erreur: " + cause.toString());
            errorLabel.setWrapText(true);
            errorLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-padding: 10; -fx-background-color: #fee2e2; -fx-background-radius: 5;");
            donationContainer.getChildren().add(0, errorLabel);
        }
    }

    private StackPane findContentArea(Node node) {
        if (node instanceof StackPane && "contentArea".equals(node.getId())) {
            return (StackPane) node;
        }
        if (node instanceof javafx.scene.Parent) {
            for (Node child : ((javafx.scene.Parent) node).getChildrenUnmodifiable()) {
                StackPane found = findContentArea(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    @FXML
    private void onAddCauseClick() {
        showAddCauseDialog();
    }

    private void loadDonations() {
        donationContainer.getChildren().clear();
        List<Donation> donations = donationService.getAllDonations();

        if (donations.isEmpty()) {
            Label noData = new Label("Aucune cause de donation disponible.");
            noData.setStyle("-fx-font-size: 16px; -fx-text-fill: #666; -fx-padding: 20;");
            donationContainer.getChildren().add(noData);
            return;
        }

        for (Donation donation : donations) {
            donationContainer.getChildren().add(createDonationBox(donation));
        }
    }

    private VBox createDonationBox(Donation donation) {
        VBox box = new VBox(10);
        box.setPrefWidth(280);
        box.setPadding(new Insets(15));
        box.setAlignment(Pos.TOP_LEFT);
        box.setStyle("-fx-background-color: white; " +
                     "-fx-background-radius: 12; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 4); " +
                     "-fx-border-color: #e2e8f0; " +
                     "-fx-border-radius: 12; " +
                     "-fx-border-width: 1;");

        // Image de la cause
        ImageView imageView = new ImageView();
        imageView.setFitWidth(250);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(true);
        VBox imageContainer = new VBox(imageView);
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8;");
        imageContainer.setMinHeight(150);
        
        try {
            if (donation.getImage() != null && !donation.getImage().isEmpty()) {
                String imagePath = donation.getImage();
                Image img;
                if (imagePath.startsWith("http") || imagePath.startsWith("file:")) {
                    img = new Image(imagePath, true);
                } else {
                    // Si c'est un chemin local absolu sans le préfixe file:
                    File file = new File(imagePath);
                    if (file.exists()) {
                        img = new Image(file.toURI().toString(), true);
                    } else {
                        // Tentative via ressources
                        img = new Image(getClass().getResourceAsStream("/com/medicare/images/logo.png"));
                    }
                }
                imageView.setImage(img);
            } else {
                imageView.setImage(new Image(getClass().getResourceAsStream("/com/medicare/images/logo.png")));
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement image : " + e.getMessage());
            try {
                imageView.setImage(new Image(getClass().getResourceAsStream("/com/medicare/images/logo.png")));
            } catch(Exception ignored) {}
        }

        Label causeLabel = new Label(donation.getCause());
        causeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #7c3aed; -fx-background-color: #f5f3ff; -fx-padding: 4 10; -fx-background-radius: 20;");

        Label nomLabel = new Label(donation.getNom());
        nomLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        nomLabel.setWrapText(true);

        Label descLabel = new Label(donation.getDescription());
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        descLabel.setWrapText(true);
        descLabel.setMinHeight(60);

        Label objLabel = new Label("Objectif : " + String.format("%.2f", donation.getObjectifMontant()) + " DT");
        objLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0d9488;");

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button btnDelete = new Button("Supprimer la cause");
        btnDelete.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnDelete, javafx.scene.layout.Priority.ALWAYS);
        btnDelete.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-padding: 8; -fx-background-radius: 8; -fx-cursor: hand;");
        btnDelete.setGraphic(new FontIcon(FontAwesomeSolid.TRASH_ALT) {{ setIconColor(Color.web("#dc2626")); }});
        btnDelete.setOnAction(e -> {
            if (donationService.deleteCause(donation.getId())) {
                loadDonations();
            }
        });

        buttonBox.getChildren().add(btnDelete);

        box.getChildren().addAll(imageContainer, causeLabel, nomLabel, descLabel, objLabel, buttonBox);
        return box;
    }

    private void showAddCauseDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("Ajouter une nouvelle cause");

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;");

        TextField nomField = new TextField();
        nomField.setPromptText("Nom de la cause");
        
        TextArea descField = new TextArea();
        descField.setPromptText("Description");
        descField.setPrefRowCount(3);

        TextField objField = new TextField();
        objField.setPromptText("Objectif de montant (ex: 5000)");

        TextField urlField = new TextField();
        urlField.setPromptText("URL de l'image (ex: https://...)");

        Button btnSave = new Button("Enregistrer");
        btnSave.setMaxWidth(Double.MAX_VALUE);
        btnSave.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10; -fx-background-radius: 8;");
        btnSave.setOnAction(e -> {
            try {
                String nom = nomField.getText();
                String desc = descField.getText();
                double obj = Double.parseDouble(objField.getText());
                String imgUrl = urlField.getText().trim();

                Donation d = new Donation(0, nom, desc, "Donation", imgUrl, obj);
                if (donationService.createCause(d)) {
                    dialog.close();
                    loadDonations();
                }
            } catch (Exception ex) {
                System.err.println("Erreur saisie : " + ex.getMessage());
            }
        });

        root.getChildren().addAll(
            new Label("Nom :"), nomField,
            new Label("Description :"), descField,
            new Label("Objectif (DT) :"), objField,
            new Label("URL Image :"), urlField,
            btnSave
        );

        dialog.setScene(new Scene(root, 400, 500));
        dialog.show();
    }
}
