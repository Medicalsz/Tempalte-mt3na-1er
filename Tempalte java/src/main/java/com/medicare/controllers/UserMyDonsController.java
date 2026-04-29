package com.medicare.controllers;

import com.medicare.models.Don;
import com.medicare.models.User;
import com.medicare.services.DonationService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class UserMyDonsController {

    @FXML private FlowPane donsContainer;

    private final DonationService donationService = new DonationService();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        loadMyDons();
    }

    private void loadMyDons() {
        donsContainer.getChildren().clear();
        User currentUser = DashboardPatientController.getCurrentUser();
        
        if (currentUser == null) return;

        List<Don> myDons = donationService.getDonsByUserId(currentUser.getId());

        if (myDons.isEmpty()) {
            Label noData = new Label("Vous n'avez pas encore effectué de dons.");
            noData.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748b; -fx-padding: 20;");
            donsContainer.getChildren().add(noData);
            return;
        }

        for (Don don : myDons) {
            donsContainer.getChildren().add(createDonCard(don));
        }
    }

    private VBox createDonCard(Don don) {
        VBox card = new VBox(15);
        card.setPrefWidth(300);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: white; " +
                     "-fx-background-radius: 12; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 4);");

        // Header: Type et Statut
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label typeLabel = new Label(don.getType().toUpperCase());
        typeLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #64748b; -fx-background-color: #f1f5f9; -fx-padding: 3 8; -fx-background-radius: 5;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Label statutLabel = new Label(don.getStatut());
        statutLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #059669; -fx-background-color: #ecfdf5; -fx-padding: 4 10; -fx-background-radius: 20;");

        header.getChildren().addAll(typeLabel, spacer, statutLabel);

        // Cause et Date
        Label causeLabel = new Label(don.getCauseNom());
        causeLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        causeLabel.setWrapText(true);

        HBox dateBox = new HBox(5);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        FontIcon calendarIcon = new FontIcon(FontAwesomeSolid.CALENDAR_ALT);
        calendarIcon.setIconSize(12);
        calendarIcon.setIconColor(Color.web("#94a3b8"));
        Label dateLabel = new Label(don.getDate() != null ? don.getDate().format(formatter) : "");
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
        dateBox.getChildren().addAll(calendarIcon, dateLabel);

        // Valeur (Montant ou Matériels)
        VBox valueBox = new VBox(5);
        if ("argent".equals(don.getType())) {
            Label montantLabel = new Label(String.format("%.2f DT", don.getMontant()));
            montantLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #10b981;");
            valueBox.getChildren().add(montantLabel);
        } else {
            Label materielTitle = new Label("Objets donnés :");
            materielTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
            Label materielLabel = new Label(don.getMateriels());
            materielLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
            
            Button btnEdit = new Button("Modifier mon don");
            btnEdit.setMaxWidth(Double.MAX_VALUE);
            btnEdit.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8; -fx-cursor: hand;");
            btnEdit.setGraphic(new FontIcon(FontAwesomeSolid.EDIT) {{ setIconColor(Color.WHITE); setIconSize(12); }});
            btnEdit.setOnAction(e -> onEditDon(don.getId()));
            
            valueBox.getChildren().addAll(materielTitle, materielLabel, btnEdit);
        }

        // Footer: Adresse
        HBox adresseBox = new HBox(5);
        adresseBox.setAlignment(Pos.CENTER_LEFT);
        adresseBox.setStyle("-fx-border-color: #f1f5f9; -fx-border-width: 1 0 0 0; -fx-padding: 10 0 0 0;");
        FontIcon pinIcon = new FontIcon(FontAwesomeSolid.MAP_MARKER_ALT);
        pinIcon.setIconSize(12);
        pinIcon.setIconColor(Color.web("#94a3b8"));
        Label adresseLabel = new Label(don.getAdresse() != null ? don.getAdresse() : "pas d'adresse");
        adresseLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        adresseBox.getChildren().addAll(pinIcon, adresseLabel);

        card.getChildren().addAll(header, causeLabel, dateBox, valueBox, adresseBox);
        return card;
    }

    private void onEditDon(int donId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/user-donation-form-view.fxml"));
            Node view = loader.load();
            
            UserDonationFormController controller = loader.getController();
            controller.setEditMode(donId);
            
            StackPane contentArea = (StackPane) donsContainer.getScene().lookup("#contentArea");
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onBackClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/user-donation-view.fxml"));
            Node view = loader.load();
            StackPane contentArea = (StackPane) donsContainer.getScene().lookup("#contentArea");
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
