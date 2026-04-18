package com.medicare.controllers;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import com.medicare.controllers.CollaborationFormController;
import com.medicare.models.Collaboration;
import com.medicare.services.CollaborationService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AdminCollaborationsController {

    @FXML
    private VBox container;

    private final CollaborationService collaborationService = new CollaborationService();

    @FXML
    private void initialize() {
        loadCollaborations();
    }

    private void loadCollaborations() {
        container.getChildren().clear();

        // Header
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Gestion des Collaborations");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #7c3aed;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("Ajouter Collaboration");
        addBtn.setGraphic(new FontIcon(FontAwesomeSolid.PLUS));
        addBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-size: 13px; -fx-background-radius: 8; -fx-cursor: hand;");
        addBtn.setOnAction(e -> showCollaborationForm(null));

        header.getChildren().addAll(title, spacer, addBtn);
        container.getChildren().add(header);

        // Table header
        HBox tableHeader = new HBox();
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        tableHeader.setPadding(new Insets(10, 15, 10, 15));
        tableHeader.setStyle("-fx-background-color: #7c3aed; -fx-background-radius: 8 8 0 0;");
        tableHeader.getChildren().addAll(
            colLabel("Titre", 200),
            colLabel("Partenaire", 150),
            colLabel("Statut", 100),
            colLabel("Actions", 120)
        );
        container.getChildren().add(tableHeader);

        // Rows
        List<Collaboration> collaborations = collaborationService.getAll();
        if (collaborations.isEmpty()) {
            Label empty = new Label("Aucune collaboration trouvée.");
            empty.setStyle("-fx-font-size: 14px; -fx-text-fill: #888; -fx-padding: 20;");
            container.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < collaborations.size(); i++) {
            Collaboration c = collaborations.get(i);
            HBox row = new HBox();
            row.getStyleClass().add("collaboration-row");
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 15, 10, 15));
            row.setStyle("-fx-background-color: " + (i % 2 == 0 ? "white" : "#f9fafb") + ";");

            Label titre = new Label(c.getTitre());
            titre.setPrefWidth(200);
            titre.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");

            Label partner = new Label(c.getPartnerName());
            partner.setPrefWidth(150);
            partner.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
            
            Label statut = new Label(c.getStatut());
            statut.setPrefWidth(100);
            statut.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

            // Actions
            HBox actions = new HBox(6);
            actions.setAlignment(Pos.CENTER);
            actions.setPrefWidth(120);

            Button btnVoir = actionBtn(FontAwesomeSolid.EYE, "#7c3aed", "#ede9fe");
            btnVoir.setTooltip(new Tooltip("Modifier les détails"));
            btnVoir.setOnAction(e -> showCollaborationForm(c));

            Button btnDelete = actionBtn(FontAwesomeSolid.TRASH_ALT, "#dc2626", "#fee2e2");
            btnDelete.setTooltip(new Tooltip("Supprimer"));
            btnDelete.setOnAction(e -> showDeleteConfirm(c));

            actions.getChildren().addAll(btnVoir, btnDelete);

            row.getChildren().addAll(titre, partner, statut, actions);
            container.getChildren().add(row);
        }
    }

    private Label colLabel(String text, double width) {
        Label l = new Label(text);
        l.setPrefWidth(width);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");
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

    private void showCollaborationForm(Collaboration collaboration) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/collaboration-form-view.fxml"));
            Parent root = loader.load();

            CollaborationFormController controller = loader.getController();
            controller.setCollaboration(collaboration);
            controller.setOnFormClose(updated -> {
                if (updated) {
                    loadCollaborations();
                }
            });

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(collaboration == null ? "Ajouter une Collaboration" : "Modifier la Collaboration");
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger le formulaire de collaboration.");
        }
    }

    private void showDeleteConfirm(Collaboration collaboration) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer la collaboration : " + collaboration.getTitre());
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cette collaboration ? Cette action est irréversible.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            collaborationService.delete(collaboration.getId());
            loadCollaborations();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "La collaboration a été supprimée avec succès.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}