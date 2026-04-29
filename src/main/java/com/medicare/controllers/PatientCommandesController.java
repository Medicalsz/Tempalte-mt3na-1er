package com.medicare.controllers;

import com.medicare.models.Commande;
import com.medicare.models.Produit;
import com.medicare.models.User;
import com.medicare.services.CommandeService;
import com.medicare.services.FacturePdfService;
import com.medicare.services.ProduitService;
import com.medicare.utils.Validators;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Patient view: list own commandes, edit quantity/delivery date (while en_attente), cancel or delete.
 */
public class PatientCommandesController {

    @FXML private VBox container;

    private final CommandeService service = new CommandeService();
    private final ProduitService produitService = new ProduitService();
    private final FacturePdfService pdfService = new FacturePdfService();
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    private void initialize() {
        loadCommandes();
    }

    private void loadCommandes() {
        container.getChildren().clear();

        Label title = new Label("Mes commandes");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1a73e8;");
        container.getChildren().add(title);

        HBox tableHeader = new HBox();
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        tableHeader.setPadding(new Insets(10, 15, 10, 15));
        tableHeader.setStyle("-fx-background-color: #1a73e8; -fx-background-radius: 8 8 0 0;");
        tableHeader.getChildren().addAll(
                colLabel("N° commande", 140), colLabel("Produit", 170), colLabel("Qte", 55),
                colLabel("Total", 90), colLabel("Date", 100), colLabel("Statut", 100),
                colLabel("Actions", 170)
        );
        container.getChildren().add(tableHeader);

        User cu = DashboardPatientController.getCurrentUser();
        List<Commande> commandes = (cu != null) ? service.getByUser(cu.getId()) : List.of();
        addRows(commandes);
    }

    private void addRows(List<Commande> commandes) {
        while (container.getChildren().size() > 2) {
            container.getChildren().remove(2);
        }

        if (commandes.isEmpty()) {
            Label empty = new Label("Vous n'avez pas encore de commande.");
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

            Label num = cellLabel(c.getCommandeNumber() != null ? c.getCommandeNumber() : "-", 140, "#333");
            Label prod = cellLabel(c.getProductName() != null ? c.getProductName() : "#" + c.getProductId(), 170, "#1a73e8");
            Label qty = cellLabel(String.valueOf(c.getQuantity()), 55, "#333");
            Label tot = cellLabel(c.getTotalPrice() != null ? c.getTotalPrice() + " DT" : "-", 90, "#16a34a");
            Label date = cellLabel(c.getCommandeDate() != null ? c.getCommandeDate().format(DF) : "-", 100, "#555");

            Label status = new Label(c.getStatus() != null ? c.getStatus() : "-");
            status.setPrefWidth(100);
            status.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-padding: 3 10; " +
                            "-fx-background-color: " + statusColor(c.getStatus()) + "; -fx-background-radius: 12;");

            HBox actions = new HBox(6);
            actions.setAlignment(Pos.CENTER);
            actions.setPrefWidth(170);

            Button btnView = actionBtn(FontAwesomeSolid.EYE, "#1a73e8", "#dbeafe");
            btnView.setTooltip(new Tooltip("Voir"));
            btnView.setOnAction(e -> showDetails(c));

            Button btnPdf = actionBtn(FontAwesomeSolid.FILE_PDF, "#dc2626", "#fee2e2");
            btnPdf.setTooltip(new Tooltip("Telecharger PDF"));
            btnPdf.setOnAction(e -> downloadPdf(c));

            Button btnEdit = actionBtn(FontAwesomeSolid.EDIT, "#f59e0b", "#fef3c7");
            btnEdit.setTooltip(new Tooltip("Modifier"));
            btnEdit.setDisable(!"en_attente".equalsIgnoreCase(c.getStatus()));
            btnEdit.setOnAction(e -> showEditForm(c));

            Button btnCancel = actionBtn(FontAwesomeSolid.BAN, "#dc2626", "#fee2e2");
            btnCancel.setTooltip(new Tooltip("Annuler"));
            btnCancel.setDisable(!"en_attente".equalsIgnoreCase(c.getStatus())
                                 && !"confirmee".equalsIgnoreCase(c.getStatus()));
            btnCancel.setOnAction(e -> showCancelConfirm(c));

            actions.getChildren().addAll(btnView, btnPdf, btnEdit, btnCancel);

            row.getChildren().addAll(num, prod, qty, tot, date, status, actions);
            container.getChildren().add(row);
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

    // ========== EDIT ==========

    private void showEditForm(Commande c) {
        Produit product = produitService.getById(c.getProductId());
        if (product == null) {
            showInfo("Produit introuvable", "Ce produit n'existe plus.", "#dc2626", FontAwesomeSolid.EXCLAMATION_TRIANGLE);
            return;
        }

        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        Label title = new Label("Modifier : " + product.getName());
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1a73e8;");

        Label priceLbl = new Label("Prix unitaire : " + product.getPrice() + " DT · Stock : " + product.getQuantity());
        priceLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

        TextField qtyField = styledField("Quantite");
        qtyField.setText(String.valueOf(c.getQuantity()));
        TextField totalField = styledField("Total (DT)");
        totalField.setEditable(false);
        totalField.setStyle(totalField.getStyle() + "-fx-background-color: #f3f4f6;");

        DatePicker delivDatePicker = new DatePicker();
        delivDatePicker.setPromptText("Livraison souhaitee");
        delivDatePicker.setPrefHeight(36);
        delivDatePicker.setMaxWidth(Double.MAX_VALUE);
        delivDatePicker.setStyle("-fx-background-radius: 8; -fx-font-size: 13px;");
        if (c.getDeliveryDate() != null) delivDatePicker.setValue(c.getDeliveryDate().toLocalDate());

        TextArea notes = new TextArea(c.getNotes() != null ? c.getNotes() : "");
        notes.setPrefRowCount(2);
        notes.setStyle("-fx-background-radius: 8; -fx-font-size: 13px;");

        Runnable recompute = () -> {
            if (Validators.isStrictlyPositiveInt(qtyField.getText())) {
                int q = Validators.parseInt(qtyField.getText());
                BigDecimal t = (product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(q));
                totalField.setText(t.toString());
            } else {
                totalField.setText("");
            }
        };
        qtyField.textProperty().addListener((obs, o, n) -> recompute.run());
        recompute.run();

        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
        errorLbl.setWrapText(true);

        Button btnSave = new Button("Enregistrer");
        btnSave.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-size: 13px; " +
                         "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 24;");
        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #333; -fx-font-size: 13px; " +
                           "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 9 24;");
        btnCancel.setOnAction(e -> popup.close());

        btnSave.setOnAction(e -> {
            Validators.markValid(qtyField);
            errorLbl.setText("");

            if (!Validators.isStrictlyPositiveInt(qtyField.getText())) {
                Validators.markInvalid(qtyField);
                errorLbl.setText("Quantite invalide (entier > 0).");
                return;
            }
            int qty = Validators.parseInt(qtyField.getText());
            if (qty > product.getQuantity()) {
                Validators.markInvalid(qtyField);
                errorLbl.setText("Stock insuffisant. Disponible : " + product.getQuantity());
                return;
            }
            if (delivDatePicker.getValue() != null && !Validators.isStrictlyFuture(delivDatePicker.getValue())) {
                errorLbl.setText("Date de livraison doit etre dans le futur.");
                return;
            }

            c.setQuantity(qty);
            c.setTotalPrice((product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO)
                    .multiply(BigDecimal.valueOf(qty)));
            c.setNotes(notes.getText());
            c.setDeliveryDate(delivDatePicker.getValue() != null ? delivDatePicker.getValue().atStartOfDay() : null);

            service.update(c);
            popup.close();
            showInfo("Commande mise a jour",
                    product.getName() + " x" + qty, "#16a34a", FontAwesomeSolid.CHECK_CIRCLE);
        });

        HBox btns = new HBox(12, btnCancel, btnSave);
        btns.setAlignment(Pos.CENTER_RIGHT);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(labeled("Quantite *", qtyField), 0, 0);
        grid.add(labeled("Total", totalField), 1, 0);
        grid.add(labeled("Livraison souhaitee", delivDatePicker), 0, 1, 2, 1);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c1, c2);

        VBox box = new VBox(14, title, priceLbl, new Separator(), grid, labeled("Notes", notes), errorLbl, btns);
        box.setPadding(new Insets(25));
        box.setPrefWidth(520);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 16; " +
                     "-fx-border-color: #e0e0e0; -fx-border-radius: 16; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 4);");
        popup.setScene(new Scene(box));
        popup.showAndWait();
        loadCommandes();
    }

    // ========== CANCEL ==========

    private void showCancelConfirm(Commande c) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        FontIcon warn = new FontIcon(FontAwesomeSolid.EXCLAMATION_TRIANGLE);
        warn.setIconSize(44);
        warn.setIconColor(Color.web("#dc2626"));

        Label t = new Label("Annuler cette commande ?");
        t.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");

        Label m = new Label((c.getCommandeNumber() != null ? c.getCommandeNumber() : "Commande #" + c.getId())
                + "\n" + (c.getProductName() != null ? c.getProductName() : ""));
        m.setStyle("-fx-font-size: 13px; -fx-text-fill: #666; -fx-text-alignment: center;");

        Button btnYes = new Button("Annuler la commande");
        btnYes.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-size: 13px; " +
                        "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 20;");
        Button btnNo = new Button("Retour");
        btnNo.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #333; -fx-font-size: 13px; " +
                       "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 20;");

        HBox btns = new HBox(15, btnNo, btnYes);
        btns.setAlignment(Pos.CENTER);

        VBox box = new VBox(15, warn, t, m, btns);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 16; " +
                     "-fx-border-color: #e0e0e0; -fx-border-radius: 16; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 4);");

        popup.setScene(new Scene(box, 420, 270));
        popup.show();
        btnNo.setOnAction(e -> popup.close());
        btnYes.setOnAction(e -> {
            popup.close();
            service.updateStatus(c.getId(), "annulee");
            showInfo("Commande annulee",
                    c.getCommandeNumber() != null ? c.getCommandeNumber() : "Commande #" + c.getId(),
                    "#dc2626", FontAwesomeSolid.BAN);
        });
    }

    // ========== DETAILS ==========

    private void showDetails(Commande c) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        FontIcon icon = new FontIcon(FontAwesomeSolid.SHOPPING_CART);
        icon.setIconSize(44);
        icon.setIconColor(Color.web("#1a73e8"));

        Label t = new Label(c.getCommandeNumber() != null ? c.getCommandeNumber() : "Commande #" + c.getId());
        t.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1a73e8;");

        Label d = new Label(
            "Produit :  " + (c.getProductName() != null ? c.getProductName() : "#" + c.getProductId()) + "\n" +
            "Quantite :  " + c.getQuantity() + "\n" +
            "Total :  " + (c.getTotalPrice() != null ? c.getTotalPrice() + " DT" : "-") + "\n" +
            "Statut :  " + (c.getStatus() != null ? c.getStatus() : "-") + "\n" +
            "Date :  " + (c.getCommandeDate() != null ? c.getCommandeDate().format(DF) : "-") + "\n" +
            "Livraison :  " + (c.getDeliveryDate() != null ? c.getDeliveryDate().format(DF) : "-") + "\n" +
            "Notes :  " + (c.getNotes() != null ? c.getNotes() : "-")
        );
        d.setWrapText(true);
        d.setStyle("-fx-font-size: 13px; -fx-text-fill: #444; -fx-line-spacing: 3;");

        Button close = new Button("Fermer");
        close.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-size: 13px; " +
                       "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 30;");
        close.setOnAction(e -> popup.close());

        VBox box = new VBox(12, icon, t, d, close);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 16; " +
                     "-fx-border-color: #e0e0e0; -fx-border-radius: 16; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 4);");
        popup.setScene(new Scene(box, 420, 410));
        popup.show();
    }

    // ========== INFO POPUP ==========

    private void showInfo(String title, String message, String color, FontAwesomeSolid iconType) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(container.getScene().getWindow());

        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(48);
        icon.setIconColor(Color.web(color));

        Label t = new Label(title);
        t.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1a73e8;");

        Label m = new Label(message);
        m.setStyle("-fx-font-size: 13px; -fx-text-fill: #555;");
        m.setWrapText(true);

        VBox box = new VBox(14, icon, t, m);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 16; " +
                     "-fx-border-color: #e0e0e0; -fx-border-radius: 16; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 4);");
        popup.setScene(new Scene(box, 380, 240));
        popup.show();

        PauseTransition p = new PauseTransition(Duration.seconds(1.4));
        p.setOnFinished(e -> { popup.close(); loadCommandes(); });
        p.play();
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
        return new VBox(4, l, field);
    }

    // ========== PDF ==========

    private void downloadPdf(Commande c) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer la facture PDF");
        String defaultName = "Facture_" +
                (c.getCommandeNumber() != null ? c.getCommandeNumber() : ("CMD-" + c.getId())) + ".pdf";
        fc.setInitialFileName(defaultName);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File out = fc.showSaveDialog(container.getScene().getWindow());
        if (out == null) return;

        try {
            Produit p = produitService.getById(c.getProductId());
            User u = DashboardPatientController.getCurrentUser();
            pdfService.generate(c, p, u, out);
            showInfo("PDF genere", out.getName(), "#16a34a", FontAwesomeSolid.FILE_PDF);
        } catch (Exception ex) {
            ex.printStackTrace();
            showInfo("Echec generation", ex.getMessage(),
                    "#dc2626", FontAwesomeSolid.EXCLAMATION_TRIANGLE);
        }
    }
}
