package com.medicare.controllers;

import com.medicare.models.Commande;
import com.medicare.services.CommandeService;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Admin view of commandes: read-only list with Accept/Reject actions.
 * Users (patients) create and manage their own commandes elsewhere.
 */
public class CommandeController {

    @FXML private VBox container;

    private final CommandeService service = new CommandeService();
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    private void initialize() {
        loadCommandes();
    }

    private void loadCommandes() {
        container.getChildren().clear();

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Commandes des patients");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #7c3aed;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TextField searchField = new TextField();
        searchField.setPromptText("Rechercher...");
        searchField.setPrefWidth(200);
        searchField.setStyle("-fx-background-radius: 8; -fx-font-size: 13px;");
        searchField.textProperty().addListener((obs, old, val) -> filterCommandes(val));

        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("Tous", "en_attente", "confirmee", "livree", "annulee");
        statusFilter.setValue("Tous");
        statusFilter.setStyle("-fx-background-radius: 8; -fx-font-size: 12px;");
        statusFilter.setOnAction(e -> {
            String v = statusFilter.getValue();
            if ("Tous".equals(v)) addRows(service.getAll());
            else addRows(service.getByStatus(v));
        });

        header.getChildren().addAll(title, spacer, searchField, statusFilter);
        container.getChildren().add(header);

        // Table header
        HBox tableHeader = new HBox();
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        tableHeader.setPadding(new Insets(10, 15, 10, 15));
        tableHeader.setStyle("-fx-background-color: #7c3aed; -fx-background-radius: 8 8 0 0;");

        tableHeader.getChildren().addAll(
            colLabel("N° commande", 130), colLabel("Produit", 140), colLabel("Client", 140),
            colLabel("Qte", 50), colLabel("Total", 80), colLabel("Date", 90),
            colLabel("Statut", 95), colLabel("Actions", 155)
        );
        container.getChildren().add(tableHeader);

        addRows(service.getAll());
    }

    private void addRows(List<Commande> commandes) {
        while (container.getChildren().size() > 2) {
            container.getChildren().remove(2);
        }

        if (commandes.isEmpty()) {
            Label empty = new Label("Aucune commande trouvee.");
            empty.setStyle("-fx-font-size: 14px; -fx-text-fill: #888; -fx-padding: 20;");
            container.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < commandes.size(); i++) {
            Commande c = commandes.get(i);
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 15, 10, 15));
            row.setStyle("-fx-background-color: " + (i % 2 == 0 ? "white" : "#f9fafb") + ";");

            Label num = cellLabel(c.getCommandeNumber() != null ? c.getCommandeNumber() : "-", 130, "#333");
            Label product = cellLabel(c.getProductName() != null ? c.getProductName() : "Produit #" + c.getProductId(), 140, "#1a73e8");
            Label client = cellLabel(c.getUserFullName() != null && !c.getUserFullName().isBlank()
                    ? c.getUserFullName() : "-", 140, "#555");
            Label qty = cellLabel(String.valueOf(c.getQuantity()), 50, "#333");
            Label total = cellLabel(c.getTotalPrice() != null ? c.getTotalPrice() + " DT" : "-", 80, "#16a34a");
            Label date = cellLabel(c.getCommandeDate() != null ? c.getCommandeDate().format(DF) : "-", 90, "#555");

            Label status = new Label(c.getStatus() != null ? c.getStatus() : "-");
            status.setPrefWidth(95);
            status.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-padding: 3 10; " +
                            "-fx-background-color: " + statusColor(c.getStatus()) + "; -fx-background-radius: 12;");

            HBox actions = new HBox(6);
            actions.setAlignment(Pos.CENTER);
            actions.setPrefWidth(155);

            Button btnView = actionBtn(FontAwesomeSolid.EYE, "#7c3aed", "#ede9fe");
            btnView.setTooltip(new Tooltip("Voir"));
            btnView.setOnAction(e -> showDetails(c));

            Button btnAccept = actionBtn(FontAwesomeSolid.CHECK, "#16a34a", "#dcfce7");
            btnAccept.setTooltip(new Tooltip("Accepter"));
            btnAccept.setDisable(!"en_attente".equalsIgnoreCase(c.getStatus()));
            btnAccept.setOnAction(e -> {
                service.updateStatus(c.getId(), "confirmee");
                showPopup("Commande acceptee",
                          c.getCommandeNumber() != null ? c.getCommandeNumber() : "Commande #" + c.getId(),
                          FontAwesomeSolid.CHECK_CIRCLE, "#16a34a");
            });

            Button btnReject = actionBtn(FontAwesomeSolid.TIMES, "#dc2626", "#fee2e2");
            btnReject.setTooltip(new Tooltip("Rejeter"));
            btnReject.setDisable(!"en_attente".equalsIgnoreCase(c.getStatus()));
            btnReject.setOnAction(e -> {
                service.updateStatus(c.getId(), "annulee");
                showPopup("Commande rejetee",
                          c.getCommandeNumber() != null ? c.getCommandeNumber() : "Commande #" + c.getId(),
                          FontAwesomeSolid.BAN, "#dc2626");
            });

            Button btnDeliver = actionBtn(FontAwesomeSolid.TRUCK, "#1a73e8", "#dbeafe");
            btnDeliver.setTooltip(new Tooltip("Marquer livree"));
            btnDeliver.setDisable(!"confirmee".equalsIgnoreCase(c.getStatus()));
            btnDeliver.setOnAction(e -> {
                service.updateStatus(c.getId(), "livree");
                showPopup("Commande livree",
                          c.getCommandeNumber() != null ? c.getCommandeNumber() : "Commande #" + c.getId(),
                          FontAwesomeSolid.TRUCK, "#1a73e8");
            });

            actions.getChildren().addAll(btnView, btnAccept, btnReject, btnDeliver);

            row.getChildren().addAll(num, product, client, qty, total, date, status, actions);
            container.getChildren().add(row);
        }
    }

    private void filterCommandes(String search) {
        if (search == null || search.trim().isEmpty()) {
            addRows(service.getAll());
        } else {
            addRows(service.search(search));
        }
    }

    private String statusColor(String status) {
        if (status == null) return "#6b7280";
        return switch (status.toLowerCase()) {
            case "en_attente" -> "#f59e0b";
            case "confirmee" -> "#1a73e8";
            case "livree" -> "#16a34a";
            case "annulee" -> "#dc2626";
            default -> "#6b7280";
        };
    }

    // ========== DETAILS ==========

    private void showDetails(Commande c) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        FontIcon icon = new FontIcon(FontAwesomeSolid.SHOPPING_CART);
        icon.setIconSize(44);
        icon.setIconColor(Color.web("#7c3aed"));

        Label titleLbl = new Label(c.getCommandeNumber() != null ? c.getCommandeNumber() : ("Commande #" + c.getId()));
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #7c3aed;");

        Label details = new Label(
            "Client :  " + (c.getUserFullName() != null && !c.getUserFullName().isBlank() ? c.getUserFullName() : "-") + "\n" +
            "Produit :  " + (c.getProductName() != null ? c.getProductName() : "#" + c.getProductId()) + "\n" +
            "Quantite :  " + c.getQuantity() + "\n" +
            "Total :  " + (c.getTotalPrice() != null ? c.getTotalPrice() + " DT" : "-") + "\n" +
            "Statut :  " + (c.getStatus() != null ? c.getStatus() : "-") + "\n" +
            "Date commande :  " + (c.getCommandeDate() != null ? c.getCommandeDate().format(DF) : "-") + "\n" +
            "Date livraison :  " + (c.getDeliveryDate() != null ? c.getDeliveryDate().format(DF) : "-") + "\n" +
            "Notes :  " + (c.getNotes() != null ? c.getNotes() : "-")
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

        popup.setScene(new Scene(box, 440, 460));
        popup.show();
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

        PauseTransition pause = new PauseTransition(Duration.seconds(1.3));
        pause.setOnFinished(e -> {
            popup.close();
            loadCommandes();
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
}
